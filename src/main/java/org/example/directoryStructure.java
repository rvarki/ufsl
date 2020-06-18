package org.example;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A tree representing the internal directory structure of the locker
 */
public class directoryStructure implements Serializable {
   ////////
   //Data//
   ////////

   //The root of the entire directory system. It's children are the users of the system
   public folder root;

   //Used for keeping track of which file containers have the same byteSamplerHash
   HashMap<Integer,LinkedList<fileContainer>> unionDetectionMap;

    //Used for keeping track of similar images
    HashMap<Integer,LinkedList<fileContainer>> imageSimilarityMap;
   ////////////////
   //Constructors//
   ////////////////

    public directoryStructure(){
       root = new folder();
       unionDetectionMap = new HashMap<>();
       imageSimilarityMap= new HashMap<>();
    }

   //////////////////////
   //Users Manipulation//
   //////////////////////

   /**
    * Add a new user to the file system if they don't already exist
    * @param userName the name of the user to add to the system
    * @return false if the user already exists, true otherwise
    */
    public boolean addNewUser(String userName){
        if(root.children.containsKey(userName))
            return false;

        root.addChildDirectory(userName);
        root.children.get(userName).isUser = true;
        return true;
    }

    /**
     * delete an existing user from the file system if they exist
     * @param userName the name of the user to delete from the system
     * @return false if the user doesn't exist already, true otherwise
     */
    public boolean deleteUser(String userName){
        if(root.children.containsKey(userName))
            return false;

        root.children.remove(userName);
        return true;
    }

   /////////////////////
   //File Manipulation//
   /////////////////////

    /**
     * @param osFilePath The path on your computer containing the file you would like to add
     * @param lockerPathNoFile The path in the locker that you would like to add the file to (not including the file name)
     * @param fileName the name of the file to add
     * @param userName The user account you wish to add your file to
     * @param image is the input a png file
     * @return The success or failure of the creation of a new file
     */
   public boolean addFile(String osFilePath, String lockerPathNoFile, String fileName, String userName,boolean image){
       //UNCOMMENT THESE LINES TO ENABLE IMAGE SIMILARITY (UNSTABLE)
//     if(image)
//         return addImage(osFilePath,lockerPathNoFile,fileName,userName);

       folder finalFolder = traverseToFolder(userName + "/" + lockerPathNoFile + "/"+ fileName, true);

       if(finalFolder.directoryData.containsKey(fileName))
           return false;

       byte[] fileData = auxiliary.readBytesFromFile(osFilePath);
       Integer fileHash = auxiliary.byteSamplerHash(fileData); //Use a cheap hash to reduce the number of file comparisons
       fileContainer myFile = null;

       //If no other file has your hash, add yourself to the hashmap
       //If any other files have your hash, check if you are identical to any of them. If you are you should reference them
       if(!unionDetectionMap.containsKey(fileHash)){
            myFile = new fileContainer(fileName,fileData,userName+"/"+lockerPathNoFile);
            unionDetectionMap.put(fileHash,new LinkedList<fileContainer>());
            unionDetectionMap.get(fileHash).add(myFile);
       }else{
            //Iterate through the matching hashes and see if you have the same data as any of them
            for(fileContainer file:unionDetectionMap.get(fileHash)){
                if(Arrays.equals(fileData,file.getData())){
                    if(file.isReference){
                        myFile = new fileContainer(fileName,file.referenceFile,userName+"/"+lockerPathNoFile);
                        file.referenceFile.filePaths.add(myFile.path+"/"+myFile.name);
                    } else{
                        myFile = new fileContainer(fileName,file,userName+"/"+lockerPathNoFile);
                        file.filePaths.add(myFile.path+"/"+myFile.name);
                    }
                    break;
                }
            }

            //If you didn't find a match add the new file to the UnionDetectionMap 
            if(myFile == null){
                myFile = new fileContainer(fileName,fileData,userName+"/"+ lockerPathNoFile);
                unionDetectionMap.get(fileHash).add(myFile);
            }
       }
       return finalFolder.addData(fileName,myFile);
   }

