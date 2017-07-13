package vcontroller;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Item;

import java.nio.file.Paths;

/**
 * Created by kostyazxcvbn on 10.07.2017.
 */
public final class ItemViewFactory {

    private static FXOptimizedItem root;

    private static Image directoryUnavaible;
    private static Image  directoryHidden;
    private static Image  directoryCutted;
    private static Image directoryNormal;
    private static Image fileUnavaible;
    private static Image fileHidden;
    private static Image fileNormal;
    private static Image fileCutted;
    private static Image itemWaiting;
    private static Image itemRoot;
    private static Image itemDrive;

    public static class FXOptimizedItem extends TreeItem<Item>{

        public FXOptimizedItem(Item value, Node graphic) {
            super(value, graphic);
        }

        public FXOptimizedItem(Item value) {
            super(value, getItemImageView(value));
        }

        public String getCreatedDate() {
            return getValue().getCreatedDate();
        }

        public boolean isDirectory(){
            return getValue().isDirectory();
        }

        public Item getItem() {
            return getValue();
        }



        public String getName() {
            return getValue().getName();
        }

        public String getType() {
            return getValue().getType();
        }

        public String getSize() {
            return getValue().getSize();
        }

        public String getLastModifiedDate() {
            return getValue().getLastModifiedDate();
        }

        public ImageView getIcon() {
            return (ImageView)getGraphic();
        }
    }

    static{
        try {

            directoryUnavaible=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirUnavaible.png")));
        } catch (NullPointerException e) {
            directoryUnavaible=null;
        }
        try {
            directoryHidden=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirHidden.png")));
        } catch (Exception e) {
            directoryHidden=null;
        }
        try {
            directoryCutted=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirCutted.png")));
        } catch (Exception e) {
            directoryCutted=null;
        }
        try {
            directoryNormal=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDirNormal.png")));
        } catch (Exception e) {
            directoryNormal=null;
        }
        try {
            fileUnavaible=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileUnavaible.png")));
        } catch (Exception e) {
            fileUnavaible=null;
        }
        try {
            fileHidden=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileHidden.png")));
        } catch (Exception e) {
            fileHidden=null;
        }
        try {
            fileNormal=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileNormal.png")));
        } catch (Exception e) {
            fileNormal=null;
        }
        try {
            fileCutted=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemFileCutted.png")));
        } catch (Exception e) {
            fileCutted=null;
        }
        try {
            itemWaiting=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemWaiting.png")));
        } catch (Exception e) {
            itemWaiting=null;
        }
        try {
            itemRoot=new Image((ItemViewFactory.class.getResourceAsStream("/img/itemRoot.png")));
        } catch (Exception e) {
            itemRoot=null;
        }
        try {
            itemDrive =new Image((ItemViewFactory.class.getResourceAsStream("/img/itemDrive.png")));
        } catch (Exception e) {
            itemDrive =null;
        }
    }

    public static TreeItem<Item> getTreeItem(Item item){
        return new TreeItem(item, getItemImageView(item));//TODO image from imageMap
    }

    public static FXOptimizedItem getRoot(){
        if (root == null) {
            root = new FXOptimizedItem(new Item(Paths.get("/root")), new ImageView(itemRoot));// TODO imagePath
        }
        return root;
    }

    public static ImageView getItemImageView(Item item){

        try {
            if (!item.isRootStorage()) {
                if(!item.isAvailable()){
                    return(item.isDirectory())?new ImageView(directoryUnavaible):new ImageView(fileUnavaible);
                }
                if(item.isHidden()){
                    return(item.isDirectory())?new ImageView(directoryHidden):new ImageView(fileHidden);
                }
                if(!item.isHidden()) {
                    return (item.isDirectory()) ? new ImageView(directoryNormal):new ImageView(fileNormal);

                }
            }else{
                return new ImageView(itemDrive);
            }
        } catch (Exception e) {
            return new ImageView();
        }


        return new ImageView();
    }

    public static ImageView getItemWaiting() {
        return new ImageView(itemWaiting);
    }

    public static ImageView getDirectoryUnavaible() {
        return new ImageView(directoryUnavaible);
    }

    public static FXOptimizedItem getNewfxOptimizedItem(Item item){
        return new FXOptimizedItem(item, getItemImageView(item));
    }
}
