package helpers;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.FileManagerImpl;
import model.Item;

import java.nio.file.Paths;

/**
 * Created by kostyazxcvbn on 10.07.2017.
 */
public final class FileManagerItemsFactory {

    private static FXOptimizedItem root;

    private static Image directoryUnavaible;
    private static Image directoryHidden;
    private static Image directoryCutted;
    private static Image directoryNormal;
    private static Image fileUnavaible;
    private static Image fileHidden;
    private static Image fileNormal;
    private static Image fileCutted;
    private static Image itemWaiting;
    private static Image itemRoot;
    private static Image itemDrive;

    private static FileManagerImpl fileManager = FileManagerImpl.getInstance();

    public static class FXOptimizedItem extends TreeItem<Item>{

        private SimpleObjectProperty<ImageView> icon;
        private SimpleStringProperty name;
        private SimpleStringProperty type;
        private SimpleStringProperty createdDate;
        private SimpleStringProperty modifiedDate;
        private SimpleStringProperty size;
        private SimpleStringProperty attributes;

        public FXOptimizedItem(Item value) {
            super(value, getItemImageView(value));
            icon=new SimpleObjectProperty<ImageView>(getItemImageView(value));
            name=new SimpleStringProperty(getValue().getName());
            type=new SimpleStringProperty(getValue().getType());
            createdDate=new SimpleStringProperty(getValue().getCreatedDate());
            modifiedDate=new SimpleStringProperty(getValue().getLastModifiedDate());
            size=new SimpleStringProperty(getValue().getSize());
            attributes=new SimpleStringProperty(getValue().getAttributes());
        }

        public ImageView getIcon() {
            return icon.get();
        }

        public SimpleObjectProperty<ImageView>iconProperty() {
            return icon;
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public String getType() {
            return type.get();
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public String getCreatedDate() {
            return createdDate.get();
        }

        public SimpleStringProperty createdDateProperty() {
            return createdDate;
        }

        public String getModifiedDate() {
            return modifiedDate.get();
        }

        public SimpleStringProperty modifiedDateProperty() {
            return modifiedDate;
        }

        public String getSize() {
            return size.get();
        }

        public SimpleStringProperty sizeProperty() {
            return size;
        }

        public String getAttributes() {
            return attributes.get();
        }

        public SimpleStringProperty attributesProperty() {
            return attributes;
        }

        public boolean isDirectory(){
            return getValue().isDirectory();
        }

        public Item getItem() {
            return getValue();
        }

        public void setIcon(ImageView itemIcon) {
            icon.set(itemIcon);
            Platform.runLater(()->this.setGraphic(itemIcon));
        }

        public boolean isHidden() {
            return getValue().isHidden();
        }

        public FXOptimizedItem getParentItem() {
            if (isRootStorage()){
                return getRoot();
            }
            return new FXOptimizedItem(fileManager.getParentItem(this.getItem()));
        }

        private boolean isRootStorage() {
            return getValue().isRootStorage();
        }
    }

    static{
        try {
            directoryUnavaible=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemDirUnavaible.png")));
        } catch (NullPointerException e) {
            directoryUnavaible=null;
        }
        try {
            directoryHidden=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemDirHidden.png")));
        } catch (Exception e) {
            directoryHidden=null;
        }
        try {
            directoryCutted=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemDirCutted.png")));
        } catch (Exception e) {
            directoryCutted=null;
        }
        try {
            directoryNormal=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemDirNormal.png")));
        } catch (Exception e) {
            directoryNormal=null;
        }
        try {
            fileUnavaible=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemFileUnavaible.png")));
        } catch (Exception e) {
            fileUnavaible=null;
        }
        try {
            fileHidden=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemFileHidden.png")));
        } catch (Exception e) {
            fileHidden=null;
        }
        try {
            fileNormal=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemFileNormal.png")));
        } catch (Exception e) {
            fileNormal=null;
        }
        try {
            fileCutted=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemFileCutted.png")));
        } catch (Exception e) {
            fileCutted=null;
        }
        try {
            itemWaiting=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemWaiting.png")));
        } catch (Exception e) {
            itemWaiting=null;
        }
        try {
            itemRoot=new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemRoot.png")));
        } catch (Exception e) {
            itemRoot=null;
        }
        try {
            itemDrive =new Image((FileManagerItemsFactory.class.getResourceAsStream("/img/itemDrive.png")));
        } catch (Exception e) {
            itemDrive =null;
        }
    }

    public static FXOptimizedItem getRoot(){
        if (root == null) {
            root = new FXOptimizedItem(new Item(Paths.get("/root")));// TODO imagePath
        }
        return root;
    }

    public static ImageView getItemImageView(Item item){

        try {
            if (item.isRoot()) {
                return new ImageView(itemRoot);
            }

            if (!item.isRootStorage()) {
                if (item.isCutted()) {
                    return(item.isDirectory())?new ImageView(directoryCutted):new ImageView(fileCutted);
                }
                if(!item.isAvailable()){
                    return(item.isDirectory())?new ImageView(directoryUnavaible):new ImageView(fileUnavaible);
                }
                if(item.isHidden()){
                    return(item.isDirectory())?new ImageView(directoryHidden):new ImageView(fileHidden);
                }
                if(!item.isHidden()) {
                    return (item.isDirectory()) ? new ImageView(directoryNormal): new ImageView(fileNormal);
                }
            }
            return new ImageView(itemDrive);
        } catch (Exception e) {
            return new ImageView();
        }
    }

    public static ImageView getItemWaiting() {
        return new ImageView(itemWaiting);
    }
    public static ImageView getDirectoryCutted() {return new ImageView(directoryCutted);}
    public static ImageView getFileCutted() {return new ImageView(fileCutted);}
    public static ImageView getDirectoryUnavaible() {
        return new ImageView(directoryUnavaible);
    }
    public static void updateIcon(FXOptimizedItem item, ImageView icon) {
            item.setIcon(icon);
            Platform.runLater(()->item.setGraphic(icon));
    }

    public static FXOptimizedItem getNewfxOptimizedItem(Item item){
        return new FXOptimizedItem(item);
    }
}
