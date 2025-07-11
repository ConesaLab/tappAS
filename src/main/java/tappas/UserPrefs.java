/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.util.prefs.Preferences;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu and Pedro Salguero - psalguero@cipf.es
 */

public class UserPrefs extends AppObject {
    Preferences prefs = Preferences.userNodeForPackage(UserPrefs.class);
    
    private static final String PREFS_TEST = "PREFS_TEST";
    private static final String APP_FOLDER = "APP_FOLDER";
    private static final String RECENT_PROJECT = "RECENT_PROJECT_";
    private static final String EXPORT_DATA_FOLDER = "EXPORT_DATA_FOLDER";
    private static final String EXPORT_IMAGE_FOLDER = "EXPORT_IMAGE_FOLDER";
    private static final String WEBFILES_UPDATE = "WEBFILES_UPDATE";
    
    private static final String IMPORT_LIST_FOLDER = "IMPORT_LIST_FOLDER";
    private static final String IMPORT_DATA_FOLDER = "IMPORT_DATA_FOLDER";
    private static final String IMPORT_ANNOT_FOLDER = "IMPORT_ANNOT_FOLDER";
    private static final String IMPORT_RANKEDLIST_FOLDER_GSEA = "IMPORT_RANKEDLIST_FOLDER_GSEA";
    private static final String IMPORT_RANKEDLIST_FOLDER_FEA = "IMPORT_RANKEDLIST_FOLDER_FEA";
    private static final String IMPORT_RANKEDLIST_SELECTION = "IMPORT_RANKEDLIST_SELECTION";
    
    private static final String IMPORT_FEATURES_FOLDER = "IMPORT_FEATURES_FOLDER";
    
    public UserPrefs(App app) {
        super(app);
    }
    public boolean canWrite() {
        boolean result = false;
        prefs.put(PREFS_TEST, "TAPPAS");
        if(prefs.get(PREFS_TEST, "").equals("TAPPAS"));
            result = true;
        return result;
    }
    public void setAppBaseFolder(String value) {
        prefs.put(APP_FOLDER, value);
    }
    public String getAppBaseFolder() {
        return prefs.get(APP_FOLDER, "");
    }
    public void setRecentProject(int num, String id) {
        prefs.put(RECENT_PROJECT + num, id);
    }
    public String getRecentProject(int num) {
        return prefs.get(RECENT_PROJECT + num, "");
    }
    public void setExportDataFolder(String value) {
        prefs.put(EXPORT_DATA_FOLDER, value);
    }
    public String getExportDataFolder() {
        return prefs.get(EXPORT_DATA_FOLDER, "");
    }
    public void setExportImageFolder(String value) {
        prefs.put(EXPORT_IMAGE_FOLDER, value);
    }
    public String getExportImageFolder() {
        return prefs.get(EXPORT_IMAGE_FOLDER, "");
    }
    public void setImportListFolder(String value) {
        prefs.put(IMPORT_LIST_FOLDER, value);
    }
    public String getImportListFolder() {
        return prefs.get(IMPORT_LIST_FOLDER, "");
    }
    public void setImportFeaturesFolder(String value) {
        prefs.put(IMPORT_FEATURES_FOLDER, value);
    }
    public String getImportFeaturesFolder() {
        return prefs.get(IMPORT_FEATURES_FOLDER, "");
    }
    public void setImportDataFolder(String value) {
        prefs.put(IMPORT_DATA_FOLDER, value);
    }
    public String getImportDataFolder() {
        return prefs.get(IMPORT_DATA_FOLDER, "");
    }
    public void setImportAnnotFolder(String value) {
        prefs.put(IMPORT_ANNOT_FOLDER, value);
    }
    public String getImportAnnotFolder() {
        return prefs.get(IMPORT_ANNOT_FOLDER, "");
    }
    public void setImportRankedListGSEAFolder(String value) {
        prefs.put(IMPORT_RANKEDLIST_FOLDER_GSEA, value);
    }
    public String getImportRankedListGSEAFolder() {
        return prefs.get(IMPORT_RANKEDLIST_FOLDER_GSEA, "");
    }
    public void setImportRankedListFEAFolder(String value) {
        prefs.put(IMPORT_RANKEDLIST_FOLDER_FEA, value);
    }
    public String getImportRankedListFEAFolder() {
        return prefs.get(IMPORT_RANKEDLIST_FOLDER_FEA, "");
    }
    public void setImportRankedListSelectionFolder(String value) {
        prefs.put(IMPORT_RANKEDLIST_SELECTION, value);
    }
    public String getImportRankedListSelectionFolder() {
        return prefs.get(IMPORT_RANKEDLIST_SELECTION, "");
    }
    
    public String getInfo() {
        String children = "";
        try {
            for(String child : prefs.childrenNames())
            {
                if(!children.isEmpty())
                    children += ",";
                children += child;
            }
        }
        catch(Exception e) {children = "ERROR: " + e.getMessage();}
        return "Path: " + prefs.absolutePath() +
                ", userRootPath: " + Preferences.userRoot().absolutePath() +
                ", sysRootPath: " + Preferences.systemRoot().absolutePath() +
                ", name: " + prefs.name() +
                ", class: " + UserPrefs.class +
                ", isUserNode: " + prefs.isUserNode() + 
                ", children: " + children;
    }
    public void setWebFilesUpdate(String value) {
        prefs.put(WEBFILES_UPDATE, value);
    }
    public String getWebFilesUpdate() {
        return prefs.get(WEBFILES_UPDATE, "");
    }
}
