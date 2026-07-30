package maisfluminense.vikkynsnorth.noticias.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.model.NewsItem;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * NewsApiClient — consome a API própria de notícias do servidor Node.js.
 */
public class NewsApiClient {

    private static final String TAG    = "NewsApiClient";
    private static final int    LIMIT  = 50;

    public interface NewsCallback {
        void onSuccess(List<NewsItem> items);
        void onError(String message);
    }

    // ─── Warm-up ─────────────────────────────────────────────────────
    /**
     * Ping ao servidor para "acordar" o Render antes do usuário chegar no feed.
     * Chamado no onStart() da Principal.
     */
    public static void warmUp(Context context) {
        String baseUrl = context.getString(R.string.api_noticias_url);
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(baseUrl + "health")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(@NonNull okhttp3.Call call,
                                   @NonNull okhttp3.Response response) {
                ////////Log.e(TAG, "Servidor acordado: HTTP " + response.code());
                response.close();
            }
            @Override
            public void onFailure(@NonNull okhttp3.Call call,
                                  @NonNull java.io.IOException e) {
                ////////Log.e(TAG, "Warm-up falhou: " + e.getMessage());
            }
        });
    }

    // ─── Notícias do Vitória-BA ───────────────────────────────────────
    public static void fetchNews(Context context, NewsCallback callback) {
        call(context, "noticias", NewsCache.KEY_NOTICIAS, callback);
    }

    // ─── Outras notícias (futebol geral) ─────────────────────────────
    public static void fetchOutrasNoticias(Context context, NewsCallback callback) {
        call(context, "outras-noticias", NewsCache.KEY_OUTRAS, callback);
    }

    /** Retorna notícias em cache (ou null se não houver) */
    public static List<NewsItem> getCachedNews() {
        return NewsCache.get(NewsCache.KEY_NOTICIAS);
    }

    /** Retorna outras notícias em cache (ou null se não houver) */
    public static List<NewsItem> getCachedOutras() {
        return NewsCache.get(NewsCache.KEY_OUTRAS);
    }

    // ─── Helper genérico ─────────────────────────────────────────────
    private static void call(Context context, String endpoint, String cacheKey, NewsCallback callback) {
        String baseUrl = context.getString(R.string.api_noticias_url);
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        ////////Log.e(TAG, "Buscando " + endpoint + " em: " + baseUrl);

        // Timeout reduzido: 8s para falhar rápido e cair pro RSS fallback
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NewsApiService service  = retrofit.create(NewsApiService.class);
        Call<List<NewsApiService.NewsApiItem>> apiCall =
                endpoint.equals("noticias")
                        ? service.getNoticias(LIMIT)
                        : service.getOutrasNoticias(LIMIT);

        apiCall.enqueue(new Callback<List<NewsApiService.NewsApiItem>>() {
            @Override
            public void onResponse(Call<List<NewsApiService.NewsApiItem>> call,
                                   Response<List<NewsApiService.NewsApiItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    ////////Log.e(TAG, endpoint + " HTTP " + response.code());
                    callback.onError("HTTP " + response.code());
                    return;
                }
                List<NewsItem> items = convertItems(response.body());
                ////////Log.e(TAG, endpoint + " retornou " + items.size() + " notícias");
                // Cache será preenchido pelo showResults() após deduplicação
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Call<List<NewsApiService.NewsApiItem>> call, Throwable t) {
                ////////Log.e(TAG, endpoint + " falhou: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    // ─── Conversão JSON → NewsItem ────────────────────────────────────
    private static List<NewsItem> convertItems(List<NewsApiService.NewsApiItem> raw) {
        List<NewsItem> result = new ArrayList<>();
        for (NewsApiService.NewsApiItem r : raw) {
            if (r.title == null || r.title.isEmpty()) continue;
            result.add(new NewsItem(
                    r.title,
                    r.link,
                    r.description,
                    r.image,
                    parseDate(r.date),
                    r.source != null ? r.source : "Futebol",
                    r.color  != null ? r.color  : "#C8102E"
            ));
        }
        return result;
    }

    private static Date parseDate(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf.parse(iso);
            } catch (ParseException ignored) {}
        }
        return null;
    }
}
