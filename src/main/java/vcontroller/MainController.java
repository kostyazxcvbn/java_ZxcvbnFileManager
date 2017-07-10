package vcontroller;/**
 * Created by kostyazxcvbn on 09.07.2017.
 */

import interfaces.IController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainController extends Application {

    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setPrimaryStage(Stage primaryStage) {
        MainController.primaryStage = primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        Parent root=null;
        Stage startStage;
        IController startScreenController;

        setPrimaryStage(primaryStage);
        FXMLLoader fxmlLoader=new FXMLLoader(getClass().getResource("/fxml/StartScreen.fxml"));
        try {
            root=fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startScreenController=fxmlLoader.getController();
        Scene scene=new Scene(root);
        startStage=new Stage(StageStyle.UNDECORATED);
        startStage.setScene(scene);
        startStage.sizeToScene();
        startStage.show();
        startScreenController.initialize();
    }
}
