package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

public class Controller implements Serializable {
    @FXML
    private Label usernamePrompt;

    @FXML
    private Label label;

    @FXML
    private Label dropped;

    @FXML
    private ListView<String> dirDiff;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField lockerDest;

    @FXML
    private TextField osDest;

    @FXML
    private TextField regexField;

    @FXML
    private TextField subdir1;

    @FXML
    private TextField subdir2;

    @FXML
    private Button loginBtn;

    @FXML
    private ListView<String> pathList;

    @FXML
    private VBox dragTarget;

    ObservableList list = FXCollections.observableArrayList();
    ObservableList list2 = FXCollections.observableArrayList();


    public String dragPath;
    public directoryStructure myDirectory = ufsl.myDirectory;
    boolean loginStat = false;


    public void Login(ActionEvent event) throws IOException, ClassNotFoundException {
        if (!usernameField.getText().isEmpty() && usernameField.getText() != null) {
            String[] paths = createPathsArray(myDirectory, usernameField.getText());
            populateDirs(paths);
            dragPath = handleDrag();
            usernameField.setStyle(null);
            loginStat = true;
        }else{
            usernameField.clear();
            usernameField.setPromptText("Enter username...");
            usernameField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }

    }

    public void AddFileAction(ActionEvent event){
        if(loginStat){
            if(!lockerDest.getText().isEmpty() && dragPath != null){
                myDirectory.addFile(dragPath, lockerDest.getText(), Paths.get(dragPath).getFileName().toString(),usernameField.getText(),Paths.get(dragPath).getFileName().toString().endsWith("png"));
                String[] paths = createPathsArray(myDirectory, usernameField.getText());
                populateDirs(paths);
                lockerDest.clear();
                dropped.setText("");
            }else if(dragPath == null){
                lockerDest.clear();
                lockerDest.setPromptText("Please drag file");
                lockerDest.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
            }else{
                lockerDest.clear();
                lockerDest.setPromptText("Please enter locker destination");
                lockerDest.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
            }
        }else{
            usernameField.clear();
            usernameField.setPromptText("Login first");
            usernameField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }
    }

    public void RetrieveFileAction(ActionEvent event){
        if(loginStat && pathList.getSelectionModel().getSelectedItem() != null && !osDest.getText().trim().isEmpty()){
            fileContainer myFile = myDirectory.getFile(pathList.getSelectionModel().getSelectedItem(), usernameField.getText());
            String[] myPath = pathList.getSelectionModel().getSelectedItem().split("/");
            String osPath =  osDest.getText() + "/" + myPath[myPath.length - 1];
            if(myFile == null){
                osDest.clear();
                osDest.setPromptText("Please enter proper os path");
                osDest.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
                osDest.clear();
            }else{
                try {
                    File writeFile = new File(osPath);
                    if(!writeFile.createNewFile())
                        throw new FileAlreadyExistsException("The directory you are trying to write to already has a file with this name");
                    OutputStream os = new FileOutputStream(writeFile);
                    os.write(myFile.getData());
                    System.out.println("Successfully retrieved file: " + usernameField.getText() + "/" + pathList.getSelectionModel().getSelectedItem());
                    os.close();
                } catch (Exception exception) {
                    System.out.println("Exception writing to file: " + exception);
                }
                osDest.clear();
            }
        }else if(!loginStat){
            usernameField.clear();
            usernameField.setPromptText("Login first");
            usernameField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }else if(pathList.getSelectionModel().getSelectedItem() == null){
            osDest.clear();
            osDest.setPromptText("Please select directory");
            osDest.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }else{
            osDest.clear();
            osDest.setPromptText("Please enter OS path");
            osDest.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }
    }

    public void RegexAction(ActionEvent event){
        if(loginStat && !regexField.getText().trim().isEmpty()){
            ArrayList<String> matchingFiles = myDirectory.regexMatch(regexField.getText());
            if(matchingFiles.size() == 0){
                regexField.clear();
                regexField.setPromptText("No matches");
                regexField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
            }else{
                String[] paths= new String[matchingFiles.size()];
                for(int i = 0; i < matchingFiles.size(); i++){
                    paths[i] = matchingFiles.get(i);
                }
                populateDirs(paths);
                regexField.clear();
            }
        }else if(regexField.getText().trim().isEmpty()){
            String[] paths = createPathsArray(myDirectory, usernameField.getText());
            populateDirs(paths);
        }else{
            usernameField.clear();
            usernameField.setPromptText("Login first");
            usernameField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }
    }

    public void DirDiffAction(ActionEvent event){
        dirDiff.getItems().clear();
        if(loginStat && !subdir1.getText().trim().isEmpty() && !subdir2.getText().trim().isEmpty()){
            list2.removeAll(list2);
            String difs = myDirectory.directoryDifference(subdir1.getText(), subdir2.getText());
            subdir1.clear();
            subdir2.clear();
            if (difs == null)
               dirDiff.getItems().add("One or more of your input paths does not exist");
            else if (difs.isEmpty())
                dirDiff.getItems().add("Directories are identical!");
            else {
                String paths[] = difs.split("\\r?\\n");
                for (int i = 0; i < paths.length; i++) {
                    list2.add(paths[i]);
                }
                dirDiff.getItems().addAll(list2);
            }
        }else if(subdir1.getText().trim().isEmpty()){
            subdir1.clear();
            subdir1.setPromptText("Enter sub directory 1");
            subdir1.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }else if(subdir2.getText().trim().isEmpty()){
            subdir2.clear();
            subdir2.setPromptText("Enter sub directory 2");
            subdir2.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }else{
            usernameField.clear();
            usernameField.setPromptText("Login first");
            usernameField.setStyle("-fx-border-color: red ; -fx-border-width: 1px ;");
        }

    }

    public String handleDrag() {
        label.setText("Drag files here");
        dropped = new Label("");
        label.setFont(new Font("Arial", 16));
        dropped.setFont(new Font("Arial", 16));
        dragTarget.getChildren().addAll(dropped);
        dragTarget.setStyle("-fx-border-color: black ; -fx-border-width: 1px ;");
        dragTarget.setOnDragOver(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != dragTarget
                        && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });

        dragTarget.setOnDragDropped(new EventHandler<DragEvent>() {

            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    dragPath = db.getFiles().toString().substring(1, db.getFiles().toString().length()-1);
                    dropped.setText("     "+dragPath);
                    success = true;
                }
                /* let the source know whether the string was successfully
                 * transferred and used */
                event.setDropCompleted(success);

                event.consume();
            }
        });

        return dragPath;
    }

    public void populateDirs(String[] paths){
        pathList.getItems().clear();
        list.removeAll(list);

        list.add("Your directories:\n");
        if(paths[0] == "Unknown User"){
            list.add("No files...");
        }else{
            for(int i = 0; i < paths.length; i++){
                list.add(paths[i]);
            }
        }

       pathList.getItems().addAll(list);
    }

    public String[] createPathsArray(directoryStructure directory, String username){
        String lines[] = directory.listFiles(username).split("\\r?\\n");

        return lines;
    }


}
