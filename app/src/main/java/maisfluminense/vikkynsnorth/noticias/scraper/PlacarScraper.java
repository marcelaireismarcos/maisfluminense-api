package maisfluminense.vikkynsnorth.noticias.scraper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import maisfluminense.vikkynsnorth.noticias.model.FixtureItem;
import maisfluminense.vikkynsnorth.noticias.model.StandingItem;

/**
 * PlacarScraper — scraping do placardefutebol.com.br.
 *
 * Independente de API key. Sempre atualizado.
 * Usado como fonte primária de tabela e jogos.
 *
 * URLs:
 *   Tabela Série A:       https://www.placardefutebol.com.br/brasileiro-serie-a/classificacao
 *   Jogos do Fluminense:  https://www.placardefutebol.com.br/time/fluminense/proximos-jogos
 *   Últimos jogos:        https://www.placardefutebol.com.br/time/fluminense/ultimos-jogos
 */
public class PlacarScraper {

    private static final String TAG = "PlacarScraper";

    private static final String URL_TABELA =
            "https://www.placardefutebol.com.br/brasileiro-serie-a/classificacao";
    private static final String URL_PROXIMOS =
            "https://www.placardefutebol.com.br/time/fluminense/proximos-jogos";
    private static final String URL_ULTIMOS =
            "https://www.placardefutebol.com.br/time/fluminense/ultimos-jogos";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ─── Callbacks ───────────────────────────────────────────────────
    public interface StandingsCallback {
        void onSuccess(List<StandingItem> standings);
        void onError(String message);
    }

    public interface FixturesCallback {
        void onSuccess(List<FixtureItem> fixtures);
        void onError(String message);
    }

