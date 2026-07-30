package maisfluminense.vikkynsnorth.noticias.rss;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

/**
 * RssFetcher — busca RSS de futebol GERAL para "Outras Notícias".
 *
 * IMPORTANTE: Este fetcher EXCLUI notícias do Fluminense.
 * O feed principal (FeedFragment) já mostra notícias do Fluminense.
 * Aqui mostramos apenas futebol geral (Série A, outros times, mercado, etc.).
 */
public class RssFetcher {

    private static final String TAG = "RssFetcher";

    public interface Callback {
        void onSuccess(List<NewsItem> items);
        void onError(Exception e);
    }

    /** Formatos de data RFC 822 / ISO 8601 comuns em feeds RSS */
    private static final List<String> DATE_FORMATS = Arrays.asList(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "dd MMM yyyy HH:mm:ss Z"
    );

    /**
     * Fontes RSS de futebol GERAL (NÃO específicas do Fluminense).
     * Todas retornam HTTP 200 com content-type application/rss+xml.
     */
    public static final List<NewsSource> SOURCES = Arrays.asList(
            new NewsSource("Gazeta Geral",
                    "https://www.gazetaesportiva.com/feed/",
                    "#880E4F"),
            new NewsSource("Lance!",
                    "https://www.lance.com.br/feed/",
                    "#FF6F00"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=Brasileirao+Serie+A+2026&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#0066CC"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=mercado+da+bola+futebol+2026&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#2E7D32"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=Copa+do+Brasil+2026+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#1565C0"),
            new NewsSource("Google News",
                    "https://news.google.com/rss/search?q=Libertadores+2026+futebol&hl=pt-BR&gl=BR&ceid=BR%3Apt-419",
                    "#6A1B9A")
    );

    /** Palavras-chave do FLUMINENSE — usadas para EXCLUIR notícias do feed de outras */
    private static final List<String> FLUMINENSE_KEYWORDS = Arrays.asList(
            "fluminense",
            "flu",
            "tricolor carioca",
            "ec fluminense",
            "fluminense fc",
            "nense",
            "laranjeiras"
    );

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void fetchAll(Callback callback) {
        final List<NewsItem> allItems = new ArrayList<>();
        final AtomicInteger pending = new AtomicInteger(SOURCES.size());
        final List<Exception> errors = new ArrayList<>();

        for (NewsSource source : SOURCES) {
            executor.execute(() -> {
                try {
                    List<NewsItem> items = parseFeed(source);
                    // EXCLUI notícias do Fluminense — já aparecem no feed principal
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        items.removeIf(RssFetcher::isFluminenseNews);
                    }
                    ////////Log.e(TAG, source.getName() + " → " + items.size() + " itens (sem Fluminense)");
                    synchronized (allItems) {
                        allItems.addAll(items);
                    }
                } catch (Exception e) {
                    ////////Log.e(TAG, "Erro em " + source.getName() + ": " + e.getMessage());
                    synchronized (errors) {
                        errors.add(e);
                    }
                } finally {
                    if (pending.decrementAndGet() == 0) {
                        List<NewsItem> finalItems;
                        synchronized (allItems) {
                            // Remove itens sem título
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                allItems.removeIf(i -> i.getTitle() == null || i.getTitle().isEmpty());
                            }
                            // Deduplica por título normalizado
                            finalItems = deduplicate(allItems);
                            // Ordena por data — mais recente primeiro
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                finalItems.sort((a, b) -> {
                                    if (a.getPubDate() == null && b.getPubDate() == null) return 0;
                                    if (a.getPubDate() == null) return 1;
                                    if (b.getPubDate() == null) return -1;
                                    return b.getPubDate().compareTo(a.getPubDate());
                                });
                            }
                        }
                        ////////Log.e(TAG, "Total de itens carregados: " + finalItems.size());
                        mainHandler.post(() -> {
                            if (finalItems.isEmpty() && !errors.isEmpty()) {
                                callback.onError(errors.get(0));
                            } else {
                                callback.onSuccess(finalItems);
                            }
                        });
                    }
                }
            });
        }
    }

    private static List<NewsItem> parseFeed(NewsSource source) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        Document doc = Jsoup.connect(source.getRssUrl())
                .userAgent("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .timeout(12_000)
                .followRedirects(true)
                .parser(org.jsoup.parser.Parser.xmlParser())
                .get();

        // RSS 2.0: <item>
        Elements entries = doc.select("item");
        // Atom: <entry>
        if (entries.isEmpty()) entries = doc.select("entry");

        ////////Log.e(TAG, source.getName() + " raw entries: " + entries.size());

        for (Element entry : entries) {
            NewsItem item = parseEntry(entry, source);
            if (item != null && item.getTitle() != null && !item.getTitle().trim().isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private static NewsItem parseEntry(Element entry, NewsSource source) {
        try {
            // Título
            String title = cleanText(getFirstText(entry, "title", "media:title"));
            if (title.isEmpty()) return null;

            // Link
            String link = getFirstAttr(entry, "link", "href");
            if (link == null || link.isEmpty()) {
                link = cleanText(getFirstText(entry, "link"));
            }
            if (link == null || link.isEmpty()) {
                link = cleanText(getFirstText(entry, "guid"));
            }

            // Descrição
            String description = cleanText(getFirstText(entry,
                    "description", "summary", "content:encoded", "media:description"));

            // Imagem
            String imageUrl = extractImage(entry);

            // Data
            String pubDateStr = getFirstText(entry, "pubDate", "published", "updated", "dc:date");
            Date pubDate = parseDate(pubDateStr);

            return new NewsItem(title, link, description, imageUrl,
                    pubDate, source.getName(), source.getColor());

        } catch (Exception e) {
            ////////Log.e(TAG, "parseEntry error: " + e.getMessage());
            return null;
        }
    }

    private static String extractImage(Element entry) {
        // 1. media:content com url
        Element media = entry.selectFirst("[url]");
        if (media != null) {
            String url = media.attr("url");
            if (!url.isEmpty() && isImageUrl(url)) return url;
        }

        // 2. enclosure
        Element enclosure = entry.selectFirst("enclosure");
        if (enclosure != null) {
            String url = enclosure.attr("url");
            String type = enclosure.attr("type");
            if (!url.isEmpty() && (type.startsWith("image") || isImageUrl(url))) return url;
        }

        // 3. Primeira <img> dentro do HTML do description/content
        String[] contentTags = {"description", "content:encoded", "summary", "content"};
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

    private static boolean isImageUrl(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.contains("/image/") || lower.contains("/img/")
                || lower.contains("thumbnail");
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private static String getFirstText(Element parent, String... tags) {
        for (String tag : tags) {
            Element el = parent.selectFirst(tag);
            if (el != null) {
                String text = el.text();
                if (!text.isEmpty()) return text;
                String html = el.html();
                if (!html.isEmpty()) return html;
            }
        }
        return "";
    }

    private static String getFirstAttr(Element parent, String tag, String attr) {
        Element el = parent.selectFirst(tag);
        return el != null ? el.attr(attr) : null;
    }

    private static String cleanText(String text) {
        if (text == null || text.isEmpty()) return "";
        return Jsoup.parse(text).text().trim();
    }

    private static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String cleaned = dateStr.trim();
        for (String fmt : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setLenient(true);
                return sdf.parse(cleaned);
            } catch (ParseException ignored) {}
        }
        ////////Log.e(TAG, "Data não reconhecida: " + cleaned);
        return null;
    }

    /** Verifica se a notícia é do Fluminense (para EXCLUIR do feed de outras notícias) */
    private static boolean isFluminenseNews(NewsItem item) {
        String combined = "";
        if (item.getTitle()       != null) combined += item.getTitle().toLowerCase();
        if (item.getDescription() != null) combined += " " + item.getDescription().toLowerCase();
        for (String kw : FLUMINENSE_KEYWORDS) {
            if (combined.contains(kw)) return true;
        }
        return false;
    }

    /** Remove itens com o mesmo link OU mesmo fingerprint de título */
    private static List<NewsItem> deduplicate(List<NewsItem> items) {
        List<NewsItem> result = new ArrayList<>();
        java.util.Set<String> seenLinks = new java.util.HashSet<>();
        java.util.Set<String> seenFingerprints = new java.util.HashSet<>();
        for (NewsItem item : items) {
            String linkKey = item.getLink() != null ? item.getLink() : "";
            String titleKey = titleFingerprint(item.getTitle());
            if ((linkKey.isEmpty() || seenLinks.add(linkKey)) && seenFingerprints.add(titleKey)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Fingerprint agressivo: remove acentos, stop words, pega 5 palavras significativas.
     */
    private static String titleFingerprint(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
        String[] stopWords = {"o", "a", "os", "as", "de", "do", "da", "dos", "das",
                "em", "no", "na", "nos", "nas", "que", "e", "para", "por", "com",
                "um", "uma", "ao", "aos", "pelo", "pela", "se", "mas", "ou"};
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String w : words) {
            if (w.isEmpty()) continue;
            boolean isStop = false;
            for (String sw : stopWords) {
                if (w.equals(sw)) { isStop = true; break; }
            }
            if (!isStop) {
                sb.append(w).append(" ");
                count++;
                if (count >= 5) break;
            }
        }
        return sb.toString().trim();
    }
}
