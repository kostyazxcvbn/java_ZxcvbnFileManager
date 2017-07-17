package controllers;

import interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FileManagerImpl;
import model.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static model.AppEnums.*;
import static controllers.FileManagerItemsFactory.FXOptimizedItem;

/**
 * Created by kostyazxcvbn on 06.07.2017.
 */

public class MainAppWindowController implements IConflictListener {

    @FXML
    private MenuItem contextBack;
    @FXML
    private MenuItem contextCopy;
    @FXML
    private MenuItem contextCut;
    @FXML
    private MenuItem contextDelete;
    @FXML
    private MenuItem contextPaste;
    @FXML
    private MenuItem miCopy;
    @FXML
    private MenuItem miCut;
    @FXML
    private MenuItem miDelete;
    @FXML
    private MenuItem miPaste;
    @FXML
    private CheckMenuItem cmiShowHiddenItems;
    @FXML
    private TreeView treevItemsTree;
    @FXML
    private TableView tablevDirContent;
    @FXML
    private Button toolbCopy;
    @FXML
    private Button toolbCut;
    @FXML
    private Button toolbPaste;
    @FXML
    private Button toolbDelete;
    @FXML
    private Button toolbUp;
    @FXML
    private ToggleButton toolbShowHiddenItems;
    @FXML
    private TableColumn columnItemImage;
    @FXML
    private TableColumn columnName;
    @FXML
    private TableColumn columnType;
    @FXML
    private TableColumn columnCreated;
    @FXML
    private TableColumn columnLastModified;
    @FXML
    private TableColumn columnSize;
    @FXML
    private TableColumn columnAttributes;

    private IFileManager fileManager;
    private FXOptimizedItem parentItem;
    private ObservableList<FXOptimizedItem> selectedItemsList;
    private ExecutorService threadLogicUIPool;
    private boolean showHiddenItemsState;

    private FXMLLoader itemNameConflictModalLoader;
    private Stage itemNameConflictModalStage;
    private ItemNameConflictModalController itemNameConflictModalController;
    private Parent itemNameConflictModalparent;

    private FXMLLoader okCancelModalLoader;
    private Stage okCancelModalStage;
    private OkCancelModalController okCancelModalController;
    private Parent okCancelModalparent;

    private FXMLLoader operationsConflictModalLoader;
    private Stage operationsConflictModalStage;
    private OperationsConflictModalController operationsConflictModalController;
    private Parent operationsConflictModalparent;

    private IWarningable actionOnDeleteItem;
    private IWarningable actionOnCloseApp;
    private IWarningable actionOnAboutInfo;
    private IRefreshingListener itemsTreeRefreshListener;
    private IRefreshingListener itemContentContainerListener;

    private Object lock;

    @Override
    public NameConflictState onConflict() {

        Platform.runLater(() -> showModalWindow(itemNameConflictModalStage));
        NameConflictState nameConflictState = null;

        itemNameConflictModalController.setWaitingResultLock(lock);

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return nameConflictState;
    }

    //inner runnable subtasks
    private class ItemContentLoader extends Task<Void> {

        FXOptimizedItem item;
        CountDownLatch countDownLatch;
        ObservableList<FXOptimizedItem> selectedItemsList;

        public ItemContentLoader(FXOptimizedItem item, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch) {
            this.item = item;
            this.countDownLatch = countDownLatch;
            this.selectedItemsList = selectedItemsList;
        }

        private int toInt(boolean value) {
            return (value) ? 1 : 0;
        }

        @Override
        protected Void call() throws Exception {
            ObservableList<FXOptimizedItem> innerItemsInView = tablevDirContent.getItems();
            innerItemsInView.clear();

            if (!selectedItemsList.isEmpty()) {
                innerItemsInView.addAll((showHiddenItemsState) ? selectedItemsList : selectedItemsList.filtered(fxOptimizedItem -> !fxOptimizedItem.isHidden()));
                innerItemsInView.sort((o1, o2) -> {
                    int o1ToInt = toInt(o1.isDirectory());
                    int o2ToInt = toInt(o2.isDirectory());
                    if (o1ToInt == o2ToInt) {
                        return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                    }
                    return o2ToInt - o1ToInt;
                });
            }
            countDownLatch.countDown();
            return null;
        }
    }

    private class SubdirectoriesLoader extends Task<Void> {

        CountDownLatch countDownLatch;
        FXOptimizedItem parentItem;
        ObservableList<FXOptimizedItem> selectedItemsList;

        public SubdirectoriesLoader(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.parentItem = parentItem;
            this.selectedItemsList = selectedItemsList;
        }

