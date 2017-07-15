package model;

import interfaces.IConflictListener;
import interfaces.IConlictable;
import interfaces.IFileManager;
import interfaces.IRefreshingListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

import static model.AppEnums.*;

public class FileManagerImpl implements IFileManager, IConlictable{
    private static FileManagerImpl ourInstance = new FileManagerImpl();

    private HashSet<Item>copiedItemsBuffer;
    private boolean isCutOperation;

    private IConflictListener conflictListener;

    private ExecutorService onConflictResultTasksPool;

    public static FileManagerImpl getInstance() {
        return ourInstance;
    }

    private FileManagerImpl() {
        onConflictResultTasksPool =Executors.newCachedThreadPool();
        copiedItemsBuffer = new HashSet<>();
    }

    protected boolean isCutOperation() {
        return isCutOperation;
    }

    @Override
    public void setConflictListener(IConflictListener conflictListener) {
        this.conflictListener = conflictListener;
    }

    @Override
    public Item getParentItem(Item child) {
            return new Item(child.getPath().resolve("..").normalize());
    }

    @Override
    public Map<Item, ItemConflicts> deleteItems(HashSet<Item> items) {
        Map<Item, ItemConflicts>notDeletedItems = new HashMap<>();
        for (Item item : items) {
            if (Files.isDirectory(item.getPath())) {
                notDeletedItems.putAll(deleteDirectory(item.getPath()));
            } else {
                try {
                    Files.delete(item.getPath());
                } catch (Exception e) {
                    notDeletedItems.put(item,ItemConflicts.CANT_DELETE_ITEM);
                }
            }
        }
        return notDeletedItems;
    }

