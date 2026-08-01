/**
 * pollDb.js — Camada de acesso ao MySQL das enquetes.
 *
 * As enquetes vivem no banco MySQL do usuário (Umbler), e não mais no arquivo
 * JSON efêmero do Render free (que era apagado a cada spin-down/restart/redeploy).
 *
 * MULTI-APP: o mesmo banco serve vários apps. Cada app tem uma linha na tabela
 * `apps` (slug único, ex.: 'maisfluminense') e suas enquetes ficam isoladas
 * pela coluna `app_id` em `enquetes`. Os endpoints aceitam ?app=slug ou o
 * header X-App-Id para escolher o app (default: 'maisfluminense').
 *
 * Configuração via variáveis de ambiente (com defaults apontando para a
 * conexão Umbler já usada na pasta php/):
 *   DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME
 */
const mysql = require('mysql2/promise');

const DB_CONFIG = {
  host: process.env.DB_HOST || 'mysql741.umbler.com',
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || 'vikkynsnorth',
  password: process.env.DB_PASS || 'yUZu4Q*6.t',
  database: process.env.DB_NAME || 'vikkynsnorth',
  waitForConnections: true,
  connectionLimit: 5,
  queueLimit: 0,
  dateStrings: true, // createdAt volta como string 'YYYY-MM-DD HH:MM:SS'
  charset: 'utf8mb4', // acentos e emojis nas perguntas/opções
};

let pool = null;

function getPool() {
  if (!pool) pool = mysql.createPool(DB_CONFIG);
  return pool;
}

/** Agora (UTC, formato DATETIME do MySQL) */
function nowDb() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ');
}

/**
 * Cria as tabelas caso ainda não existam e migra bancos antigos.
 * - apps: registro de cada aplicativo que usa o banco de enquetes
 * - enquetes: cabeçalho (id, pergunta, ativa, data de criação, app_id)
 * - enquetes_opcoes: opções de cada enquete (voto atômico via UPDATE)
 */
async function init() {
  const conn = getPool();

  // 1) Tabela de apps (multi-app)
  await conn.query(`
    CREATE TABLE IF NOT EXISTS apps (
      id INT AUTO_INCREMENT PRIMARY KEY,
      slug VARCHAR(50) NOT NULL UNIQUE,
      nome VARCHAR(100) NOT NULL,
      created_at DATETIME NOT NULL
    ) DEFAULT CHARSET=utf8mb4
  `);

  // 2) Tabela de enquetes (agora com app_id)
  await conn.query(`
    CREATE TABLE IF NOT EXISTS enquetes (
      id INT AUTO_INCREMENT PRIMARY KEY,
      question VARCHAR(500) NOT NULL,
      active TINYINT(1) NOT NULL DEFAULT 0,
      created_at DATETIME NOT NULL,
      app_id INT NOT NULL DEFAULT 1,
      KEY idx_app (app_id)
    ) DEFAULT CHARSET=utf8mb4
  `);

  // Migração: banco criado antes da versão multi-app (sem coluna app_id)
  const [cols] = await conn.query(`SHOW COLUMNS FROM enquetes LIKE 'app_id'`);
  if (cols.length === 0) {
    await conn.query(
      `ALTER TABLE enquetes ADD COLUMN app_id INT NOT NULL DEFAULT 1, ADD KEY idx_app (app_id)`
    );
    console.log('[pollDb] Migração: coluna app_id adicionada a enquetes.');
  }

  // 3) Tabela de opções
  await conn.query(`
    CREATE TABLE IF NOT EXISTS enquetes_opcoes (
      id INT AUTO_INCREMENT PRIMARY KEY,
      poll_id INT NOT NULL,
      opt_id VARCHAR(4) NOT NULL,
      texto VARCHAR(255) NOT NULL,
      votos INT NOT NULL DEFAULT 0,
      KEY idx_poll (poll_id),
      UNIQUE KEY uq_poll_opt (poll_id, opt_id)
    ) DEFAULT CHARSET=utf8mb4
  `);

  // 4) App padrão (id 1 = Mais Fluminense)
  await conn.query(
    `INSERT IGNORE INTO apps (id, slug, nome, created_at) VALUES (1, 'maisfluminense', 'Mais Fluminense', ?)`,
    [nowDb()]
  );

  console.log('[pollDb] Tabelas apps/enquetes/enquetes_opcoes garantidas.');
}

/** Executa uma query e devolve as linhas (SELECT) ou o resultado (INSERT/UPDATE) */
async function query(sql, params = []) {
  return getPool().query(sql, params);
}

/** SELECT com retorno de linhas */
async function select(sql, params = []) {
  const [rows] = await getPool().query(sql, params);
  return rows;
}

module.exports = { init, query, select, getPool };
