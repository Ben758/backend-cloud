const { Client } = require('pg');

const regions = [
  'us-west-2', 'us-east-1', 'sa-east-1', 'us-east-2', 'us-west-1',
  'ca-central-1', 'eu-west-1', 'eu-west-2', 'eu-west-3', 'eu-central-1',
  'eu-central-2', 'ap-southeast-1', 'ap-southeast-2', 'ap-northeast-1',
  'ap-northeast-2', 'ap-northeast-3', 'ap-south-1', 'me-central-1'
];
const passwords = ['adminbus123*', '[adminbus123*]'];
const ports = [6543, 5432];
const projectRef = 'oweibnrnqndeohhlioue';

async function testConnection() {
  for (const region of regions) {
    const host = `aws-0-${region}.pooler.supabase.com`;
    
    // Probar primero si el host resuelve para no perder tiempo
    const dns = require('dns').promises;
    let hostExists = false;
    try {
      await dns.resolve4(host);
      hostExists = true;
    } catch (e) {
      // Si no resuelve, saltamos la región
      continue;
    }

    console.log(`\nProbando region activa en DNS: ${region}...`);

    for (const port of ports) {
      for (const password of passwords) {
        const user = `postgres.${projectRef}`;
        
        console.log(`  -> Puerto: ${port}, password: ${password.substring(0, 3)}...`);
        const client = new Client({
          host: host,
          user: user,
          password: password,
          database: 'postgres',
          port: port,
          ssl: { rejectUnauthorized: false }
        });
        
        try {
          await client.connect();
          console.log(`\n=============================================`);
          console.log(`¡CONEXION EXITOSA!`);
          console.log(`Region: ${region}`);
          console.log(`Port: ${port}`);
          console.log(`Host: ${host}`);
          console.log(`User: ${user}`);
          console.log(`Password: ${password}`);
          console.log(`=============================================\n`);
          await client.end();
          process.exit(0);
        } catch (err) {
          // Si el error es de contraseña (password authentication failed), significa que el tenant SÍ existe.
          if (err.message.includes('password authentication failed') || err.message.includes('authentication failed')) {
            console.log(`  [OK - Tenant encontrado, pero clave incorrecta en ${region}:${port}]`);
          } else {
            console.log(`  [Error en ${region}:${port}]: ${err.message}`);
          }
          try {
            await client.end();
          } catch (e) {}
        }
      }
    }
  }
  
  console.log('\nPrueba finalizada. No se pudo conectar.');
  process.exit(1);
}

testConnection();
