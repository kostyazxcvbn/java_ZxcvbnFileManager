package model;

import interfaces.IConflictListener;
import interfaces.IFileManager;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;

import static model.AppEnums.*;

public class FileManagerImpl implements IFileManager{
    private static FileManagerImpl ourInstance = new FileManagerImpl();

    private HashSet<Path>copiedItemsBuffer;
    private boolean isCutOperation;

    private IConflictListener conflictListener;

    private ExecutorService executorService;

    public static FileManagerImpl getInstance() {
        return ourInstance;
    }

    private FileManagerImpl() {
        executorService=Executors.newCachedThreadPool();
    }

    @Override
    public void setConflictListener(IConflictListener conflictListener) {
        this.conflictListener = conflictListener;
    }

    @Override
    public Map<Path, ItemConflicts> deleteItems(HashSet<Path> items) {
        Map<Path, ItemConflicts>notDeletedItems = new HashMap<>();
        for (Path item : items) {
            if (Files.isDirectory(item)) {
                notDeletedItems.putAll(deleteDirectory(item));
            } else {
                try {
                    Files.delete(item);
                } catch (Exception e) {
                    notDeletedItems.put(item,ItemConflicts.CANT_DELETE_ITEM);
                }
            }
        }
        return notDeletedItems;
    }

    @Override
    public Path renameItem(Path target, String newName) {
        try {
            return Files.move(target, target.getParent().resolve(newName),StandardCopyOption.COPY_ATTRIBUTES);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Path createFile(Path path, String newName) {
        try {
            return Files.createFile(path.resolve(newName));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Path createDirectory(Path path, String newName) {
        try {
            return Files.createDirectory(path.resolve(newName));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public HashSet<Path> getContent(Path source, boolean onlyDirectories) {
        HashSet<Path>sourceContent = new HashSet<>();

        try(DirectoryStream<Path> dirContent = Files.newDirectoryStream(source)) {
            for (Path innerItem : dirContent) {
                if (onlyDirectories) {
                    if (Files.isDirectory(innerItem)) {
                        sourceContent.add(innerItem);
                    }
                } else {
                    sourceContent.add(innerItem);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return sourceContent;
    }

    @Override
    public Map<Path, ItemConflicts> cutItemsToBuffer(HashSet<Path> items) {
        isCutOperation=true;
        return copyItemsToBuffer(items);
    }

    @Override
    public Map<Path, ItemConflicts> copyItemsToBuffer(HashSet<Path> items) {

        Path source=null;

        isCutOperation=false;
        copiedItemsBuffer.clear();

        Map<Path, ItemConflicts>notCopiedItems = new HashMap<>();

        for (Path item : items) {
            if (Files.exists(item)) {
                copiedItemsBuffer.add(item);
                if (source != null) {
                    source=item.getParent();
                }
            }
            else{
                notCopiedItems.put(item,ItemConflicts.ITEM_NOT_FOUND);
            }
        }
        return notCopiedItems;
    }

    @Override
    public Map<Path, ItemConflicts> moveItemsTo(HashSet<Path> items, Path destination, boolean isSourceWillBeDeleted) {

        NameConflictState nameConflictState=NameConflictState.NO_CONFLICTS;

        Map<Path, ItemConflicts>notMovedItems = new HashMap<>();

        for (Path item : items) {
            Path newItem = destination.resolve(item.getFileName());

            if (nameConflictState==NameConflictState.NO_CONFLICTS && Files.exists(newItem)){
                nameConflictState=NameConflictState.UNKNOWN;
            }

            switch (nameConflictState) {
                case NO_CONFLICTS:
                case REPLACE_EXISTING_ALL:{
                    notMovedItems.putAll(moveWithReplace(item,newItem,isSourceWillBeDeleted));
                    break;
                }
                case UNKNOWN:{
                    nameConflictState=getConflictResult(executorService);

                    switch (nameConflictState) {
                        case REPLACE_EXISTING:{
                            notMovedItems.putAll(moveWithReplace(item,newItem,isSourceWillBeDeleted));
                            nameConflictState=NameConflictState.NO_CONFLICTS;
                            break;
                        }
                        case REPLACE_EXISTING_ALL:{
                            notMovedItems.putAll(moveWithReplace(item,newItem,isSourceWillBeDeleted));
                            break;
                        }
                        case UNKNOWN: {
                            notMovedItems.put(item, ItemConflicts.FATAL_APP_ERROR);
                            nameConflictState = NameConflictState.NO_CONFLICTS;
                            break;
                        }
                        case NOT_REPLACE:{
                            notMovedItems.put(item, ItemConflicts.ITEM_EXISTS);
                            nameConflictState = NameConflictState.NO_CONFLICTS;
                            break;
                        }
                        case NOT_REPLACE_ALL:
                        default:{
                            notMovedItems.put(item, ItemConflicts.ITEM_EXISTS);
                            break;
                        }
                    }
                }

                case NOT_REPLACE_ALL:
                default:{
                    notMovedItems.put(item, ItemConflicts.ITEM_EXISTS);
                    break;
                }
            }
        }
        return notMovedItems;
    }

    @Override
    public Map<Path,ItemConflicts> pasteItemsFromBuffer(Path destination) {
        return moveItemsTo(copiedItemsBuffer,destination,isCutOperation);
    }


    private Map<Path, ItemConflicts> deleteDirectory(Path directory) {

        Map<Path, ItemConflicts> notDeletedItems = new HashMap<>(1);

        try {
            Files.walkFileTree(directory, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!Files.exists(dir)) {
                        notDeletedItems.put(directory,ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (Exception e) {
                        notDeletedItems.put(file,ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    notDeletedItems.put(file,ItemConflicts.CANT_DELETE_ITEM);
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (Exception e) {
                        notDeletedItems.put(directory,ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            notDeletedItems.put(directory,ItemConflicts.CANT_DELETE_ITEM);
        }
        return notDeletedItems;
    }

    private Map<Path, ItemConflicts> moveWithReplace(Path source, Path dest, boolean isSourceWillBeDeleted) {
        Map<Path, ItemConflicts>notMovedItems=new HashMap<>();

        if (Files.isDirectory(source)) {
            try {
                Files.walkFileTree(source, new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                        Path newDir = dest.resolve(dir.getFileName());

                        try {
                            if (!Files.exists(newDir)) {
                                Files.createDirectory(newDir);
                            }
                        } catch (IOException e) {
                            notMovedItems.put(newDir, ItemConflicts.CANT_CREATE_ITEM);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path newFile = dest.resolve(file.getFileName());
                        try {
                            if (!Files.exists(newFile)) {
                                Files.copy(file, newFile, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            notMovedItems.put(newFile, ItemConflicts.SECURITY_ERROR);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.TERMINATE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                notMovedItems.put(source, ItemConflicts.FATAL_APP_ERROR);
            }

            if (isSourceWillBeDeleted && notMovedItems.isEmpty()) {
                notMovedItems.putAll(deleteDirectory(source));
            }
        } else {
            try {
                if (!Files.exists(source)) {
                    if(isSourceWillBeDeleted){
                        Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    else{
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                notMovedItems.put(source, ItemConflicts.SECURITY_ERROR);
            }
        }
        return notMovedItems;
    }

    private NameConflictState getConflictResult(ExecutorService es) {

        Future<NameConflictState>nameConflictResult=executorService.submit(new Callable<NameConflictState>() {
            @Override
            public NameConflictState call() throws Exception {
                return conflictListener.onConflict();
            }
        });

        try {
            return nameConflictResult.get();
        } catch (Exception e) {
            return NameConflictState.UNKNOWN;
        }
    }
}
