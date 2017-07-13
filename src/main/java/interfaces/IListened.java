package interfaces;

import javafx.collections.ObservableList;

import static vcontroller.ItemViewFactory.*;

import java.util.ArrayList;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public interface IListened {
    void addListener(IContentChangedEventListener listener);
    void addListener(IItemsTreeUpdatedEventListener listener);

    void removeListener(IContentChangedEventListener listener);
    void removeListener(IItemsTreeUpdatedEventListener listener);

    void notifyItemsContentChangedListeners(FXOptimizedItem item);
    void notifyItemsTreeUpdatedListeners(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> innerItems);
}
