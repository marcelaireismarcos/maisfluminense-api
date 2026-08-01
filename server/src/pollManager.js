/**
 * pollManager.js — Gerenciamento de enquetes da torcida.
 *
 * Persistência: MySQL (pollDb.js). Antes, as enquetes viviam em um arquivo
 * JSON no filesystem efêmero do Render free — o que era apagado a cada
 * spin-down, restart ou redeploy. Agora os dados (inclusive o histórico de
 * enquetes encerradas) ficam no banco do usuário e sobrevivem a tudo.
 *
 * MULTI-APP: todas as funções recebem o `appId` do app dono da enquete.
 * O slug do app (ex.: 'maisfluminense') é resolvido para o id via
 * resolveAppId() — que registra o app automaticamente no primeiro uso.
 *
 * Todas as funções públicas são assíncronas.
 */
const fs   = require('fs');
const path = require('path');
const pollDb = require('./pollDb');

const DATA_FILE = path.join(__dirname, '..', 'data', 'polls.json');

const DEFAULT_APP_SLUG = 'maisfluminense';

// ─── Vida útil da enquete: 30 horas ─────────────────────────────
const POLL_LIFETIME_MS = 30 * 60 * 60 * 1000;

/** Converte DATETIME do MySQL ('YYYY-MM-DD HH:MM:SS') em timestamp ms */
function toMs(dateStr) {
  if (!dateStr) return 0;
  return new Date(String(dateStr).replace(' ', 'T') + 'Z').getTime();
}

/** Normaliza qualquer data (ISO, Date, etc.) para o formato DATETIME do MySQL (UTC) */
function toDbDate(value) {
  if (!value) return nowDb();
  const s = String(value);
  // Já está no formato MySQL (criado pelo nowDb)
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(s)) return s;
  const d = new Date(s);
  return Number.isNaN(d.getTime()) ? nowDb() : d.toISOString().slice(0, 19).replace('T', ' ');
}

function isExpired(poll) {
  if (!poll || !poll.createdAt) return false;
  const created = toMs(poll.createdAt);
  return created > 0 && Date.now() - created > POLL_LIFETIME_MS;
}

/** Agora (UTC, formato do MySQL) */
function nowDb() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ');
}

// ─── Enquetes padrão (criadas na primeira execução) ──────────────
// ─── SEM votos de seed ──────────────────────────────────────────
function getDefaultPolls() {
  const now = nowDb();
  return [
    {
      id: 1,
      question: 'Qual contratação você mais quer para o Fluminense?',
      options: [
        { id: 'a', text: 'Gabriel Barbosa (Gabigol)',  votes: 0 },
        { id: 'b', text: 'Arrascaeta',                  votes: 0 },
        { id: 'c', text: 'Pedro (Flamengo)',             votes: 0 },
        { id: 'd', text: 'Nenhum desses',                votes: 0 },
      ],
      active:    true,
      createdAt: now,
    },
    {
      id: 2,
      question: 'Qual o maior ídolo da história do Fluminense?',
      options: [
        { id: 'a', text: 'Fred',               votes: 0 },
        { id: 'b', text: 'Castilho',            votes: 0 },
        { id: 'c', text: 'Assis',               votes: 0 },
        { id: 'd', text: 'Conca',               votes: 0 },
        { id: 'e', text: 'Telê Santana',        votes: 0 },
      ],
      active:    false,
      createdAt: now,
    },
    {
      id: 3,
      question: 'O Fluminense vai ser campeão brasileiro em 2026?',
      options: [
        { id: 'a', text: 'Sim, com certeza!',    votes: 0 },
        { id: 'b', text: 'Vai brigar pelo título', votes: 0 },
        { id: 'c', text: 'Vai ficar no G-4',       votes: 0 },
        { id: 'd', text: 'Vai ficar no meio',      votes: 0 },
        { id: 'e', text: 'Infelizmente não',       votes: 0 },
      ],
      active:    false,
      createdAt: now,
    },
  ];
}

// ─── Multi-app ──────────────────────────────────────────────────
// Cache slug -> id (evita SELECT a cada requisição)
const appIdCache = new Map();

/**
 * Resolve o slug de um app para o id numérico (cria o app se não existir).
 * @param {string} [slug] - ex.: 'maisfluminense'. Default: maisfluminense
 * @returns {Promise<number>}
 */
