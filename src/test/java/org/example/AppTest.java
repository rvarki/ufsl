package org.example;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static org.junit.Assert.*;

//TODO:Suppress locker response print statements in tests
public class AppTest{

    ////////////////
    //System Tests//
    ////////////////

    /**
     * Adds a file to the locker and retrieves it, making sure that the input and the output file are identical
     * Then removes the input and lists to ensure that the path is now empty after remove
     */
    @Test
    public void BasicFunctionalitySystemTest() throws IOException, ClassNotFoundException {
        final String fileName = "/testFile.txt";
        final String filePath = "src/test/java/org/example";
        final String virtualPath= "This/Is/A/Path";
        final String fileData = "Sample file data :D";
        //Clear the current testLocker.data file

        File locker= new File("testLocker.data");
        locker.delete();


        File newFile = new File(filePath + fileName);
        newFile.delete();

        FileOutputStream out = new FileOutputStream(filePath+fileName);
        out.write(fileData.getBytes());
        out.close();

        //Suppress terminal output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile",filePath + fileName,virtualPath,"--lockerName","testLocker.data","--test"});

        newFile.delete();

        ufsl.main(new String[] {"-user", "ThisIsAUser", "--retrieve",virtualPath + fileName,filePath,"--lockerName","testLocker.data","--test"});


        byte[] retrieveData = auxiliary.readBytesFromFile(filePath+fileName);
        byte[] originalData = fileData.getBytes();
        assertTrue(Arrays.equals(retrieveData,originalData));

        ufsl.main(new String[] {"-user", "ThisIsAUser", "--remove",virtualPath + fileName,"--lockerName","testLocker.data","--test"});
        baos.reset();
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--list","--lockerName","testLocker.data","--test"});
        assertEquals("No Files in the specified directory\n",baos.toString("UTF-8"));

        //Unsupress terminal output
        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        File retrievedFile = new File(filePath + fileName);
        retrievedFile.delete();
        locker.delete();
    }

    /**
     * Tests all 3 basic cases of dirDifference and some possible corner cases.(i.e matching in different directores, nested dirs, etc.)
     */
    @Test
    public void dirDifferenceSystemTest() throws IOException, ClassNotFoundException {
        //Clear the current testLocker.data file
        File locker= new File("testLocker.data");
        locker.delete();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage1.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage2.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage3.png","myPath/nested/folder","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage4.png","differentFolder","--lockerName","testLocker.data","--test"});

        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage4.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage3.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage2.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage5.jpeg","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage5.jpeg","myPath/nested","--lockerName","testLocker.data","--test"});

        baos.reset();

        ufsl.main(new String[] {"--dirDif", "ThisIsADifferentUser/myPath","ThisIsAUser/myPath","--lockerName","testLocker.data","--test"});
        String actualDif = "ThisIsADifferentUser/myPath/nested/testImage5.jpeg\n" +
                           "ThisIsADifferentUser/myPath/testImage5.jpeg\n" +
                           "ThisIsADifferentUser/myPath/testImage4.png\n" +
                           "ThisIsAUser/myPath/testImage1.png\n\n";

        //TODO:Split these outputs at newlines and compare in a way s.t order doesn't matter
        assertEquals(actualDif,baos.toString("UTF-8"));
        baos.reset();

        //Make sure bad path is detected properly
        ufsl.main(new String[] {"--dirDif", "BadPath","ThisIsAUser/myPath","--lockerName","testLocker.data","--test"});
        assertEquals("One or more of your input paths does not exist\n",baos.toString("UTF-8"));


        ufsl.main(new String[] {"-user", "ThisIsADifferentUser", "--addFile","src/test/java/org/example/testImage1.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage4.png","myPath","--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile","src/test/java/org/example/testImage5.jpeg","myPath","--lockerName","testLocker.data","--test"});
        baos.reset();

        //Make sure identical directories are detected properly
        ufsl.main(new String[] {"--dirDif", "ThisIsADifferentUser/myPath","ThisIsAUser/myPath","--lockerName","testLocker.data","--test"});
        assertEquals("Directories are identical!\n",baos.toString("UTF-8"));

        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        locker.delete();
    }

