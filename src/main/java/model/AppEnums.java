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
        ITEM_NOT_FOUND,
        CANT_DELETE_ITEM,
        FATAL_APP_ERROR,
        CANT_CREATE_ITEM,
        SECURITY_ERROR,
        ITEM_EXISTS
    }
}
