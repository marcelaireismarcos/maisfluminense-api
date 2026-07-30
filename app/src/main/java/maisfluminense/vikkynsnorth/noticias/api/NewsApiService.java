package maisfluminense.vikkynsnorth.noticias.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * NewsApiService — interface Retrofit para a API de notícias própria.
 *
 * Base URL: valor de R.string.api_noticias_url (configurado em strings.xml)
 * Exemplo:  https://maisflamengo-api.onrender.com
 */
public interface NewsApiService {

    /**
     * Busca notícias agregadas de todas as fontes.
     * GET /noticias?limit=50
     */
    @GET("noticias")
    Call<List<NewsApiItem>> getNoticias(
            @Query("limit") int limit
    );

    /**
     * Busca outras notícias (futebol geral, sem Náutico).
     * GET /outras-noticias?limit=50
     */
    @GET("outras-noticias")
    Call<List<NewsApiItem>> getOutrasNoticias(
            @Query("limit") int limit
    );

    // ─── Model da resposta ───────────────────────────────────────
    class NewsApiItem {
        @SerializedName("title")       public String title;
        @SerializedName("link")        public String link;
        @SerializedName("description") public String description;
        @SerializedName("image")       public String image;
        @SerializedName("date")        public String date;   // ISO 8601
        @SerializedName("source")      public String source;
        @SerializedName("color")       public String color;
    }
}
