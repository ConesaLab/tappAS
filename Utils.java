/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.Chart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import tappas.DataDFI.DFISelectionResults;
import tappas.DataDIU.DIUSelectionResults;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

//
// Static functions class - no need to instantiate
//

public class Utils {
    public static void showAlertLater(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
    public static String properCase(String str) {
        if(!str.isEmpty())
            str = str.substring(0, 1).toUpperCase() + str.substring(1);
        return str;
    }
    // Note: will replace file if it already exists
    public static boolean copyFile(Path fromPath, Path toPath) {
        boolean result = false;
        try {
            Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
            result = true;
        }
        catch (IOException e) {
            Tappas.getApp().logInfo("File copy exception: " + e.getMessage());
        }
        return result;
    }
    public static boolean makeFileWritable(Path filePath) {
        boolean result = false;
        if(Files.exists(filePath)) {
            File file = new File(filePath.toString());
            file.setWritable(true);
            result = true;
        }
        return result;
    }
    public static void removeFile(String filePath) {
        if(filePath != null && !filePath.isEmpty()) {
            try {
                removeFile(Paths.get(filePath));
            }
            catch (Exception e) { 
                Tappas.getApp().logInfo("Remove file path exception (" + filePath + "): " + e.getMessage());
            }
        }
    }
    public static void removeFile(Path filePath) {
        if(filePath != null && Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            }
            catch (IOException e) { 
                Tappas.getApp().logInfo("Remove file exception (" + filePath + "): " + e.getMessage());
            }
        }
    }
    // removes all files from folder except parameter files if flag not set - not recursive
    public static void removeAllFolderFiles(Path pathFolder, boolean rmvPrms) {
        File folder = new File(pathFolder.toString());
        if(folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    if(files[i].isFile()) {
                        if(rmvPrms || !files[i].getName().toLowerCase().endsWith(".prm")) {
                            Path path = Paths.get(pathFolder.toString(), files[i].getName());
                            removeFile(path);
                        }
                    }
                }
            }
        }
    }
    // get list of sub folders in the given folder
    public static ArrayList<String> getFolderSubFolders(Path pathFolder) {
        ArrayList<String> lst = new ArrayList();
        
        File folder = new File(pathFolder.toString());
        if(folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    if(files[i].isDirectory()) {
                        lst.add(files[i].getName());
                    }
                }
            }
        }
        return lst;
    }
    // removes all files starting with 'prefix' from folder
    public static void removeFolderFilesStartingWith(Path pathFolder, String prefix) {
        File folder = new File(pathFolder.toString());
        if(folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    if(files[i].isFile()) {
                        if(files[i].getName().startsWith(prefix)) {
                            Path path = Paths.get(pathFolder.toString(), files[i].getName());
                            removeFile(path);
                        }
                    }
                }
            }
        }
    }
    // removes all files containing 'substring' from folder
    public static void removeFolderFilesContaining(Path pathFolder, String substring) {
        File folder = new File(pathFolder.toString());
        if(folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    if(files[i].isFile()) {
                        if(files[i].getName().contains(substring)) {
                            Path path = Paths.get(pathFolder.toString(), files[i].getName());
                            removeFile(path);
                        }
                    }
                }
            }
        }
    }
    public static boolean createFilepathFolder(String filepath) {
        File f = new File(filepath);
        String filename = f.getName();
        int idx = filepath.indexOf(filename);
        String folder = filepath.substring(0, idx);
        return createFolderRecursive(folder);
        
    }
    public static boolean createFolderRecursive(String folderPath) {
        boolean result;
        boolean windows = isWindowsOS();
        File f = new File(folderPath);
        if(f.exists() && f.isDirectory())
            result = true;
        else {
            System.out.println("createFolderRecursive: " + folderPath);

            // deal with \\ issue with regex handling in split
            String sep = File.separator;
            if(sep.equals("\\"))
                sep = "\\\\";
            String[]folders = folderPath.split(sep);
            String strFolder = "";
            result = true;
            for(String folder : folders) {
                strFolder += folder + File.separator;
                // deal with the C: part of windows path, unix allows ':' in folder names
                if(!windows || !folder.endsWith(":")) {
                    if(!createFolder(strFolder)) {
                        result = false;
                        break;
                    }
                }
            }
            if(result)
                result = createFolder(folderPath);
        }
        return result;
    }
    public static boolean createFolder(String folderPath) {
        boolean result = false;
        try {
            File f = new File(folderPath);
            if(f.exists() && f.isDirectory())
                result = true;
            else {
                Files.createDirectory(Paths.get(folderPath));
                result = true;
            }
        }
        catch (IOException e) { 
            Tappas.getApp().logInfo("Folder creation exception (" + folderPath + "): " + e.getMessage());
        }
        return result;
    }
    
    //Create DONE fild for check hasAnyData analysis
    public static boolean createDoneFile(String path, String id, Logger logger) {
        boolean result = false;
        Writer writer = null;
        try{
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"));
            writer.write("end");
            result = true;
        } catch (IOException e) {
            logger.logWarning("Unable to save done FDA file '" + path +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    
    // must make sure to update if OS names are changed so this check fails
    public static boolean isWindowsOS() {
        boolean result = false;
        if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
            result = true;
        return result;
    }
    public static boolean isMacOS() {
        boolean result = false;
        if(System.getProperty("os.name").toLowerCase().contains("mac os"))
            result = true;
        return result;
    }
    // Warning: Must use "." for empty tab separated fields
    //          If not, if you do "ABC\t\t\t".split("\t")  - you will end up with one field only ("ABC")
    public static void loadParams(HashMap<String, String> hmParams, String filepath) {
        try {
            hmParams.clear();
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    String[] fields = line.split("\t");
                    if(fields.length >= 2)
                        hmParams.put(fields[0], line.substring(fields[0].length()).trim());
                    else if(fields.length != 1){
                        Tappas.getApp().logInfo("Invalid line, " + line + ", in parameter file '" + filepath + "'.");
                        hmParams.clear();
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load parameter file exception (" + filepath + "): " + e.getMessage());
            hmParams.clear();
        }
    }
    // Note: when saving params, it will create lower level folders if needed
    public static boolean saveParams(HashMap<String, String> hmParams, String filepath) {
        Writer writer = null;
        boolean result = false;
        try {
            createFilepathFolder(filepath);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            for(String key : hmParams.keySet())
                writer.write(key + "\t" + hmParams.get(key) + "\n");
            result = true;
        } catch (IOException e) {
            Tappas.getApp().logInfo("Save parameter file exception(" + filepath + "): " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Code exception closing Writer object while handling exception."); }
        }
        return result;
    }
    
    //Save tblData as txt file to load it in R for ggenerate specials plots
    public static boolean saveDIUResultAnalysis(TableView tblData, String  analysisFolder){
        Writer writer = null;
        boolean result = false;
        try{
            createFilepathFolder(analysisFolder);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFolder), "utf-8"));
            String res = "Gene\ttotalChange\tpodiumChange\tDS\tMeanExpression1\tMeanExpression2\n";
            writer.write(res);
            ObservableList<DIUSelectionResults> items = tblData.getItems();
            for(DIUSelectionResults item : items){
                writer.write(item.getGene()+"\t"+
                            String.valueOf(item.getTotalChange())+"\t"+
                            item.getPodiumChange()+"\t"+
                            item.getDS()+"\t"+
                            item.getCondition("0")+"\t" +
                            item.getCondition("1")+ "\n");
            }
            result = true;
        } catch (IOException e) {
            Tappas.getApp().logInfo("Save matrix file exception(" + analysisFolder + "): " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Code exception closing Writer object while handling exception."); }
        }
        return result;
    }
    
    //Save tblData as txt file to load it in R for ggenerate specials plots
    public static boolean saveFDAIDResultAnalysis(TableView tblData, String  analysisFolder){
        Writer writer = null;
        boolean result = false;
        try{
            createFilepathFolder(analysisFolder);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFolder), "utf-8"));
            String res = "#FeatureID\tDescription\tPValue\tAdjPValue\tVarying\tNotVarying\n";
            writer.write(res);
            ObservableList<DataFDA.DataSelectionResults> items = tblData.getItems();
            for(DataFDA.DataSelectionResults item : items){
                writer.write(item.gene.get()+"\t"+
                            item.geneDescription.get()+"\t"+
                            item.pValue+"\t"+
                            item.adjPValue+"\t"+
                            item.intSources[0]+ "\t" +
                            item.intSources[1]+ "\n");
            }
            result = true;
        } catch (IOException e) {
            Tappas.getApp().logInfo("Save matrix file exception(" + analysisFolder + "): " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Code exception closing Writer object while handling exception."); }
        }
        return result;
    }
    
        public static boolean saveDFIResultAnalysis(TableView tblData, String  analysisFolder){
        Writer writer = null;
        boolean result = false;
        try{
            createFilepathFolder(analysisFolder);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(analysisFolder), "utf-8"));
            String res = "Gene\tFeature\tFeatureID\tDFI\tQValue\tTotalChange\tFavored\tDiffFI\n";
            writer.write(res);
            ObservableList<DFISelectionResults> items = tblData.getItems();
            for(DFISelectionResults item : items){
                writer.write(item.getGene()+"\t"+
                            item.getFeature()+"\t"+
                            item.getFeatureId()+"\t"+
                            item.getDS()+"\t"+
                            item.getQValue()+"\t" +
                            item.getTotalChange()+"\t"+ 
                            item.getFavored()+"\t"+
                            item.getTotalChange()+"\n");
            }
            result = true;
        } catch (IOException e) {
            Tappas.getApp().logInfo("Save matrix file exception(" + analysisFolder + "): " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Code exception closing Writer object while handling exception."); }
        }
        return result;
    }
    
    // will return list of sets
    public static ArrayList<Set> loadSetsFile(String filepath) {
        ArrayList<Set> lst = new ArrayList<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    if(line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length > 2) {
                            ArrayList<String> lstMembers = new ArrayList<>();
                            for(int i = 2; i < fields.length; i++)
                                lstMembers.add(fields[i]);
                            lst.add(new Set(fields[0], fields[1], lstMembers));
                        }
                        else {
                            Tappas.getApp().logInfo("Invalid line, " + line + ", in sets file '" + filepath + "'.");
                            lst.clear();
                            break;
                        }
                    }
                }
                
                // check for unique set names
                HashMap<String, Object> hm = new HashMap<>();
                for(Set set : lst) {
                    if(hm.containsKey(set.name)) {
                        Tappas.getApp().logInfo("Duplicate set names are not allowed: '" + set.name + "'.");
                        lst.clear();
                        break;
                    }
                    else
                        hm.put(set.name, null);
                }
            }
        }
        catch (Exception e) {
            lst.clear();
            Tappas.getApp().logInfo("Load parameter file exception (" + filepath + "): " + e.getMessage());
        }
        return lst;
    }
    public static boolean saveTextToFile(String txt, String filepath) {
        Writer writer = null;
        boolean result = false;
        try {
            createFilepathFolder(filepath);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write(txt);
            result = true;
        } catch (IOException e) {
            Tappas.getApp().logInfo("Save text to file exception(" + filepath + "): " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) {;}
        }
        return result;
    }
    public static HashMap<String, Object> loadListFromFile(String filepath) {
        HashMap<String, Object> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines)
                    hmItems.put(line.trim(), null);
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    
    public static HashMap<String, Object> loadListFromFile(String filepath, boolean header) {
        HashMap<String, Object> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int nline = 0;
                for(String line : lines){
                    if(nline == 0 && header)
                        continue;
                    hmItems.put(line.trim(), null);
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    // WARNING: First field in the list is treated as a key, duplicates will be discarded
    public static HashMap<String, Integer> loadSingleIntTSVListFromFile(String filepath, boolean hasHeaderLine) {
        HashMap<String, Integer> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 1;
                for(String line : lines) {
                    if((!hasHeaderLine || lnum > 1) && line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length == 2)
                            hmItems.put(fields[0].trim(), Integer.parseInt(fields[1].trim()));
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load TSV list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    
    // WARNING: First field in the list is treated as a key, duplicates will be discarded
    public static HashMap<String, String> loadSingleStringTSVListFromFile(String filepath, boolean hasHeaderLine) {
        HashMap<String, String> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 1;
                for(String line : lines) {
                    if(!hasHeaderLine || lnum > 1) {
                        line = line.trim();
                        if(!line.isEmpty() && line.charAt(0) != '#') {
                            String[] fields = line.split("\t");
                            if(fields.length == 2)
                                hmItems.put(fields[0].trim(), fields[1].trim());
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load TSV list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    
    // WARNING: First field in the list is treated as a key, duplicates will be discarded 
    // int select allow select the correct colum of the file 1=3UTR length, 2 = 5UTR length, 3=CDS length, 4=polyASite 
    public static HashMap<String, String> loadGenomicPositions(String filepath, int select, boolean hasHeaderLine) {
        HashMap<String, String> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 1;
                for(String line : lines) {
                    if(!hasHeaderLine || lnum > 1) {
                        line = line.trim();
                        if(!line.isEmpty() && line.charAt(0) != '#') {
                            String[] fields = line.split("\t");
                            if(fields.length == 6) // SeqName, Length3, Length 5, CDS, genomicPAS, TotalLength 
                                hmItems.put(fields[0].trim(), fields[select].trim());
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load GenomicPos list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    // WARNING: First field in the list is treated as a key, duplicates will be discarded
    public static HashMap<String, String[]> loadTSVListFromFile(String filepath) {
        HashMap<String, String[]> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    if(line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length >= 2)
                            hmItems.put(fields[0].trim(), fields);
                    }
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load TSV list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    
    public static HashMap<String, String[]> loadTSVListFromFile(String filepath, boolean hasHeaderLine) {
        HashMap<String, String[]> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int l = 0;
                for(String line : lines) {
                    if(hasHeaderLine && l == 0){
                        l++;
                        continue;
                    }
                    if(line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length >= 2)
                            hmItems.put(fields[0].trim(), fields);
                    }
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load TSV list from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        return hmItems;
    }
    
    public static ArrayList<DataApp.RankedListEntry> loadRankedList(String filepath) {
        ArrayList<DataApp.RankedListEntry> lst = new ArrayList<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    if(line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length >= 2)
                            lst.add(new DataApp.RankedListEntry(fields[0].trim(), Double.parseDouble(fields[1].trim())));
                        else {
                            Tappas.getApp().logWarning("Invalid line, " + line + ", in ranked list file '" + filepath + "'.");
                            lst.clear();
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logWarning("Unable to load ranked list file '" + filepath + "': " + e.getMessage());
            lst.clear();
        }
        return lst;
    }
    public static ArrayList<String> loadSingleItemListFromFile(String filepath, boolean hasHeaderLine) {
        ArrayList<String> lst = new ArrayList<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 1;
                for(String line : lines) {
                    // skip header if requested
                    if(lnum++ == 1 && hasHeaderLine)
                        continue;
                    
                    line = line.trim();
                    if(!line.isEmpty() && line.charAt(0) != '#') {
                        String[] fields = line.split("\t");
                        if(fields.length >= 1)
                            lst.add(fields[0].trim());
                        else {
                            Tappas.getApp().logWarning("Invalid line, " + line + ", in list file '" + filepath + "'.");
                            lst.clear();
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logWarning("Unable to load list file '" + filepath + "': " + e.getMessage());
            lst.clear();
        }
        return lst;
    }
    // load text file contents - should only use for small files
    public static String loadTextFromFile(String filepath) {
        String content = "";
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines)
                    content += line + "\n";
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load text from file exception (" + filepath + "): " + e.getMessage());
        }
        return content;
    }
    public static List<String> loadTextLinesFromFile(String filepath) {
        List<String> lines = new ArrayList<>();
        try {
            if(Files.exists(Paths.get(filepath)))
                lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load text from file exception (" + filepath + "): " + e.getMessage());
        }
        return lines;
    }
    
    enum ValueFileType { TSV, CSV, ENTRY, UNKNOWN }
    public static String loadValuesFromFile(String filepath) {
        String values = "";
        HashMap<String, Object> hmItems = new HashMap<>();
        try {
            if(Files.exists(Paths.get(filepath))) {
                ValueFileType vft = ValueFileType.UNKNOWN;
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 1;
                for(String line : lines) {
                    if(!line.isEmpty() && !line.substring(0, 1).equals("#")) {
                        if(vft.equals(ValueFileType.UNKNOWN)) {
                            String fields[] = line.split("\t");
                            if(fields.length > 1) {
                                vft = ValueFileType.TSV;
                                hmItems.put(fields[0], null);
                            }
                            else {
                                fields = line.split(",");
                                if(fields.length > 1) {
                                    vft = ValueFileType.CSV;
                                    hmItems.put(fields[0], null);
                                }
                                else {
                                    // check for a reasonable value length
                                    // only used for gene, transcripts, proteins, etc.
                                    if(line.length() <= 50) {
                                        vft = ValueFileType.ENTRY;
                                        hmItems.put(line, null);
                                    }
                                }
                            }
                            
                            // check if we were able to find a likely file type
                            if(vft.equals(ValueFileType.UNKNOWN))
                                break;
                        }
                        else {
                            String fields[];
                            switch(vft) {
                                case TSV:
                                    fields = line.split("\t");
                                    hmItems.put(fields[0], null);
                                    break;
                                case CSV:
                                    fields = line.split("\t");
                                    hmItems.put(fields[0], null);
                                    break;
                                default:
                                    hmItems.put(line, null);
                                    break;
                            }
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            Tappas.getApp().logInfo("Load values from file exception (" + filepath + "): " + e.getMessage());
            hmItems.clear();
        }
        
        // check if got any values
        if(!hmItems.isEmpty()) {
            for(String value : hmItems.keySet())
                values += (values.isEmpty()? "" : ";") + value;
        }
        return values;
    }
    public static String getIdHashCode(String idString) {
        return Integer.toString(idString.hashCode()).replace("-", "0");
    }
    
    //
    //  Copy methods - Source: http://javaonlineguide.net/2018/03/copy-text-and-image-into-clipboard-using-javafx-example.html
    //  Author: http://javaonlineguide.net/author/vkjegan
    //
    
    public static void copyToClipboardText(String s) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
 
		content.putString(s);
		clipboard.setContent(content);
    }

    public static void copyToClipboardImage(Label lbl) {
            WritableImage snapshot = lbl.snapshot(new SnapshotParameters(), null);
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(snapshot);
            clipboard.setContent(content);
    }
    
    public static void copyToClipboardImage(Canvas lbl) {
            WritableImage snapshot = lbl.snapshot(new SnapshotParameters(), null);
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(snapshot);
            clipboard.setContent(content);
    }
    
    public static void copyToClipboardImage(Chart lbl) {
            WritableImage snapshot = lbl.snapshot(new SnapshotParameters(), null);
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(snapshot);
            clipboard.setContent(content);
    }
    
    public static void copyToClipboardImage(AnchorPane lbl) {
            WritableImage snapshot = lbl.snapshot(new SnapshotParameters(), null);
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(snapshot);
            clipboard.setContent(content);
    }
    
    public static void copyToClipboardImage(ImageView lbl) {
            WritableImage snapshot = lbl.snapshot(new SnapshotParameters(), null);
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(snapshot);
            clipboard.setContent(content);
    }

    public static void copyToClipboardImageFromFile(String path) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putImage(Utils.getImage(path));
            clipboard.setContent(content);
    }

    public static Image getImage(String path) {
            InputStream is = Utils.class.getResourceAsStream(path);
            return new Image(is);
    }


    public static ImageView setIcon(String path) {
            InputStream is = Utils.class.getResourceAsStream(path);
            ImageView iv = new ImageView(new Image(is));

            iv.setFitWidth(100);
            iv.setFitHeight(100);
            return iv;
    }
    
    //
    // Data Classes
    //
    static public class Set {
        String name;
        String description;
        ArrayList<String> lstMembers;
        public Set(String name, String description, ArrayList<String> lstMembers) {
            this.name = name;
            this.description = description;
            this.lstMembers = lstMembers;
        }
    }
}
