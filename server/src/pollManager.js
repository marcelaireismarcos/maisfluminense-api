/**
 * pollManager.js — Gerenciamento de enquetes da torcida.
 *
 * As enquetes são armazenadas em memória com persistência em JSON.
 * A cada voto, o arquivo é reescrito para survive restart do servidor.
 */
const fs   = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'polls.json');

// ─── Enquetes padrão (criadas na primeira execução) ──────────────
const DEFAULT_POLLS = [
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
    createdAt: new Date().toISOString(),
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
    createdAt: new Date().toISOString(),
  },
  {
    id: 3,
    question: 'O Fluminense vai ser campeão brasileiro em 2026?',
    options: [
      { id: 'a', text: 'Sim, com certeza! 💚',  votes: 0 },
      { id: 'b', text: 'Vai brigar pelo título', votes: 0 },
      { id: 'c', text: 'Vai ficar no G-4',       votes: 0 },
      { id: 'd', text: 'Vai ficar no meio',      votes: 0 },
      { id: 'e', text: 'Infelizmente não 🙁',    votes: 0 },
    ],
    active:    false,
    createdAt: new Date().toISOString(),
  },
];

// ─── Estado em memória ──────────────────────────────────────────
let polls = [];

// ─── Carregar do disco ──────────────────────────────────────────
function load() {
  try {
    const dir = path.dirname(DATA_FILE);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

    if (fs.existsSync(DATA_FILE)) {
      const raw = fs.readFileSync(DATA_FILE, 'utf-8');
      polls = JSON.parse(raw);
      console.log(`[pollManager] ${polls.length} enquetes carregadas`);
    } else {
      polls = JSON.parse(JSON.stringify(DEFAULT_POLLS));
      save();
      console.log('[pollManager] Enquetes padrão criadas');
    }
  } catch (err) {
    console.error('[pollManager] Erro ao carregar:', err.message);
    polls = JSON.parse(JSON.stringify(DEFAULT_POLLS));
  }
}

// ─── Salvar no disco ───────────────────────────────────────────
function save() {
  try {
    const dir = path.dirname(DATA_FILE);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(DATA_FILE, JSON.stringify(polls, null, 2), 'utf-8');
  } catch (err) {
    console.error('[pollManager] Erro ao salvar:', err.message);
  }
}

// ─── API Pública ───────────────────────────────────────────────

/** Retorna a enquete ativa (ou null) */
function getActivePoll() {
  const active = polls.find(p => p.active);
  if (!active) return null;

  // Retorna cópia com totais calculados
  const total = active.options.reduce((sum, o) => sum + o.votes, 0);
  return {
    id:         active.id,
    question:   active.question,
    options:    active.options.map(o => ({
      id:     o.id,
      text:   o.text,
      votes:  o.votes,
      pct:    total > 0 ? Math.round((o.votes / total) * 100) : 0,
    })),
    totalVotes: total,
    createdAt:  active.createdAt,
  };
}

/**
 * Registra um voto.
 * @param {number} pollId
 * @param {string} optionId
 * @returns {{ success: boolean, message: string, poll?: object }}
 */
function vote(pollId, optionId) {
  const poll = polls.find(p => p.id === pollId && p.active);
  if (!poll) {
    return { success: false, message: 'Enquete não encontrada ou inativa.' };
  }

  const option = poll.options.find(o => o.id === optionId);
  if (!option) {
    return { success: false, message: 'Opção inválida.' };
  }

  option.votes = (option.votes || 0) + 1;
  save();

  const total = poll.options.reduce((sum, o) => sum + o.votes, 0);
  return {
    success: true,
    message: 'Voto computado!',
    poll: {
      id:         poll.id,
      question:   poll.question,
      options:    poll.options.map(o => ({
        id:    o.id,
        text:  o.text,
        votes: o.votes,
        pct:   total > 0 ? Math.round((o.votes / total) * 100) : 0,
      })),
      totalVotes: total,
      createdAt:  poll.createdAt,
    },
  };
}

/** Retorna lista de todas as enquetes (para debug/admin) */
function getAllPolls() {
  return polls.map(p => {
    const total = p.options.reduce((sum, o) => sum + o.votes, 0);
    return {
      id:         p.id,
      question:   p.question,
      active:     p.active,
      options:    p.options.map(o => ({
        id:    o.id,
        text:  o.text,
        votes: o.votes,
        pct:   total > 0 ? Math.round((o.votes / total) * 100) : 0,
      })),
      totalVotes: total,
      createdAt:  p.createdAt,
    };
  });
}

/** Ativa uma enquete e desativa as demais */
function activatePoll(pollId) {
  const found = polls.find(p => p.id === pollId);
  if (!found) return { success: false, message: 'Enquete não encontrada.' };
  polls.forEach(p => { p.active = (p.id === pollId); });
  save();
  return { success: true, message: `Enquete #${pollId} ativada.` };
}

// ─── Inicializar ────────────────────────────────────────────────
load();

module.exports = { getActivePoll, vote, getAllPolls, activatePoll };
