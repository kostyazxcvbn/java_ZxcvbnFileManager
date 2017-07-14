package vcontroller;

import interfaces.*;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static vcontroller.FileManagerItemsFactory.*;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public class AppViewRefresher extends Task<Void> implements IRefreshed {
    ArrayList<IRefresher> refreshers;
    CountDownLatch countDownLatch;
    IIconChanger iconChanger;
    FXOptimizedItem item;

    public AppViewRefresher(FXOptimizedItem item, IIconChanger iconChanger) {
        this.refreshers = new ArrayList<>();
        this.item = item;
        this.iconChanger=iconChanger;
    }

    @Override
    protected Void call() throws Exception {

        this.countDownLatch = new CountDownLatch(refreshers.size());

        iconChanger.changeWaiting(item);

        Thread.sleep(2000);
        notifyListeners();
        countDownLatch.await();

        iconChanger.changeNormal(item);

        return null;
    }

    @Override
    public void addListener(IRefresher listener) {
        refreshers.add(listener);
    }

    @Override
    public void removeAllListeners() {
        refreshers.clear();
    }

    @Override
    public void notifyListeners() {
        for (IRefresher refresher : refreshers) {
            refresher.refresh(countDownLatch);
        }
    }
}
