package vcontroller;

import interfaces.IFileManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.FileManagerImpl;
import model.Item;

import java.util.Comparator;
import java.util.HashSet;

import static model.AppEnums.*;


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

    private IFileManager fileManager;
    private TreeItem<Item> selectedItem;

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

    public NameConflictState onConflict() {
        return null;
    }

    public void initialize(){

        fileManager=FileManagerImpl.getInstance();

        initButtons();
        initItemsView();

    }

    private void initItemsView() {
        initItemsTree();
        initItemContentView();
        getItemContent(ItemViewFactory.getRoot());
    }

    private void initItemContentView() {
        tablevDirContent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void getItemContent(TreeItem<Item> item) {
        ImageView tempImageView = (ImageView)item.getGraphic();
        item.setGraphic(ItemViewFactory.getItemWaiting()); //TODO image loading

        HashSet<Item>innerItems = (HashSet)fileManager.getContent(item.getValue(), false);
        if(cmiShowHiddenItems.isSelected()){

        }

        if (innerItems == null) { //TODO make Exception
            runFatalErrorHandler();
        }

        ObservableList<Item> innerItemsInView = tablevDirContent.getItems();
        innerItemsInView.addAll(innerItems);

        item.setGraphic(tempImageView); //TODO image loading,  make a method in the Item class
    }

    private void initItemsTree() {

        treevItemsTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        HashSet<Item>rootItems = (HashSet)fileManager.getContent(ItemViewFactory.getRoot().getValue(),true);

        if (rootItems == null) { //TODO make Exception
            runFatalErrorHandler();
        }

        treevItemsTree.setRoot(ItemViewFactory.getRoot());
        for (Item item : rootItems) {
            treevItemsTree.getRoot().getChildren().add(ItemViewFactory.getTreeItem(item));
        }
        treevItemsTree.getRoot().getChildren().sort(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return (((TreeItem<Item>)o1).getValue().getPath().toAbsolutePath().toString().compareTo(((TreeItem<Item>)o2).getValue().getPath().toAbsolutePath().toString()));
            }
        });

    }

    private void runFatalErrorHandler() {
    }

    private void initButtons() {
        Image image;

        image= new Image(getClass().getResourceAsStream("/img/iconCopy.png"));
        toolbCopy.setGraphic(new ImageView(image));

        image= new Image(getClass().getResourceAsStream("/img/iconCut.png"));
        toolbCut.setGraphic(new ImageView(image));

        image= new Image(getClass().getResourceAsStream("/img/iconRemove.png"));
        toolbDelete.setGraphic(new ImageView(image));

        image= new Image(getClass().getResourceAsStream("/img/iconRename.png"));
        toolbRename.setGraphic(new ImageView(image));

        image= new Image(getClass().getResourceAsStream("/img/iconPaste.png"));
        toolbPaste.setGraphic(new ImageView(image));

        image= new Image(getClass().getResourceAsStream("/img/iconHide.png"));
        toolbShowHiddenItems.setGraphic(new ImageView(image));
    }
}
