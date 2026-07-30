/**
 * gameScraper.js — Scraper gratuito do próximo jogo do Fluminense.
 *
 * Fonte: placardefutebol.com.br (HTML estático, sem JS, sem API key)
 * Cache de 30 min via NodeCache.
 *
 * Retorna objeto com: homeTeam, awayTeam, competition, round,
 * venue, city, date, time, timestamp, status, homeGoals, awayGoals
 */
const axios = require('axios');
const cheerio = require('cheerio');
const NodeCache = require('node-cache');

const cache = new NodeCache({ stdTTL: 1800 }); // 30 min

const USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
  '(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36';

/**
 * Busca o próximo jogo do Fluminense no placardefutebol.com.br.
 * Resultado é cacheado por 30 min.
 */
async function fetchNextGame() {
  const cached = cache.get('next_game');
  if (cached) return cached;

  const data = await scrapePlacarFutebol();
  if (data) {
    cache.set('next_game', data);
    return data;
  }

  return null;
}

/**
 * Scrape do placardefutebol.com.br.
 * URL: https://www.placardefutebol.com.br/time/fluminense/proximos-jogos
 */
async function scrapePlacarFutebol() {
  const url = 'https://www.placardefutebol.com.br/time/fluminense/proximos-jogos';
  const { data: html } = await axios.get(url, {
    headers: { 'User-Agent': USER_AGENT },
    timeout: 10000,
  });
  const $ = cheerio.load(html);

  // ─── Estratégia: procurar por linhas/divs que contenham info de jogo ───
  // placardefutebol geralmente lista jogos em <tr> dentro de <table>,
  // ou em <div> com classes como .partida, .jogo, etc.
  // Cada linha tem: time-casa, placar (ou "-"), time-fora, data, horário

  let bestMatch = null;

  // 1. Busca por linhas de tabela e divs de partida
  $('tr, div[class*="partida"], div[class*="jogo"], div[class*="game"], li[class*="match"]').each((_, el) => {
    const text = $(el).text().trim();
    if (!text || text.length < 10) return;

    const lower = text.toLowerCase();
    // Precisa mencionar Fluminense
    if (!lower.includes('fluminense') && !lower.includes('flu') && !lower.includes('nense')) return;

    // Precisa ter indício de data (dd/mm) ou horário (hh:mm)
    const hasDate = /\d{1,2}\/\d{1,2}/.test(text) || /\d{1,2}:\d{2}/.test(text);
    if (!hasDate) return;

    // Extrai as informações
    const parsed = parseGameText(text);
    if (parsed) {
      bestMatch = parsed;
      return false; // break do each
    }
  });

  if (bestMatch) {
    console.log('[gameScraper] Jogo encontrado via tabela:', bestMatch.awayTeam);
    return bestMatch;
  }

  // 2. Fallback: busca por texto puro em toda a página
  const bodyText = $('body').text();
  const lines = bodyText.split('\n').map(l => l.trim()).filter(l => l.length > 10);

  for (const line of lines) {
    const lower = line.toLowerCase();
    if (!lower.includes('fluminense') && !lower.includes('flu') && !lower.includes('nense')) continue;
    const hasDate = /\d{1,2}\/\d{1,2}/.test(line) || /\d{1,2}:\d{2}/.test(line);
    if (!hasDate) continue;

    const parsed = parseGameText(line);
    if (parsed) {
      bestMatch = parsed;
      break;
    }
  }

  if (bestMatch) {
    console.log('[gameScraper] Jogo encontrado via texto:', bestMatch.awayTeam);
    return bestMatch;
  }

  console.log('[gameScraper] Nenhum jogo futuro encontrado');
  return null;
}

/**
 * Extrai informações estruturadas de um texto de jogo.
 * Exemplos de formato esperado:
 *   "Fluminense x Palmeiras - 15/07 21:00 - Maracanã - Brasileirão"
 *   "14/07 - Fluminense 2 x 1 Vasco - Maracanã"
 *   "Fluminense vs Corinthians | 20/07/2026 16:00 | Neo Química Arena"
 */
