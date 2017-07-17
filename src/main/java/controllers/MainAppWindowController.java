package controllers;

import interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    private Label labelParentPath;
    @FXML
    private Button toolbNewFolder;
    @FXML
    private MenuItem miNewFolder;
    @FXML
    private MenuItem contextNewFolder;
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

    private FXMLLoader newFolderNameModalLoader;
    private Stage newFolderNameModalStage;
    private NewFolderNameModalController newFolderNameModalController;
    private Parent newFolderNameModalparent;

    private IOkCancelHandler actionOnDeleteItem;
    private IOkCancelHandler actionOnCloseApp;
    private IOkCancelHandler actionOnAboutInfo;
    private IRefreshingListener itemsTreeRefreshListener;
    private IRefreshingListener itemContentContainerListener;

    private Object lock;
    private IOkCancelHandler actionOnCreateFolder;

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
            Platform.runLater(()->labelParentPath.setText((parentItem.getValue().isRoot()?"\\root":parentItem.getValue().getPath().toAbsolutePath().toString())));
            countDownLatch.countDown();
            return null;
        }
    }

    private class GuiControlsStateSwitcher extends Task<Void> {

        private boolean stateCopy=false;
        private boolean stateCut=false;
        private boolean statePaste=false;
        private boolean stateDelete=false;
        private boolean stateBack=false;
        private boolean stateCreate;

        public GuiControlsStateSwitcher(boolean stateCopy, boolean stateCut, boolean statePaste, boolean stateDelete, boolean stateBack, boolean stateCreate) {
            this.stateCopy = !stateCopy;
            this.stateCut = !stateCut;
            this.statePaste = !statePaste;
            this.stateDelete = !stateDelete;
            this.stateBack = !stateBack;
            this.stateCreate =!stateCreate;
        }

        @Override
        protected Void call() throws Exception {
            Platform.runLater(()->toolbCopy.setDisable(stateCopy));
            Platform.runLater(()->contextCopy.setDisable(stateCopy));
            Platform.runLater(()->miCopy.setDisable(stateCopy));

            Platform.runLater(()->toolbCut.setDisable(stateCut));
            Platform.runLater(()->contextCut.setDisable(stateCut));
            Platform.runLater(()->miCut.setDisable(stateCut));

            Platform.runLater(()-> toolbPaste.setDisable(statePaste));
            Platform.runLater(()->contextPaste.setDisable(statePaste));
            Platform.runLater(()->miPaste.setDisable(statePaste));

            Platform.runLater(()->toolbDelete.setDisable(stateDelete));
            Platform.runLater(()->contextDelete.setDisable(stateDelete));
            Platform.runLater(()->miDelete.setDisable(stateDelete));

            Platform.runLater(()->toolbNewFolder.setDisable(stateCreate));
            Platform.runLater(()->contextNewFolder.setDisable(stateCreate));
            Platform.runLater(()->miNewFolder.setDisable(stateCreate));

            Platform.runLater(()->toolbUp.setDisable(stateBack));
            Platform.runLater(()->contextBack.setDisable(stateBack));

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
            appViewRefresher.addListener(itemsTreeRefreshListener);
            appViewRefresher.addListener(itemContentContainerListener);
            threadLogicUIPool.execute(appViewRefresher);

            return null;
        }
    }

    private class ItemCreator extends Task<Void> {

        Map<Item, ItemConflicts> operationErrorsMap;
        FXOptimizedItem destination;

        public ItemCreator(FXOptimizedItem destination) {
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
        initNewFolderNametModal();

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
        showHiddenItemsState = cmiShowHiddenItems.isSelected();
        toolbShowHiddenItems.setSelected(showHiddenItemsState);
    }

    private void initImpls() {

        actionOnCloseApp = new IOkCancelHandler() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection, String path) {
                onCloseAppHandler();

            }

            @Override
            public void onButtonOkPressed(String destinationPath, String newFolderName) {

            }
        };

        actionOnAboutInfo = new IOkCancelHandler() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection, String path) {
                Platform.runLater(() -> MainController.getCurrentStage().hide());
            }

            @Override
            public void onButtonOkPressed(String destinationPath, String newFolderName) {

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

        actionOnDeleteItem = new IOkCancelHandler() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection, String path) {
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

            @Override
            public void onButtonOkPressed(String destinationPath, String newFolderName) {

            }
        };

        actionOnCreateFolder = new IOkCancelHandler() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection, String path) {

            }

            @Override
            public void onButtonOkPressed(String destinationPath, String newFolderName) {
                Map<Item, ItemConflicts> operationErrorsMap=new HashMap<>();
                Item newDirectory = fileManager.createDirectory(destinationPath,newFolderName);
                if (newDirectory == null) {
                    operationErrorsMap.put(newDirectory, ItemConflicts.CANT_CREATE_ITEM);
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

    private void initNewFolderNametModal() {
        if (newFolderNameModalLoader == null) {
            newFolderNameModalLoader = new FXMLLoader(getClass().getResource("/fxml/NewFolderNameModal.fxml"));
            newFolderNameModalStage = new Stage();

            try {
                newFolderNameModalparent = newFolderNameModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(newFolderNameModalparent);
            newFolderNameModalStage.setTitle("New folder...");
            newFolderNameModalStage.setScene(scene);
            newFolderNameModalStage.sizeToScene();
            newFolderNameModalStage.setResizable(false);
            newFolderNameModalStage.initModality(Modality.WINDOW_MODAL);
            newFolderNameModalStage.initOwner(MainController.getPrimaryStage());
            newFolderNameModalController = newFolderNameModalLoader.getController();
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
        tablevDirContent.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (event.getClickCount() == 2) {
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
                    return;
                }
                if (event.getClickCount() == 1) {
                    FXOptimizedItem selectedItem=(FXOptimizedItem)tablevDirContent.getSelectionModel().getSelectedItem();
                    if (selectedItem != null) {
                        if (selectedItem.isDirectory()) {
                            if (!selectedItem.getItem().isRootStorage()) {
                                guiControlsStateHandler(GuiControlsState.FOLDER_SELECTED);
                            } else {
                                guiControlsStateHandler(GuiControlsState.ROOT_LEVEL);
                            }

                        } else {
                            guiControlsStateHandler(GuiControlsState.FILE_SELECTED);
                        }
                    }
                }else{
                    guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
                }
            }
        });
        selectedItemsList.addListener(new ListChangeListener<FXOptimizedItem>() {
            @Override
            public void onChanged(Change<? extends FXOptimizedItem> c) {
                if (parentItem.getItem().isRoot()) {
                    guiControlsStateHandler(GuiControlsState.ROOT_LEVEL);
                    return;
                }
                if (!parentItem.getItem().isRoot()) {
                    guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
                    return;
                }
                if (selectedItemsList.isEmpty()) {
                    guiControlsStateHandler(GuiControlsState.EMPTY_CONTENT);
                    return;
                }
            }
        });
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

               Task<Void> guiStateSwitcher = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                boolean stateCopy=false;
                boolean stateCut=false;
                boolean stateDelete=false;
                boolean stateBack=false;
                boolean statePaste=false;
                boolean stateCreate=false;

                switch (guiControlsState) {
                    case ROOT_LEVEL:{
                        stateCopy=false;
                        stateCut=false;
                        statePaste=false;
                        stateDelete=false;
                        stateBack=true;
                        stateCreate=false;
                        break;
                    }
                    case NOTHING_SELECTED:
                    case EMPTY_CONTENT:{
                        stateCopy=false;
                        stateCut=false;
                        statePaste=(fileManager.getBuffer().isEmpty())?false:true;
                        stateDelete=false;
                        stateBack=true;
                        stateCreate=true;
                        break;
                    }
                    case FILE_SELECTED:{
                        stateCopy=true;
                        stateCut=true;
                        statePaste=false;
                        stateDelete=true;
                        stateBack=true;
                        stateCreate=false;
                        break;
                    }
                    case FOLDER_SELECTED:{
                        stateCopy=true;
                        stateCut=true;
                        statePaste=(fileManager.getBuffer().isEmpty())?false:true;
                        stateDelete=true;
                        stateBack=true;
                        stateCreate=true;
                        break;
                    }
                    default:{
                        break;
                    }
                }

                threadLogicUIPool.execute(new GuiControlsStateSwitcher(stateCopy,stateCut,statePaste,stateDelete,stateBack,stateCreate));

                return null;
            }
        };

        threadLogicUIPool.execute(guiStateSwitcher);
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

        guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
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

        guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
    }

    public void pasteItems(ActionEvent actionEvent) {

        FXOptimizedItem destinationFolder = parentItem;

        if (actionEvent.getSource() instanceof MenuItem && ((MenuItem) actionEvent.getSource()).getId().equals("contextPaste")) {
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
        toolbShowHiddenItems.setSelected(showHiddenItemsState);

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

    public void onButtonPressedshowHiddenItems(ActionEvent actionEvent) {
        showHiddenItemsState = toolbShowHiddenItems.isSelected();
        cmiShowHiddenItems.setSelected(showHiddenItemsState);

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, treevItemsTree, 0);
        appViewRefresher.addListener(itemsTreeRefreshListener);
        appViewRefresher.addListener(itemContentContainerListener);
        threadLogicUIPool.execute(appViewRefresher);
    }

    public void onCreateNewFolderPressed(ActionEvent actionEvent) {
        FXOptimizedItem destinationFolder = parentItem;

        if (actionEvent.getSource() instanceof MenuItem && ((MenuItem) actionEvent.getSource()).getId().equals("contextNewFolder")) {
            if (tablevDirContent.getSelectionModel().getSelectedItem() != null) {
                destinationFolder = (FXOptimizedItem) tablevDirContent.getSelectionModel().getSelectedItem();
            }
        }

        newFolderNameModalController.init(destinationFolder, actionOnCreateFolder);
        showModalWindow(newFolderNameModalStage);
    }

    //additional methods
    protected void getOkCancelCloseModal() {
        okCancelModalController.initWarningModal("Now the application will be closed. Are you sure?", actionOnCloseApp, null);
        showModalWindow(okCancelModalStage);
    }
}
