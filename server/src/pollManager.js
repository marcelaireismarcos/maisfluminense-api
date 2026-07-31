/**
 * pollManager.js — Gerenciamento de enquetes da torcida.
 *
 * As enquetes são armazenadas em memória com persistência em JSON.
 * A cada voto, o arquivo é reescrito para survive restart do servidor.
 */
const fs   = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'polls.json');

// ─── Vida útil da enquete: 30 horas ─────────────────────────────
const POLL_LIFETIME_MS = 30 * 60 * 60 * 1000;

/** Verifica se uma enquete já expirou (mais de 30h desde a criação) */
function isExpired(poll) {
  if (!poll || !poll.createdAt) return false;
  const created = new Date(poll.createdAt).getTime();
  return Date.now() - created > POLL_LIFETIME_MS;
}

// ─── Enquetes padrão (criadas na primeira execução) ──────────────
// ─── SEM votos de seed ──────────────────────────────────────────
// Todas as opções começam com 0 votos. Percentuais só aparecem
// quando usuários reais votam.
// Se o servidor reiniciar (Render free tier não persiste disco),
// os votos reais são perdidos. O mecanismo restoreVote no Android
// reenvia o voto do usuário ao detectar totalVotes <= 5.
function getDefaultPolls() {
  const now = new Date().toISOString();
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
        { id: 'a', text: 'Sim, com certeza!',  votes: 0 },
        { id: 'b', text: 'Vai brigar pelo título', votes: 0 },
        { id: 'c', text: 'Vai ficar no G-4',       votes: 0 },
        { id: 'd', text: 'Vai ficar no meio',      votes: 0 },
        { id: 'e', text: 'Infelizmente não',    votes: 0 },
      ],
      active:    false,
      createdAt: now,
    },
  ];
}

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
      polls = getDefaultPolls();
      save();
      console.log('[pollManager] Enquetes padrão criadas (0 votos — aguardando votos reais)');
    }
  } catch (err) {
    console.error('[pollManager] Erro ao carregar:', err.message);
    polls = getDefaultPolls();
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

  // Expiração automática: enquete ativa há mais de 30h é desativada
  if (isExpired(active)) {
    active.active = false;
    save();
    console.log(`[pollManager] Enquete #${active.id} expirada (30h).`);
    return null;
  }

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

  // Expiração automática: não aceita votos em enquete expirada
  if (isExpired(poll)) {
    poll.active = false;
    save();
    console.log(`[pollManager] Voto recusado — enquete #${poll.id} expirada.`);
    return { success: false, message: 'Enquete encerrada.' };
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

/**
 * Restaura o voto de um usuário (re-envio após restart do servidor).
 * Só incrementa se o total de votos for <= 5 (sinal de que o servidor
 * reiniciou e perdeu os dados da enquete).
 */
function restoreVote(pollId, optionId) {
  const poll = polls.find(p => p.id === pollId && p.active);
  if (!poll) {
    return { success: false, message: 'Enquete não encontrada ou inativa.' };
  }

  // Expiração automática: não restaura votos em enquete expirada
  if (isExpired(poll)) {
    poll.active = false;
    save();
    console.log(`[pollManager] Restauração recusada — enquete #${poll.id} expirada.`);
    return { success: false, message: 'Enquete encerrada.' };
  }

  const option = poll.options.find(o => o.id === optionId);
  if (!option) {
    return { success: false, message: 'Opção inválida.' };
  }

  // Só incrementa se o total de votos for <= 5 (sinal de restart)
  const total = poll.options.reduce((sum, o) => sum + o.votes, 0);
  if (total <= 5) {
    option.votes = (option.votes || 0) + 1;
    save();
    console.log('[pollManager] Voto restaurado para opção ' + optionId);
  }

  const newTotal = poll.options.reduce((sum, o) => sum + o.votes, 0);
  return {
    success: true,
    message: total <= 5 ? 'Voto restaurado!' : 'Enquete já possui dados.',
    poll: {
      id:         poll.id,
      question:   poll.question,
      options:    poll.options.map(o => ({
        id:    o.id,
        text:  o.text,
        votes: o.votes,
        pct:   newTotal > 0 ? Math.round((o.votes / newTotal) * 100) : 0,
      })),
      totalVotes: newTotal,
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
      expired:    isExpired(p),
    };
  });
}

/**
 * Reseta todos os votos da enquete ativa para 0.
 * Útil quando o servidor tem dados de seed ou corrompidos.
 */
function resetActivePoll() {
  const active = polls.find(p => p.active);
  if (!active) {
    return { success: false, message: 'Nenhuma enquete ativa para resetar.' };
  }
  active.options.forEach(o => { o.votes = 0; });
  save();
  console.log('[pollManager] Votos da enquete ativa resetados para 0');
  return { success: true, message: 'Votos resetados com sucesso!' };
}

/** Ativa uma enquete e desativa as demais (reinicia as 30h de validade) */
function activatePoll(pollId) {
  const found = polls.find(p => p.id === pollId);
  if (!found) return { success: false, message: 'Enquete não encontrada.' };
  polls.forEach(p => { p.active = (p.id === pollId); });
  // Reinicia o relógio de 30h ao ativar
  found.createdAt = new Date().toISOString();
  save();
  return { success: true, message: `Enquete #${pollId} ativada (30h de votação).` };
}

// ─── Inicializar ────────────────────────────────────────────────
load();

module.exports = { getActivePoll, vote, restoreVote, getAllPolls, activatePoll, resetActivePoll };
