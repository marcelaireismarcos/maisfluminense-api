// Polyfill Promise.allSettled para Node.js < 12
if (!Promise.allSettled) {
  Promise.allSettled = function(promises) {
    return Promise.all(promises.map(function(p) {
      return p
        .then(function(value) { return { status: 'fulfilled', value: value }; })
        .catch(function(reason) { return { status: 'rejected', reason: reason }; });
    }));
  };
}

const express   = require('express');
const cors      = require('cors');
const NodeCache = require('node-cache');
const rssFetcher    = require('./fetchers/rssFetcher');
const outrasFetcher = require('./fetchers/outrasFetcher');
const newsScraper   = require('./fetchers/newsScraper');
const pollManager   = require('./pollManager');
const gameScraper   = require('./fetchers/gameScraper');

const app   = express();
const cache = new NodeCache({ stdTTL: 300 }); // cache 5 minutos

app.use(cors());
app.use(express.json());

// ─── Helpers ──────────────────────────────────────────────────
const STOP_WORDS = new Set(['o', 'a', 'os', 'as', 'de', 'do', 'da', 'dos', 'das',
  'em', 'no', 'na', 'nos', 'nas', 'que', 'e', 'para', 'por', 'com',
  'um', 'uma', 'ao', 'aos', 'pelo', 'pela', 'se', 'mas', 'ou']);

/** Fingerprint agressivo: remove acentos, stop words, pega 5 palavras significativas */
function titleFingerprint(title) {
  if (!title) return '';
  const s = title
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9 ]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  const words = s.split(' ').filter(w => w && !STOP_WORDS.has(w));
  return words.slice(0, 5).join(' ');
}

/** Deduplica por link E por fingerprint do título */
function deduplicate(items) {
  const seenLinks = new Set();
  const seenFingerprints = new Set();
  return items.filter(item => {
    const linkKey = item.link || '';
    const titleKey = titleFingerprint(item.title);
    if (titleKey && seenFingerprints.has(titleKey)) return false;
    if (linkKey && seenLinks.has(linkKey)) return false;
    if (linkKey) seenLinks.add(linkKey);
    if (titleKey) seenFingerprints.add(titleKey);
    return true;
  });
}

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    cache_keys: cache.keys().length
  });
});

// Limpa o cache (útil após deploy)
app.get('/cache/clear', (req, res) => {
  cache.flushAll();
  res.json({ ok: true, message: 'Cache limpo' });
});

