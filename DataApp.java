/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class DataApp extends AppObject {
    // reference URL where annotation files are downloaded from - MUST end with "/"
    public static final String refDataUrl = "http://app.tappas.org/resources/downloads/annotations/";
    public static final String refDownloadsUrl = "http://app.tappas.org/resources/downloads/tappAS/";
    public static enum ExperimentType { Two_Group_Comparison, Time_Course_Single, Time_Course_Multiple }
    public static final List<EnumData> lstExperiments = Arrays.asList(
        new EnumData(ExperimentType.Two_Group_Comparison.name(), "Two-Group Comparison"),
        new EnumData(ExperimentType.Time_Course_Single.name(), "Single Series Time-Course"),
        new EnumData(ExperimentType.Time_Course_Multiple.name(), "Multiple Series Time-Course")
    );
    // Note: Species and RefType are used as folder name
    public static enum Species { Homo_sapiens, Mus_musculus, Arabidopsis_thaliana, Drosophila, Zea_mays, Other }
    public static enum RefType { Ensembl, RefSeq, Demo }
    /*
    public static final List<RefFilesData> lstHomoSapiens = Arrays.asList(
        new RefFilesData("annotation_file_Ensembl_Homo_sapiens_v1.0.tar.gz", RefType.Ensembl.name(), "1.0", "Ensembl 86 reference"),
        new RefFilesData("annotation_file_RefSeq_Homo_sapiens_v1.0.tar.gz", RefType.RefSeq.name(), "1.0", "RefSeq - NCBI Reference Sequence Database")
    );
    public static final List<RefFilesData> lstMusMusculus = Arrays.asList(
        new RefFilesData("annotation_file_Ensembl_Mus_musculus_v1.0.tar.gz", RefType.Ensembl.name(), "1.0", "Ensembl 86 reference"),
        new RefFilesData("annotation_file_RefSeq_Mus_musculus_v1.0.tar.gz",  RefType.RefSeq.name(), "1.0", "RefSeq - NCBI Reference Sequence Database"),
        new RefFilesData("annotation_file_Demo_Mus_musculus_v1.0.tar.gz", RefType.Demo.name(), "1.0", "Application Demo - includes design and expression matrix file")
    );
    public static final List<RefFilesData> lstArabidopsisThaliana = Arrays.asList(
        new RefFilesData("annotation_file_Ensembl_Arabidopsis_thaliana_v1.0.tar.gz", RefType.Ensembl.name(), "1.0", "Ensembl reference")
    );
    public static final List<RefFilesData> lstZeaMays = Arrays.asList(
        new RefFilesData("annotation_file_Ensembl_Zea_mays_v1.0.tar.gz", RefType.Ensembl.name(), "1.0", "Ensembl reference")
    );
    public static final List<SpeciesData> lstSpecies = Arrays.asList(
        new SpeciesData(Species.Homo_sapiens.name(), "Homo sapiens", lstHomoSapiens),
        new SpeciesData(Species.Mus_musculus.name(), "Mus musculus", lstMusMusculus),
        new SpeciesData(Species.Arabidopsis_thaliana.name(), "Arabidopsis thaliana", lstArabidopsisThaliana),
        new SpeciesData(Species.Drosophila.name(), "Drosophila", null),
        new SpeciesData(Species.Zea_mays.name(), "Zea mays", lstZeaMays),
        new SpeciesData(Species.Other.name(), "Other", null)
    );*/
    public enum DataType { TRANS, PROTEIN, GENE };
    
    public enum FeatureType { Transcripts, Proteins, Genomic };
    public enum FeatureTypeDiversity { Transcripts, Proteins};
    
    //public String[] FeatureStructural = new String[]{"polyA_Site", "CDS", "5UTR_Length", "3UTR_Length"};
    //public String[] FeatureTranscript = new String[]{"uORF", "RNA_binding", "repeat", "PAS", "NMD", "miRNA", "5UTRmotif", "3UTRmotif"};
    //public String[] FeatureProtein = new String[]{"TRANSMEM", "SIGNAL", "PTM", "MOTIF", "INTRAMEM", "DOMAIN", "clan", "DISORDER", "COMPBIAS", "COILED", "BINDING", "ACT_SITE", "C", "F", "P", "FunctionalImpact", "PATHWAY"}; 
    
    // application workspace level folders
    private static final String appFolderName = "tappasWorkspace";
    private static final String FOLDER_APP = "App";
    private static final String SUBFOLDER_WEB = "web";
    private static final String SUBFOLDER_WEB_CSS = Paths.get(SUBFOLDER_WEB, "css").toString();
    private static final String SUBFOLDER_WEB_IMAGES = Paths.get(SUBFOLDER_WEB, "images").toString();
    private static final String SUBFOLDER_WEB_SCRIPTS = Paths.get(SUBFOLDER_WEB, "scripts").toString();
    private static final String FOLDER_PROJECTS = "Projects";
    private static final String FOLDER_REFS = "References";

    // global names and file extensions
    private static String rscriptPath = "Rscript";
    public static final String APPLOG_NAME = "tappas.log";
    public static final String APP_FOLDER_EXT = ".tappas";
    public static final String JPG_EXT = ".jpg";
    public static final String LOG_NAME = "log.txt";
    public static final String LOG_PREFIX = "log_";
    public static final String LOG_PREFIXID = "log.";
    public static final String LOG_EXT = ".txt";

    // databases
    public static final String  ANNOTATION_DB = "annotations.db";
    public static final String  PROJECT_DB = "project.db";

    // User annotation reference files folder
    public static final String FOLDER_USER_REFS_PREFIX = "UserDefined.";
    public static String getUserRefsNamePrefix() { return FOLDER_USER_REFS_PREFIX; }
    public static String getUserRefsNameExt() { return APP_FOLDER_EXT; }

    // annotation reference files
    public static final String PRM_USER_REFS = "reference.prm";
    
    // project parameter and state files
    public static final String PRM_PROJECT = "project.prm";
    public static final String PRM_PROJECT_DATA = "project_data.prm";
    public static final String PRM_EXT = ".prm";
    public static final String PRM_FDA = "fda.";//fda.prm";
    public static final String PRM_FDA_NAME = "fda.";
    public static final String PRM_DFI_NAME = "dfi.";
    public static final String PRM_DFI = "dfi.prm";
    public static final String PRM_DPA = "dpa.prm";
    public static final String PRM_UTRL = "utrl.prm";
    public static final String PRM_GSEA_NAME = "gsea.";
    public static final String PRM_GSEACLUSTER_NAME = "gsea_cluster.";
    public static final String PRM_FEA_NAME = "fea.";
    public static final String PRM_FEACLUSTER_NAME = "fea_cluster.";
    
    // application file names
    public static final String APP_ANNOTATION_DEF = "annotations.def";
    public static final String APP_GOTERMS = "go.obo";
    public static final String APP_GOTERMSANCES = "goAncestors.obo";
    public static final String APP_RSCRIPT_PATH = "rscript_path.txt";
    public static final String APP_MM_CHROMOBANDS = "chromo_bands_Mus_musculus.tsv";
    public static final String APP_HS_CHROMOBANDS = "chromo_bands_Homo_sapiens.tsv";
    public static final String APP_MM_NCBI_GENES = "mm_ncbi_genes.tsv";
    public static final String APP_HS_NCBI_GENES = "hs_ncbi_genes.tsv";
    public static final String REFDOWNLOAD_OK = "ref_download.ok";
    public static final String ACTUAL_VERSION = tappas.Tappas.APP_MAJOR_VER + "_" + tappas.Tappas.APP_MINOR_VER + "_" + tappas.Tappas.APP_STRREV + ".version";
    
    // prefix/suffix/extensions
    public static final String DONE_NAME = "done_";
    public static final String CLUSTER_NAME = "cluster_";
    public static final String CLUSTER_NAMEID = "cluster.";
    public static final String VENNDIAG_NAME = "venn_diag_";
    public static final String TEXT_EXT = ".txt";
    public static final String TSV_EXT = ".tsv";
    public static final String PNG_EXT = ".png";
    public static final String FEATURES_NAME = "features";
    public static final String RESULTS_NAME = "result_";
    public static final String RESULTS_DFI_NAME = "dfi_result";
    public static final String RESULTSCLUSTER_NAME = "result_cluster_";
    public static final String RESULTS_EXT = ".tsv";
    public static final String DBG_EXT = ".dbg";
    public static final String VERSION_EXT = ".version";

    // application folder functions
    public String getAppDataFolder() { return appFolder; }
    public String getAppDataBaseFolder() { return appBaseFolder; }
    public String getAppRunFolder() { return appRunPath; }
    public String getAppProjectsFolder() { return appProjectsBaseFolder; }   
    public String getProjectFolder(String id) { return(Paths.get(app.data.getAppProjectsFolder(), DataProject.getProjectFolderFilename(id)).toString()); }
    public String getAppAnnotationDefFilepath() { return(Paths.get(getAppDataFolder(), APP_ANNOTATION_DEF).toString()); }
    public String getAppGOTermsFilepath() { return Paths.get(getAppDataFolder(), APP_GOTERMS).toString(); }
    public String getAppGOTermsAncesFilepath() {return Paths.get(getAppDataFolder(), APP_GOTERMSANCES).toString(); }
    public String getAppActualVersionFilepath() {return Paths.get(getAppDataFolder(), ACTUAL_VERSION).toString(); }
    
    // Rscript file paths
    private String getRScriptOverwriteFilepath() { return Paths.get(getAppDataFolder(), APP_RSCRIPT_PATH).toString(); }
    public String getRscriptFilepath() { return rscriptPath; }

    // Resource files - this table drives the loading of resource files to the installed FOLDER_APP folder
    // WARNING: update version if any resource file changes - otherwise resource files will not be updated
    //          if you add a new file, it will automatically copy it (always copies if not found)
    // Must create new file in resource folder and update version number here. Remove old files in resource folder
    private static final String RES_VER_FILE = Tappas.RES_VER_FILE;
    private final List<App.AppResource> lstResources = Arrays.asList(
        // The definitions in APP_ANNOTATION_DEF can never change or it will break reading existing annotation files
        // see notes in AnnotationDefs file
        new App.AppResource(APP_ANNOTATION_DEF, APP_ANNOTATION_DEF),
        new App.AppResource("d3.min.js", "d3.min.js"),
        new App.AppResource("dagre-d3.min.js", "dagre-d3.min.js"),
        new App.AppResource(APP_GOTERMS, APP_GOTERMS),
        new App.AppResource(APP_MM_CHROMOBANDS, APP_MM_CHROMOBANDS),
        new App.AppResource(APP_HS_CHROMOBANDS, APP_HS_CHROMOBANDS),
        new App.AppResource(APP_MM_NCBI_GENES, APP_MM_NCBI_GENES),
        new App.AppResource(APP_HS_NCBI_GENES, APP_HS_NCBI_GENES),
        // always place RES_VER_FILE at the end to make sure it all completed properly before copying
        new App.AppResource(RES_VER_FILE, RES_VER_FILE)
    );
    public static final String RESULTS_GENE_TRANS = "result_gene_trans.tsv";
    public static final String RESULTS_GENE_PROT = "result_gene_prot.tsv";

    // class data
    private final String appRunPath = System.getProperty("user.dir");
    private String appBaseFolder;
    private String appProjectsBaseFolder;
    private String appFolder = "";
    private final HashMap<String, String> hmNCBI_MM = new HashMap<>();
    private final HashMap<String, String> hmNCBI_HS = new HashMap<>();
    
    public DataApp(App app) {
        super(app);
    }
    public void loadRscriptPath() {
        try {
            if(Files.exists(Paths.get(getRScriptOverwriteFilepath()))) {
                List<String> lines = Files.readAllLines(Paths.get(getRScriptOverwriteFilepath()), StandardCharsets.UTF_8);
                if(lines != null && lines.size() == 1) {
                    // get full path from user defined entry
                    // deal with windows and spaces in name, e.g. "Program Files"
                    rscriptPath = lines.get(0).trim();
                    if(Utils.isWindowsOS() && rscriptPath.contains(" "))
                        rscriptPath = "\"" + rscriptPath + "\"";
                    logger.logInfo("Setting Rscript full file path to: " + rscriptPath);
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load Rscript path from file '" + getRScriptOverwriteFilepath() + "': " + e.getMessage());
        }
    }
    public void initialize() {
    }
    
    public Integer getFeatureTypeLength() {
        return FeatureType.values().length;
    }

    public String getFeatureType(Integer ind) {
        int cont = 0;
        for(FeatureType aux : FeatureType.values()){
            if(cont == ind)
                return aux.toString();
            else
                cont++;
        }
        return "";
    }

    public Integer getFeatureTypeDiversityLength() {
        return FeatureTypeDiversity.values().length;
    }

    public String getFeatureTypeDiversity(Integer ind) {
        int cont = 0;
        for(FeatureTypeDiversity aux : FeatureTypeDiversity.values()){
            if(cont == ind)
                return aux.toString();
            else
                cont++;
        }
        return "";
    }
    
    public String getDataTypeSingular(String dataType) {
        String dt = "";
        if(dataType.equals(DataType.TRANS.name()))
            dt = "transcript";
        else if(dataType.equals(DataType.PROTEIN.name()))
            dt = "protein";
        else if(dataType.equals(DataType.GENE.name()))
            dt = "gene";
        return dt;
    }
    
    public String getDataTypePlural(String dataType) {
        String dt = "";
        if(dataType.equals(DataType.TRANS.name()))
            dt = "transcripts";
        else if(dataType.equals(DataType.PROTEIN.name()))
            dt = "proteins";
        else if(dataType.equals(DataType.GENE.name()))
            dt = "genes";
        return dt;
    }

    //
    // User interactive function will open dialogs if needed
    //
    
    // get required application folder paths
    public boolean getFolderPaths(Stage stage) {
        boolean result;
        boolean extract = false;
        UserPrefs usrprefs = app.userPrefs;
        if(usrprefs.getAppBaseFolder().isEmpty()) {
            extract = true;
            if(usrprefs.canWrite()) {
                result = getAppFolderPath(stage, usrprefs, false);
                if(result) {
                    result = checkForNoAppBaseFolder(appBaseFolder);
                    if(result)
                        result = createAppFoldersUI(appBaseFolder);
                }
            }
            else {
                app.ctls.alertError("tappAS - Java Peferences Data Storage Not Supported", "Unable to save application information.\n");
                logger.logInfo("ERROR: Java Peferences Data Storage Not Supported");
                result = false;
            }
        }
        else {
            File f = null;
            appBaseFolder = app.userPrefs.getAppBaseFolder();
            if(appBaseFolder == null || appBaseFolder.isEmpty())
                logger.logInfo("WARNING: " + ((appBaseFolder == null)? "Null base folder" : " Empty base folder") + " in user preferences");
            else
                f = new File(appBaseFolder);
            
            if(f != null && f.exists() && f.isDirectory())
                result = true;
            else {
                // missing folder - previously set in usrprefs
                result = getAppFolderPath(stage, usrprefs, true);
                if(result) {
                    // check if user pointed to another location that does have the folder
                    result = checkForNoAppBaseFolder(appBaseFolder);
                    if(result) {
                        // if it is an existing folder then set to extract all files just in case
                        if(Files.exists(Paths.get(appBaseFolder)))
                            extract = true;
                        result = createAppFoldersUI(appBaseFolder);
                    }
                }
            }
        }  
        if(result) {
            // setup remaning paths
            appProjectsBaseFolder = Paths.get(appBaseFolder, FOLDER_PROJECTS).toString();
            appFolder = Paths.get(appBaseFolder, FOLDER_APP).toString();

            // if not forcing extract, check to see if we need to due to updated resource files
            if(!extract) {
                if(!Files.exists(Paths.get(appFolder, RES_VER_FILE))) {
                    extract = true;
                    System.out.println("Updated resource files - force extraction of all files");
                }
            }
            // we need to make sure all application files have been extracted
            // possible concern that it might take too long
            // not sure that it's worth the complications of moving to background thread
            if(!extractApplicationFiles(extract))
                result = false;
            else if(!extractApplicationWebFiles(extract))
                result = false;
        }
        return result;
    }
    private boolean getAppFolderPath(Stage stage, UserPrefs usrprefs, boolean missing) {
        boolean result = false;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if(missing)
            alert.setTitle("tappAS - Application Data Folder Not Found");
        else
            alert.setTitle("tappAS - Create Application Data Folder");
        alert.setHeaderText(null);
        ButtonType btnChange = new ButtonType("Change");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnOK, btnChange, btnCancel);
        if(missing)
            alert.setContentText("The application data folder\n'" + appFolderName + "' was not found.\nClick OK to create it, or\nClick Change to set new location.\n\n");
        else
            alert.setContentText("The application will create a '" + appFolderName + "'\ndata folder in your home directory.\nClick OK to proceeed or Change to set location.\n\n");
        Optional<ButtonType> reply = alert.showAndWait();
        if(reply.get() == btnOK) {
            appBaseFolder = Paths.get(System.getProperty("user.home"), appFolderName).toString();
            usrprefs.setAppBaseFolder(appBaseFolder);
            result = true;
        }
        else if(reply.get() == btnChange) {
            // ask user for location where to create TAPPAS folder
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("tappAS - Select location to add '" + appFolderName + "' application data folder");
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = dirChooser.showDialog(stage);
            // make sure it's not something like "/" or "c:\"
            if (selectedDirectory != null && selectedDirectory.getAbsolutePath().length() > 4) {
                appBaseFolder = Paths.get(selectedDirectory.getAbsolutePath(), appFolderName).toString();
                usrprefs.setAppBaseFolder(appBaseFolder);
                result = true;
            }
        }
        return result;
    }
    private boolean createAppFoldersUI(String basepath) {
        boolean result = true;
        if(!createAppFolders(basepath)) {
            app.ctls.alertError("tappAS - Application Data Folder Creation Error", "Unable to create application data folder in specified location.\n");
            logger.logInfo("ERROR: Java Peferences Data Storage Not Supported");
            result = false;
        }
        return result;
    }
    public static boolean createAppFolders(String basePath) {
        boolean result = false;
        if(Utils.createFolder(basePath) && Utils.createFolder(Paths.get(basePath, FOLDER_APP).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_APP, SUBFOLDER_WEB).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_APP, SUBFOLDER_WEB_CSS).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_APP, SUBFOLDER_WEB_IMAGES).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_APP, SUBFOLDER_WEB_SCRIPTS).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_PROJECTS).toString()) && 
                Utils.createFolder(Paths.get(basePath, FOLDER_REFS).toString()))
            result = true;
        return result;
    }   
    private boolean checkForNoAppBaseFolder(String basepath) {
        boolean result = true;
        if(Files.exists(Paths.get(basepath))) {
            String msg = "You have an existing '" + appFolderName + "' folder in this location.\nAll existing project data will be kept if you click OK.\nYou may click Cancel and manually delete or\nrename the folder.\n\nDo you want to Proceeed?\n\n";
            result = app.ctls.alertConfirmation("tappAS - Create Application Data Folder", msg, ButtonType.CANCEL);
        }
        return result;
    }
    private boolean extractApplicationFiles(boolean forced) {
        boolean result = false;
        
        // always make sure folders exist in case user deletes them
        if(createAppFolders(appBaseFolder)) {
            result = true;
            for(App.AppResource res : lstResources) {
                if(forced || !Files.exists(Paths.get(appFolder, res.fileName))) {
                    System.out.println("Extracting resource " + res.resName + " to file.");
                    if(Files.exists(Paths.get(appFolder, res.fileName))) {
                        File file = new File(Paths.get(appFolder, res.fileName).toString());
                        file.setWritable(true);
                    }
                    result = writeFileContentFromResource("/tappas/resources/" + res.resName, Paths.get(appFolder, res.fileName).toString());
                    if(!result) {
                        app.ctls.alertError("tappAS - Application Resource Files Extraction Error", "Unable to extract application resource to files.");
                        logger.logInfo("ERROR: Application Resource Files Extraction Error");
                        break;
                    }
                }
            }
        }
        return result;
    }
    private boolean extractApplicationWebFiles(boolean forced) {
        boolean result = false;
        String webFiles = getFileContentFromResource("/tappas/resources/web/webFiles.txt");
        if(!webFiles.isEmpty()) {
            result = true;

            // unix lines only
            String[] lines = webFiles.split("\\n");
            int lnum = 1;
            for(String line : lines) {
                if(!line.trim().isEmpty() && line.charAt(0) != '#') {
                    Path fpath = Paths.get(appFolder, SUBFOLDER_WEB, line);
                    if(forced || !Files.exists(fpath)) {
                        System.out.println("Extracting web resource " + line + " to file.");
                        if(Files.exists(fpath)) {
                            File file = new File(fpath.toString());
                            file.setWritable(true);
                        }
                        result = writeFileContentFromResource("/tappas/resources/web/" + line, fpath.toString());
                        if(!result) {
                            app.ctls.alertError("tappAS - Application Resource Web Files Extraction Error", "Unable to extract application web resource to files.");
                            logger.logError("ERROR: Application Resource Web Files Extraction Error");
                            app.userPrefs.setWebFilesUpdate("FAILED");
                            break;
                        }
                    }
                }
                else {
                    if(lnum == 1 && !line.trim().isEmpty() && line.charAt(0) == '#') {
                        // check if webFiles updated, set forced flag if so
                        if(!app.userPrefs.getWebFilesUpdate().equals(line)) {
                            logger.logInfo("WebFiles update set to " + line);
                            app.userPrefs.setWebFilesUpdate(line);
                            forced = true;
                        }
                    }
                }
                lnum++;
            }
        }
        return result;
    }
    
    //
    // General support functions
    //
    
    // WARNING: this is a recursive function - will delete all sub folders
    //          added check to make sure it is in the TAPPAS projects folder area
    public boolean removeFolder(Path pathFolder) {
        // better safe than sorry - make sure we are in the TAPPAS projects folder area
        if(!getAppProjectsFolder().isEmpty() && pathFolder.toString().contains(getAppProjectsFolder())) {
            File folder = new File(pathFolder.toString());
            if(!folder.exists())
                return true;
            if(!folder.isDirectory())
                return false;

            // remove all files in the folder first
            boolean result = true;
            Utils.removeAllFolderFiles(pathFolder, true);

            // now remove all the sub folders if any
            String[] subFolders = folder.list();
            for(String subFolder : subFolders) {
                if(!removeFolder(Paths.get(pathFolder.toString(), subFolder))) {
                    result = false;
                    break;
                }
            }
            if(result)
                result = folder.delete();
            return result;
        }
        else
            return false;
    }

    //
    // Reference files
    //
    
    //Create the last version of tappAS the user had to check if re-create goAncestors.obo file
    //true for the same version and false if the usar run an older version of tappAS
    public boolean checkAppLastVersion(){
        boolean res = false;
        //look for all the files with sufix .version
        File appFolder = new File(Paths.get(getAppDataFolder()).toString());
        FilenameFilter filter = (File dir, String name) -> {
            return (name.endsWith(DataApp.VERSION_EXT));
        };        
        File[] files = appFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                String[] fields = name.split("\\.");
                fields = fields[0].split("_");
                if(fields[0].equals(Integer.toString(Tappas.APP_MAJOR_VER))){
                    if(fields[1].equals(Integer.toString(Tappas.APP_MINOR_VER))){
                        if(fields[1].equals(Integer.toString(Tappas.APP_STRREV))){
                            System.out.println("The last version run in this computer was: " + Integer.toString(Tappas.APP_MAJOR_VER) + "." + Integer.toString(Tappas.APP_MINOR_VER) + "." + Integer.toString(Tappas.APP_STRREV));
                            res = true;
                            return res;
                        }
                    }
                }
            }
        }
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                Utils.removeFile(Paths.get(getAppDataFolder(), name));
            }
        }
        
        //create the new version file
        String filepath = getAppActualVersionFilepath();

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, true), "utf-8"));
            writer.close();
            System.out.println("Finish");
        }catch (Exception e) {
            app.logError("Unable to create version file ':" + e.getMessage());
        } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        
        return res;
    }
    
