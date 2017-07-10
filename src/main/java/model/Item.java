package model;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by kostyazxcvbn on 09.07.2017.
 */
public class Item{
    private Path path;
    private String imagePath;

    public Path getPath() {
        return path;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Item(Path path, String imagePath) {
        this.path = path;
        this.imagePath = imagePath;
    }

    public Item(Path path) {
        this.path = path;
        if(Files.isDirectory(path)){
            try {
                if(Files.isHidden(path)){
                    setImagePath("/img/itemDirHidden.png");
                }else{
                    setImagePath("/img/itemDirVisible.png");
                }
            } catch (IOException e) {
                setImagePath("/img/itemDirUnavaible.png");
            }
        }else{
            try {
                if(Files.isHidden(path)){
                    setImagePath("/img/itemFileHidden.png");
                }else{
                    setImagePath("/img/itemFileVisible.png");
                }
            } catch (IOException e) {
                setImagePath("/img/itemFileUnavaible.png");
            }
        }
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (this.hashCode()==obj.hashCode() && path.getParent().toString().equals(((Item)obj).getPath().getParent().toString()));
    }

    @Override
    public String toString() {
        try {
            return path.getFileName().toString();
        } catch (NullPointerException e) {
            return path.toAbsolutePath().toString();
        }
    }
}
