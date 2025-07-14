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
import tappas.DataAnnotation.TransData;
import tappas.DataApp.DataType;
import tappas.DataApp.SelectionDataResults;
import tappas.DataDIU.DSType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */

public class DataDPA extends AppObject {
    public static final String SHORT_SUFFIX = "_Short";
    public static final String LONG_SUFFIX = "_Long";
    public static final String DPA_RESULTS = "result.tsv";
    
    public static final String DPA_RESULTS_SUMMARY = "result_differentialPolyA_summary.tsv";
    public static final String DPA_DIFFPOLYAIDMAP_NAME = "diff_PolyA_id_map";
    public static final String DPA_DIFFPOLYAMATRIX_NAME = "diff_PolyA_matrix";
    public static final String DPA_DIFFPOLYAMEANMATRIX_NAME = "diff_PolyA_matrix_mean";
    public static final String DPA_DIFFPOLYAMATRIXRAW_NAME = "diff_PolyA_matrix_raw";
    public static final String DPA_DIFFPOLYAIDDATA_NAME = "diff_PolyA_id_data";
    // transcripts with TransData
    private HashMap<String, TransData> hmTransData = new HashMap<>();

    public String getDPAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DPA).toString(); }
    public String getDPAParamsFilepath() { return Paths.get(getDPAFolder(), DataApp.PRM_DPA).toString(); }
    public String getDPAGeneResultsFilepath() { return Paths.get(getDPAFolder(), DPA_RESULTS).toString(); }
    public String getDPAResultsSummaryFilepath() { return Paths.get(getDPAFolder(), DPA_RESULTS_SUMMARY).toString(); }
    public String getDPALogFilepath() { return Paths.get(getDPAFolder(), DataApp.LOG_NAME).toString(); }
    public String getDPAVennDiagFilepath(DataType dataType) { return Paths.get(getDPAFolder(), DataApp.VENNDIAG_NAME + DataApp.PNG_EXT).toString(); }
    
    public String getDPAClusterImageFilepath(String grp, int cluster) { return Paths.get(getDPAFolder(), DataApp.CLUSTER_NAME + grp + "." + cluster + DataApp.PNG_EXT).toString(); }
    public String getDPAClusterImagePrefix() { return DataApp.CLUSTER_NAME + "_"; }
    public String getDPAClusterMembersFilepath(String grp) { return Paths.get(getDPAFolder(), DataApp.CLUSTER_NAME + grp + DataApp.TSV_EXT).toString(); }
    
    public String getDPADiffPolyAIdMapFilepath() { return Paths.get(project.data.getProjectDataFolder(), DPA_DIFFPOLYAIDMAP_NAME + DataApp.TSV_EXT).toString(); }
    public String getDPADiffPolyAMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), DPA_DIFFPOLYAMATRIX_NAME + DataApp.TSV_EXT).toString(); }
    public String getDPADiffPolyAMeanMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), DPA_DIFFPOLYAMEANMATRIX_NAME + DataApp.TSV_EXT).toString(); }
    public String getDPADiffPolyAMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), DPA_DIFFPOLYAMATRIXRAW_NAME + DataApp.TSV_EXT).toString(); }
    public String getDPADiffPolyAIdDataFilepath() { return Paths.get(project.data.getProjectDataFolder(), DPA_DIFFPOLYAIDDATA_NAME + DataApp.TSV_EXT).toString(); }
    
    public DataDPA(Project project) {
        super(project, null);
    }
    public void initialize() {
        clearData();
    }
    public boolean hasDPAData() { return (Files.exists(Paths.get(getDPAGeneResultsFilepath()))); }
    public boolean hasDPASummaryData() { return (Files.exists(Paths.get(getDPAResultsSummaryFilepath()))); }
    public void clearData() {}
    
    public DlgDPAnalysis.Params getDPAParams() { 
        return DlgDPAnalysis.Params.load(getDPAParamsFilepath(), project);
    }
    
    public void clearDataDPA(boolean rmvPrm) {
        clearData();
        removeAllDPAResultFiles(rmvPrm);
    }
    public void removeAllDPAResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DPA), rmvPrms);
    }
    
    public boolean genDPAInputFiles(DlgDPAnalysis.Params dpaParams) {
        boolean result = false;

        // make sure expression factors is available
        if(!Files.exists(Paths.get(project.data.getExpFactorsFilepath())))
            project.data.copyExpFactorsFile(project.data.getExpFactorsFilepath());

        // get expression data
        HashMap<String, double[]> hmTransExp = new HashMap<>();
        DataInputMatrix.ExpMatrixData ed = project.data.getExpressionData(DataType.TRANS, new HashMap<>());
        for(DataInputMatrix.ExpMatrixArray em : ed.data)
            hmTransExp.put(em.getTranscript(), em.daSamples);
        String[] names = project.data.getGroupNames();

        // create structural file if it has not created yet
        if(!Files.exists(Paths.get(project.data.getStructuralInfoFilePath()))){
            DbAnnotation db = new DbAnnotation(project);
            db.initialize();
            if(db.openDB(project.data.getAnnotationDBFilepath())){
                if(db.getStructureFile(project.data.getProjectDataFolder(), db.getTranscriptGenomicPosition())){
                    app.logInfo("File created succesfully");
                }
            }
            db.close();
        }
        
        //Get genomic Position to PolyA reading the structural information file
        if(!Files.exists(Paths.get(project.data.getStructuralInfoFilePath()))){
            
        }
        HashMap<String, String> genomicPos = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);

        // get genes with diversity in PolyA
        HashMap<String, Object> hm = new HashMap<>();

        hm.put(dpaParams.getFeature(),null);
        boolean varyingflg = false;
        boolean multipleIso = false;
        DbProject.TransDataResults trd = project.data.loadTransData();
        hmTransData = trd.hmTransData;
        int length = dpaParams.lengthValue;
        List<String> lstDPA = new ArrayList<>();
        lstDPA.add("Gene\tTrans\tGenPos\tStrand");
        
        //gene, trans, genomicPos
        HashMap<String, HashMap<String, Object>> hmTGGeneTrans = project.data.getResultsGeneTrans();
        HashMap<String, Object> hmNMD = project.data.getTransWithAnnot("NMD");
        for(String gene : hmTGGeneTrans.keySet()) {
            //at least 2  isoforms
            HashMap<String, Object> hmTrans = hmTGGeneTrans.get(gene);
            HashMap<String, Object> hmTransFiltered = new HashMap<>();

            for(String trans : hmTrans.keySet()){
                if(project.data.isTransCoding(trans) && !hmNMD.containsKey(trans))
                    hmTransFiltered.put(trans, null);
            }
            varyingflg = !areAllGenePASSimilar(gene, hmTransFiltered, genomicPos, length);
            multipleIso = hmTransFiltered.size()>1;

            if(varyingflg && multipleIso){
                //if gene has diverse polyA order transcripts in short or long
                for(String trans : hmTransFiltered.keySet()){
                    if(genomicPos.get(trans)!=null)
                        lstDPA.add(gene + "\t" + trans + "\t" + genomicPos.get(trans) + "\t" + project.data.getGeneStrand(gene));
                }
            }else{
                continue;
            }
            
        }
        // write a info file for RScript
        if(!lstDPA.isEmpty()) {
            result = writeFile(project.data.getProjectDataFolder()+"/DPA", lstDPA);
        }
        return result;
    }
    
    
    //Save info
    private boolean writeFile(String folder, List<String> lstDPA) {
        boolean result = false;
        logger.logDebug("Writing DPA file...");
        String fp = Paths.get(folder, DataProject.DPA_ShortLong).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fp), "utf-8"));
            for(String str_info : lstDPA){
                System.out.println(str_info);
                writer.write(str_info + "\n");
            }
            logger.logDebug("Annotation DPA file written.");
            result = true;
        } catch (IOException e) {
            logger.logError("Annotation DPA file code exception: " + e.getMessage());
            result = false;
        } finally {
           try { if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception" + e.getMessage()); }
        }
        return result;
    }
    
    // all transcripts must be coding or will return false even if all are non-coding
    public boolean areAllGenePASSimilar(String gene, HashMap<String, Object> hm, HashMap<String, String> genomicPos, int length) {
        boolean result = false;
        //which is max position in genomic position? int is 2.147M!!!
        long minPOS=Long.MAX_VALUE;
        long maxPOS=0;
        for(String trans : hm.keySet()) {
            if(hmTransData.containsKey(trans)) {
                if(genomicPos.containsKey(trans)){
                    long transPASPOS = Long.parseLong(genomicPos.get(trans));
                    if(transPASPOS > maxPOS)
                        maxPOS = transPASPOS;
                    if(transPASPOS < minPOS)
                        minPOS = transPASPOS;
                }else{
                    continue;
                }
            }else {
                System.out.println("WARN: No data available for transcript " + trans);
                break;
            }
        }
        //Similar is less than that length
        if(maxPOS-minPOS<length){
            result = true;
        }
        return result;
    }
    
    public ArrayList<String> getDPAGenes(DSType dsType, double sigValue) {
        ArrayList<String> lstDS = new ArrayList<>();
        ArrayList<DPAResultsData> lstResults = getDPAResultsData(sigValue);
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
    
    public HashMap<String, HashMap<String, Object>> getDPAGeneTransFilter(DSType dsType, double sigValue) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        ArrayList<String> lst = getDPAGenes(dsType, sigValue);
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
    
    public ArrayList<DPAResultsData> getDPAResultsData(double sigValue) {
        ArrayList<DPAResultsData> lstDS = new ArrayList<>();
        DlgDPAnalysis.Params params = getDPAParams();
        try {
            HashMap<String, HashMap<String, Integer>> hmCluster = null;
            if(project.data.isTimeCourseExpType())
                hmCluster = getDPACluster();
            HashMap<String, double[]> hmMeanExp = getDPAMeanExp();
            HashMap<String, String> hmFavored = getDPAIdData();
            String[] groupNames = project.data.getGroupNames();
            int count = 0;
            if(Files.exists(Paths.get(getDPAGeneResultsFilepath()))) {
                List<String> lines = Files.readAllLines(Paths.get(getDPAGeneResultsFilepath()), StandardCharsets.UTF_8);
                
                // if the results are from edgeR we get: gene pValue qValue podiumChange totalChange
                // if the results are from DEXSeq we get: gene qValue podiumChange totalChange
                // if the results are form maSigPro we get: gene qValue podiumChange podiumTime (in single)
                String gene;
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
                            //Couse = 4/6, Single = 5, Multiple = 5
                            if(fldLength != 4 && fldLength != 5 && fldLength != 6) {
                                logger.logError("Invalid number of columns, " + fldLength + ", in DPA gene results data.");
                                break;
                            }
                        }
                        if(fields.length == fldLength) {
                            // R is sticking spaces between the tabs
                            for(int i = 0; i < fields.length; i++)
                                fields[i] = fields[i].trim();
                            int fldIdx = 0;
                            gene = fields[fldIdx++];
                            if(fldLength == 6)
                                    pValue = Double.parseDouble(fields[fldIdx++]);
                                else
                                    pValue = 0;
                            qValue = Double.parseDouble(fields[fldIdx++]);
                            // skipping favorC2 field, not used - remove from script results later!!!
                                if(fldLength == 6)
                                    fldIdx++;
                            podiumChange = Boolean.valueOf(fields[fldIdx++]);
                            totalChange = (project.data.isTimeCourseExpType())? 0.0 : Double.parseDouble(fields[fldIdx++]);
                            String timePoints = (project.data.isTimeCourseExpType())? (fields[fldIdx++].equals(".")? "" : fields[fldIdx-1]) : "";
                            String favoredTimes = (project.data.isTimeCourseExpType())? (fields[fldIdx++].equals(".")? "" : fields[fldIdx-1]) : "";
                            String geneDescription = project.data.getGeneDescription(gene);
                            String favored = "N/A";
                            if(hmFavored.containsKey(gene))
                                favored = hmFavored.get(gene);
                            else if(project.data.isTimeCourseExpType() && !favoredTimes.equals(""))
                                favored = favoredTimes;
                            double[] LongMeanExp = null;
                            double[] ShortMeanExp = null;
                            if(hmMeanExp.containsKey(gene + LONG_SUFFIX))
                                LongMeanExp = hmMeanExp.get(gene + LONG_SUFFIX);
                            if(hmMeanExp.containsKey(gene + SHORT_SUFFIX))
                                ShortMeanExp = hmMeanExp.get(gene + SHORT_SUFFIX);
                            double[] LongerConditionValues = new double[LongMeanExp.length];
                            for(int i=0; i<LongerConditionValues.length; i++){
                                if(ShortMeanExp[i]!=0){
                                    LongerConditionValues[i]=LongMeanExp[i]/(ShortMeanExp[i]+LongMeanExp[i]);
                                }else{
                                    LongerConditionValues[i]=0.0;
                                }
                            }
                            
                            int[] clus = new int[groupNames.length];
                            
                            boolean useCluster = false;
                            if(project.data.isTimeCourseExpType()){
                                useCluster = true;
                                for(int k=0; k<groupNames.length; k++){
                                    if(hmCluster.get(groupNames[k]).containsKey(gene)){
                                        clus[k]=hmCluster.get(groupNames[k]).get(gene);
                                        count++;
                                    }else{
                                        clus[k]=0;
                                    }
                                }
                            }
                            boolean timeSeries = project.data.isTimeCourseExpType();
                            lstDS.add(new DPAResultsData(params.method, gene, geneDescription, pValue, qValue, favored, LongMeanExp, ShortMeanExp, LongerConditionValues, podiumChange, timePoints, favoredTimes, totalChange, qValue < sigValue, groupNames, useCluster, clus, timeSeries));
                        }
                        else {
                            logger.logError("Invalid line, " + lnum + ", in DPA results data.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
            System.out.println(count);
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA results data: " + e.getMessage());
        }
        logger.logDebug("Returned  " + lstDS.size() + " DPA result entries");
        return lstDS;
    }
    
    public HashMap<String, String> getDPAIdData() {
        HashMap<String, String> hmData = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDPADiffPolyAIdDataFilepath()))) {
                List<String> lines = Files.readAllLines(Paths.get(getDPADiffPolyAIdDataFilepath()), StandardCharsets.UTF_8);

                // get id and favored
                String[] fields;
                int lnum = 1;
                for(String line : lines) {
                    if(!line.isEmpty() && !line.subSequence(0, 1).equals("#")) {
                        fields = line.split("\t");
                        if(fields.length == 2)
                            hmData.put(fields[0], fields[1]);
                        else {
                            logger.logError("Invalid number of columns, " + fields.length + ", in DPA featureId  data.");
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
    public HashMap<String, double[]> getDPAMeanExp() {
        HashMap<String, double[]> hmData = new HashMap<>();
        try {
            if(Files.exists(Paths.get(getDPADiffPolyAMeanMatrixFilepath()))) {
                List<String> lines = Files.readAllLines(Paths.get(getDPADiffPolyAMeanMatrixFilepath()), StandardCharsets.UTF_8);
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
                            logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in DPA mean expression matrix file.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA mean expression matrix: " + e.getMessage());
        }
        return hmData;
    }
    
    //Group - Gene - Cluster
    public HashMap<String, HashMap<String, Integer>> getDPACluster() {
        HashMap<String, HashMap<String, Integer>> hmData = new HashMap<>();
        String[] names = project.data.getGroupNames();
        try {
            for(int i=0; i<names.length; i++){
                HashMap<String, Integer> hmClusters = new HashMap<>();
                if(Files.exists(Paths.get(getDPAClusterMembersFilepath(names[i])))) {
                    List<String> lines = Files.readAllLines(Paths.get(getDPAClusterMembersFilepath(names[i])), StandardCharsets.UTF_8);
                    // get cluster info 
                    String[] fields;
                    int length = 2; //gene and cluster
                    int lnum = 1; // 1st line not count
                    for(String line : lines) {
                        if(!line.isEmpty() && lnum>1) {
                            fields = line.split("\t");
                            if(fields.length == length) {
                                hmClusters.put(fields[0], Integer.parseInt(fields[1]));
                            }
                            else {
                                logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in DPA cluster file.");
                                break;
                            }
                        }
                        lnum++;
                    }
                }
                hmData.put(names[i], hmClusters);
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA cluster file: " + e.getMessage());
        }
        return hmData;
    }

    public ObservableList<DPASelectionResults> getDPASelectionResults(double sigValue, boolean getGeneData) {
        ObservableList<DPASelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<DPAResultsData> lstDS = getDPAResultsData(sigValue);
        for(DPAResultsData dsrd : lstDS)
            lstResults.add(new DPASelectionResults(false, dsrd));
        if(getGeneData) {
            String gene;
            HashMap<String, double[]> hmMEL = project.data.getMeanExpressionLevelsHM(DataType.GENE, project.data.getResultsTrans());
            for(DPASelectionResults dr : lstResults) {
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
                        //dr.distal = new SimpleDoubleProperty[conds.length];
                        //dr.proximal = new SimpleDoubleProperty[conds.length];
                        for(int i = 0; i < conds.length; i++){
                            dr.conditions[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(conds[i]*100)/100.0))));
                            //dr.distal[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", (dr.getLongMeanExp(i)))));
                            //dr.proximal[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", (dr.getShortMeanExp(i)))));
                        }
                    }
                    else
                        logger.logWarning("Unable to find DPA expression values for gene '" + gene + "'");
                }    
            }
        }
        return lstResults;
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

    //
    // Data Classes
    //

    // DPA data for genes
    // the result values will change based on the method used for DPA - handled in DPAResults
    enum DEXSeqValues { QValue, PodiumChange, TotalChange }
    enum maSigProValues { PValue, RSQD, Cluster }
    // meanExp should be just Exp at least when dealing within a single gene - check later and change name if needed?
    public static class DPAResultsData {
        public String gene;
        public String geneDescription;
        public String source;
        public double pValue;
        public double qValue;
        public String favored;
        private DlgDPAnalysis.Params.Method method;
        
        public boolean clusters;
        public int[] clus;
        public ArrayList<DPAResultsData> lstResults;
        
        public double[] LongMeanExp, ShortMeanExp, LongerConditionValues;
        public boolean podiumChange;
        public String LongerCondition;
        public String podiumTimes;
        public String favoredTimes;
        public double totalChange;
        public boolean ds;
        //Expresion
        public double distalExpressionA;
        public double distalExpressionB;
        public double proximalExpressionA;
        public double proximalExpressionB;
        //Mean expresion
        public double distalMeanExpression;
        public double proximalMeanExpression;
     
        public DPAResultsData(DlgDPAnalysis.Params.Method method, String gene, String geneDescription,
                double pValue, double qValue, String favored, double[] LongMeanExp, double[] ShortMeanExp, double[] LongerConditionValues, boolean podiumChange, String podiumTimes, String favoredTimes, double totalChange, boolean ds, String[] groupNames, boolean clusters, int[] clus, boolean timeSeries) {
            this.method = method;
            this.gene = gene;
            this.geneDescription = geneDescription;
            this.pValue = pValue;
            this.qValue = qValue;
            this.favored = favored;
            this.LongMeanExp = LongMeanExp;
            this.ShortMeanExp = ShortMeanExp;
            this.LongerConditionValues = LongerConditionValues;
            
            for(int i=0; i<LongMeanExp.length;i++){
                if(!timeSeries){
                    if(i==0){
                    //first group
                    this.distalExpressionA = LongMeanExp[i];
                    this.proximalExpressionA = ShortMeanExp[i];
                    }else{
                    //second group
                    this.distalExpressionB = LongMeanExp[i];
                    this.proximalExpressionB = ShortMeanExp[i];
                    }
                }
                this.distalMeanExpression += LongMeanExp[i];
                this.proximalMeanExpression += ShortMeanExp[i];
            }
            
            this.distalMeanExpression = distalMeanExpression/LongMeanExp.length;
            this.proximalMeanExpression = proximalMeanExpression/LongMeanExp.length;
            
            double max=0.0;
            int ind = 0;
            for(int i=0; i<LongerConditionValues.length;i++){
                if(LongerConditionValues[i]>max){
                    max=LongerConditionValues[i];
                    ind=i;
                }
            }
            
            if(!clusters){
                //case-control
                this.LongerCondition = groupNames[ind];
            }else{
                //time series
                this.LongerCondition = "T"+String.valueOf(ind);
            }
            
            this.podiumChange = podiumChange;
            this.podiumTimes = podiumTimes;
            this.favoredTimes = favoredTimes;
            this.totalChange = totalChange;
            this.clusters = clusters;
            this.clus = clus;
            this.ds = ds;
            
            this.source = STRUCTURAL_SOURCE;
            lstResults = new ArrayList<>();
        }
        public void setDSFlag(double sigValue) {
            if(qValue < sigValue)
                ds = true;
            else
                ds = false;
        }
        
        public HashMap<String, DPAResultsData> getHMGene() {
            HashMap<String, DPAResultsData> hm = new HashMap<>();
            for(DPAResultsData rd : lstResults)
                hm.put(rd.gene, rd);
            return hm;
        }
        
        public ArrayList<DataApp.RankedListEntry> getRankedList() {
            ArrayList<DataApp.RankedListEntry> lst = new ArrayList<>();
            double value;
            for(DPAResultsData rd : lstResults) {
                if(rd.pValue!=0) // check this!!!
                    value = rd.pValue;
                else
                    value = 1.0;
                lst.add(new DataApp.RankedListEntry(rd.gene, value));
            }
            Collections.sort(lst);
            return lst;
        }
        public DlgDPAnalysis.Params.Method getMethod() { return method; }
        public void addResultData(DPAResultsData rd) { lstResults.add(rd); }
    }
    // specific analysis results
    public static class DPASelectionResults extends SelectionDataResults implements Comparable<DPASelectionResults> {
        static final double MAX_L2FC = 100.0;
        
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleStringProperty coding = null;
        public SimpleStringProperty source = null;
        public SimpleStringProperty favored = null;

        // these values are normally based on the experimental group
        // but in the case of single series time course
        // they are based on time slots
        public final SimpleDoubleProperty[] LongMeanExp;
        public final SimpleDoubleProperty[] ShortMeanExp;
        public final SimpleDoubleProperty[] LongerConditionValues;
        public final SimpleDoubleProperty[] L2FC;
        public SimpleDoubleProperty zero;
        public SimpleDoubleProperty pValue = null;
        public SimpleDoubleProperty qValue = null;
        public SimpleDoubleProperty totalChange = null;
        public SimpleStringProperty podiumChange = null;
        public SimpleStringProperty podiumTimes = null;
        public SimpleStringProperty favoredTimes = null;
        public SimpleStringProperty LongerCondition = null;
        public SimpleStringProperty ds = null;
        //Expression Levels Two-Group Comparison
        public SimpleDoubleProperty  distalExpressionA = null;
        public SimpleDoubleProperty  distalExpressionB = null;
        public SimpleDoubleProperty  proximalExpressionA = null;
        public SimpleDoubleProperty  proximalExpressionB = null;
        //MeanExpresion
        public SimpleDoubleProperty distalMeanExpression = null;
        public SimpleDoubleProperty proximalMeanExpression = null;
        
        public final SimpleStringProperty[] clusterCmp;
        public final SimpleStringProperty cluster;
        
        public DPASelectionResults(boolean selected, DPAResultsData dsra) {
            super(selected, DataType.GENE, dsra.gene, dsra.gene, dsra.geneDescription);
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.favored = new SimpleStringProperty(dsra.favored);
            if(dsra.LongMeanExp != null && dsra.ShortMeanExp != null && dsra.LongMeanExp.length == dsra.ShortMeanExp.length) {
                // Long
                int cnt = dsra.LongMeanExp.length;
                this.LongMeanExp = new SimpleDoubleProperty[cnt];
                this.distal = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++){
                    this.LongMeanExp[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.LongMeanExp[i])));
                    //extends SelectionDataResults to get column explevel
                    this.distal[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.LongMeanExp[i])));
                }
                // Short
                cnt = dsra.ShortMeanExp.length;
                this.ShortMeanExp = new SimpleDoubleProperty[cnt];
                this.proximal = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++){
                    this.ShortMeanExp[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.ShortMeanExp[i])));
                    //extends SelectionDataResults to get column explevel
                    this.proximal[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.ShortMeanExp[i])));
                }
                // LongerConditionValues
                cnt = dsra.LongerConditionValues.length;
                this.LongerConditionValues = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++){
                    this.LongerConditionValues[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.LongerConditionValues[i])));
                }
                // L2FC
                this.L2FC = new SimpleDoubleProperty[cnt];
                for(int i = 0; i < cnt; i++) {
                    // all expression values are obviously positive, 0 or greater
                    if(this.ShortMeanExp[i].get() > 0.0) {
                        double fc = this.LongMeanExp[i].get() / this.ShortMeanExp[i].get();
                        if(fc > 0.0) {
                            double l2fc = Math.log(fc)/Math.log(2);
                            this.L2FC[i] = new SimpleDoubleProperty(Double.parseDouble(formatter.format(l2fc)));
                        }
                        else {
                            // LongMeanExp must be 0
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
                this.LongMeanExp = null;
                this.ShortMeanExp = null;
                this.LongerConditionValues = null;
                this.L2FC = null;
            }            

            this.pValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.pValue)));
            this.qValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.qValue)));
            this.podiumChange = new SimpleStringProperty(dsra.podiumChange? "YES" : "NO");
            this.podiumTimes = new SimpleStringProperty(dsra.podiumTimes);
            this.LongerCondition = new SimpleStringProperty(dsra.LongerCondition);
            this.favoredTimes = new SimpleStringProperty(dsra.favoredTimes);
            this.totalChange = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(dsra.totalChange*100)/100.0))));
            this.ds = new SimpleStringProperty(dsra.ds? "DPA" : "Not DPA");
            //Expression Levels
            this.distalExpressionA = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.distalExpressionA)));
            this.distalExpressionB = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.distalExpressionB)));
            this.proximalExpressionA = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.proximalExpressionA)));
            this.proximalExpressionB = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.proximalExpressionB)));
            //Mean expression
            this.distalMeanExpression = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.distalMeanExpression)));
            this.proximalMeanExpression = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.proximalMeanExpression)));
            // cluster is only valid in time course series - will need to change if other than maSigPro
            if(dsra.clusters){
                // Clus Value
                int cnt = dsra.clus.length;
                this.clusterCmp = new SimpleStringProperty[cnt];
                if(!String.valueOf(dsra.clus[0]).equals("0"))
                    this.cluster = new SimpleStringProperty(String.valueOf(dsra.clus[0]));
                else
                    this.cluster = new SimpleStringProperty("");
                for(int i = 0; i < cnt; i++)
                    if(!String.valueOf(dsra.clus[i]).equals("0"))
                        this.clusterCmp[i] = new SimpleStringProperty(String.valueOf(dsra.clus[i]));
                    else
                        this.clusterCmp[i]=new SimpleStringProperty("");
            }else{
                this.cluster = null;
                this.clusterCmp = null;
            }
        }
        
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public String getCoding() { return coding.get(); }
        public String getFavored() { return favored.get(); }

        public Double getL2FC(int idx) { 
            double value = 0.0;
            if(L2FC != null && idx < L2FC.length)
                value = L2FC[idx].get();
            return value; 
        }
        public Double getLongMeanExp(int idx) {
            double value = 0.0;
            if(LongMeanExp != null && idx < LongMeanExp.length)
                value = LongMeanExp[idx].get();
            return value; 
        }
        public Double getShortMeanExp(int idx) {
            double value = 0.0;
            if(ShortMeanExp != null && idx < ShortMeanExp.length)
                value = ShortMeanExp[idx].get();
            return value;
        }
        public Double getLongerConditionValues(int idx) {
            double value = 0.0;
            if(LongerConditionValues != null && idx < LongerConditionValues.length)
                value = LongerConditionValues[idx].get();
            return value;
        }
        
        public Double getPValue() { return pValue.get(); }
        public Double getQValue() { return qValue.get(); }
        public String getPodiumChange() { return podiumChange.get(); }
        public String getPodiumTimes() { return podiumTimes.get(); }
        public String getLongerCondition() { return LongerCondition.get(); }
        public String getFavoredTimes() { return favoredTimes.get(); }
        public Double getTotalChange() { return totalChange.get(); }
        public String getDS() { return ds.get(); }
        
        public Double getDistalExpressionA() { return distalExpressionA.get(); }
        public Double getDistalExpressionB() { return distalExpressionB.get(); }
        public Double getProximalExpressionA() { return proximalExpressionA.get(); }
        public Double getProximalExpressionB() { return proximalExpressionB.get(); }
        
        public Double getDistalMeanExpression() { return distalMeanExpression.get(); }
        public Double getProximalMeanExpression() { return proximalMeanExpression.get(); }
        
        public String getCluster() { return cluster.get(); }
        //public String getDECmp1() { return deCmp[1].get(); }
        public String getClusterCmp1() { return clusterCmp[1].get(); }
        //public String getDECmp2() { return deCmp[2].get(); }
        public String getClusterCmp2() { return clusterCmp[2].get(); }
        //public String getDECmp3() { return deCmp[3].get(); }
        public String getClusterCmp3() { return clusterCmp[3].get(); }
        
        
        @Override
        public int compareTo(DPASelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
}