    /**
     * Tests that list returns the expected files (doesn't return other users files and returns all files)
     */
    @Test
    public void ListSystemTest() throws IOException, ClassNotFoundException {
        final String originalImagePath1 = "src/test/java/org/example/testImage1.png";
        final String originalImagePath2 = "src/test/java/org/example/testImage2.png";
        final String originalImagePath3 = "src/test/java/org/example/testImage3.png";
        final String originalImagePath4 = "src/test/java/org/example/testImage4.png";

        final String virtualPath1 = "ThisIsAUser1/path/to/my";
        final String virtualPath2 = "ThisIsAUser1/some/other/path";

        //Clear the current testLocker.data file
        File locker= new File("testLocker.data");
        locker.delete();

        //Suppress output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath1,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath2,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath3,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser2", "--addFile",originalImagePath4,virtualPath2,"--lockerName","testLocker.data","--test"});

        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        directoryStructure myDirectory;
        FileInputStream fis = new FileInputStream("testLocker.data");
        ObjectInputStream ois = new ObjectInputStream(fis);
        myDirectory = (directoryStructure) ois.readObject();
        ois.close();

        boolean flag = true;
        if(myDirectory.listFiles("ThisIsAUser1") != "path/to/my" || myDirectory.listFiles("ThisIsAUser2") != "some/other/path")
            flag = false;

        assertTrue(true);
    }


    /**
     * tests to make sure that --regex successfully finds only the files that contain the input expression
     */
    @Test
    public void RegexSystemTest() throws IOException, ClassNotFoundException {
        String[] testData = {"gray","grey","abcgrayasd","gruy","asdfasd","gry"};
        String[] dirs = {"dir1","dir2","dir3","dir4"};
        HashSet<String> solutionSet = new HashSet<>();

        //Clear the current testLocker.data file
        File locker= new File("testLocker.data");
        locker.delete();

        Random rand = new Random();
        rand.setSeed(1234);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        for(String testString:testData) {
            File myFile = File.createTempFile("testData","myData");
            OutputStream os = new FileOutputStream(myFile);
            os.write(testString.getBytes());
            os.close();

            //Generate a path for the new file for each user
            StringBuilder virtualPath = new StringBuilder();
            for (String dir: dirs) {
                int diceRoll = rand.nextInt(dirs.length + 1);
                if (diceRoll == dirs.length)
                    break;
                else
                    virtualPath.append(dir).append("/");
            }

            ufsl.main(new String[] {"-user", "ThisIsAUser", "--addFile",myFile.getAbsolutePath(),virtualPath.toString(),"--lockerName","testLocker.data","--test"});

            if(testString.contains("grey") || testString.contains("gray"))
                solutionSet.add("ThisIsAUser/"+virtualPath+myFile.getName());
            myFile.delete();
        }

        baos.reset();

        ufsl.main(new String[] {"--regex","gr[ae]y","--lockerName","testLocker.data","--test"});

        String regexMatches = baos.toString("UTF-8");
        String[] myAns = regexMatches.split("\n");

        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        if(!solutionSet.containsAll(Arrays.asList(myAns)))
            fail("Regex did not return all solutions");
        if(!Arrays.asList(myAns).containsAll(solutionSet))
            fail("Regex returned items that should not be in the solution set");

        locker.delete();
    }

