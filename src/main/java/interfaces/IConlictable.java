package interfaces;

import model.AppEnums.NameConflictState;

import java.util.concurrent.Future;

/**
 * Created by kostyazxcvbn on 15.07.2017.
 */
public interface IConlictable {
    void addListener(IConflictListener listener);
    void removeListener();
    NameConflictState notifyListener();
}
