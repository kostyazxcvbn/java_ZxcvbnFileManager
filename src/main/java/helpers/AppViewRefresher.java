package helpers;

import interfaces.*;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public class AppViewRefresher extends Task<Void> implements IRefreshable {
    ArrayList<IRefreshingListener> refreshers;
    CountDownLatch countDownLatch;
    FileManagerItemsFactory.FXOptimizedItem item;
    Object itemContainer;
    long delayImitationMs;

    public AppViewRefresher(FileManagerItemsFactory.FXOptimizedItem item, Object itemContainer, long delayImitaionMs) {
        this.refreshers = new ArrayList<>();
        this.item = item;
        this.itemContainer=itemContainer;
        this.delayImitationMs=delayImitaionMs;
    }

    @Override
    protected Void call() throws Exception {

        ImageView tempLink=item.getIcon();
        ImageView tempIcon=tempLink;

        this.countDownLatch = new CountDownLatch(refreshers.size());

        FileManagerItemsFactory.updateIcon(itemContainer, item, FileManagerItemsFactory.getItemWaiting());

        Thread.sleep(delayImitationMs);
        notifyListeners();
        countDownLatch.await();

        FileManagerItemsFactory.updateIcon(itemContainer, item, tempIcon);

        return null;
    }

    @Override
    public void addListener(IRefreshingListener listener) {
        refreshers.add(listener);
    }

    @Override
    public void removeAllListeners() {
        refreshers.clear();
    }

    @Override
    public void notifyListeners() {
        for (IRefreshingListener refresher : refreshers) {
            refresher.refresh(countDownLatch);
        }
    }
}
