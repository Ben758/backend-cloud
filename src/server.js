require('dotenv').config();
const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const db = require('./db');

const app = express();
const PORT = process.env.PORT || 5000;

// Logger middleware
const fs = require('fs');
const path = require('path');
const logFilePath = path.join(__dirname, '..', 'server_requests.log');

const bcrypt = require('bcrypt');

const { v4: uuidv4 } = require("uuid");
const QRCode = require("qrcode");

app.use((req, res, next) => {
  const logMsg = `[${new Date().toISOString()}] ${req.method} ${req.url} - IP: ${req.ip} - Length: ${req.headers['content-length'] || 0}\n`;
  try {
    fs.appendFileSync(logFilePath, logMsg);
  } catch (e) {}
  console.log(logMsg.trim());
  next();
});

app.use(cors());
app.use(express.json());

// Helper para validar formato de UUID
function parseUuid(val) {
  if (!val) return null;
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  return uuidRegex.test(val) ? val : null;
}

// Helper para extraer device_id de los códigos autogenerados por el ESP32
function extractDeviceId(idString) {
  if (!idString) return 'ESP32-DESCONOCIDO';
  const parts = idString.split('-');
  if (parts.length > 1) {
    // Si es ESP32-BUS-001-1749720000, unimos todo menos el último elemento (timestamp)
    return parts.slice(0, -1).join('-');
  }
  return idString;
}

// Ruta principal / Health Check
app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'bus-facultativo-umsa-backend', timestamp: Date.now() });
});

/**
 * POST /api/sync/eventos
 * Recibe la sincronización de viajes y eventos del bus de forma manual.
 */
app.post('/api/sync/eventos', async (req, res) => {
  const { viajes, eventos } = req.body;

  const viajesRecibidos = [];
  const eventosRecibidos = [];

  // 1. Procesar Viajes en Lote (Bulk Insert/Update)
  if (Array.isArray(viajes) && viajes.length > 0) {
    console.log(`[SYNC] Procesando ${viajes.length} viajes recibidos en lote...`);
    const values = [];
    const valuePlaceholders = [];
    
    for (let i = 0; i < viajes.length; i++) {
      const v = viajes[i];
      const tripId = crypto.randomUUID();
      const offset = i * 11;
      
      values.push(
        tripId,
        v.viaje_id,
        v.dispositivo_id,
        v.bus_codigo,
        v.ruta_codigo,
        v.sentido || 'IDA',
        v.inicio_ts,
        v.fin_ts || null,
        v.estado,
        v.total_aceptados || 0,
        v.total_rechazados || 0
      );
      
      valuePlaceholders.push(`($${offset + 1}, $${offset + 2}, $${offset + 3}, $${offset + 4}, $${offset + 5}, $${offset + 6}, $${offset + 7}, $${offset + 8}, $${offset + 9}, $${offset + 10}, $${offset + 11})`);
    }

    const queryText = `
      INSERT INTO viajes (id, viaje_codigo, device_id, bus_codigo, ruta_codigo, sentido, inicio_ts, fin_ts, estado, total_aceptados, total_rechazados)
      VALUES ${valuePlaceholders.join(', ')}
      ON CONFLICT (viaje_codigo) DO UPDATE
      SET fin_ts = EXCLUDED.fin_ts,
          estado = EXCLUDED.estado,
          ruta_codigo = EXCLUDED.ruta_codigo,
          sentido = EXCLUDED.sentido,
          total_aceptados = EXCLUDED.total_aceptados,
          total_rechazados = EXCLUDED.total_rechazados
    `;

    try {
      await db.query(queryText, values);
      viajes.forEach(v => viajesRecibidos.push(v.viaje_id));
    } catch (err) {
      console.error(`[SYNC] Error en inserción masiva de viajes:`, err.message);
    }
  }

  // 2. Procesar Eventos en Paralelo
  if (Array.isArray(eventos) && eventos.length > 0) {
    console.log(`[SYNC] Procesando ${eventos.length} eventos de validación...`);
    
    await Promise.all(eventos.map(async (evento) => {
      const {
        evento_id,
        viaje_id,
        uid,
        usuario_id,
        timestamp,
        resultado,
        motivo,
        saldo_viajes_restantes
      } = evento;

      const deviceId = extractDeviceId(evento_id);
      const validUsuarioId = parseUuid(usuario_id);

      try {
        // Registrar viaje de respaldo si no fue registrado antes (fallback)
        const tripId = crypto.randomUUID();
        const tripTimestamp = timestamp || Math.floor(Date.now() / 1000);
        await db.query(`
          INSERT INTO viajes (id, viaje_codigo, device_id, bus_codigo, ruta_codigo, sentido, inicio_ts, estado)
          VALUES ($1, $2, $3, $4, $5, 'IDA', $6, 0)
          ON CONFLICT (viaje_codigo) DO NOTHING
        `, [
          tripId,
          viaje_id,
          deviceId,
          'BUS-FI-01',
          'RUTA-POR-DEFECTO',
          tripTimestamp
        ]);

        // Insertar evento de validación
        const logId = crypto.randomUUID();
        const insertResult = await db.query(`
          INSERT INTO eventos_validacion (
            id, evento_id, viaje_codigo, device_id, uid, usuario_id, timestamp, resultado, motivo, saldo_viajes_restantes
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
          ON CONFLICT (evento_id) DO NOTHING
        `, [
          logId,
          evento_id,
          viaje_id,
          deviceId,
          uid,
          validUsuarioId,
          timestamp,
          resultado,
          motivo,
          saldo_viajes_restantes
        ]);

        // Descontar saldo maestro en base exitosa
        if (insertResult.rowCount > 0 && resultado === 1 && motivo === 1) {
          if (uid && !uid.startsWith('CASH-')) {
            console.log(`[SYNC] Descontando viaje para tarjeta UID ${uid} en Supabase...`);
            await db.query(
              `UPDATE usuarios 
               SET saldo_viajes = saldo_viajes - 1 
               WHERE id = (SELECT usuario_id FROM tarjetas WHERE uid = $1)`,
              [uid]
            );
          }
        }
        eventosRecibidos.push(evento_id);
      } catch (err) {
        console.error(`[SYNC] Error al insertar evento ${evento_id}:`, err.message);
      }
    }));
  }

  res.json({ ok: true, viajes_recibidos: viajesRecibidos, eventos_recibidos: eventosRecibidos });
});

