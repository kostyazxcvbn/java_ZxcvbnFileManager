package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by kostyazxcvbn on 06.07.2017.
 */
public class StartScreenController {

    @FXML
    private ProgressBar progbStartScreenLoading;

    private ExecutorService threadLogicUIPool;

    public void init() {
        imitateLoading();
    }

    public void imitateLoading() {
        threadLogicUIPool=MainController.getThreadLogicUIPool();
        Task<Void> loadingImitator = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Random random = new Random();
                double progressLevel = 0;
                do {
                    double finalProgressLevel = progressLevel;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            progbStartScreenLoading.setProgress(finalProgressLevel);
                        }
                    });

                    progressLevel = progressLevel + (double) (random.nextInt(5) + 5) / 100;
                    try {
                        Thread.sleep(random.nextInt(200) + 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (progressLevel < 1);
                progbStartScreenLoading.setProgress(1);

                Platform.runLater(() -> showMainWindow());
                return null;
            }
        };

        threadLogicUIPool.execute(loadingImitator);
    }

    private void showMainWindow() {
        Stage stage = MainController.getPrimaryStage();
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("/fxml/MainAppWindow.fxml"));
        } catch (IOException e) {
            runAppFatalErrorHandler();
        }
        Scene scene=new Scene(root);
        stage.setTitle("The ZxcvbnFileNManager");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        MainController.getCurrentStage().close();
    }

    private void runAppFatalErrorHandler() {
    }
}

