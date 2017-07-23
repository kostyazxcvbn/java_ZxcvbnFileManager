package controllers;

import helpers.AppViewRefresher;
import helpers.FileManagerItemsFactory;
import interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static model.AppEnums.*;
import static helpers.FileManagerItemsFactory.FXOptimizedItem;

/**
 * Created by kostyazxcvbn on 06.07.2017.
 */

public class MainAppWindowController implements Initializable {

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
    private TableView innerItemsList;
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
    private ObservableList<FXOptimizedItem> innerItems;
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

    private ExecutorService itemsOperationsPool;

    private ResourceBundle locales;
    private ResourceBundle devResources;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.locales =resources;
        this.devResources=MainController.getDevResources();
        initialize();
    }

    //inner runnable subtasks
    private class SubfoldersLoader extends Task<Void> {

        CountDownLatch countDownLatch;
        FXOptimizedItem parentItem;
        ObservableList<FXOptimizedItem> innerItemsList;

        public SubfoldersLoader(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> innerItemsList, CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
            this.parentItem = parentItem;
            this.innerItemsList = innerItemsList;
        }

        @Override
        protected Void call() throws Exception {

            ObservableList<TreeItem<Item>> childrenDirectories = parentItem.getChildren();
            childrenDirectories.clear();

            childrenDirectories.addAll(innerItemsList.filtered(item -> (item.isDirectory() && (showHiddenItemsState | !item.isHidden()))?true:false));
            childrenDirectories.sort((o1, o2) -> (o1.getValue().getName().toUpperCase().compareTo(o2.getValue().getName().toUpperCase())));
            countDownLatch.countDown();
            return null;
        }
    }

    private class ItemContentLoader extends Task<Void> {

        FXOptimizedItem item;
        CountDownLatch countDownLatch;
        ObservableList<FXOptimizedItem> innerItemsList;

        public ItemContentLoader(FXOptimizedItem item, ObservableList<FXOptimizedItem> innerItemsList, CountDownLatch countDownLatch) {
            this.item = item;
            this.countDownLatch = countDownLatch;
            this.innerItemsList = innerItemsList;
        }

        private int toInt(boolean value) {
            return (value) ? 1 : 0;
        }

        @Override
        protected Void call() throws Exception {
            ObservableList<FXOptimizedItem> innerItemsInView = MainAppWindowController.this.innerItemsList.getItems();
            innerItemsInView.clear();

            if (!innerItemsList.isEmpty()) {
                innerItemsInView.addAll((showHiddenItemsState) ? innerItemsList : innerItemsList.filtered(fxOptimizedItem -> !fxOptimizedItem.isHidden()));
                innerItemsInView.sort((o1, o2) -> {
                    int o1ToInt = toInt(o1.isDirectory());
                    int o2ToInt = toInt(o2.isDirectory());
                    if (o1ToInt == o2ToInt) {
                        return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                    }
                    return o2ToInt - o1ToInt;
                });
            }
            Platform.runLater(()->labelParentPath.setText((parentItem.getValue().isRoot()?parentItem.getName():parentItem.getValue().getPath().toAbsolutePath().toString())));
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

            if(fileManager.isCutOperation())
                fileManager.getBuffer().clear();

            if (!operationErrorsMap.isEmpty()) {
                onConflictsHandler(operationErrorsMap);
            }

            refreshItems(parentItem, false, 0, itemsTreeRefreshListener, itemContentContainerListener);

            return null;
        }
    }

    //initialization's methods
    public void initialize() {

        fileManager = FileManagerImpl.getInstance();

        threadLogicUIPool = MainController.getThreadLogicUIPool();
        itemsOperationsPool= Executors.newCachedThreadPool();

        innerItems = FXCollections.observableArrayList();

        lock = new Object();

        initButtons();
        initImpls();

        initItemNameConflictModal();
        initOkCancelModal();
        initOperationsConflictModal();
        initNewFolderNameModal();

        initItemsTree();
        initItemContentView();

        MainController.getPrimaryStage().setOnCloseRequest(exit -> {
            exit.consume();
            getOkCancelCloseModal();
        });

        guiControlsStateHandler(GuiControlsState.ROOT_LEVEL);
        refreshItems(parentItem, true, 2000, itemsTreeRefreshListener, itemContentContainerListener);
    }

    private void initButtons() {

        Task<Void> buttonsImageLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                toolbNewFolder.setTooltip(new Tooltip(locales.getString("tooltipCreate")));
                toolbCopy.setTooltip(new Tooltip(locales.getString("tooltipCopy")));
                toolbCut.setTooltip(new Tooltip(locales.getString("tooltipCut")));
                toolbDelete.setTooltip(new Tooltip(locales.getString("tooltipDelete")));
                toolbPaste.setTooltip(new Tooltip(locales.getString("tooltipPaste")));
                toolbShowHiddenItems.setTooltip(new Tooltip(locales.getString("tooltipShowHide")));
                toolbUp.setTooltip(new Tooltip(locales.getString("tooltipBack")));

                Image imageToolbNewFolder = new Image(getClass().getResourceAsStream(devResources.getString("imagePathCreate")));
                Image imageToolbCopy = new Image(getClass().getResourceAsStream(devResources.getString("imagePathCopy")));
                Image imageToolbCut = new Image(getClass().getResourceAsStream(devResources.getString("imagePathCut")));
                Image imageToolbDelete = new Image(getClass().getResourceAsStream(devResources.getString("imagePathDelete")));
                Image imageToolbPaste = new Image(getClass().getResourceAsStream(devResources.getString("imagePathPaste")));
                Image imageToolbShowHiddenItems = new Image(getClass().getResourceAsStream(devResources.getString("imagePathShowHide")));
                Image imageToolbUp = new Image(getClass().getResourceAsStream(devResources.getString("imagePathBack")));

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        toolbNewFolder.setGraphic(new ImageView(imageToolbNewFolder));
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
                itemsOperationsPool.execute(new SubfoldersLoader(parentItem, innerItems, countDownLatch));
            }
        };

        itemContentContainerListener = new IRefreshingListener() {
            @Override
            public void refresh(CountDownLatch countDownLatch) {
                itemsOperationsPool.execute(new ItemContentLoader(parentItem, innerItems, countDownLatch));
            }
        };

        actionOnDeleteItem = new IOkCancelHandler() {
            @Override
            public void onButtonOkPressed(HashSet<Item> itemsCollection, String path) {
                Map<Item, ItemConflicts> operationErrorsMap = fileManager.deleteItems(itemsCollection);

                if (!operationErrorsMap.isEmpty()) {
                    onConflictsHandler(operationErrorsMap);
                }
                Platform.runLater(() -> MainController.getCurrentStage().hide());
                refreshItems(parentItem, false, 0, itemsTreeRefreshListener,itemContentContainerListener);
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

                Platform.runLater(() -> MainController.getCurrentStage().hide());
                if (newDirectory == null) {
                    operationErrorsMap.put(new Item(Paths.get(destinationPath, newFolderName)), ItemConflicts.CANT_CREATE_ITEM);
                    onConflictsHandler(operationErrorsMap);
                }
                refreshItems(parentItem, false, 0, itemsTreeRefreshListener, itemContentContainerListener);

            }
        };
    }

    private void initItemNameConflictModal() {
        if (itemNameConflictModalLoader == null) {
            itemNameConflictModalLoader = new FXMLLoader(getClass().getResource(devResources.getString("fxmlItemNameConflictModal")));
            itemNameConflictModalStage = new Stage();
            itemNameConflictModalLoader.setResources(MainController.getLocales());

            try {
                itemNameConflictModalparent = itemNameConflictModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(itemNameConflictModalparent);
            itemNameConflictModalStage.setTitle(locales.getString("titleNameConflictModal"));
            itemNameConflictModalStage.setScene(scene);
            itemNameConflictModalStage.sizeToScene();
            itemNameConflictModalStage.setResizable(false);
            itemNameConflictModalStage.initModality(Modality.WINDOW_MODAL);
            itemNameConflictModalStage.initOwner(MainController.getPrimaryStage());
            itemNameConflictModalController = itemNameConflictModalLoader.getController();
            itemNameConflictModalController.setWaitingResultLock(lock);
            scene.getWindow().setOnCloseRequest(event -> event.consume());
        }
    }

    private void initOkCancelModal() {
        if (okCancelModalLoader == null) {
            okCancelModalLoader = new FXMLLoader(getClass().getResource(devResources.getString("fxmlOkCancelModal")));
            okCancelModalStage = new Stage();
            okCancelModalLoader.setResources(MainController.getLocales());

            try {
                okCancelModalparent = okCancelModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(okCancelModalparent);
            okCancelModalStage.setTitle(locales.getString("titleOkCancelModal"));
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
            operationsConflictModalLoader = new FXMLLoader(getClass().getResource(devResources.getString("fxmlOperationsConflictModal")));
            operationsConflictModalStage = new Stage();
            operationsConflictModalLoader.setResources(MainController.getLocales());

            try {
                operationsConflictModalparent = operationsConflictModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(operationsConflictModalparent);
            operationsConflictModalStage.setTitle(locales.getString("titleOperationsConflictsModal"));
            operationsConflictModalStage.setScene(scene);
            operationsConflictModalStage.sizeToScene();
            operationsConflictModalStage.setResizable(false);
            operationsConflictModalStage.initModality(Modality.WINDOW_MODAL);
            operationsConflictModalStage.initOwner(MainController.getPrimaryStage());
            operationsConflictModalController = operationsConflictModalLoader.getController();
        }
    }

    private void initNewFolderNameModal() {
        if (newFolderNameModalLoader == null) {
            newFolderNameModalLoader = new FXMLLoader(getClass().getResource(devResources.getString("fxmlNewFolderNameModal")));
            newFolderNameModalStage = new Stage();
            newFolderNameModalLoader.setResources(MainController.getLocales());

            try {
                newFolderNameModalparent = newFolderNameModalLoader.load();
            } catch (Exception e) {
                onFatalErrorHandler();
            }

            Scene scene = new Scene(newFolderNameModalparent);
            newFolderNameModalStage.setTitle(locales.getString("titleCreateFolderModal"));
            newFolderNameModalStage.setScene(scene);
            newFolderNameModalStage.sizeToScene();
            newFolderNameModalStage.setResizable(false);
            newFolderNameModalStage.initModality(Modality.WINDOW_MODAL);
            newFolderNameModalStage.initOwner(MainController.getPrimaryStage());
            newFolderNameModalController = newFolderNameModalLoader.getController();
        }
    }

    private void initItemsTree() {

        parentItem = FileManagerItemsFactory.getRoot();
        treevItemsTree.setRoot(parentItem);
        treevItemsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void initItemContentView() {
        innerItemsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        innerItems.addListener(new ListChangeListener<FXOptimizedItem>() {
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
                if (innerItems.isEmpty()) {
                    guiControlsStateHandler(GuiControlsState.EMPTY_CONTENT);
                    return;
                }
            }
        });
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
                        stateBack=false;
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
                        statePaste=(innerItemsList.getSelectionModel().getSelectedItems().size()==1)?false:true;
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
        okCancelModalController.initWarningModal(locales.getString("textFatalError"),actionOnCloseApp,null);
        okCancelModalController.getButtonCancel().setVisible(false);
        showModalWindow(okCancelModalStage);
    }

    //gui reactions
    public void onClickItemsTree(MouseEvent event) {

        FXOptimizedItem tempSelectedLink = parentItem;
        FXOptimizedItem tempSelected = tempSelectedLink;
        boolean isIconWillChanged=false;
        long delayImitation=0;

        parentItem = (FXOptimizedItem) treevItemsTree.getSelectionModel().getSelectedItem();
        if (event.getClickCount() == 1) {

            if (parentItem == null) {
                parentItem = tempSelected;
            }

            if (parentItem.isLeaf() || parentItem.isExpanded()) {
                isIconWillChanged = true;
                parentItem.getChildren().clear();
                delayImitation=2000;
            }
            refreshItems(parentItem, isIconWillChanged, delayImitation, itemsTreeRefreshListener, itemContentContainerListener);
        }
    }

    public void onClickItemsTable(MouseEvent event) {
        if (event.getClickCount() == 2) {
            FXOptimizedItem selectedInList;
            try {

                selectedInList = (FXOptimizedItem) innerItemsList.getSelectionModel().getSelectedItem();

                if (selectedInList.isDirectory()) {
                    parentItem = selectedInList;

                    refreshItems(parentItem, true, 2000, itemContentContainerListener);
                }
            } catch (NullPointerException e) {
                return;
            }
            return;
        }
        if (event.getClickCount() == 1) {
            FXOptimizedItem selectedItem=(FXOptimizedItem) innerItemsList.getSelectionModel().getSelectedItem();
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

    public void onClickClose(ActionEvent actionEvent) {
        getOkCancelCloseModal();
    }

    public void onClickCopy(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = innerItemsList.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }
        Map<Item, ItemConflicts> operationErrorsMap = fileManager.copyItemsToBuffer(itemsCollection);
        if (!operationErrorsMap.isEmpty()) {
            onConflictsHandler(operationErrorsMap);
        }

        refreshItems(parentItem, false, 0, itemContentContainerListener);

        guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
    }

    public void onClickCut(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = innerItemsList.getSelectionModel().getSelectedItems();
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
                selectedItem.setIcon((selectedItem.isDirectory()) ? FileManagerItemsFactory.getDirectoryCutted() : FileManagerItemsFactory.getFileCutted());
            }
        }

        refreshItems(parentItem, true, 0, itemsTreeRefreshListener, itemContentContainerListener);

        guiControlsStateHandler(GuiControlsState.NOTHING_SELECTED);
    }

    public void onClickPaste(ActionEvent actionEvent) {

        FXOptimizedItem destinationFolder = parentItem;

        if (innerItemsList.getSelectionModel().getSelectedItem() != null && innerItemsList.getSelectionModel().getSelectedItems().size()==1) {
            destinationFolder = (FXOptimizedItem) innerItemsList.getSelectionModel().getSelectedItem();
        }

        ItemPaster itemPaster = new ItemPaster(destinationFolder);
        itemsOperationsPool.execute(itemPaster);
    }

    public void onClickDelete(ActionEvent actionEvent) {

        ObservableList<FXOptimizedItem> selectedItems = innerItemsList.getSelectionModel().getSelectedItems();
        HashSet<Item> itemsCollection = new HashSet<>(selectedItems.size());

        for (FXOptimizedItem selectedItem : selectedItems) {
            itemsCollection.add(selectedItem.getItem());
        }

        MainController.setCurrentStage(okCancelModalStage);

        okCancelModalController.initWarningModal(locales.getString("textDeleteItems"), actionOnDeleteItem, itemsCollection);
        showModalWindow(okCancelModalStage);
    }

    public void onClickMenuShowHide(ActionEvent actionEvent) {

        showHiddenItemsState = cmiShowHiddenItems.isSelected();
        toolbShowHiddenItems.setSelected(showHiddenItemsState);

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, false, 0,itemsTreeRefreshListener,itemContentContainerListener);
        itemsOperationsPool.execute(appViewRefresher);
    }

    public void onClickAbout(ActionEvent actionEvent) {
        okCancelModalController.initWarningModal(locales.getString("textAbout"),actionOnAboutInfo,null);
        okCancelModalController.getButtonCancel().setVisible(false);
        showModalWindow(okCancelModalStage);
    }

    public void onClickBack(ActionEvent actionEvent) {

        if (parentItem.equals(FileManagerItemsFactory.getRoot())) {
            return;
        }
        parentItem = parentItem.getParentItem();

        refreshItems(parentItem, false, 0, itemContentContainerListener);
    }

    public void onClickButtonShowHide(ActionEvent actionEvent) {
        showHiddenItemsState = toolbShowHiddenItems.isSelected();
        cmiShowHiddenItems.setSelected(showHiddenItemsState);

        AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, false, 0, itemsTreeRefreshListener,itemContentContainerListener);
        itemsOperationsPool.execute(appViewRefresher);
    }

    public void onClickCreateFolder(ActionEvent actionEvent) {
        FXOptimizedItem destinationFolder = parentItem;

        if (actionEvent.getSource() instanceof MenuItem && ((MenuItem) actionEvent.getSource()).getId().equals("contextNewFolder")) {
            if (innerItemsList.getSelectionModel().getSelectedItem() != null) {
                destinationFolder = (FXOptimizedItem) innerItemsList.getSelectionModel().getSelectedItem();
            }
        }

        newFolderNameModalController.init(destinationFolder, actionOnCreateFolder);
        showModalWindow(newFolderNameModalStage);
    }

    //additional methods
    protected void getOkCancelCloseModal() {
        okCancelModalController.initWarningModal(locales.getString("textCloseApp"), actionOnCloseApp, null);
        showModalWindow(okCancelModalStage);
    }

    private void refreshItems(FXOptimizedItem parentItem, boolean isIconWillChanged, long delayImitationMs, IRefreshingListener...refreshers) {
        threadLogicUIPool.execute(()->{
            HashSet<Item> innerItems;
            try {
                innerItems = (HashSet<Item>) fileManager.getContent(parentItem.getValue());
                this.innerItems.clear();
                for (Item innerItem : innerItems) {
                    this.innerItems.add(new FXOptimizedItem(innerItem));
                }
                AppViewRefresher appViewRefresher = new AppViewRefresher(parentItem, isIconWillChanged, delayImitationMs,refreshers);
                itemsOperationsPool.execute(appViewRefresher);

            } catch (Exception e) {
                Map<Item, ItemConflicts> operationErrorsMap=new HashMap();
                operationErrorsMap.put(parentItem.getValue(), ItemConflicts.ACCESS_ERROR);
                onConflictsHandler(operationErrorsMap);
                parentItem.setIcon(FileManagerItemsFactory.getDirectoryUnavaible());
                this.innerItems.clear();
            }
        });
    }

    private void showModalWindow(Stage modalWindowStage) {
        MainController.setCurrentStage(modalWindowStage);
        modalWindowStage.show();
    }
}
