package maisfluminense.vikkynsnorth.noticias.rss;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import maisfluminense.vikkynsnorth.noticias.model.NewsItem;
import maisfluminense.vikkynsnorth.noticias.model.NewsSource;
import maisfluminense.vikkynsnorth.noticias.util.GoogleNewsUrlResolver;

/**
 * NewsFetcher — busca notícias do Fluminense de múltiplas fontes RSS.
 *
 * Estratégia principal: Google News RSS (agrega GE, Lance, UOL, ESPN, etc.)
 * Estratégia complementar: feeds RSS diretos de sites que ainda os publicam.
 *
 * O Google News RSS funciona assim:
 *   https://news.google.com/rss/search?q=QUERY&hl=pt-BR&gl=BR&ceid=BR:pt-419
 * Cada item retornado tem título, link para o artigo original e data.
 */
public class NewsFetcher {

    private static final String TAG = "NewsFetcher";

    public interface Callback {
        void onSuccess(List<NewsItem> items);
        void onError(Exception e);
    }

    // ─── Formatos de data RSS ────────────────────────────────────────
    private static final List<String> DATE_FORMATS = Arrays.asList(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "dd MMM yyyy HH:mm:ss Z"
    );

    // ─── Fontes ──────────────────────────────────────────────────────
    /**
     * Fontes RSS — diretas e via Google News.
     *
     * Principais:
     *   - Google News: Fluminense (agrega GE, UOL, ESPN, Lance, etc.)
     *   - GE Fluminense: feed específico do time
     *   - Lance! Fluminense: feed específico do time
     *
     * Adicionais:
     *   - UOL Fluminense: feed direto do time
     *   - Google News: consultas variadas para capturar o máximo de notícias
     */
    public static final List<NewsSource> SOURCES = Arrays.asList(
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=Fluminense+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#9E1B32"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=%22Fluminense+FC%22+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#9E1B32"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=%22Tricolor+Carioca%22+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#006442"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=%22EC+Fluminense%22+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#9E1B32"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=Fluminense+not%C3%ADcias+2026&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#9E1B32"),
            new NewsSource("UOL",
                    "https://news.google.com/rss/search?q=site:uol.com.br+fluminense&hl=pt-BR&gl=BR&ceid=BR:pt-419",
                    "#9E1B32"),
            new NewsSource("GE",
                    "https://news.google.com/rss/search?q=site:ge.globo.com+fluminense&hl=pt-BR&gl=BR&ceid=BR:pt-419",
                    "#006442"),
            new NewsSource("Lance!",
                    "https://www.lance.com.br/feed/",
                    "#FF6F00"),
            new NewsSource("UOL Fluminense",
                    "https://rss.uol.com.br/feed/esporte/futebol/times/fluminense.xml",
                    "#9E1B32")
    );

    /** Keywords do Fluminense */
    private static final List<String> KEYWORDS = Arrays.asList(
            "fluminense",
            "flu",
            "tricolor carioca",
            "ec fluminense",
            "fluminense fc",
            "laranjeiras",
            "tricolor das laranjeiras",
            "nense",
            "maracanã",  // notícias específicas do Maracanã com contexto do Fluminense
            "maracana"
    );

