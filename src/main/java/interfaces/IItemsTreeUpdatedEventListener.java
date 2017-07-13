package interfaces;

import javafx.collections.ObservableList;
import vcontroller.ItemViewFactory.*;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public interface IItemsTreeUpdatedEventListener {
    void onUpdateItemsTree(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList);
}
