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
 *
 * Estrutura atual do HTML (jul/2026):
 *   <a class="match__lg" href="...">
 *     <div class="match__lg_card">
 *       <div class="match__lg_card--league">Campeonato Brasileiro</div>
 *       <div class="match__lg_card--ht-name text">Fluminense</div>
 *       <div class="match__lg_card--at-name text">Palmeiras</div>
 *       <div class="match__lg_card--ht-logo">...</div>
 *       <div class="match__lg_card--at-logo">...</div>
 *       <div class="match__lg_card--info">
 *         <div class="match__lg_card--datetime">SÁB, 15/08<br>16:30</div>
 *       </div>
 *     </div>
 *   </a>
 */
async function scrapePlacarFutebol() {
  const url = 'https://www.placardefutebol.com.br/time/fluminense/proximos-jogos';
  const { data: html } = await axios.get(url, {
    headers: { 'User-Agent': USER_AGENT },
    timeout: 10000,
  });
  const $ = cheerio.load(html);

  // ─── Busca TODOS os cards de jogos ───
  const games = [];

  $('a.match__lg').each((_, el) => {
    const card = $(el).find('> .match__lg_card');
    if (!card.length) return;

    const league    = card.find('.match__lg_card--league').text().trim();
    const homeName  = card.find('.match__lg_card--ht-name').text().trim();
    const awayName  = card.find('.match__lg_card--at-name').text().trim();
    const dateTimeRaw = card.find('.match__lg_card--datetime').text().trim();

    // Verifica se Fluminense está envolvido
    const homeIsFlu = /fluminense/i.test(homeName);
    const awayIsFlu = /fluminense/i.test(awayName);
    if (!homeIsFlu && !awayIsFlu) return;

    // Extrai data e horário do texto "SÁB, 15/08\n16:30"
    let dateStr = '';
    let timeStr = '';
    const lines = dateTimeRaw.split('\n').map(l => l.trim()).filter(l => l);

    for (const line of lines) {
      // Procura padrão dd/mm
      const dMatch = line.match(/(\d{1,2})\/(\d{1,2})/);
      if (dMatch) {
        const day = dMatch[1].padStart(2, '0');
        const month = dMatch[2].padStart(2, '0');
        dateStr = `2026-${month}-${day}`;
      }
      // Procura horário hh:mm
      const tMatch = line.match(/(\d{1,2}:\d{2})/);
      if (tMatch) {
        timeStr = tMatch[1];
      }
    }

    // Timestamp
    let timestamp = 0;
    if (dateStr && timeStr) {
      timestamp = Math.floor(new Date(`${dateStr}T${timeStr}:00`).getTime() / 1000);
    }

    // Mapa de competições para normalizar o nome
    const compMap = {
      'copa do brasil': 'Copa do Brasil',
      'campeonato brasileiro': 'Campeonato Brasileiro',
      'copa libertadores': 'Copa Libertadores',
      'copa sul-americana': 'Copa Sul-Americana',
      'campeonato carioca': 'Campeonato Carioca',
      'recopa sul-americana': 'Recopa Sul-Americana',
      'mundial de clubes': 'Mundial de Clubes',
    };
    const leagueLower = league.toLowerCase().trim();
    const competition = compMap[leagueLower] || league;

    games.push({
      source: 'placardefutebol',
      homeTeam: homeName,
      awayTeam: awayName,
      competition: competition,
      round: '',
      venue: '',
      city: '',
      date: dateStr,
      time: timeStr,
      timestamp: timestamp,
      status: 'NS',
      homeGoals: null,
      awayGoals: null,
    });
  });

  if (games.length === 0) {
    console.log('[gameScraper] Nenhum jogo encontrado');
    return null;
  }

  // Filtra apenas jogos FUTUROS (timestamp > agora) e ordena pelo mais próximo
  const now = Math.floor(Date.now() / 1000);
  const futureGames = games
    .filter(g => g.timestamp === 0 || g.timestamp > now)
    .sort((a, b) => (a.timestamp || Infinity) - (b.timestamp || Infinity));

  if (futureGames.length === 0) {
    console.log('[gameScraper] Nenhum jogo futuro encontrado');
    return null;
  }

  const nextGame = futureGames[0];
  console.log('[gameScraper] Próximo jogo:',
    nextGame.homeTeam, 'x', nextGame.awayTeam,
    '-', nextGame.date, nextGame.time,
    '-', nextGame.competition);

  return nextGame;
}

/** Limpa o cache */
function clearCache() {
  cache.del('next_game');
}

module.exports = { fetchNextGame, clearCache };
