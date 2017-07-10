package vcontroller;

import com.sun.imageio.plugins.png.PNGImageReader;
import com.sun.javafx.iio.ImageFrame;
import interfaces.IConflictListener;
import interfaces.IFileManager;
import javafx.application.Application;
import javafx.collections.ObservableSet;
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
import javafx.stage.Stage;
import model.FileManagerImpl;
import model.Item;

import javax.imageio.ImageReader;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Item selectedItem;

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
        initItemContent();
    }

    private void initItemContent() {

    }

    private void initItemsTree() {
        HashSet<Item>rootItems = (HashSet)fileManager.getContent(null,true);
        if (rootItems == null) {
            runFatalErrorHandler();
        }

        treevItemsTree.setRoot(TreeItemFactory.getRoot());
        for (Item item : rootItems) {
            treevItemsTree.getRoot().getChildren().add(TreeItemFactory.getTreeItem(item));
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