    @Override
    public Item renameItem(Item target, String newName) {
        try {
            return new Item(Files.move(target.getPath(), target.getPath().getParent().resolve(newName),StandardCopyOption.COPY_ATTRIBUTES));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Item createFile(String path, String newName) {
        try {
            return new Item(Files.createFile(Paths.get(path).resolve(newName)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Item createDirectory(String path, String newName) {
        try {
            return new Item(Files.createDirectory(Paths.get(path).resolve(newName)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<Item> getContent(Item source){

        Set<Item>sourceContent = new HashSet<>();

        if (source.getName().equals("root")) {
            File[] roots = File.listRoots();
            for (File root : roots) {
                sourceContent.add(new Item(root.toPath()));
            }
        } else {
            try(DirectoryStream<Path> dirContent = Files.newDirectoryStream(source.getPath())) {
                for (Path innerItem : dirContent) {
                    sourceContent.add(new Item(innerItem));
                }
            } catch (Exception e) {
                return Collections.EMPTY_SET;
            }
        }
        return sourceContent;
    }

    @Override
    public Map<Item, ItemConflicts> cutItemsToBuffer(HashSet<Item> items) {
        Map<Item, ItemConflicts>notCopiedItems=copyItemsToBuffer(items);
        isCutOperation=true;
        return notCopiedItems;
    }

    @Override
    public HashSet<Item> getBuffer() {
        return copiedItemsBuffer;
    }

    @Override
    public Map<Item, ItemConflicts> copyItemsToBuffer(Collection<Item> items) {

        isCutOperation=false;
        copiedItemsBuffer.clear();

        Map<Item, ItemConflicts>notCopiedItems = new HashMap<>();

        for (Item item : items) {
            if (Files.exists(item.getPath())) {
                if (item.isRootStorage() || item.isRoot()) {
                    notCopiedItems.put(item,ItemConflicts.ITEM_IS_NOT_COPIED);
                } else {
                    copiedItemsBuffer.add(item);
            }

            }
            else{
                notCopiedItems.put(item,ItemConflicts.ITEM_NOT_FOUND);
            }
        }
        return notCopiedItems;
    }

    @Override
    public Map<Item, ItemConflicts> moveItemsTo(HashSet<Item> items, Item destination, boolean isSourceWillBeDeleted) {

        NameConflictState nameConflictState=NameConflictState.NO_CONFLICTS;

        Map<Item, ItemConflicts>notMovedItems = new HashMap<>();

        for (Item item : items) {
            Path newItem = destination.getPath().resolve(item.getPath().getFileName());

            if (nameConflictState==NameConflictState.NO_CONFLICTS && Files.exists(newItem)){
                nameConflictState=NameConflictState.UNKNOWN;
            }

            switch (nameConflictState) {
                case NO_CONFLICTS:
                case REPLACE_EXISTING_ALL:{
                    notMovedItems.putAll(moveWithReplace(item.getPath(),newItem,isSourceWillBeDeleted));
                    break;
                }
                case UNKNOWN:{
                    nameConflictState=getConflictResult(onConflictResultTasksPool);

                    switch (nameConflictState) {
                        case REPLACE_EXISTING:{
                            notMovedItems.putAll(moveWithReplace(item.getPath(),newItem,isSourceWillBeDeleted));
                            nameConflictState=NameConflictState.NO_CONFLICTS;
                            break;
                        }
                        case REPLACE_EXISTING_ALL:{
                            notMovedItems.putAll(moveWithReplace(item.getPath(),newItem,isSourceWillBeDeleted));
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
    public Map<Item,ItemConflicts> pasteItemsFromBuffer(Item destination) {
        return moveItemsTo(copiedItemsBuffer,destination,isCutOperation);
    }


    private Map<Item, ItemConflicts> deleteDirectory(Path directory) {

        Map<Item, ItemConflicts> notDeletedItems = new HashMap<>(1);

        try {
            Files.walkFileTree(directory, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!Files.exists(dir)) {
                        notDeletedItems.put(new Item(directory),ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                    } catch (Exception e) {
                        notDeletedItems.put(new Item(file),ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    notDeletedItems.put(new Item(file),ItemConflicts.CANT_DELETE_ITEM);
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (Exception e) {
                        notDeletedItems.put(new Item(directory),ItemConflicts.CANT_DELETE_ITEM);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            notDeletedItems.put(new Item(directory),ItemConflicts.CANT_DELETE_ITEM);
        }
        return notDeletedItems;
    }

    private Map<Item, ItemConflicts> moveWithReplace(Path source, Path dest, boolean isSourceWillBeDeleted) {
        Map<Item, ItemConflicts>notMovedItems=new HashMap<>();

        if (Files.isDirectory(source)) {
            try {
                Files.walkFileTree(source, new FileVisitor<Path>() {
                    Path currentDest=dest;
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                        Path newDir=dest;

                        if (!dir.toString().equals(source.toString())) {
                            newDir = dest.resolve(dir.getFileName());
                        }

                        currentDest=newDir;

                        try {
                            if (!Files.exists(newDir)) {
                                Files.createDirectory(newDir);
                            }
                        } catch (IOException e) {
                            notMovedItems.put(new Item(newDir), ItemConflicts.CANT_CREATE_ITEM);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path newFile = currentDest.resolve(file.getFileName());
                        try {
                            if (!Files.exists(newFile)) {
                                Files.copy(file, newFile, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            notMovedItems.put(new Item(newFile), ItemConflicts.SECURITY_ERROR);
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
                notMovedItems.put(new Item(source), ItemConflicts.FATAL_APP_ERROR);
            }

            if (isSourceWillBeDeleted && notMovedItems.isEmpty()) {
                notMovedItems.putAll(deleteDirectory(source));
            }
        } else {
            try {
                if (Files.exists(source)) {
                    if(isSourceWillBeDeleted){
                        Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    else{
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                notMovedItems.put(new Item(source), ItemConflicts.SECURITY_ERROR);
            }
        }
        return notMovedItems;
    }

    private NameConflictState getConflictResult(ExecutorService es) {

        Future<NameConflictState>nameConflictResult= onConflictResultTasksPool.submit(new Callable<NameConflictState>() {
            @Override
            public NameConflictState call() throws Exception {
                return notifyListener();
            }
        });

        try {
            return nameConflictResult.get();
        } catch (Exception e) {
            return NameConflictState.UNKNOWN;
        }
    }

    @Override
    public void addListener(IConflictListener listener) {
        conflictListener=listener;
    }

    @Override
    public void removeListener() {
        conflictListener=null;
    }

    @Override
    public NameConflictState notifyListener() {
        if (conflictListener == null) {
            return null;
        }
        return conflictListener.onConflict();
    }
}
