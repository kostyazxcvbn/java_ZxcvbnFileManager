package vcontroller;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Item;

import java.nio.file.Paths;
import java.util.HashSet;

/**
 * Created by kostyazxcvbn on 10.07.2017.
 */
public class TreeItemFactory {
    private static TreeItem<Item> root;

    public static TreeItem<Item> getTreeItem(Item item){
        return new TreeItem(item, new ImageView(new Image(TreeItemFactory.class.getResourceAsStream(item.getImagePath()))));//TODO image from imageMap
    }

    public static TreeItem<Item>getRoot(){
        if (root == null) {
            Item item = new Item(Paths.get("/root"), "/img/itemRoot.png"); //TODO imagePath
            root = new TreeItem(item, new ImageView(new Image(TreeItemFactory.class.getResourceAsStream(item.getImagePath()))));
        }
        return root;
    }
}