async function resolveAppId(slug) {
  const s = String(slug || DEFAULT_APP_SLUG).trim().toLowerCase() || DEFAULT_APP_SLUG;
  if (appIdCache.has(s)) return appIdCache.get(s);

  let rows = await pollDb.select('SELECT id FROM apps WHERE slug = ?', [s]);
  if (rows.length) {
    appIdCache.set(s, rows[0].id);
    return rows[0].id;
  }

  // INSERT IGNORE evita corrida: se dois requests criarem o mesmo app ao
  // mesmo tempo, um é ignorado pelo UNIQUE (slug) e ambos re-leem o id.
  await pollDb.query(
    'INSERT IGNORE INTO apps (slug, nome, created_at) VALUES (?, ?, ?)',
    [s, s, nowDb()]
  );
  rows = await pollDb.select('SELECT id FROM apps WHERE slug = ?', [s]);
  const id = rows.length ? rows[0].id : 1;
  appIdCache.set(s, id);
  console.log(`[pollManager] App "${s}" registrado automaticamente (id ${id}).`);
  return id;
}

// ─── Montagem da resposta pública ───────────────────────────────
function toPublic(poll) {
  const total = poll.options.reduce((sum, o) => sum + (o.votes || 0), 0);
  return {
    id:         poll.id,
    question:   poll.question,
    active:     poll.active,
    options:    poll.options.map(o => ({
      id:    o.id,
      text:  o.text,
      votes: o.votes || 0,
      pct:   total > 0 ? Math.round(((o.votes || 0) / total) * 100) : 0,
    })),
    totalVotes: total,
    createdAt:  poll.createdAt,
    // ⚠️ 'expired' significa "passou das 30h", NÃO "encerrada manualmente".
    // Para saber se foi encerrada pelo admin, use active === false.
    expired:    isExpired(poll),
  };
}

/** Lê uma enquete + opções do banco e monta o objeto completo */
async function loadPollWithOptions(row) {
  const options = await pollDb.select(
    'SELECT opt_id, texto, votos FROM enquetes_opcoes WHERE poll_id = ? ORDER BY id',
    [row.id]
  );
  return {
    id:         row.id,
    question:   row.question,
    active:     !!row.active,
    createdAt:  row.created_at,
    appId:      row.app_id,
    options:    options.map(o => ({ id: o.opt_id, text: o.texto, votes: o.votos || 0 })),
  };
}

/** Busca o cabeçalho de uma enquete (sempre escopada ao app) */
async function getPollRow(pollId, appId) {
  const rows = await pollDb.select(
    'SELECT * FROM enquetes WHERE id = ? AND app_id = ?',
    [pollId, appId]
  );
  return rows.length ? rows[0] : null;
}

/**
 * Torna uma enquete a ÚNICA ativa do app e reinicia o relógio de 30h.
 * Usado por activatePoll() e createPoll(active=true).
 */
async function setAsOnlyActive(pollId, appId) {
  await pollDb.query('UPDATE enquetes SET active = 0 WHERE app_id = ?', [appId]);
  await pollDb.query(
    'UPDATE enquetes SET active = 1, created_at = ? WHERE id = ? AND app_id = ?',
    [nowDb(), pollId, appId]
  );
}

/** Insere uma enquete (cabeçalho + opções) no banco. Retorna o id. */
async function insertPoll(poll, appId) {
  const [header] = await pollDb.query(
    'INSERT INTO enquetes (question, active, created_at, app_id) VALUES (?, ?, ?, ?)',
    [poll.question, poll.active ? 1 : 0, toDbDate(poll.createdAt), appId]
  );
  const pollId = header.insertId;
  for (const opt of poll.options) {
    await pollDb.query(
      'INSERT INTO enquetes_opcoes (poll_id, opt_id, texto, votos) VALUES (?, ?, ?, ?)',
      [pollId, opt.id, opt.text, opt.votes || 0]
    );
  }
  return pollId;
}

