package model;


import javax.swing.text.html.ImageView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

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
    private String attributes;
    private boolean isHidden;
    private boolean isDirectory;
    private boolean isAvailable;
    private boolean isRootStorage;
    private boolean isRoot;
    private boolean isCutted;

    public Item(Path path) {
        this.path = path;
        this.isAvailable=true;
        this.isRootStorage = (path.getParent()==null)?true:false;
        this.attributes="?";
        initAttributes(path);
        if (FileManagerImpl.getInstance().getBuffer().contains(this) && FileManagerImpl.getInstance().isCutOperation() ) {
            isCutted=true;
        }
    }

    public boolean isCutted() {
        return isCutted;
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
        boolean isWritable=false;
        boolean isReadable=false;
        DateFormat formattedDate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

                    this.createdDate = (formattedDate.format(attribs.creationTime().toMillis()));
                    this.lastModifiedDate = (formattedDate.format(attribs.lastModifiedTime().toMillis()));
                    this.size="";
                    this.name=path.getFileName().toString();
                    this.isHidden = path.toFile().isHidden();
                    isReadable = Files.isReadable(path);
                    isWritable = Files.isWritable(path);
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

                    this.createdDate = (formattedDate.format(attribs.creationTime().toMillis()));
                    this.lastModifiedDate = (formattedDate.format(attribs.lastModifiedTime().toMillis()));
                    this.size=String.valueOf(attribs.size())+" B";

                    this.isHidden = path.toFile().isHidden();
                    isReadable = Files.isReadable(path);
                    isWritable = Files.isWritable(path);
                }

            } catch (IOException e) {
                this.isAvailable=false;
            }

        }

        StringBuilder attributes=new StringBuilder();

        if (isAvailable){
            if (isReadable) {
                attributes.append('r');
            }
            if (isWritable)
                attributes.append('w');
        }
        this.attributes=attributes.toString();
    }

    @Override
    public int hashCode() {
        return path.toString().hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        return (this.getPath().toString().equals(((Item)obj).getPath().toString()));
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getAttributes() {
        return attributes;
    }
}
