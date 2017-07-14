package interfaces;

import javafx.scene.image.ImageView;

import static controllers.FileManagerItemsFactory.*;

/**
 * Created by user on 14.07.2017.
 */
public interface IIconChanger {
    void updateIcon(Object itemsContainer, FXOptimizedItem item, ImageView icon);
}
