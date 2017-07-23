package interfaces;


import model.Item;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static model.AppEnums.*;

/**
 * Created by kostyazxcvbn on 08.07.2017.
 */

public interface IFileManager {

    Map<Item, ItemConflicts> deleteItems(HashSet<Item> items);
    Item renameItem(Item Item, String newName);
    Item createFile(String path, String newName);
    Item createDirectory(String path, String newName);
    Set<Item> getContent(Item source);
    Map<Item, ItemConflicts> copyItemsToBuffer(Collection<Item> items);
    Map<Item, ItemConflicts> cutItemsToBuffer(HashSet<Item> items);
    Map<Item, ItemConflicts> moveItemTo(Item source, Item destination, boolean isSourceWillBeDeleted, NameConflictState nameConflictState);
    Map<Item, ItemConflicts> pasteItemFromBuffer(Item source, Item destination, NameConflictState nameConflictState);
    Item getParentItem(Item child);
    Set<Item> getBuffer();
    boolean isCutOperation();
}
