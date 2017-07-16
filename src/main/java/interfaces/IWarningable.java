package interfaces;

import controllers.FileManagerItemsFactory.FXOptimizedItem;
import javafx.collections.ObservableList;
import model.Item;

import java.util.HashSet;

/**
 * Created by kostyazxcvbn on 16.07.2017.
 */
public interface IWarningable {
    void onButtonOkPressed(HashSet<Item>itemsCollection);
}
