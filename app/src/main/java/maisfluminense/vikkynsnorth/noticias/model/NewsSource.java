package maisfluminense.vikkynsnorth.noticias.model;

/**
 * NewsSource — define uma fonte de RSS com seus metadados.
 */
public class NewsSource {

    private final String name;
    private final String rssUrl;
    private final String color;   // cor hex para o chip (ex: "#C8102E")

    public NewsSource(String name, String rssUrl, String color) {
        this.name = name;
        this.rssUrl = rssUrl;
        this.color = color;
    }

    public String getName()   { return name; }
    public String getRssUrl() { return rssUrl; }
    public String getColor()  { return color; }
}