    /**
     * Adds an image to the locker performing a check for similarity
     * @param lockerPathNoFile The path in the locker that you would like to add the file to (not including the file name)
     * @param fileName the name of the file to add
     * @param userName The user account you wish to add your file to
     * @return The success or failure of the creation of a new file
     */
    private boolean addImage(String osFilePath, String lockerPathNoFile, String fileName, String userName){
        folder finalFolder = traverseToFolder(userName + "/" + lockerPathNoFile + "/"+ fileName, true);
        if(finalFolder.directoryData.containsKey(fileName))
            return false;

        byte[] fileData = auxiliary.readBytesFromFile(osFilePath);
        int[][] pixelValues = auxiliary.bytesToPixelMatrix(fileData); //Pixel value matrix from byte array

        if(pixelValues == null){
            //If it can't be read, treat it as a normal file
            return addFile(osFilePath,lockerPathNoFile,fileName,userName,false);
        }

        Integer fileHash = auxiliary.imageHash(pixelValues); //Use a cheap hash to identify possible image matches
        fileContainer myFile = null;
        fileContainer similarImage = null;

        //We spend 12 bytes to store replacement data and therefore get negative data savings when using more than this many byte replacements
        final int maxDifs = pixelValues.length/12 * pixelValues[0].length;
        HashMap<String,Integer> minDifs = new HashMap<>(); //The difs in the file with the minimum difs observed

        //If no other file has your hash, add yourself to the hashmap
        //If any other files have your hash, check if you are identical to any of them. If you are you should reference them
        if(!imageSimilarityMap.containsKey(fileHash)){
            myFile = new fileContainer(fileName,fileData,userName+"/"+lockerPathNoFile);
            imageSimilarityMap.put(fileHash,new LinkedList<fileContainer>());
            imageSimilarityMap.get(fileHash).add(myFile);
        }else{
            //Iterate through the matching hashes and see if you have the same data as any of them
            for(fileContainer compareFile:imageSimilarityMap.get(fileHash)){

                //Don't do any check if the file is a reference
                if(compareFile.isReference)
                    continue;

                int[][] comparePixels =  auxiliary.bytesToPixelMatrix(compareFile.data); //Pixel value matrix from byte array

                //Check if the two pictures are equal
                boolean equal = true;
                if(pixelValues.length != comparePixels.length || pixelValues[0].length != comparePixels[0].length)
                    continue;

                HashMap<String,Integer> currentDifs = new HashMap<>();
                for(int ii = 0; ii  < pixelValues.length;ii++){
                    for(int jj = 0; jj  <pixelValues[0].length;jj++){
                        if(pixelValues[ii][jj] != comparePixels[ii][jj]){
                            currentDifs.put(ii+":"+jj,pixelValues[ii][jj]);
                            equal = false;
                        }
                    }
                }

                if(!equal &&  (currentDifs.size() < minDifs.size() || minDifs.size() == 0) && currentDifs.size() < maxDifs) {
                    similarImage = compareFile;
                    minDifs = currentDifs;
                }else if(equal){
                    myFile = new fileContainer(fileName,compareFile,userName+"/"+lockerPathNoFile);
                    compareFile.filePaths.add(myFile.path+"/"+myFile.name);
                    break;
                }
            }

            if(minDifs.size() > 0){
                myFile = new fileContainer(fileName,similarImage,userName+"/"+lockerPathNoFile);
                myFile.refDifs = minDifs;
                myFile.referenceFile.filePaths.add(myFile.path+"/"+myFile.name);
                imageSimilarityMap.get(fileHash).add(myFile);
            }
            //If you didn't find a match add the new file to the imageSimilarityMap
            else if(myFile == null){
                myFile = new fileContainer(fileName,fileData,userName+"/"+ lockerPathNoFile);
                imageSimilarityMap.get(fileHash).add(myFile);
            }
        }

        myFile.isImage = true;
        return finalFolder.addData(fileName,myFile);
    }

   /**
    * Remove a specified file from the locker
    * @param lockerFilePath The virtual path of the file to remove
    * @param userName the user that you wish to remove the file from
    * @return true if success or false if the file was not found
    */
   public boolean removeFile(String lockerFilePath,String userName){
       File lockerFile = new File(lockerFilePath);
       String fileName = lockerFile.getName();

       if(!root.children.containsKey(userName))
           return false;

       //The folder that contains the file you want to remove
       folder finalFolder = traverseToFolder(userName + "/" + lockerFilePath, false);

       if(finalFolder == null)
           return false;

       fileContainer fileToDelete = finalFolder.getData(fileName);
       if(fileToDelete == null)
           return false;

       //TODO: I think this delete can be done lazily by just adding the data to another file and making it a pointer, leaving all of the other files alone
       //This could potentially lead to a long chain of ops to get data but it may be worth looking into. Maybe if over a certain number of hops then rearrange?

       //If the file you are deleting is an original and is referenced by other files you need to change all of the references that pointed at it
       //TODO: Need to recalculate image difs here
       if(!fileToDelete.isReference && !fileToDelete.filePaths.isEmpty()){
            String newRef = fileToDelete.filePaths.iterator().next();//Get a random element from the hash set
            fileContainer myFile = traverseToFile(newRef);
            if(myFile != null){
                //Move the data to the newRef file
                myFile.filePaths = fileToDelete.filePaths;
                myFile.filePaths.remove(newRef); //Remove yourself from the filePathList
                myFile.isReference = false;
                myFile.referenceFile = null;
                myFile.setData(fileToDelete.getData());

                for(String myFilePath:myFile.filePaths)
                    traverseToFile(myFilePath).referenceFile = myFile;
            }else
                return false;
       }

       finalFolder.removeData(fileName);
       if(unionDetectionMap.containsKey(auxiliary.byteSamplerHash(fileToDelete.getData())))
           unionDetectionMap.get(auxiliary.byteSamplerHash(fileToDelete.getData())).remove(fileToDelete);
       return true;
    }

