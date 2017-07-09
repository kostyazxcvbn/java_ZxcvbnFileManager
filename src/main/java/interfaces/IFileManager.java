package interfaces;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

import static model.AppEnums.*;

/**
 * Created by kostyazxcvbn on 08.07.2017.
 */

public interface IFileManager {

    Map<Path, ItemConflicts> deleteItems(HashSet<Path> items);
    Path renameItem(Path path, String newName);
    Path createFile(Path path, String newName);
    Path createDirectory(Path path, String newNam);
    HashSet<Path> getContent(Path source, boolean onlyDirectories);
    Map<Path, ItemConflicts> copyItemsToBuffer(HashSet<Path> items);
    Map<Path, ItemConflicts> cutItemsToBuffer(HashSet<Path> items);
    Map<Path, ItemConflicts> moveItemsTo(HashSet<Path> items, Path destination, boolean isSourceWillBeDeleted);
    Map<Path,ItemConflicts> pasteItemsFromBuffer(Path destination);
    void setConflictListener(IConflictListener conflictListener);
}
