package vcontroller;

import interfaces.IController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.FileManagerImpl;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by kostyazxcvbn on 06.07.2017.
 */
public class StartScreenController implements  IController{

    public ProgressBar progbStartScreenLoading;

    synchronized public void init() {
        imitateLoading();
    }

    private void imitateLoading() {

        Thread loadingImitator = new Thread(new Runnable() {
            @Override
            public void run() {

                Random random = new Random();
                double progressLevel = 0;
                do {
                    progbStartScreenLoading.setProgress(progressLevel);
                    progressLevel = progressLevel + (double) (random.nextInt(5) + 5) / 100;
                    try {
                        Thread.sleep(random.nextInt(200) + 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (progressLevel < 1);
                progbStartScreenLoading.setProgress(1);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        showMainWindow();
                    }
                });

            }
        });
        loadingImitator.start();
    }

    private void showMainWindow() {
        Stage stage = MainController.getPrimaryStage();
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("/fxml/MainAppWindow.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene=new Scene(root);
        stage.setTitle("The ZxcvbnFileNManager");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        MainController.getCurrentStage().close();
    }
}

