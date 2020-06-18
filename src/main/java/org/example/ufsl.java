package org.example;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.lang.Runtime;

import com.beust.jcommander.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ufsl  extends Application {
    public static directoryStructure myDirectory;

    enum opperation{
        addFile,
        retrieve,
        remove,
        dirDif,
        regex,
        list,
        GUI,
    }

    final static InputParameters mainArgs = new InputParameters();

    public static void main( String[] args ) throws IOException, ClassNotFoundException {
        String input = "";

        JCommander jCommander = new JCommander(mainArgs);
        try {
            jCommander.parse(args);
        }catch (ParameterException e){
            System.out.println("Bad input. Please try again");
            return;
        }
        //Load the locker from disk or create one if the specified file doesn't exist
        File check = new File(mainArgs.getLockerName());
        if (check.length() == 0) {
            myDirectory = new directoryStructure();
        }
        else{
            FileInputStream fis = new FileInputStream(mainArgs.getLockerName());
            ObjectInputStream ois = new ObjectInputStream(fis);
            myDirectory = (directoryStructure) ois.readObject();
            ois.close();
        }

        //Count the non lockerName params to decide whether to loop or not
        int paramsCount = 0;
        boolean guiCalled = false;
        for(String params:args){
            if(!params.equals("--lockerName") && !params.equals(mainArgs.getLockerName()) && !params.equals("--GUI") && !params.equals("--test"))
                paramsCount++;
            if(params.equals("--GUI"))
                guiCalled = true;
        }

        if(guiCalled){
            System.out.println("GUI can only be run from interactive mode");
            System.exit(1);
        }

        //Make sure that regardless of how the process ends, the locker is serialized back into a file
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try{
                    File f = new File(mainArgs.getLockerName());
                    FileOutputStream fos = new FileOutputStream(f);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(myDirectory);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        boolean exit = true;

        CLI_LOOP:
        do{
            jCommander = new JCommander(mainArgs);
            String[] myArgs = {};
            try {
                if (paramsCount != 0) {
                    myArgs = args;
                    input = "quit";
                } else {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Enter Command: ");
                    input = scanner.nextLine();
                    if(input.equals("quit"))
                        break;
                    myArgs = input.split(" ");
                }
                jCommander.parse(myArgs);
            } catch (ParameterException exception) {
                jCommander.usage();
                if(paramsCount != 0)
                    return;
                else
                    continue;
            }

            String username = "";
            String virtualPath = "";
            String comparePath = "";
            String osPath = "";
            opperation myOpperation = null;


            //Parse command line args
            for (String option : myArgs) {
                FilePathExistsParameter doesPathExist = new FilePathExistsParameter();

                if((option.equals("--GUI") && myArgs.length > 1)){
                    System.out.println("GUI must be run alone");
                    continue CLI_LOOP;
                }
                else if(option.equals("--GUI")) {
                    myOpperation = opperation.GUI;
                    try {
                        launch(args);
                    } catch (Exception e)
                    {
                        System.out.println("GUI can only be used once per session. Please rerun application to use GUI again.");
                    }
                }

                if (option.equals("-user"))
                    username = mainArgs.getUserName();

                if (option.equals("--addFile")) {
                    if (doesPathExist.osPath(mainArgs.getAddPaths().get(0))) {
                        myOpperation = opperation.addFile;
                        osPath = mainArgs.getAddPaths().get(0);
                        virtualPath = mainArgs.getAddPaths().get(1);
                    } else {
                        System.out.println("The file does not exist on the OS");
                        continue CLI_LOOP;
                    }
                } else if (option.equals("--retrieve")) {
                    myOpperation = opperation.retrieve;
                    String[] myPath = mainArgs.getRetrievePaths().get(0).split("/");
                    osPath = mainArgs.getRetrievePaths().get(1) + "/" + myPath[myPath.length - 1];
                    virtualPath = mainArgs.getRetrievePaths().get(0);
                } else if (option.equals("--remove")) {
                    myOpperation = opperation.remove;
                    virtualPath = mainArgs.getRemovePath();
                } else if (option.equals("--list")){
                    myOpperation = opperation.list;
                } else if (option.equals("--regex")) {
                    myOpperation = opperation.regex;
                } else if (option.equals("--dirDif")) {
                    myOpperation = opperation.dirDif;
                    virtualPath = mainArgs.getdirDifPaths().get(0);
                    comparePath = mainArgs.getdirDifPaths().get(1);
                } else if(option.equals("--test"))
                    exit = false;
            }

            if (username.isEmpty() && myOpperation != opperation.dirDif &&  myOpperation != opperation.regex && myOpperation != opperation.GUI) {
                System.out.println("No Username Provided");
                if(input.equals("quit"))
                    return;
                else
                    continue;
            }

            if(myOpperation != null){
                switch (myOpperation) {
                    case addFile:
                        String fileName = Paths.get(osPath).getFileName().toString();
                        if(myDirectory.addFile(osPath, virtualPath, Paths.get(osPath).getFileName().toString(), username,fileName.endsWith(".png"))) {
                            System.out.println("Successfully added your new file at: " + username + "/" + virtualPath);
                        }else
                            System.out.println("Failed to add your file. If the file already exists, you will need to remove it before trying to add again");
                        break;
                    case retrieve:
                        fileContainer myFile = myDirectory.getFile(virtualPath, username);
                        if (myFile == null)
                            System.out.println("File not found");
                        else {
                            //Try to write the byte stream back to the file
                            try {
                                File writeFile = new File(osPath);
                                if(!writeFile.createNewFile())
                                    throw new FileAlreadyExistsException("The directory you are trying to write to already has a file with this name");
                                OutputStream os = new FileOutputStream(writeFile);
                                os.write(myFile.getData());
                                System.out.println("Successfully retrieved file: " + username + "/" + virtualPath);
                                os.close();
                            } catch (Exception exception) {
                                System.out.println("Exception writing to file: " + exception);
                            }
                        }
                        break;
                    case remove:
                        if (myDirectory.removeFile(virtualPath, username))
                            System.out.println("Successfully removed file: " + username + "/" + virtualPath);
                        else
                            System.out.println("Failed to remove file - bad username or file path");
                        break;
                    case dirDif:
                        String difs = myDirectory.directoryDifference(virtualPath, comparePath);
                        if (difs == null)
                            System.out.println("One or more of your input paths does not exist");
                        else if (difs.isEmpty())
                            System.out.println("Directories are identical!");
                        else
                            System.out.println(difs);
                        break;
                    case regex:
                        ArrayList<String> matchingFiles = myDirectory.regexMatch(mainArgs.getRegex());

                        if (matchingFiles.size() == 0)
                            System.out.println("There are no files that match the regex");
                        else {
                            for (String matchingFile : matchingFiles)
                                System.out.println(matchingFile);
                        }
                        break;
                    case list:
                        System.out.println(myDirectory.listFiles(username));
                        break;
                }
            }
        }while(!input.equals("quit"));

        try {
            File f = new File(mainArgs.getLockerName());
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
        primaryStage.setTitle("Union File Locker");
        primaryStage.setScene(new Scene(root, 800, 500));
        primaryStage.show();
    }
}
