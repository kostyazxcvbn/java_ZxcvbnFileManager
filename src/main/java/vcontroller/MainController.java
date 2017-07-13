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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController extends Application {

    private static Stage primaryStage;
    private static Stage currentStage;
    private static ExecutorService threadLogicUIPool;

    public static ExecutorService getThreadLogicUIPool() {
        return threadLogicUIPool;
    }

    public static void setThreadLogicUIPool(ExecutorService threadLogicUIPool) {
        if (MainController.threadLogicUIPool == null) {
            MainController.threadLogicUIPool = threadLogicUIPool;
        }
    }

    public static Stage getCurrentStage() {
        return currentStage;
    }

    public static void setCurrentStage(Stage currentStage) {
        MainController.currentStage = currentStage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void setPrimaryStage(Stage primaryStage) {
        if(MainController.primaryStage==null){
            MainController.primaryStage = primaryStage;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        Stage startStage = new Stage(StageStyle.UNDECORATED);;

        setPrimaryStage(primaryStage);
        setThreadLogicUIPool(Executors.newCachedThreadPool());
        FXMLLoader fxmlLoader=null;

        Parent root = null;
        try {
            fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/StartScreen.fxml"));
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene=new Scene(root);
        startStage.setScene(scene);
        startStage.sizeToScene();
        startStage.show();
        setCurrentStage(startStage);
        IController c=fxmlLoader.getController();
        c.init();

    }
}
