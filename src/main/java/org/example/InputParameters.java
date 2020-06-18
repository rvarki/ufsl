package org.example;
import com.beust.jcommander.Parameter;

import java.io.Serializable;
import java.util.ArrayList;
import java.lang.*;

//Class contains the parameters that the Union File Locker System (ufsl) recognizes

public class InputParameters implements Serializable {

    @Parameter(names={"-user"},
            description = "Usage: -user [username] (Required for all flags except --regex, --dirDif, --GUI).",
            order = 0)

    String userName;

    public String getUserName() {

        if (userName != null)
            return userName;
        else
            throw new NullPointerException("userName does not exist");
    }

    @Parameter(names={"--addFile"},
            arity = 2,
            splitter = SpaceSplitter.class,
            description = "Usage: -user [username] --addFile [osFilePath] [ufsFilePath] (Note: The osFilePath must be an absolute filepath). It adds the file from the osFilePath to the virtual path that is specified, maintaining the original name of the file.",
            order = 1)

    ArrayList<String> addPaths;

    public ArrayList<String> getAddPaths() {

        if (addPaths != null)
            return addPaths;
        else
            throw new NullPointerException("addPaths does not exist");

    }

    @Parameter(names={"--retrieve"},
            arity = 2,
            splitter = SpaceSplitter.class,
            description = "Usage: -user [username] --retrieve [ufsFilePath] [osFilePath] (Note: The osFilePath must be an absolute filepath). It retrieves the file at the virtual path and deposits it at the specified OS filepath.",
            order = 2)

    ArrayList<String> retrievePaths;

    public ArrayList<String> getRetrievePaths() {

        if (retrievePaths != null)
            return retrievePaths;
        else
            throw new NullPointerException("retrievePaths does not exist");
    }

    @Parameter(names={"--remove"},
            description = "Usage: -user [username] --remove [ufsFilePath]. It removes the file at the virtual filepath specified.",
            order = 3)

    String removePath;

    public String getRemovePath(){

        if (removePath != null)
            return removePath;
        else
            throw new NullPointerException("removePath does not exist");

    }

    @Parameter(names={"--list"},
            arity = 0,
            description = "Usage: -user [username] --list. Lists all the files stored for the user.",
            order = 4)

    boolean list = false;

    public boolean getList(){
        return list;
    }

    /*@Parameter(names={"--addUser"},
            description = "Usage: Adds the input username",
            order = 5)

    String addUserName;

    public String getAddUserName() {

        if (addUserName != null)
            return addUserName;
        else
            throw new NullPointerException("userName does not exist");

    }

    @Parameter(names={"--removeUser"},
            description = "Usage: Removes the input username",
            order = 6)

    String removeUserName;

    public String removeUserName() {

        if (removeUserName != null)
            return removeUserName;
        else
            throw new NullPointerException("userName does not exist");

    }*/

    @Parameter(names={"--lockerName"},
            description = "Usage: Input the name of the locker. Can be appended to any command when passed as an argument, or passed alone to start an interactive session on a specific locker. Once inside of an interactive session, the locker can not be changed.",
            order = 5)

    String lockerName;

    public String getLockerName() {

        if (lockerName != null)
            return lockerName;
        else
            return "locker.data";

    }

    @Parameter(names={"--regex"},
            description = "Usage: --regex [regex]. Input the regular expression [regex] that you want to search for in the files of the locker. It returns the file paths of all the files whose CONTENTS contain the expression assuming files are encoded in UTF-8. Does not match file names. Searches the entire locker. Cannot be paired with -user or a specific file path.",
            order = 6)

    String regex;

    public String getRegex() {

        if (regex != null)
            return regex;
        else
            throw new NullPointerException("regex does not exist");

    }

    @Parameter(names={"--dirDif"},
            arity = 2,
            splitter = SpaceSplitter.class,
            description = "Usage: --dirDif [ufsFilePath] [ufsFilePath]. Returns all files that are in only one of the two paths by CONTENTS (ignores naming). Is not used with the user flag, as users are specified in the paths.",
            order = 7)

    ArrayList<String> dirDifPaths;

    public ArrayList<String> getdirDifPaths() {

        if (dirDifPaths != null)
            return dirDifPaths;
        else
            throw new NullPointerException("dirDifPaths does not exist");

    }

    @Parameter(names={"--GUI"},
            arity = 0,
            description = "Usage: Can only be called from interactive mode. If you close the window you must re-run to open the GUI again. Cannot be combined with any other flags. First run the program with no arguments, then run --GUI.",
            order = 8)

    boolean GUI = false;

    public boolean getGUI()
    {
        return GUI;
    }


    @Parameter(names={"--test"},
            arity = 0,
            description = "Usage: --test. Used for the unit tests",
            order = 9)

    boolean test = false;

    public boolean getTest()
    {
        return test;
    }
}