    ///////////////
    //Locker Data//
    ///////////////

    /**
     * @param lockerFilePath the path in that users locker to return
     * @param userName The name of the user that is accessing their files
     * @return null if the specified file does not exist or the fileContainer representing the file if it does
     */
    public fileContainer getFile(String lockerFilePath,String userName) {
        File lockerFile = new File(lockerFilePath);
        String fileName = lockerFile.getName();

        folder finalFolder = traverseToFolder(userName+"/"+lockerFilePath,false);

        if(finalFolder == null)
            return null;
        else
            return finalFolder.getData(fileName);
    }

    /**
     * Returns the all files (by data) that are only in pathA or only in pathB (ignores folders and file names)
     * @param pathA the path of the first folder
     * @param pathB the path of the second folder
     * @return A string of files that are only in pathA or pathB
     */
   public String directoryDifference(String pathA ,String pathB){
       folder folderA = traverseToFolder(pathA+"/file",false);
       folder folderB = traverseToFolder(pathB+"/file",false);
       if(folderA == null || folderB == null)
           return null;

       return recursiveDif(folderA,pathA,pathB) + recursiveDif(folderB,pathB,pathA);
    }

    /**
     * A helper function for directory differencing. Performs DFS traversal and returns files that are in only one of the paths
     * @param currentFolder the current folder in the directoryStructure to traverse
     * @param prevPath the path without currentFolder
     * @param checkPath the path that you need to check for difs with
     * @return a list of all files below or in the current folder
     */
    private String recursiveDif(folder currentFolder,String prevPath,String checkPath){
        StringBuilder returnString = new StringBuilder();
        String currentPath = "";
        currentPath = prevPath + currentFolder.name+"/";

        //If there are folders in this folder
        if(!currentFolder.children.isEmpty())
            for(folder myFolder:currentFolder.children.values())
                returnString.append(recursiveDif(myFolder,currentPath ,checkPath));

        //If there are files in this folder
        if(!currentFolder.directoryData.isEmpty()){
            for(fileContainer myFile : currentFolder.directoryData.values()){
                //If you don't point at anything and nothing points at you the other directory cannot have this file
                if(!myFile.isReference && myFile.filePaths.isEmpty()){
                    returnString.append(myFile.path).append("/").append(myFile.name).append("\n");
                }else{
                    boolean foundFlag = false;
                    if(myFile.isReference){
                        //If the file you reference is in checkPath
                        if(myFile.referenceFile.path.startsWith(checkPath)){
                            foundFlag = true;
                        }else{
                            //Check to see if checkPath is also pointed at
                            for(String path : myFile.referenceFile.filePaths){
                                if(path.startsWith(checkPath)){
                                    foundFlag = true;
                                    break;
                                }
                            }
                        }
                    }else{
                        //Check to see if checkPath is also pointed at
                        for(String path : myFile.filePaths){
                            if(path.startsWith(checkPath)){
                                foundFlag = true;
                                break;
                            }
                        }
                    }

                    //If you didn't find the other path, you are a unique file among the two directories
                    if(!foundFlag)
                        returnString.append(myFile.path).append("/").append(myFile.name).append("\n");
                }
            }
        }
        return returnString.toString();
    }

    /**
     * Performs a DFS Traversal of the tree and returns a string of all of the files found
     * @param user the user who's files you would like to list
     * @return a string of all of the files that belong to user
     */
   public String listFiles(String user){
        if(!root.children.containsKey(user))
            return "Unknown User";

        String myReturnVal = recursiveList(root.children.get(user),"");
        if(!myReturnVal.isEmpty())
            return myReturnVal;
        else
            return "No Files in the specified directory";
   }

   /**
    * A helper function for directory traversal
    * @param currentFolder the current folder in the directoryStructure to traverse
    * @param prevPath the path without currentFolder
    * @return a list of all files below or in the current folder
    */
   private String recursiveList(folder currentFolder,String prevPath){
        StringBuilder returnString = new StringBuilder();
        String currentPath = "";
        if(!currentFolder.isUser)
            currentPath = prevPath + currentFolder.name+"/";

        //If there are folders in this folder
        if(!currentFolder.children.isEmpty())
           for(folder myFolder:currentFolder.children.values())
                returnString.append(recursiveList(myFolder, currentPath));

        //If there are files in this folder
        if(!currentFolder.directoryData.isEmpty())
            for(fileContainer myFile:currentFolder.directoryData.values())
                returnString.append(currentPath).append(myFile.name).append('\n');

        return returnString.toString();
   }

