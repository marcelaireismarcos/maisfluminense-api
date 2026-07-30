package maisfluminense.vikkynsnorth.noticias.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Model de resposta do endpoint /teams — usado para descobrir o team ID */
public class TeamSearchResponse {

    @SerializedName("response")
    public List<TeamItem> response;

    public static class TeamItem {
        @SerializedName("team")
        public TeamInfo team;
    }

    public static class TeamInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("country")
        public String country;

        @SerializedName("logo")
        public String logo;
    }
}