        @Override
        protected Void call() throws Exception {

            ObservableList<TreeItem<Item>> childrenDirectories = parentItem.getChildren();
            childrenDirectories.clear();

            for (FXOptimizedItem item : selectedItemsList) {
                if (item.isDirectory()) {
                    if (showHiddenItemsState | !item.isHidden()) {
                        childrenDirectories.add(new FXOptimizedItem(item.getItem()));
                    }
                }
            }
            parentItem.getChildren().sort((o1, o2) ->
                    (o1.getValue().getName().toUpperCase().compareTo(o2.getValue().getName().toUpperCase())));
            countDownLatch.countDown();
            return null;
        }
    }

    private class ItemPaster extends Task<Void> {

        Map<Item, ItemConflicts> operationErrorsMap;
        FXOptimizedItem destination;

        public ItemPaster(FXOptimizedItem destination) {
            this.operationErrorsMap = new HashMap<>();
            this.destination = destination;
        }

        @Override
        protected Void call() throws Exception {
            Map<Item, ItemConflicts> operationError;
            Set<Item> itemsBuffer = fileManager.getBuffer();

            itemNameConflictModalController.init(NameConflictState.NO_CONFLICTS);

            for (Item item : itemsBuffer) {
                operationError = fileManager.pasteItemFromBuffer(item, destination.getItem(), itemNameConflictModalController.getNameConflictState());
                if (operationError != null) {
                    operationErrorsMap.putAll(operationError);
                } else {
                    Platform.runLater(() -> showModalWindow(itemNameConflictModalStage));
                    synchronized (lock) {
                        lock.wait();
                    }
                    operationErrorsMap.putAll(fileManager.pasteItemFromBuffer(item, destination.getItem(), itemNameConflictModalController.getNameConflictState()));
                }
            }

            if (!operationErrorsMap.isEmpty()) {
                onConflictsHandler(operationErrorsMap);
            }

            AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 0);

