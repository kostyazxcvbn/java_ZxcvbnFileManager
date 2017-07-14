package controllers;

import interfaces.IFileManager;
import interfaces.IRefresher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import model.FileManagerImpl;
import model.Item;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static model.AppEnums.*;
import static controllers.FileManagerItemsFactory.FXOptimizedItem;


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
    private boolean showHiddenItemsState;

    private class ItemContentLoader extends Task<Void>{

        FXOptimizedItem item;
        CountDownLatch countDownLatch;
        ObservableList<FXOptimizedItem> selectedItemsList;

        public ItemContentLoader(FXOptimizedItem item, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch) {
            this.item=item;
            this.countDownLatch = countDownLatch;
            this.selectedItemsList=selectedItemsList;
        }

        private int toInt(boolean value) {
            return (value)?1:0;
        }

        @Override
        protected Void call() throws Exception {
            ObservableList<FXOptimizedItem> innerItemsInView = tablevDirContent.getItems();
            innerItemsInView.clear();

            if(!selectedItemsList.isEmpty()){
                innerItemsInView.addAll((showHiddenItemsState)?selectedItemsList:selectedItemsList.filtered(fxOptimizedItem -> !fxOptimizedItem.isHidden()));
                innerItemsInView.sort((o1,o2)-> {
                            int o1ToInt=toInt(o1.isDirectory());
                            int o2ToInt=toInt(o2.isDirectory());
                            if (o1ToInt == o2ToInt) {
                                return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                            }
                            return o2ToInt-o1ToInt;
                        });
            }
            countDownLatch.countDown();
            return null;
        }
    }
    private class SubdirectoriesLoader extends Task<Void>{

        CountDownLatch countDownLatch;
        FXOptimizedItem parentItem;
        ObservableList<FXOptimizedItem> selectedItemsList;

        public SubdirectoriesLoader(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.parentItem=parentItem;
            this.selectedItemsList=selectedItemsList;
        }

        @Override
        protected Void call() throws Exception {

            ObservableList<TreeItem<Item>> childrenDirectories =parentItem.getChildren();
            childrenDirectories.clear();

            for (FXOptimizedItem item : selectedItemsList) {
                if (item.isDirectory()) {
                    if (showHiddenItemsState | !item.isHidden()) {
                        childrenDirectories.add(new FXOptimizedItem(item.getItem()));
                    }
                }
            }
            parentItem.getChildren().sort((o1,  o2) ->
                    (o1.getValue().getName().toUpperCase().compareTo(o2.getValue().getName().toUpperCase())));
            countDownLatch.countDown();
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

        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, treevItemsTree, 2000L);
        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList,countDownLatch));
            }
        });

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        threadLogicUIPool.execute(appViewRefresher);
    }

    private void initItemContentView() {
        tablevDirContent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void initItemsTree() {
        HashSet<Item> selectedItems=null;
        parentItem= FileManagerItemsFactory.getRoot();
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

        Object itemsContainer=treevItemsTree;

        FXOptimizedItem tempSelectedLink=parentItem;
        FXOptimizedItem tempSelected=tempSelectedLink;

        try {
            parentItem=(FXOptimizedItem) treevItemsTree.getSelectionModel().getSelectedItem();

        } catch (NullPointerException e) {
            return;
        }



        if (parentItem == null) {
            parentItem=tempSelected;
            itemsContainer=null;
        }

        if(!parentItem.isLeaf()){
            itemsContainer=null;
        }

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
            parentItem.setGraphic(FileManagerItemsFactory.getDirectoryUnavaible());
        }

        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, itemsContainer, 2000L);
        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList,countDownLatch));
            }
        });

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        threadLogicUIPool.execute(appViewRefresher);

    }
    public void closeApp(ActionEvent actionEvent) {

    }

    public void copyItems(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = tablevDirContent.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }
        Map<Item, ItemConflicts> operationErrorsMap = fileManager.copyItemsToBuffer(itemsCollection);
        if (!operationErrorsMap.isEmpty()) {
            onItemsLoadingErrorHandler();
        }

        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, tablevDirContent, 0);

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList,countDownLatch));
            }
        });

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        threadLogicUIPool.execute(appViewRefresher);

    }

    public void cutItems(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = tablevDirContent.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }
        Map<Item, ItemConflicts> operationErrorsMap = fileManager.cutItemsToBuffer(itemsCollection);

        if (!operationErrorsMap.isEmpty()) {
            onItemsLoadingErrorHandler();
        }

        for (FXOptimizedItem selectedItem : selectedItems) {
            if (!operationErrorsMap.containsKey(selectedItem.getItem())) {

                FileManagerItemsFactory.updateIcon(
                        tablevDirContent,
                        selectedItem,
                        (selectedItem.isDirectory())?FileManagerItemsFactory.getDirectoryCutted():FileManagerItemsFactory.getFileCutted());
            }
        }

        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, tablevDirContent, 0);

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList,countDownLatch));
            }
        });

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        threadLogicUIPool.execute(appViewRefresher);
    }

    public void pasteItems(ActionEvent actionEvent) {

    }

    public void deleteItems(ActionEvent actionEvent) {

    }

    public void renameItem(ActionEvent actionEvent) {

    }

    public void showHiddenItems(ActionEvent actionEvent) {
        showHiddenItemsState=cmiShowHiddenItems.isSelected();

        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, treevItemsTree, 2000L);

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });


        threadLogicUIPool.execute(appViewRefresher);
    }

    public void copyItemsTo(ActionEvent actionEvent) {

    }

    public void moveItemsTo(ActionEvent actionEvent) {

    }

    public void showTableViewContextMenu(ContextMenuEvent contextMenuEvent) {

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
                        parentItem.setGraphic(FileManagerItemsFactory.getDirectoryUnavaible());
                    }


                    AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, tablevDirContent, 2000L);

                    appViewRefresher.addListener(new IRefresher() {
                        @Override
                        public void refresh(CountDownLatch countDownLatch) {
                            threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
                        }
                    });

                    threadLogicUIPool.execute(appViewRefresher);

                }
            } catch (NullPointerException e) {
                return;
            }
        }
    }

    private void onUnavaibleItemHandler() {
    }

    public void getLevelUp(ActionEvent actionEvent) {

        if (parentItem.equals(FileManagerItemsFactory.getRoot())) {
            return;
        }

        HashSet<Item> selectedItems;

        parentItem=parentItem.getParentItem();

        try {
            selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
            selectedItemsList.clear();
            for (Item selectedItem : selectedItems) {
                selectedItemsList.add(new FXOptimizedItem(selectedItem));
            }
        } catch (ClassCastException e) {
            onUnavaibleItemHandler();
        }
        AppViewRefresher appViewRefresher=new AppViewRefresher(parentItem, tablevDirContent, 2000L);

        appViewRefresher.addListener(new IRefresher() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        });

        threadLogicUIPool.execute(appViewRefresher);
    }
}
