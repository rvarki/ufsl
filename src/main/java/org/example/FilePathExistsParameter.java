package org.example;

import java.io.File;
import java.io.Serializable;

//Class that checks whether the given OS or virtual filepath exists
//Note the OS Filepath must be an absolute filepath

public class FilePathExistsParameter implements Serializable{

    public boolean osPath(String path)
    {
        File file = new File(path);
        return file.exists();
    }
}
