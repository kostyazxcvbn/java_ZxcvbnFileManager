package model;

/**
 * Created by kostyazxcvbn on 09.07.2017.
 */
public class AppEnums {
    public enum NameConflictState {
        REPLACE_EXISTING,
        REPLACE_EXISTING_ALL,
        NOT_REPLACE,
        NOT_REPLACE_ALL,
        NO_CONFLICTS,
        UNKNOWN
    }

    public enum ItemConflicts {
        ITEM_NOT_FOUND("Item not found."),
        CANT_DELETE_ITEM("Can't delete the item."),
        CANT_CREATE_ITEM("Can't create the item."),
        ACCESS_ERROR("Access error."),
        ITEM_EXISTS("The Item already exists."),
        ITEM_IS_NOT_COPIED("Can't copy the item."),
        DESTINATION_ERROR("Source must differ with destination!");

        private String message;

        ItemConflicts(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public enum GuiControlsState {
        ROOT_LEVEL,
        EMPTY_CONTENT,
        FILE_SELECTED,
        NOTHING_SELECTED,
        FOLDER_SELECTED
    }
}
