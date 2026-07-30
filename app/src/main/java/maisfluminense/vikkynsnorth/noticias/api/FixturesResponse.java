package maisfluminense.vikkynsnorth.noticias.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * FixturesResponse — mapeamento da resposta JSON de /fixtures da API-Football.
 */
public class FixturesResponse {

    @SerializedName("response")
    public List<FixtureItem> response;

    public static class FixtureItem {

        @SerializedName("fixture")
        public FixtureInfo fixture;

        @SerializedName("league")
        public LeagueInfo league;

        @SerializedName("teams")
        public TeamsInfo teams;

        @SerializedName("goals")
        public GoalsInfo goals;

        @SerializedName("score")
        public ScoreInfo score;
    }

    public static class FixtureInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("date")
        public String date; // ISO 8601: "2025-07-15T21:00:00+00:00"

        @SerializedName("timestamp")
        public long timestamp;

        @SerializedName("venue")
        public VenueInfo venue;

        @SerializedName("status")
        public StatusInfo status;
    }

    public static class VenueInfo {
        @SerializedName("name")
        public String name;

        @SerializedName("city")
        public String city;
    }

    public static class StatusInfo {
        @SerializedName("short")
        public String shortStatus; // "NS" = não iniciado, "FT" = encerrado, "1H", "2H"

        @SerializedName("long")
        public String longStatus;

        @SerializedName("elapsed")
        public Integer elapsed; // minutos decorridos (null se não iniciado)
    }

    public static class LeagueInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("round")
        public String round; // "Regular Season - 10"
    }

    public static class TeamsInfo {
        @SerializedName("home")
        public TeamDetail home;

        @SerializedName("away")
        public TeamDetail away;
    }

    public static class TeamDetail {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("logo")
        public String logo;

        @SerializedName("winner")
        public Boolean winner; // true/false/null
    }

    public static class GoalsInfo {
        @SerializedName("home")
        public Integer home; // null antes de iniciar

        @SerializedName("away")
        public Integer away;
    }

    public static class ScoreInfo {
        @SerializedName("halftime")
        public GoalsInfo halftime;

        @SerializedName("fulltime")
        public GoalsInfo fulltime;
    }
}