    /** Fontes específicas do Fluminense — não precisam de filtro adicional */
    private static final List<String> SPECIFIC_SOURCES = Arrays.asList(
            "Google News",
            "UOL",
            "GE",
            "UOL Fluminense",
            "Lance!"
    );

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ─── fetchAll ────────────────────────────────────────────────────
    public static void fetchAll(Callback callback) {
        final List<NewsItem> allItems = new ArrayList<>();
        final List<Exception> errors  = new ArrayList<>();
        final AtomicInteger pending   = new AtomicInteger(SOURCES.size());

        for (NewsSource source : SOURCES) {
            executor.execute(() -> {
                try {
                    List<NewsItem> items = parseFeed(source);

                    // Filtrar por relevância nas fontes genéricas
                    if (!SPECIFIC_SOURCES.contains(source.getName())) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            items.removeIf(item -> !isRelevant(item));
                        }
                    }

                    synchronized (allItems) { allItems.addAll(items); }

                } catch (Exception e) {
                    synchronized (errors) { errors.add(e); }
                } finally {
                    if (pending.decrementAndGet() == 0) {
                        // Deduplica por link + ordena por data
                        List<NewsItem> result = deduplicate(allItems);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            result.sort((a, b) -> {
                                if (a.getPubDate() == null) return 1;
                                if (b.getPubDate() == null) return -1;
                                return b.getPubDate().compareTo(a.getPubDate());
                            });
                        }
                        mainHandler.post(() -> {
                            if (result.isEmpty() && !errors.isEmpty()) {
                                callback.onError(errors.get(0));
                            } else {
                                callback.onSuccess(result);
                            }
                        });
                    }
                }
            });
        }
    }

    // ─── Parse RSS ───────────────────────────────────────────────────
    private static List<NewsItem> parseFeed(NewsSource source) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        Document doc = Jsoup.connect(source.getRssUrl())
                .userAgent("Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .timeout(15_000)
                .followRedirects(true)
                .parser(org.jsoup.parser.Parser.xmlParser())
                .get();

        Elements entries = doc.select("item");
        if (entries.isEmpty()) entries = doc.select("entry");

        for (Element entry : entries) {
            NewsItem item = parseEntry(entry, source);
            if (item != null && item.getTitle() != null && !item.getTitle().isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private static NewsItem parseEntry(Element entry, NewsSource source) {
        try {
            String title = cleanText(getFirstText(entry, "title"));
            if (title.isEmpty()) return null;

            // Link — Google News usa <link> diferente
            String link = entry.attr("rss:link");
            if (link.isEmpty()) link = getFirstText(entry, "link");
            if (link.isEmpty()) link = entry.selectFirst("link") != null
                    ? entry.selectFirst("link").attr("href") : "";
            if (link.isEmpty()) link = cleanText(getFirstText(entry, "guid"));

            // Detecta Google News pela URL (não pelo nome da fonte — mudou)
            boolean isFromGoogleNews = link.contains("news.google.com");
            if (isFromGoogleNews) {
                link = GoogleNewsUrlResolver.resolve(link);
            }

            String description = cleanText(getFirstText(entry,
                    "description", "summary", "content:encoded"));

            // Imagem — Google News não tem media tags
            String imageUrl = extractImage(entry);

            // Fonte real (Google News embute o nome do site no título: "Título - Fonte")
            String sourceName = source.getName();
            if (isFromGoogleNews && title.contains(" - ")) {
                // Ex: "Atlético-MG vence - UOL" → source="UOL", title="Atlético-MG vence"
                String[] parts = title.split(" - ");
                sourceName = parts[parts.length - 1].trim();
                title = title.substring(0, title.lastIndexOf(" - ")).trim();
            }

            Date pubDate = parseDate(getFirstText(entry, "pubDate", "published", "updated"));

            return new NewsItem(title, link, description, imageUrl,
                    pubDate, sourceName, source.getColor());

        } catch (Exception e) {
            return null;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────
    private static String extractImage(Element entry) {
        // 1. media:content / media:thumbnail com atributo url
        Element media = entry.selectFirst("[url]");
        if (media != null) {
            String url = media.attr("url");
            if (!url.isEmpty() && !url.startsWith("data:")) return url;
        }
        // 2. enclosure
        Element enclosure = entry.selectFirst("enclosure[url]");
        if (enclosure != null) {
            String url = enclosure.attr("url");
            if (!url.isEmpty()) return url;
        }

        // 3. Primeira <img> dentro do HTML do description/content — USA el.html()!
        String[] contentTags = {"content:encoded", "description", "summary", "content"};
        for (String tag : contentTags) {
            Element el = entry.selectFirst(tag);
            if (el != null) {
                String html = el.html();
                if (!html.isEmpty()) {
                    try {
                        Element img = Jsoup.parse(html).selectFirst("img[src]");
                        if (img != null) {
                            String src = img.attr("src");
                            if (!src.isEmpty() && !src.startsWith("data:")) return src;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private static String getFirstText(Element parent, String... tags) {
        for (String tag : tags) {
            Element el = parent.selectFirst(tag);
            if (el != null) {
                String t = el.text();
                if (!t.isEmpty()) return t;
                String h = el.html();
                if (!h.isEmpty()) return h;
            }
        }
        return "";
    }

    private static String cleanText(String text) {
        if (text == null || text.isEmpty()) return "";
        return Jsoup.parse(text).text().trim();
    }

    private static Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        for (String fmt : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setLenient(true);
                return sdf.parse(s.trim());
            } catch (ParseException ignored) {}
        }
        return null;
    }

    /**
     * Filtro: APENAS notícias relacionadas ao Fluminense.
     * Exclui menções a "Fluminense" de outros contextos (cidade, etc.).
     */
    private static boolean isRelevant(NewsItem item) {
        String text = "";
        if (item.getTitle()       != null) text += item.getTitle().toLowerCase();
        if (item.getDescription() != null) text += " " + item.getDescription().toLowerCase();

        // 1. Precisa ter keyword do Fluminense
        boolean hasKeyword = false;
        for (String kw : KEYWORDS) {
            if (text.contains(kw)) { hasKeyword = true; break; }
        }
        if (!hasKeyword) return false;

        // 2. Verifica se é Fluminense (não há outro time grande com esse nome)
        return isFluminenseContent(text);
    }

    /**
     * Determina se o texto se refere ao Fluminense Football Club.
     * Não há outro clube de futebol chamado "Fluminense" no Brasil,
     * mas evitamos falsos positivos com a palavra "fluminense" usada
     * como gentílico (ex: "time fluminense" = time do Rio de Janeiro).
     */
    private static boolean isFluminenseContent(String text) {
        String lower = text.toLowerCase();

        // Se menciona "fluminense" + palavras de futebol, é sobre o time
        String[] footballTerms = {"time", "jogo", "partida", "gol", "jogador",
                "técnico", "tecnico", "campeonato", "vitória", "vitoria",
                "derrota", "empate", "brasileirão", "brasileirao", "série",
                "serie", "copa", "libertadores", "sul-americana", "elenco",
                "contratação", "contratacao", "reforço", "reforco", "lesão",
                "lesao", "treino", "clube", "fc", "estádio", "estadio"};

        if (lower.contains("fluminense")) {
            for (String term : footballTerms) {
                if (lower.contains(term)) return true;
            }
            // Se só tem "fluminense" sem termos de futebol, verifica contexto
            return lower.contains("fluminense") && !lower.contains("rio fluminense")
                    && !lower.contains("cidade fluminense");
        }

        // Palavras-chave específicas do time (flu, tricolor carioca, etc.)
        if (lower.contains("tricolor carioca") || lower.contains("ec fluminense")
                || lower.contains("fluminense fc") || lower.contains("nense")) {
            return true;
        }

        // "flu" sozinho só conta se tiver contexto esportivo
        // Usa espaços para evitar matches parciais (fluxo, fluido, influência, etc.)
        if (lower.contains(" flu ") || lower.contains("flu ") || lower.endsWith(" flu")) {
            for (String term : footballTerms) {
                if (lower.contains(term)) return true;
            }
        }

        // "Maracanã" + contexto do time
        if (lower.contains("maracanã") || lower.contains("maracana")) {
            if (lower.contains("fluminense") || lower.contains("flu")) return true;
        }

        // "Laranjeiras" quase sempre se refere ao estádio do Fluminense em contexto esportivo
        if (lower.contains("laranjeiras")) {
            for (String term : footballTerms) {
                if (lower.contains(term)) return true;
            }
        }

        return false;
    }

    /** Remove itens com o mesmo fingerprint de título (3 palavras + números).
     *  Captura mesma notícia de fontes diferentes com redação diferente. */
    private static List<NewsItem> deduplicate(List<NewsItem> items) {
        List<NewsItem> result = new ArrayList<>();
        java.util.Set<String> seenFingerprints = new java.util.HashSet<>();
        for (NewsItem item : items) {
            String fp = titleFingerprint(item.getTitle());
            if (!fp.isEmpty() && seenFingerprints.add(fp)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Fingerprint AGRESSIVO para deduplicação entre fontes diferentes.
     * Pega 3 palavras significativas + todos os números do título.
     */
    private static String titleFingerprint(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        String[] stopWords = {"o", "a", "os", "as", "de", "do", "da", "dos", "das",
                "em", "no", "na", "nos", "nas", "que", "e", "para", "por", "com",
                "um", "uma", "ao", "aos", "pelo", "pela", "se", "mas", "ou",
                "apos", "antes", "sobre", "entre", "ate", "mais", "menos"};
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        StringBuilder numbers = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            // Coleta números (placar, ano, etc.)
            if (w.matches("\\d+")) {
                numbers.append(w).append(" ");
                continue;
            }
            boolean isStop = false;
            for (String sw : stopWords) {
                if (w.equals(sw)) { isStop = true; break; }
            }
            if (!isStop && count < 3) {
                sb.append(w).append(" ");
                count++;
            }
        }
        sb.append(numbers);
        return sb.toString().trim();
    }
}
