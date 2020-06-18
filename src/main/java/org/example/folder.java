package org.example;

import java.io.Serializable;
import java.util.HashMap;

public class folder implements Serializable {

       ////////
       //Data//
       ////////

       //TODO:Some of these should have access functions rather than being all public
       public boolean isUser; //True if the folder is a user folder
       public String name; //The name of the folder
       public HashMap<String,folder> children; //The folders(children) of this directory
       public HashMap<String, fileContainer> directoryData; //The files in the directory

       ////////////////
       //Constructors//
       ////////////////

       /**
        * Creates a root directory folder
        */
       public folder(){
           this.name = "~";
           this.children = new HashMap<>();
           this.directoryData = new HashMap<>();
           this.isUser= false;
       }

       /**
        * @param name The name of the new folder to create
        */
       public folder(String name){
          this.name = name;
          this.children = new HashMap<>();
          this.directoryData = new HashMap<>();
          this.isUser= false;
       }


       /////////////////////////
       //Children Manipulation//
       /////////////////////////

       /**
        * Adds a new child directory with the given name if it does not already exist
        * @param childName The name of the child directory
        * @return the new or existing folder with childName
        */
       public folder addChildDirectory(String childName){
           if(!this.children.containsKey(childName)){
               this.children.put(childName,new folder(childName));
               return this.children.get(childName);
           }

           return this.children.get(childName);
       }

       /**
        * Remove a child directory
        * @param childName the name of the child directory you would like to remove
        * @return true iff the child exists and was successfully removed
        */
       public boolean deleteChildDirectory(String childName){
           if(this.children.containsKey(childName)){
               this.children.remove(childName);
               return true;
           }

           return false;
       }

       /////////////////////
       //Data Manipulation//
       /////////////////////

       /**
        * Returns the specified fileContainer
        * @param fileName the name of the fileContainer to be returned
        * @return the fileContainer specified or null if the file does not exist
        */
       public fileContainer getData(String fileName){
           if(directoryData.containsKey(fileName))
               return directoryData.get(fileName);

           return null;
       }

       /**
        * Create a fileContainer object with the specified name and data and add it to this folder if the name is not taken already
        * @return true iff there were no name collisions while trying to add the new file
        */
       //TODO: Should decide if it makes more sense to overwrite here or just throw an error
       public boolean addData(String fileName,fileContainer myData){
           if(directoryData.containsKey(fileName))
               return false;

           directoryData.put(fileName,myData);
           return true;
       }

       public boolean removeData(String fileName){
           if(directoryData.containsKey(fileName)){
               directoryData.remove(fileName);
               return true;
           }

           return false;
       }
}