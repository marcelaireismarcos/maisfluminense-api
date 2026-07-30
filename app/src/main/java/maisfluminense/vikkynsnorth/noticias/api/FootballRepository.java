package maisfluminense.vikkynsnorth.noticias.api;

import android.content.Context;
import android.util.Log;

import java.util.List;

import maisfluminense.vikkynsnorth.noticias.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FootballRepository — chamadas à API-Football v3.
 *
 * IDs:
 *   Fluminense = 124
 *   Série A = 71
 *   Temporada = 2026
 */
public class FootballRepository {

    private static final String TAG = "FootballRepo";

    public static final int LEAGUE_SERIE_A = 71;
    public static final int TEAM_FLUMINENSE  = 124;
    public static final int SEASON         = 2026;
    public static final int NEXT_GAMES     = 5;
    public static final int LAST_GAMES     = 5;

    public interface StandingsCallback {
        void onSuccess(List<StandingsResponse.StandingEntry> standings);
        void onError(String message);
    }

    public interface FixturesCallback {
        void onSuccess(List<FixturesResponse.FixtureItem> fixtures);
        void onError(String message);
    }

    // ─── Standings ───────────────────────────────────────────────────
    public static void getStandings(Context context, StandingsCallback callback) {
        FootballApiService service = getService(context);
        service.getStandings(LEAGUE_SERIE_A, SEASON)
                .enqueue(new Callback<StandingsResponse>() {
                    @Override
                    public void onResponse(Call<StandingsResponse> call,
                                           Response<StandingsResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            String err = "HTTP " + response.code();
                            try { err += " — " + response.errorBody().string(); }
                            catch (Exception ignored) {}
                            callback.onError(err);
                            return;
                        }
                        try {
                            if (response.body().response == null
                                    || response.body().response.isEmpty()
                                    || response.body().response.get(0).league == null
                                    || response.body().response.get(0).league.standings == null
                                    || response.body().response.get(0).league.standings.isEmpty()) {
                                callback.onError("Sem dados para a temporada " + SEASON);
                                return;
                            }
                            List<StandingsResponse.StandingEntry> entries =
                                    response.body().response.get(0).league.standings.get(0);
                            callback.onSuccess(entries);
                        } catch (Exception e) {
                            callback.onError("Erro ao processar classificação: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<StandingsResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    // ─── Next Fixtures ───────────────────────────────────────────────
    public static void getNextFixtures(Context context, FixturesCallback callback) {
        FootballApiService service = getService(context);
        service.getNextFixtures(TEAM_FLUMINENSE, NEXT_GAMES)
                .enqueue(new Callback<FixturesResponse>() {
                    @Override
                    public void onResponse(Call<FixturesResponse> call,
                                           Response<FixturesResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            String err = "HTTP " + response.code();
                            try { err += " — " + response.errorBody().string(); }
                            catch (Exception ignored) {}
                            callback.onError(err);
                            return;
                        }
                        List<FixturesResponse.FixtureItem> fixtures = response.body().response;
                        callback.onSuccess(fixtures != null ? fixtures : new java.util.ArrayList<>());
                    }

                    @Override
                    public void onFailure(Call<FixturesResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    // ─── Last Fixtures ───────────────────────────────────────────────
    public static void getLastFixtures(Context context, FixturesCallback callback) {
        FootballApiService service = getService(context);
        service.getLastFixtures(TEAM_FLUMINENSE, LAST_GAMES)
                .enqueue(new Callback<FixturesResponse>() {
                    @Override
                    public void onResponse(Call<FixturesResponse> call,
                                           Response<FixturesResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            String err = "HTTP " + response.code();
                            try { err += " — " + response.errorBody().string(); }
                            catch (Exception ignored) {}
                            callback.onError(err);
                            return;
                        }
                        List<FixturesResponse.FixtureItem> fixtures = response.body().response;
                        callback.onSuccess(fixtures != null ? fixtures : new java.util.ArrayList<>());
                    }

                    @Override
                    public void onFailure(Call<FixturesResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    // ─── Helper ──────────────────────────────────────────────────────
    private static FootballApiService getService(Context context) {
        String apiKey = context.getString(R.string.api_football_key);
        return ApiClient.getInstance(apiKey).create(FootballApiService.class);
    }

    // ─── Debug: temporadas disponíveis ───────────────────────────────
    public static void debugGetSeasons(Context context) {
        Log.d(TAG, "=== Buscando temporadas da Série A ===");
        FootballApiService service = getService(context);
        service.getLeagueSeasons(LEAGUE_SERIE_A)
                .enqueue(new retrofit2.Callback<LeagueSeasonsResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<LeagueSeasonsResponse> call,
                                           retrofit2.Response<LeagueSeasonsResponse> response) {
                        Log.d(TAG, "Seasons HTTP " + response.code());
                        if (response.isSuccessful() && response.body() != null
                                && response.body().response != null) {
                            for (LeagueSeasonsResponse.LeagueItem li : response.body().response) {
                                if (li.seasons != null) {
                                    for (LeagueSeasonsResponse.Season s : li.seasons) {
                                        Log.d(TAG, "Temporada → year=" + s.year
                                                + " current=" + s.current
                                                + " standings=" + (s.coverage != null && s.coverage.standings));
                                    }
                                }
                            }
                        } else {
                            try {
                                ////////Log.e(TAG, "Seasons error: " + (response.errorBody() != null ? response.errorBody().string() : "null"));
                            } catch (Exception e) {
                                ////////Log.e(TAG, "Seasons error parse: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<LeagueSeasonsResponse> call, Throwable t) {
                        ////////Log.e(TAG, "Seasons failure: " + t.getMessage());
                    }
                });
    }

    // ─── Debug: buscar ID do Fluminense ───────────────────────────────
    public static void debugFindFluminenseId(Context context) {
        Log.d(TAG, "=== Buscando ID do Fluminense ===");
        FootballApiService service = getService(context);
        service.searchTeam("Fluminense", "Brazil")
                .enqueue(new retrofit2.Callback<TeamSearchResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<TeamSearchResponse> c,
                                           retrofit2.Response<TeamSearchResponse> response) {
                        Log.d(TAG, "TeamSearch HTTP " + response.code());
                        if (response.isSuccessful() && response.body() != null
                                && response.body().response != null) {
                            for (TeamSearchResponse.TeamItem t : response.body().response) {
                                Log.d(TAG, "Time → id=" + t.team.id
                                        + " name=" + t.team.name
                                        + " country=" + t.team.country);
                            }
                        } else {
                            try {
                                ////////Log.e(TAG, "Erro: " + (response.errorBody() != null ? response.errorBody().string() : "null"));
                            } catch (Exception e) {
                                ////////Log.e(TAG, "Erro parse: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<TeamSearchResponse> c, Throwable t) {
                        ////////Log.e(TAG, "TeamSearch failure: " + t.getMessage());
                    }
                });
    }
}