// Debug: mostra primeiros 3 itens de cada fonte com campo image
app.get('/debug', async (req, res) => {
  const rssFetcher = require('./fetchers/rssFetcher');
  try {
    const items = await rssFetcher.fetchAll();
    // Agrupa por fonte, mostra só title + image + link
    const bySource = {};
    items.forEach(i => {
      if (!bySource[i.source]) bySource[i.source] = [];
      if (bySource[i.source].length < 3) {
        bySource[i.source].push({
          title: i.title,
          image: i.image,
          link:  i.link ? i.link.substring(0, 80) : null,
        });
      }
    });
    res.json(bySource);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Endpoint de notícias do Vitória-BA
app.get('/noticias', async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 50, 100);

  const cached = cache.get('noticias');
  if (cached) {
    console.log(`[cache] ${cached.length} itens`);
    return res.json(cached.slice(0, limit));
  }

  try {
    // RSS + Scrapers em paralelo
    const [rssItems, scrapedItems] = await Promise.allSettled([
      rssFetcher.fetchAll(),
      newsScraper.fetchAll(),
    ]);

    let items = [];
    if (rssItems.status === 'fulfilled') items.push(...rssItems.value);
    if (scrapedItems.status === 'fulfilled') items.push(...scrapedItems.value);

    console.log(`[merge] RSS: ${rssItems.status === 'fulfilled' ? rssItems.value.length : 0} itens, Scrapers: ${scrapedItems.status === 'fulfilled' ? scrapedItems.value.length : 0} itens`);

    // Deduplica por link E título
    const unique = deduplicate(items);

    // Ordena por data
    unique.sort((a, b) => {
      if (!a.date) return 1;
      if (!b.date) return -1;
      return new Date(b.date) - new Date(a.date);
    });

    cache.set('noticias', unique);
    console.log(`[ok] ${unique.length} notícias`);
    res.json(unique.slice(0, limit));

  } catch (err) {
    console.error('[erro]', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Endpoint: Outras notícias (futebol geral, sem Vitória-BA) ───
app.get('/outras-noticias', async (req, res) => {
  const limit = Math.min(parseInt(req.query.limit) || 50, 100);

  const cached = cache.get('outras-noticias');
  if (cached) {
    return res.json(cached.slice(0, limit));
  }

  try {
    const items = await outrasFetcher.fetchAll();

    // Deduplica por link E título
    const unique = deduplicate(items);

    unique.sort((a, b) => {
      if (!a.date) return 1;
      if (!b.date) return -1;
      return new Date(b.date) - new Date(a.date);
    });

    cache.set('outras-noticias', unique);
    console.log(`[ok] ${unique.length} outras notícias`);
    res.json(unique.slice(0, limit));

  } catch (err) {
    console.error('[erro outras]', err.message);
    res.status(500).json({ error: err.message });
  }
});

// ══════════════════════════════════════════════
// ENQUETES DA TORCIDA (multi-app)
// ══════════════════════════════════════════════

/**
 * Identifica o app da requisição: query ?app=slug ou header X-App-Id.
 * Default: 'maisfluminense' (app atual — sem mudança para o Android).
 */
function resolveAppSlug(req) {
  const fromQuery = req.query && req.query.app;
  const fromHeader = req.headers && req.headers['x-app-id'];
  const slug = fromQuery || fromHeader || 'maisfluminense';
  return String(slug).trim();
}

/** Resolve o appId numérico (registra o app automaticamente se for novo) */
async function appIdFrom(req) {
  return pollManager.resolveAppId(resolveAppSlug(req));
}

/** GET /enquetes/ativa — Retorna a enquete ativa do app */
app.get('/enquetes/ativa', async (req, res) => {
  try {
    const appId = await appIdFrom(req);
    const poll = await pollManager.getActivePoll(appId);
    if (!poll) {
      return res.status(404).json({ error: 'Nenhuma enquete ativa no momento.' });
    }
    res.json(poll);
  } catch (err) {
    console.error('[enquetes/ativa] Erro:', err.message);
    res.status(500).json({ error: 'Erro interno ao consultar enquete.' });
  }
});

/**
 * POST /enquetes/votar — Registra um voto
 * Body: { pollId: number, optionId: string }
 */
app.post('/enquetes/votar', async (req, res) => {
  try {
    const { pollId, optionId } = req.body;

    if (!pollId || !optionId) {
      return res.status(400).json({ success: false, message: 'pollId e optionId são obrigatórios.' });
    }

    const appId = await appIdFrom(req);
    const result = await pollManager.vote(Number(pollId), optionId, appId);
    if (!result.success) {
      return res.status(400).json(result);
    }

    res.json(result);
  } catch (err) {
    console.error('[enquetes/votar] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao registrar voto.' });
  }
});

/** GET /enquetes/todas — Lista todas as enquetes do app (debug/admin) */
app.get('/enquetes/todas', async (req, res) => {
  try {
    const appId = await appIdFrom(req);
    res.json(await pollManager.getAllPolls(appId));
  } catch (err) {
    console.error('[enquetes/todas] Erro:', err.message);
    res.status(500).json({ error: 'Erro interno ao listar enquetes.' });
  }
});

/**
 * POST /enquetes/restaurar-voto — Restaura um voto perdido pelo restart do servidor.
 * Body: { pollId: number, optionId: string }
 * Diferença do /votar: só incrementa se o total de votos for muito baixo
 * (sinal de que o servidor reiniciou e perdeu os dados).
 */
app.post('/enquetes/restaurar-voto', async (req, res) => {
  try {
    const { pollId, optionId } = req.body;

    if (!pollId || !optionId) {
      return res.status(400).json({ success: false, message: 'pollId e optionId são obrigatórios.' });
    }

    const appId = await appIdFrom(req);
    const result = await pollManager.restoreVote(Number(pollId), optionId, appId);
    if (!result.success) {
      return res.status(400).json(result);
    }

    res.json(result);
  } catch (err) {
    console.error('[enquetes/restaurar-voto] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao restaurar voto.' });
  }
});

/** POST /enquetes/reset — Reseta todos os votos da enquete ativa do app */
app.post('/enquetes/reset', async (req, res) => {
  try {
    const appId = await appIdFrom(req);
    const result = await pollManager.resetActivePoll(appId);
    res.json(result);
  } catch (err) {
    console.error('[enquetes/reset] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao resetar votos.' });
  }
});

/** POST /enquetes/ativar — Ativa uma enquete pelo ID (admin) */
app.post('/enquetes/ativar', async (req, res) => {
  try {
    const { pollId } = req.body;
    if (!pollId) {
      return res.status(400).json({ success: false, message: 'pollId é obrigatório.' });
    }
    const appId = await appIdFrom(req);
    const result = await pollManager.activatePoll(Number(pollId), appId);
    res.json(result);
  } catch (err) {
    console.error('[enquetes/ativar] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao ativar enquete.' });
  }
});

/**
 * POST /enquetes/criar — Cria uma nova enquete (admin)
 * Body: { question: string, options: string[], active?: boolean }
 */
app.post('/enquetes/criar', async (req, res) => {
  try {
    const { question, options, active } = req.body || {};
    const appId = await appIdFrom(req);
    const result = await pollManager.createPoll(question, options, active === true, appId);
    if (!result.success) {
      return res.status(400).json(result);
    }
    res.json(result);
  } catch (err) {
    console.error('[enquetes/criar] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao criar enquete.' });
  }
});

