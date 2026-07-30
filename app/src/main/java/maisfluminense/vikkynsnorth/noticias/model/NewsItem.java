package maisfluminense.vikkynsnorth.noticias.model;

import java.util.Date;

/**
 * NewsItem — representa uma manchete de notícia agregada do RSS.
 */
public class NewsItem {

    private String title;
    private String link;
    private String description;
    private String imageUrl;
    private Date pubDate;
    private String sourceName;   // "Globo Esporte", "UOL", etc.
    private String sourceColor;  // cor hex da fonte para o chip

    public NewsItem() {}

    public NewsItem(String title, String link, String description,
                    String imageUrl, Date pubDate,
                    String sourceName, String sourceColor) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pubDate = pubDate;
        this.sourceName = sourceName;
        this.sourceColor = sourceColor;
    }

    public String getTitle()            { return title; }
    public String getLink()             { return link; }
    public String getDescription()      { return description; }
    public String getImageUrl()         { return imageUrl; }
    public Date   getPubDate()          { return pubDate; }
    public String getSourceName()       { return sourceName; }
    public String getSourceColor()      { return sourceColor; }

    public void setTitle(String v)      { this.title = v; }
    public void setLink(String v)       { this.link = v; }
    public void setDescription(String v){ this.description = v; }
    public void setImageUrl(String v)   { this.imageUrl = v; }
    public void setPubDate(Date v)      { this.pubDate = v; }
    public void setSourceName(String v) { this.sourceName = v; }
    public void setSourceColor(String v){ this.sourceColor = v; }
}
