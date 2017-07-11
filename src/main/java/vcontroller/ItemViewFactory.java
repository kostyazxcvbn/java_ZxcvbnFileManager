package vcontroller;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Item;

import java.nio.file.Paths;

/**
 * Created by kostyazxcvbn on 10.07.2017.
 */
public class ItemViewFactory {
    private static TreeItem<Item> root;
    private static ImageView directoryUnavaible;
    private static ImageView directoryHidden;
    private static ImageView directoryCutted;
    private static ImageView directoryNormal;
    private static ImageView fileUnavaible;
    private static ImageView fileHidden;
    private static ImageView fileNormal;
    private static ImageView fileCutted;
    private static ImageView itemWaiting;
    private static ImageView itemNoImage;
    private static ImageView itemRoot;

    public static ImageView getDirectoryUnavaible() {
        return directoryUnavaible;
    }

    public static ImageView getDirectoryHidden() {
        return directoryHidden;
    }

    public static ImageView getDirectoryCutted() {
        return directoryCutted;
    }

    public static ImageView getDirectoryNormal() {
        return directoryNormal;
    }

    public static ImageView getFileUnavaible() {
        return fileUnavaible;
    }

    public static ImageView getFileHidden() {
        return fileHidden;
    }

    public static ImageView getFileNormal() {
        return fileNormal;
    }

    public static ImageView getFileCutted() {
        return fileCutted;
    }

    public static ImageView getItemWaiting() {
        return itemWaiting;
    }

    public static ImageView getItemNoImage() {
        return itemNoImage;
    }

    public static ImageView getItemRoot() {
        return itemRoot;
    }

    static{
        directoryUnavaible=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirUnavaible"))));
        directoryHidden=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirHidden"))));
        directoryCutted=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirCutted"))));
        directoryNormal=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirNormal"))));
        fileUnavaible=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileUnavaible"))));
        fileHidden=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileHidden"))));
        fileNormal=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileNormal"))));
        fileCutted=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileCutted"))));
        itemWaiting=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemWaiting"))));
        itemNoImage=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemNoImage"))));
        itemRoot=new ImageView(new Image((ItemViewFactory.class.getResourceAsStream("/img/itemRoot.png"))));
    }

    public static TreeItem<Item> getTreeItem(Item item){
        return new TreeItem(item, getFileImageView(item));//TODO image from imageMap
    }

    public static TreeItem<Item>getRoot(){
        if (root == null) {
            root = new TreeItem(new Item(Paths.get("/root")), itemRoot);// TODO imagePath
        }
        return root;
    }

    private static ImageView getFileImageView(Item item){
        if(!item.isAvailable()){
            return(item.isDirectory())?directoryUnavaible:fileUnavaible;
        }
        if(item.isHidden()){
            return(item.isDirectory())?directoryHidden:fileHidden;
        }
        if(!item.isHidden()) {
            return (item.isDirectory()) ? directoryNormal : fileNormal;

        }
        return itemNoImage;
    }
}
