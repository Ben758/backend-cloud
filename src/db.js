const { Pool } = require('pg');
require('dotenv').config();

const url = new URL(process.env.DATABASE_URL);

const pool = new Pool({
  host: url.hostname,
  port: url.port || 5432,
  user: url.username,
  password: decodeURIComponent(url.password),
  database: url.pathname.substring(1),
  ssl: {
    rejectUnauthorized: false
  }
});

// Probar conexión
pool.on('connect', () => {
  console.log('[DB] Pool de conexiones establecido con Supabase.');
});

pool.on('error', (err) => {
  console.error('[DB] Error inesperado en el cliente de base de datos:', err);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