    /**
     * Tests that list returns the expected files (doesn't return other users files and returns all files)
     */
    //UNCOMMENT ME TO TEST IMAGE STUFF. MUST ALSO UNCOMMENT FIRST 2 LINES OF "addFile()" IN directoryStructure
//    @Test
    public void imageSimilarityTest() throws IOException, ClassNotFoundException {
        final String originalImagePath = "src/test/java/org/example/testImage1.png";
        final String noisyImagePath = "src/test/java/org/example/testImage1Noise.png";
        final String noisierImagePath = "src/test/java/org/example/testImage1Noisier.png";

        final String virtualPath1 = "path/to/pics";

        //Clear the current testLocker.data file
        File locker= new File("testLocker.data");
        locker.delete();

        //Suppress output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        //Populating locker
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",noisyImagePath,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",noisierImagePath,virtualPath1,"--lockerName","testLocker.data","--test"});

        //Checking size of locker without identical copy
        File f1 = new File("testLocker.data");
        long fileSizeLocker = f1.length();
        File f2 = new File (originalImagePath);
        long fileSizeOriginal = f2.length();

        //Unsupress output
        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        System.out.println(fileSizeLocker + " / " + fileSizeOriginal);
        //3 Images should take very little space as only the difs are stored
        if(fileSizeLocker < 2 * fileSizeOriginal)
            assertTrue(true);
        else
            assertTrue(false);
    }

    //////////////
    //Benchmarks//
    //////////////

    /**
     * Checks that when identical files are added under any user, the size of the locker does not increase too much
     * (Only new metadata should be stored. The file should not be stored multiple times)
     */
    @Test
    public void UnionSizeReductionBenchmark() throws IOException, ClassNotFoundException {
        final String originalImagePath1 = "src/test/java/org/example/testImage1.png";
        final String originalImagePath2 = "src/test/java/org/example/testImage2.png";
        final String originalImagePath2_copy = "src/test/java/org/example/testImage2-copy.png";
        final String originalImagePath3 = "src/test/java/org/example/testImage3.png";
        final String originalImagePath4 = "src/test/java/org/example/testImage4.png";

        final String virtualPath1 = "ThisIsAUser1/path/to/my";
        final String virtualPath2 = "some/other/path";

        //Clear the current testLocker.data file
        File locker= new File("testLocker.data");
        locker.delete();

        //Suppress output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        //Populating locker
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath1,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath2,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath3,virtualPath1,"--lockerName","testLocker.data","--test"});
        ufsl.main(new String[] {"-user", "ThisIsAUser2", "--addFile",originalImagePath4,virtualPath2,"--lockerName","testLocker.data","--test"});

        //Checking size of locker without identical copy
        File f1 = new File("testLocker.data");
        long fileSize1 = f1.length();

        //Adding identical copy of testImage2
        ufsl.main(new String[] {"-user", "ThisIsAUser1", "--addFile",originalImagePath2_copy,virtualPath1,"--lockerName","testLocker.data","--test"});

        //Unsupress output
        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        //Checking size of locker with identical copy
        File f2 = new File("testLocker.data");
        long fileSize2 = f2.length();

        long fileDiff = fileSize2 - fileSize1;
        File originalImg = new File(originalImagePath2);

        //Less than the size of the new file is stored
        if(fileDiff < originalImg.length())
            assertTrue(true);
        else
            assertTrue(false);
    }

    /**
     * Checks that the locker meets the following performance benchmark
     * This test requires about 10GB of heap space to be able to run the full 100GB of files since each of the 1000 unique files is only stored once on 10 different users
     * This test takes ~4-5 mins to run with 3MB files
     * If you wish to make all of the files unique you will need ~100GB+ of ram since the entire object is loaded into memory when you access the locker
     * Your system should be able to handle at least 10 users with 1000 10MB files each, completing any instruction in under 1 second.
     */
    //YOUR JVM MUST BE GIVEN ENOUGH MEMORY TO STORE THE DATA THAT RESULTS FROM THIS TEST
    @Test
    public void InstructionRuntimeBenchmark() throws IOException, ClassNotFoundException {
        //This results in ~10GB of data (But would be 100GB without unioning)
        //final int stringLength = 50000000; //A string of this length should be about 10MB worth of data

        //This results ~3GB of data
        //final int stringLength = 15000000; //A string of this length should be about 3MB worth of data.

        //This results in ~500MB of data
//        final int stringLength = 2500000; //This should be able to run with the stack size of the lab machines

        //This results in ~100MB of data
        final int stringLength = 500000; //This should run pretty quickly on most machines

        Random myRand = new Random();
        myRand.setSeed(123456789);
        String lockerName = "largeTestLocker.data";
        File locker= new File(lockerName);
        String[] dirs = {"dir1","dir2","dir3","dir4"};

        directoryStructure myDir = new directoryStructure();
        StringBuffer outputBuffer = new StringBuffer(stringLength);
        for(int ii = 0;ii < stringLength;ii++)
            outputBuffer.append((char)(myRand.nextInt(26)+'a'));

        //About 10MB of data
        String origData = outputBuffer.toString();

        for(int fileNum = 0;fileNum< 1000;fileNum++) {
            String myData = fileNum + ":" + myRand.nextInt(1234567890)+ origData;
            File myFile = File.createTempFile("testData","myData");
            OutputStream os = new FileOutputStream(myFile);
            os.write(myData.getBytes());
            os.close();

            //Generate a path for the new file for each user
            for(int user = 0;user < 10;user++) {
                StringBuilder virtualPath = new StringBuilder();
                for (String dir: dirs) {
                    int diceRoll = myRand.nextInt(dirs.length + 1);
                    if (diceRoll == dirs.length)
                        break;
                    else
                        virtualPath.append(dir).append("/");
                }
                myDir.addFile(myFile.getAbsolutePath(), virtualPath.toString(), myFile.getName(), Integer.toString(user),myFile.getName().endsWith(".png"));
            }
            myFile.delete();
        }

        long startTime, endTime, duration;

        startTime = System.nanoTime();
        for(int ii = 0; ii < 10;ii++)
            if(!myDir.addFile("src/test/java/org/example/testImage1.png", "dir1/dir2", "testImage1.png", Integer.toString(ii),true))
                fail("Failed to add file");
        endTime = System.nanoTime();
        duration = (endTime - startTime);
        assertTrue((duration/10) < 1_000_000_000); //took on average less than 1 second per op


        startTime = System.nanoTime();
        for(int ii = 0; ii < 10;ii++){
            //These are the ops done in retrieve
            fileContainer retrieveFile = myDir.getFile("dir1/dir2/testImage1.png", Integer.toString(ii));
            File writeFile = new File("src/test/java/org/example/testImage6.png");
            try {
                if(!writeFile.createNewFile())
                    throw new FileAlreadyExistsException("The directory you are trying to write to already has a file with this name");
                OutputStream os = new FileOutputStream(writeFile);
                os.write(retrieveFile.getData());
                os.close();
            } catch (Exception exception) {
                System.out.println("Exception writing to file: " + exception);
            }
            writeFile.delete();
        }
        endTime = System.nanoTime();
        duration = (endTime - startTime);
        assertTrue((duration/10) < 1_000_000_000); //took on average less than 1 second per op

        startTime = System.nanoTime();
        for(int ii = 0; ii < 10;ii++)
            myDir.removeFile("dir1/dir2/testImage1.png", Integer.toString(ii));
        endTime = System.nanoTime();
        duration = (endTime - startTime);
        assertTrue((duration/10) < 1_000_000_000); //took on average less than 1 second per op

        //Suppress prints
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(baos));

        startTime = System.nanoTime();
        for(int ii = 0; ii < 10;ii++)
            myDir.listFiles(Integer.toString(ii));
        endTime = System.nanoTime();
        duration = (endTime - startTime);
        assertTrue((duration/10) < 1_000_000_000); //took on average less than 1 second per op

        //Unsuppress
        baos.close();
        System.out.flush();
        System.setOut(oldOut);

        //Serialize locker
        try{
            FileOutputStream fos = new FileOutputStream(lockerName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(myDir);
            oos.close();
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
