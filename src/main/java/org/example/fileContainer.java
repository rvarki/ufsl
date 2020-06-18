package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

//Contains the data and metadata associated with a single file in the locker
//TODO: This class needs setters and getters rather than all public data
public class fileContainer implements Serializable {

    //Data
    public String name; //The name of this instance of the file
    public String path; //The path that this file resides in
    public byte[] data; //The actual data contained by this fileContainer if it is not a reference

    //Reference data
    public boolean isImage;
    public HashSet<String> filePaths; //The path(s) that this file is being stored at. Used in the event that this file is deleted but is referenced elsewhere
    public HashMap<String,Integer> refDifs;
    public boolean isReference; //True if this fileContainer is a pointer to another file
    public fileContainer referenceFile; //The file that is referenced if there is one

    public fileContainer(String Name,fileContainer referenceFile,String path){
        this.data = null;
        this.name = Name;
        this.isReference = true;
        this.filePaths = new HashSet<>();
        this.referenceFile = referenceFile;
        this.path = path;
        this.isImage = false;
        this.refDifs = new HashMap<>();
    }

    public fileContainer(String Name,byte[] Data,String path){
        this.data = Data;
        this.name = Name;
        this.isReference = false;
        this.filePaths = new HashSet<>();
        this.path = path;
        this.referenceFile = null;
        this.isImage = false;
        this.refDifs = new HashMap<>();

    }


    public byte[] getData(){
        if(!isImage || !isReference){
            if(isReference)
                return referenceFile.getData();
            else
                return data;
        }else{
            //If you have an image that references another image, you need to add the changes back before you return it
            InputStream in = new ByteArrayInputStream(referenceFile.getData().clone());
            BufferedImage bufferedImage = null;
            try {
                bufferedImage = ImageIO.read(in);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Invalid Input");
            }
            for (Map.Entry<String, Integer> kv:refDifs.entrySet()) {
                String[] x_y = kv.getKey().split(":");
                bufferedImage.setRGB(Integer.parseInt(x_y[0]),Integer.parseInt(x_y[1]),kv.getValue());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                if(this.name.endsWith(".png"))
                    ImageIO.write(bufferedImage, "png", baos);
                else if(this.name.endsWith(".jpg"))
                    ImageIO.write(bufferedImage, "jpg", baos);
                else{
                    System.out.println("Invalid file format. Code should not reach here");
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return baos.toByteArray();
        }
    }

    public void setData(byte[] Data){
        this.data = Data;
    }
}
