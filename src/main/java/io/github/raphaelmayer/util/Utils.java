package io.github.raphaelmayer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Correct handling of InterruptedException
        }
    }

    /**
     * Zips a single file and saves it to the specified output file path.
     * 
     * @param inputFilePath  the path to the file to be zipped
     * @param outputFilePath the path where the zip file should be saved
     * @return the full path to the zipped file as a string, or null if an error
     *         occurred
     */
    public static String zipFile(String inputFilePath, String outputFilePath) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFilePath));
                FileInputStream fis = new FileInputStream(inputFilePath)) {

            String fileName = new File(inputFilePath).getName();
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            System.err.println("Error creating zip file: " + e);
            return null; // Return null or consider throwing a custom exception
        }

        return outputFilePath;
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str; // return the string as is if it's null or empty
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static void zipFunctions() {
        System.out.print("Zipping all functions...");
        File functionsDir = new File(Constants.FUNCTION_DIRECTORY);

        if (!functionsDir.exists() || !functionsDir.isDirectory()) {
            System.out.println("Functions directory not found: " + Constants.FUNCTION_DIRECTORY);
            return; // TODO: Replace with a custom exception, if necessary.
        }

        // List all subdirectories within the functions directory
        File[] subDirs = functionsDir.listFiles(File::isDirectory);

        if (subDirs == null || subDirs.length == 0) {
            System.out.println("No subdirectories found in functions directory.");
            return;
        }

        // Iterate over each subdirectory
        for (File subDir : subDirs) {
            File[] jsFiles = subDir.listFiles((dir, name) -> name.endsWith(".mjs"));

            if (jsFiles == null || jsFiles.length == 0) {
                System.out.println("No .mjs files found in directory: " + subDir.getName());
                continue;
            }

            // Zip each .mjs file in the current subdirectory
            for (File jsFile : jsFiles) {
                String zipFilePath = jsFile.getAbsolutePath().replace(".mjs", ".zip");
                Utils.zipFile(jsFile.getAbsolutePath(), zipFilePath);
            }
        }
        System.out.println("done.");
    }

}
