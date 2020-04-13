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
import tappas.DataDIU.DSType;

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
public class DataDFI extends AppObject {
    public static final String WITH_SUFFIX = "_with";
    public static final String WITHOUT_SUFFIX = "_without";
    //public static final String DFI_RESULTS = "result.tsv";
    public static final String DFI_RESULTS_NAME = "dfi_result";
    //public static final String DFI_RESULTS_SUMMARY = "result_features_summary.tsv";
    public static final String DFI_RESULTS_SUMMARY_NAME = "dfi_result_features_summary";
    public static final String DFI_FEATUREIDMAP_NAME = "dfi_feature_id_map";
    public static final String DFI_FEATUREMATRIX_NAME = "dfi_feature_matrix";
    public static final String DFI_FEATUREMEANMATRIX_NAME = "dfi_feature_matrix_mean";
    public static final String DFI_FEATUREMATRIXRAW_NAME = "dfi_feature_matrix_raw";
    public static final String DFI_FEATUREIDDATA_NAME = "dfi_feature_id_data";

    public String getDFIFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DFI).toString(); }
//    public String getDFIParamsFilepath() { return Paths.get(getDFIFolder(), DataApp.PRM_DFI).toString(); }
//    public String getDFIGeneResultsFilepath() { return Paths.get(getDFIFolder(), DFI_RESULTS).toString(); }
//    public String getDFIResultsSummaryFilepath() { return Paths.get(getDFIFolder(), DFI_RESULTS_SUMMARY).toString(); }
//    public String getDFILogFilepath() { return Paths.get(getDFIFolder(), DataApp.LOG_NAME).toString(); }
//    public String getDFIFeatureIdMapFilepath() { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREIDMAP_NAME + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMATRIX_NAME + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMeanMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMEANMATRIX_NAME + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMATRIXRAW_NAME + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureIdDataFilepath() { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREIDDATA_NAME + DataApp.TSV_EXT).toString(); }
    
    public String getDFIParamsFilepath(String id) { return Paths.get(getDFIFolder(), DataApp.PRM_DFI_NAME + id + DataApp.PRM_EXT).toString(); }
    public String getDFILogFilepath(String id) { return Paths.get(getDFIFolder(), DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getDFIGeneResultsFilepath(String id) { return Paths.get(getDFIFolder(), DFI_RESULTS_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFIResultsSummaryFilepath(String id) { return Paths.get(getDFIFolder(), DFI_RESULTS_SUMMARY_NAME + "." + id + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureIdMapFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREIDMAP_NAME + "." + id + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMatrixFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMATRIX_NAME + "." + id + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMeanMatrixFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMEANMATRIX_NAME + "." + id + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureMatrixRawFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREMATRIXRAW_NAME + "." + id + DataApp.TSV_EXT).toString(); }
//    public String getDFIFeatureIdDataFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), DFI_FEATUREIDDATA_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    
    public String getDFIFeatureIdMapFilepath(String id) { return Paths.get(getDFIFolder(), DFI_FEATUREIDMAP_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFIFeatureMatrixFilepath(String id) { return Paths.get(getDFIFolder(), DFI_FEATUREMATRIX_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFIFeatureMeanMatrixFilepath(String id) { return Paths.get(getDFIFolder(), DFI_FEATUREMEANMATRIX_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFIFeatureMatrixRawFilepath(String id) { return Paths.get(getDFIFolder(), DFI_FEATUREMATRIXRAW_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFIFeatureIdDataFilepath(String id) { return Paths.get(getDFIFolder(), DFI_FEATUREIDDATA_NAME + "." + id + DataApp.TSV_EXT).toString(); }
    
    public DataDFI(Project project) {
        super(project, null);
    }
    public void initialize() {
        clearData();
    }
    public boolean hasDFIData(String id) { 
        Path results =  Paths.get(getDFIFolder(), DFI_RESULTS_NAME + "." + id + DataApp.TSV_EXT);
        return (Files.exists(results));
    }
    public boolean hasDFISummaryData(String id) { 
        return (Files.exists(Paths.get(getDFIResultsSummaryFilepath(id)))); 
    }
    
    public boolean hasAnyDFIData() {
        File dfiFolder = new File(Paths.get(getDFIFolder()).toString());
        FilenameFilter filter = (File dir, String name) -> (name.startsWith(DataApp.RESULTS_DFI_NAME) && name.endsWith(DataApp.RESULTS_EXT));        
        File[] files = dfiFolder.listFiles(filter);
        return (files != null && files.length > 0);
    }
    
    public ArrayList<DataApp.EnumData> getDFIResultsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        HashMap<String, String> hmNames = new HashMap<>();
        
        File dfiFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DFI).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.RESULTS_DFI_NAME) && name.endsWith(DataApp.RESULTS_EXT);        
        File[] files = dfiFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String fdaName = id;
                        HashMap<String, String> hm = getDFIParams(id);
                        if(hm != null && hm.containsKey(DlgDFIAnalysis.Params.NAME_PARAM))
                            fdaName = hm.get(DlgDFIAnalysis.Params.NAME_PARAM);
                        if(!hmNames.containsKey(id)){
                            lst.add(new DataApp.EnumData(id, fdaName));
                            hmNames.put(id, fdaName);
                        }
                    }
                }

            }
        }
        return lst;
    }
    
    public ArrayList<DataApp.EnumData> getDFIParamsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File dfiFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DFI).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.PRM_DFI_NAME) && name.endsWith(DataApp.PRM_EXT);        
        File[] files = dfiFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String fdaName = id;
                        HashMap<String, String> hm = getDFIParams(id);
                        if(hm != null && hm.containsKey(DlgDFIAnalysis.Params.NAME_PARAM))
                            fdaName = hm.get(DlgDFIAnalysis.Params.NAME_PARAM);
                        lst.add(new DataApp.EnumData(id, fdaName));
                    }
                }
            }
        }
        return lst;
    }
    
    public void clearData() {
    }
    public void clearDataDFI(String id, boolean rmvPrm) {
        removeDFIFiles(id, rmvPrm);
    }
    public void clearDataDFI(boolean rmvPrm) {
        clearData();
        removeAllDFIResultFiles(rmvPrm);
    }
    public void removeAllDFIResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DFI), rmvPrms);
    }
    
    public void removeDFIFiles(String id, boolean rmvPrms) {
        ArrayList<String> lstFiles = getDFIFilesList(id, rmvPrms);
        for(String filepath : lstFiles)
            Utils.removeFile(Paths.get(filepath));
    }
    
    public void setDFIParams(HashMap<String, String> hmp, String id) { 
        if(hmp != null)
            Utils.saveParams(hmp, getDFIParamsFilepath(id));
    }
    public HashMap<String, String> getDFIParams(String id) {
        HashMap<String, String> hm = new HashMap<>();
        Utils.loadParams(hm, getDFIParamsFilepath(id));
        return hm;
    }
    
    public boolean genDFIInputFiles(HashMap<String, HashMap<String, Object>> hmFeatures, DlgDFIAnalysis.Params dfiParams, String analysisID) {

        boolean result = false;

        // make sure expression factors is available
        if(!Files.exists(Paths.get(project.data.getExpFactorsFilepath())))
            project.data.copyExpFactorsFile(project.data.getExpFactorsFilepath());

        // get raw expression data
        HashMap<String, double[]> hmTransRawExp = new HashMap<>();
        DataInputMatrix.ExpMatrixData red = project.data.getRawExpressionData(DataType.TRANS, project.data.getResultsTrans());
        for(DataInputMatrix.ExpMatrixArray em : red.data)
            hmTransRawExp.put(em.getTranscript(), em.daSamples);

        // determine total number of samples and samples per condition
        // in case-control we just need the condition means but
        // in time course analysys, we need the time slots for each condition
        int nsamples = red.data.get(0).daSamples.length;
        ArrayList<Integer> lstCounts = new ArrayList<>();
        int[] grps = project.data.getGroupTimes();
        for(int i = 0; i < grps.length; i++) {
            int timecnt = grps[i];
            for(int j = 0; j < timecnt; j++) {
                lstCounts.add(project.data.getGroupTimeSamples(i, j));
            }
        }
        int totalCols = lstCounts.size();
        Integer[] scnt = new Integer[totalCols];
        scnt = lstCounts.toArray(scnt);
            
        // get expression data
        HashMap<String, double[]> hmTransExp = new HashMap<>();
        DataInputMatrix.ExpMatrixData ed = project.data.getExpressionData(DataType.TRANS, new HashMap<>());
        for(DataInputMatrix.ExpMatrixArray em : ed.data)
            hmTransExp.put(em.getTranscript(), em.daSamples);
        String[] names = project.data.getGroupNames();

        // expression data, id_with:explevel, id_wout:explevel
        HashMap<String, double[]> hmRawExpData = new HashMap<>();
        HashMap<String, double[]> hmExpData = new HashMap<>();
        HashMap<String, double[]> hmExpMeanData = new HashMap<>();
        HashMap<String, String> hmFavoredCondition = new HashMap<>();
        
        // get genes with diversity, only diverse ones returned
        // feature->gene-><trans, true/false> true if it  contains feature
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity;
        //boolean genpos = false;
        boolean new_genpos = false;
        if(dfiParams.using.equals(DlgDFIAnalysis.Params.Using.NEW_GENPOS))
            new_genpos = true;

        if(new_genpos)
            hmGeneDiversity = project.data.getDiverseFeaturesUsingNewGenomicPosition(project.data.getResultsGeneTrans(), hmFeatures, false, null);
        else //presence
            hmGeneDiversity = project.data.getDiverseFeaturesUsingPresence(project.data.getResultsGeneTrans(), hmFeatures);
        
        // write a debug file so that we can check which genes/features are selected
        // TODO: this is actually used somewhere else in the code now - rename!!!
        writeGeneFeatureDbg(hmGeneDiversity, getDFIFeatureIdMapFilepath(analysisID) + ".dbg");
        int fcnt = 0;
        int tcnt = 0;
        String[] fields;
        ArrayList<GeneFeatureDiversity> lstDiversity = new ArrayList<>();
        if(hmGeneDiversity.size()==0){
            app.logInfo("Differential Feature Inclusion Analysis can't continue because your data has not genes with this feature.");
        }
        
        ArrayList<GeneFeatureId> lstBaseIds = new ArrayList<>();
        for(String feature : hmGeneDiversity.keySet()) {
            // should have db\tcat\tid for feature name
            boolean diverse;
            String posval;
            double[] emsamples, remsamples;
            fields = feature.split("\t");
            if(fields.length == 3 || fields.length == 4) {
                HashMap<String, HashMap<String, Object>> hmGenes = hmGeneDiversity.get(feature);
                for(String gene : hmGenes.keySet()) {
                    // create expression matrix entries, gene;db;cat;id_with/without or gene;db;cat;id;pos_with/without
                    // we need to not allow gene/db/cats that use ";" or translate here to avoid parsing conflict later!!!
                    posval = "";
                    if(fields.length == 4)
                        posval = fields[3];
                    GeneFeatureId gfi = new GeneFeatureId(gene, fields[0], fields[1], fields[2], posval);
                    String baseId = gfi.getBaseId();
                    String idWith = baseId + DataDFI.WITH_SUFFIX;
                    String idWithout = baseId + DataDFI.WITHOUT_SUFFIX;
                    double[] with = new double[nsamples];
                    double[] without = new double[nsamples];
                    double[] rawWith = new double[nsamples];
                    double[] rawWithout = new double[nsamples];
                    HashMap<String, Object> hmGeneTrans = hmGenes.get(gene);
                    for(String trans : hmGeneTrans.keySet()) {
                        diverse = (Boolean) hmGeneTrans.get(trans);
                        lstDiversity.add(new GeneFeatureDiversity(gene, trans, fields[0], fields[1], fields[2], posval, diverse));
                        // WARN: raw expression counts are being added - OK according to Lorena
                        // update expression counts
                        emsamples = hmTransExp.get(trans);
                        remsamples = hmTransRawExp.get(trans);
                        if(diverse) {
                            for(int i = 0; i < nsamples; i++) {
                                with[i] += emsamples[i];
                                rawWith[i] += remsamples[i];
                            }
                        }
                        else {
                            for(int i = 0; i < nsamples; i++) {
                                without[i] += emsamples[i];
                                rawWithout[i] += remsamples[i];
                            }
                        }
                    }
                    
                    // caculate means - we must provide the values for all group:time slots
                    double[] withMean = new double[totalCols];
                    double[] withoutMean = new double[totalCols];
                    int[] wcnt = new int[totalCols];
                    int[] wocnt = new int[totalCols];
                    for(int i = 0; i < nsamples; i++) {
                        int c = 0;
                        for(int offset = 0; c < totalCols; c++) {
                            if(i < (scnt[c] + offset)) {
                                wcnt[c]++;
                                break;
                            }
                            else
                                offset += scnt[c];
                        }
                        withMean[c] += with[i];
                    }
                    for(int c = 0; c < totalCols; c++) {
                        double mean = withMean[c]/wcnt[c];
                        withMean[c] = Double.parseDouble(String.format("%.02f", ((double)Math.round(mean*100)/100.0)));
                    }
                    for(int i = 0; i < nsamples; i++) {
                        int c = 0;
                        for(int offset = 0; c < totalCols; c++) {
                            if(i < (scnt[c] + offset)) {
                                wocnt[c]++;
                                break;
                            }
                            else
                                offset += scnt[c];
                        }
                        withoutMean[c] += without[i];
                    }
                    for(int c = 0; c < totalCols; c++) {
                        double mean = withoutMean[c]/wocnt[c];
                        withoutMean[c] = Double.parseDouble(String.format("%.02f", ((double)Math.round(mean*100)/100.0)));
                    }

                    boolean add = true;
                    double fc1 = 0;
                    double fc2 = 0;
                    
                    // If we had 0 anywhere we dont want to compare this feature because is expressed in all proteins (just 1 protein)
                    if(withoutMean[0] == 0 || withoutMean[1] == 0) {
                        add = false;
                    }
                    /* We already filter with fold or prop
                    if(project.data.isCaseControlExpType()) {
                        if(withoutMean[0] != 0 && withoutMean[1] != 0) {
                            // filter extreme fold changes between conditions
                            // generates too many significant results (see Lorena)
                            // NOTE: we may want to let users decide whether to filter or not!
                            // filtering is not done for maSigPro - already handled by maSigPro
                            fc1 = withMean[0]/withoutMean[0];
                            fc2 = withMean[1]/withoutMean[1];
                            if((fc1 > 10 && fc2 > 10) || (fc1 < 0.1 && fc2 < 0.1))
                                add = false;
                        }
                        else
                            add = false;
                    }
                    */
                    // add gene feature expression levels
                    if(add) {
                        // we are deciding which condition is favored with respect to the 'with feature' isoforms
                        // it all boils down to: in which condition does the (with/without) fold change is greater
                        // regardless of the slope of the with and without lines from C1 to C2
                        // since the FC division will take into account the gain/loss of the 'with' in respect to 'without'
                        //double w = withMean[1]/withMean[0];
                        //double wo = withoutMean[1]/withoutMean[0];
                        fc1 = withMean[0]/withoutMean[0];
                        fc2 = withMean[1]/withoutMean[1];
                        if(project.data.isCaseControlExpType())
                            hmFavoredCondition.put(baseId, ((fc2 == fc1)? "None" : ((fc2 > fc1)? names[1] : names[0])));
                        lstBaseIds.add(gfi);
                        hmRawExpData.put(idWith, rawWith);
                        hmRawExpData.put(idWithout, rawWithout);
                        hmExpData.put(idWith, with);
                        hmExpData.put(idWithout, without);
                        hmExpMeanData.put(idWith, withMean);
                        hmExpMeanData.put(idWithout, withoutMean);
                    }
                    else {
                        fcnt++;
                        //System.out.println("Filtering DFI: " + baseId + ", FC1: " + fc1 + ", FC2: " + fc2);
                        logger.logDebug("Filtering DFI: " + baseId + ", FC1: " + fc1 + ", FC2: " + fc2);
                    }
                    tcnt++;
                }
            }
            else {
                result = false;
                lstDiversity.clear();
                logger.logError("Unexpected result found checking for gene diversity.");
                break;
            }
        }
        if(fcnt > 0)
            logger.logDebug("Filtered " + fcnt + " DFI records out of " + tcnt);
        
        if(!lstDiversity.isEmpty()) {
            Collections.sort(lstBaseIds);
            if(writeGeneFeatureIdMap(lstBaseIds, getDFIFeatureIdMapFilepath(analysisID))) {
                // change expression data to list and sort it just for ease of debugging file output
                ArrayList<GeneFeatureExpMatrixData> lstRawExpData = new ArrayList<>();
                for(String id : hmRawExpData.keySet())
                    lstRawExpData.add(new GeneFeatureExpMatrixData(id, hmRawExpData.get(id)));
                Collections.sort(lstRawExpData);
                ArrayList<GeneFeatureExpMatrixData> lstExpData = new ArrayList<>();
                for(String id : hmExpData.keySet())
                    lstExpData.add(new GeneFeatureExpMatrixData(id, hmExpData.get(id)));
                Collections.sort(lstExpData);
                ArrayList<GeneFeatureExpMatrixData> lstExpMeanData = new ArrayList<>();
                for(String id : hmExpMeanData.keySet())
                    lstExpMeanData.add(new GeneFeatureExpMatrixData(id, hmExpMeanData.get(id)));
                Collections.sort(lstExpMeanData);
                String header = project.data.getExpMatrixFileHeader();
                String headerMean = "";
                for(String name : names)
                    headerMean += (headerMean.isEmpty()? "#" : "\t") + name;
                if(writeGeneFeatureMatrix(lstRawExpData, header, getDFIFeatureMatrixRawFilepath(analysisID)) &&
                   writeGeneFeatureMatrix(lstExpMeanData, headerMean, getDFIFeatureMeanMatrixFilepath(analysisID)) &&
                   writeGeneFeatureMatrix(lstExpData, header, getDFIFeatureMatrixFilepath(analysisID))) {
                        if(project.data.isCaseControlExpType()) {
                            result = writeGeneFeatureData(hmFavoredCondition, "#Feature\tFavored", getDFIFeatureIdDataFilepath(analysisID));
                        }
                        else
                            result = true;
                }
            }
        }
        return result;
    }
    public ArrayList<String> getDFIGenes(DSType dsType, double sigValue, String analysisID) {
        ArrayList<String> lstDS = new ArrayList<>();
        ArrayList<DFIResultsData> lstResults = getDFIResultsData(sigValue, analysisID);
        lstResults.forEach((dsar) -> {
            if(dsType.equals(DSType.DS) && dsar.ds)
                lstDS.add(dsar.gene);
            else if(dsType.equals(DSType.NOTDS) && !dsar.ds)
                lstDS.add(dsar.gene);
            else if(dsType.equals(DSType.ALL))
                lstDS.add(dsar.gene);
        });
        return lstDS;
    }
    public HashMap<String, HashMap<String, Object>> getDFIGeneTransFilter(DSType dsType, double sigValue, String analysisID) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        ArrayList<String> lst = getDFIGenes(dsType, sigValue, analysisID);
        HashMap<String, HashMap<String, Object>> hmGT = project.data.getResultsGeneTrans();
        for(String gene : lst) {
            if(hmGT.containsKey(gene)) {
                HashMap<String, Object> hm = new HashMap<>();
                hmGeneTrans.put(gene, hm);
                HashMap<String, Object> hmt = hmGT.get(gene);
                hmt.keySet().forEach((trans) -> {
                    hm.put(trans, null);
                });
            }
        }
        return hmGeneTrans;
    }
    public ArrayList<DFIResultsData> getDFIResultsData(double sigValue, String analysisID) {
        ArrayList<DFIResultsData> lstDS = new ArrayList<>();
        try {
            HashMap<String, double[]> hmMeanExp = getDFIMeanExp(analysisID);
            HashMap<String, String> hmFavored = getDFIIdData(analysisID);
            if(Files.exists(Paths.get(getDFIGeneResultsFilepath(analysisID)))) {
                List<String> lines = Files.readAllLines(Paths.get(getDFIGeneResultsFilepath(analysisID)), StandardCharsets.UTF_8);

                // if the results are from edgeR we get: gene pValue qValue MayorIsoformSwitching totalChange
                // if the results are from DEXSeq we get: gene qValue MayorIsoformSwitching totalChange
                // if the results are form maSigPro we get: gene qValue MayorIsoformSwitching podiumTime (in single)
                String gene, geneFeature, db, cat, feature;
                String[] fields;
                double pValue, qValue, totalChange;
                boolean podiumChange;
                int lnum = 1;
                int fldLength = -1;
                for(String line : lines) {
                    if(lnum > 1) {
                        fields = line.split("\t");
                        if(fldLength == -1) {
                            fldLength = fields.length;
                            //Case = 4/6, Single = 5, Multiple = 5
                            if(fldLength != 4 && fldLength != 5 && fldLength != 6) {
                                logger.logError("Invalid number of columns, " + fldLength + ", in DIU gene results data.");
                                break;
                            }
                        }
                        if(fields.length == fldLength) {
                            // R is sticking spaces between the tabs
                            for(int i = 0; i < fields.length; i++)
                                fields[i] = fields[i].trim();
                            int fldIdx = 0;
                            String position; //posval;
                            geneFeature = fields[fldIdx++];
                            String[] parts = geneFeature.split(";");
                            // another approach would be to use the id map file
                            // this could be an issue if there was a ";" replaced with a "_"
                            if(parts.length == 4 || parts.length == 5) {
                                gene = parts[0];
                                db = parts[1];
                                cat = parts[2];
                                feature = parts[3];
                                position = "";
                                if(parts.length == 5)
                                    position = parts[4];
                                if(fldLength == 6)
                                    pValue = Double.parseDouble(fields[fldIdx++]);
                                else
                                    pValue = 0;
                                qValue = Double.parseDouble(fields[fldIdx++]);
                                // skipping favorC2 field, not used - remove from script results later
                                if(fldLength == 6)
                                    fldIdx++;
                                podiumChange = Boolean.valueOf(fields[fldIdx++]);
                                totalChange = (project.data.isTimeCourseExpType())? 0.0 : Double.parseDouble(fields[fldIdx++]);
                                String timePoints = (project.data.isTimeCourseExpType())? (fields[fldIdx++].equals(".")? "" : fields[fldIdx-1]) : "";
                                String favoredTimes = (project.data.isTimeCourseExpType())? (fields[fldIdx++].equals(".")? "" : fields[fldIdx-1]) : "";
                                String geneDescription = project.data.getGeneDescription(gene);
                                String favored = "N/A";
                                if(hmFavored.containsKey(geneFeature))
                                    favored = hmFavored.get(geneFeature);
                                else if(project.data.isTimeCourseExpType() && !favoredTimes.equals(""))
                                    favored = favoredTimes;
                                double[] withMeanExp = null;
                                double[] withoutMeanExp = null;
                                if(hmMeanExp.containsKey(geneFeature + WITH_SUFFIX))
                                    withMeanExp = hmMeanExp.get(geneFeature + WITH_SUFFIX);
                                if(hmMeanExp.containsKey(geneFeature + WITHOUT_SUFFIX))
                                    withoutMeanExp = hmMeanExp.get(geneFeature + WITHOUT_SUFFIX);
                                lstDS.add(new DFIResultsData(gene, geneDescription, db, cat, feature, project.data.getFeatureDescription(db,cat,feature), position, pValue, qValue, favored, withMeanExp, withoutMeanExp, podiumChange, timePoints, favoredTimes, totalChange, qValue < sigValue));
                            }
                            else {
                                logger.logError("Invalid line, " + lnum + ", in DFI results data.");
                                break;
                            }
                        }
                        else {
                            logger.logError("Invalid line, " + lnum + ", in DFI results data.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DFI results data: " + e.getMessage());
        }
        logger.logDebug("Returned  " + lstDS.size() + " DFI result entries");
        return lstDS;
    }
    public HashMap<String, String> getDFIIdData(String analysisID) {
        HashMap<String, String> hmData = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDFIFeatureIdDataFilepath(analysisID)))) {
                List<String> lines = Files.readAllLines(Paths.get(getDFIFeatureIdDataFilepath(analysisID)), StandardCharsets.UTF_8);

                // get id and favored
                String[] fields;
                int lnum = 1;
                for(String line : lines) {
                    if(!line.isEmpty() && !line.subSequence(0, 1).equals("#")) {
                        fields = line.split("\t");
                        if(fields.length == 2)
                            hmData.put(fields[0], fields[1]);
                        else {
                            logger.logError("Invalid number of columns, " + fields.length + ", in DFI featureId  data.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load FeatureId data: " + e.getMessage());
        }
        return hmData;
    }
    public HashMap<String, double[]> getDFIMeanExp(String analysisID) {
        HashMap<String, double[]> hmData = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDFIFeatureMeanMatrixFilepath(analysisID)))) {
                List<String> lines = Files.readAllLines(Paths.get(getDFIFeatureMeanMatrixFilepath(analysisID)), StandardCharsets.UTF_8);

                // get mean expression matrix
                String[] fields;
                int length = -1;
                int lnum = 1;
                for(String line : lines) {
                    if(!line.isEmpty() && !line.subSequence(0, 1).equals("#")) {
                        fields = line.split("\t");
                        if(length == -1)
                            length = fields.length;
                        if(fields.length == length) {
                            int cols = length - 1;
                            double[] means = new double[cols];
                            for(int i = 0; i < cols; i++)
                                means[i] = Double.parseDouble(fields[i+1]);
                            hmData.put(fields[0], means);
                        }
                        else {
                            logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in DFI mean expression matrix file.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DFI mean expression matrix: " + e.getMessage());
        }
        return hmData;
    }
    public HashMap<String, DFIGeneCounts> getDFIGeneCounts(ArrayList<DFIResultsData> lstDSRD, String analysisID) {
        HashMap<String, DFIGeneCounts> hmGeneCounts = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.getResultsGeneTrans();
        
        // get tested gene count
        HashMap<String, Integer> hmTested = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDFIFeatureIdMapFilepath(analysisID)))) {
                List<String> lines = Files.readAllLines(Paths.get(getDFIFeatureIdMapFilepath(analysisID)), StandardCharsets.UTF_8);

                int lnum = 1;
                String[] fields;
                for(String line : lines) {
                    if(lnum > 1) {
                        fields = line.split("\t");
                        if(fields.length == 6) {
                            if(fields[1].endsWith(WITH_SUFFIX)) {
                                String key = fields[3] + ";" + fields[4] + ";" + fields[5];
                                if(hmTested.containsKey(key))
                                    hmTested.put(key, hmTested.get(key) + 1);
                                else
                                    hmTested.put(key, 1);
                            }
                        }
                    }
                    lnum++;
                }
            }
            logger.logDebug("Got tested gene counts");
        }
        catch (Exception e) {
            logger.logError("Unable to load DFI tested gene counts: " + e.getMessage());
        }

        // get varying gene count
        HashMap<String, Integer> hmVarying = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDFIFeatureIdMapFilepath(analysisID) + ".dbg"))) {
                List<String> lines = Files.readAllLines(Paths.get(getDFIFeatureIdMapFilepath(analysisID) + ".dbg"), StandardCharsets.UTF_8);

                int lnum = 1;
                String[] fields;
                for(String line : lines) {
                    if(lnum > 1) {
                        fields = line.split("\t");
                        if(fields.length == 6) {
                            String key = fields[1] + ";" + fields[2] + ";" + fields[3];
                            if(hmVarying.containsKey(key))
                                hmVarying.put(key, hmVarying.get(key) + 1);
                            else
                                hmVarying.put(key, 1);
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DFI varying gene counts: " + e.getMessage());
        }

        // save all gene counts
        logger.logDebug("Processing " + lstDSRD.size() + " gene feature id counts...");
        int count = 0;
        boolean dspflg = true;
        for(DFIResultsData rd : lstDSRD) {
            String key = rd.source + ";" + rd.feature + ";" + rd.featureId;
            if(!hmGeneCounts.containsKey(key)) {
                int tested = 0;
                if(hmTested.containsKey(key))
                    tested = hmTested.get(key);
                int varying = 0;
                if(hmVarying.containsKey(key))
                    varying = hmVarying.get(key);
                DFIGeneCounts gc = new DFIGeneCounts(tested, varying, 0);
                for(String gene : hmGeneTrans.keySet()) {
                    if(dspflg)
                        logger.logDebug("Checking first gene for features...");
                    if(project.data.geneHasFeatureId(gene, rd.source, rd.feature, rd.featureId, hmGeneTrans.get(gene)))
                        gc.total++;
                    if(dspflg) {
                        dspflg = false;
                        logger.logDebug("Done checking first gene for features.");
                    }
                }
                hmGeneCounts.put(key, gc);
            }
            if((++count % 100) == 0)
                logger.logDebug("Checked " + count + " genes feature Ids...");
        }
        logger.logInfo("Processed " + count + " gene feature id count records.");

        return hmGeneCounts;
    }
    /*
    public ObservableList<DFIResults> getDFIResults(double sigValue) {
        ObservableList<DFIResults> lst = FXCollections.observableArrayList();
        ArrayList<DFIResultsData> lstDS = getDFIResultsData(sigValue);
        for(DFIResultsData dsrd : lstDS)
            lst.add(new DFIResults(dsrd));
        return lst;
    }*/
    public ObservableList<DFISelectionResults> getDFISelectionResults(double sigValue, boolean getGeneData, String analysisID) {
        ObservableList<DFISelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<DFIResultsData> lstDS = getDFIResultsData(sigValue, analysisID);
        for(DFIResultsData dsrd : lstDS)
            lstResults.add(new DFISelectionResults(false, dsrd));
        if(getGeneData) {
            String gene;
            HashMap<String, double[]> hmMEL = project.data.getMeanExpressionLevelsHM(DataType.GENE, project.data.getResultsTrans());
            for(DFISelectionResults dr : lstResults) {
                gene = dr.getGene();
                dr.chromo = new SimpleStringProperty(project.data.getGeneChromo(gene));
                dr.strand = new SimpleStringProperty(project.data.getGeneStrand(gene));
                dr.isoforms = new SimpleIntegerProperty(project.data.getGeneTransCount(gene));
                dr.coding = new SimpleStringProperty(project.data.isGeneCoding(gene)? "YES" : "NO");
                if(!hmMEL.isEmpty()) {
                    //boolean fnd = false;
                    if(hmMEL.containsKey(gene)) {
                        double[] conds = hmMEL.get(gene);
                        dr.conditions = new SimpleDoubleProperty[conds.length];
                        for(int i = 0; i < conds.length; i++)
                            dr.conditions[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(conds[i]*100)/100.0))));
                    }
                    else
                        logger.logWarning("Unable to find DFI expression values for gene '" + gene + "'");
                }    
            }
        }
        return lstResults;
    }
    // if the significance value changes, the summary file must be regenerated afterwards
    public ObservableList<DFISelectionResultsSummary> getDFISelectionResultsSummary(double sigValue, String analysisID) {
        ObservableList<DFISelectionResultsSummary> lstSummary = FXCollections.observableArrayList();
        String filepath = getDFIResultsSummaryFilepath(analysisID);
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                DFIResultsSummaryData sd;
                int lnum = 1;
                for(String line : lines) {
                    if(lnum > 1) {
                        String[] fields = line.split("\t");
                        if(fields.length == 8) {
                            sd = new DFIResultsSummaryData("", "", fields[0], fields[1], fields[2], project.data.getFeatureDescription(fields[0],fields[1],fields[2]), fields[3],
                                    Integer.parseInt(fields[4]), Integer.parseInt(fields[5]), Integer.parseInt(fields[6]), 
                                    Integer.parseInt(fields[7]));
                            lstSummary.add(new DFISelectionResultsSummary(false, sd));
                        }
                        else {
                            logger.logWarning("Invalid DFI results summary file data in line " + lnum + ".");
                            lstSummary.clear();
                            break;
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logWarning("Load DFI results summary file exception (" + filepath + "): " + e.getMessage());
            lstSummary.clear();
        }
        return lstSummary;
    }
    public ObservableList<DFISelectionResultsSummary> genDFISelectionResultsSummary(double sigValue, String analysisID) {
        ObservableList<DFISelectionResultsSummary> lstSummary = FXCollections.observableArrayList();
        String[] names = project.data.getGroupNames();
        logger.logDebug("Calling getDFIResultsData...");
        ArrayList<DFIResultsData> lstDSRD = getDFIResultsData(sigValue, analysisID);
        logger.logDebug("Calling getDFIGeneCounts...");
        HashMap<String, DFIGeneCounts> hmGeneCounts = getDFIGeneCounts(lstDSRD, analysisID);
        logger.logDebug("Processing gene counts...");
        HashMap<String, Object> hmFeatureIDs = new HashMap<>();
        for(DFIResultsData rd : lstDSRD) {
            String key = rd.source + ";" + rd.feature + ";" + rd.featureId;
            if(!hmFeatureIDs.containsKey(key)) {
                hmFeatureIDs.put(key, null);
                HashMap<String, Integer> hmFavored = new HashMap<>();
                int dsFeatures = 0;
                for(DFIResultsData d : lstDSRD) {
                    if(d.ds) {
                        String cmpkey = d.source + ";" + d.feature + ";" + d.featureId;
                        if(key.equals(cmpkey)) {
                            dsFeatures++;
                            if(hmFavored.containsKey(d.favored))
                                hmFavored.put(d.favored, hmFavored.get(d.favored) + 1);
                            else
                                hmFavored.put(d.favored, 1);
                        }
                    }
                }
                if(dsFeatures > 0) {
                    int c1Favored = 0;
                    int c2Favored = 0;
// implement once favored is desired for time course exps!!!
                    if(!project.data.isTimeCourseExpType()) {
                        c1Favored = hmFavored.containsKey(names[0])? hmFavored.get(names[0]) : 0;
                        c2Favored = hmFavored.containsKey(names[1])? hmFavored.get(names[1]) : 0;
                    }
                    
                    DFIGeneCounts gc;
                    if(hmGeneCounts.containsKey(key))
                        gc = hmGeneCounts.get(key);
                    else
                        gc = new DFIGeneCounts(0, 0, 0);
                    DFIResultsSummaryData sd = new DFIResultsSummaryData(rd.gene, rd.geneDescription, rd.source, rd.feature, rd.featureId, project.data.getFeatureDescription(rd.source,rd.feature,rd.featureId),
                                                                        rd.position, dsFeatures, c1Favored, c2Favored, gc.tested);
                    DFISelectionResultsSummary rs = new DFISelectionResultsSummary(false, sd);
                    lstSummary.add(rs);
                }
            }
        }
        Collections.sort(lstSummary, new DFISummarySort());
        writeSummaryFile(lstSummary, analysisID);
        return lstSummary;
    }
    private boolean writeSummaryFile(ObservableList<DFISelectionResultsSummary> lstSummary, String analysisID) {
        boolean result = false;
        String filepath = getDFIResultsSummaryFilepath(analysisID);
        try {
            long tstart = System.nanoTime();
            logger.logInfo("Writing DFI summary file to " + filepath);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer.write("Source\tFeature\tFeatureId\tPosition\tDSFeatureIDs\tC1Favored\tC2Favored\tTestedFeatureIDs\n");
                for(DFISelectionResultsSummary rs : lstSummary) {
                    writer.write(rs.getSource() + "\t" + rs.getFeature() + "\t" + rs.getFeatureId() + "\t" + rs.getPosition() + "\t" +
                                 rs.getDSFeatureIDs() + "\t" + rs.getC1Favored() + "\t" + rs.getC2Favored() + "\t" + rs.getTestedFeatureIDs() + "\n");
                }
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logInfo("Generated DFI summary file in " + duration + " ms");
            } catch (IOException e) {
                logger.logError("Unable to generate DFI summary file: " + e.getMessage());
                result = false;
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to generate DFI summary file: " + e.getMessage());
            result = false;
        }
        if(!result)
            Utils.removeFile(filepath);
        
        return result;
    }
    public ObservableList<DFISelectionResultsAssociation> getDFISelectionResultsAssociation(double sigValue, String analysisID) {
        int minNumberGenesDS = 5;
        ObservableList<DFISelectionResultsAssociation> lstSummary = FXCollections.observableArrayList();
        String[] names = project.data.getGroupNames();
        ArrayList<DFIResultsData> lstDSRD = getDFIResultsData(sigValue, analysisID);
        
        // build a list of all the keys, and relevant data, that have at least 5 DS genes... ####any DS genes
        HashMap<String, HashMap<String, Integer>> hmKeys = new HashMap<>();
        for(DFIResultsData rd : lstDSRD) {
            if(rd.ds) {
                String key = rd.source + ";" + rd.feature + ";" + rd.featureId;
                HashMap<String, Integer> hm = new HashMap<>();
                if(hmKeys.containsKey(key))
                    hm = hmKeys.get(key);
                else {
                    hm = new HashMap<>();
                    hmKeys.put(key, hm);
                }
                hm.put(rd.gene, rd.favored.equals(names[0])? 1 : 2);
            }
        }
        ArrayList<String> lstKeys = new ArrayList(hmKeys.keySet());
        Collections.sort(lstKeys, String.CASE_INSENSITIVE_ORDER);
        
        // process keys
        HashMap<String, Integer> hmGenes1, hmGenes2;
        for(int idx = 0; idx < lstKeys.size() - 1; idx++) {
            String key = lstKeys.get(idx);
            hmGenes1 = hmKeys.get(key);
            Set<String> geneset = new HashSet<>(hmGenes1.keySet());
            if(geneset.size()<minNumberGenesDS)
                continue;
            // now we need to loop through all the other keys
            // and get all the ones that have any of the same genes
            // we want an intersection of the two gene sets
            for(int cmpidx = idx + 1; cmpidx < lstKeys.size(); cmpidx++) {
                String cmpkey = lstKeys.get(cmpidx);
                hmGenes2 = hmKeys.get(cmpkey);
                Set<String> intersection = new HashSet<>(hmGenes2.keySet());
                if(intersection.size()<minNumberGenesDS)
                    continue;
                intersection.retainAll(geneset);
                if(!intersection.isEmpty()) {
                    String fields1[] = key.split(";");
                    String fields2[] = cmpkey.split(";");
                    int same = 0;
                    int opposite = 0;
                    for(String gene : intersection) {
                        if(hmGenes1.get(gene).equals(hmGenes2.get(gene)))
                            same++;
                        else
                            opposite++;
                    }
                    DFIResultsAssociationData rad = new DFIResultsAssociationData(fields1[0], fields1[1], fields1[2],
                                                            fields2[0], fields2[1], fields2[2], intersection.size(), same, opposite);
                    lstSummary.add(new DFISelectionResultsAssociation(false, rad));
                }
            }
            idx++;
        }
        logger.logDebug("Returning association data...");
        return lstSummary;
    }
    public boolean writeGeneFeatureMatrix(ArrayList<GeneFeatureExpMatrixData> lstExpData, String header, String filepath) {
        boolean result = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write(header + "\n");
            for(GeneFeatureExpMatrixData data : lstExpData) {
                writer.write(data.id);
                for(double value : data.expData)
                    writer.write("\t" + value);
                writer.write("\n");
            }
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to save gene feature expression matrix file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    public boolean writeGeneFeatureData(HashMap<String, String> hmFavoredCondition, String header, String filepath) {
        boolean result = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write(header + "\n");
            for(String id : hmFavoredCondition.keySet()) {
                writer.write(id + "\t" + hmFavoredCondition.get(id) + "\n");
            }
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to save gene feature data file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    public boolean writeGeneFeatureIdMap(ArrayList<GeneFeatureId> lstBaseIds, String filepath) {
        boolean result = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write("featureId\tgeneFeature\tgene\tdb\tcat\tid\n");
            for(GeneFeatureId gfi : lstBaseIds) {
                writer.write(gfi.getBaseId() + "\t" + gfi.getBaseId() + DataDFI.WITH_SUFFIX + "\t" + gfi.gene + "\t" + gfi.db + "\t" + gfi.cat + "\t" + gfi.id + "\n");
                writer.write(gfi.getBaseId() + "\t" + gfi.getBaseId() + DataDFI.WITHOUT_SUFFIX + "\t" + gfi.gene + "\t" + gfi.db + "\t" + gfi.cat + "\t" + gfi.id + "\n");
            }
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to save gene feature id map file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    // save genes with diversity for debugging - this file is now actually used in the code!!!
    // feature:gene:trans - true if it  contains/overlaps feature
    public boolean writeGeneFeatureDbg(HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity, String filepath) {
        boolean result = false;
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write("gene\tdb\tcat\tid\tpos\ttrans\n");
            String[] fields;
            for(String feature : hmGeneDiversity.keySet()) {
                // should have db\tcat\tid for feature name
                boolean diverse;
                String posval;
                fields = feature.split("\t");
                if(fields.length == 3 || fields.length == 4) {
                    posval = ".";
                    if(fields.length == 4)
                        posval = fields[3];
                    HashMap<String, HashMap<String, Object>> hmGenes = hmGeneDiversity.get(feature);
                    for(String gene : hmGenes.keySet()) {
                        HashMap<String, Object> hmGeneTrans = hmGenes.get(gene);
                        String transflgs = "";
                        for(String trans : hmGeneTrans.keySet()) {
                            diverse = (Boolean) hmGeneTrans.get(trans);
                            transflgs += (transflgs.isEmpty()? "" : ",") + trans + ":" + diverse;
                        }
                        writer.write(gene + "\t" + fields[0] + "\t" + fields[1] + "\t" + fields[2] + "\t" + posval + "\t" + transflgs + "\n");
                    }
                }
            }
            result = true;
        } catch (IOException e) {
            logger.logWarning("Unable to save gene feature debug file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    
    // Internal Functions
    
    private ArrayList<String> getDFIFilesList(String id, boolean rmvPrms) {
        ArrayList<String> lst = new ArrayList<>();
        File dfiFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DFI).toString());
        FilenameFilter filter = (File dir, String name) -> {
            if(rmvPrms)
                return (name.endsWith(DataApp.PRM_EXT) || name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT) || name.endsWith(DataApp.DBG_EXT));
            else
                return (name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT) || name.endsWith(DataApp.DBG_EXT));
        };        
        File[] files = dfiFolder.listFiles(filter);
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
    
    //
    // Data Classes
    //
    
    public static class DFIGeneCounts {
        int tested, varying, total;
        public DFIGeneCounts(int tested, int varying, int total) {
            this.tested = tested;
            this.varying = varying;
            this.total = total;
        }
    }
    // DIU data for genes
    // DEA result data for genes, proteins, and transcripts
    // the result values will change based on the method used for DEA - handled in DEAResults
    enum DEXSeqValues { QValue, PodiumChange, TotalChange }
    enum EdgeRValues { PValue, QValue, FavorC2, PodiumChange, TotalChange }
    // meanExp should be just Exp at least when dealing within a single gene - check later and change name if needed?
    public static class DFIResultsData {
        public String gene;
        public String geneDescription;
        public String source, feature, featureId, position, favored;
        public String featureDescription;
        public double pValue;
        public double qValue;
        public boolean podiumChange;
        public String podiumTimes;
        public String favoredTimes;
        public double totalChange;
        public double[] withMeanExp, withoutMeanExp;
        public boolean ds;

        public DFIResultsData(String gene, String geneDescription, String source, String feature, String featureId, String featureDescription , String position,
                double pValue, double qValue, String favored, double[] withMeanExp, double[] withoutMeanExp, boolean podiumChange, String podiumTimes, String favoredTimes, double totalChange, boolean ds) {
            this.gene = gene;
            this.geneDescription = geneDescription;
            this.source = source;
            this.feature = feature;
            this.featureId = featureId;
            this.featureDescription = featureDescription;
            this.position = position;
            this.pValue = pValue;
            this.qValue = qValue;
            this.favored = favored;
            this.withMeanExp = withMeanExp;
            this.withoutMeanExp = withoutMeanExp;
            this.podiumChange = podiumChange;
            this.podiumTimes = podiumTimes;
            this.favoredTimes = favoredTimes;
            this.totalChange = totalChange;
            this.ds = ds;
        }
        public void setDSFlag(double sigValue) {
            if(qValue < sigValue)
                ds = true;
            else
                ds = false;
        }
    }
    // specific analysis results
    public static class DFISelectionResults extends SelectionDataResults implements Comparable<DFISelectionResults> {
        static final double MAX_L2FC = 100.0;
        
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleStringProperty coding = null;
        public SimpleStringProperty source = null;
        public SimpleStringProperty feature = null;
        public SimpleStringProperty featureId = null;
        public SimpleStringProperty featureDescription = null;
        public SimpleStringProperty position = null;
        public SimpleStringProperty favored = null;
        
        // these values are normally based on the experimental group
        // but in the case of single series time course
        // they are based on time slots
        public final SimpleDoubleProperty[] withMeanExp;
        public final SimpleDoubleProperty[] withoutMeanExp;
        public final SimpleDoubleProperty[] L2FC;
        public SimpleDoubleProperty zero;
        public SimpleDoubleProperty pValue = null;
        public SimpleDoubleProperty qValue = null;
        public SimpleDoubleProperty totalChange = null;
        public SimpleStringProperty podiumChange = null;
        public SimpleStringProperty podiumTimes = null;
        public SimpleStringProperty favoredTimes = null;
        public SimpleStringProperty ds = null;
        
        public DFISelectionResults(boolean selected, DFIResultsData dsra) {
            super(selected, DataType.GENE, dsra.gene, dsra.gene, dsra.geneDescription);
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.source = new SimpleStringProperty(dsra.source);
            this.feature = new SimpleStringProperty(dsra.feature);
            this.featureId = new SimpleStringProperty(dsra.featureId);
            this.featureDescription = new SimpleStringProperty(dsra.featureDescription);
            this.position = new SimpleStringProperty(dsra.position);
            this.favored = new SimpleStringProperty(dsra.favored);
            if(dsra.withMeanExp != null && dsra.withoutMeanExp != null && dsra.withMeanExp.length == dsra.withoutMeanExp.length) {
                // with
                int cnt = dsra.withMeanExp.length;
                this.withMeanExp = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++)
                    this.withMeanExp[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.withMeanExp[i])));
                // without
                cnt = dsra.withoutMeanExp.length;
                this.withoutMeanExp = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++)
                    this.withoutMeanExp[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.withoutMeanExp[i])));
                // L2FC
                this.L2FC = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++) {
                    // all expression values are obviously positive, 0 or greater
                    if(this.withoutMeanExp[i].get() > 0.0) {
                        double fc = this.withMeanExp[i].get() / this.withoutMeanExp[i].get();
                        if(fc > 0.0) {
                            double l2fc = Math.log(fc)/Math.log(2);
                            this.L2FC[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(l2fc)));
                        }
                        else {
                            // withMeanExp must be 0
                            this.L2FC[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(-MAX_L2FC)));
                        }
                    }
                    else {
                        // can never have both being 0, would not include in analysis
                        this.L2FC[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(MAX_L2FC)));
                    }
                }
            }
            else {
                this.withMeanExp = null;
                this.withoutMeanExp = null;
                this.L2FC = null;
            }            

            this.pValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.pValue)));
            this.qValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.qValue)));
            this.podiumChange = new SimpleStringProperty(dsra.podiumChange? "YES" : "NO");
            this.podiumTimes = new SimpleStringProperty(dsra.podiumTimes);
            this.favoredTimes = new SimpleStringProperty(dsra.favoredTimes);
            this.totalChange = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(dsra.totalChange*100)/100.0))));
            this.ds = new SimpleStringProperty(dsra.ds? "DFI" : "Not DFI");
        }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public String getCoding() { return coding.get(); }
        public String getSource() { return source.get(); }
        public String getFeature() { return feature.get(); }
        public String getFeatureId() { return featureId.get(); }
        public String getFeatureDescription() { return featureDescription.get(); }
        public String getPosition() { return position.get(); }
        public String getFavored() { return favored.get(); }
        
        
        public Double getL2FC(int idx) { 
            double value = 0.0;
            if(L2FC != null && idx < L2FC.length)
                value = L2FC[idx].get();
            return value; 
        }
        public Double getWithMeanExp(int idx) {
            double value = 0.0;
            if(withMeanExp != null && idx < withMeanExp.length)
                value = withMeanExp[idx].get();
            return value; 
        }
        public Double getWithoutMeanExp(int idx) {
            double value = 0.0;
            if(withoutMeanExp != null && idx < withoutMeanExp.length)
                value = withoutMeanExp[idx].get();
            return value;
        }
        public Double getPValue() { return pValue.get(); }
        public Double getQValue() { return qValue.get(); }
        public String getPodiumChange() { return podiumChange.get(); }
        public String getPodiumTimes() { return podiumTimes.get(); }
        public String getFavoredTimes() { return favoredTimes.get(); }
        public Double getTotalChange() { return totalChange.get(); }
        public String getDS() { return ds.get(); }
        
        @Override
        public int compareTo(DFISelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
    // DFI results summary
    public static class DFIResultsSummaryData {
        public String gene;
        public String geneDescription;
        public String source, feature, featureId, featureDescription, position;
        public int c1Favored, c2Favored;
        public int dsFeatureIDs, testedFeatureIDs;

        public DFIResultsSummaryData(String gene, String geneDescription, String source, String feature, String featureId, String featureDescription,
                String position, int dsFeatureIDs, int c1Favored, int c2Favored, int testedFeatureIDs) {
            this.gene = gene;
            this.geneDescription = geneDescription;
            this.source = source;
            this.feature = feature;
            this.featureId = featureId;
            this.featureDescription = featureDescription;
            this.position = position;
            this.dsFeatureIDs = dsFeatureIDs;
            this.c1Favored = c1Favored;
            this.c2Favored = c2Favored;
            this.testedFeatureIDs = testedFeatureIDs;
        }
    }
    public static class DFISelectionResultsSummary extends SelectionDataResults implements Comparable<DFISelectionResultsSummary> {
        public SimpleStringProperty source = null;
        public SimpleStringProperty feature = null;
        public SimpleStringProperty featureId = null;
        public SimpleStringProperty featureDescription = null;
        public SimpleStringProperty position = null;
        public SimpleIntegerProperty dsFeatureIDs = null;
        public SimpleIntegerProperty c1Favored = null;
        public SimpleIntegerProperty c2Favored = null;
        public SimpleIntegerProperty testedFeatureIDs = null;

        public DFISelectionResultsSummary(boolean selected, DFIResultsSummaryData dsra) {
            super(selected, DataType.GENE, dsra.gene, dsra.gene, dsra.geneDescription);
            this.source = new SimpleStringProperty(dsra.source);
            this.feature = new SimpleStringProperty(dsra.feature);
            this.featureId = new SimpleStringProperty(dsra.featureId);
            this.featureDescription = new SimpleStringProperty(dsra.featureDescription);
            this.position = new SimpleStringProperty(dsra.position);
            this.dsFeatureIDs = new SimpleIntegerProperty(dsra.dsFeatureIDs);
            this.c1Favored = new SimpleIntegerProperty(dsra.c1Favored);
            this.c2Favored = new SimpleIntegerProperty(dsra.c2Favored);
            this.testedFeatureIDs = new SimpleIntegerProperty(dsra.testedFeatureIDs);
        }
        public String getSource() { return source.get(); }
        public String getFeature() { return feature.get(); }
        public String getFeatureId() { return featureId.get(); }
        public String getFeatureDescription() { return featureDescription.get(); }
        public String getPosition() { return position.get(); }
        public Integer getDSFeatureIDs() { return dsFeatureIDs.get(); }
        public Integer getC1Favored() { return c1Favored.get(); }
        public Integer getC2Favored() { return c2Favored.get(); }
        public Integer getTestedFeatureIDs() { return testedFeatureIDs.get(); }
        
        @Override
        public int compareTo(DFISelectionResultsSummary td) {
            return (featureId.get().compareTo(td.featureId.get()));
        }
    }
    // DFI results association
    public static class DFIResultsAssociationData {
        public String source1, source2;
        public String feature1, feature2;
        public String featureId1, featureId2;
        public int geneCount, sameFavored, oppositeFavored;

        public DFIResultsAssociationData(String source1, String feature1, String featureId1,
                                          String source2, String feature2, String featureId2,
                                          int geneCount, int sameFavored, int oppositeFavored) {
            this.source1 = source1;
            this.feature1 = feature1;
            this.featureId1 = featureId1;
            this.source2 = source2;
            this.feature2 = feature2;
            this.featureId2 = featureId2;
            this.geneCount = geneCount;
            this.sameFavored = sameFavored;
            this.oppositeFavored = oppositeFavored;
        }
    }
    public static class DFISelectionResultsAssociation extends SelectionDataResults implements Comparable<DFISelectionResultsAssociation> {
        public SimpleStringProperty source1 = null;
        public SimpleStringProperty feature1 = null;
        public SimpleStringProperty featureId1 = null;
        public SimpleStringProperty source2 = null;
        public SimpleStringProperty feature2 = null;
        public SimpleStringProperty featureId2 = null;
        public SimpleIntegerProperty geneCount = null;
        public SimpleIntegerProperty sameFavored = null;
        public SimpleIntegerProperty oppositeFavored = null;
        public double pValue;
        public double adjPValue;

        public DFISelectionResultsAssociation(boolean selected, DFIResultsAssociationData dsra) {
            super(selected, DataType.GENE, "", "", "");
            this.source1 = new SimpleStringProperty(dsra.source1);
            this.feature1 = new SimpleStringProperty(dsra.feature1);
            this.featureId1 = new SimpleStringProperty(dsra.featureId1);
            this.source2 = new SimpleStringProperty(dsra.source2);
            this.feature2 = new SimpleStringProperty(dsra.feature2);
            this.featureId2 = new SimpleStringProperty(dsra.featureId2);
            this.geneCount = new SimpleIntegerProperty(dsra.geneCount);
            this.sameFavored = new SimpleIntegerProperty(dsra.sameFavored);
            this.oppositeFavored = new SimpleIntegerProperty(dsra.oppositeFavored);
        }
        public String getSource1() { return source1.get(); }
        public String getFeature1() { return feature1.get(); }
        public String getFeatureId1() { return featureId1.get(); }
        public String getSource2() { return source2.get(); }
        public String getFeature2() { return feature2.get(); }
        public String getFeatureId2() { return featureId2.get(); }
        public Integer getGeneCount() { return geneCount.get(); }
        public Integer getSameFavored() { return sameFavored.get(); }
        public Integer getOppositeFavored() { return oppositeFavored.get(); }
        public double getPValue() { return pValue; }
        public double getAdjPValue() { return adjPValue; }
        
        @Override
        public int compareTo(DFISelectionResultsAssociation td) {
            if(featureId1.get().equals(td.featureId1.get()))
                return (featureId2.get().compareTo(td.featureId2.get()));
            else
                return (featureId1.get().compareTo(td.featureId1.get()));
        }
    }
    class DFISummarySort implements Comparator<DFISelectionResultsSummary>  {
        @Override
        public int compare(DFISelectionResultsSummary a, DFISelectionResultsSummary b)
        {
            if(a.getSource().equals(b.getSource())) {
                if(a.getFeature().equals(b.getFeature()))
                    return a.getFeatureId().compareToIgnoreCase(b.getFeatureId());
                else
                    return a.getFeature().compareToIgnoreCase(b.getFeature());
            }
            else
                return a.getSource().compareToIgnoreCase(b.getSource());
        }
    }
    public static class GeneFeatureDiversity {
        String gene, trans, db, cat, id, posval;
        boolean diverse;
        public GeneFeatureDiversity(String gene, String trans, String db, String cat, String id, String posval, boolean diverse) {
            this.gene = gene;
            this.trans = trans;
            this.db = db;
            this.cat = cat;
            this.id = id;
            this.posval = posval;
            this.diverse = diverse;
        }
    }
    public static class GeneFeatureId implements Comparable<GeneFeatureId> {
        String gene, db, cat, id, posval;
        public GeneFeatureId(String gene, String db, String cat, String id, String posval) {
            this.gene = gene;
            this.db = db;
            this.cat = cat;
            this.id = id;
            this.posval = posval;
        }
        public String getBaseId() {
            // replace ";" in each individual field, should not really have it but just in case
            String strpos = "";
            if(!posval.isEmpty())
                strpos = ";" + posval.replaceAll("[;]", "_");
            return (gene.replaceAll("[;]", "_") + ";" + db.replaceAll("[;]", "_") + ";" + cat.replaceAll("[;]", "_") + ";" + id.replaceAll("[;]", "_") + strpos);
        }
        @Override
        public int compareTo(GeneFeatureId data) {
            if(getBaseId() == null || data.getBaseId() == null)
                System.out.println("here");
            return (getBaseId().compareTo(data.getBaseId()));
        }
    }
    public static class GeneFeatureExpMatrixData implements Comparable<GeneFeatureExpMatrixData> {
        String id;
        double[] expData;
        public GeneFeatureExpMatrixData(String id, double[] expData) {
            this.id = id;
            this.expData = expData;
        }
        @Override
        public int compareTo(GeneFeatureExpMatrixData data) {
            return (id.compareTo(data.id));
        }
    }
}
