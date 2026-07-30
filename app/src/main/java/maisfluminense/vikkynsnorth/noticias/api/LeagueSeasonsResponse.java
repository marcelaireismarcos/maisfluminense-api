package maisfluminense.vikkynsnorth.noticias.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LeagueSeasonsResponse {

    @SerializedName("response")
    public List<LeagueItem> response;

    public static class LeagueItem {
        @SerializedName("league")
        public LeagueInfo league;

        @SerializedName("seasons")
        public List<Season> seasons;
    }

    public static class LeagueInfo {
        @SerializedName("id")   public int id;
        @SerializedName("name") public String name;
    }

    public static class Season {
        @SerializedName("year")    public int year;
        @SerializedName("start")   public String start;
        @SerializedName("end")     public String end;
        @SerializedName("current") public boolean current;
        @SerializedName("coverage") public Coverage coverage;
    }

    public static class Coverage {
        @SerializedName("standings") public boolean standings;
        @SerializedName("fixtures")  public Fixtures fixtures;
    }

    public static class Fixtures {
        @SerializedName("events")       public boolean events;
        @SerializedName("lineups")      public boolean lineups;
        @SerializedName("statistics_fixtures") public boolean statisticsFixtures;
        @SerializedName("statistics_players")  public boolean statisticsPlayers;
    }
}