    // ─── Tabela de Classificação ─────────────────────────────────────
    public static void getStandings(StandingsCallback callback) {
        executor.execute(() -> {
            try {
                ////////Log.e(TAG, "Scraping tabela: " + URL_TABELA);
                Document doc = Jsoup.connect(URL_TABELA)
                        .userAgent(USER_AGENT)
                        .timeout(12_000)
                        .followRedirects(true)
                        .get();

                List<StandingItem> standings = parseStandings(doc);
                ////////Log.e(TAG, "Tabela: " + standings.size() + " times");
                mainHandler.post(() -> {
                    if (standings.isEmpty()) {
                        callback.onError("Nenhum dado de classificação encontrado");
                    } else {
                        callback.onSuccess(standings);
                    }
                });
            } catch (Exception e) {
                ////////Log.e(TAG, "Erro tabela: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ─── Próximos e Últimos Jogos ────────────────────────────────────
    public static void getFixtures(FixturesCallback callback) {
        List<FixtureItem> allFixtures = new ArrayList<>();
        int[] pending = {2};

        executor.execute(() -> {
            try {
                List<FixtureItem> proximos = parseFixtures(URL_PROXIMOS, false);
                synchronized (allFixtures) { allFixtures.addAll(proximos); }
            } catch (Exception e) {
                ////////Log.e(TAG, "Próximos jogos erro: " + e.getMessage());
            } finally {
                synchronized (pending) {
                    pending[0]--;
                    if (pending[0] == 0) deliver(allFixtures, callback);
                }
            }
        });

        executor.execute(() -> {
            try {
                List<FixtureItem> ultimos = parseFixtures(URL_ULTIMOS, true);
                synchronized (allFixtures) { allFixtures.addAll(ultimos); }
            } catch (Exception e) {
                ////////Log.e(TAG, "Últimos jogos erro: " + e.getMessage());
            } finally {
                synchronized (pending) {
                    pending[0]--;
                    if (pending[0] == 0) deliver(allFixtures, callback);
                }
            }
        });
    }

    private static void deliver(List<FixtureItem> fixtures, FixturesCallback callback) {
        ////////Log.e(TAG, "Fixtures total: " + fixtures.size());
        mainHandler.post(() -> {
            if (fixtures.isEmpty()) {
                callback.onError("Nenhum jogo encontrado");
            } else {
                callback.onSuccess(fixtures);
            }
        });
    }

    // ─── Parsers ─────────────────────────────────────────────────────
    private static List<StandingItem> parseStandings(Document doc) {
        List<StandingItem> list = new ArrayList<>();
        // Seletores do placardefutebol.com.br
        Elements rows = doc.select("table.classification-table tr, tr.standing-row, "
                + ".table-classification tr, tbody tr");

        ////////Log.e(TAG, "Rows encontradas: " + rows.size());

        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 6) continue;

            try {
                StandingItem item = new StandingItem();
                // Posição
                item.rank = parseInt(cells.get(0).text());
                if (item.rank <= 0) continue;

                // Nome do time (pode ter logo junto)
                Element nameEl = cells.get(1).selectFirst("span, a, .team-name");
                item.teamName = nameEl != null
                        ? nameEl.text().trim()
                        : cells.get(1).text().trim();

                // Logo
                Element imgEl = cells.get(1).selectFirst("img");
                item.logoUrl = imgEl != null ? imgEl.attr("src") : "";
                if (item.logoUrl.startsWith("//")) item.logoUrl = "https:" + item.logoUrl;

                // Estatísticas — posições variam, tenta múltiplos layouts
                if (cells.size() >= 9) {
                    item.played  = parseInt(cells.get(2).text());
                    item.wins    = parseInt(cells.get(3).text());
                    item.draws   = parseInt(cells.get(4).text());
                    item.losses  = parseInt(cells.get(5).text());
                    item.points  = parseInt(cells.get(cells.size() - 1).text());
                    // Saldo de gols — geralmente penúltima coluna
                    item.goalDiff = parseInt(cells.get(cells.size() - 2).text());
                } else {
                    item.played  = parseInt(cells.get(2).text());
                    item.points  = parseInt(cells.get(cells.size() - 1).text());
                }

                // Destacar Fluminense
                item.isFluminense = item.teamName.toLowerCase().contains("fluminense")
                        || item.teamName.toLowerCase().contains("flu")
                        || item.teamName.toLowerCase().contains("tricolor");

                list.add(item);
            } catch (Exception e) {
                ////////Log.e(TAG, "Erro ao parsear linha: " + e.getMessage());
            }
        }
        return list;
    }

    private static List<FixtureItem> parseFixtures(String url, boolean isPast) {
        List<FixtureItem> list = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(12_000)
                    .followRedirects(true)
                    .get();

            // Seletores genéricos para cards de jogo
            Elements gameCards = doc.select(
                    ".match-card, .game-card, .fixture-item, "
                    + ".match-item, .jogo-item, article.match, "
                    + ".partida, .confronto");

            ////////Log.e(TAG, url + " → " + gameCards.size() + " cards");

            for (Element card : gameCards) {
                try {
                    FixtureItem item = new FixtureItem();
                    item.isPast = isPast;

                    // Times
                    Elements teams = card.select(".team-name, .club-name, span.name, "
                            + ".time-nome, .team, h3, strong");
                    if (teams.size() >= 2) {
                        item.homeName = teams.get(0).text().trim();
                        item.awayName = teams.get(1).text().trim();
                    }

                    // Logos
                    Elements logos = card.select("img");
                    if (logos.size() >= 2) {
                        item.homeLogo = fixUrl(logos.get(0).attr("src"));
                        item.awayLogo = fixUrl(logos.get(1).attr("src"));
                    }

                    // Placar ou data
                    Element scoreEl = card.selectFirst(
                            ".score, .placar, .resultado, .score-box");
                    Element dateEl = card.selectFirst(
                            ".date, .data, time, .match-date, .fixture-date");

                    if (scoreEl != null && !scoreEl.text().trim().isEmpty()) {
                        item.scoreText = scoreEl.text().trim();
                    }
                    if (dateEl != null) {
                        item.dateText = dateEl.text().trim();
                        if (dateEl.hasAttr("datetime")) {
                            item.dateText = dateEl.attr("datetime");
                        }
                    }

                    // Rodada / competição
                    Element roundEl = card.selectFirst(".round, .rodada, .competition");
                    if (roundEl != null) item.round = roundEl.text().trim();

                    if (item.homeName != null && !item.homeName.isEmpty()) {
                        list.add(item);
                    }
                } catch (Exception e) {
                    ////////Log.e(TAG, "Erro card: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            ////////Log.e(TAG, "Erro fixtures " + url + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
        return list;
    }

    // ─── Helpers ─────────────────────────────────────────────────────
    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim().replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fixUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return "https://www.placardefutebol.com.br" + url;
        return url;
    }
}
