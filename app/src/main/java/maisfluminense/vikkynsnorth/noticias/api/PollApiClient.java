package maisfluminense.vikkynsnorth.noticias.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import maisfluminense.vikkynsnorth.noticias.R;
import maisfluminense.vikkynsnorth.noticias.model.PollItem;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * PollApiClient — consome as enquetes da torcida do servidor Node.js.
 */
public class PollApiClient {

    private static final String TAG = "PollApiClient";

    public interface PollCallback {
        void onSuccess(PollItem poll);
        void onError(String message);
    }

    public interface VoteCallback {
        void onSuccess(PollItem updatedPoll);
        void onError(String message);
    }

    // ─── Retrofit interface ────────────────────────────────────────
    private interface PollService {
        @GET("enquetes/ativa")
        Call<PollItem> getActivePoll();

        @POST("enquetes/votar")
        Call<PollItem.VoteResponse> vote(@Body PollItem.VoteRequest request);

        @POST("enquetes/restaurar-voto")
        Call<PollItem.VoteResponse> restoreVote(@Body PollItem.VoteRequest request);
    }

    // ─── Buscar enquete ativa ──────────────────────────────────────
    public static void fetchActivePoll(Context context, PollCallback callback) {
        PollService service = getService(context);
        service.getActivePoll().enqueue(new Callback<PollItem>() {
            @Override
            public void onResponse(Call<PollItem> call, Response<PollItem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 404) {
                    // 404 = nenhuma enquete ativa — não é erro, só não tem
                    callback.onError("no_active_poll");
                } else {
                    callback.onError("HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PollItem> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // ─── Votar ─────────────────────────────────────────────────────
    public static void vote(Context context, int pollId, String optionId,
                            VoteCallback callback) {
        PollService service = getService(context);
        PollItem.VoteRequest request = new PollItem.VoteRequest(pollId, optionId);
        service.vote(request).enqueue(new Callback<PollItem.VoteResponse>() {
            @Override
            public void onResponse(Call<PollItem.VoteResponse> call,
                                   Response<PollItem.VoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PollItem.VoteResponse voteRes = response.body();
                    if (voteRes.isSuccess() && voteRes.getPoll() != null) {
                        callback.onSuccess(voteRes.getPoll());
                    } else {
                        callback.onError(voteRes.getMessage() != null
                                ? voteRes.getMessage() : "Erro ao registrar voto.");
                    }
                } else {
                    callback.onError("HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PollItem.VoteResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // ─── Restaurar voto (após restart do servidor) ─────────────────
    /**
     * Reenvia o voto do usuário após o servidor reiniciar e perder os dados.
     * Só incrementa se o total de votos for muito baixo (sinal de restart).
     */
    public static void restoreVote(Context context, int pollId, String optionId,
                                   VoteCallback callback) {
        PollService service = getService(context);
        PollItem.VoteRequest request = new PollItem.VoteRequest(pollId, optionId);
        service.restoreVote(request).enqueue(new Callback<PollItem.VoteResponse>() {
            @Override
            public void onResponse(Call<PollItem.VoteResponse> call,
                                   Response<PollItem.VoteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PollItem.VoteResponse voteRes = response.body();
                    if (voteRes.isSuccess() && voteRes.getPoll() != null) {
                        callback.onSuccess(voteRes.getPoll());
                    } else {
                        callback.onError(voteRes.getMessage() != null
                                ? voteRes.getMessage() : "Erro ao restaurar voto.");
                    }
                } else {
                    callback.onError("HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PollItem.VoteResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // ─── Helper ────────────────────────────────────────────────────
    private static PollService getService(Context context) {
        String baseUrl = context.getString(R.string.api_noticias_url);
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(PollService.class);
    }
}
