package controllers.fxTasks;

import controllers.FileManagerItemsFactory.FXOptimizedItem;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TableView;
import java.util.concurrent.CountDownLatch;

/**
 * Created by kostyazxcvbn on 16.07.2017.
 */
public class ItemContentLoader extends Task<Void> {

    FXOptimizedItem item;
    CountDownLatch countDownLatch;
    ObservableList<FXOptimizedItem> selectedItemsList;
    boolean showHiddenItemsState;
    TableView tablevDirContent;

    public ItemContentLoader(FXOptimizedItem item, ObservableList<FXOptimizedItem> selectedItemsList, CountDownLatch countDownLatch, boolean showHiddenItemsState, TableView tablevDirContent) {
        this.item=item;
        this.countDownLatch = countDownLatch;
        this.selectedItemsList=selectedItemsList;
        this.showHiddenItemsState=showHiddenItemsState;
        this.tablevDirContent=tablevDirContent;
    }

    private int toInt(boolean value) {
        return (value)?1:0;
    }

    @Override
    protected Void call() throws Exception {
        ObservableList<FXOptimizedItem> innerItemsInView = tablevDirContent.getItems();
        innerItemsInView.clear();

        if(!selectedItemsList.isEmpty()){
            innerItemsInView.addAll((showHiddenItemsState)?selectedItemsList:selectedItemsList.filtered(fxOptimizedItem -> !fxOptimizedItem.isHidden()));
            innerItemsInView.sort((o1,o2)-> {
                int o1ToInt=toInt(o1.isDirectory());
                int o2ToInt=toInt(o2.isDirectory());
                if (o1ToInt == o2ToInt) {
                    return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                }
                return o2ToInt-o1ToInt;
            });
        }
        countDownLatch.countDown();
        return null;
    }
}