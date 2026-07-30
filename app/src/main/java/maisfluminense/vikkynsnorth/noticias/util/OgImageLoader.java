package maisfluminense.vikkynsnorth.noticias.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OgImageLoader — busca og:image de artigos em background, com cache.
 *
 * Usado pelo NewsAdapter para carregar imagens progressivamente:
 * 1. O card aparece sem imagem (instantâneo)
 * 2. Em background, busca og:image da URL do artigo
 * 3. Quando encontra, carrega via callback → Glide
 *
 * Cache em memória evita re-fetch ao rolar a lista.
 */
public class OgImageLoader {

    private static final String TAG = "OgImageLoader";

    /** Cache: articleUrl → ogImageUrl (ou "" se não tem) */
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(String imageUrl);
    }

    /**
     * Busca og:image para a URL do artigo.
     * Retorna imediatamente do cache, ou busca em background.
     */
    public static void load(String articleUrl, Callback callback) {
        if (articleUrl == null || articleUrl.isEmpty()) {
            callback.onResult(null);
            return;
        }

        // Cache hit?
        String cached = cache.get(articleUrl);
        if (cached != null) {
            callback.onResult(cached.isEmpty() ? null : cached);
            return;
        }

        // Busca em background
        executor.execute(() -> {
            String ogImage = fetchOgImage(articleUrl);
            // Cache: guarda "" se não encontrou (evita re-fetch)
            cache.put(articleUrl, ogImage != null ? ogImage : "");

            String result = ogImage;
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /** Verifica se a URL já tem cache (mesmo que seja "sem imagem") */
    public static boolean isCached(String articleUrl) {
        return articleUrl != null && cache.containsKey(articleUrl);
    }

    /** Busca og:image na página do artigo (segue redirects do Google News) */
    private static String fetchOgImage(String articleUrl) {
        try {
            Document doc = Jsoup.connect(articleUrl)
                    .userAgent("Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html")
                    .header("Accept-Language", "pt-BR,pt;q=0.9")
                    .timeout(5_000)
                    .followRedirects(true)
                    .get();

            // og:image
            Element og = doc.selectFirst("meta[property=og:image]");
            if (og != null) {
                String url = og.attr("content");
                if (isValidImageUrl(url)) return url;
            }

            // twitter:image
            Element tw = doc.selectFirst("meta[name=twitter:image]");
            if (tw != null) {
                String url = tw.attr("content");
                if (isValidImageUrl(url)) return url;
            }

            // Primeira <img> no <article> ou com src http
            Element img = doc.selectFirst("article img[src], main img[src], .content img[src]");
            if (img != null) {
                String src = img.attr("src");
                if (isValidImageUrl(src)) return src;
            }

        } catch (Exception e) {
            ////////Log.e(TAG, "fetchOgImage erro: " + e.getMessage());
        }
        return null;
    }

    private static boolean isValidImageUrl(String url) {
        return url != null && !url.isEmpty()
                && !url.startsWith("data:")
                && url.startsWith("http")
                && !url.contains("1x1")
                && !url.contains("pixel.gif")
                && !url.contains("spacer.gif");
    }

    /** Limpa o cache (útil para forçar refresh) */
    public static void clearCache() {
        cache.clear();
    }
}
