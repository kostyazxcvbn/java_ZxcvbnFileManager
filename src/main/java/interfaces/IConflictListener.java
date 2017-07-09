package interfaces;

import model.AppEnums;
import static model.AppEnums.*;
/**
 * Created by kostyazxcvbn on 08.07.2017.
 */
public interface IConflictListener {
    NameConflictState onConflict();
}
