package vcontroller;

import interfaces.IController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by kostyazxcvbn on 06.07.2017.
 */
public class StartScreenController {

    public Label labelStartScreenLoading;
    public ProgressBar progbStartScreenLoading;

    public void initialize() {
        loadData();
    }



    private void imitateLoading() {

        MainController.getMainAppPool().submit(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                double progressLevel=0;
                do{
                    progbStartScreenLoading.setProgress(progressLevel);
                    progressLevel=progressLevel+(double)(random.nextInt(5)+5)/100;
                    try {
                        Thread.sleep(random.nextInt(200)+100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }while(progressLevel<1);
                progbStartScreenLoading.setProgress(1);

                showMainWindow();
            }
        });
    }

    private void loadData() {
        imitateLoading();
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
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        //MainController.getCurrentStage().close();
    }
}

