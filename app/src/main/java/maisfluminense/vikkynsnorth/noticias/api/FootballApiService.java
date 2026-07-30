package maisfluminense.vikkynsnorth.noticias.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * FootballApiService — endpoints da API-Football v3.
 *
 * IDs:
 *   Série A = 71
 *   Fluminense = 124
 *   Temporada = 2026
 */
public interface FootballApiService {

    @GET("standings")
    Call<StandingsResponse> getStandings(
            @Query("league") int leagueId,
            @Query("season") int season
    );

    @GET("fixtures")
    Call<FixturesResponse> getNextFixtures(
            @Query("team") int teamId,
            @Query("next") int count
    );

    @GET("fixtures")
    Call<FixturesResponse> getLastFixtures(
            @Query("team") int teamId,
            @Query("last") int count
    );

    /** Busca time por nome — para descobrir o team ID correto */
    @GET("teams")
    Call<TeamSearchResponse> searchTeam(
            @Query("name") String name,
            @Query("country") String country
    );

    /** Busca temporadas disponíveis de uma liga */
    @GET("leagues")
    Call<LeagueSeasonsResponse> getLeagueSeasons(
            @Query("id") int leagueId
    );
}