/**
 * GET /api/sync/balances
 * Retorna todos los saldos y tarjetas autorizadas con CI y RU reales de Supabase.
 */
app.get('/api/sync/balances', async (req, res) => {
  const { dispositivo_id } = req.query;
  console.log(`[SYNC] Descarga de balances solicitada por dispositivo: ${dispositivo_id || 'Desconocido'}`);

  try {
    const result = await db.query(`
      SELECT t.uid, u.id as usuario_id,
             concat(u.nombres, ' ', u.apellidos) as nombre,
             u.codigo_universitario,
             u.ci,
             u.carrera,
             u.saldo_viajes
      FROM tarjetas t
      JOIN usuarios u ON t.usuario_id = u.id
      WHERE t.estado = 1 AND u.estado = 1
    `);

    res.json({ tarjetas: result.rows });
  } catch (err) {

  console.error("================================");
  console.error("ERROR COMPLETO:");
  console.dir(err, { depth: null });
  console.error("================================");

  res.status(500).json({
    error: String(err)
  });

}
});


app.get('/api/usuario/:codigo', async (req, res) => {
  try {

    const { codigo } = req.params;

    const result = await db.query(
      `
      SELECT
        id,
        codigo_universitario,
        ci,
        carrera,
        nombres,
        apellidos,
        saldo_viajes
      FROM usuarios
      WHERE codigo_universitario = $1
      `,
      [codigo]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({
        error: 'Usuario no encontrado'
      });
    }

    res.json(result.rows[0]);

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }
});
// Global error-handling middleware
app.use((err, req, res, next) => {
  const errEscaped = err.message ? err.message : String(err);
  const errMsg = `[${new Date().toISOString()}] ERROR: ${errEscaped}\nStack: ${err.stack || ''}\n`;
  try {
    fs.appendFileSync(logFilePath, errMsg);
  } catch (e) {}
  console.error(errMsg.trim());
  res.status(err.status || 500).json({ error: errEscaped });
});
app.post('/api/register', async (req, res) => {

  try {

    const {
      nombres,
      apellidos,
      ci,
      codigo_universitario,
      correo,
      password
    } = req.body;

    const existe = await db.query(
      `SELECT id FROM usuarios
       WHERE codigo_universitario = $1
       OR correo = $2`,
      [codigo_universitario, correo]
    );

    if (existe.rows.length > 0) {
      return res.status(400).json({
        error: 'Usuario ya registrado'
      });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const nuevoUsuario = await db.query(
      `
      INSERT INTO usuarios (
        id,
        nombres,
        apellidos,
        ci,
        codigo_universitario,
        correo,
        password_hash,
        saldo_viajes
      )
      VALUES (
        gen_random_uuid(),
        $1,$2,$3,$4,$5,$6,0
      )
      RETURNING *
      `,
      [
        nombres,
        apellidos,
        ci,
        codigo_universitario,
        correo,
        passwordHash
      ]
    );

    res.json({
      ok: true,
      usuario: nuevoUsuario.rows[0]
    });

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }

});

app.post('/api/login', async (req, res) => {

  try {

    const { correo, password } = req.body;

    const result = await db.query(
      `
      SELECT *
      FROM usuarios
      WHERE correo = $1
      `,
      [correo]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({
        error: 'Usuario no encontrado'
      });
    }

    const usuario = result.rows[0];

    const valido = await bcrypt.compare(
      password,
      usuario.password_hash
    );

    if (!valido) {
      return res.status(401).json({
        error: 'Contraseña incorrecta'
      });
    }

    res.json({
      ok: true,
      usuario: {
        id: usuario.id,
        nombres: usuario.nombres,
        apellidos: usuario.apellidos,
        correo: usuario.correo,
        codigo_universitario: usuario.codigo_universitario,
        saldo_viajes: usuario.saldo_viajes
      }
    });

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }

});
app.post('/api/recargar', async (req, res) => {

  try {

    const {
      codigo_universitario,
      cantidad
    } = req.body;

    const usuario = await db.query(
      `
      SELECT *
      FROM usuarios
      WHERE codigo_universitario = $1
      `,
      [codigo_universitario]
    );

    if (usuario.rows.length === 0) {

      return res.status(404).json({
        error: 'Usuario no encontrado'
      });

    }

    const actualizado = await db.query(
      `
      UPDATE usuarios
      SET saldo_viajes = saldo_viajes + $1
      WHERE codigo_universitario = $2
      RETURNING saldo_viajes
      `,
      [cantidad, codigo_universitario]
    );

    await db.query(
  `
  INSERT INTO recargas (
    codigo_universitario,
    cantidad
  )
  VALUES ($1, $2)
  `,
  [codigo_universitario, cantidad]
);

    res.json({
      ok: true,
      saldo_actual: actualizado.rows[0].saldo_viajes
    });

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }

});

app.get('/api/usuario/:ru', async (req, res) => {

  try {

    const ru = req.params.ru;

    const result = await db.query(
      `
      SELECT
        id,
        nombres,
        apellidos,
        correo,
        codigo_universitario,
        saldo_viajes
      FROM usuarios
      WHERE codigo_universitario = $1
      `,
      [ru]
    );

    if (result.rows.length === 0) {

      return res.status(404).json({
        error: 'Usuario no encontrado'
      });

    }

    res.json(result.rows[0]);

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }
});

app.post('/api/generar-qr', async (req, res) => {

  try {

    const {
      codigo_universitario,
      monto,
      glosa
    } = req.body;

    const transaccion_id = crypto.randomUUID();

    const qr_image_url =
      "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
      encodeURIComponent(
        `${codigo_universitario}|${monto}|${transaccion_id}`
      );

    res.json({
      ok: true,
      transaccion_id,
      qr_image_url
    });

  } catch (err) {

    console.error(err);

    res.status(500).json({
      ok: false
    });

  }

});
app.post('/api/verificar-pago', async (req, res) => {

  try {

    const {
      transaccion_id
    } = req.body;

    console.log(
      `[PAGO] Verificando transacción ${transaccion_id}`
    );

    // Simulación:
    // por ahora siempre aprobado

    res.json({
      ok: true,
      estado: "PAGADO"
    });

  } catch (err) {

    console.error(err);

    res.status(500).json({
      ok: false
    });

  }

});
app.get('/api/historial/:ru', async (req, res) => {

  try {

    const { ru } = req.params;

    const resultado = await db.query(
      `
      SELECT
        cantidad,
        fecha
      FROM recargas
      WHERE codigo_universitario = $1
      ORDER BY fecha DESC
      `,
      [ru]
    );

    res.json(resultado.rows);

  } catch (err) {

    console.error(err);

    res.status(500).json({
      error: 'Error interno'
    });

  }

});
app.listen(PORT, () => {
  console.log(`[SYSTEM] Servidor API corriendo en el puerto ${PORT}`);
});
