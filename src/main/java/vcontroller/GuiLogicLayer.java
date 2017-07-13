package vcontroller;

import interfaces.IListened;
import interfaces.IContentChangedEventListener;
import interfaces.IItemsTreeUpdatedEventListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static vcontroller.ItemViewFactory.*;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public class GuiLogicLayer implements IListened {
    private static GuiLogicLayer ourInstance = new GuiLogicLayer();

    private ArrayList<IItemsTreeUpdatedEventListener> onUpdateItemsTreeListeners;
    private ArrayList<IContentChangedEventListener> onItemContentChangedListeners;

    private ExecutorService notGuiThreadPool;

    public GuiLogicLayer() {
        notGuiThreadPool=MainController.getThreadLogicUIPool();
    }
    public static GuiLogicLayer getInstance() {
        return ourInstance;
    }

    @Override
    public void addListener(IContentChangedEventListener listener) {
        onItemContentChangedListeners.add(listener);
    }

    @Override
    public void addListener(IItemsTreeUpdatedEventListener listener) {
        onUpdateItemsTreeListeners.add(listener);
    }

    @Override
    public void removeListener(IContentChangedEventListener listener) {
        onItemContentChangedListeners.remove(listener);
    }

    @Override
    public void removeListener(IItemsTreeUpdatedEventListener listener) {
        onUpdateItemsTreeListeners.remove(listener);
    }

    @Override
    public void notifyItemsContentChangedListeners(FXOptimizedItem item) {
        for (IContentChangedEventListener onItemContentChangingListener : onItemContentChangedListeners) {
            onItemContentChangingListener.onItemContentChanged(item);
        }
    }

    @Override
    public void notifyItemsTreeUpdatedListeners(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> innerItems) {
        for (IItemsTreeUpdatedEventListener onUpdateItemsTreeListener : onUpdateItemsTreeListeners) {
            onUpdateItemsTreeListener.onUpdateItemsTree(parentItem, innerItems);
        }
    }

    protected void updateItemsTree(FXOptimizedItem parentItem, ObservableList<FXOptimizedItem> selectedItemsList) {

        Task<Void> subdirectoriesLoader = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                for (FXOptimizedItem item : selectedItemsList) {
                    if (item.isDirectory()) {
                        Platform.runLater(() -> notifyItemsTreeUpdatedListeners(item, selectedItemsList));
                    }
                }
                return  null;
            }
        };

        notGuiThreadPool.execute(subdirectoriesLoader);
    }
/*
    protected void getItemContent(FXOptimizedItem item, boolean isIconChanging, ) {

        ItemContentLoader contentLoader = new ItemContentLoader(tempImageView, item, true);
        threadLogicUIPool.execute(contentLoader);
    }

    private class ItemContentLoader extends Task<Void>{
        ImageView tempImageView;
        FXOptimizedItem item;
        boolean isIconChanging;


        public ItemContentLoader(ImageView tempImageView, FXOptimizedItem item, boolean isItemChanging) {
            this.tempImageView = tempImageView;
            this.item=item;
            this.isIconChanging=isItemChanging;
        }

        private int toInt(boolean value) {
            return (value)?1:0;
        }

        @Override
        protected Void call() throws Exception {

            ObservableList<FXOptimizedItem> innerItemsInView = tablevDirContent.getItems();
            Platform.runLater(()->innerItemsInView.clear());

            if(!selectedItemsList.isEmpty()){
                Platform.runLater(() -> {
                    innerItemsInView.addAll(selectedItemsList);
                    innerItemsInView.sort((o1,o2)-> {
                        int o1ToInt=toInt(o1.isDirectory());
                        int o2ToInt=toInt(o2.isDirectory());
                        if (o1ToInt == o2ToInt) {
                            return o1.getName().toUpperCase().compareTo(o2.getName().toUpperCase());
                        }
                        return o2ToInt-o1ToInt;
                    }) ;
                });
            }
            if (isIconChanging) {
                Platform.runLater(() -> item.setGraphic(tempImageView));
            }
            return null;
        }
    }
    */
}