            appViewRefresher.addListener(new IRefreshingListener() {
                @Override
                public void refresh(CountDownLatch countDownLatch) {
                    threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList, countDownLatch));
                }
            });

            appViewRefresher.addListener(new IRefreshingListener() {
                @Override
                public void refresh(CountDownLatch countDownLatch) {
                    threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
                }
            });

            threadLogicUIPool.execute(appViewRefresher);

            return null;
        }
    }



    //initialization's methods
    public void initialize() {

        fileManager = FileManagerImpl.getInstance();
        ((IConlictable) fileManager).addListener(this);

        threadLogicUIPool = MainController.getThreadLogicUIPool();
        selectedItemsList = FXCollections.observableArrayList();

        lock = new Object();

        initButtons();
        initImpls();

        initItemNameConflictModal();
        initOkCancelModal();
        initOperationsConflictModal();

        initItemsTree();
        initItemContentView();

        MainController.getPrimaryStage().setOnCloseRequest(exit -> {
            exit.consume();
            getOkCancelCloseModal();
        });

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, treevItemsTree, 2000L);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);
        ;
        threadLogicUIPool.execute(appViewRefresher);
        guiControlsStateHandler(GuiControlsState.ROOT_LEVEL);
    }

    private void initButtons() {

        Task<Void> buttonsImageLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                toolbCopy.setTooltip(new Tooltip("Copy item(s)"));
                toolbCut.setTooltip(new Tooltip("Cut item(s)"));
                toolbDelete.setTooltip(new Tooltip("Delete item(s)"));
                toolbPaste.setTooltip(new Tooltip("Paste item(s)"));
                toolbShowHiddenItems.setTooltip(new Tooltip("Show/hide hidden items"));
                toolbUp.setTooltip(new Tooltip("Back"));

                Image imageToolbCopy = new Image(getClass().getResourceAsStream("/img/iconCopy.png"));
                Image imageToolbCut = new Image(getClass().getResourceAsStream("/img/iconCut.png"));
                Image imageToolbDelete = new Image(getClass().getResourceAsStream("/img/iconRemove.png"));
                Image imageToolbPaste = new Image(getClass().getResourceAsStream("/img/iconPaste.png"));
                Image imageToolbShowHiddenItems = new Image(getClass().getResourceAsStream("/img/iconHide.png"));
                Image imageToolbUp = new Image(getClass().getResourceAsStream("/img/iconLevelUp.png"));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        toolbCopy.setGraphic(new ImageView(imageToolbCopy));
                        toolbCut.setGraphic(new ImageView(imageToolbCut));
                        toolbDelete.setGraphic(new ImageView(imageToolbDelete));
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

    private void initImpls() {

        actionOnCloseApp = new IWarningable() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection) {
                onCloseAppHandler();

            }
        };

        actionOnAboutInfo = new IWarningable() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection) {
                Platform.runLater(() -> MainController.getCurrentStage().hide());
            }
        };

        itemsTreeRefreshListener = new IRefreshingListener() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new SubdirectoriesLoader(parentItem, selectedItemsList, countDownLatch));
            }
        };

        itemContentContainerListener = new IRefreshingListener() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                threadLogicUIPool.execute(new ItemContentLoader(parentItem, selectedItemsList, countDownLatch));
            }
        };

        actionOnDeleteItem = new IWarningable() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection) {
                Map<Item, ItemConflicts> operationErrorsMap = fileManager.deleteItems(itemsCollection);

                if (!operationErrorsMap.isEmpty()) {
                    onConflictsHandler(operationErrorsMap);
                }

                AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 0);
                appViewRefresher.addListener(itemsTreeRefreshListener);
                appViewRefresher.addListener(itemContentContainerListener);

                threadLogicUIPool.execute(appViewRefresher);
                Platform.runLater(() -> MainController.getCurrentStage().hide());

            }
        };
    }

    private void initItemNameConflictModal() {
        if (itemNameConflictModalLoader == null) {
            itemNameConflictModalLoader = new FXMLLoader(getClass().getResource("/fxml/ItemNameConflictModal.fxml"));
            itemNameConflictModalStage = new Stage();

            try {
                itemNameConflictModalparent = itemNameConflictModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(itemNameConflictModalparent);
            itemNameConflictModalStage.setTitle("Please choose an action...");
            itemNameConflictModalStage.setScene(scene);
            itemNameConflictModalStage.sizeToScene();
            itemNameConflictModalStage.setResizable(false);
            itemNameConflictModalStage.initModality(Modality.WINDOW_MODAL);
            itemNameConflictModalStage.initOwner(MainController.getPrimaryStage());
            itemNameConflictModalController = itemNameConflictModalLoader.getController();
            itemNameConflictModalController.setWaitingResultLock(lock);
        }
    }

    private void initOkCancelModal() {
        if (okCancelModalLoader == null) {
            okCancelModalLoader = new FXMLLoader(getClass().getResource("/fxml/OkCancelContainerModal.fxml"));
            okCancelModalStage = new Stage();

            try {
                okCancelModalparent = okCancelModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(okCancelModalparent);
            okCancelModalStage.setTitle("Warning!");
            okCancelModalStage.setScene(scene);
            okCancelModalStage.sizeToScene();
            okCancelModalStage.setResizable(false);
            okCancelModalStage.initModality(Modality.WINDOW_MODAL);
            okCancelModalStage.initOwner(MainController.getPrimaryStage());
            okCancelModalController = okCancelModalLoader.getController();
        }
    }

    private void initOperationsConflictModal() {
        if (operationsConflictModalLoader == null) {
            operationsConflictModalLoader = new FXMLLoader(getClass().getResource("/fxml/OperationsConflictModal.fxml"));
            operationsConflictModalStage = new Stage();

            try {
                operationsConflictModalparent = operationsConflictModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(operationsConflictModalparent);
            operationsConflictModalStage.setTitle("Operation's conflicts");
            operationsConflictModalStage.setScene(scene);
            operationsConflictModalStage.sizeToScene();
            operationsConflictModalStage.setResizable(false);
            operationsConflictModalStage.initModality(Modality.WINDOW_MODAL);
            operationsConflictModalStage.initOwner(MainController.getPrimaryStage());
            operationsConflictModalController = operationsConflictModalLoader.getController();
        }
    }

    private void initItemsTree() {
        HashSet<Item> selectedItems = null;
        parentItem = FileManagerItemsFactory.getRoot();
        treevItemsTree.setRoot(parentItem);
        treevItemsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
        if (selectedItems.isEmpty()){
            Map<Item, ItemConflicts> operationErrorsMap=new HashMap();
            operationErrorsMap.put(parentItem.getValue(),ItemConflicts.SECURITY_ERROR);
            onConflictsHandler(operationErrorsMap);
        }
        for (Item selectedItem : selectedItems) {
            selectedItemsList.add(new FXOptimizedItem(selectedItem));
        }
    }

    private void initItemContentView() {
        tablevDirContent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void showModalWindow(Stage modalWindowStage) {
        MainController.setCurrentStage(modalWindowStage);
        modalWindowStage.show();
    }

    //handlers
    private void onCloseAppHandler() {

        Platform.runLater(() -> MainController.getPrimaryStage().close());

        Platform.exit();
        System.exit(0);
    }

    private void guiControlsStateHandler(GuiControlsState guiControlsState) {
        toolbCopy.setDisable(true);
        toolbCut.setDisable(true);
        toolbPaste.setDisable(true);
        toolbDelete.setDisable(true);
        Task<Void> guiControlsStateSwitcher = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                boolean stateCopy=false;
                boolean stateCut=false;
                boolean statePaste=false;
                boolean stateDelete=false;
                boolean stateBack=false;

                switch (guiControlsState) {
                    case ROOT_LEVEL:{
                        stateCopy=false;
                        stateCut=false;
                        statePaste=false;
                        stateDelete=false;
                        stateBack=true;
                        break;
                    }
                    case NOTHING_SELECTED:
                    case EMPTY_CONTENT:{
                        stateCopy=false;
                        stateCut=false;
                        statePaste=true;
                        stateDelete=false;
                        stateBack=true;
                        break;
                    }
                    case FILE_SELECTED:{
                        stateCopy=true;
                        stateCut=true;
                        statePaste=false;
                        stateDelete=true;
                        stateBack=true;
                        break;
                    }
                    case FOLDER_SELECTED:{
                        stateCopy=true;
                        stateCut=true;
                        statePaste=true;
                        stateDelete=true;
                        stateBack=true;
                        break;
                    }
                    default:{
                        break;
                    }
                }






                /*



                Platform.runLater(() ->{

                    toolbCopy.setDisable(!stateCopy);
                    contextCopy.setDisable(!stateCopy);
                    miCopy.setDisable(!stateCopy);

                    toolbCut.setDisable(!stateCut);
                    contextCut.setDisable(!stateCut);
                    miCut.setDisable(!stateCut);

                    toolbPaste.setDisable(!statePaste);
                    contextPaste.setDisable(!statePaste);
                    miPaste.setDisable(!statePaste);

                    toolbDelete.setDisable(!stateDelete);
                    contextDelete.setDisable(!stateDelete);
                    miDelete.setDisable(!stateDelete);

                    toolbUp.setDisable(!stateBack);
                    contextBack.setDisable(!stateBack)

                });

                */
                return null;
            }
        };

        threadLogicUIPool.execute(guiControlsStateSwitcher);


    }

    private void onConflictsHandler(Map<Item, ItemConflicts> operationErrorsMap) {
        operationsConflictModalController.init(operationErrorsMap);
        Platform.runLater(() -> showModalWindow(operationsConflictModalStage));

    }

    private void onFatalErrorHandler() {
        okCancelModalController.initWarningModal("The fatal application error happened and the application will be closed!",actionOnCloseApp,null);
        okCancelModalController.getButtonCancel().setVisible(false);
        showModalWindow(okCancelModalStage);
    }

    //gui reactions
    public void loadItemContent(MouseEvent mouseEvent) {

        Object itemsContainer = treevItemsTree;

        FXOptimizedItem tempSelectedLink = parentItem;
        FXOptimizedItem tempSelected = tempSelectedLink;

        try {
            parentItem = (FXOptimizedItem) treevItemsTree.getSelectionModel().getSelectedItem();

        } catch (NullPointerException e) {
            return;
        }

        if (parentItem == null) {
            parentItem = tempSelected;
            itemsContainer = null;
        }

        if (!parentItem.isLeaf()) {
            itemsContainer = null;
        }

        HashSet<Item> selectedItems = null;

        try {
            selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
            selectedItemsList.clear();
            for (Item selectedItem : selectedItems) {
                selectedItemsList.add(new FXOptimizedItem(selectedItem));
            }
        } catch (Exception e) {
            Map<Item, ItemConflicts> operationErrorsMap=new HashMap();
            operationErrorsMap.put(parentItem.getValue(),ItemConflicts.SECURITY_ERROR);
            onConflictsHandler(operationErrorsMap);
            selectedItemsList.clear();
            FileManagerItemsFactory.updateIcon(tablevDirContent,parentItem,FileManagerItemsFactory.getDirectoryUnavaible());
        }

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, itemsContainer, 2000L);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);

        threadLogicUIPool.execute(appViewRefresher);

    }

    public void onClosePressed(ActionEvent actionEvent) {
        getOkCancelCloseModal();
    }

    public void copyItems(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = tablevDirContent.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }
        Map<Item, ItemConflicts> operationErrorsMap = fileManager.copyItemsToBuffer(itemsCollection);
        if (!operationErrorsMap.isEmpty()) {
            onConflictsHandler(operationErrorsMap);
        }

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 0);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);

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
            onConflictsHandler(operationErrorsMap);
        }

        for (FXOptimizedItem selectedItem : selectedItems) {
            if (!operationErrorsMap.containsKey(selectedItem.getItem())) {

                FileManagerItemsFactory.updateIcon(
                        tablevDirContent,
                        selectedItem,
                        (selectedItem.isDirectory()) ? FileManagerItemsFactory.getDirectoryCutted() : FileManagerItemsFactory.getFileCutted());
            }
        }

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 0);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);

        threadLogicUIPool.execute(appViewRefresher);
    }

    public void pasteItems(ActionEvent actionEvent) {

        FXOptimizedItem destinationFolder = parentItem;

        if (actionEvent.getSource() instanceof MenuItem && ((MenuItem) actionEvent.getSource()).getId().equals("contextmenuPaste")) {
            if (tablevDirContent.getSelectionModel().getSelectedItem() != null) {
                destinationFolder = (FXOptimizedItem) tablevDirContent.getSelectionModel().getSelectedItem();
            }
        }

        ItemPaster itemPaster = new ItemPaster(destinationFolder);
        threadLogicUIPool.execute(itemPaster);
    }

    public void deleteItems(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = tablevDirContent.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }

        MainController.setCurrentStage(okCancelModalStage);

        okCancelModalController.initWarningModal("Selected items will be deleted! Are you sure?", actionOnDeleteItem, itemsCollection);
        showModalWindow(okCancelModalStage);
    }

    public void showHiddenItems(ActionEvent actionEvent) {
        showHiddenItemsState = cmiShowHiddenItems.isSelected();

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, treevItemsTree, 0);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);


        threadLogicUIPool.execute(appViewRefresher);
    }

    public void showAboutInfo(ActionEvent actionEvent) {
        okCancelModalController.initWarningModal("It's my test application!",actionOnAboutInfo,null);
        okCancelModalController.getButtonCancel().setVisible(false);
        showModalWindow(okCancelModalStage);
    }

    public void onItemsListClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            FXOptimizedItem selectedInList;
            HashSet<Item> selectedItems;

            try {

                selectedInList = (FXOptimizedItem) tablevDirContent.getSelectionModel().getSelectedItem();

                if (selectedInList.isDirectory()) {
                    parentItem = selectedInList;

                    try {
                        selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
                        selectedItemsList.clear();
                        for (Item selectedItem : selectedItems) {
                            selectedItemsList.add(new FXOptimizedItem(selectedItem));
                        }
                    } catch (Exception e) {
                        Map<Item, ItemConflicts> operationErrorsMap=new HashMap();
                        operationErrorsMap.put(parentItem.getValue(),ItemConflicts.SECURITY_ERROR);
                        onConflictsHandler(operationErrorsMap);
                        FileManagerItemsFactory.updateIcon(tablevDirContent,parentItem,FileManagerItemsFactory.getDirectoryUnavaible());
                    }


                    AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 2000L);
                    appViewRefresher.addListener(itemContentContainerListener);

                    threadLogicUIPool.execute(appViewRefresher);

                }
            } catch (NullPointerException e) {
                return;
            }
        }
    }

    public void getLevelUp(ActionEvent actionEvent) {

        if (parentItem.equals(FileManagerItemsFactory.getRoot())) {
            return;
        }

        HashSet<Item> selectedItems;

        parentItem = parentItem.getParentItem();

        try {
            selectedItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
            selectedItemsList.clear();
            for (Item selectedItem : selectedItems) {
                selectedItemsList.add(new FXOptimizedItem(selectedItem));
            }
        } catch (Exception e) {
            Map<Item, ItemConflicts> operationErrorsMap=new HashMap();
            operationErrorsMap.put(parentItem.getValue(),ItemConflicts.ITEM_NOT_FOUND);
            onConflictsHandler(operationErrorsMap);
            parentItem=FileManagerItemsFactory.getRoot();
        }
        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, tablevDirContent, 0);
        appViewRefresher.addListener(itemContentContainerListener);
        ;

        threadLogicUIPool.execute(appViewRefresher);
    }

    //additional methods
    protected void getOkCancelCloseModal() {
        okCancelModalController.initWarningModal("Now the application will be closed. Are you sure?", actionOnCloseApp, null);
        showModalWindow(okCancelModalStage);
    }
}
