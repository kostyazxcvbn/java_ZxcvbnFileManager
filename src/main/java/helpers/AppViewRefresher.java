package helpers;

import interfaces.*;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import helpers.FileManagerItemsFactory.*;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public class AppViewRefresher extends Task<Void> implements IRefreshable {
    ArrayList<IRefreshingListener> refreshers;
    CountDownLatch countDownLatch;
    FXOptimizedItem item;
    boolean isIconWillChanged;
    long delayImitationMs;

    public AppViewRefresher(FXOptimizedItem item, boolean isIconWillChanged, long delayImitaionMs,IRefreshingListener...refreshers) {
        this.refreshers = new ArrayList<>();
        this.item = item;
        this.isIconWillChanged =isIconWillChanged;
        this.delayImitationMs=delayImitaionMs;
        for (IRefreshingListener refresher : refreshers) {
            this.refreshers.add(refresher);
        }
    }

    @Override
    protected Void call() throws Exception {

        ImageView tempLink=item.getIcon();
        ImageView tempIcon=tempLink;

        this.countDownLatch = new CountDownLatch(refreshers.size());

        if (isIconWillChanged) {
            FileManagerItemsFactory.updateIcon(item, FileManagerItemsFactory.getItemWaiting());
        }

        Thread.sleep(delayImitationMs);
        notifyListeners();
        countDownLatch.await();

        if (isIconWillChanged) {
            FileManagerItemsFactory.updateIcon(item, tempIcon);
        }

        return null;
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
