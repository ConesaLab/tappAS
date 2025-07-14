/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tappas.DataApp.DataType;
import tappas.DataApp.SelectionDataResults;

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

public class DataFEA extends AppObject {
    public String getFEAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FEA).toString(); }
    public String getFEAParamsFilepath(String id) { return Paths.get(getFEAFolder(), DataApp.PRM_FEA_NAME + id + DataApp.PRM_EXT).toString(); }
    public String getFEAClusterParamsFilepath(String id) { return Paths.get(getFEAFolder(), DataApp.PRM_FEACLUSTER_NAME + id + DataApp.PRM_EXT).toString(); }
    public String getFEAResultsFilepath(String dataType, String id) { 
        return (Paths.get(getFEAFolder(), DataApp.RESULTS_NAME + dataType.toLowerCase() + "." + id + DataApp.RESULTS_EXT).toString());
    }
    public String getFEAResultsClusterFilepath(String cdataType, String id) { 
        return (Paths.get(getFEAFolder(), DataApp.RESULTSCLUSTER_NAME + cdataType.toLowerCase() + "." + id + DataApp.RESULTS_EXT).toString());
    }
    public String getFEATestListFilepath(String dataType, String id) { 
        return (Paths.get(getFEAFolder(), (dataType.toLowerCase() + "_" + DataProject.LIST_TEST + "." + id + DataApp.TSV_EXT)).toString());
    }
    public String getFEABkgndListFilepath(String dataType, String id) { 
        return (Paths.get(getFEAFolder(), (dataType.toLowerCase() + "_" + DataProject.LIST_BKGND + "." + id + DataApp.TSV_EXT)).toString());
    }
    public String getFEALogFilepath(String id) { return Paths.get(getFEAFolder(), DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getFEAClusterLogFilepath(String id) { return Paths.get(getFEAFolder(), DataApp.CLUSTER_NAME + DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getFEAClusterDoneFilepath(String id) { return Paths.get(getFEAFolder(), DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID + id + DataApp.TEXT_EXT).toString(); }
    public String getFEAFeaturesFilepath(String id, String type) { return Paths.get(getFEAFolder(), type.toLowerCase() + "_" + DataApp.FEATURES_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    
    DataDEA deaData;
    
    public DataFEA(Project project) {
        super(project, null);
        deaData = new DataDEA(project);
    }
    public void initialize() {
        clearData();
    }
    public void clearData() {
    }
    public boolean hasFEAData(String dataType, String id) { 
        return (Files.exists(Paths.get(getFEAFolder(), DataApp.DONE_NAME + dataType.toLowerCase() + "." + id + DataApp.TEXT_EXT)));
    }
    public boolean hasFEAClusterData(String id) { 
        return (Files.exists(Paths.get(getFEAFolder(), DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID + id + DataApp.TEXT_EXT)));
    }
    public boolean hasAnyFEAData() {
        File feaFolder = new File(Paths.get(getFEAFolder()).toString());
        FilenameFilter filter = (File dir, String name) -> (name.startsWith(DataApp.DONE_NAME) && name.endsWith(DataApp.TEXT_EXT));        
        File[] files = feaFolder.listFiles(filter);
        return (files != null && files.length > 0);
    }
    public void setFEAParams(HashMap<String, String> hmp, String id) { 
        if(hmp != null)
            Utils.saveParams(hmp, getFEAParamsFilepath(id));
    } 
    public HashMap<String, String> getFEAParams(String id) {
        HashMap<String, String> hmp = new HashMap<>();
        Utils.loadParams(hmp, getFEAParamsFilepath(id));
        return hmp;
    }
    public void setFEAClusterParams(HashMap<String, String> hmp, String id) { 
        if(hmp != null)
            Utils.saveParams(hmp, getFEAClusterParamsFilepath(id));
    } 
    public HashMap<String, String> getFEAClusterParams(String id) {
        HashMap<String, String> hmp = new HashMap<>();
        Utils.loadParams(hmp, getFEAClusterParamsFilepath(id));
        return hmp;
    }
    public ArrayList<DataApp.EnumData> getFEAResultsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File feaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FEA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.DONE_NAME) && name.endsWith(DataApp.TEXT_EXT);        
        File[] files = feaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                // ignore cluster done files "done_cluster.*"
                if(!name.startsWith(DataApp.DONE_NAME + DataApp.CLUSTER_NAMEID)) {
                    int sidx = name.indexOf('.');
                    if(sidx != -1) {
                        int eidx = name.indexOf(".", ++sidx);
                        if(eidx != -1) {
                            String id = name.substring(sidx, eidx);
                            String feaName = id;
                            HashMap<String, String> hm = getFEAParams(id);
                            if(hm != null && hm.containsKey(DlgFEAnalysis.Params.NAME_PARAM))
                                feaName = hm.get(DlgFEAnalysis.Params.NAME_PARAM);
                            lst.add(new DataApp.EnumData(id, feaName));
                        }
                    }
                }
            }
        }
        return lst;
    }
    public ArrayList<DataApp.EnumData> getFEAParamsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File feaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FEA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.PRM_FEA_NAME) && name.endsWith(DataApp.PRM_EXT);        
        File[] files = feaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String feaName = id;
                        HashMap<String, String> hm = getFEAParams(id);
                        if(hm != null && hm.containsKey(DlgFEAnalysis.Params.NAME_PARAM))
                            feaName = hm.get(DlgFEAnalysis.Params.NAME_PARAM);
                        lst.add(new DataApp.EnumData(id, feaName));
                    }
                }
            }
        }
        return lst;
    }
    public void clearDataFEA(boolean rmvPrm) {
        clearData();
        removeAllFEAResultFiles(rmvPrm);
    }
    public void clearDataFEA(String id, boolean rmvPrm) {
        clearData();
        removeFEAFiles(id, rmvPrm);
    }
    public void removeFEAFiles(String id, boolean rmvPrms) {
        ArrayList<String> lstFiles = getFEAFilesList(id, rmvPrms);
        for(String filepath : lstFiles)
            Utils.removeFile(Paths.get(filepath));
    }
    public void removeFEAClusterFiles(String id) {
        Utils.removeFile(Paths.get(getFEAClusterDoneFilepath(id)));
        Utils.removeFile(Paths.get(getFEAClusterParamsFilepath(id)));
        Utils.removeFile(Paths.get(getFEAClusterLogFilepath(id)));
    }
    public void removeAllFEAResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FEA), rmvPrms);
    }    
    public boolean genFEAInputFiles(String featureId, DlgFEAnalysis.Params feaParams, SubTabBase subTab, String logfilepath) {
        boolean result = false;

        // generate lists and features files
        HashMap<String, Object> hmFilterTrans = project.data.getResultsTrans();
        HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = project.data.getResultsGeneTrans();
        Path pathFeatures = Paths.get(getFEAFeaturesFilepath(featureId, feaParams.dataType.name()));
        ArrayList<String> lstTest = new ArrayList<>();
        ArrayList<String> lstBkgnd = new ArrayList<>();
        switch(feaParams.dataType) {
            case GENE:
                // get list of test genes
                ArrayList<String> lstTestBad = new ArrayList<>();
                ArrayList<String> lstTestTmp = getGeneTestList(feaParams.testList, feaParams.testFilepath, subTab, logfilepath);
                ArrayList<String> lstBkgndBad = new ArrayList<>();
                ArrayList<String> lstBkgndTmp;
                if(!lstTestTmp.isEmpty()) {
                    for(String gene : lstTestTmp) {
                        if(hmFilterGeneTrans.containsKey(gene))
                            lstTest.add(gene);
                        else
                            lstTestBad.add(gene);
                    }
                    if(!lstTestBad.isEmpty())
                        logger.logInfo("Gene test list contains " + lstTestBad.size() + " genes that are not part of the project.");
                    if(!lstTest.isEmpty()) {
                        lstBkgndTmp = getGeneBkgndList(feaParams.bkgndList, feaParams.bkgndFilepath, subTab, logfilepath);
                        if(!lstBkgndTmp.isEmpty()) {
                            for(String gene : lstBkgndTmp) {
                                if(hmFilterGeneTrans.containsKey(gene))
                                    lstBkgnd.add(gene);
                                else
                                    lstBkgndBad.add(gene);
                            }
                            if(!lstBkgndBad.isEmpty())
                                logger.logInfo("Gene background list contains " + lstBkgndBad.size() + " genes that are not part of the project.");
                            if(!lstBkgnd.isEmpty()) {
                                int cnt = 0;
                                for(String gene : lstTest) {
                                    if(!lstBkgnd.contains(gene)) {
                                        cnt++;
                                        lstBkgnd.add(gene);
                                    }
                                }
                                if(cnt > 0)
                                    System.out.println("Added " + cnt + " test genes to background list.");
                            }
                        }
                    }
                }

                // check if we generated lists successfully
                if(!lstTest.isEmpty() && !lstBkgnd.isEmpty()) {
                    Collections.sort(lstTest);
                    Collections.sort(lstBkgnd);

                    // create gene transcripts filter
                    HashMap<String, Object> hmTrans = new HashMap<>();
                    HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
                    for(String gene : lstBkgnd) {
                        HashMap<String, Object> hm = hmFilterGeneTrans.get(gene);
                        hmGeneTrans.put(gene, hm);
                        for(String trans : hm.keySet())
                            hmTrans.put(trans, null);
                    }

                    // generate the features files
                    subTab.appendLogLine("Loading gene features.", logfilepath);
                    project.data.genGeneFeatures(pathFeatures.toString(), feaParams.hmFeatures, hmTrans);
                    if(Files.exists(pathFeatures)) {
                        // get length list - Warning: must be in same order as lstBkgnd
                        HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getGeneTransLength(hmGeneTrans);
                        ArrayList<Integer> lstLength = new ArrayList<>();
                        for(String gene : lstBkgnd) {
                            HashMap<String, Integer> hmGTLengths = hmLengths.get(gene);
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
                        result = writeFiles(featureId, feaParams.dataType.name().toLowerCase(), lstTest, lstBkgnd, lstLength);
                    }
                }
                break;
            case PROTEIN:
                // get list of proteins
                HashMap<String, Object> hmFilterProteins = project.data.getResultsProteins();

                // get list of test proteins
                lstTestBad = new ArrayList<>();
                lstTestTmp = getProteinTestList(feaParams.testList, feaParams.testFilepath, subTab, logfilepath);
                lstBkgndBad = new ArrayList<>();
                lstBkgndTmp = new ArrayList<>();
                if(!lstTestTmp.isEmpty()) {
                    for(String protein : lstTestTmp) {
                        if(hmFilterProteins.containsKey(protein)) {
                            if(!lstTest.contains(protein))
                                lstTest.add(protein);
                        }
                        else {
                            if(!lstTestBad.contains(protein))
                                lstTestBad.add(protein);
                        }
                    }
                    if(!lstTestBad.isEmpty())
                        logger.logInfo("Protein test list contains " + lstTestBad.size() + " proteins that are not part of the project.");
                    if(!lstTest.isEmpty()) {
                        lstBkgndTmp = getProteinBkgndList(feaParams.bkgndList, feaParams.bkgndFilepath, subTab, logfilepath);
                        if(!lstBkgndTmp.isEmpty()) {
                            for(String protein : lstBkgndTmp) {
                                if(hmFilterProteins.containsKey(protein)) {
                                    if(!lstBkgnd.contains(protein))
                                        lstBkgnd.add(protein);
                                }
                                else {
                                    if(!lstBkgndBad.contains(protein))
                                        lstBkgndBad.add(protein);
                                }
                            }
                            if(!lstBkgndBad.isEmpty())
                                logger.logInfo("Preteins background list contains " + lstBkgndBad.size() + " proteins that are not part of the project.");
                            if(!lstBkgnd.isEmpty()) {
                                int cnt = 0;
                                for(String protein : lstTest) {
                                    if(!lstBkgnd.contains(protein)) {
                                        cnt++;
                                        lstBkgnd.add(protein);
                                    }
                                }
                                if(cnt > 0)
                                    logger.logInfo("Added " + cnt + " test proteins to background list which were not initially included.");
                            }
                        }
                    }
                }
                // check if we generated lists successfully
                if(!lstTest.isEmpty() && !lstBkgnd.isEmpty()) {
                    Collections.sort(lstTest);
                    Collections.sort(lstBkgnd);

                    // create transcripts filter
                    HashMap<String, Object> hmTrans = new HashMap<>();
                    HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
                    ArrayList<String> lstTrans;
                    String gene;
                    for(String protein : lstBkgnd) {
                        lstTrans = project.data.getProteinTrans(protein, hmFilterTrans);
                        for(String trans : lstTrans) {
                            if(hmFilterTrans.containsKey(trans)) {
                                hmTrans.put(trans, null);
                                gene = project.data.getTransGene(trans);
                                if(!hmGeneTrans.containsKey(gene)) {
                                    HashMap<String, Object> hm = hmFilterGeneTrans.get(gene);
                                    hmGeneTrans.put(gene, hm);
                                }
                            }
                        }
                    }

                    // generate the features files
                    subTab.appendLogLine("Loading protein features.", logfilepath);
                    project.data.genProteinFeatures(pathFeatures.toString(), feaParams.hmFeatures, hmTrans);
                    if(Files.exists(pathFeatures)) {
                        // make a hash map of proteins and get corresponding transcripts
                        HashMap<String, Object> hmProteins = new HashMap<>();
                        for(String protein : lstBkgnd)
                            hmProteins.put(protein, null);

                        // get length list - Warning: must be in same order as lstBkgnd
                        HashMap<String, HashMap<String, Integer>> hmLengths = project.data.getProteinTransLengths(hmProteins, hmFilterTrans);
                        ArrayList<Integer> lstLength = new ArrayList<>();
                        for(String protein : lstBkgnd) {
                            HashMap<String, Integer> hmPTLengths = hmLengths.get(protein);
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

                        result = writeFiles(featureId, feaParams.dataType.name().toLowerCase(), lstTest, lstBkgnd, lstLength);
                    }
                }
                break;
            case TRANS:
                // get list of test genes
                lstTestBad = new ArrayList<>();
                lstTestTmp = getTransTestList(feaParams.testList, feaParams.testFilepath, subTab, logfilepath);
                lstBkgndBad = new ArrayList<>();
                lstBkgndTmp = new ArrayList<>();
                if(!lstTestTmp.isEmpty()) {
                    for(String trans : lstTestTmp) {
                        if(hmFilterTrans.containsKey(trans))
                            lstTest.add(trans);
                        else
                            lstTestBad.add(trans);
                    }
                    if(!lstTestBad.isEmpty())
                        logger.logInfo("Transcripts test list contains " + lstTestBad.size() + " transcripts that are not part of the project.");
                    if(!lstTest.isEmpty()) {
                        lstBkgndTmp = getTransBkgndList(feaParams.bkgndList, feaParams.bkgndFilepath, subTab, logfilepath);
                        if(!lstBkgndTmp.isEmpty()) {
                            for(String trans : lstBkgndTmp) {
                                if(hmFilterTrans.containsKey(trans))
                                    lstBkgnd.add(trans);
                                else
                                    lstBkgndBad.add(trans);
                            }
                            if(!lstBkgndBad.isEmpty())
                                logger.logInfo("Transcripts background list contains " + lstBkgndBad.size() + " transcripts that are not part of the project.");
                            if(!lstBkgnd.isEmpty()) {
                                int cnt = 0;
                                for(String trans : lstTest) {
                                    if(!lstBkgnd.contains(trans)) {
                                        cnt++;
                                        lstBkgnd.add(trans);
                                    }
                                }
                                if(cnt > 0)
                                    logger.logInfo("Added " + cnt + " test transcripts to background list which were not initially included.");
                            }
                        }
                    }
                }
                // check if we generated lists successfully
                if(!lstTest.isEmpty() && !lstBkgnd.isEmpty()) {
                    Collections.sort(lstTest);
                    Collections.sort(lstBkgnd);

                    // create transcripts filter
                    HashMap<String, Object> hmTrans = new HashMap<>();
                    HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
                    String gene;
                    for(String trans : lstBkgnd) {
                        hmTrans.put(trans, null);
                        gene = project.data.getTransGene(trans);
                        if(!hmGeneTrans.containsKey(gene)) {
                            HashMap<String, Object> hm = hmFilterGeneTrans.get(gene);
                            hmGeneTrans.put(gene, hm);
                        }
                    }

                    // generate the features files
                    subTab.appendLogLine("Loading transcript features.", logfilepath);
                    project.data.genTransFeatures(pathFeatures.toString(), feaParams.hmFeatures, hmTrans);
                    if(Files.exists(pathFeatures)) {
                        // get length list - Warning: must be in same order as lstBkgnd
                        ArrayList<Integer> lstLength = new ArrayList<>();
                        for(String trans : lstBkgnd)
                            lstLength.add(project.data.getTransLength(trans));
                        result = writeFiles(featureId, feaParams.dataType.name().toLowerCase(), lstTest, lstBkgnd, lstLength);
                    }
                }
                break;
        }
        return result;
    }
    // FEA test and background lists
    public boolean writeFiles(String featureId, String dataType, ArrayList<String> lstTest, ArrayList<String> lstBkgnd, ArrayList<Integer> lstLength) {
        boolean result = false;
        String filepath = Paths.get(getFEAFolder(), (dataType + "_" + DataProject.LIST_LENGTHS + "." + featureId + DataApp.TSV_EXT)).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            int idx = 0;
            for(String item : lstBkgnd)
                writer.write(item + "\t" + lstLength.get(idx++) + "\n");
            result = true;
        }
        catch (Exception e) {
            System.out.println("FEAnalysis list length file Code exception: " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        
        if(result) {
            result = false;
            filepath = getFEABkgndListFilepath(dataType, featureId);
            writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                int idx = 0;
                for(String item : lstBkgnd)
                    writer.write(item + "\n");
                result = true;
            }
            catch (Exception e) {
                System.out.println("FEAnalysis background list file Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }

        if(result) {
            result = false;
            filepath = getFEATestListFilepath(dataType, featureId);
            writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                int idx = 0;
                for(String item : lstTest)
                    writer.write(item + "\n");
                result = true;
            }
            catch (Exception e) {
                System.out.println("FEAnalysis test list file Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    // returns the actual FEA results returned by the R script
    // caller needs to deal with calculating Significant using calls in FEAResults
    public ArrayList<FEAResultsData> getFEAResultsData(String type, String featureId, double sigValue) {
        long tstart = System.nanoTime();
        ArrayList<FEAResultsData> lstResults = new ArrayList<>();
        try {
            String filepath = getFEAResultsFilepath(type, featureId);
            System.out.println("Reading FEA statistical test result data from " + filepath + " .");
            List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
            int lnum = 1;
            String[] fields;
            for(String line : lines) {
                if(lnum > 1) {
                    // R is sticking spaces between the tabs
                    fields = line.split("\t");
                    for(int i = 0; i < fields.length; i++)
                        fields[i] = fields[i].trim();
                    if(fields.length == 9) {
                        // feature, overPValue, underPValue, numDEInCat, numInCat, adjPValue, description, category
                        lstResults.add(new FEAResultsData(fields[0], fields[6], fields[8], fields[7], Double.parseDouble(fields[1]), Double.parseDouble(fields[2]), 
                                           Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Double.parseDouble(fields[5]), sigValue));
                    }
                    else {
                        logger.logError("Invalid number of columns, " + fields.length + ", in FEA results data, line " + lnum + ".");
                        break;
                    }
                }
                lnum++;
            }
        }
        catch (Exception e) {
            lstResults.clear();
            logger.logError("Unable to load FEA " + type + " results data: " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Loaded FEA statistical test result data in " + duration + " ms");
        return lstResults;
    }
    public ArrayList<ArrayList<FEAResultsData>> getFEAEnrichedChartData(String type, String featureId, double sigValue) {
        HashMap<String, ArrayList<FEAResultsData>> hmFeatures = new HashMap<>();
        ArrayList<ArrayList<FEAResultsData>> lstData = new ArrayList<>();
        
        // get FEA results and sort list by adjusted p-value
        ArrayList<FEAResultsData> lstResults = getFEAResultsData(type, featureId, sigValue);
        Collections.sort(lstResults);
        
        // process FEA results
        ArrayList<FEAResultsData> lst;
        for(FEAResultsData frd: lstResults) {
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
    
    public ArrayList<ArrayList<FEAResultsData>> getFEAChartData(String type, String featureId, double sigValue) {
        HashMap<String, ArrayList<FEAResultsData>> hmFeatures = new HashMap<>();
        ArrayList<ArrayList<FEAResultsData>> lstData = new ArrayList<>();
        
        // get FEA results and sort list by adjusted p-value
        ArrayList<FEAResultsData> lstResults = getFEAResultsData(type, featureId, sigValue);
        Collections.sort(lstResults);
        
        // process FEA results
        ArrayList<FEAResultsData> lst;
        for(FEAResultsData frd: lstResults) {
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
    
    public EnrichedFeaturesData getFEAEnrichedFeaturesData(String dataType, String id, double sigValue,
                                                    HashMap<String, Object> hmTest, HashMap<String, Object> hmBkgnd) {
        ArrayList<FEAStatsData> lstStats = new ArrayList<>();
        ArrayList<String> lstMembers = new ArrayList<>();
        ArrayList<BitSet> lstFeatureMembers = new ArrayList<>();
        EnrichedFeaturesData data = new EnrichedFeaturesData(hmTest.size(), hmBkgnd.size(), lstStats, lstMembers, lstFeatureMembers);
        ArrayList<FEAResultsData> lstResults = getFEAResultsData(dataType, id, sigValue);
        String filepathFeatures = getFEAFeaturesFilepath(id, dataType);
        
        // get list of terms for members - must have been previously generated to run the analysis
        HashMap<String, Integer> hmItems = new HashMap<>();
        HashMap<String, DataAnnotation.FeatureItemsInfo> hmFI = project.data.loadFeatureItemsInfo(filepathFeatures);

        // process all terms
        for(FEAResultsData result : lstResults) {
            // check if term is enriched
            if(result.significant) {
                // add members to list to track bitset location
                int testCnt = 0;
                ArrayList<String> lstItems = hmFI.get(result.feature).items;
                for(String item : lstItems) {
                    if(!hmItems.containsKey(item)) {
                        hmItems.put(item, lstMembers.size());
                        lstMembers.add(item);
                    }
                    if(hmTest.containsKey(item))
                        testCnt++;
                }
                
                // generate bitset for this feature
                BitSet bs = new BitSet(hmBkgnd.size());
                lstFeatureMembers.add(bs);
                for(String item : lstItems)
                    bs.set(hmItems.get(item));

                // feature, description, cat, overPV, fdr, String sig, testItemsCnt, bkgndItemsCnt (remove sig?)
                lstStats.add(new FEAStatsData(result.feature, result.description, result.category, result.source,
                        result.overPValue, result.adjPValue, result.significant? "YES" : "NO", testCnt, lstItems.size()));
            }
        }
        return data;
    }
    public ObservableList<FEASelectionResults> getFEASelectionResults(String type, String featureId, double sigValue) {
        ObservableList<FEASelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<FEAResultsData> lstDS = getFEAResultsData(type, featureId, sigValue);
        for(FEAResultsData dsrd : lstDS)
            lstResults.add(new FEASelectionResults(false, DataType.valueOf(type), dsrd));
        return lstResults;
    }
    
    //
    // Internal Functions
    //
    
    private ArrayList<String> getFEAFilesList(String id, boolean rmvPrms) {
        ArrayList<String> lst = new ArrayList<>();
        File feaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FEA).toString());
        FilenameFilter filter = (File dir, String name) -> {
            if(rmvPrms)
                return (name.endsWith(DataApp.PRM_EXT) || name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
            else
                return (name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
        };        
        File[] files = feaFolder.listFiles(filter);
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
    // NOTE: May return duplicate protein entries in list so all transcripts can be included
    private ArrayList<String> getProteinBkgndList(DlgFEAnalysis.Params.BkgndList lstBkgndType, String lstfilepath, SubTabBase subTab,String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        switch(lstBkgndType) {
            case ALL:
                subTab.appendLogLine("Loading all project proteins background list.", logfilepath);
                HashMap<String, Object> hmFilterTrans = project.data.getResultsTrans();
                String protein;
                for(String trans : hmFilterTrans.keySet()) {
                    protein = project.data.getTransProtein(trans);
                    if(!protein.isEmpty())
                        lst.add(protein);
                }
                Collections.sort(lst);
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading proteins background list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    // get list of project proteins
                    HashMap<String, Object> hmFilterProteins = project.data.getResultsProteins();
                    ArrayList<String> lstBad = new ArrayList<>();
                    for(String entry : lstItems) {
                        if(hmFilterProteins.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: Background list contains " + lstBad.size() + " proteins that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }
    // NOTE: May return duplicate protein entries in list so all transcripts can be included
    private ArrayList<String> getProteinTestList(DlgFEAnalysis.Params.TestList lstTestType, String lstfilepath, SubTabBase subTab,String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        boolean flg = true;
        DlgDEAnalysis.Params deaParams;
        switch(lstTestType) {
            case NOTDE:
                flg = false;
                // flow into DE
            case DE:
                subTab.appendLogLine("Loading " + (flg? "DE" : "NOT DE") + " genes test list.", logfilepath);
                deaParams = deaData.getDEAParams(DataType.GENE);
                DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.PROTEIN, "", deaParams.sigValue, deaParams.FCValue);
                if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                    // build list
                    for(DataDEA.DEAResultsData data : deaResults.lstResults) {
                        if(data.de == flg)
                            lst.add(data.id);
                    }
                }
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading proteins test list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    // get list of project proteins
                    HashMap<String, Object> hmFilterProteins = project.data.getResultsProteins();
                    ArrayList<String> lstBad = new ArrayList<>();
                    for(String entry : lstItems) {
                        if(hmFilterProteins.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: Test list contains " + lstBad.size() + " proteins that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }
    private ArrayList<String> getTransBkgndList(DlgFEAnalysis.Params.BkgndList lstBkgndType, String lstfilepath, SubTabBase subTab, String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        HashMap<String, Object> hmFilterTrans;
        switch(lstBkgndType) {
            case ALL:
                subTab.appendLogLine("Loading all project transcripts background list.", logfilepath);
                hmFilterTrans = project.data.getResultsTrans();
                for(String trans : hmFilterTrans.keySet())
                    lst.add(trans);
                Collections.sort(lst);
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading transcripts background list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    ArrayList<String> lstBad = new ArrayList<>();
                    hmFilterTrans = project.data.getResultsTrans();
                    for(String entry : lstItems) {
                        if(hmFilterTrans.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: background list contains " + lstBad.size() + " transcripts that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }
    private ArrayList<String> getTransTestList(DlgFEAnalysis.Params.TestList lstTestType, String lstfilepath, SubTabBase subTab,String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        boolean flg = true;
        DlgDEAnalysis.Params deaParams;
        switch(lstTestType) {
            case NOTDE:
                flg = false;
                // flow into DE
            case DE:
                subTab.appendLogLine("Loading " + (flg? "DE" : "NOT DE") + " transcripts test list.", logfilepath);
                deaParams = deaData.getDEAParams(DataType.GENE);
                DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.TRANS, "", deaParams.sigValue, deaParams.FCValue);
                if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                    // build list
                    for(DataDEA.DEAResultsData data : deaResults.lstResults) {
                        if(data.de == flg)
                            lst.add(data.id);
                    }
                }
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading transcripts test list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    ArrayList<String> lstBad = new ArrayList<>();
                    HashMap<String, Object> hmFilterTrans = project.data.getResultsTrans();
                    for(String entry : lstItems) {
                        if(hmFilterTrans.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: Test list contains " + lstBad.size() + " transcripts that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }
    private ArrayList<String> getGeneBkgndList(DlgFEAnalysis.Params.BkgndList lstBkgndType, String lstfilepath, SubTabBase subTab, String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = project.data.getResultsGeneTrans();
        switch(lstBkgndType) {
            case ALL:
                subTab.appendLogLine("Loading all project genes background list.", logfilepath);
                for(String gene : hmFilterGeneTrans.keySet())
                    lst.add(gene);
                Collections.sort(lst);
                break;
            case MULTIISOFORM:
                HashMap<String, Integer> hmGenes = project.data.getGeneTransCount();
                for(String gene : hmGenes.keySet()) {
                    if(hmGenes.get(gene) > 1)
                        lst.add(gene);
                }
                Collections.sort(lst);
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading genes background list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    ArrayList<String> lstBad = new ArrayList<>();
                    for(String entry : lstItems) {
                        if(hmFilterGeneTrans.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: Background list contains " + lstBad.size() + " genes that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }
    private ArrayList<String> getGeneTestList(DlgFEAnalysis.Params.TestList lstTestType, String lstfilepath, SubTabBase subTab, String logfilepath) {
        ArrayList<String> lst = new ArrayList<>();
        ArrayList<DataDIU.DIUResultsData> lstResults;
        boolean flg = true;
        DataType type;
        DlgDIUAnalysis.Params diuParams;
        DlgDEAnalysis.Params deaParams;
        switch(lstTestType) {
            case NOTDE:
                flg = false;
                // flow into DE
            case DE:
                subTab.appendLogLine("Loading " + (flg? "DE" : "NOT DE") + " genes test list.", logfilepath);
                deaParams = deaData.getDEAParams(DataType.GENE);
                DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.GENE, "", deaParams.sigValue, deaParams.FCValue);
                if(deaResults != null && !deaResults.lstResults.isEmpty()) {
                    // build list
                    for(DataDEA.DEAResultsData result : deaResults.lstResults) {
                       if(result.de == flg)
                           lst.add(result.id);
                    }
                    Collections.sort(lst);
                }
                break;
            case NOTDSTRANS:
                flg = false;
                // flow into normal DS
            case DSTRANS:
                subTab.appendLogLine("Loading transcript " + (flg? "DS" : "NOT DS") + " genes test list.", logfilepath);
                type = DataType.TRANS;
                diuParams = DlgDIUAnalysis.Params.load(project.data.analysis.getDIUParamsFilepath(type), project);
                lstResults = project.data.analysis.getDIUResultsData(type, diuParams.sigValue);
                if(lstResults != null && !lstResults.isEmpty()) {
                    // build list
                    for(DataDIU.DIUResultsData data : lstResults) {
                       if(data.ds == flg)
                           lst.add(data.gene);
                    }
                    Collections.sort(lst);
                }
                break;
            case NOTDSPROT:
                flg = false;
                // flow into normal DS
            case DSPROT:
                subTab.appendLogLine("Loading protein " + (flg? "DS" : "NOT DS") + " genes test list.", logfilepath);
                type = DataType.PROTEIN;
                diuParams = DlgDIUAnalysis.Params.load(project.data.analysis.getDIUParamsFilepath(type), project);
                lstResults = project.data.analysis.getDIUResultsData(type, diuParams.sigValue);
                if(lstResults != null && !lstResults.isEmpty()) {
                    // build list
                    for(DataDIU.DIUResultsData data : lstResults) {
                       if(data.ds == flg)
                           lst.add(data.gene);
                    }
                    Collections.sort(lst);
                }
                break;
            case FROMFILE:
                subTab.appendLogLine("Loading genes test list from file: " + lstfilepath, logfilepath);
                ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(lstfilepath, false);
                if(!lstItems.isEmpty()) {
                    ArrayList<String> lstBad = new ArrayList<>();
                    HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = project.data.getResultsGeneTrans();
                    for(String entry : lstItems) {
                        if(hmFilterGeneTrans.containsKey(entry))
                            lst.add(entry);
                        else
                            lstBad.add(entry);
                    }
                    if(!lstBad.isEmpty())
                        subTab.appendLogLine("WARNING: Test list contains " + lstBad.size() + " genes that are not part of the project.", logfilepath);
                }
                break;
        }
        return lst;
    }    

    //
    // Data Classes
    //

    // FEA data for features
    public static class FEAResultsData implements Comparable<FEAResultsData> {
        public String source;
        public String feature;
        public String description;
        public String category;
        public double overPValue;
        public double underPValue;
        public int numDEInCat;
        public int numInCat;
        public double adjPValue;
        public boolean significant;

        public FEAResultsData(String feature, String description, String category, String source, double overPValue, double underPValue, 
                int numDEInCat, int numInCat, double adjPValue, double sigValue) {
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.feature = feature;
            this.description = description;
            this.category = category;
            this.source = source;
            this.overPValue = Double.parseDouble(formatter.format(overPValue));
            this.underPValue = Double.parseDouble(formatter.format(underPValue));
            this.numDEInCat = numDEInCat;
            this.numInCat = numInCat;
            this.adjPValue = Double.parseDouble(formatter.format(adjPValue));
            this.significant = adjPValue < sigValue;
        }
        @Override
        public int compareTo(FEAResultsData td) {
            if(adjPValue != td.adjPValue)
                return ((adjPValue > td.adjPValue)? 1 : -1);
            else // could go on checking if equal but not worth the effort
                return ((overPValue > td.overPValue)? 1 : (overPValue < td.overPValue)? -1 : 0);
        }
    }
    public static class FEAStatsData {
        public final SimpleStringProperty source;
        public final SimpleStringProperty feature;
        public final SimpleStringProperty description;
        public final SimpleStringProperty cat;
        public final SimpleDoubleProperty overPV;
        public final SimpleDoubleProperty adjPValue;
        public final SimpleStringProperty significant;
        public final SimpleIntegerProperty testItemsCnt;
        public final SimpleIntegerProperty nonTestItemsCnt;
        public final SimpleIntegerProperty bkgndItemsCnt;
 
        public FEAStatsData(String feature, String description, String cat, String source, double overPV, double adjPValue, String significant, 
                            int testItemsCnt, int bkgndItemsCnt) {
            this.source = new SimpleStringProperty(source);
            this.feature = new SimpleStringProperty(feature);
            this.description = new SimpleStringProperty(description);
            this.cat = new SimpleStringProperty(cat);
            this.overPV = new SimpleDoubleProperty(overPV);
            this.adjPValue = new SimpleDoubleProperty(adjPValue);
            this.significant = new SimpleStringProperty(significant);
            this.testItemsCnt = new SimpleIntegerProperty(testItemsCnt);
            this.nonTestItemsCnt = new SimpleIntegerProperty(bkgndItemsCnt - testItemsCnt);
            this.bkgndItemsCnt = new SimpleIntegerProperty(bkgndItemsCnt);
        }
        public String getFeature() { return feature.get(); }
        public String getDescription() { return description.get(); }
        public String getCategory() { return cat.get(); }
        public String getSource() { return source.get(); }
        public Double getOverPV() { return overPV.get(); }
        public Double getAdjustedPV() { return adjPValue.get(); }
        public String getSignificant() { return significant.get(); }
        public Integer getTestItemsCnt() { return testItemsCnt.get(); }
        public Integer getNonTestItemsCnt() { return nonTestItemsCnt.get(); }
        public Integer getBkgndItemsCnt() { return bkgndItemsCnt.get(); }
    }
    public static class EnrichedFeaturesData {
        public int cntTestMembers;
        public int cntBkgndMembers;
        public ArrayList<FEAStatsData> lstStats;    // features information - FEA results
        public ArrayList<String> lstMembers;        // order of members in bitsets
        public ArrayList<BitSet> lstTermMembers;    // terms in same order as in stats
        public EnrichedFeaturesData(int cntTestMembers, int cntBkgndMembers,
                ArrayList<FEAStatsData> lstStats, 
                ArrayList<String> lstMembers, ArrayList<BitSet> lstTermMembers) {
            this.cntTestMembers = cntTestMembers;
            this.cntBkgndMembers = cntBkgndMembers;
            this.lstStats = lstStats;
            this.lstMembers = lstMembers;
            this.lstTermMembers = lstTermMembers;
        }
    }
    // specific analysis results
    public static class FEASelectionResults extends SelectionDataResults implements Comparable<FEASelectionResults> {
        public final SimpleStringProperty source;
        public final SimpleStringProperty featureId;
        public final SimpleStringProperty description;
        public final SimpleStringProperty feature;
        public final SimpleDoubleProperty overPValue;
        public final SimpleDoubleProperty underPValue;
        public final SimpleDoubleProperty adjPValue;
        public final SimpleIntegerProperty numInCat;
        public final SimpleIntegerProperty numTotalInCat;
        public final SimpleStringProperty significant;
        
        public FEASelectionResults(boolean selected, DataType dataType, FEAResultsData data) {
            super(selected, dataType, "", "", "");
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.source = new SimpleStringProperty(data.source);
            this.featureId = new SimpleStringProperty(data.feature);
            this.description = new SimpleStringProperty(data.description);
            this.feature = new SimpleStringProperty(data.category);
            this.overPValue = new SimpleDoubleProperty(data.overPValue);
            this.underPValue = new SimpleDoubleProperty(data.underPValue);
            this.adjPValue = new SimpleDoubleProperty(data.adjPValue);
            this.numInCat = new SimpleIntegerProperty(data.numDEInCat);
            this.numTotalInCat = new SimpleIntegerProperty(data.numInCat);
            this.significant = new SimpleStringProperty(data.significant? "YES" : "NO");
        }
        
        public String getSource() { return source.get(); }
        public String getFeatureId() { return featureId.get(); }
        public String getDescription() { return description.get(); }
        public String getFeature() { return feature.get(); }
        public Double getOverPValue() { return overPValue.get(); }
        public Double getUnderPValue() { return underPValue.get(); }
        public Double getAdjustedPValue() { return adjPValue.get(); }
        public int getNumInCat() { return numInCat.get(); }
        public int getNumTotalInCat() { return numTotalInCat.get(); }
        public String getSignificant() { return significant.get(); }
        
        @Override
        public int compareTo(FEASelectionResults td) {
            if(!feature.get().equals(td.feature.get()))
                return (feature.get().compareTo(td.feature.get()));
            else
                return (featureId.get().compareTo(td.featureId.get()));

            /* use if we want to sort by adj pval
            if(adjPValue.get() != td.adjPValue.get())
                return ((adjPValue.get() > td.adjPValue.get())? 1 : -1);
            else
                return ((overPValue.get() > td.overPValue.get())? 1 : (overPValue.get() < td.overPValue.get())? -1 : 0);*/
        }
    }
}