    /**
     * Traverse through the locker using DFS and does pattern matching of the file contents with the regex
     * @param regex the regular expression to look for in the locker
     * @return an ArrayList of all the files in the locker that match the regex
     */
    public ArrayList<String> regexMatch(String regex) {
        folder currentFolder = root;
        ArrayList<String> matchingFiles = new ArrayList<>(); //Hold the file paths of the files whose contents match the regex

        //This try/catch block checks to see if regex is valid and if not I handle the exception
        try
        {
            Pattern validRegex = Pattern.compile(regex);
        }
        catch (Exception e)
        {
            System.out.println("The regular expression entered has syntax errors");
            return matchingFiles;
        }

        Pattern pattern = Pattern.compile(regex); //The pattern to be matched

        StringBuilder filepath = new StringBuilder();
        Deque<folder> grayStack = new ArrayDeque<>();
        HashMap<folder, Integer> blackSet = new HashMap<>();

        grayStack.push(currentFolder);

        //Traverse all the directories of the locker (DFS)
        while (!grayStack.isEmpty()) {

            boolean isFolderEmpty = true;
            folder head = grayStack.peek();

            for (folder nextFolder : head.children.values()) {
                if (!blackSet.containsKey(nextFolder)) {
                    filepath.append(nextFolder.name).append('/');
                    grayStack.push(nextFolder);
                    isFolderEmpty = false;
                    break;
                }
            }

            //If the next folder is empty or all the folders have been visited
            if (isFolderEmpty) {
                folder removeFolder = grayStack.pop();

                for (fileContainer file : removeFolder.directoryData.values()) {

                    if (!file.isReference) {
                        String fileContents = new String(file.getData(), StandardCharsets.UTF_8);
                        Matcher matcher = pattern.matcher(fileContents);

                        //If the pattern is found in the file then add this filepath and the other paths this file is being stored
                        if (matcher.find()) {
                            filepath.setLength(filepath.length() - 1);
                            filepath.append('/').append(file.name);
                            matchingFiles.add(filepath.toString());

                            //Remove the name of the file in the filepath (not including the /)
                            filepath.delete(filepath.lastIndexOf("/") + 1, filepath.length());

                            //Add all the other filepaths that correspond to this file
                            for (String matchFile : file.filePaths)
                                matchingFiles.add(matchFile);
                        }
                    }
                }

                //Remove the directory name from filepath
                int dirIndex = filepath.lastIndexOf(removeFolder.name);

                //The only reason that the directory name should not exist is if the dir is the root
                if (dirIndex != -1)
                    filepath.delete(dirIndex, filepath.length());

                blackSet.put(removeFolder, 1);
            }
        }

        return matchingFiles;
    }

   //////////////////
   //Tree Traversal//
   //////////////////

   /**
    * Traverse to and return the folder specified by the path
    * @param folderPath the path of the folder to return. This should include the user name and a filename. i.e (username/folder1/folder2/filename.txt)
    * @param createNewFolders whether or not new folders should be created if a folder in the path doesn't exist
    * @return the last folder in the specified path if it exists or is created. null otherwise. i.e(username/folder1/folder2/filename.txt returns folder 2)
    */
   private folder traverseToFolder(String folderPath,boolean createNewFolders){
       //Check if the file path is valid before proceeding
       File f = new File(folderPath);
       try {
           f.getCanonicalPath();
       }
       catch (IOException e) {
           return null;
       }

       String[] directories = folderPath.split("/");

        folder currentFolder = root;

        //If you need to add a user make sure its isUser field is set
        if(createNewFolders && directories.length > 0 && !root.children.containsKey(directories[0])){
            root.addChildDirectory(directories[0]);
            root.children.get(directories[0]).isUser = true;
        }

        //Traverse the directories from the given file path 
        for(int ii = 0; ii < directories.length-1;ii++){
            if(!directories[ii].isEmpty()){
                if(currentFolder.children.containsKey(directories[ii]))
                    currentFolder = currentFolder.children.get(directories[ii]);
                else{
                    //Create new folders if the option is set and the path doesn't exist, else return null
                    if(createNewFolders)
                        currentFolder = currentFolder.addChildDirectory(directories[ii]);
                    else
                        return null;
                }
            }
        }

        return currentFolder;
   }

   /**
    * Traverse to and return a specified fileContainer
    * @param filePath the path of the file to return (This includes user name) i.e username/folder/folder/file
    * @return the file retrieved from the specified path or null if it is not found
    */
   private fileContainer traverseToFile(String filePath){
        folder myFolder = traverseToFolder(filePath, false);
        if(myFolder == null)
            return null;

        String[] directories = filePath.split("/");
        return myFolder.getData(directories[directories.length - 1]);
   }
}
