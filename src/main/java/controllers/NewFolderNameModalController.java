package controllers;

import interfaces.IOkCancelHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.util.regex.Pattern;
import helpers.FileManagerItemsFactory.*;

/**
 * Created by user on 17.07.2017.
 */
public class NewFolderNameModalController {
    @FXML
    private Button buttonOk;
    @FXML
    private TextField textfNewFoldername;

    private String destinationFolderPath;
    private IOkCancelHandler warningAction;

    public void initialize() {
        Pattern pattern = Pattern.compile("[a-zA-Zа-яА-Я, 0-9]+|$");
        textfNewFoldername.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!pattern.matcher(newValue).matches()) {
                    textfNewFoldername.setText(oldValue);
                }
                if(textfNewFoldername.getText().isEmpty()){
                    buttonOk.setDisable(true);
                }
                if(!textfNewFoldername.getText().isEmpty()){
                    buttonOk.setDisable(false);
                }
            }
        });
    }

    public void init(FXOptimizedItem destinationFolder, IOkCancelHandler warningAction) {
        textfNewFoldername.clear();
        buttonOk.setDisable(true);

        this.destinationFolderPath = destinationFolder.getItem().getPath().toAbsolutePath().toString();
        this.warningAction=warningAction;

    }

    public void onButtonPressed(ActionEvent actionEvent) {
        if (((Button) actionEvent.getSource()).getId().equals("buttonOk")) {
            MainController.getThreadLogicUIPool().execute(new Runnable() {
                @Override
                public void run() {
                    warningAction.onButtonOkPressed(destinationFolderPath, textfNewFoldername.getText());
                }
            });
        }
        if (((Button) actionEvent.getSource()).getId().equals("buttonCancel")) {
            MainController.getCurrentStage().hide();
        }
    }
}
