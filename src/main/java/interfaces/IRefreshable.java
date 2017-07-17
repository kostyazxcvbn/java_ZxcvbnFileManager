package interfaces;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public interface IRefreshable {
    void addListener(IRefreshingListener listener);
    void removeAllListeners();
    void notifyListeners();

}