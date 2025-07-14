/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tappas.DataApp.DataType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class DataGSEA extends AppObject {
    
    public String getGSEAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_GSEA).toString(); }
    public String getGSEAParamsFilepath(String id) { return Paths.get(getGSEAFolder(), DataApp.PRM_GSEA_NAME + id + DataApp.PRM_EXT).toString(); }
    public String getGSEAClusterParamsFilepath(String id) { return Paths.get(getGSEAFolder(), DataApp.PRM_GSEACLUSTER_NAME + id + DataApp.PRM_EXT).toString(); }
    public String getGSEARankedListFilepath(String dataType, String id, int number) { 
        return Paths.get(getGSEAFolder(), (dataType + "_" + DataProject.RANKEDLIST_VALUES + "." + number + "." + id + DataApp.TSV_EXT)).toString();
    }
    public String getGSEAResultsClusterFilepath(String cdataType, String id) {
        return (Paths.get(getGSEAFolder(), DataApp.RESULTSCLUSTER_NAME + cdataType.toLowerCase() + "." + id + DataApp.RESULTS_EXT).toString());
    }
    public String getGSEAResultsFilepath(String dataType, String id) { 
        return (Paths.get(getGSEAFolder(), DataApp.RESULTS_NAME + dataType.toLowerCase() + "." + id + DataApp.RESULTS_EXT).toString());
    }
    public String getGSEAResultsItemsFilepath(String dataType, String id) { 
        return (Paths.get(getGSEAFolder(), DataApp.RESULTS_NAME + dataType.toLowerCase() + ".items." + id + DataApp.RESULTS_EXT).toString());
    }
    public String getGSEALogFilepath(String id) { return Paths.get(getGSEAFolder(), DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getGSEAFeaturesFilepath(String id, String type, int value) { return Paths.get(getGSEAFolder(), type.toLowerCase() + "_" + DataApp.FEATURES_NAME + "." + value + "." + id + DataApp.TSV_EXT).toString(); }

    public String getGSEAClusterLogFilepath(String id) { return Paths.get(getGSEAFolder(), DataApp.CLUSTER_NAME + DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getGSEAClusterDoneFilepath(String id) { return Paths.get(getGSEAFolder(), DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID + id + DataApp.TEXT_EXT).toString(); }
    //public String getGSEAFeaturesFilepath(String id, String type) { return Paths.get(getGSEAFolder(), type.toLowerCase() + "_" + DataApp.FEATURES_NAME + "." + id + DataApp.TSV_EXT).toString(); }

    DataDEA deaData;
    public DataGSEA(Project project) {
        super(project, null);
        deaData = new DataDEA(project);
    }
    public void initialize() {
        clearData();
    }
    public void clearData() {
    }
    public boolean hasGSEAData(String dataType, String id) { 
        return (Files.exists(Paths.get(getGSEAFolder(), DataApp.DONE_NAME + dataType.toLowerCase() + "." + id + DataApp.TEXT_EXT)));
    }
    public boolean hasGSEAClusterData(String id) {
        return (Files.exists(Paths.get(getGSEAFolder(), DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID + id + DataApp.TEXT_EXT)));
    }
    public boolean hasAnyGSEAData() {
        File folder = new File(Paths.get(getGSEAFolder()).toString());
        FilenameFilter filter = (File dir, String name) -> (name.startsWith(DataApp.DONE_NAME) && name.endsWith(DataApp.TEXT_EXT));        
        File[] files = folder.listFiles(filter);
        return (files != null && files.length > 0);
    }
    public void setGSEAParams(HashMap<String, String> hmp, String id) { 
        if(hmp != null)
            Utils.saveParams(hmp, getGSEAParamsFilepath(id));
    } 
    public HashMap<String, String> getGSEAParams(String id) {
        HashMap<String, String> hmp = new HashMap<>();
        Utils.loadParams(hmp, getGSEAParamsFilepath(id));
        return hmp;
    }
    public void setGSEAClusterParams(HashMap<String, String> hmp, String id) {
        if(hmp != null)
            Utils.saveParams(hmp, getGSEAClusterParamsFilepath(id));
    }
    public HashMap<String, String> getGSEAClusterParams(String id) {
        HashMap<String, String> hmp = new HashMap<>();
        Utils.loadParams(hmp, getGSEAClusterParamsFilepath(id));
        return hmp;
    }
    public ArrayList<DataApp.EnumData> getGSEAResultsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File gseaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_GSEA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.DONE_NAME) && name.endsWith(DataApp.TEXT_EXT);        
        File[] files = gseaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                // ignore cluster done files "done_cluster.*"
                if(!name.startsWith(DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID)) {
                    int sidx = name.indexOf('.');
                    if (sidx != -1) {
                        int eidx = name.indexOf(".", ++sidx);
                        if (eidx != -1) {
                            String id = name.substring(sidx, eidx);
                            String gseaName = id;
                            HashMap<String, String> hm = getGSEAParams(id);
                            if (hm != null && hm.containsKey(DlgGSEAnalysis.Params.NAME_PARAM))
                                gseaName = hm.get(DlgGSEAnalysis.Params.NAME_PARAM);
                            lst.add(new DataApp.EnumData(id, gseaName));
                        }
                    }
                }
            }
        }
        return lst;
    }
    public ArrayList<DataApp.EnumData> getGSEAParamsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File gseaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_GSEA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.PRM_GSEA_NAME) && name.endsWith(DataApp.PRM_EXT);        
        File[] files = gseaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String gseaName = id;
                        HashMap<String, String> hm = getGSEAParams(id);
                        if(hm != null && hm.containsKey(DlgGSEAnalysis.Params.NAME_PARAM))
                            gseaName = hm.get(DlgGSEAnalysis.Params.NAME_PARAM);
                        lst.add(new DataApp.EnumData(id, gseaName));
                    }
                }
            }
        }
        return lst;
    }
    public void clearDataGSEA(boolean rmvPrm) {
        clearData();
        removeAllGSEAResultFiles(rmvPrm);
    }
    public void clearDataGSEA(String id, boolean rmvPrm) {
        clearData();
        removeGSEAFiles(id, rmvPrm);
    }
    public void removeGSEAFiles(String id, boolean rmvPrms) {
        ArrayList<String> lstFiles = getGSEAFilesList(id, rmvPrms);
        for(String filepath : lstFiles)
            Utils.removeFile(Paths.get(filepath));
    }
    public void removeGSEAClusterFiles(String id) {
        Utils.removeFile(Paths.get(getGSEAClusterDoneFilepath(id)));
        Utils.removeFile(Paths.get(getGSEAClusterParamsFilepath(id)));
        Utils.removeFile(Paths.get(getGSEAClusterLogFilepath(id)));
    }
    public void removeAllGSEAResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_GSEA), rmvPrms);
    }    
    public boolean genGSEAInputFiles(String featureId, DlgGSEAnalysis.Params gseaParams, SubTabBase subTab, String logfilepath) {
        boolean result = false;

        //generate matrix files (DEA files)
        if(!Files.exists(Paths.get(project.data.getExpFactorsFilepath())))
            project.data.copyExpFactorsFile(project.data.getExpFactorsFilepath());
        if(!project.data.getExperimentType().equals(DataApp.ExperimentType.Two_Group_Comparison)) {
            if(!Files.exists(Paths.get(project.data.getTimeFactorsFilepath())))
                project.data.copyTimeFactorsFile(project.data.getTimeFactorsFilepath());
        }
        
        // generate lists and features files
        HashMap<String, Object> hmFilterTrans = project.data.getResultsTrans();
        Path pathFeatures1 = Paths.get(getGSEAFeaturesFilepath(featureId, gseaParams.dataType.name(), 1));
        Path pathFeatures2 = Paths.get(getGSEAFeaturesFilepath(featureId, gseaParams.dataType.name(), 2));
        boolean extended = false;
        switch(gseaParams.dataType) {
            case GENE:
                
                if(!Files.exists(Paths.get(project.data.getGeneMatrixFilepath())) || !Files.exists(Paths.get(project.data.getGeneMatrixRawFilepath()))){
                    if(project.data.genExpressionRawFile(gseaParams.dataType, false) && project.data.genExpressionFile(gseaParams.dataType, false));
                }
                
                switch(gseaParams.rankedList1) {
                    case DEA:
                        subTab.appendLogLine("Loading gene DEA results list...", logfilepath);
                        HashMap<String, HashMap<String, Object>> hmResultsGeneTrans = project.data.getResultsGeneTrans();
                        DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.GENE);
                        DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.GENE, "", deaParams.sigValue, deaParams.FCValue);
                        if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                            // get ranked list: id and value
                            ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                            if(lstRL != null && !lstRL.isEmpty()) {
                                // generate the features files
                                subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                if(gseaParams.customFile.isEmpty()){
                                    project.data.genGeneFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmFilterTrans);
                                    //subTab.appendLogLine(pathFeatures.toString(), logfilepath);
                                    //subTab.appendLogLine(gseaParams.hmFeatures.toString(), logfilepath);
                                }else
                                    project.data.genCustomFeatures(pathFeatures1.toString(), gseaParams.customFile, gseaParams.dataType, lstRL);
                                if(Files.exists(pathFeatures1)) {
                                    subTab.appendLogLine("Generating length file...", logfilepath);

                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // get length list - Warning: must be in same order as lstRL
                                    HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmResultsGeneTrans);
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        HashMap<String, Integer> hmGTLengths = hmLengths.get(entry.id);
                                        int len = 0;
                                        int cnt = 0;
                                        for(int translen : hmGTLengths.values()) {
                                            len += translen;
                                            cnt++;
                                        }
                                        double gl = 0.0;
                                        if(cnt > 0)
                                            gl = len / cnt;
                                        lstLength.add((int)gl);
                                    }
                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                        }
                        break;
                    case DIUTRANSEXT:
                        extended = true;
                        // flow into normal DIU
                    case DIUTRANS:
                        subTab.appendLogLine("Loading Transcript DIU results list...", logfilepath);
                        result = loadDIURankedList(DataType.TRANS, extended, featureId, pathFeatures1, gseaParams, subTab, logfilepath, 1);
                        
                        break;
                    case DIUPROTEXT:
                        extended = true;
                        // flow into normal DIU
                    case DIUPROT:
                        subTab.appendLogLine("Loading Protein DIU results list...", logfilepath);
                        result = loadDIURankedList(DataType.PROTEIN, extended, featureId, pathFeatures1, gseaParams, subTab, logfilepath, 1);
                        break;
                    case FROMFILE:
                        subTab.appendLogLine("Loading genes ranked list from file: ", logfilepath);
                        ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile1);
                        if(!lstRanked.isEmpty()) {
                            ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                            ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                            HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = new HashMap<>();
                            HashMap<String, HashMap<String, Object>> hmGT = project.data.getResultsGeneTrans();
                            for(DataApp.RankedListEntry entry : lstRanked) {
                                if(hmGT.containsKey(entry.id)) {
                                    lstRL.add(entry);
                                    hmFilterGeneTrans.put(entry.id, hmGT.get(entry.id));
                                }
                                else
                                    lstRLBad.add(entry);
                            }
                            if(!lstRLBad.isEmpty()) {
                                logger.logInfo("WARNING: Ranked list contains " + lstRLBad.size() + " genes that are not part of the project data.");
                            }
                            if(!lstRL.isEmpty()) {
                                subTab.appendLogLine("Generating annotation features file...", logfilepath);

                                // create transcripts filter
                                HashMap<String, Object> hmTrans = new HashMap<>();
                                for(HashMap<String, Object> hm : hmFilterGeneTrans.values()) {
                                    for(String trans : hm.keySet())
                                        hmTrans.put(trans, null);
                                }
                                if(gseaParams.customFile.isEmpty())
                                    project.data.genGeneFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmTrans);
                                else
                                    project.data.genCustomFeatures(pathFeatures1.toString(), gseaParams.customFile, gseaParams.dataType, lstRL);
                                if(Files.exists(pathFeatures1)) {
                                    subTab.appendLogLine("Generating length file...", logfilepath);

                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // get length list - Warning: must be in same order as lstRL
                                    HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmFilterGeneTrans);
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        HashMap<String, Integer> hmGTLengths = hmLengths.get(entry.id);
                                        int len = 0;
                                        int cnt = 0;
                                        for(int translen : hmGTLengths.values()) {
                                            len += translen;
                                            cnt++;
                                        }
                                        double gl = 0.0;
                                        if(cnt > 0)
                                            gl = len / cnt;
                                        lstLength.add((int)gl);
                                    }
                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                        }
                        break;
                }
                if(gseaParams.useMulti.equals("TRUE")){
                    switch(gseaParams.rankedList2) {
                        case DEA:
                            subTab.appendLogLine("Loading gene DEA results list...", logfilepath);
                            HashMap<String, HashMap<String, Object>> hmResultsGeneTrans = project.data.getResultsGeneTrans();
                            DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.GENE);
                            DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.GENE, "", deaParams.sigValue, deaParams.FCValue);
                            if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                                // get ranked list: id and value
                                ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                                if(lstRL != null && !lstRL.isEmpty()) {
                                    // generate the features files
                                    subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                    if(gseaParams.customFile.isEmpty()){
                                        project.data.genGeneFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmFilterTrans);
                                        //subTab.appendLogLine(pathFeatures.toString(), logfilepath);
                                        //subTab.appendLogLine(gseaParams.hmFeatures.toString(), logfilepath);
                                    }else
                                        project.data.genCustomFeatures(pathFeatures2.toString(), gseaParams.customFile, gseaParams.dataType, lstRL);
                                    if(Files.exists(pathFeatures2)) {
                                        subTab.appendLogLine("Generating length file...", logfilepath);

                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // get length list - Warning: must be in same order as lstRL
                                        HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmResultsGeneTrans);
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            HashMap<String, Integer> hmGTLengths = hmLengths.get(entry.id);
                                            int len = 0;
                                            int cnt = 0;
                                            for(int translen : hmGTLengths.values()) {
                                                len += translen;
                                                cnt++;
                                            }
                                            double gl = 0.0;
                                            if(cnt > 0)
                                                gl = len / cnt;
                                            lstLength.add((int)gl);
                                        }
                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                    }
                                }
                            }
                            break;
                        case DIUTRANSEXT:
                            extended = true;
                            // flow into normal DIU
                        case DIUTRANS:
                            subTab.appendLogLine("Loading Transcript DIU results list...", logfilepath);
                            result = loadDIURankedList(DataType.TRANS, extended, featureId, pathFeatures2, gseaParams, subTab, logfilepath, 2);
                            break;
                        case DIUPROTEXT:
                            extended = true;
                            // flow into normal DIU
                        case DIUPROT:
                            subTab.appendLogLine("Loading Protein DIU results list...", logfilepath);
                            result = loadDIURankedList(DataType.PROTEIN, extended, featureId, pathFeatures2, gseaParams, subTab, logfilepath, 2);
                            break;
                        case FROMFILE:
                            subTab.appendLogLine("Loading genes ranked list from file: ", logfilepath);
                            ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile2);
                            if(!lstRanked.isEmpty()) {
                                ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                                ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                                HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = new HashMap<>();
                                HashMap<String, HashMap<String, Object>> hmGT = project.data.getResultsGeneTrans();
                                for(DataApp.RankedListEntry entry : lstRanked) {
                                    if(hmGT.containsKey(entry.id)) {
                                        lstRL.add(entry);
                                        hmFilterGeneTrans.put(entry.id, hmGT.get(entry.id));
                                    }
                                    else
                                        lstRLBad.add(entry);
                                }
                                if(!lstRLBad.isEmpty()) {
                                    logger.logInfo("WARNING: Ranked list contains " + lstRLBad.size() + " genes that are not part of the project data.");
                                }
                                if(!lstRL.isEmpty()) {
                                    subTab.appendLogLine("Generating annotation features file...", logfilepath);

                                    // create transcripts filter
                                    HashMap<String, Object> hmTrans = new HashMap<>();
                                    for(HashMap<String, Object> hm : hmFilterGeneTrans.values()) {
                                        for(String trans : hm.keySet())
                                            hmTrans.put(trans, null);
                                    }
                                    if(gseaParams.customFile.isEmpty())
                                        project.data.genGeneFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmTrans);
                                    else
                                        project.data.genCustomFeatures(pathFeatures2.toString(), gseaParams.customFile, gseaParams.dataType, lstRL);
                                    if(Files.exists(pathFeatures2)) {
                                        subTab.appendLogLine("Generating length file...", logfilepath);

                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // get length list - Warning: must be in same order as lstRL
                                        HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmFilterGeneTrans);
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            HashMap<String, Integer> hmGTLengths = hmLengths.get(entry.id);
                                            int len = 0;
                                            int cnt = 0;
                                            for(int translen : hmGTLengths.values()) {
                                                len += translen;
                                                cnt++;
                                            }
                                            double gl = 0.0;
                                            if(cnt > 0)
                                                gl = len / cnt;
                                            lstLength.add((int)gl);
                                        }
                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                    }
                                }
                            }
                            break;
                    }
                }
                
                break;
            case PROTEIN:
                
                if(!Files.exists(Paths.get(project.data.getProteinMatrixFilepath())) || !Files.exists(Paths.get(project.data.getProteinMatrixRawFilepath()))){
                    if(project.data.genExpressionRawFile(gseaParams.dataType, false) && project.data.genExpressionFile(gseaParams.dataType, false));
                }
                
                switch(gseaParams.rankedList1) {
                    case DEA:
                        subTab.appendLogLine("Loading protein DEA results list...", logfilepath);
                        DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.PROTEIN);
                        DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.PROTEIN, "", deaParams.sigValue, deaParams.FCValue);
                        if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                            // get ranked list: id and value
                            ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                            if(lstRL != null && !lstRL.isEmpty()) {
                                // make a hash map of proteins and get corresponding transcripts
                                HashMap<String, Object> hmProteins = new HashMap<>();
                                for(DataApp.RankedListEntry entry : lstRL)
                                    hmProteins.put(entry.id, null);
                                HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getProteinTransLengths(hmProteins, hmFilterTrans);

                                // generate the features files
                                subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                HashMap<String, Object> hmProteinTrans = new HashMap<>();
                                for(HashMap<String, Integer> hm : hmLengths.values()) {
                                    for(String trans : hm.keySet())
                                        hmProteinTrans.put(trans, null);
                                }
                                project.data.genProteinFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmProteinTrans);
                                if(Files.exists(pathFeatures1)) {
                                    subTab.appendLogLine("Generating length file...", logfilepath);

                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // get length list - Warning: must be in same order as lstRL
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        HashMap<String, Integer> hmPTLengths = hmLengths.get(entry.id);
                                        int len = 0;
                                        int cnt = 0;
                                        for(int translen : hmPTLengths.values()) {
                                            len += translen;
                                            cnt++;
                                        }
                                        double gl = 0.0;
                                        if(cnt > 0)
                                            gl = len / cnt;
                                        lstLength.add((int)gl);
                                    }
                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                        }
                        break;
                    case FROMFILE:
                        subTab.appendLogLine("Loading protein ranked list from file...", logfilepath);
                        ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile1);
                        if(!lstRanked.isEmpty()) {
                            // get list of project proteins
                            HashMap<String, Object> hmFilterProteins = project.data.getResultsProteins();

                            ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                            ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                            for(DataApp.RankedListEntry entry : lstRanked) {
                                if(hmFilterProteins.containsKey(entry.id))
                                    lstRL.add(entry);
                                else
                                    lstRLBad.add(entry);
                            }
                            if(!lstRLBad.isEmpty()) {
                                logger.logInfo("WARNING: Ranked list contains " + lstRLBad.size() + " proteins that are not part of the project.");
                            }
                            if(!lstRL.isEmpty()) {
                                // create transcripts filter
                                HashMap<String, Object> hmTrans = new HashMap<>();
                                ArrayList<String> lstTrans;
                                String gene;
                                for(DataApp.RankedListEntry entry : lstRL) {
                                    lstTrans = project.data.getProteinTrans(entry.id, hmFilterTrans);
                                    for(String trans : lstTrans) {
                                        if(hmFilterTrans.containsKey(trans))
                                            hmTrans.put(trans, null);
                                    }
                                }

                                // generate the features files
                                project.data.genProteinFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmTrans);
                                if(Files.exists(pathFeatures1)) {
                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // make a hash map of proteins and get corresponding transcripts
                                    HashMap<String, Object> hmProteins = new HashMap<>();
                                    for(DataApp.RankedListEntry entry : lstRL)
                                        hmProteins.put(entry.id, null);

                                    // get length list - Warning: must be in same order as lstBkgnd
                                    HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getProteinTransLengths(hmProteins, hmFilterTrans);
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        HashMap<String, Integer> hmPTLengths = hmLengths.get(entry.id);
                                        int len = 0;
                                        int cnt = 0;
                                        for(int translen : hmPTLengths.values()) {
                                            len += translen;
                                            cnt++;
                                        }
                                        double gl = 0.0;
                                        if(cnt > 0)
                                            gl = len / cnt;
                                        lstLength.add((int)gl);
                                    }

                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                            else {
                                subTab.appendLogLine("There were no matching proteins found in the project", logfilepath);
                            }
                        }
                        break;
                }
                if(gseaParams.useMulti.equals("TRUE")){
                    switch(gseaParams.rankedList2) {
                        case DEA:
                            subTab.appendLogLine("Loading protein DEA results list...", logfilepath);
                            DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.PROTEIN);
                            DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.PROTEIN, "", deaParams.sigValue, deaParams.FCValue);
                            if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                                // get ranked list: id and value
                                ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                                if(lstRL != null && !lstRL.isEmpty()) {
                                    // make a hash map of proteins and get corresponding transcripts
                                    HashMap<String, Object> hmProteins = new HashMap<>();
                                    for(DataApp.RankedListEntry entry : lstRL)
                                        hmProteins.put(entry.id, null);
                                    HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getProteinTransLengths(hmProteins, hmFilterTrans);

                                    // generate the features files
                                    subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                    HashMap<String, Object> hmProteinTrans = new HashMap<>();
                                    for(HashMap<String, Integer> hm : hmLengths.values()) {
                                        for(String trans : hm.keySet())
                                            hmProteinTrans.put(trans, null);
                                    }
                                    project.data.genProteinFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmProteinTrans);
                                    if(Files.exists(pathFeatures2)) {
                                        subTab.appendLogLine("Generating length file...", logfilepath);

                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // get length list - Warning: must be in same order as lstRL
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            HashMap<String, Integer> hmPTLengths = hmLengths.get(entry.id);
                                            int len = 0;
                                            int cnt = 0;
                                            for(int translen : hmPTLengths.values()) {
                                                len += translen;
                                                cnt++;
                                            }
                                            double gl = 0.0;
                                            if(cnt > 0)
                                                gl = len / cnt;
                                            lstLength.add((int)gl);
                                        }
                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                    }
                                }
                            }
                            break;
                        case FROMFILE:
                            subTab.appendLogLine("Loading protein ranked list from file...", logfilepath);
                            ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile2);
                            if(!lstRanked.isEmpty()) {
                                // get list of project proteins
                                HashMap<String, Object> hmFilterProteins = project.data.getResultsProteins();

                                ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                                ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                                for(DataApp.RankedListEntry entry : lstRanked) {
                                    if(hmFilterProteins.containsKey(entry.id))
                                        lstRL.add(entry);
                                    else
                                        lstRLBad.add(entry);
                                }
                                if(!lstRLBad.isEmpty()) {
                                    logger.logInfo("WARNING: Ranked list contains " + lstRLBad.size() + " proteins that are not part of the project.");
                                }
                                if(!lstRL.isEmpty()) {
                                    // create transcripts filter
                                    HashMap<String, Object> hmTrans = new HashMap<>();
                                    ArrayList<String> lstTrans;
                                    String gene;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        lstTrans = project.data.getProteinTrans(entry.id, hmFilterTrans);
                                        for(String trans : lstTrans) {
                                            if(hmFilterTrans.containsKey(trans))
                                                hmTrans.put(trans, null);
                                        }
                                    }

                                    // generate the features files
                                    project.data.genProteinFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmTrans);
                                    if(Files.exists(pathFeatures2)) {
                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // make a hash map of proteins and get corresponding transcripts
                                        HashMap<String, Object> hmProteins = new HashMap<>();
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            hmProteins.put(entry.id, null);

                                        // get length list - Warning: must be in same order as lstBkgnd
                                        HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getProteinTransLengths(hmProteins, hmFilterTrans);
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            HashMap<String, Integer> hmPTLengths = hmLengths.get(entry.id);
                                            int len = 0;
                                            int cnt = 0;
                                            for(int translen : hmPTLengths.values()) {
                                                len += translen;
                                                cnt++;
                                            }
                                            double gl = 0.0;
                                            if(cnt > 0)
                                                gl = len / cnt;
                                            lstLength.add((int)gl);
                                        }

                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                    }
                                }
                                else {
                                    subTab.appendLogLine("There were no matching proteins found in the project", logfilepath);
                                }
                            }
                            break;
                    }
                    break;
                }
                break;
                
            case TRANS:
                
                if(!Files.exists(Paths.get(project.data.getTranscriptMatrixFilepath())) || !Files.exists(Paths.get(project.data.getTranscriptMatrixRawFilepath()))){
                    if(project.data.genExpressionRawFile(gseaParams.dataType, false) && project.data.genExpressionFile(gseaParams.dataType, false));
                }
                
                switch(gseaParams.rankedList1) {
                    case DEA:
                        subTab.appendLogLine("Loading transcript DEA results list...", logfilepath);
                        DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.TRANS);
                        DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.TRANS, "", deaParams.sigValue, deaParams.FCValue);
                        if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                            // get ranked list: id and value
                            ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                            if(lstRL != null && !lstRL.isEmpty()) {
                                // generate the features files
                                subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                project.data.genTransFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmFilterTrans);
                                if(Files.exists(pathFeatures1)) {
                                    subTab.appendLogLine("Generating length file...", logfilepath);

                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // get length list - Warning: must be in same order as lstRL
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL)
                                        lstLength.add(project.data.getTransLength(entry.id));
                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                        }
                        break;
                    case FROMFILE:
                        subTab.appendLogLine("Loading transcripts ranked list from file: ", logfilepath);
                        ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile1);
                        if(!lstRanked.isEmpty()) {
                            ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                            ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                            for(DataApp.RankedListEntry entry : lstRanked) {
                                if(hmFilterTrans.containsKey(entry.id))
                                    lstRL.add(entry);
                                else
                                    lstRLBad.add(entry);
                            }
                            if(!lstRLBad.isEmpty())
                                subTab.appendLogLine("WARNING: Ranked list contains " + lstRLBad.size() + " transcripts that are not part of the project.", logfilepath);
                            if(!lstRL.isEmpty()) {
                                subTab.appendLogLine("Generating annotation features file...", logfilepath);

                                // create transcripts filter
                                HashMap<String, Object> hmTrans = new HashMap<>();
                                for(DataApp.RankedListEntry entry : lstRL)
                                    hmTrans.put(entry.id, null);
                                project.data.genTransFeatures(pathFeatures1.toString(), gseaParams.hmFeatures, hmTrans);
                                if(Files.exists(pathFeatures1)) {
                                    subTab.appendLogLine("Generating length file...", logfilepath);

                                    // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                    double min = Double.MAX_VALUE;
                                    boolean hasZero = false;
                                    for(DataApp.RankedListEntry entry : lstRL) {
                                        if(entry.value != 0.0)
                                            min = Math.min(min, entry.value);
                                        else
                                            hasZero = true;
                                    }
                                    if(hasZero) {
                                        System.out.println("hasZero: " + hasZero + ", min: " + min);
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            entry.value += min;
                                    }

                                    // get length list - Warning: must be in same order as lstRL
                                    ArrayList<Integer> lstLength = new ArrayList<>();
                                    for(DataApp.RankedListEntry entry : lstRL)
                                        lstLength.add(project.data.getTransLength(entry.id));
                                    result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 1);
                                }
                            }
                        }
                        break;
                }
                if(gseaParams.useMulti.equals("TRUE")){
                    switch(gseaParams.rankedList2) {
                        case DEA:
                            subTab.appendLogLine("Loading transcript DEA results list...", logfilepath);
                            DlgDEAnalysis.Params deaParams = deaData.getDEAParams(DataType.TRANS);
                            DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.TRANS, "", deaParams.sigValue, deaParams.FCValue);
                            if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                                // get ranked list: id and value
                                ArrayList<DataApp.RankedListEntry> lstRL = deaResults.getRankedList();
                                if(lstRL != null && !lstRL.isEmpty()) {
                                    // generate the features files
                                    subTab.appendLogLine("Generating annotation features file...", logfilepath);
                                    project.data.genTransFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmFilterTrans);
                                    if(Files.exists(pathFeatures2)) {
                                        subTab.appendLogLine("Generating length file...", logfilepath);

                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // get length list - Warning: must be in same order as lstRL
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            lstLength.add(project.data.getTransLength(entry.id));
                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                        }
                                    }
                                }
                                break;
                        case FROMFILE:
                            subTab.appendLogLine("Loading transcripts ranked list from file: ", logfilepath);
                            ArrayList<DataApp.RankedListEntry> lstRanked = Utils.loadRankedList(gseaParams.fromFile2);
                            if(!lstRanked.isEmpty()) {
                                ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                                ArrayList<DataApp.RankedListEntry> lstRLBad = new ArrayList<>();
                                for(DataApp.RankedListEntry entry : lstRanked) {
                                    if(hmFilterTrans.containsKey(entry.id))
                                        lstRL.add(entry);
                                    else
                                        lstRLBad.add(entry);
                                }
                                if(!lstRLBad.isEmpty())
                                    subTab.appendLogLine("WARNING: Ranked list contains " + lstRLBad.size() + " transcripts that are not part of the project.", logfilepath);
                                if(!lstRL.isEmpty()) {
                                    subTab.appendLogLine("Generating annotation features file...", logfilepath);

                                    // create transcripts filter
                                    HashMap<String, Object> hmTrans = new HashMap<>();
                                    for(DataApp.RankedListEntry entry : lstRL)
                                        hmTrans.put(entry.id, null);
                                    project.data.genTransFeatures(pathFeatures2.toString(), gseaParams.hmFeatures, hmTrans);
                                    if(Files.exists(pathFeatures2)) {
                                        subTab.appendLogLine("Generating length file...", logfilepath);

                                        // GOglm can not handle zero values, shift all by smallest value if any 0s found
                                        double min = Double.MAX_VALUE;
                                        boolean hasZero = false;
                                        for(DataApp.RankedListEntry entry : lstRL) {
                                            if(entry.value != 0.0)
                                                min = Math.min(min, entry.value);
                                            else
                                                hasZero = true;
                                        }
                                        if(hasZero) {
                                            System.out.println("hasZero: " + hasZero + ", min: " + min);
                                            for(DataApp.RankedListEntry entry : lstRL)
                                                entry.value += min;
                                        }

                                        // get length list - Warning: must be in same order as lstRL
                                        ArrayList<Integer> lstLength = new ArrayList<>();
                                        for(DataApp.RankedListEntry entry : lstRL)
                                            lstLength.add(project.data.getTransLength(entry.id));
                                        result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, 2);
                                    }
                                }
                            }
                        break;
                    }
                    break;
                }
            break;
        }
        return result;
    }

    // Enriched GSEA chart
    public ArrayList<ArrayList<DataGSEA.GSEAResultsData>> getGSEAChartData(String type, String featureId, double sigValue) {
        HashMap<String, ArrayList<DataGSEA.GSEAResultsData>> hmFeatures = new HashMap<>();
        ArrayList<ArrayList<DataGSEA.GSEAResultsData>> lstData = new ArrayList<>();

        // get GSEA results and sort list by adjusted p-value
        ArrayList<DataGSEA.GSEAResultsData> lstResults = getGSEAResultsData(type, featureId, sigValue);
        Collections.sort(lstResults);

        // process GSEA results
        ArrayList<DataGSEA.GSEAResultsData> lst;
        for(DataGSEA.GSEAResultsData frd: lstResults) {
            // all genes if select by list
            if(hmFeatures.containsKey(frd.category))
                lst = hmFeatures.get(frd.category);
            else {
                lst = new ArrayList<>();
                hmFeatures.put(frd.category, lst);
            }
            lst.add(frd);
        }

        // sort features alphabetically and save in final list
        ArrayList<String> lstFeatures = new ArrayList<>();
        lstFeatures.addAll(hmFeatures.keySet());
        Collections.sort(lstFeatures, String.CASE_INSENSITIVE_ORDER);
        for(String feature : lstFeatures)
            lstData.add(hmFeatures.get(feature));
        return lstData;
    }

    public ArrayList<ArrayList<DataGSEA.GSEAResultsData>> getGSEAEnrichedChartData(String type, String featureId, double sigValue) {
        HashMap<String, ArrayList<DataGSEA.GSEAResultsData>> hmFeatures = new HashMap<>();
        ArrayList<ArrayList<DataGSEA.GSEAResultsData>> lstData = new ArrayList<>();

        // get GSEA results and sort list by adjusted p-value
        ArrayList<DataGSEA.GSEAResultsData> lstResults = getGSEAResultsData(type, featureId, sigValue);
        Collections.sort(lstResults);

        // process GSEA results
        ArrayList<DataGSEA.GSEAResultsData> lst;
        for(DataGSEA.GSEAResultsData frd: lstResults) {
            // only interested in significant features
            if(frd.significant) {
                if(hmFeatures.containsKey(frd.category))
                    lst = hmFeatures.get(frd.category);
                else {
                    lst = new ArrayList<>();
                    hmFeatures.put(frd.category, lst);
                }
                lst.add(frd);
            }
        }

        // sort features alphabetically and save in final list
        ArrayList<String> lstFeatures = new ArrayList<>();
        lstFeatures.addAll(hmFeatures.keySet());
        Collections.sort(lstFeatures, String.CASE_INSENSITIVE_ORDER);
        for(String feature : lstFeatures)
            lstData.add(hmFeatures.get(feature));
        return lstData;
    }


    // GSEA ranked list
    public boolean writeFiles(String featureId, String dataType,  ArrayList<DataApp.RankedListEntry> lstRL, ArrayList<Integer> lstLength, int number) {
        boolean result = false;
        
        String filepath = Paths.get(getGSEAFolder(), (dataType + "_" + DataProject.RANKEDLIST_LENGTHS + "." + number + "." + featureId + DataApp.TSV_EXT)).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            int idx = 0;
            for(DataApp.RankedListEntry entry : lstRL)
                writer.write(entry.id + "\t" + lstLength.get(idx++) + "\n");
            result = true;
        }
        catch (Exception e) {
            System.out.println("Enrichment analysis ranked list length file Code exception: " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }

        if(result) {
            result = false;
            filepath = getGSEARankedListFilepath(dataType, featureId, number);
            writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                int idx = 0;
                for(DataApp.RankedListEntry entry : lstRL)
                    writer.write(entry.id + "\t" + entry.value + "\n");
                result = true;
            }
            catch (Exception e) {
                System.out.println("Enrichment analysis ranked list values file Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    
    public DataGSEA.EnrichedFeaturesData getGSEAEnrichedFeaturesData(String dataType, String id, double sigValue) {
        ArrayList<String> lstMembers = new ArrayList<>();
        ArrayList<BitSet> lstFeatureMembers = new ArrayList<>();
        ArrayList<DataGSEA.GSEAStatsData> lstStats = new ArrayList<>();
        ArrayList<GSEAResultsData> lstResults = getGSEAResultsData(dataType, id, sigValue);
        DataGSEA.EnrichedFeaturesData data = new EnrichedFeaturesData(lstStats, lstMembers, lstFeatureMembers);
        String filepathFeatures1 = getGSEAFeaturesFilepath(id, dataType, 1);
        String filepathFeatures2 = getGSEAFeaturesFilepath(id, dataType, 2);

        // get list of terms for members - must have been previously generated to run the analysis
        HashMap<String, Integer> hmItems = new HashMap<>();
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI1 = project.data.loadFeatureItemsInfo(filepathFeatures1);
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI2 = project.data.loadFeatureItemsInfo(filepathFeatures2);

        //get min 0.0 value
        double min = Double.MAX_VALUE;
        for(GSEAResultsData result : lstResults) {
            if(result.overPValue != 0.0 && result.overPValue2 != 0.0){
                if(Math.min(result.overPValue, result.overPValue2) < min)
                    min = Math.min(result.overPValue, result.overPValue2);
            }else if(result.overPValue != 0.0){
                if(result.overPValue < min)
                    min = result.overPValue;
            }else{
                if(result.overPValue2 < min)
                    min = result.overPValue2;
            }
        }
        
        // process all terms
        for(GSEAResultsData result : lstResults) {
            // check if term is enriched
            if(result.significant) {
                // add members to list to track bitset location
                int geneNumber = 0;
                ArrayList<String> lstItems = null;
                if(hmFI1.containsKey(result.feature) && hmFI2.containsKey(result.feature)) {
                    if(hmFI2.get(result.feature).items.size() > hmFI1.get(result.feature).items.size())
                        lstItems = hmFI2.get(result.feature).items;
                    else
                        lstItems = hmFI1.get(result.feature).items;
                }else if(hmFI2.containsKey(result.feature))
                    lstItems = hmFI2.get(result.feature).items;
                else
                    lstItems = hmFI1.get(result.feature).items;
                if(lstItems != null){
                    for(String item : lstItems) {
                        if (!hmItems.containsKey(item)) {
                            hmItems.put(item, lstMembers.size());
                            lstMembers.add(item);
                        }
                    }
                    geneNumber = lstItems.size();
                }
                // generate bitset for this feature
                BitSet bs = new BitSet(Math.max(hmFI1.size(), hmFI2.size()));
                lstFeatureMembers.add(bs);
                for(String item : lstItems)
                    bs.set(hmItems.get(item));

                //proportion in pie chart graph
                double prop = 0;
                if((result.overPValue == 0.0 && result.overPValue2 == 0.0) || (result.overPValue == 1.0 && result.overPValue2 == 1.0))
                    prop = 0.5;
                else if ((result.overPValue == 0.0 || result.overPValue2 == 0.0)){
                    // if anyone is 0.0 -> assign minimum value to calc prop
                    if(result.overPValue == 0.0)
                        prop = -Math.log10(min) / (- Math.log10(min) - Math.log10(result.overPValue2));
                    else
                        prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(min));
                }else
                    prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(result.overPValue2));

                // feature, description, cat, overPV, fdr, String sig
                lstStats.add(new GSEAStatsData(result.feature, result.description, result.category,
                        result.overPValue, result.adjPValue, result.significant? "YES" : "NO", result.md, result.overPValue2, result.adjPValue2, prop, geneNumber));
            }
        }
        return data;
    }
    
    //return enriched features but not 0.0
    public DataGSEA.EnrichedFeaturesData getGSEAEnrichedFeaturesDataCluster(String dataType, String id, double sigValue) {
        ArrayList<String> lstMembers = new ArrayList<>();
        ArrayList<BitSet> lstFeatureMembers = new ArrayList<>();
        ArrayList<DataGSEA.GSEAStatsData> lstStats = new ArrayList<>();
        ArrayList<GSEAResultsData> lstResults = getGSEAResultsData(dataType, id, sigValue);
        DataGSEA.EnrichedFeaturesData data = new EnrichedFeaturesData(lstStats, lstMembers, lstFeatureMembers);
        String filepathFeatures1 = getGSEAFeaturesFilepath(id, dataType, 1);
        String filepathFeatures2 = getGSEAFeaturesFilepath(id, dataType, 2);

        // get list of terms for members - must have been previously generated to run the analysis
        HashMap<String, Integer> hmItems = new HashMap<>();
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI1 = project.data.loadFeatureItemsInfo(filepathFeatures1);
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI2 = project.data.loadFeatureItemsInfo(filepathFeatures2);

        //get min 0.0 value
        double min = Double.MAX_VALUE;
        for(GSEAResultsData result : lstResults) {
            if(result.overPValue != 0.0 && result.overPValue2 != 0.0){
                if(Math.min(result.overPValue, result.overPValue2) < min)
                    min = Math.min(result.overPValue, result.overPValue2);
            }else if(result.overPValue != 0.0){
                if(result.overPValue < min)
                    min = result.overPValue;
            }else{
                if(result.overPValue2 < min)
                    min = result.overPValue2;
            }
        }
        
        //Sort by pvalue1 - small-big
        Collections.sort(lstResults, GSEAResultsDataOverPValue1);
        
        // process all terms
        int cont = 0;
        for(GSEAResultsData result : lstResults) {
            if(cont == 25)
                break;
            // check if term is enriched
            if(result.significant){
                // add members to list to track bitset location
                int geneNumber = 0;
                ArrayList<String> lstItems = null;
                if(hmFI1.containsKey(result.feature) && hmFI2.containsKey(result.feature)) {
                    if(hmFI2.get(result.feature).items.size() > hmFI1.get(result.feature).items.size())
                        lstItems = hmFI2.get(result.feature).items;
                    else
                        lstItems = hmFI1.get(result.feature).items;
                }else if(hmFI2.containsKey(result.feature))
                    lstItems = hmFI2.get(result.feature).items;
                else
                    lstItems = hmFI1.get(result.feature).items;
                if(lstItems != null){
                    for(String item : lstItems) {
                        if (!hmItems.containsKey(item)) {
                            hmItems.put(item, lstMembers.size());
                            lstMembers.add(item);
                        }
                    }
                    geneNumber = lstItems.size();
                }
                // generate bitset for this feature
                BitSet bs = new BitSet(Math.max(hmFI1.size(), hmFI2.size()));
                lstFeatureMembers.add(bs);
                for(String item : lstItems)
                    bs.set(hmItems.get(item));

                //proportion in pie chart graph
                double prop = 0;
                if((result.overPValue == 0.0 && result.overPValue2 == 0.0) || (result.overPValue == 1.0 && result.overPValue2 == 1.0))
                    prop = 0.5;
                else if ((result.overPValue == 0.0 || result.overPValue2 == 0.0)){
                    // if anyone is 0.0 -> assign minimum value to calc prop
                    if(result.overPValue == 0.0)
                        prop = -Math.log10(min) / (- Math.log10(min) - Math.log10(result.overPValue2));
                    else
                        prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(min));
                }else
                    prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(result.overPValue2));

                // feature, description, cat, overPV, fdr, String sig
                lstStats.add(new GSEAStatsData(result.feature, result.description, result.category,
                        result.overPValue, result.adjPValue, result.significant? "YES" : "NO", result.md, result.overPValue2, result.adjPValue2, prop, geneNumber));
                cont++;
            }
        }
        
        //Sort by pvalue1 - small-big
        Collections.sort(lstResults, GSEAResultsDataOverPValue2);
        
        // process all terms
        cont = 0;
        for(GSEAResultsData result : lstResults) {
            if(cont == 25)
                break;
            // check if term is enriched
            if(result.significant){
                // add members to list to track bitset location
                int geneNumber = 0;
                ArrayList<String> lstItems = null;
                if(hmFI1.containsKey(result.feature) && hmFI2.containsKey(result.feature)) {
                    if(hmFI2.get(result.feature).items.size() > hmFI1.get(result.feature).items.size())
                        lstItems = hmFI2.get(result.feature).items;
                    else
                        lstItems = hmFI1.get(result.feature).items;
                }else if(hmFI2.containsKey(result.feature))
                    lstItems = hmFI2.get(result.feature).items;
                else
                    lstItems = hmFI1.get(result.feature).items;
                if(lstItems != null){
                    for(String item : lstItems) {
                        if (!hmItems.containsKey(item)) {
                            hmItems.put(item, lstMembers.size());
                            lstMembers.add(item);
                        }
                    }
                    geneNumber = lstItems.size();
                }
                // generate bitset for this feature
                BitSet bs = new BitSet(Math.max(hmFI1.size(), hmFI2.size()));
                lstFeatureMembers.add(bs);
                for(String item : lstItems)
                    bs.set(hmItems.get(item));

                //proportion in pie chart graph
                double prop = 0;
                if((result.overPValue == 0.0 && result.overPValue2 == 0.0) || (result.overPValue == 1.0 && result.overPValue2 == 1.0))
                    prop = 0.5;
                else if ((result.overPValue == 0.0 || result.overPValue2 == 0.0)){
                    // if anyone is 0.0 -> assign minimum value to calc prop
                    if(result.overPValue == 0.0)
                        prop = -Math.log10(min) / (- Math.log10(min) - Math.log10(result.overPValue2));
                    else
                        prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(min));
                }else
                    prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(result.overPValue2));

                // feature, description, cat, overPV, fdr, String sig
                lstStats.add(new GSEAStatsData(result.feature, result.description, result.category,
                        result.overPValue, result.adjPValue, result.significant? "YES" : "NO", result.md, result.overPValue2, result.adjPValue2, prop, geneNumber));
                cont++;
            }
        }
        
        return data;
    }
    
    //return data by ArrayList<String>
    public DataGSEA.EnrichedFeaturesData getGSEAFeaturesData(String dataType, String id, double sigValue, ArrayList<String> lstFeatures) {
        ArrayList<String> lstMembers = new ArrayList<>();
        ArrayList<BitSet> lstFeatureMembers = new ArrayList<>();
        ArrayList<DataGSEA.GSEAStatsData> lstStats = new ArrayList<>();
        ArrayList<GSEAResultsData> lstResults = getGSEAResultsData(dataType, id, sigValue);
        
        DataGSEA.EnrichedFeaturesData data = new EnrichedFeaturesData(lstStats, lstMembers, lstFeatureMembers);
        String filepathFeatures1 = getGSEAFeaturesFilepath(id, dataType, 1);
        String filepathFeatures2 = getGSEAFeaturesFilepath(id, dataType, 2);

        // get list of terms for members - must have been previously generated to run the analysis
        HashMap<String, Integer> hmItems = new HashMap<>();
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI1 = project.data.loadFeatureItemsInfo(filepathFeatures1);
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI2 = project.data.loadFeatureItemsInfo(filepathFeatures2);

        //get min 0.0 value
        double min = Double.MAX_VALUE;
        for(GSEAResultsData result : lstResults) {
            if(result.overPValue != 0.0 && result.overPValue2 != 0.0){
                if(Math.min(result.overPValue, result.overPValue2) < min)
                    min = Math.min(result.overPValue, result.overPValue2);
            }else if(result.overPValue != 0.0){
                if(result.overPValue < min)
                    min = result.overPValue;
            }else{
                if(result.overPValue2 < min)
                    min = result.overPValue2;
            }
        }
        
        // process all terms
        for(GSEAResultsData result : lstResults) {
                //check if in list
                if(!lstFeatures.contains(result.feature))
                    continue;
                // add members to list to track bitset location
                int geneNumber = 0;
                ArrayList<String> lstItems = null;
                if(hmFI1.containsKey(result.feature) && hmFI2.containsKey(result.feature)) {
                    if(hmFI2.get(result.feature).items.size() > hmFI1.get(result.feature).items.size())
                        lstItems = hmFI2.get(result.feature).items;
                    else
                        lstItems = hmFI1.get(result.feature).items;
                }else if(hmFI2.containsKey(result.feature))
                    lstItems = hmFI2.get(result.feature).items;
                else
                    lstItems = hmFI1.get(result.feature).items;
                if(lstItems != null){
                    for(String item : lstItems) {
                        if (!hmItems.containsKey(item)) {
                            hmItems.put(item, lstMembers.size());
                            lstMembers.add(item);
                        }
                    }
                    geneNumber = lstItems.size();
                }
                // generate bitset for this feature
                BitSet bs = new BitSet(Math.max(hmFI1.size(), hmFI2.size()));
                lstFeatureMembers.add(bs);
                for(String item : lstItems)
                    bs.set(hmItems.get(item));

                //proportion in pie chart graph
                double prop = 0;
                if((result.overPValue == 0.0 && result.overPValue2 == 0.0) || (result.overPValue == 1.0 && result.overPValue2 == 1.0))
                    prop = 0.5;
                else if ((result.overPValue == 0.0 || result.overPValue2 == 0.0)){
                    // if anyone is 0.0 -> assign minimum value to calc prop
                    if(result.overPValue == 0.0)
                        prop = -Math.log10(min) / (- Math.log10(min) - Math.log10(result.overPValue2));
                    else
                        prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(min));
                }else
                    prop = -Math.log10(result.overPValue) / (- Math.log10(result.overPValue) - Math.log10(result.overPValue2));

                // feature, description, cat, overPV, fdr, String sig
                lstStats.add(new GSEAStatsData(result.feature, result.description, result.category,
                        result.overPValue, result.adjPValue, result.significant? "YES" : "NO", result.md, result.overPValue2, result.adjPValue2, prop, geneNumber));
        }
        return data;
    }

    // returns the actual GSEA results returned by the R script
    // caller needs to deal with calculating Significant using calls in GSEAResults
    // also you have to indicate if multidimensional method are running and overPValue and AdjPValue or null
    public ArrayList<GSEAResultsData> getGSEAResultsData(String type, String featureId, double sigValue) {
        ArrayList<GSEAResultsData> lstResults = new ArrayList<>();
        try {
            String filepath = getGSEAResultsFilepath(type, featureId);
            List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
            int lnum = 1;
            String[] fields;
            for(String line : lines) {
                if(lnum > 1) {
                    // R is sticking spaces between the tabs
                    fields = line.split("\t");
                    for(int i = 0; i < fields.length; i++)
                        fields[i] = fields[i].trim();
                    // not md
                    if(fields.length == 7) {
                        lstResults.add(new GSEAResultsData(fields[5], fields[0], fields[1], fields[2], Double.parseDouble(fields[3]), Double.parseDouble(fields[4]), sigValue, false, 1.0, 1.0, Integer.parseInt(fields[6])));
                    // multi
                    }else if(fields.length == 9) {
                        lstResults.add(new GSEAResultsData(fields[7], fields[0], fields[1], fields[2], Double.parseDouble(fields[3]), Double.parseDouble(fields[5]), sigValue, true, Double.parseDouble(fields[4]), Double.parseDouble(fields[6]), Integer.parseInt(fields[8])));
                    }
                    else {
                        logger.logError("Invalid number of columns, " + fields.length + ", in GSEA results data, line " + lnum + ".");
                        break;
                    }
                }
                lnum++;
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load GSEA " + type + " results data: " + e.getMessage());
        }
        return lstResults;
    }
    
    public Integer[] getGSEAItemsResults(String type, String featureId) {
        Integer[] res = null;
        try {
            String filepath = getGSEAResultsItemsFilepath(type, featureId);
            List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
            int lnum = 0;
            String[] fields;
            for(String line : lines) {
                fields = line.split("\t");
                Integer[] aux = new Integer[fields.length];
                for(int i = 0; i < fields.length; i++)
                    aux[i] = Integer.parseInt(fields[i].trim());
                res = aux;
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load GSEA " + type + " items data: " + e.getMessage());
        }
        return res;
    }

    public ObservableList<GSEASelectionResults> getGSEASelectionResults(String type, String featureId, double sigValue) {
        ObservableList<GSEASelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<GSEAResultsData> lstData = getGSEAResultsData(type, featureId, sigValue);
        for(GSEAResultsData data : lstData)
            lstResults.add(new GSEASelectionResults(false, DataType.valueOf(type), data));
        return lstResults;
    }

    //
    // Internal Functions
    //
    
    private boolean loadDIURankedList(DataType type, boolean extended, String featureId, Path pathFeatures, DlgGSEAnalysis.Params gseaParams, SubTabBase subTab, String logfilepath, int number) {
        boolean result = false;
        
        DlgDIUAnalysis.Params diuParams = DlgDIUAnalysis.Params.load(project.data.analysis.getDIUParamsFilepath(type), project);
        ArrayList<DataDIU.DIUResultsData> lstResults = project.data.analysis.getDIUResultsData(type, diuParams.sigValue);

        if(lstResults != null && !lstResults.isEmpty()) {
            // get ranked list: id and value
            HashMap<String, HashMap<String, Object>> hmGT = project.data.getResultsGeneTrans();
            HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = new HashMap<>();
            for(DataDIU.DIUResultsData dsrd : lstResults)
                hmFilterGeneTrans.put(dsrd.gene, hmGT.get(dsrd.gene));

            // generate the features files
            subTab.appendLogLine("Generating annotation features file...", logfilepath);

            // create DIU transcripts filter
            HashMap<String, Object> hmDIUTrans = new HashMap<>();
            for(HashMap<String, Object> hm : hmFilterGeneTrans.values()) {
                for(String trans : hm.keySet())
                    hmDIUTrans.put(trans, null);
            }
            if(gseaParams.customFile.isEmpty())
                project.data.genGeneFeatures(pathFeatures.toString(), gseaParams.hmFeatures, hmDIUTrans);
            else {
                ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();
                for(DataDIU.DIUResultsData drd : lstResults)
                    lstRL.add(new DataApp.RankedListEntry(drd.gene, drd.qValue));
                project.data.genCustomFeatures(pathFeatures.toString(), gseaParams.customFile, gseaParams.dataType, lstRL);
            }
            if(Files.exists(pathFeatures)) {
                subTab.appendLogLine("Generating length file...", logfilepath);
                ArrayList<DataApp.RankedListEntry> lstRL = new ArrayList<>();

                // GOglm can not handle zero values, shift all by smallest value if any 0s found
                double min = Double.MAX_VALUE;
                boolean hasZero = false;
                for(DataDIU.DIUResultsData dr1 : lstResults) {
                    if(dr1.qValue != 0.0)
                        min = Math.min(min, dr1.qValue);
                    else
                        hasZero = true;
                }
                if(!hasZero || min == Double.MAX_VALUE)
                    min = 0.0;
                System.out.println("hasZero: " + hasZero + ", min: " + min);
                for(DataDIU.DIUResultsData dr2 : lstResults)
                    lstRL.add(new DataApp.RankedListEntry(dr2.gene, dr2.qValue+min));

                // check if DIU extended and add genes with single isoforms and value of 1.0
                if(extended) {
                    for(String gene : hmGT.keySet()) {
                        if(!hmFilterGeneTrans.containsKey(gene)) {
                            lstRL.add(new DataApp.RankedListEntry(gene, 1.0));
                            hmFilterGeneTrans.put(gene, hmGT.get(gene));
                        }
                    }
                }
                Collections.sort(lstRL);

                // get length list - Warning: must be in same order as lstRL
                HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmFilterGeneTrans);
                ArrayList<Integer> lstLength = new ArrayList<>();
                for(DataApp.RankedListEntry entry : lstRL) {
                    HashMap<String, Integer> hmGTLengths = hmLengths.get(entry.id);
                    int len = 0;
                    int cnt = 0;
                    for(int translen : hmGTLengths.values()) {
                        len += translen;
                        cnt++;
                    }
                    double gl = 0.0;
                    if(cnt > 0)
                        gl = len / cnt;
                    lstLength.add((int)gl);
                }
                result = writeFiles(featureId, gseaParams.dataType.name().toLowerCase(), lstRL, lstLength, number);
            }
        }
        return result;
    }
    private ArrayList<String> getGSEAFilesList(String id, boolean rmvPrms) {
        ArrayList<String> lst = new ArrayList<>();
        File gseaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_GSEA).toString());
        FilenameFilter filter = (File dir, String name) -> {
            if(rmvPrms)
                return (name.endsWith(DataApp.PRM_EXT) || name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
            else
                return (name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
        };        
        File[] files = gseaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String fileId = name.substring(sidx, eidx);
                        if(fileId.equals(id))
                            lst.add(files[i].getPath());
                    }
                }
            }
        }
        return lst;
    }
    
    // Sort GSEA overPValue1
        Comparator<GSEAResultsData> GSEAResultsDataOverPValue1 = new Comparator<GSEAResultsData>() {
            @Override
            public int compare(GSEAResultsData g1, GSEAResultsData g2) {
                if(g1.overPValue < g2.overPValue) {
                    return -1;
                } else if (g1.overPValue > g2.overPValue) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        
        // Sort GSEA overPValue2
        Comparator<GSEAResultsData> GSEAResultsDataOverPValue2 = new Comparator<GSEAResultsData>() {
            @Override
            public int compare(GSEAResultsData g1, GSEAResultsData g2) {
                if(g1.overPValue2 < g2.overPValue2) {
                    return -1;
                } else if (g1.overPValue2 > g2.overPValue2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    
    //
    // Data Classes
    //

    // GSEA data for features
    public static class GSEAResultsData implements Comparable<DataGSEA.GSEAResultsData>{
        public String source;
        public String feature;
        public String description;
        public String category;
        public double overPValue;
        //public int numDEInCat;
        public int numInCat;
        public double adjPValue;
        public boolean significant;

        public boolean md;

        //if md
        public double overPValue2;
        public double adjPValue2;

        public GSEAResultsData(String source, String feature, String description, String category, double overPValue, double adjPValue, double sigValue, boolean md, double overPValue2, double adjPValue2, int numInCat) {
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.feature = feature;
            this.source = source;
            this.description = description;
            this.category = category;
            this.overPValue = Double.parseDouble(formatter.format(overPValue));
            this.adjPValue = Double.parseDouble(formatter.format(adjPValue));
            this.numInCat = numInCat;
            if(md){
                this.significant = overPValue < sigValue || overPValue < sigValue;
            }else {
                this.significant = adjPValue < sigValue;
            }

            this.md = md;

            if(this.md){
                this.overPValue2 = Double.parseDouble(formatter.format(overPValue2));
                this.adjPValue2 = Double.parseDouble(formatter.format(adjPValue2));
            }
        }

        @Override
        public int compareTo(GSEAResultsData data) {
            if(this.overPValue < data.overPValue) {
                return -1;
            } else if (this.overPValue > data.overPValue) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static class GSEAStatsData {
        public final SimpleStringProperty feature;
        public final SimpleStringProperty description;
        public final SimpleStringProperty cat;
        public final SimpleDoubleProperty overPV;
        public final SimpleDoubleProperty adjPValue;
        public final SimpleStringProperty significant;
        public final SimpleIntegerProperty geneNumber;

        public final SimpleBooleanProperty md;

        //if multidimensional
        public final SimpleDoubleProperty overPV2;
        public final SimpleDoubleProperty adjPValue2;
        public final SimpleDoubleProperty prop;

        // change test and background items counts - here we work with rankedlists!!!
        public GSEAStatsData(String feature, String description, String cat, double overPV, double adjPValue, String significant, boolean md, double overPV2, double adjPValue2, double prop, int geneNumber) {
            this.feature = new SimpleStringProperty(feature);
            String[] fields = description.split(" ");
            String new_desc = "";
//            if(fields.length>4){
//                for(int i = 0; i<fields.length;i++){
//                    if(i==fields.length-1)
//                        new_desc += fields[i];
//                    else if(i%3==0 && i!=0)
//                        new_desc += fields[i] + " </tspan><tspan> ";
//                    else
//                        new_desc += fields[i] + " ";
//                }
//                this.description = new SimpleStringProperty(new_desc);
//            }else{               
//                this.description = new SimpleStringProperty(description);
//            }
            this.description = new SimpleStringProperty(description);
            this.cat = new SimpleStringProperty(cat);
            this.overPV = new SimpleDoubleProperty(overPV);
            this.adjPValue = new SimpleDoubleProperty(adjPValue);
            this.significant = new SimpleStringProperty(significant);
            this.geneNumber = new SimpleIntegerProperty(geneNumber);
            this.md = new SimpleBooleanProperty(md);

            if(this.md.get()){
                this.overPV2 = new SimpleDoubleProperty(overPV2);
                this.adjPValue2 = new SimpleDoubleProperty(adjPValue2);
                this.prop = new SimpleDoubleProperty(prop);
            }else{
                this.overPV2 = null;
                this.adjPValue2 = null;
                this.prop = null;
            }
        }

        public String getFeature() { return feature.get(); }
        public String getDescription() { return description.get(); }
        public String getCategory() { return cat.get(); }
        public Double getOverPV() { return overPV.get(); }
        public Double getAdjustedPV() { return adjPValue.get(); }

        //these just works for case-control!!
        public Double getOverPV2() { return overPV2.get(); }
        public Double getAdjustedPV2() { return adjPValue2.get(); }
        public Integer geneNumber() { return geneNumber.get(); }
        public String getSignificant() { return significant.get(); }
    }

    public static class EnrichedFeaturesData {
        public ArrayList<GSEAStatsData> lstStats;    // features information - GSEA results
        public ArrayList<String> lstMembers;        // order of members in bitsets
        public ArrayList<BitSet> lstTermMembers;    // terms in same order as in stats
        public EnrichedFeaturesData(ArrayList<GSEAStatsData> lstStats,
                                    ArrayList<String> lstMembers, ArrayList<BitSet> lstTermMembers) {
            this.lstStats = lstStats;
            this.lstMembers = lstMembers;
            this.lstTermMembers = lstTermMembers;
        }
    }

    // specific analysis results
    public static class GSEASelectionResults extends DataApp.SelectionDataResults implements Comparable<GSEASelectionResults> {
        public final SimpleStringProperty featureId;
        public final SimpleStringProperty description;
        public final SimpleStringProperty feature;
        public final SimpleDoubleProperty overPValue;
        public final SimpleDoubleProperty adjPValue;
        public final SimpleStringProperty significant;
        // for md
        public final SimpleDoubleProperty overPValue2;
        public final SimpleDoubleProperty adjPValue2;
        
        public GSEASelectionResults(boolean selected, DataType dataType, GSEAResultsData data) {
            super(selected, dataType, "", "", "");
            this.featureId = new SimpleStringProperty(data.feature);
            this.description = new SimpleStringProperty(data.description);
            this.feature = new SimpleStringProperty(data.category);
            this.overPValue = new SimpleDoubleProperty(data.overPValue);
            this.adjPValue = new SimpleDoubleProperty(data.adjPValue);
            this.significant = new SimpleStringProperty(data.significant? "YES" : "NO");
            this.overPValue2 = new SimpleDoubleProperty(data.overPValue2);
            this.adjPValue2 = new SimpleDoubleProperty(data.adjPValue2);
        }
        
        public String getFeatureId() { return featureId.get(); }
        public String getDescription() { return description.get(); }
        public String getFeature() { return feature.get(); }
        public Double getOverPValue() { return overPValue.get(); }
        public Double getAdjustedPValue() { return adjPValue.get(); }
        public String getSignificant() { return significant.get(); }

        public Double getOverPValue2() { return overPValue2.get(); }
        public Double getAdjustedPValue2() { return adjPValue2.get(); }
        
        @Override
        public int compareTo(GSEASelectionResults td) {
            if(!feature.get().equals(td.feature.get()))
                return (feature.get().compareTo(td.feature.get()));
            else
                return (featureId.get().compareTo(td.featureId.get()));
            
            /* use if want to deafult to sort by pvalue instead
            if(adjPValue.get() != td.adjPValue.get())
                return ((adjPValue.get() > td.adjPValue.get())? 1 : -1);
            else
                return ((overPValue.get() > td.overPValue.get())? 1 : (overPValue.get() < td.overPValue.get())? -1 : 0); */
        }
    }
    
    public static class GSEADisplayData implements Comparable<GSEADisplayData>{
        public final SimpleStringProperty feature;
        public final SimpleStringProperty description;
        public final SimpleStringProperty category;
        public final SimpleDoubleProperty overPValue;
        public final SimpleDoubleProperty adjPValue;
        public final SimpleStringProperty significant;
        
        public GSEADisplayData(GSEAResultsData data, double sigValue) {
            this.feature = new SimpleStringProperty(data.feature);
            this.description = new SimpleStringProperty(data.description);
            this.category = new SimpleStringProperty(data.category);
            this.overPValue = new SimpleDoubleProperty(data.overPValue);
            this.adjPValue = new SimpleDoubleProperty(data.adjPValue);
            this.significant = new SimpleStringProperty(data.adjPValue < sigValue? "YES" : "NO");
        }
        public GSEADisplayData(String feature, String description, String category,
                double overPValue, double adjPValue, String significant) {
            this.feature = new SimpleStringProperty(feature);
            this.description = new SimpleStringProperty(description);
            this.category = new SimpleStringProperty(category);
            this.overPValue = new SimpleDoubleProperty(overPValue);
            this.adjPValue = new SimpleDoubleProperty(adjPValue);
            this.significant = new SimpleStringProperty(significant);
        }
        public String getFeature() { return feature.get(); }
        public String getDescription() { return description.get(); }
        public String getCategory() { return category.get(); }
        public Double getOverPValue() { return overPValue.get(); }
        public Double getAdjustedPValue() { return adjPValue.get(); }
        public String getSignificant() { return significant.get(); }
        @Override
        public int compareTo(GSEADisplayData td) {
            return (feature.get().compareTo(td.feature.get()));
        }
    }
}
