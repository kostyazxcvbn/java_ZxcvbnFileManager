package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import model.AppEnums.NameConflictState;

import java.util.concurrent.ExecutorService;

/**
 * Created by kostyazxcvbn on 15.07.2017.
 */
public class ItemNameConflictModalController {

    @FXML
    private RadioButton radiobReplaceExisting;
    @FXML
    private RadioButton radiobNotReplaceExisting;
    @FXML
    private CheckBox checkbForAllItems;

    private ExecutorService threadLogicUIPool;
    private Object lock;
    private NameConflictState nameConflictState;


    public void initialize() {
        threadLogicUIPool=MainController.getThreadLogicUIPool();
        nameConflictState=NameConflictState.NO_CONFLICTS;
    }

    public void init(NameConflictState nameConflictState) {
        radiobNotReplaceExisting.setSelected(true);
        checkbForAllItems.setSelected(false);
        this.nameConflictState = nameConflictState;
    }

    public NameConflictState getNameConflictState() {
        NameConflictState stateToReturnLink=nameConflictState;
        NameConflictState stateToReturn=stateToReturnLink;
        switch (nameConflictState) {
            case NOT_REPLACE:
            case REPLACE_EXISTING:{
                nameConflictState=NameConflictState.NO_CONFLICTS;
                break;
            }
            case NOT_REPLACE_ALL:
            case REPLACE_EXISTING_ALL:{
                break;
            }
        }

        return stateToReturn;
    }

    public void setWaitingResultLock(Object lock) {
        this.lock = lock;
    }

    public void onOKPressed(ActionEvent actionEvent) {

        if (radiobReplaceExisting.isSelected()) {
            nameConflictState=(checkbForAllItems.isSelected())?NameConflictState.REPLACE_EXISTING_ALL:NameConflictState.REPLACE_EXISTING;
        }
        if (radiobNotReplaceExisting.isSelected()) {
            nameConflictState=(checkbForAllItems.isSelected())?NameConflictState.NOT_REPLACE_ALL:NameConflictState.NOT_REPLACE;
        }

        threadLogicUIPool.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        MainController.getCurrentStage().hide();
    }
}
