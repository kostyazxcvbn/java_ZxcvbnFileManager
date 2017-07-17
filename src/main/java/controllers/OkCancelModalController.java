package controllers;

import interfaces.IOkCancelHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import model.Item;

import java.util.HashSet;

/**
 * Created by kostyazxcvbn on 16.07.2017.
 */
public class OkCancelModalController {

    @FXML
    private Label labelWarningMessage;
    @FXML
    private Button buttonCancel;
    @FXML
    private Button buttonOk;

    private IOkCancelHandler warningAction;
    HashSet<Item> selectedItems;

    public Button getButtonCancel() {
        return buttonCancel;
    }

    public void initWarningModal(String message, IOkCancelHandler warningAction, HashSet<Item>selectedItems) {
        labelWarningMessage.setText(message);
        this.warningAction=warningAction;
        this.selectedItems=selectedItems;
        this.buttonCancel.setVisible(true);
    }

    public void onButtonPressed(ActionEvent actionEvent) {
        if (((Button) actionEvent.getSource()).getId().equals("buttonOk")) {
            MainController.getThreadLogicUIPool().execute(new Runnable() {
                @Override
                public void run() {
                    warningAction.onButtonOkPressed(selectedItems, "");
                }
            });
        }
        if (((Button) actionEvent.getSource()).getId().equals("buttonCancel")) {
            MainController.getCurrentStage().hide();
        }
    }
}
