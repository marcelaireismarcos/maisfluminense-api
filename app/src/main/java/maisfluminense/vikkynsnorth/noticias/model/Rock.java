package maisfluminense.vikkynsnorth.noticias.model;

/**
 * Created by ronnykibet on 11/14/17.
 */

public class Rock {

    private int image;
    private String name;
    private String name2;

    public Rock() {
    }

    public Rock(int image, String name, String name2) {
        this.image = image;
        this.name = name;
        this.name2 = name2;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }
}
