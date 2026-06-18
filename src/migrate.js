const fs = require('fs');
const path = require('path');
const { Client } = require('pg');

const sqlPath = path.join(__dirname, '..', '..', 'database', 'cloud-postgresql', 'schema_cloud.sql');
const sqlContent = fs.readFileSync(sqlPath, 'utf8');

// Usamos el host y puerto correctos que descubrimos
const host = 'aws-1-us-west-2.pooler.supabase.com';
const user = 'postgres.oweibnrnqndeohhlioue';
const password = 'adminbus123*';
const database = 'postgres';
const port = 5432; // Intentamos Session Mode (5432) para las migraciones de DDL

async function runMigration() {
  console.log(`Conectando a Supabase Pooler (${host}:${port})...`);
  const client = new Client({
    host,
    user,
    password,
    database,
    port,
    ssl: { rejectUnauthorized: false }
  });

  try {
    await client.connect();
    console.log('¡Conectado exitosamente!');
    console.log('Aplicando esquema SQL de base de datos...');
    await client.query(sqlContent);
    console.log('¡Esquema de base de datos migrado correctamente en Supabase!');
  } catch (err) {
    console.error(`Error durante la migración: ${err.message}`);
    // Si falla por puerto en Session Mode, intentamos Transaction Mode
    if (port === 5432) {
      console.log('Intentando re-conectar usando Transaction Mode (puerto 6543)...');
      const fallbackClient = new Client({
        host,
        user,
        password,
        database,
        port: 6543,
        ssl: { rejectUnauthorized: false }
      });
      try {
        await fallbackClient.connect();
        await fallbackClient.query(sqlContent);
        console.log('¡Esquema migrado correctamente usando puerto 6543!');
        await fallbackClient.end();
      } catch (fallbackErr) {
        console.error(`Error en fallback: ${fallbackErr.message}`);
        process.exit(1);
      }
    } else {
      process.exit(1);
    }
  } finally {
    try {
      await client.end();
    } catch (e) {}
    console.log('Conexión cerrada.');
  }
}

runMigration();
