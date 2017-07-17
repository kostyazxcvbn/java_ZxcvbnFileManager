package interfaces;

import model.Item;

import java.util.HashSet;

/**
 * Created by kostyazxcvbn on 16.07.2017.
 */
public interface IOkCancelHandler {
    void onButtonOkPressed(HashSet<Item>itemsCollection, String path);
    void onButtonOkPressed(String destinationPath, String newFolderName);

}
