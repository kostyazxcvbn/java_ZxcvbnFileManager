package controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import model.AppEnums.*;
import model.Item;

import java.util.Map;

/**
 * Created by kostyazxcvbn on 08.07.2017.
 */
public class OperationsConflictModalController {
    @FXML
    private TableView tablevItemsConflictContainer;
    @FXML
    private TableColumn columnItemsWithConflicts;
    @FXML
    private TableColumn columnDescription;
    @FXML
    private Button buttonOk;

    private ObservableList<Map.Entry<Item,ItemConflicts>> conflictsList;

    public void initialize() {

        conflictsList=tablevItemsConflictContainer.getItems();

        columnItemsWithConflicts.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<Item, ItemConflicts>, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<Item, ItemConflicts>, String> p) {
                return new SimpleStringProperty(p.getValue().getKey().getPath().toString());}});


        columnDescription.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<Item, ItemConflicts>, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<Item, ItemConflicts>, String> p) {
                return new SimpleStringProperty(p.getValue().getValue().toString());}});
    }

    public void init(Map<Item, ItemConflicts> conficts) {
        conflictsList.clear();
        conflictsList.addAll(conficts.entrySet());
    }

    public void onOkPressed(ActionEvent actionEvent) {
        MainController.getCurrentStage().hide();
    }
}