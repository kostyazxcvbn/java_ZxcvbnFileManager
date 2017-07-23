package model;

import interfaces.IFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static model.AppEnums.*;

public class FileManagerImpl implements IFileManager{
    private static FileManagerImpl ourInstance = new FileManagerImpl();

    private HashSet<Item>copiedItemsBuffer;
    private boolean isCutOperation;

    public static FileManagerImpl getInstance() {
        return ourInstance;
    }

    private FileManagerImpl() {
        copiedItemsBuffer = new HashSet<>();
    }

    @Override
    public boolean isCutOperation() {
        return isCutOperation;
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
            if (notDeletedItems.isEmpty() && item.isCutted()) {
                copiedItemsBuffer.remove(item);
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
    public Map<Item, ItemConflicts> moveItemTo(Item source, Item destination, boolean isSourceWillBeDeleted, NameConflictState nameConflictState) {

        Map<Item, ItemConflicts>notMovedItems = new HashMap<>(1);

        if (source.equals(destination)) {
            notMovedItems.put(source, ItemConflicts.DESTINATION_ERROR);
            return notMovedItems;
        }

        Path newItem = destination.getPath().resolve(source.getPath().getFileName());

        if (nameConflictState==NameConflictState.NO_CONFLICTS && Files.exists(newItem)){
            nameConflictState=NameConflictState.UNKNOWN;
        }
        switch (nameConflictState) {
            case NO_CONFLICTS:
            case REPLACE_EXISTING_ALL:{
                notMovedItems.putAll(moveWithReplace(source.getPath(),newItem,(isCutOperation&&copiedItemsBuffer.contains(source)&&source.getPath().equals(newItem))?false:isSourceWillBeDeleted));
                break;
            }
            case REPLACE_EXISTING:{
                notMovedItems.putAll(moveWithReplace(source.getPath(),newItem,(isCutOperation&&copiedItemsBuffer.contains(source)&&source.getPath().equals(newItem))?false:isSourceWillBeDeleted));
                break;
            }
            case NOT_REPLACE:{
                notMovedItems.put(source, ItemConflicts.ITEM_EXISTS);
                break;
            }
            case UNKNOWN: {
                return null;
            }
            case NOT_REPLACE_ALL:
            default:{
                notMovedItems.put(source, ItemConflicts.ITEM_EXISTS);
                break;
            }
        }
        return notMovedItems;
    }

    @Override
    public Map<Item,ItemConflicts> pasteItemFromBuffer(Item source, Item destination, NameConflictState nameConflictState) {
        Map<Item, ItemConflicts>notMovedItems=moveItemTo(source,destination,isCutOperation, nameConflictState);
        return notMovedItems;
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
                Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)

                    {
                        Path newDir = dest.resolve(source.relativize(dir));
                        try {
                            Files.copy(dir, newDir, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            notMovedItems.put(new Item(newDir), ItemConflicts.CANT_CREATE_ITEM);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    {
                        try {
                            Files.copy(file, dest.resolve(source.relativize(file)),StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            notMovedItems.put(new Item(dest), ItemConflicts.CANT_CREATE_ITEM);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

            } catch (IOException e) {
                notMovedItems.put(new Item(source), ItemConflicts.CANT_CREATE_ITEM);
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
                notMovedItems.put(new Item(source), ItemConflicts.ACCESS_ERROR);
            }
        }
        return notMovedItems;
    }
}
