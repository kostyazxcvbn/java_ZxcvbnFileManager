package model;

import javafx.scene.image.Image;

import java.nio.file.Path;

/**
 * Created by kostyazxcvbn on 09.07.2017.
 */
public class Item{
    private Path path;
    private Image icon;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public Item(Path path) {
        this.path = path;
    }
}
