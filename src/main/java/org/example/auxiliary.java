package org.example;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class auxiliary {
    /**
     * takes a path to a file and returns the bytes stored in the file
     * @param filePath the path of the file that you would like to read from
     */
    public static byte[] readBytesFromFile(String filePath) {
        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return bytesArray;
    }

    /**
     * A constant time hash for byte vectors that uses random sampling. This is likely to give the same hash for similar files
     * @param myFileData The byte vector of the data you would like to hash
     * @return a hash of the input array
     */
    public static Integer byteSamplerHash(byte[] myFileData){
        final int samples = 10;

        String inputString = new String(myFileData, StandardCharsets.UTF_8);
        if(inputString.isEmpty())
            return 0;

        Random myRand = new Random();

        //Seed with hash of first "samples" chars
        String mySubStr = inputString.substring(0,Math.min(samples,inputString.length()));
        myRand.setSeed(mySubStr.hashCode());

        //Sample chars at next "samples" rand ints
        String randSample = "";
        for(int ii = 0;ii < samples;ii++)
            randSample += inputString.charAt(myRand.nextInt(inputString.length()));

        return randSample.hashCode();
    }

    /**
     * Converts a byte array into an rgb pixel matrix
     * @param fileData the byte representation of the file you wish to convert
     * @return a matrix containing the rbg values of each pixel
     */
    public static int[][] bytesToPixelMatrix(byte[] fileData){
        //Get pixel value for my image
        InputStream in = new ByteArrayInputStream(fileData);
        try {
            BufferedImage bufferedImage = ImageIO.read(in);
            in.close();

            if(bufferedImage == null)
                return null;

            int[][] pixelValues = new int[bufferedImage.getWidth()][bufferedImage.getHeight()];

            for(int ii = 0; ii < bufferedImage.getWidth();ii++)
                for(int jj = 0; jj < bufferedImage.getHeight();jj++)
                    pixelValues[ii][jj] = bufferedImage.getRGB(ii,jj);

            return pixelValues;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Invalid Input");
            return null;
        }


    }

    /**
     * A constant time hash for images that uses random sampling. This is likely to give the same hash for similar files
     * @param myFileData The int matrix representing your images pixel values
     * @return a hash of the input
     */
    public static Integer imageHash(int[][] myFileData){
        final int samples = Math.min(myFileData.length,10);

        if(myFileData.length == 0)
            return 0;

        Random myRand = new Random();

        //Seed with hash of first "samples" chars
        StringBuilder seed = new StringBuilder();
        for(int ii = 0;ii < samples;ii++)
            seed.append(myFileData[ii][0]);

        myRand.setSeed(seed.hashCode());

        //Sample chars at next "samples" rand ints
        StringBuilder randSample = new StringBuilder();
        for(int ii = 0;ii < samples;ii++)
            randSample.append(myFileData[myRand.nextInt(myFileData.length - 1)][myFileData[0].length - 1]);

        return randSample.toString().hashCode();
    }
}

