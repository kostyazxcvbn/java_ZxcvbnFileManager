package model;


import javax.swing.text.html.ImageView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by kostyazxcvbn on 09.07.2017.
 */
public class Item{
    private Path path;

    private String name;
    private String type;
    private String size;
    private String lastModifiedDate;
    private String createdDate;
    private boolean isHidden;
    private boolean isWritable;
    private boolean isReadable;
    private boolean isDirectory;
    private boolean isAvailable;
    private boolean isRootStorage;
    private boolean isRoot;

    public Item(Path path) {
        this.path = path;
        this.isAvailable=true;
        this.isRootStorage = (path.getParent()==null)?true:false;
        initAttributes(path);
    }

    public boolean isRootStorage() {
        return isRootStorage;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isDirectory() {

        return isDirectory;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public boolean isReadable() {
        return isReadable;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSize() {
        return size;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public Path getPath() {
        return path;
    }



    private void initAttributes(Path path){

        if(path.startsWith("/root")){
            this.name=path.getFileName().toString();
            this.isDirectory=true;
            this.isRoot=true;
        }
        else{
            BasicFileAttributes attribs = null;

            try {
                attribs = Files.readAttributes(path, BasicFileAttributes.class);
                this.isDirectory = attribs.isDirectory();

                if(isDirectory && !isRootStorage){
                    this.type = "<DIR>";

                    this.createdDate = attribs.creationTime().toString();
                    this.lastModifiedDate = attribs.lastModifiedTime().toString();
                    this.size="";
                    this.name=path.getFileName().toString();
                    this.isHidden = path.toFile().isHidden();
                    this.isReadable = Files.isReadable(path);
                    this.isWritable = Files.isWritable(path);
                }
                if (isRootStorage) {
                    this.type = "<DRIVE>";
                    this.name =path.toAbsolutePath().toString();
                    this.size ="";
                    this.createdDate="";
                    this.isHidden = false;
                    this.lastModifiedDate="";
                }
                if (!isDirectory()) {

                    this.name=path.getFileName().toString();

                    if (this.name.contains(".")) {
                        String[] nameArray = this.name.split("\\.");
                        this.type = nameArray[nameArray.length - 1];
                    } else {
                        this.type = "?";
                    }

                    this.createdDate = attribs.creationTime().toString();
                    this.lastModifiedDate = attribs.lastModifiedTime().toString();
                    this.size=String.valueOf(attribs.size())+" B";

                    this.isHidden = path.toFile().isHidden();
                    this.isReadable = Files.isReadable(path);
                    this.isWritable = Files.isWritable(path);
                }

            } catch (IOException e) {
                this.isAvailable=false;
            }

        }


    };

    @Override
    public int hashCode() {
        return path.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (this.hashCode()==obj.hashCode());
    }

    @Override
    public String toString() {
        return this.name;
    }
}
