package maisfluminense.vikkynsnorth.noticias.model;

/** Item de jogo — usado pelo PlacarScraper */
public class FixtureItem {
    public String homeName;
    public String awayName;
    public String homeLogo;
    public String awayLogo;
    public String scoreText;  // ex: "2 x 1" ou null se não iniciado
    public String dateText;   // ex: "15/07 21:00"
    public String round;      // ex: "Rodada 10"
    public boolean isPast;    // true = jogo passado, false = futuro
}
