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
    private static HashSet<TreeItem>items=new HashSet<>();
    private static TreeItem<Item> root;

    public static TreeItem<Item> getTreeItem(Item item){
        TreeItem<Item> tempTreeItem = new TreeItem(item, new ImageView(new Image(TreeItemFactory.class.getResourceAsStream(item.getImagePath()))));//TODO image from imageMap
        for (TreeItem treeItem : items) {
            if(treeItem.equals(tempTreeItem)){
                return treeItem;
            }
        }
        items.add(tempTreeItem);
        return tempTreeItem;
    }

    public static boolean removeFromCache(Item item){
        return items.remove(item);
    }

    public static TreeItem<Item>getRoot(){
        if (root == null) {
            Item item = new Item(Paths.get("/root"), "/img/itemRoot.png"); //TODO imagePath
            root = new TreeItem(item, new ImageView(new Image(TreeItemFactory.class.getResourceAsStream(item.getImagePath()))));
        }
        return root;
    }
}