// ─── Inicialização / migração ───────────────────────────────────
async function init() {
  await pollDb.init();

  const appId = await resolveAppId(DEFAULT_APP_SLUG);

  const countRows = await pollDb.select('SELECT COUNT(*) AS n FROM enquetes');
  if (countRows[0].n > 0) {
    console.log(`[pollManager] ${countRows[0].n} enquetes carregadas do MySQL`);
    return;
  }

  // Tabela vazia — tenta migrar do polls.json antigo (se existir e tiver dados)
  let migrated = false;
  try {
    if (fs.existsSync(DATA_FILE)) {
      const raw = JSON.parse(fs.readFileSync(DATA_FILE, 'utf-8'));
      if (Array.isArray(raw) && raw.length > 0) {
        for (const poll of raw) {
          await insertPoll({
            question:  poll.question,
            active:    !!poll.active,
            createdAt: toDbDate(poll.createdAt),
            options:   (poll.options || []).map(o => ({
              id: o.id, text: o.text, votes: o.votes || 0,
            })),
          }, appId);
        }
        console.log(`[pollManager] Migradas ${raw.length} enquetes do polls.json para o MySQL`);
        migrated = true;
      }
    }
  } catch (err) {
    console.error('[pollManager] Erro ao migrar polls.json:', err.message);
  }

  if (!migrated) {
    const defaults = getDefaultPolls();
    for (const poll of defaults) {
      await insertPoll(poll, appId);
    }
    console.log('[pollManager] Enquetes padrão criadas no MySQL (0 votos)');
  }
}

// ─── API Pública ────────────────────────────────────────────────

/** Retorna a enquete ativa de um app (ou null) */
async function getActivePoll(appId) {
  const rows = await pollDb.select(
    'SELECT * FROM enquetes WHERE active = 1 AND app_id = ? LIMIT 1',
    [appId]
  );
  if (!rows.length) return null;

  const poll = await loadPollWithOptions(rows[0]);

  // Expiração automática: enquete ativa há mais de 30h é desativada
  if (isExpired(poll)) {
    await pollDb.query('UPDATE enquetes SET active = 0 WHERE id = ?', [poll.id]);
    console.log(`[pollManager] Enquete #${poll.id} expirada (30h).`);
    return null;
  }

  return toPublic(poll);
}

/** Busca uma enquete pelo id (ativa ou encerrada) — para consulta de resultados */
async function getPollById(pollId, appId) {
  const row = await getPollRow(pollId, appId);
  if (!row) return null;
  const poll = await loadPollWithOptions(row);
  return toPublic(poll);
}

/**
 * Registra um voto (incremento atômico no MySQL — sem corrida entre requests).
 * @param {number} pollId
 * @param {string} optionId
 * @param {number} appId
 */
async function vote(pollId, optionId, appId) {
  const row = await getPollRow(pollId, appId);
  if (!row || !row.active) {
    return { success: false, message: 'Enquete não encontrada ou inativa.' };
  }

  const poll = await loadPollWithOptions(row);

  // Expiração automática: não aceita votos em enquete expirada
  if (isExpired(poll)) {
    await pollDb.query('UPDATE enquetes SET active = 0 WHERE id = ?', [poll.id]);
    console.log(`[pollManager] Voto recusado — enquete #${poll.id} expirada.`);
    return { success: false, message: 'Enquete encerrada.' };
  }

  const exists = poll.options.some(o => o.id === optionId);
  if (!exists) {
    return { success: false, message: 'Opção inválida.' };
  }

  // Voto atômico: UPDATE ... SET votos = votos + 1
  await pollDb.query(
    'UPDATE enquetes_opcoes SET votos = votos + 1 WHERE poll_id = ? AND opt_id = ?',
    [pollId, optionId]
  );

  const updated = await getPollById(pollId, appId);
  return { success: true, message: 'Voto computado!', poll: updated };
}

/**
 * Restaura o voto de um usuário (re-envio após restart do servidor).
 * Só incrementa se o total de votos for <= 5 (sinal de que o servidor
 * reiniciou e perdeu os dados da enquete).
 */
async function restoreVote(pollId, optionId, appId) {
  const row = await getPollRow(pollId, appId);
  if (!row || !row.active) {
    return { success: false, message: 'Enquete não encontrada ou inativa.' };
  }

  const poll = await loadPollWithOptions(row);

  if (isExpired(poll)) {
    await pollDb.query('UPDATE enquetes SET active = 0 WHERE id = ?', [poll.id]);
    console.log(`[pollManager] Restauração recusada — enquete #${poll.id} expirada.`);
    return { success: false, message: 'Enquete encerrada.' };
  }

  const exists = poll.options.some(o => o.id === optionId);
  if (!exists) {
    return { success: false, message: 'Opção inválida.' };
  }

  const total = poll.options.reduce((sum, o) => sum + (o.votes || 0), 0);
  if (total <= 5) {
    await pollDb.query(
      'UPDATE enquetes_opcoes SET votos = votos + 1 WHERE poll_id = ? AND opt_id = ?',
      [pollId, optionId]
    );
    console.log('[pollManager] Voto restaurado para opção ' + optionId);
  }

  const updated = await getPollById(pollId, appId);
  return {
    success: true,
    message: total <= 5 ? 'Voto restaurado!' : 'Enquete já possui dados.',
    poll: updated,
  };
}

