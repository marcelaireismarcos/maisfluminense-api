package maisfluminense.vikkynsnorth.noticias.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * StandingsResponse — mapeamento da resposta JSON de /standings da API-Football.
 *
 * Estrutura resumida da resposta:
 * {
 *   "response": [{
 *     "league": {
 *       "standings": [[ { rank, team, points, goalsDiff, all:{played,win,draw,lose} } ]]
 *     }
 *   }]
 * }
 */
public class StandingsResponse {

    @SerializedName("response")
    public List<ResponseItem> response;

    public static class ResponseItem {
        @SerializedName("league")
        public LeagueData league;
    }

    public static class LeagueData {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("season")
        public int season;

        @SerializedName("standings")
        public List<List<StandingEntry>> standings;
    }

    public static class StandingEntry {

        @SerializedName("rank")
        public int rank;

        @SerializedName("team")
        public TeamInfo team;

        @SerializedName("points")
        public int points;

        @SerializedName("goalsDiff")
        public int goalsDiff;

        @SerializedName("form")
        public String form; // ex: "WWDLL"

        @SerializedName("status")
        public String status;

        @SerializedName("description")
        public String description; // "Promotion - Serie A"

        @SerializedName("all")
        public Stats all;
    }

    public static class TeamInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("logo")
        public String logo;
    }

    public static class Stats {
        @SerializedName("played")
        public int played;

        @SerializedName("win")
        public int win;

        @SerializedName("draw")
        public int draw;

        @SerializedName("lose")
        public int lose;

        @SerializedName("goals")
        public Goals goals;
    }

    public static class Goals {
        @SerializedName("for")
        public int goalsFor;

        @SerializedName("against")
        public int goalsAgainst;
    }
}
