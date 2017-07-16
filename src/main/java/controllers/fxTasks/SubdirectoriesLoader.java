package controllers.fxTasks;

import controllers.FileManagerItemsFactory.FXOptimizedItem;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import model.Item;

import java.util.concurrent.CountDownLatch;

/**
 * Created by kostyazxcvbn on 16.07.2017.
 */
public class SubdirectoriesLoader extends Task<Void> {

    CountDownLatch countDownLatch;
    FXOptimizedItem parentItem;
    ObservableList<FXOptimizedItem> selectedItemsList;
    boolean showHiddenItemsState;

    public SubdirectoriesLoader(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch, boolean showHiddenItemsState) {
        this.countDownLatch = countDownLatch;
        this.parentItem=parentItem;
        this.selectedItemsList=selectedItemsList;
        this.showHiddenItemsState=showHiddenItemsState;
    }

    @Override
    protected Void call() throws Exception {

        ObservableList<TreeItem<Item>> childrenDirectories =parentItem.getChildren();
        childrenDirectories.clear();

        for (FXOptimizedItem item : selectedItemsList) {
            if (item.isDirectory()) {
                if (showHiddenItemsState | !item.isHidden()) {
                    childrenDirectories.add(new FXOptimizedItem(item.getItem()));
                }
            }
        }
        parentItem.getChildren().sort((o1,  o2) ->
                (o1.getValue().getName().toUpperCase().compareTo(o2.getValue().getName().toUpperCase())));
        countDownLatch.countDown();
        return null;
    }
}