// TODO: the original GFF3 annotation filepath is being passed and it is using that to search in all the
// user reference parameter files for a match - this should not be done for the original project that
// resulted in the creation of the reference and should only be done at the creation of a new project
// the actual path to the database should then be stored and used from there on w/o any search!!!
// FINALIZE THIS! may not really be an issue
    public String getAnnotationFeaturesFolder(DlgInputData.Params params) {
        String folder = "";
        // check if using application annotation file - make sure parameters have been set
        // it is possible to get here before project is actuallty created so values will not have been set
        if(params.useAppRef) {
            if(params.refType != null && !params.refRelease.isEmpty())
                folder = getAppReferenceFileFolder(params.genus, params.species, params.refType.name(), params.refRelease);
        }
        else {
            if(!params.afFilepath.isEmpty())
                folder = getUserReferenceFileFolder(params.afFilepath);
        }
        return folder;
    }
    
    public String getAnnotationFeaturesFolderWithoutSize(DlgInputData.Params params) {
        String folder = "";
        // check if using application annotation file - make sure parameters have been set
        // it is possible to get here before project is actuallty created so values will not have been set
        if(params.useAppRef) {
            if(params.refType != null && !params.refRelease.isEmpty())
                folder = getAppReferenceFileFolder(params.genus, params.species, params.refType.name(), params.refRelease);
        }
        else {
            if(!params.afFilepath.isEmpty())
                folder = getUserReferenceFileFolderWithoutSize(params.afFilepath);
        }
        return folder;
    }
    
    // Note: this is called from a background thread in DlgInputDataLoad
    public boolean processAnnotationFeatures(Project project, HashMap<String, String> hmResults, double start, double featuresPart, ProgressBar pbProgress) {
        boolean result = false;
        boolean useref = Boolean.valueOf(hmResults.get(DlgInputData.Params.USEAPPREF_PARAM));
        if(useref) {
            // check if this is an application reference file - must have been downloaded already, otherwise fail
            if(hasAppReferenceFiles(hmResults.get(DlgInputData.Params.GENUS_PARAM), hmResults.get(DlgInputData.Params.SPECIES_PARAM), hmResults.get(DlgInputData.Params.REFTYPE_PARAM), hmResults.get(DlgInputData.Params.REFREL_PARAM))) {
                // check if we already have a DB file created
                String folder = getAppReferenceFileFolder(hmResults.get(DlgInputData.Params.GENUS_PARAM), hmResults.get(DlgInputData.Params.SPECIES_PARAM), hmResults.get(DlgInputData.Params.REFTYPE_PARAM), hmResults.get(DlgInputData.Params.REFREL_PARAM));
                Path dbPath = Paths.get(folder, ANNOTATION_DB);
                if(Files.exists(dbPath)) {
                    result = true;
                    updateProgress(pbProgress, start + featuresPart);
                }
                else {
                    String filepath = Paths.get(folder, DataProject.ANNOTATION_FILE).toString();
                    DbAnnotation db = new DbAnnotation(project);
                    db.initialize();
                    if(db.createDB(folder, dbPath.toString(), filepath, start, featuresPart, pbProgress)) {
                        result = true;
                        updateProgress(pbProgress, start + featuresPart);
                    }
                    db.close();
                }
            }
        }
        else {
            // check if we have already processed successfully this annotation file from before
            String path = getUserReferenceFileFolder(hmResults.get(DlgInputData.Params.AFILE_PARAM));
            if(!path.isEmpty()) {
                result = true;
                updateProgress(pbProgress, start + featuresPart);
            }
            else {
                // copy file to workspace area, assume copying 5% of total for progress report
                path = assignUserReferenceFileFolder(hmResults.get(DlgInputData.Params.AFILE_PARAM));
                Path filepath = Paths.get(path, DataProject.ANNOTATION_FILE);
                if(Utils.createFilepathFolder(filepath.toString())) {
                    Utils.copyFile(Paths.get(hmResults.get(DlgInputData.Params.AFILE_PARAM)), filepath);
                    updateProgress(pbProgress, start + 0.05 * featuresPart);

                    // create annotation database
                    DbAnnotation db = new DbAnnotation(project);
                    db.initialize();
                    Path dbPath = Paths.get(path, ANNOTATION_DB);
                    //starting to create project.db
                    if(db.createDB(path, dbPath.toString(), filepath.toString(), (start + 0.05 * featuresPart), (0.95 * featuresPart), pbProgress)) {
                        result = true;
                        updateProgress(pbProgress, start + featuresPart);
                    }
                    db.close();

                    if(result) {
                        // create parameter file and reference ok file
                        HashMap<String, String> hmParams = createUserRefsParams(hmResults.get(DlgInputData.Params.AFILE_PARAM), filepath.toString());
                        if(saveUserRefsParams(hmParams, path))
                            result = createReferencesOKFile(path);
                    }
                }
            }
        }
        return result;
    }
    private void updateProgress(ProgressBar pbProgress, double value) {
        Platform.runLater(() -> {
            pbProgress.setProgress(value);
        });
    }
    // Note: If a reference file is updated, it changes all the derived files, e.g. index, categories, etc.
    //       Each time we update the file, a new folder will be created so the old projects can still work as is
    public String getAppReferenceFileUrl(String name) {
        return refDataUrl + name;
    }
    public String getAppReferenceFileBaseFolder() {
        return Paths.get(getAppDataBaseFolder(), FOLDER_REFS).toString();
    }
    public String getAppReferenceFileFolder(String genus, String species, String refType, String version) {
        return Paths.get(getAppReferenceFileBaseFolder(), genus + "_" + species, refType, version).toString();
    }
    public boolean hasAppReferenceFiles(String genus, String species, String refType, String version) {
        boolean result = Files.exists(Paths.get(getAppReferenceFileBaseFolder(), genus + "_" + species, refType, version, REFDOWNLOAD_OK));
        return result;
    }
    public String getDemoExpMatrixFilepath(String genus, String species, String refType, String version) {
        String filepath = "";
        if(refType.equals(RefType.Demo.name()))
            filepath = Paths.get(getAppReferenceFileBaseFolder(), genus + "_" + species, refType, version, DataProject.EXP_MATRIX).toString();
        return filepath;
    }
    public String getDemoDesignFilepath(String genus, String species, String refType, String version) {
        String filepath = "";
        if(refType.equals(RefType.Demo.name()))
            filepath = Paths.get(getAppReferenceFileBaseFolder(), genus + "_" + species, refType, version, DataProject.EXP_DESIGN).toString();
        return filepath;
    }
    static String SRCPATH_PARAM = "srcPath";
    static String SIZE_PARAM = "size";
    static String DATETIME_PARAM = "datetime";
    static String REFPATH_PARAM = "refPath";
    public String getUserReferenceFileFolder(String filepath) {
        String result = "";
        File file = new File(filepath);
        String name = filepath;
        String size = String.format("%d", file.length());
        String datetime = String.format("%d", file.lastModified());
        ArrayList<HashMap<String, String>> lst = getUserReferenceFilesList();
        for(HashMap<String, String> hm : lst) {
            if(hm.containsKey(SRCPATH_PARAM) && hm.containsKey(SIZE_PARAM) && hm.containsKey(DATETIME_PARAM)) {
                if(name.equals(hm.get(SRCPATH_PARAM)) && size.equals(hm.get(SIZE_PARAM)) && datetime.equals(hm.get(DATETIME_PARAM))) {
                    // add content comparison later or assume match
                    result = hm.get(REFPATH_PARAM);
                    break;
                }
            }
        }
        return result;
    }
    
    public String getUserReferenceFileFolderWithoutSize(String filepath) {
        String result = "";
        File file = new File(filepath);
        String name = filepath;
        ArrayList<HashMap<String, String>> lst = getUserReferenceFilesList();
        for(HashMap<String, String> hm : lst) {
            if(hm.containsKey(SRCPATH_PARAM) && hm.containsKey(SIZE_PARAM) && hm.containsKey(DATETIME_PARAM)) {
                if(name.equals(hm.get(SRCPATH_PARAM))) {
                    // add content comparison later or assume match
                    result = hm.get(REFPATH_PARAM);
                    break;
                }
            }
        }
        return result;
    }
    
    // Only call if no existing folder was found - this function does not check
    public String assignUserReferenceFileFolder(String filepath) {
        String id = Utils.getIdHashCode(filepath);
        int idx = 0;
        Path path = Paths.get(getAppDataBaseFolder(), FOLDER_REFS, getUserRefsNamePrefix() + id + getUserRefsNameExt());
        while (Files.exists(path))
            path = Paths.get(getAppDataBaseFolder(), FOLDER_REFS, getUserRefsNamePrefix() + id + "_" + (++idx) + getUserRefsNameExt());
        return path.toString();
    }
    public boolean createReferencesOKFile(String path) {
        boolean result = false;
        String filepath = Paths.get(path, REFDOWNLOAD_OK).toString();
        Writer writer = null;
        try {
            Utils.createFilepathFolder(path);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write("Files downloaded and extracted OK\n");
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to create reference file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    // need todefine a prm (file path, size, datetime) - if all match then compare content
    public ArrayList<HashMap<String, String>> getUserReferenceFilesList() {
        ArrayList<HashMap<String, String>> lst = new ArrayList<>();
        File pfolder = new File(Paths.get(getAppDataBaseFolder(), FOLDER_REFS).toString());
        FilenameFilter filter = (File dir, String name) -> dir.isDirectory() && name.startsWith(getUserRefsNamePrefix()) && name.endsWith(getUserRefsNameExt());
        File[] files = pfolder.listFiles(filter);
        if(files != null) {
            for (File file : files) {
                HashMap<String, String> hm = new HashMap<>();
                loadUserRefsParams(hm, Paths.get(file.getAbsolutePath()).toString());
                if (!hm.isEmpty()) {
                    // add PATH_PARAM here
                    hm.put(REFPATH_PARAM, file.getPath());
                    lst.add(hm);
                }
            }
        }
        return lst;
    }
    // supporting functions
    public static HashMap<String, String> createUserRefsParams(String srcpath, String refpath) {
        HashMap<String, String> hmParams = new HashMap<>();
        File file = new File(srcpath);
        String size = String.format("%d", file.length());
        String datetime = String.format("%d", file.lastModified());
        hmParams.put(SRCPATH_PARAM, srcpath);
        hmParams.put(SIZE_PARAM, size);
        hmParams.put(DATETIME_PARAM, datetime);
        hmParams.put(REFPATH_PARAM, refpath);
        return hmParams;
    }
    public static void loadUserRefsParams(HashMap<String, String> hmParams, String folderpath) {
        String filepath = Paths.get(folderpath, DataApp.PRM_USER_REFS).toString();
        Utils.loadParams(hmParams, filepath);
    }
    public static boolean saveUserRefsParams(HashMap<String, String> hmParams, String folderpath) {
        String filepath = Paths.get(folderpath, DataApp.PRM_USER_REFS).toString();
        return Utils.saveParams(hmParams, filepath);
    }
    
    public boolean extractTarGzFiles(String file, String folder) {
        boolean result = false;
        TarArchiveEntry entry;
        FileInputStream fis = null;
        TarArchiveInputStream tarais = null;
        try {
            fis = new FileInputStream(file);
            tarais = new TarArchiveInputStream(new GzipCompressorInputStream(fis));
            while ((entry = (TarArchiveEntry)tarais.getNextEntry()) != null) {
                logger.logInfo("Extracting file: " + entry.getName());
                Files.copy(tarais, Paths.get(folder, entry.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
            result = true;
        }
        catch(Exception e) {
            logger.logError("Unable to extract files from '" + file + "': " + e.getMessage());
        } finally {
           try {if(tarais != null) tarais.close();} catch (Exception e) { System.out.println("TarArchiveInputStream close exception within exception: " + e.getMessage()); }
           try {if(fis != null) fis.close();} catch (Exception e) { System.out.println("FileInputStream close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    
    public boolean extractZipFiles(String file, String folder) {
        boolean result = false;
        ZipEntry entry;
        FileInputStream fis = null;
        ZipInputStream zipais = null;
        try {
            fis = new FileInputStream(file);
            zipais = new ZipInputStream(fis);
            while ((entry = zipais.getNextEntry()) != null) {
                logger.logInfo("Extracting file: " + entry.getName());
                Files.copy(zipais, Paths.get(folder, entry.getName()), StandardCopyOption.REPLACE_EXISTING);
            }
            result = true;
        }
        catch(Exception e) {
            logger.logError("Unable to extract files from '" + file + "': " + e.getMessage());
        } finally {
           try {if(zipais != null) zipais.close();} catch (Exception e) { System.out.println("ZipInputStream close exception within exception: " + e.getMessage()); }
           try {if(fis != null) fis.close();} catch (Exception e) { System.out.println("FileInputStream close exception within exception: " + e.getMessage()); }
        }
        return result;
    }

    //
    // Resource Functions
    //
    
    // Note: will use script extension, if available, for temp file
    public Path getTmpScriptFileFromResource(String scriptname) {
        Path filepath = null;
        try {
            Path outpath;
            String ext = ".R";
            int pos = scriptname.lastIndexOf(".");
            if(pos != -1)
                ext = scriptname.substring(pos);
            if(Utils.isWindowsOS())
                outpath = Files.createTempFile("tappas", ext);
            else {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
                outpath = Files.createTempFile("tappas", ext, PosixFilePermissions.asFileAttribute(perms));
            }
            InputStream is = getClass().getResourceAsStream("/tappas/scripts/" + scriptname);
            if(is != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                System.out.println("tmpout: " + outpath.toString());
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outpath.toString(), true), "utf-8"));
                    String line;
                    while((line = r.readLine()) != null)
                        writer.write(line + "\n");
                    filepath = outpath;
                } catch(Exception e) {
                    logger.logError("Unable to write script file from resource '" + scriptname + "': " + e.getMessage());
                } finally {
                   try {r.close();} catch(Exception e) { System.out.println("BufferedReader close exception within exception: " + e.getMessage()); } 
                   try {is.close();} catch (Exception e) { System.out.println("InputStream close exception within exception: " + e.getMessage()); }
                   try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
                }
            }
            else
                logger.logError("Unable to get resource stream for '" + scriptname + "'");
        } catch(Exception e)  {
            logger.logError("Unable to create script file from resource '" + scriptname + "': " + e.getMessage());
        }
        
        return filepath;
    }
    public String getFileContentFromResource(String filename) {
        String content = "";
        try {
            InputStream is = getClass().getResourceAsStream(filename);
            if(is != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String line;
                try {
                    while((line = r.readLine()) != null)
                        content += line + "\n";
                } catch(Exception e) {
                    logger.logError("Unable to read file contents from resource '" + filename + "': " + e.getMessage());
                } finally {
                   try {r.close();} catch(Exception e) { System.out.println("BufferedReader close exception within exception: " + e.getMessage()); } 
                   try {is.close();} catch (Exception e) { System.out.println("InputStream close exception within exception: " + e.getMessage()); }
                }
            }
            else
                logger.logError("Unable to get resource stream for '" + filename + "'");
        } catch(Exception e)  {
            logger.logError("Unable to read file from resource '" + filename + "': " + e.getMessage());
        }
        
        return content;
    }
    // will write files out as read only
    public boolean writeFileContentFromResource(String resName, String fileName) {
        boolean result = false;
        try {
            FileOutputStream fos = null;
            byte[] buf = new byte[64 * 1024];
            InputStream is = Tappas.class.getResourceAsStream(resName);
            try {
                int cnt;
                fos = new FileOutputStream(fileName);
                while((cnt = is.read(buf)) > 0)
                    fos.write(buf, 0, cnt);
                result = true;
            } catch(Exception e) {
                logger.logError("Unable to copy file contents from resource '" + resName + "': " + e.getMessage());
            } finally {
                try {is.close();} catch (Exception e) { System.out.println("InputStream close exception within exception: " + e.getMessage()); }
                try {if(fos != null) fos.close();} catch (Exception e) { System.out.println("FileOutputStream close exception within exception: " + e.getMessage()); }
            }
        } catch(Exception e)  {
            logger.logError("Unable to create script file from resource '" + resName + "': " + e.getMessage());
        }
        return result;
    }
    public boolean writeRscriptFilepath(String rFilepath) {
        boolean result = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getRScriptOverwriteFilepath(), false), "utf-8"));
            writer.write(rFilepath);
            result = true;
        } catch(Exception e) {
            logger.logError("Unable to save Rscript filepath: " + e.getMessage());
        } finally {
            try {if(writer != null) writer.close();} catch (Exception e) {}
        }
        return result;
    }
    public String getNCBIGeneId(String species, String geneName) {
        String id = "";
        if(species.equals("Mus_musculus")) {
            if(hmNCBI_MM.isEmpty())
                loadNCBIGeneData(hmNCBI_MM, APP_MM_NCBI_GENES);
            if(!hmNCBI_MM.isEmpty()) {
                if(hmNCBI_MM.containsKey(geneName))
                    id = hmNCBI_MM.get(geneName);
            }
        }
        else if(species.equals("Homo_sapiens")) {
            if(hmNCBI_HS.isEmpty())
                loadNCBIGeneData(hmNCBI_HS, APP_HS_NCBI_GENES);
            if(!hmNCBI_HS.isEmpty()) {
                if(hmNCBI_HS.containsKey(geneName))
                    id = hmNCBI_HS.get(geneName);
            }
        }
        return id;
    }
    private void loadNCBIGeneData(HashMap<String, String> hm, String filename) {
        hm.clear();
        String filepath = Paths.get(getAppDataFolder(), filename).toString();
        if(Files.exists(Paths.get(filepath))) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                int lnum = 0;
                for(String line : lines) {
                    if(lnum > 0 && !line.trim().isEmpty()) {
                        String[] fields = line.split("\t");
                        if(fields.length >= 2)
                            hm.put(fields[1], fields[0]);
                        else {
                            logger.logWarning("Invalid line, " + line + ", in NCBI genes resource file '" + filename + "'");
                            hm.clear();
                            break;
                        }
                    }
                    lnum++;
                }
            }
            catch (Exception e) {
                logger.logWarning("Load NCBI genes file, " + filename + " , exception: " + e.getMessage());
                hm.clear();
            }
        }
    }
    
    //
    // Data Classes
    //
    public static class KeyValueData {
        public final SimpleStringProperty key;
        public final SimpleStringProperty value;
 
        public KeyValueData(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }
        public String getKey() { return key.get(); }
        public String getValue() { return value.get(); }
    }
    protected static class EnumData implements Comparable<EnumData> {
        public String id;
        public String name;
        
        public EnumData(String id, String name) {
            this.id = id;
            this.name = name;
        }
        @Override
        public int compareTo(EnumData entry) {
            return (id.compareToIgnoreCase(entry.id));
        }
    }
    protected static class SpeciesData implements Comparable<SpeciesData> {
        public String id;
        public String name;
        public List<RefFilesData> files;
        
        public SpeciesData(String id, String name, List<RefFilesData> files) {
            this.id = id;
            this.name = name;
            this.files = files;
        }
        @Override
        public int compareTo(SpeciesData entry) {
            return (name.compareToIgnoreCase(entry.name));
        }
    }
    protected static class RefFilesData implements Comparable<RefFilesData> {
        public String id;
        public String refType;
        public String version;
        public String caption;
        
        public RefFilesData(String id, String refType, String version, String caption) {
            this.id = id;
            this.refType = refType;
            this.version = version;
            this.caption = caption;
        }
        @Override
        public int compareTo(RefFilesData entry) {
            return (caption.compareToIgnoreCase(entry.caption));
        }
    }
    protected static class EnumDataObj implements Comparable<EnumData> {
        public String id;
        public String name;
        public Object obj;
        
        public EnumDataObj(String id, String name, Object obj) {
            this.id = id;
            this.name = name;
            this.obj = obj;
        }
        @Override
        public int compareTo(EnumData entry) {
            return (id.compareToIgnoreCase(entry.id));
        }
    }
    public static class RankedListEntry  implements Comparable<RankedListEntry> {
        String id;
        double value;
        public RankedListEntry(String id, double value) {
            this.id = id;
            this.value = value;
        }
        @Override
        public int compareTo(RankedListEntry entry) {
            return (id.compareToIgnoreCase(entry.id));
        }
    }

    
    // Gene, protein, and transcript analysis results
    // (could have a lower base class for SelectionData and have this one extend it)
    public static class SelectionDataResults { //extends SelectionData {
        // data type - determines id and which features to get based on data type
        public DataType dataType;

        public SimpleBooleanProperty selected = null;
        // primary id, data type determines what it means (trans,protein,ene)
        public SimpleStringProperty id = null;
        // gene specific data
        public SimpleStringProperty gene = null;
        public SimpleStringProperty geneDescription = null;

        // mean expression level data by condition
        public SimpleDoubleProperty[] conditions = null;
        // distal level data by condition DPA
        public SimpleDoubleProperty[] distal = null;
        // proximal level data by condition DPA
        public SimpleDoubleProperty[] proximal = null;
        
        // custom annotation columns by column name
        public HashMap<String, SimpleIntegerProperty> hmAnnotInt = null;
        public HashMap<String, SimpleStringProperty> hmAnnotString = null;

        public SelectionDataResults(boolean selected, DataType dataType, String id, String gene, String geneDescription) {
            this.selected = new SimpleBooleanProperty(selected);
            //super(selected);
            this.dataType = dataType;
            this.id = new SimpleStringProperty(id);
            this.gene = new SimpleStringProperty(gene);
            this.geneDescription = new SimpleStringProperty(geneDescription);
        }
        public DataType getDataType() { return dataType; }
        public SimpleBooleanProperty SelectedProperty() { return selected; }        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
        public String getId() { return id.get(); }
        public String getGene() { return gene.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        // internal use - called from row selection
        public Double getCondition(String strIdx) { return conditions[Integer.parseInt(strIdx.trim())].get(); }
        public Double getDistal(String strIdx) { return distal[Integer.parseInt(strIdx.trim())].get(); }
        public Double getProximal(String strIdx) { return proximal[Integer.parseInt(strIdx.trim())].get(); }
        public Integer getAnnotationInt(String colName) { return hmAnnotInt.get(colName).get(); }
        public String getAnnotationString(String colName) { return hmAnnotString.get(colName).get(); }

        // DPA
        // data and table support functions
        public static Callback getValueDistalCallback(ObservableList<DataDPA.DPASelectionResults> data, Integer idx) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Double>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Double> p) {
                    //ObservableValue<Number> res = new ReadOnlyObjectWrapper<>(data.getValue().conditions[idx]);
                    return p.getValue().distal[idx];
                }
            });
        }
        
        // data and table support functions
        public static Callback getValueProximalCallback(ObservableList<DataDPA.DPASelectionResults> data, Integer idx) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Double>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Double> p) {
                    //ObservableValue<Number> res = new ReadOnlyObjectWrapper<>(data.getValue().conditions[idx]);
                    return p.getValue().proximal[idx];
                }
            });
        }

        // UTRL
        // data and table support functions
        public static Callback getValueDistalUTRLCallback(ObservableList<DataUTRL.UTRLSelectionResults> data, Integer idx) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Double>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Double> p) {
                    //ObservableValue<Number> res = new ReadOnlyObjectWrapper<>(data.getValue().conditions[idx]);
                    return p.getValue().distal[idx];
                }
            });
        }

        // data and table support functions
        public static Callback getValueProximalUTRLCallback(ObservableList<DataUTRL.UTRLSelectionResults> data, Integer idx) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Double>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Double> p) {
                    //ObservableValue<Number> res = new ReadOnlyObjectWrapper<>(data.getValue().conditions[idx]);
                    return p.getValue().proximal[idx];
                }
            });
        }

        // data and table support functions
        public static Callback getExpMatrixDataCallback(int idx) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Double>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Double> p) {
                    return p.getValue().conditions[idx];
                }
            });
        }
        public static Callback getAnnotationDataCallbackInt(String colName) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, Integer>, ObservableValue<Number>>() {
                @Override
                public ObservableValue<Number> call(TableColumn.CellDataFeatures<SelectionDataResults, Integer> p) {
                    return p.getValue().hmAnnotInt.get(colName);
                }
            });
        }
        public static Callback getAnnotationDataCallbackString(String colName) {
            return (new Callback<TableColumn.CellDataFeatures<SelectionDataResults, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<SelectionDataResults, String> p) {
                    return p.getValue().hmAnnotString.get(colName);
                }
            });
        }
        public static HashMap<String, HashMap<String, String>> getAnnotationFeaturesData(DataType dataType, DlgAddColumn.Params params,
               HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmFilter, TableView tblView) {
            HashMap<String, HashMap<String, String>> hmTreeSelections = new HashMap<>();
            //String gene;
            String colCount = "";
            String colId = "";
            String colName = "";
            Boolean bId, bName, bCount;
            bId = bName = bCount = false;
            int fldFactor = 10;
            for(String id : params.lstColumnSels) {
                String name = DlgAddColumn.Params.getColName(id, params.name);
                if(id.equals(DlgAddColumn.Params.ColType.ID.name())) {
                    bId = true;
                    colId = name;
                }
                else if(id.equals(DlgAddColumn.Params.ColType.DESCRIPTION.name())) {
                    bName = true;
                    colName = name;
                }
                else if(id.equals(DlgAddColumn.Params.ColType.COUNT.name())) {
                    bCount = true;
                    colCount = name;
                }
            }
            TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
            ObservableList<SelectionDataResults> items = (ObservableList<SelectionDataResults>) info.data;
            String id, idVal, nameVal;
            int cntVal, fldCnt;
            for(SelectionDataResults dr : items) {
                idVal = nameVal = "";
                cntVal = fldCnt = 0;
                id = dr.getId();
                //gene = dr.getGene();
                if(hmFilter.containsKey(id)) {
                    HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmFilter.get(id);
                    for(String db : hmDBs.keySet()) {
                        HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                        for(String cat : hmCats.keySet()) {
                            if(!hmTreeSelections.containsKey(cat))
                                hmTreeSelections.put(cat, new HashMap<>());
                            HashMap<String, String> hmCatSels = hmTreeSelections.get(cat);
                            HashMap<String, String> hmIds = hmCats.get(cat);
                            cntVal += hmIds.size();
                            for(String dbCatId : hmIds.keySet()) {
                                idVal += idVal.isEmpty()? "" : ",";
                                nameVal += nameVal.isEmpty()? "" : ",";
                                if((fldCnt++ % fldFactor) == 0 && fldCnt > 1) {
                                    if(bId)
                                        idVal += "\n";
                                    if(bName)
                                        nameVal += "\n";
                                }
                                if(bId)
                                    idVal += (params.prefix? cat + ":" : "") + dbCatId;
                                if(bName)
                                    nameVal += (params.prefix? cat + ":" : "") + hmIds.get(dbCatId);
                                hmCatSels.put(dbCatId, hmIds.get(dbCatId));
                            }
                        }

                        // should never have more than 1 db, ignore if so
                        break;
                    }
                }
                if(bId) {
                    if(dr.hmAnnotString == null)
                        dr.hmAnnotString = new HashMap<>();
                    dr.hmAnnotString.put(colId, new SimpleStringProperty(idVal));
                }
                if(bName) {
                    if(dr.hmAnnotString == null)
                        dr.hmAnnotString = new HashMap<>();
                    dr.hmAnnotString.put(colName, new SimpleStringProperty(nameVal));
                }
                if(bCount) {
                    if(dr.hmAnnotInt == null)
                        dr.hmAnnotInt = new HashMap<>();
                    dr.hmAnnotInt.put(colCount, new SimpleIntegerProperty(cntVal));
                }
            }
            return hmTreeSelections;
        }
    }
}
