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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */

public class DataUTRL extends AppObject {
    public static final String UTRL_RESULTS = "result.tsv";
    public static final String UTRL_RESULTS_SUMMARY = "result_UTRL_summary.tsv";
    // transcripts with TransData
    private HashMap<String, TransData> hmTransData = new HashMap<>();

    public String getUTRLFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_UTRL).toString(); }
    public String getUTRLParamsFilepath() { return Paths.get(getUTRLFolder(), DataApp.PRM_UTRL).toString(); }
    public String getUTRLGeneResultsFilepath() { return Paths.get(getUTRLFolder(), UTRL_RESULTS).toString(); }
    public String getUTRLResultsSummaryFilepath() { return Paths.get(getUTRLFolder(), UTRL_RESULTS_SUMMARY).toString(); }
    public String getUTRLLogFilepath() { return Paths.get(getUTRLFolder(), DataApp.LOG_NAME).toString(); }
    public String getUTRLVennDiagFilepath(DataType dataType) { return Paths.get(getUTRLFolder(), DataApp.VENNDIAG_NAME + DataApp.PNG_EXT).toString(); }
    
    public String getUTRLClusterImageFilepath(String grp, int cluster) { return Paths.get(getUTRLFolder(), DataApp.CLUSTER_NAME + grp + "." + cluster + DataApp.PNG_EXT).toString(); }
    public String getUTRLClusterImagePrefix() { return DataApp.CLUSTER_NAME + "_"; }
    public String getUTRLClusterMembersFilepath(String grp) { return Paths.get(getUTRLFolder(), DataApp.CLUSTER_NAME + grp + DataApp.TSV_EXT).toString(); }

    public DataUTRL(Project project) {
        super(project, null);
    }
    public void initialize() {
        clearData();
    }
    public boolean hasUTRLData() { return (Files.exists(Paths.get(getUTRLGeneResultsFilepath()))); }
    public boolean hasUTRLSummaryData() { return (Files.exists(Paths.get(getUTRLResultsSummaryFilepath()))); }
    public void clearData() {}
    
    public DlgUTRLAnalysis.Params getUTRLParams() {
        return DlgUTRLAnalysis.Params.load(getUTRLParamsFilepath(), project);
    }
    
    public void clearDataUTRL(boolean rmvPrm) {
        clearData();
        removeAllUTRLResultFiles(rmvPrm);
    }
    public void removeAllUTRLResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_UTRL), rmvPrms);
    }
    
    public boolean genUTRLInputFiles(DlgUTRLAnalysis.Params utrlParams) {
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
        Files.exists(Paths.get(project.data.getStructuralInfoFilePath()));
        
        HashMap<String, String> genomicPos3 = new HashMap<>();
        HashMap<String, String> genomicPos5 = new HashMap<>();
        //if(utrlParams.utr.name().equals(utrlParams.utr.UTR3.name())) //UTR3
            genomicPos3 = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),1,true);
        //else //UTR5
            genomicPos5 = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),2,true);

        // get genes with diversity in UTR
        HashMap<String, Object> hm = new HashMap<>();
        hm.put(utrlParams.getFeature(),null);
        boolean varyingflg3 = false;
        boolean multipleIso = false;
        DbProject.TransDataResults trd = project.data.loadTransData();
        hmTransData = trd.hmTransData;
        int length = utrlParams.lengthValue;
        List<String> lstUTRL = new ArrayList<>();
        String header = "";

        //we need to add the mean expression for each group
        if(project.data.isCaseControlExpType()){
            header = "Gene\tTrans\tUTR3\tUTR5\tStrand";
            int group = project.data.getGroupsCount();
            for(int i = 0; i < group; i++)
                header = header + "\t" + project.data.getGroupNames()[i];
            lstUTRL.add(header);
        }else if(project.data.isTimeCourseExpType()) {
            header = "Gene\tTrans\tUTR3\tUTR5";
            //just time points
            //int times = project.data.getTimePoints();
            //for(int i = 0; i < times; i++)
            //    header = header + "\t" + project.data.getTimePointNames()[i];
            lstUTRL.add(header);
        }

        //gene, trans, length
        HashMap<String, HashMap<String, Object>> hmTGGeneTrans = project.data.getResultsGeneTrans();
        
        for(String gene : hmTGGeneTrans.keySet()) {
            //at least 2  isoforms
            varyingflg3 = !areAllGeneUTRSimilar(gene, hmTGGeneTrans.get(gene), genomicPos3, length);
            //varyingflg5 = !areAllGeneUTRSimilar(gene, hmTGGeneTrans.get(gene), genomicPos5, length);
            multipleIso = hmTGGeneTrans.get(gene).size()>1;
            // vaying in polyA (transcripts are aligned and 3' is the polyA site for all of them, including - strand)
            if(multipleIso && varyingflg3){
                HashMap<String, Object> hmFilterTrans = new HashMap<>();
                HashMap<String, DataProject.TransExpLevels> hmExp = new HashMap<>();
                HashMap<String, double[]> hmExpSample = new HashMap<>();
                if(project.data.isCaseControlExpType()){
                    hmExp = project.data.getGeneIsosExpLevels(gene, hmFilterTrans);
                    double totalExpression1 = 0;
                    double totalExpression2 = 0;
                    for(String iso : hmExp.keySet()){
                        totalExpression1 += hmExp.get(iso).X1_mean;
                        totalExpression2 += hmExp.get(iso).X2_mean;
                    }
                    //if gene has diverse UTR order transcripts in short or long
                    for(String trans : hmTGGeneTrans.get(gene).keySet()){
                        double prop1 = 0;
                        double prop2 = 0;
                        prop1 = hmExp.get(trans).X1_mean/totalExpression1;
                        prop2 = hmExp.get(trans).X2_mean/totalExpression2;
                        //if(genomicPos3.get(trans)!=null && genomicPos5.get(trans)!=null)
                        lstUTRL.add(gene + "\t" + trans + "\t" + (genomicPos3.containsKey(trans)? genomicPos3.get(trans) : 0) + "\t" + (genomicPos5.containsKey(trans)? genomicPos5.get(trans) : 0) + "\t" + project.data.getGeneStrand(gene) + "\t" + prop1 + "\t" + prop2);
                    }
                }else if(project.data.isTimeCourseExpType()){
                    //For time series the proportion and the polyA is calc in R - just pass gene-transcript-utr3 and 5 length
                    for(String trans : hmTGGeneTrans.get(gene).keySet()){
                        lstUTRL.add(gene + "\t" + trans + "\t" + (genomicPos3.containsKey(trans)? genomicPos3.get(trans) : 0) + "\t" + (genomicPos5.containsKey(trans)? genomicPos5.get(trans) : 0));
                    }
                }

            }else{
                continue;
            }
            
        }
        // write a info file for RScript
        if(!lstUTRL.isEmpty()) {
            result = writeFile(project.data.getProjectDataFolder()+"/UTRL", lstUTRL);
        }
        return result;
    }
    
    
    //Save info
    private boolean writeFile(String folder, List<String> lstUTRL) {
        boolean result = false;
        logger.logDebug("Writing UTRL file...");
        String fp = Paths.get(folder, DataProject.UTRL_ShortLong).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fp), "utf-8"));
            for(String str_info : lstUTRL){
                System.out.println(str_info);
                writer.write(str_info + "\n");
            }
            logger.logDebug("Annotation UTRL file written.");
            result = true;
        } catch (IOException e) {
            logger.logError("Annotation UTRL file code exception: " + e.getMessage());
            result = false;
        } finally {
           try { if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception" + e.getMessage()); }
        }
        return result;
    }
    
    // all transcripts must be coding or will return false even if all are non-coding
    public boolean areAllGeneUTRSimilar(String gene, HashMap<String, Object> hm, HashMap<String, String> genomicPos, int length) {
        boolean result = false;
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
    
    public ArrayList<String> getUTRLGenes() {
        ArrayList<String> lst = new ArrayList<>();
        ArrayList<UTRLResultsData> lstResults = getUTRLResultsData();
        lstResults.forEach((dsar) -> {
            lst.add(dsar.gene);
        });
        return lst;
    }
    
    public HashMap<String, HashMap<String, Object>> getUTRLGeneTransFilter() {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        ArrayList<String> lst = getUTRLGenes();
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

    //Case control
    public ArrayList<UTRLResultsData> getUTRLResultsData() {
        ArrayList<UTRLResultsData> lstDS = new ArrayList<>();
        DlgUTRLAnalysis.Params params = getUTRLParams();
        try {
            HashMap<String, HashMap<String, Integer>> hmCluster = null;
            if(project.data.isTimeCourseExpType())
                hmCluster = getUTRLCluster();

            String[] aux = project.data.getGroupNames();
            String[] groupNames = new String[aux.length*2];
            for(int i = 0; i<groupNames.length;i=i+2){
                for(int j = 0; j<aux.length;j++){
                    groupNames[i]=aux[j]+"_UTR3";
                    groupNames[i+1]=aux[j]+"_UTR5";
                }
            }

            int count = 0;
            if(Files.exists(Paths.get(getUTRLGeneResultsFilepath()))) {
                List<String> lines = Files.readAllLines(Paths.get(getUTRLGeneResultsFilepath()), StandardCharsets.UTF_8);
                
                // if the results are from edgeR we get: gene pValue qValue podiumChange totalChange
                // if the results are from DEXSeq we get: gene qValue podiumChange totalChange

                // if the results are from wilcoxon!!!
                // if the results are form maSigPro we get: gene qValue podiumChange podiumTime (in single)
                String gene;
                String[] fields;
                int lnum = 1;
                int fldLength = -1;
                for(String line : lines) {
                    if(lnum > 1) {
                        fields = line.split("\t");
                        if(fldLength == -1) {
                            fldLength = fields.length;
                            //Two-Group Comparison = project.data.getGroupNames().length*2+1
                            //Single = 5
                            // Multiple = 5
                            if(project.data.isCaseControlExpType() && fldLength != project.data.getGroupNames().length*2+1) {
                                logger.logError("Invalid number of columns, " + fldLength + ", in UTRL gene results data.");
                                break;
                            }else if(fldLength != 5){
                                logger.logError("Invalid number of columns, " + fldLength + ", in UTRL gene results data.");
                                break;
                            }
                        }
                        if(fields.length == fldLength) {
                            if(project.data.isCaseControlExpType()) {
                                // R is sticking spaces between the tabs
                                for (int i = 0; i < fields.length; i++)
                                    fields[i] = fields[i].trim();
                                int fldIdx = 0;
                                gene = fields[fldIdx++];
                                boolean casecontrol = project.data.isCaseControlExpType();
                                double[] expr = new double[fields.length - 1];
                                for (int j = 1; j < fields.length; j++)
                                    expr[j - 1] = Double.parseDouble(fields[j]);

                                String geneDescription = project.data.getGeneDescription(gene);

                                int[] clus = new int[groupNames.length];

                                boolean useCluster = false;
                                if (project.data.isTimeCourseExpType()) {
                                    useCluster = true;
                                    for (int k = 0; k < groupNames.length; k++) {
                                        if (hmCluster.get(groupNames[k]).containsKey(gene)) {
                                            clus[k] = hmCluster.get(groupNames[k]).get(gene);
                                            count++;
                                        } else {
                                            clus[k] = 0;
                                        }
                                    }
                                }

                                lstDS.add(new UTRLResultsData(params.method, gene, geneDescription, expr, useCluster, clus, casecontrol));
                            }else{
                                // R is sticking spaces between the tabs
                                for(int i = 0; i < fields.length; i++)
                                    fields[i] = fields[i].trim();
                                int fldIdx = 0;
                                gene = fields[fldIdx++];
                                boolean casecontrol = project.data.isCaseControlExpType();
                                double[] expr = new double[fields.length-1];
                                for(int j = 1; j<fields.length; j++)
                                    expr[j-1] = Double.parseDouble(fields[j]);

                                String geneDescription = project.data.getGeneDescription(gene);

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
                                lstDS.add(new UTRLResultsData(params.method, gene, geneDescription, expr, useCluster, clus, casecontrol));
                            }
                        }else {
                            logger.logError("Invalid line, " + lnum + ", in UTRL results data.");
                            break;
                        }
                    }
                    lnum++;
                }
            }
            System.out.println(count);
        }
        catch (Exception e) {
            logger.logError("Unable to load UTRL results data: " + e.getMessage());
        }
        logger.logDebug("Returned  " + lstDS.size() + " UTRL result entries");
        return lstDS;
    }

    //Group - Gene - Cluster
    public HashMap<String, HashMap<String, Integer>> getUTRLCluster() {
        HashMap<String, HashMap<String, Integer>> hmData = new HashMap<>();
        String[] aux = project.data.getGroupNames();
        String[] names = new String[aux.length*2];
        for(int i = 0; i<names.length;i=i+2){
            for(int j = 0; j<aux.length;j++){
                names[i]=aux[j]+"_UTR3";
                names[i+1]=aux[j]+"_UTR5";
            }
        }
        try {
            for(int i=0; i<names.length; i++){
                HashMap<String, Integer> hmClusters = new HashMap<>();
                if(Files.exists(Paths.get(getUTRLClusterMembersFilepath(names[i])))) {
                    List<String> lines = Files.readAllLines(Paths.get(getUTRLClusterMembersFilepath(names[i])), StandardCharsets.UTF_8);
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
                                logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in UTRL cluster file.");
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
            logger.logError("Unable to load UTRL cluster file: " + e.getMessage());
        }
        return hmData;
    }

    public ObservableList<UTRLSelectionResults> getUTRLSelectionResults(boolean getGeneData) {
        ObservableList<UTRLSelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<UTRLResultsData> lstDS = getUTRLResultsData();
        for(UTRLResultsData dsrd : lstDS)
            lstResults.add(new UTRLSelectionResults(false, dsrd));
        if(getGeneData) {
            String gene;
            HashMap<String, double[]> hmMEL = project.data.getMeanExpressionLevelsHM(DataType.GENE, project.data.getResultsTrans());
            for(UTRLSelectionResults dr : lstResults) {
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
                        logger.logWarning("Unable to find UTRL expression values for gene '" + gene + "'");
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

    // UTRL data for genes
    // the result values will change based on the method used for UTRL - handled in UTRLResults
    enum DEXSeqValues { QValue, PodiumChange, TotalChange }
    enum maSigProValues { PValue, RSQD, Cluster }
    // meanExp should be just Exp at least when dealing within a single gene - check later and change name if needed?
    public static class UTRLResultsData {
        public String gene;
        public String geneDescription;
        private DlgUTRLAnalysis.Params.Method method;

        public boolean clusters;
        public int[] clus;
        public ArrayList<UTRLResultsData> lstResults;
        
        public double GRPA3;
        public double GRPB3;
        public double GRPA5;
        public double GRPB5;

        public double PVALUTR3;
        public double ADJPVALUTR3;
        public double PVALUTR5;
        public double ADJPVALUTR5;

        public double[] expr;
        public boolean casecontrol;


        public UTRLResultsData(DlgUTRLAnalysis.Params.Method method, String gene, String geneDescription, double[] expr, boolean clusters, int[] clus, boolean casecontrol){
            this.method = method;
            this.gene = gene;
            this.geneDescription = geneDescription;
            this.casecontrol = casecontrol;
            if(casecontrol) {
                this.expr = expr;
                this.GRPA3 = expr[0];
                this.GRPB3 = expr[1];
                this.GRPA5 = expr[2];
                this.GRPB5 = expr[3];
            }else{
                this.expr = expr;
                this.PVALUTR3 = expr[0];
                this.ADJPVALUTR3 = expr[1];
                this.PVALUTR5 = expr[2];
                this.ADJPVALUTR5 = expr[3];
            }
            this.clusters = clusters;
            this.clus = clus;

            lstResults = new ArrayList<>();
        }
        
        public HashMap<String, UTRLResultsData> getHMGene() {
            HashMap<String, UTRLResultsData> hm = new HashMap<>();
            for(UTRLResultsData rd : lstResults)
                hm.put(rd.gene, rd);
            return hm;
        }

        public DlgUTRLAnalysis.Params.Method getMethod() { return method; }
        public void addResultData(UTRLResultsData rd) { lstResults.add(rd); }
    }
    // specific analysis results
    public static class UTRLSelectionResults extends SelectionDataResults implements Comparable<UTRLSelectionResults> {
        static final double MAX_L2FC = 100.0;
        
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleStringProperty coding = null;

        public double[] expr = null;
        // these values are normally based on the experimental group
        // but in the case of single series time course
        // they are based on time slots
        // Expression Levels Two-Group Comparison
        public SimpleDoubleProperty GRPA3 = null;
        public SimpleDoubleProperty GRPB3 = null;
        public SimpleDoubleProperty GRPA5 = null;
        public SimpleDoubleProperty GRPB5 = null;

        //time series
        public SimpleDoubleProperty PVALUTR3 = null;
        public SimpleDoubleProperty ADJPVALUTR3 = null;
        public SimpleDoubleProperty PVALUTR5 = null;
        public SimpleDoubleProperty ADJPVALUTR5 = null;

        public final SimpleStringProperty[] clusterCmp;
        public final SimpleStringProperty cluster;

        
        public UTRLSelectionResults(boolean selected, UTRLResultsData dsra) {
            super(selected, DataType.GENE, dsra.gene, dsra.gene, dsra.geneDescription);
            DecimalFormat formatter = new DecimalFormat("#.####E0");

            expr = dsra.expr;
            //Expression Levels
            if(dsra.casecontrol) {
                GRPA3 = new SimpleDoubleProperty(dsra.GRPA3);
                GRPB3 = new SimpleDoubleProperty(dsra.GRPB3);
                GRPA5 = new SimpleDoubleProperty(dsra.GRPA5);
                GRPB5 = new SimpleDoubleProperty(dsra.GRPB5);
            }else{
                PVALUTR3 = new SimpleDoubleProperty(dsra.PVALUTR3);
                ADJPVALUTR3 = new SimpleDoubleProperty(dsra.ADJPVALUTR3);
                PVALUTR5 = new SimpleDoubleProperty(dsra.PVALUTR5);
                ADJPVALUTR5 = new SimpleDoubleProperty(dsra.ADJPVALUTR5);
            }

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
        
        public double getPVALUTR3(){ return PVALUTR3.getValue();}
        public double getADJPVALUTR3(){ return ADJPVALUTR3.getValue();}
        public double getPVALUTR5(){ return PVALUTR5.getValue();}
        public double getADJPVALUTR5(){ return ADJPVALUTR5.getValue();}

        public double getGRPA3(){ return GRPA3.getValue();}
        public double getGRPB3(){ return GRPB3.getValue();}
        public double getGRPA5(){ return GRPA5.getValue();}
        public double getGRPB5(){ return GRPB5.getValue();}

        public String getCluster() { return cluster.get(); }
        //public String getDECmp1() { return deCmp[1].get(); }
        public String getClusterCmp1() { return clusterCmp[1].get(); }
        //public String getDECmp2() { return deCmp[2].get(); }
        public String getClusterCmp2() { return clusterCmp[2].get(); }
        //public String getDECmp3() { return deCmp[3].get(); }
        public String getClusterCmp3() { return clusterCmp[3].get(); }

        @Override
        public int compareTo(UTRLSelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
}
