package maisfluminense.vikkynsnorth.noticias.api;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * ApiClient — singleton Retrofit para a API-Football v3.
 *
 * Header obrigatório: x-rapidapi-key com sua API key.
 * Obtida em: https://www.api-football.com (plano Free: 100 req/dia)
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://v3.football.api-sports.io/";

    private static Retrofit instance;
    private static String cachedApiKey = "";

    public static synchronized Retrofit getInstance(String apiKey) {
        if (instance != null && cachedApiKey.equals(apiKey)) {
            return instance;
        }

        ////////Log.e(TAG, "Criando Retrofit. Key length=" + apiKey.length());
        cachedApiKey = apiKey;

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("x-rapidapi-key", apiKey)
                            .header("x-rapidapi-host", "v3.football.api-sports.io")
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    ////////Log.e(TAG, "→ " + request.url());
                    okhttp3.Response response = chain.proceed(request);
                    ////////Log.e(TAG, "← HTTP " + response.code() + " " + request.url());
                    return response;
                })
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        instance = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return instance;
    }

    public static synchronized void reset() {
        instance = null;
        cachedApiKey = "";
    }
}
