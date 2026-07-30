package maisfluminense.vikkynsnorth.noticias.api;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import maisfluminense.vikkynsnorth.noticias.model.NewsItem;

/**
 * NewsCache — cache em memória para notícias.
 *
 * Evita refetch desnecessário ao trocar de aba ou reabrir o app.
 * O cache expira após CACHE_TTL_MS (5 minutos).
 */
public class NewsCache {

    private static final String TAG = "NewsCache";

    /** Tempo de vida do cache: 5 minutos */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    /** Chaves de cache */
    public static final String KEY_NOTICIAS = "noticias";
    public static final String KEY_OUTRAS   = "outras-noticias";

    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final List<NewsItem> items;
        final long timestamp;

        CacheEntry(List<NewsItem> items) {
            this.items = items != null ? items : new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /** Retorna os itens em cache, ou null se não houver ou se expirou */
    public static List<NewsItem> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        ////////Log.e(TAG, "Cache HIT: " + key + " → " + entry.items.size() + " itens");
        return new ArrayList<>(entry.items);
    }

    /** Retorna os itens em cache mesmo se expirado (melhor que nada), ou null */
    public static List<NewsItem> getStale(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        ////////Log.e(TAG, "Cache STALE: " + key + " → " + entry.items.size() + " itens");
        return new ArrayList<>(entry.items);
    }

    /** Armazena itens no cache */
    public static void put(String key, List<NewsItem> items) {
        cache.put(key, new CacheEntry(items));
        ////////Log.e(TAG, "Cache PUT: " + key + " → " + (items != null ? items.size() : 0) + " itens");
    }

    /** Verifica se há cache válido (não expirado) */
    public static boolean hasValid(String key) {
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    /** Limpa todo o cache */
    public static void clear() {
        cache.clear();
    }
}
