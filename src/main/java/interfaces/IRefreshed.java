package interfaces;

/**
 * Created by kostyazxcvbn on 13.07.2017.
 */
public interface IRefreshed {
    void addListener(IRefresher listener);
    void removeAllListeners();
    void notifyListeners();

}