/** Lista todas as enquetes de um app (ativas e encerradas) com resultados */
async function getAllPolls(appId) {
  const rows = await pollDb.select(
    'SELECT * FROM enquetes WHERE app_id = ? ORDER BY id ASC',
    [appId]
  );
  const result = [];
  for (const row of rows) {
    const poll = await loadPollWithOptions(row);
    result.push(toPublic(poll));
  }
  return result;
}

/** Reseta todos os votos da enquete ativa de um app para 0 */
async function resetActivePoll(appId) {
  const rows = await pollDb.select(
    'SELECT id FROM enquetes WHERE active = 1 AND app_id = ? LIMIT 1',
    [appId]
  );
  if (!rows.length) {
    return { success: false, message: 'Nenhuma enquete ativa para resetar.' };
  }
  await pollDb.query('UPDATE enquetes_opcoes SET votos = 0 WHERE poll_id = ?', [rows[0].id]);
  console.log('[pollManager] Votos da enquete ativa resetados para 0');
  return { success: true, message: 'Votos resetados com sucesso!' };
}

/** Ativa uma enquete e desativa as demais do app (reinicia as 30h de validade) */
async function activatePoll(pollId, appId) {
  const row = await getPollRow(pollId, appId);
  if (!row) return { success: false, message: 'Enquete não encontrada.' };
  await setAsOnlyActive(pollId, appId);
  console.log(`[pollManager] Enquete #${pollId} ativada (30h de votação).`);
  return { success: true, message: `Enquete #${pollId} ativada (30h de votação).` };
}

/**
 * Cria uma nova enquete para um app.
 * @param {string} question
 * @param {string[]} options - Textos das opções (mínimo 2)
 * @param {boolean} active - Se deve ser ativada imediatamente (default false)
 * @param {number} appId
 */
async function createPoll(question, options, active = false, appId) {
  if (!question || typeof question !== 'string' || !question.trim()) {
    return { success: false, message: 'Pergunta inválida.' };
  }
  if (!Array.isArray(options) || options.length < 2) {
    return { success: false, message: 'Informe ao menos 2 opções.' };
  }

  const cleaned = options
    .map(o => (o == null ? '' : String(o)).trim())
    .filter(o => o.length > 0);
  if (cleaned.length < 2) {
    return { success: false, message: 'Informe ao menos 2 opções válidas.' };
  }

  const letterIds = 'abcdefghijklmnopqrstuvwxyz';
  const pollOptions = cleaned.map((text, i) => ({
    id:    i < letterIds.length ? letterIds[i] : String(i + 1),
    text:  text,
    votes: 0,
  }));

  // Insere sempre como inativa e deixa o setAsOnlyActive cuidar da ativação,
  // evitando a janela em que a nova enquete ficaria ativa antes das demais
  // serem desativadas.
  const pollId = await insertPoll({
    question: question.trim(),
    active:   false,
    options:  pollOptions,
  }, appId);

  // Se for ativada na criação, torna-a a única ativa do app (uma ativa por vez)
  if (active) {
    await setAsOnlyActive(pollId, appId);
  }

  console.log(`[pollManager] Enquete #${pollId} criada${active ? ' e ativada' : ''}: "${question.trim()}"`);
  return { success: true, message: `Enquete #${pollId} criada!`, poll: await getPollById(pollId, appId) };
}

/** Encerra (desativa) uma enquete pelo ID — mantém votos para consulta posterior */
async function closePoll(pollId, appId) {
  const row = await getPollRow(pollId, appId);
  if (!row) {
    return { success: false, message: 'Enquete não encontrada.' };
  }
  if (!row.active) {
    return { success: false, message: `Enquete #${pollId} já está encerrada.` };
  }
  await pollDb.query('UPDATE enquetes SET active = 0 WHERE id = ?', [pollId]);
  console.log(`[pollManager] Enquete #${pollId} encerrada manualmente.`);
  return { success: true, message: `Enquete #${pollId} encerrada!`, poll: await getPollById(pollId, appId) };
}

// ─── Inicializar ────────────────────────────────────────────────
module.exports = {
  init, resolveAppId,
  getActivePoll, vote, restoreVote, getAllPolls,
  activatePoll, resetActivePoll, createPoll, closePoll, getPollById,
};
