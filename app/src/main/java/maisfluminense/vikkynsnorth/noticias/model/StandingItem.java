package maisfluminense.vikkynsnorth.noticias.model;

/** Item de classificação — usado pelo PlacarScraper */
public class StandingItem {
    public int    rank;
    public String teamName;
    public String logoUrl;
    public int    played;
    public int    wins;
    public int    draws;
    public int    losses;
    public int    points;
    public int    goalDiff;
    public boolean isFluminense;

    // Zona: 1=promoção, -1=rebaixamento, 0=meio
    public int zone() {
        if (rank <= 4)  return 1;
        if (rank >= 17) return -1;
        return 0;
    }
}
