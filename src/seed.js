const { Client } = require('pg');
require('dotenv').config();

const client = new Client({
  host: 'aws-1-us-west-2.pooler.supabase.com',
  user: 'postgres.oweibnrnqndeohhlioue',
  password: 'adminbus123*',
  database: 'postgres',
  port: 5432,
  ssl: { rejectUnauthorized: false }
});

async function runSeed() {
  try {
    await client.connect();
    console.log('[SEED] Conectado a la base de datos remota para insertar datos de prueba...');

    // Limpiar datos anteriores (en orden de llaves foráneas)
    console.log('[SEED] Limpiando tablas...');
    await client.query('TRUNCATE TABLE eventos_validacion CASCADE;');
    await client.query('TRUNCATE TABLE viajes CASCADE;');
    await client.query('TRUNCATE TABLE tarjetas CASCADE;');
    await client.query('TRUNCATE TABLE usuarios CASCADE;');
    await client.query('TRUNCATE TABLE dispositivos CASCADE;');

    // 1. Insertar Usuarios
    console.log('[SEED] Insertando usuarios...');
    await client.query(`
      INSERT INTO usuarios (id, codigo_universitario, ci, carrera, nombres, apellidos, saldo_viajes, estado) VALUES
      ('a1111111-1111-1111-1111-111111111111', '1804321', '10234567', 'Ingenieria de Sistemas', 'Benjamin', 'Bustillos Vargas', 5, 1),
      ('b2222222-2222-2222-2222-222222222222', '1809876', '12345678', 'Informatica', 'Maria Elena', 'Lopez Quispe', 2, 1);
    `);

    // 2. Insertar Tarjetas (con UIDs que coinciden con el firmware)
    console.log('[SEED] Insertando tarjetas...');
    await client.query(`
      INSERT INTO tarjetas (id, usuario_id, uid, tipo, estado) VALUES
      ('c1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'D087CD5F', 'FISICA', 1),
      ('c2222222-2222-2222-2222-222222222222', 'b2222222-2222-2222-2222-222222222222', '53342F94017BC0', 'FISICA', 1),
      ('c1111111-1111-1111-1111-222222222222', 'a1111111-1111-1111-1111-111111111111', 'CEL-BENJAMIN-99', 'MOVIL', 1);
    `);

    // 3. Insertar Dispositivo
    console.log('[SEED] Insertando dispositivo validador...');
    await client.query(`
      INSERT INTO dispositivos (id, device_id, bus_codigo, estado) VALUES
      ('d1111111-1111-1111-1111-111111111111', 'ESP32-BUS-001', 'BUS-FI-01', 1);
    `);

    console.log('[SEED] ¡Base de datos de Supabase sembrada correctamente!');
  } catch (err) {
    console.error('[SEED] Error sembrando base de datos:', err.message);
  } finally {
    await client.end();
  }
}

runSeed();
