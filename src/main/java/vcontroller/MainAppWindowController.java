package vcontroller;

import com.sun.imageio.plugins.png.PNGImageReader;
import com.sun.javafx.iio.ImageFrame;
import interfaces.IConflictListener;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageReader;

import static model.AppEnums.*;


/**
 * Created by kostyazxcvbn on 06.07.2017.
 */

public class MainAppWindowController extends Application implements IConflictListener {

    public CheckMenuItem cmiShowHiddenItems;
    public TreeView treevItemsTree;
    public TableView tablevDirContent;
    public Button toolbCopy;

    public Button toolbCut;
    public Button toolbPaste;
    public Button toolbDelete;
    public Button toolbRename;
    public ToggleButton toolbShowHiddenItems;

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


    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainAppWindow.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setTitle("The ZxcvbnFileNManager");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        //toolbCut.setGraphic(new ImageView(getClass().getResource("/img/iconCut.png").toString()));
       // toolbDelete.setGraphic(new ImageView("/resources/img/iconDelete.png"));
        //toolbPaste.setGraphic(new ImageView("/resources/img/iconPaste.png"));
        //toolbShowHiddenItems.setGraphic(new ImageView("/resources/img/iconHide.png"));








    }

    public static void main(String[] args) {
        launch(args);
    }
}