function parseGameText(text) {
  const clean = text.replace(/\s+/g, ' ').trim();

  // ─── Data ───
  let dateStr = '';
  const dateMatch = clean.match(/(\d{1,2})\/(\d{1,2})(?:\/(\d{4}))?/);
  if (dateMatch) {
    const day = dateMatch[1].padStart(2, '0');
    const month = dateMatch[2].padStart(2, '0');
    const year = dateMatch[3] || '2026';
    dateStr = `${year}-${month}-${day}`;
  }

  // ─── Horário ───
  let timeStr = '';
  const timeMatch = clean.match(/(\d{1,2}:\d{2})/);
  if (timeMatch) {
    timeStr = timeMatch[1];
  }

  // ─── Adversário ───
  // Tenta encontrar o time adversário: o texto após "x", "×" ou "vs"
  let opponent = '';
  const separatorMatch = clean.match(
    /(?:[Ff]luminense|[Ff]lu)\s*(?:x|×|vs|VS|X)\s*([A-Za-zÀ-ÿ\s.]+?)(?:\s*\d|–|—|-|\.|,|\||$)/
  );
  if (separatorMatch) {
    opponent = separatorMatch[1].trim().replace(/\s+/g, ' ');
  }

  // Se não achou com separador, tenta abordagem reversa:
  // remove Fluminense e data, o que sobrar é o adversário
  if (!opponent) {
    let rest = clean
      .replace(/fluminense/gi, '')
      .replace(/\bflu\b/gi, '')
      .replace(/×/g, '')
      .replace(/x/gi, '')
      .replace(/vs/gi, '')
      .replace(/[\d\/:]+/g, '')
      .replace(/[–—|,.\/#!$%^&*;:{}<>=\-_`~()]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
    const words = rest.split(' ').filter(w => w.length > 1);
    if (words.length > 0) {
      opponent = words.slice(0, 3).join(' ');
    }
  }

  if (!opponent) opponent = 'Adversário';

  // ─── Competição ───
  let competition = 'Campeonato Brasileiro';
  if (clean.toLowerCase().includes('copa')) competition = 'Copa do Brasil';
  else if (clean.toLowerCase().includes('libertadores')) competition = 'Copa Libertadores';
  else if (clean.toLowerCase().includes('sul-americana') || clean.toLowerCase().includes('sulamericana'))
    competition = 'Copa Sul-Americana';
  else if (clean.toLowerCase().includes('carioca')) competition = 'Campeonato Carioca';
  else if (clean.toLowerCase().includes('amistoso')) competition = 'Amistoso';
  else if (clean.toLowerCase().includes('mundial') || clean.toLowerCase().includes('interclubes'))
    competition = 'Mundial de Clubes';
  else if (clean.toLowerCase().includes('recopa')) competition = 'Recopa Sul-Americana';

  // ─── Local ───
  let venue = '';
  const venueKeywords = ['maracanã', 'neo química arena', 'allianz', 'morumbi', 'mineirão',
    'beira-rio', 'arena', 'estádio', 'estadio', 'são januario', 'são januário',
    'engenhão', 'nilson santos', 'mané garrincha', 'castelão', 'heriberto hülse',
    'ressacada', 'ilha do retiro', 'arruda', 'fonta nova', 'fonte nova',
    'parque do sabiá', 'mangueirão', 'couto pereira', 'vila belmiro',
    'baixada', 'club', 'serra dourada', 'barradão', 'laranjeiras'];

  for (const kw of venueKeywords) {
    const idx = clean.toLowerCase().indexOf(kw);
    if (idx !== -1) {
      venue = clean.substring(idx, idx + kw.length)
        .replace(/^[,\s]+/, '')
        .replace(/[,\s]+$/, '')
        .trim();
      // Capitaliza
      venue = venue.charAt(0).toUpperCase() + venue.slice(1);
      break;
    }
  }

  // Timestamp
  let timestamp = 0;
  if (dateStr && timeStr) {
    timestamp = Math.floor(new Date(`${dateStr}T${timeStr}:00`).getTime() / 1000);
  }

  // Determina se Flu é casa ou fora
  // Se a partida é no Maraca ou Laranjeiras, Flu é casa
  const fluIsHome = venue.toLowerCase().includes('maracanã') ||
    venue.toLowerCase().includes('laranjeiras') ||
    clean.toLowerCase().indexOf('fluminense') < clean.toLowerCase().indexOf(opponent);

  return {
    source: 'placardefutebol',
    homeTeam: fluIsHome ? 'Fluminense' : opponent,
    awayTeam: fluIsHome ? opponent : 'Fluminense',
    competition: competition,
    round: '',
    venue: venue,
    city: '',
    date: dateStr,
    time: timeStr,
    timestamp: timestamp,
    status: 'NS',
    homeGoals: null,
    awayGoals: null,
  };
}

/** Limpa o cache */
function clearCache() {
  cache.del('next_game');
}

module.exports = { fetchNextGame, clearCache };
