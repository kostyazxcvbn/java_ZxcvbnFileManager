package interfaces;

import java.util.concurrent.CountDownLatch;

/**
 * Created by user on 14.07.2017.
 */
public interface IRefresher {
    void refresh(CountDownLatch countDownLatch);
}
