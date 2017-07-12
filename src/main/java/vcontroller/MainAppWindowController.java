package vcontroller;

import interfaces.IFileManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.FileManagerImpl;
import model.Item;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import static model.AppEnums.*;
import static vcontroller.ItemViewFactory.FxOptimizedItem;


/**
 * Created by kostyazxcvbn on 06.07.2017.
 */

public class MainAppWindowController{

    public CheckMenuItem cmiShowHiddenItems;
    public TreeView treevItemsTree;
    public TableView tablevDirContent;
    public Button toolbCopy;

    public Button toolbCut;
    public Button toolbPaste;
    public Button toolbDelete;
    public Button toolbRename;
    public ToggleButton toolbShowHiddenItems;
    public TableColumn columnItemImage;
    public TableColumn columnName;
    public TableColumn columnType;
    public TableColumn columnCreated;
    public TableColumn columnLastModified;
    public TableColumn columnSize;
    public TableColumn columnAttributes;

    private IFileManager fileManager;
    private TreeItem<Item>parentItem;
    private HashSet<Item>selectedItems;
    private ExecutorService threadLogicUIPool;

    private class ItemContentLoader extends Task<Void>{
        ImageView tempImageView;
        TreeItem<Item> item;

        public ItemContentLoader(ImageView tempImageView, TreeItem<Item> item) {
            this.tempImageView = tempImageView;
            this.item=item;
        }

        @Override
        protected Void call() throws Exception {
            Platform.runLater(() -> item.setGraphic(ItemViewFactory.getItemWaiting()));

            if(!selectedItems.isEmpty()){

                ObservableList<FxOptimizedItem> innerItemsInView = tablevDirContent.getItems();
                innerItemsInView.clear();

                for (Item cuttentItem : selectedItems) {
                    Platform.runLater(() -> innerItemsInView.add(ItemViewFactory.getNewfxOptimizedItem(cuttentItem)));
                }
            }

            Platform.runLater(() -> item.setGraphic(tempImageView));
            return null;
        }
    }


    public NameConflictState onConflict() {
        return null;
    }

    public void initialize(){

        fileManager=FileManagerImpl.getInstance();
        threadLogicUIPool=MainController.getThreadLogicUIPool();

        initButtons();
        initItemsTree();
        initItemContentView();

        loadParentDirectoriesInTree(parentItem,selectedItems);
        getItemContent(parentItem);



    }


    private void initItemContentView() {
        tablevDirContent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void initItemsTree() {

        parentItem=ItemViewFactory.getRoot();
        treevItemsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        selectedItems = (HashSet)fileManager.getContent(parentItem.getValue());
        treevItemsTree.setRoot(parentItem);
    }

    private void getItemContent(TreeItem<Item> item) {

        ImageView tempImageViewLink = (ImageView)item.getGraphic();
        ImageView tempImageView = tempImageViewLink;
        item.setGraphic(ItemViewFactory.getItemWaiting());


        ItemContentLoader contentLoader = new ItemContentLoader(tempImageView, item);
        threadLogicUIPool.execute(contentLoader);

    }

    private void loadParentDirectoriesInTree(TreeItem<Item> parentItem, HashSet<Item>selectedItems){

        ObservableList<TreeItem<Item>> childrenDirectories =  parentItem.getChildren();
        childrenDirectories.clear();

        Task<Void> subdirectoriesLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (Item item : selectedItems) {
                    if(item.isDirectory()){
                        Platform.runLater(() -> childrenDirectories.add(ItemViewFactory.getTreeItem(item)));
                    }
                }
                Platform.runLater(() -> parentItem.getChildren().sort((o1, o2) ->
                        (o1.getValue().getPath().toAbsolutePath().toString().compareTo(o2.getValue().getPath().toAbsolutePath().toString()))));
                return null;
            }
        };

        threadLogicUIPool.execute(subdirectoriesLoader);
    }

    private void onItemsLoadingErrorHandler() {
    }

    private void initButtons() {

        Task<Void> buttonsImageLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                Image imageToolbCopy=new Image(getClass().getResourceAsStream("/img/iconCopy.png"));
                Image imageToolbCut=new Image(getClass().getResourceAsStream("/img/iconCut.png"));
                Image imageToolbDelete=new Image(getClass().getResourceAsStream("/img/iconRemove.png"));
                Image imageToolbRename=new Image(getClass().getResourceAsStream("/img/iconRename.png"));
                Image imageToolbPaste=new Image(getClass().getResourceAsStream("/img/iconPaste.png"));
                Image imageToolbShowHiddenItems=new Image(getClass().getResourceAsStream("/img/iconHide.png"));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        toolbCopy.setGraphic(new ImageView(imageToolbCopy));
                        toolbCut.setGraphic(new ImageView(imageToolbCut));
                        toolbDelete.setGraphic(new ImageView(imageToolbDelete));
                        toolbRename.setGraphic(new ImageView(imageToolbRename));
                        toolbPaste.setGraphic(new ImageView(imageToolbPaste));
                        toolbShowHiddenItems.setGraphic(new ImageView(imageToolbShowHiddenItems));
                    }
                });

                return null;
            }
        };
        threadLogicUIPool.execute(buttonsImageLoader);
    }

    public void loadItemContent(MouseEvent mouseEvent) {

        TreeItem<Item> tempSelectedLink=parentItem;
        TreeItem<Item> tempSelected=tempSelectedLink;

        try {
            parentItem=(TreeItem<Item>)treevItemsTree.getSelectionModel().getSelectedItem();

        } catch (NullPointerException e) {
            return;
        }

        parentItem=(parentItem==null)?tempSelected:parentItem;

        selectedItems = (HashSet)fileManager.getContent(parentItem.getValue());
        loadParentDirectoriesInTree(parentItem,selectedItems);
        getItemContent(parentItem);

    }
    public void closeApp(ActionEvent actionEvent) {

    }

    public void copyItems(ActionEvent actionEvent) {

    }

    public void cutItems(ActionEvent actionEvent) {

    }

    public void pasteItems(ActionEvent actionEvent) {

    }

    public void deleteItems(ActionEvent actionEvent) {

    }

    public void renameItem(ActionEvent actionEvent) {

    }

    public void showHiddenItems(ActionEvent actionEvent) {

    }

    public void copyItemsTo(ActionEvent actionEvent) {

    }

    public void moveItemsTo(ActionEvent actionEvent) {

    }

    public void showAboutInfo(ActionEvent actionEvent) {

    }
}
