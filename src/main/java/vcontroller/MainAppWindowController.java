package vcontroller;

import interfaces.IContentChangedEventListener;
import interfaces.IFileManager;
import interfaces.IItemsTreeUpdatedEventListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.FileManagerImpl;
import model.Item;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

import static model.AppEnums.*;
import static vcontroller.ItemViewFactory.FXOptimizedItem;


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
    public Button toolbUp;
    public ToggleButton toolbShowHiddenItems;
    public TableColumn columnItemImage;
    public TableColumn columnName;
    public TableColumn columnType;
    public TableColumn columnCreated;
    public TableColumn columnLastModified;
    public TableColumn columnSize;
    public TableColumn columnAttributes;

    private IFileManager fileManager;
    private FXOptimizedItem parentItem;
    private ObservableList<FXOptimizedItem> selectedItemsList;
    private ExecutorService threadLogicUIPool;
    //private GuiLogicLayer guiLogicLayer;
/*
    @Override
    public void onItemContentChanged(FXOptimizedItem item) {

    }
/*
    @Override
    public void onUpdateItemsTree(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList) {
        selectedItemsList.add(new FXOptimizedItem(parentItem.getItem()));
        //parentItem.getChildren().sort((o1,  o2) ->
                        //(o1.getValue().getName().compareTo(o2.getValue().getName())));
    }
*/
    private class ItemContentLoader extends Task<Void>{
        ImageView tempImageView;
        FXOptimizedItem item;
        boolean isIconChanging;


        public ItemContentLoader(ImageView tempImageView, FXOptimizedItem item, boolean isItemChanging) {
            this.tempImageView = tempImageView;
            this.item=item;
            this.isIconChanging=isItemChanging;
        }

        private int toInt(boolean value) {
            return (value)?1:0;
        }

        @Override
        protected Void call() throws Exception {

            ObservableList<FXOptimizedItem> innerItemsInView = tablevDirContent.getItems();
            Platform.runLater(()->innerItemsInView.clear());

            if(!selectedItemsList.isEmpty()){
                    Platform.runLater(() -> {
                        innerItemsInView.addAll(selectedItemsList);
                        innerItemsInView.sort((o1,o2)-> {
                            int o1ToInt=toInt(o1.isDirectory());
                            int o2ToInt=toInt(o2.isDirectory());
                            if (o1ToInt == o2ToInt) {
                                return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                            }
                            return o2ToInt-o1ToInt;
                        }) ;
                    });
            }
            if (isIconChanging) {
                Platform.runLater(() -> item.setGraphic(tempImageView));
            }
            return null;
        }
    }


    public NameConflictState onConflict() {
        return null;
    }

    public void initialize(){

        fileManager=FileManagerImpl.getInstance();

        threadLogicUIPool=MainController.getThreadLogicUIPool();
        selectedItemsList= FXCollections.observableArrayList();

        initButtons();
        initItemsTree();
        initItemContentView();
        //guiLogicLayer.updateItemsTree(parentItem,selectedItemsList);
        loadChildrenDirectoriesInTree(parentItem, selectedItemsList);
        getItemContent(parentItem, true);
    }


    private void initItemContentView() {
        tablevDirContent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void initItemsTree() {
        HashSet<Item> selectedItems=null;
        parentItem=ItemViewFactory.getRoot();
        treevItemsTree.setRoot(parentItem);
        treevItemsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        try {
            selectedItems = (HashSet<Item>)fileManager.getContent(parentItem.getValue());
        } catch (NullPointerException e) {
            onUnavaibleItemHandler();
        }
        for (Item selectedItem : selectedItems) {
            selectedItemsList.add(new FXOptimizedItem(selectedItem));
        }
    }

    private void getItemContent(FXOptimizedItem item, boolean isIconChanging) {

        ImageView tempImageView=null;

        if (isIconChanging) {
            ImageView tempImageViewLink = (ImageView)item.getGraphic();
            tempImageView = tempImageViewLink;
            item.setGraphic(ItemViewFactory.getItemWaiting());
        }

        ItemContentLoader contentLoader = new ItemContentLoader(tempImageView, item, isIconChanging);
        threadLogicUIPool.execute(contentLoader);
    }

    private void loadChildrenDirectoriesInTree(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem>selectedItemsList){

        ObservableList<TreeItem<Item>> childrenDirectories =parentItem.getChildren();
        childrenDirectories.clear();

        Task<Void> subdirectoriesLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for (FXOptimizedItem item : selectedItemsList) {
                    if (item.isDirectory()) {
                        childrenDirectories.add(new FXOptimizedItem(item.getItem()));
                    }
                }

                Platform.runLater(() -> parentItem.getChildren().sort((o1,  o2) ->
                        (o1.getValue().getName().compareTo(o2.getValue().getName()))));
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
                Image imageToolbUp=new Image(getClass().getResourceAsStream("/img/iconLevelUp.png"));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        toolbCopy.setGraphic(new ImageView(imageToolbCopy));
                        toolbCut.setGraphic(new ImageView(imageToolbCut));
                        toolbDelete.setGraphic(new ImageView(imageToolbDelete));
                        toolbRename.setGraphic(new ImageView(imageToolbRename));
                        toolbPaste.setGraphic(new ImageView(imageToolbPaste));
                        toolbShowHiddenItems.setGraphic(new ImageView(imageToolbShowHiddenItems));
                        toolbUp.setGraphic(new ImageView(imageToolbUp));
                    }
                });

                return null;
            }
        };
        threadLogicUIPool.execute(buttonsImageLoader);
    }

    public void loadItemContent(MouseEvent mouseEvent) {

        FXOptimizedItem tempSelectedLink=parentItem;
        FXOptimizedItem tempSelected=tempSelectedLink;

        try {
            parentItem=(FXOptimizedItem) treevItemsTree.getSelectionModel().getSelectedItem();

        } catch (NullPointerException e) {
            return;
        }

        parentItem=(parentItem==null)?tempSelected:parentItem;

        HashSet<Item> selectedItems=null;

        try {
            selectedItems = (HashSet<Item>)fileManager.getContent(parentItem.getValue());
            selectedItemsList.clear();
            for (Item selectedItem : selectedItems) {
                selectedItemsList.add(new FXOptimizedItem(selectedItem));
            }
        } catch (ClassCastException e) {
            onUnavaibleItemHandler();
            selectedItemsList.clear();
            parentItem.setGraphic(ItemViewFactory.getDirectoryUnavaible());
        }
        loadChildrenDirectoriesInTree(parentItem, selectedItemsList);
        getItemContent(parentItem, true);

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
    public void onItemsListClick(MouseEvent mouseEvent) {
        if(mouseEvent.getClickCount()==2){
            FXOptimizedItem selectedInList;
            HashSet<Item> selectedItems;

            try {

                selectedInList = (FXOptimizedItem)tablevDirContent.getSelectionModel().getSelectedItem();



                if (selectedInList.isDirectory()) {
                    parentItem = selectedInList;

                    try {
                        selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
                        selectedItemsList.clear();
                        for (Item selectedItem : selectedItems) {
                            selectedItemsList.add(new FXOptimizedItem(selectedItem));
                        }
                    } catch (ClassCastException e) {
                        onUnavaibleItemHandler();
                        parentItem.setGraphic(ItemViewFactory.getDirectoryUnavaible());
                    }

                    getItemContent(parentItem, true);
                }
            } catch (NullPointerException e) {
                return;
            }


        }
    }

    private void onUnavaibleItemHandler() {
    }

    public void getLevelUp(ActionEvent actionEvent) {

        if (parentItem.equals(ItemViewFactory.getRoot())) {
            return;
        }

        HashSet<Item> selectedItems;

        parentItem=ItemViewFactory.getParent(parentItem);

        try {
            selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
            selectedItemsList.clear();
            for (Item selectedItem : selectedItems) {
                selectedItemsList.add(new FXOptimizedItem(selectedItem));
            }
        } catch (ClassCastException e) {
            onUnavaibleItemHandler();
        }

        getItemContent(parentItem, false);

    }
}