/** POST /enquetes/encerrar — Encerra (desativa) uma enquete pelo ID (admin) */
app.post('/enquetes/encerrar', async (req, res) => {
  try {
    const { pollId } = req.body;
    if (!pollId) {
      return res.status(400).json({ success: false, message: 'pollId é obrigatório.' });
    }
    const appId = await appIdFrom(req);
    const result = await pollManager.closePoll(Number(pollId), appId);
    if (!result.success) {
      return res.status(400).json(result);
    }
    res.json(result);
  } catch (err) {
    console.error('[enquetes/encerrar] Erro:', err.message);
    res.status(500).json({ success: false, message: 'Erro interno ao encerrar enquete.' });
  }
});

/** GET /enquetes/:id — Retorna uma enquete específica do app (ativa ou encerrada) com resultados */
app.get('/enquetes/:id', async (req, res) => {
  try {
    const appId = await appIdFrom(req);
    const poll = await pollManager.getPollById(Number(req.params.id), appId);
    if (!poll) {
      return res.status(404).json({ error: 'Enquete não encontrada.' });
    }
    res.json(poll);
  } catch (err) {
    console.error('[enquetes/:id] Erro:', err.message);
    res.status(500).json({ error: 'Erro interno ao consultar enquete.' });
  }
});

// ══════════════════════════════════════════════
// PRÓXIMO JOGO (scraper gratuito — sem API key)
// ══════════════════════════════════════════════

/** GET /proximo-jogo — Retorna o próximo jogo do Fluminense */
app.get('/proximo-jogo', async (req, res) => {
  try {
    const game = await gameScraper.fetchNextGame();
    if (!game) {
      return res.status(404).json({
        success: false,
        message: 'Nenhum jogo futuro encontrado.'
      });
    }
    res.json({ success: true, fixture: game });
  } catch (err) {
    console.error('[proximo-jogo] Erro:', err.message);
    res.status(500).json({
      success: false,
      message: 'Erro ao buscar próximo jogo: ' + err.message
    });
  }
});

/** GET /proximo-jogo/clear-cache — Limpa o cache do scraper */
app.get('/proximo-jogo/clear-cache', (req, res) => {
  gameScraper.clearCache();
  res.json({ ok: true, message: 'Cache do próximo jogo limpo.' });
});

const PORT = process.env.PORT || 3000;

/**
 * Tenta inicializar as enquetes no MySQL com retries INDEFINIDOS (banco pode
 * acordar a qualquer momento — spin-up, manutenção, rede). Se o banco estiver
 * fora, o servidor segue no ar (notícias/health) e as enquetes voltam sozinhas
 * quando o banco responder, sem precisar de redeploy.
 * Log com throttle: registra apenas a 1ª, 2ª, 4ª, 8ª... tentativa (evita spam).
 */
async function initPollsWithRetry(attempt = 0) {
  try {
    await pollManager.init();
    console.log('[startup] Enquetes inicializadas no MySQL.');
  } catch (err) {
    if (attempt <= 1 || (attempt & (attempt - 1)) === 0) {
      console.error(`[startup] Enquetes ainda sem banco (tentativa ${attempt + 1}):`, err.message);
    }
    setTimeout(() => initPollsWithRetry(attempt + 1), 60 * 1000);
  }
}

// Sobe o servidor imediatamente (health check, notícias, outros endpoints).
// A inicialização das enquetes roda em paralelo com retries automáticos.
app.listen(PORT, '0.0.0.0', () => {
  console.log(`API na porta ${PORT}`);
});
initPollsWithRetry();