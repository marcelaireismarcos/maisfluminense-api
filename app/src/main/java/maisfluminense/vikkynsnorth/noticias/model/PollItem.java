package maisfluminense.vikkynsnorth.noticias.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * PollItem — representa uma enquete da torcida.
 */
public class PollItem {

    @SerializedName("id")
    private int id;

    @SerializedName("question")
    private String question;

    @SerializedName("options")
    private List<Option> options;

    @SerializedName("totalVotes")
    private int totalVotes;

    @SerializedName("createdAt")
    private String createdAt;

    public int getId() { return id; }
    public String getQuestion() { return question; }
    public List<Option> getOptions() { return options; }
    public int getTotalVotes() { return totalVotes; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int v) { this.id = v; }
    public void setQuestion(String v) { this.question = v; }
    public void setOptions(List<Option> v) { this.options = v; }
    public void setTotalVotes(int v) { this.totalVotes = v; }

    /**
     * Option — uma opção de voto dentro da enquete.
     */
    public static class Option {

        @SerializedName("id")
        private String id;

        @SerializedName("text")
        private String text;

        @SerializedName("votes")
        private int votes;

        @SerializedName("pct")
        private int pct; // 0-100

        public String getId() { return id; }
        public String getText() { return text; }
        public int getVotes() { return votes; }
        public int getPct() { return pct; }

        public void setId(String v) { this.id = v; }
        public void setText(String v) { this.text = v; }
        public void setVotes(int v) { this.votes = v; }
        public void setPct(int v) { this.pct = v; }
    }

    /**
     * VoteRequest — corpo da requisição POST /enquetes/votar
     */
    public static class VoteRequest {
        private int pollId;
        private String optionId;

        public VoteRequest(int pollId, String optionId) {
            this.pollId = pollId;
            this.optionId = optionId;
        }
    }

    /**
     * VoteResponse — resposta do POST /enquetes/votar
     */
    public static class VoteResponse {
        @SerializedName("success")
        private boolean success;

        @SerializedName("message")
        private String message;

        @SerializedName("poll")
        private PollItem poll;

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public PollItem getPoll() { return poll; }
    }
}
