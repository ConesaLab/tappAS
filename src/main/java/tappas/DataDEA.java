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

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu  & Pedro Salguero - psalguero@cipf.es
 */

public class DataDEA extends AppObject {
    public String getDEAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DEA).toString(); }
    public String getDEALogFilepath(DataType dataType) { return Paths.get(getDEAFolder(), DataApp.LOG_PREFIX + dataType.name().toLowerCase() + DataApp.LOG_EXT).toString(); }
    public String getDEADoneFilepath(DataType dataType) { return Paths.get(getDEAFolder(), DataApp.DONE_NAME + dataType.name().toLowerCase() + DataApp.TEXT_EXT).toString(); }
    public String getDEAResultsFilepath(DataType type) { return Paths.get(getDEAFolder(), DataApp.RESULTS_NAME + type.name().toLowerCase() + DataApp.TSV_EXT).toString(); }
    public String getDEAResultsPrefix(DataType dataType) { return DataApp.RESULTS_NAME + dataType.name().toLowerCase(); }
    public String getDEAParamsFilepath(DataType dataType) { return Paths.get(getDEAFolder(), dataType.name().toLowerCase() + DataApp.PRM_EXT).toString(); }
    public String getDEAClusterImageFilepath(DataType dataType, String grp, int cluster) { return Paths.get(getDEAFolder(), DataApp.CLUSTER_NAME + dataType.name().toLowerCase() + "_" + grp + "." + cluster + DataApp.PNG_EXT).toString(); }
    public String getDEAClusterImagePrefix(DataType dataType) { return DataApp.CLUSTER_NAME + dataType.name().toLowerCase() + "_"; }
    public String getDEAClusterMembersFilepath(DataType dataType, String grp) { return Paths.get(getDEAFolder(), DataApp.CLUSTER_NAME + dataType.name().toLowerCase() + "_" + grp + DataApp.TSV_EXT).toString(); }
    public String getDEAVennDiagPrefix(DataType dataType) { return DataApp.VENNDIAG_NAME + dataType.name().toLowerCase(); }
    public String getDEAVennDiagFilepath(DataType dataType) { return Paths.get(getDEAFolder(), DataApp.VENNDIAG_NAME + dataType.name().toLowerCase() + DataApp.PNG_EXT).toString(); }
    public String getDEAResultsFilepath(DataType dataType, String grp) { if(grp.isEmpty()) return getDEAResultsFilepath(dataType); else return Paths.get(getDEAFolder(), DataApp.RESULTS_NAME + dataType.name().toLowerCase() + "_" + grp + DataApp.TSV_EXT).toString(); }
    
    public DataDEA(Project project) {
        super(project, null);
    }
    // initialization function
    public void initialize() {
        clearData();
    }
    public void clearData() {
    }
    public void clearData(DataType type) {
    }
    public boolean hasDEAData() { return (hasDEAData(DataApp.DataType.TRANS) || hasDEAData(DataApp.DataType.PROTEIN) || hasDEAData(DataApp.DataType.GENE)); }
    public boolean hasDEAData(DataType dataType) { 
        if(project.data.isTimeCourseExpType())
            return (Files.exists(Paths.get(getDEADoneFilepath(dataType))) && hasAnyResults(dataType));
        else
            return (Files.exists(Paths.get(getDEADoneFilepath(dataType))) && Files.exists(Paths.get(getDEAResultsFilepath(dataType))));
    }
    public void setDEAParams(HashMap<String, String> hmp, DataType dataType) { 
        if(hmp != null)
            Utils.saveParams(hmp, getDEAParamsFilepath(dataType));
    }
    public DlgDEAnalysis.Params getDEAParams(DataType dataType) { 
        return DlgDEAnalysis.Params.load(getDEAParamsFilepath(dataType), project);
    }
    public void clearDataDEA(boolean rmvPrm) {
        clearData();
        removeAllDEAResultFiles(rmvPrm);
    }
    public void clearDataDEA(DataType type, boolean rmvPrm) {
        clearData(type);
        removeDEAResultFiles(type, rmvPrm);
    }
    public void removeAllDEAResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DEA), rmvPrms);
    }
    public void removeDEAResultFiles(DataType type, boolean rmvPrms) {
        Utils.removeFile(Paths.get(getDEAResultsFilepath(type)));
        Utils.removeFile(Paths.get(getDEADoneFilepath(type)));
        Utils.removeFolderFilesStartingWith(Paths.get(getDEAFolder()), getDEAVennDiagPrefix(type));
        Utils.removeFolderFilesStartingWith(Paths.get(getDEAFolder()), getDEAResultsPrefix(type));
        Utils.removeFolderFilesStartingWith(Paths.get(getDEAFolder()), getDEAClusterImagePrefix(type));
        
        // remove log file and parameters if requested
        Utils.removeFile(Paths.get(getDEALogFilepath(type)));
        if(rmvPrms)
            Utils.removeFile(Paths.get(getDEAParamsFilepath(type)));
    }
   
    public boolean genDEAInputFiles(DataType dataType) {
        boolean result = false;
        if(!Files.exists(Paths.get(project.data.getExpFactorsFilepath())))
            project.data.copyExpFactorsFile(project.data.getExpFactorsFilepath());
        if(!project.data.getExperimentType().equals(DataApp.ExperimentType.Two_Group_Comparison)) {
            if(!Files.exists(Paths.get(project.data.getTimeFactorsFilepath())))
                project.data.copyTimeFactorsFile(project.data.getTimeFactorsFilepath());
        }
        if(Files.exists(Paths.get(project.data.getExpFactorsFilepath()))) {
            if(project.data.genExpressionRawFile(dataType, false) && project.data.genExpressionFile(dataType, false)){
                switch(dataType) {
                    case PROTEIN:
                    // must use gene_protein ids for DE filtering
                    if(project.data.genGeneProteinsFile())
                        result = true;
                    break;
                }
            }
            result = true;
        }
            
        return result;
    }
    public HashMap<String, Integer> getDEAClusters(DataApp.ExperimentType expType, DataType dataType, String grpname) {
        HashMap<String, Integer> hmClusters = new HashMap<>();
        try {
            String filepath = getDEAClusterMembersFilepath(dataType, grpname);
            if(Files.exists(Paths.get(filepath)))
                hmClusters = Utils.loadSingleIntTSVListFromFile(filepath, true);
        }
        catch (Exception e) {
            hmClusters.clear();
            logger.logError("Unable to load DEA cluster members results data: " + e.getMessage());
        }
        return hmClusters;
    }
    // returns the actual DEA results returned by the R script but will change fold change
    public DEAResults getDEAResults(DataType type, String groupName, double sigValue, double FCValue) {
        DecimalFormat formatter = new DecimalFormat("#.####E0");
        HashMap<String, Integer> hmClusters = new HashMap<>();
        DlgDEAnalysis.Params params = getDEAParams(type);
        DataApp.ExperimentType expType = project.data.getExperimentType();
        if(groupName == null)
            groupName = "";
        int expLength = 5;
        int idxItem = -1;
        HashMap<String, Object> hmItems = new HashMap<>();
        switch(expType) {
            case Time_Course_Single:
                expLength = 3;
                idxItem = 0;
                if(groupName.isEmpty())
                    groupName = project.data.getGroupNames()[0];
                hmClusters = getDEAClusters(expType, type, groupName);
                break;
            case Time_Course_Multiple:
                expLength = 3;
                idxItem = 0;
                if(groupName.isEmpty())
                    groupName = project.data.getGroupNames()[0];
                hmClusters = getDEAClusters(expType, type, groupName);
                break;
            case Two_Group_Comparison:
                groupName = "";
                break;
        }
        DEAResults results = new DEAResults(params.method, sigValue);
        try {
            String filepath = getDEAResultsFilepath(type, groupName);
            switch(type) {
                case GENE:
                    if(params.method.equals(DlgDEAnalysis.Params.Method.MASIGPRO))
                        hmItems = project.data.getResultsGenes();
                    break;
                case PROTEIN:
                    if(params.method.equals(DlgDEAnalysis.Params.Method.MASIGPRO))
                        hmItems = project.data.getResultsProteins();
                    break;
                case TRANS:
                    if(params.method.equals(DlgDEAnalysis.Params.Method.MASIGPRO))
                        hmItems = project.data.getResultsTrans();
                    break;
            }
            if(Files.exists(Paths.get(filepath))) {
                String item;
                String[] fields;
                boolean de, upreg;
                double[] values;
                double log2FC;
                int idx = 0;
                int fldLength = -1;
                DEAResultsData rd;
                HashMap<String, Object> hmItemsAdded = new HashMap<>();
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    if(idx > 0) {
                        fields = line.split("\t");
                        if(fldLength == -1) {
                            fldLength = fields.length;
                            if(fldLength == expLength)
                                results = new DEAResults(params.method, sigValue);
                            else {
                                logger.logError("Invalid number of columns, " + fldLength + ", in DEA results data.");
                                break;
                            }
                        }
                        if(fields.length == fldLength) {
                            // R is sticking spaces between the tabs
                            for(int i = 0; i < fields.length; i++)
                                fields[i] = fields[i].trim();

                            // the item is always the last column for case-control and first for time course
                            // all other columns are expected to be doubles - change accordingly if needed
                            if(idxItem == -1)
                                idxItem = fldLength - 1;
                            item = fields[idxItem];
                            switch(params.method) {
                                case EDGER:
                                    values = new double[4];
                                    log2FC = Double.parseDouble(fields[0]);
                                    if(log2FC != 0.0)
                                        log2FC *= -1.0;
                                    upreg = log2FC > 0;
                                    values[0] = log2FC;
                                    values[1] = Double.parseDouble(fields[1]);
                                    values[2] = Double.parseDouble(fields[2]);
                                    values[3] = Double.parseDouble(fields[3]);
                                    de = values[EdgeRValues.FDR.ordinal()] < sigValue;
                                    rd = new DEAResultsData(item, de, upreg, values);
                                    hmItemsAdded.put(item, null);
                                    results.lstResults.add(rd);
                                    break;
                                case NOISEQ:
                                    values = new double[4];
                                    values[0] = Double.parseDouble(fields[0]);
                                    values[1] = Double.parseDouble(fields[1]);
                                    values[2] = Double.parseDouble(fields[2]);
                                    log2FC = Double.parseDouble(fields[3]);
                                    de = values[NOISeqValues.probability.ordinal()] > (1 - sigValue) && Math.abs(log2FC) >= Math.log(FCValue)/Math.log(2.0);
                                    if(log2FC != 0.0)
                                        log2FC *= -1.0;
                                    upreg = log2FC > 0;
                                    
                                    values[3] = log2FC;
                                    rd = new DEAResultsData(item, de, upreg, values);
                                    hmItemsAdded.put(item, null);
                                    results.lstResults.add(rd);
                                    break;
                                case MASIGPRO:
                                    values = new double[4];
                                    // p-value and r^2 - decided not to use
                                    values[0] = Double.parseDouble(fields[1]);
                                    values[1] = Double.parseDouble(fields[2]);
                                    values[2] = 0.0;
                                    values[3] = 0.0;
                                    if(hmClusters.containsKey(item))
                                        values[2] = hmClusters.get(item);
                                    rd = new DEAResultsData(item, true, false, values);
                                    hmItemsAdded.put(item, null);
                                    results.lstResults.add(rd);
                                    break;
                            }
                        }
                        else {
                            logger.logError("Invalid line, " + line + ", in DEA results data.");
                            break;
                        }
                    }
                    idx++;
                }
                // check if using masigpro
                if(params.method.equals(DlgDEAnalysis.Params.Method.MASIGPRO)) {
                    // only significant genes are passed back
                    // decided not to use pvalue and r^2 for now but leave this way until finalized
                    for(String name : hmItems.keySet()) {
                        if(!hmItemsAdded.containsKey(name)) {
                            values = new double[4];
                            values[0] = 1.0;
                            values[1] = 0.0;
                            values[2] = 0.0;
                            values[3] = 0.0;
                            results.lstResults.add(new DEAResultsData(name, false, false, values));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            results = new DEAResults(params.method, sigValue);
            logger.logError("Unable to load DEA transcript results data: " + e.getMessage());
        }
        return results;
    }
    public ObservableList<DEASelectionResults> getDEASelectionResults(DataType type, String groupName, double sigValue, double FCValue) {
        ObservableList<DEASelectionResults> lstResults = FXCollections.observableArrayList();
        
        // get normal DEA results
        DEAResults deaResults = getDEAResults(type, groupName, sigValue, FCValue);
        DEAResults[] deaCmpResults;
        HashMap<String, DEAResultsData>[] hmGroups = null;
        // check if dealing with a multiple time series
        if(project.data.isMultipleTimeSeriesExpType()) {
            // get additional results for all comparisons vs control
            String[] grpNames = project.data.getResultNames();
            deaCmpResults = new DEAResults[grpNames.length];
            hmGroups = new HashMap[grpNames.length];
            for(int i = 1; i < grpNames.length; i++) {
                DEAResults results = getDEAResults(type, grpNames[i], sigValue, FCValue);
                deaCmpResults[i] = results;
                HashMap<String, DEAResultsData> hm = new HashMap<>();
                hmGroups[i] = hm;
                for(DEAResultsData drd : results.lstResults)
                    hm.put(drd.id, drd);
            }
        }
        
        // get additional transcripts and expression information
        HashMap<String, Object> hmTrans = project.data.getResultsTrans();
        HashMap<String, double[]> hmMEL = project.data.getMeanExpressionLevelsHM(type, hmTrans);
        ArrayList<String> lst;
        String name, trans, gene, geneDescription;
        String chromo, strand;
        int length, isoforms;
        boolean coding;
        DecimalFormat formatter = new DecimalFormat("#.####E0");
        for(DEAResultsData drd : deaResults.lstResults) {
            chromo = "";
            strand = "+";
            isoforms = 0;
            length = 0;
            coding = false;
            switch(type) {
                case GENE:
                    gene = drd.id;
                    geneDescription = project.data.getGeneDescription(gene);
                    name = geneDescription;
                    trans = "";
                    chromo = project.data.getGeneChromo(gene);
                    strand = project.data.getGeneStrand(gene);
                    isoforms = project.data.getGeneTransCount(gene);
                    coding = project.data.isGeneCoding(gene);
                    break;
                case PROTEIN:
                    name = project.data.getProteinDescription(drd.id);
                    length = project.data.getProteinLength(drd.id, hmTrans);
                    lst = project.data.getProteinTrans(drd.id, hmTrans);
                    if(!lst.isEmpty()) {
                        HashMap<String, Object> hmProtGenes = new HashMap<>();
                        String exptrans = "";
                        String expgenes = "";
                        String expgenesdesc = "";
                        for(String protrans : lst) {
                            exptrans += (exptrans.isEmpty()? "" : ",") + protrans;
                            // it is possible for the same protein id to be from 
                            // different transcripts and also from different genes
                            String transGene = project.data.getTransGene(protrans);
                            if(!hmProtGenes.containsKey(transGene)) {
                                hmProtGenes.put(transGene, null);
                                expgenes += (expgenes.isEmpty()? "" : ",") + transGene;
                                expgenesdesc += (expgenesdesc.isEmpty()? "" : "; ") + project.data.getGeneDescription(transGene);
                            }
                        }
                        trans = exptrans;
                        gene = expgenes;
                        geneDescription = expgenesdesc;
                        chromo = project.data.getTransChromo(trans);
                        strand = project.data.getTransStrand(trans);
                        coding = true;
                    }
                    else {
                        trans = "";
                        gene = "";
                        geneDescription = "";
                    }
                    break;
                case TRANS:
                default:
                    trans = drd.id;
                    name = project.data.getTransName(trans);
                    length = project.data.getTransLength(trans);
                    gene = project.data.getTransGene(trans);
                    geneDescription = project.data.getGeneDescription(gene);
                    chromo = project.data.getTransChromo(trans);
                    strand = project.data.getTransStrand(trans);
                    coding = project.data.isTransCoding(trans);
                    break;
            }
            if(deaResults.method.equals(DlgDEAnalysis.Params.Method.NOISEQ)) {
                // temporary arrangement - not using analysis mean values since we use local ones for all analysis
                // always set x1mean to 0 and use x2mean for (1 - probability) field
                // once things settle down go ahead and remove from analysis script and file then change NOISeqValues
                drd.values[NOISeqValues.X1mean.ordinal()] = 0;
                drd.values[NOISeqValues.X2mean.ordinal()] = 0;
                drd.values[NOISeqValues.probability.ordinal()] = Double.parseDouble(formatter.format(drd.values[NOISeqValues.probability.ordinal()]));
                drd.values[NOISeqValues.X2mean.ordinal()] = Double.parseDouble(formatter.format(1 - drd.values[NOISeqValues.probability.ordinal()]));
                drd.values[NOISeqValues.log2FC.ordinal()] = Double.parseDouble(String.format("%.02f", ((double)Math.round(drd.values[NOISeqValues.log2FC.ordinal()]*100)/100.0)));
            }
            else if(deaResults.method.equals(DlgDEAnalysis.Params.Method.EDGER)) {
                drd.values[EdgeRValues.PValue.ordinal()] = Double.parseDouble(formatter.format(drd.values[EdgeRValues.PValue.ordinal()]));
                drd.values[EdgeRValues.FDR.ordinal()] = Double.parseDouble(formatter.format(drd.values[EdgeRValues.FDR.ordinal()]));
                drd.values[EdgeRValues.log2FC.ordinal()] = Double.parseDouble(String.format("%.02f", ((double)Math.round(drd.values[EdgeRValues.log2FC.ordinal()]*100)/100.0)));
                drd.values[EdgeRValues.logCPM.ordinal()] = Double.parseDouble(String.format("%.02f", ((double)Math.round(drd.values[EdgeRValues.logCPM.ordinal()]*100)/100.0)));
            }
            else { // maSigPro
                drd.values[maSigProValues.PValue.ordinal()] = Double.parseDouble(formatter.format(drd.values[maSigProValues.PValue.ordinal()]));
                drd.values[maSigProValues.RSQD.ordinal()] = Double.parseDouble(formatter.format(drd.values[maSigProValues.RSQD.ordinal()]));
            }
            if(drd.id.equals("A2m")){
                System.out.print("Hi!");
            }
            DEASelectionResults dsr = new DEASelectionResults(false, type, drd.id, drd, name, trans, gene, geneDescription);
            dsr.chromo = new SimpleStringProperty(chromo);
            dsr.strand = new SimpleStringProperty(strand);
            dsr.length = new SimpleIntegerProperty(length);
            dsr.isoforms = new SimpleIntegerProperty(isoforms);
            dsr.proteins = new SimpleIntegerProperty(project.data.getGeneProteinCount(gene));
            dsr.coding = new SimpleStringProperty(coding? "YES" : "NO");
            if(type.equals(DataType.TRANS)) {
                DataAnnotation.TransData td = project.data.getTransData(trans);
                if(td != null) {
                    dsr.category = new SimpleStringProperty(td.alignCat);
                    dsr.attributes = new SimpleStringProperty(td.alignAttrs);
                }
                else
                    logger.logWarning("Unable to retrieve transcript data for '" + trans + "'.");
                
            }
            if(!hmMEL.isEmpty()) {
                if(hmMEL.containsKey(drd.id)) {
                    double[] conds = hmMEL.get(drd.id);
                    dsr.conditions = new SimpleDoubleProperty[conds.length];
                    for(int i = 0; i < conds.length; i++)
                        dsr.conditions[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(conds[i]*100)/100.0))));
                }
                else
                    logger.logWarning("Unable to find expression values for '" + drd.id + "'.");
            }
            
            // check if dealing with multiple time series
            if(project.data.isMultipleTimeSeriesExpType()) {
                // WARNING: this is going on the assumption that we will not use p-value or r^2 from maSigPro
                for(int i = 1; i < hmGroups.length; i++) {
                    // all ids must exist in all groups - if they don't will generate exception - something is screwed up
                    if(hmGroups[i].containsKey(dsr.id.get())) {
                        dsr.deCmp[i].setValue(hmGroups[i].get(dsr.id.get()).de? "DE" : "Not DE");
                        dsr.clusterCmp[i].setValue(hmGroups[i].get(dsr.id.get()).values[maSigProValues.Cluster.ordinal()]);
                    }
                    else
                        logger.logWarning("Unable to find time series comparison data for '" + dsr.id + "'.");
                }        
            }
            lstResults.add(dsr);
        }
        return lstResults;
    }
    public HashMap<String, Boolean> getGeneIsoformsDEFlags(String gene) {
        HashMap<String, Boolean> hm = new HashMap<>();
        DlgDEAnalysis.Params deaParams = getDEAParams(DataType.TRANS);
        DataDEA.DEAResults deaResults = getDEAResults(DataType.TRANS, "", deaParams.sigValue, deaParams.FCValue);
        HashMap<String, HashMap<String, Object>> hmRGT = project.data.getResultsGeneTrans();
        if(hmRGT.containsKey(gene)) {
            HashMap<String, Object> hmTrans = hmRGT.get(gene);
            ArrayList<DataDEA.DEAResultsData> lst = deaResults.lstResults;
            for(String trans : hmTrans.keySet()) {
                for(DataDEA.DEAResultsData drd : lst) {
                    if(drd.id.equals(trans)) {
                        hm.put(trans, drd.de);
                        break;
                    }
                }
            }
        }
        return hm;
    }
    public HashMap<String, Boolean> getGeneProteinsDEFlags(String gene) {
        HashMap<String, Boolean> hm = new HashMap<>();
        DlgDEAnalysis.Params deaParams = getDEAParams(DataType.PROTEIN);
        DataDEA.DEAResults deaResults = getDEAResults(DataType.PROTEIN, "", deaParams.sigValue, deaParams.FCValue);
        HashMap<String, HashMap<String, Object>> hmRGT = project.data.getResultsGeneTrans();
        if(hmRGT.containsKey(gene)) {
            HashMap<String, Object> hmTrans = hmRGT.get(gene);
            HashMap<String, Object> hmProteins = new HashMap<>();
            DataAnnotation da = project.data.getDataAnnotation();
            for(String trans : hmTrans.keySet()) {
                String prot = da.getTransProtein(trans);
                if(!prot.isEmpty()) {
                    if(!hmProteins.containsKey(prot))
                        hmProteins.put(prot, null);
                }
            }
            ArrayList<DataDEA.DEAResultsData> lst = deaResults.lstResults;
            for(String protein : hmProteins.keySet()) {
                for(DataDEA.DEAResultsData drd : lst) {
                    if(drd.id.equals(protein)) {
                        hm.put(protein, drd.de);
                        break;
                    }
                }
            }
        }
        return hm;
    }

    //
    // Internal Functions
    //
    private boolean hasAnyResults(DataType dataType) {
        File deaFolder = new File(Paths.get(getDEAFolder()).toString());
        // check for any file: result_<dataType>.*.tsv
        FilenameFilter filter = (File dir, String name) -> (name.startsWith(getDEAResultsPrefix(dataType)) && name.endsWith(DataApp.TSV_EXT));
        File[] files = deaFolder.listFiles(filter);
        return (files != null && files.length > 0);
    }
    
    //
    // Data Classes
    //
    public static class CombinedResults implements Comparable<CombinedResults>{
        public final SimpleStringProperty gene;
        public final SimpleStringProperty geneDescription;
        public final SimpleStringProperty geneDS;
        public final SimpleStringProperty geneDE;
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleStringProperty coding = null;
        public final SimpleIntegerProperty protcnt;
        public final SimpleStringProperty protDE;
        public final SimpleIntegerProperty isocnt;
        public final SimpleStringProperty isoDE;
        public final SimpleStringProperty featuresDS;
 
        public CombinedResults(String gene, String geneDescription, String geneDS, String geneDE,
                                int protcnt, String protDE, int isocnt, String isoDE, String featuresDS) {
            this.gene = new SimpleStringProperty(gene);
            this.geneDescription = new SimpleStringProperty(geneDescription);
            this.geneDE = new SimpleStringProperty(geneDE);
            this.geneDS = new SimpleStringProperty(geneDS);
            this.protcnt = new SimpleIntegerProperty(protcnt);
            this.protDE = new SimpleStringProperty(protDE);
            this.isocnt = new SimpleIntegerProperty(isocnt);
            this.isoDE = new SimpleStringProperty(isoDE);
            this.featuresDS = new SimpleStringProperty(featuresDS);
        }
        public String getGene() { return gene.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        public String getDEGene() { return geneDE.get(); }
        public String getDSGene() { return geneDS.get(); }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public String getCoding() { return coding.get(); }
        public Integer getTotalIsoformCnt() { return isocnt.get(); }
        public Integer getTotalProteinCnt() { return protcnt.get(); }
        public String getDEIsoforms() { return isoDE.get(); }
        public String getDEProteins() { return protDE.get(); }
        public String getDSFeatures() { return featuresDS.get(); }
        @Override
        public int compareTo(CombinedResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
    // combined Differential Analysis results
    public static class DASelectionResults extends SelectionDataResults implements Comparable<DASelectionResults> {
        public final SimpleStringProperty geneTransDS;
        public final SimpleStringProperty geneProtDS;
        public final SimpleStringProperty geneDE;
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleStringProperty coding = null;
        public final SimpleIntegerProperty protcnt;
        public final SimpleStringProperty protDE;
        public final SimpleStringProperty protSwitching;
        public final SimpleIntegerProperty isocnt;
        public final SimpleStringProperty isoDE;
        public final SimpleStringProperty isoSwitching; 
        
        public DASelectionResults(boolean selected, DataType dataType, String id, String gene, String geneDescription, String geneTransDS, String geneProtDS, String geneDE,
                                int protcnt, String protDE, String protSwitching, int isocnt, String isoDE, String isoSwitching) { //, String featuresDS) {
            super(selected, dataType, id, gene, geneDescription);
            this.geneDE = new SimpleStringProperty(geneDE);
            this.geneTransDS = new SimpleStringProperty(geneTransDS);
            this.geneProtDS = new SimpleStringProperty(geneProtDS);
            this.protcnt = new SimpleIntegerProperty(protcnt);
            this.protDE = new SimpleStringProperty(protDE);
            this.protSwitching = new SimpleStringProperty(protSwitching);
            this.isocnt = new SimpleIntegerProperty(isocnt);
            this.isoDE = new SimpleStringProperty(isoDE);
            this.isoSwitching = new SimpleStringProperty(isoSwitching);
        }
        
        public String getDEGene() { return geneDE.get(); }
        public String getTransDSGene() { return geneTransDS.get(); }
        public String getProtDSGene() { return geneProtDS.get(); }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public String getCoding() { return coding.get(); }
        public Integer getTotalIsoformCnt() { return isocnt.get(); }
        public Integer getTotalProteinCnt() { return protcnt.get(); }
        public String getDEIsoforms() { return isoDE.get(); }
        public String getDEProteins() { return protDE.get(); }
        public String getProteinSwitching() { return protSwitching.get(); }
        public String getTranscriptSwitching() { return isoSwitching.get(); }
        
        @Override
        public int compareTo(DASelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
    public static class DEASelectionResults extends SelectionDataResults implements Comparable<DEASelectionResults> {
        public final SimpleStringProperty name;
        public final SimpleStringProperty transcript;
        public final SimpleDoubleProperty value1;
        public final SimpleDoubleProperty value2;
        public final SimpleDoubleProperty value3; 
        public final SimpleDoubleProperty value4;
        public final SimpleStringProperty de;
        public final SimpleIntegerProperty cluster;
        public SimpleStringProperty[] deCmp;
        public SimpleIntegerProperty[] clusterCmp;
        public final SimpleStringProperty regulated;

        public SimpleIntegerProperty length;
        public SimpleStringProperty category;
        public SimpleStringProperty attributes;
        public SimpleStringProperty chromo;
        public SimpleStringProperty strand;
        public SimpleIntegerProperty isoforms;
        public SimpleIntegerProperty proteins;
        public SimpleStringProperty coding;
        
        // For all data types: name is for name/desc of 'id'
        // For DataType.PROTEIN: transcript is for protein's transcript ID
        // For DataType.TRANS/PROTEIN: gene and gene description are set to tran/protein's gene
        public DEASelectionResults(boolean selected, DataType dataType, String id, DEAResultsData drd, String name, String transcript, String gene, String geneDescription) {
            super(selected, dataType, id, gene, geneDescription);
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.transcript = new SimpleStringProperty(transcript);
            this.de = new SimpleStringProperty(drd.de? "DE" : "Not DE");
            if(drd.de)
                this.regulated = new SimpleStringProperty(drd.upreg? "UP" : "DOWN");
            else
                this.regulated = new SimpleStringProperty("");
            // cluster is only valid in time course series - will need to change if other than maSigPro
            Double dclust = drd.values[maSigProValues.Cluster.ordinal()];
            this.cluster = new SimpleIntegerProperty(dclust.intValue());
            this.value1 = new SimpleDoubleProperty(drd.values[0]);
            this.value2 = new SimpleDoubleProperty(drd.values[1]);
            this.value3 = new SimpleDoubleProperty(drd.values[2]);
            this.value4 = new SimpleDoubleProperty(drd.values[3]);
            this.category = new SimpleStringProperty("");
            this.attributes = new SimpleStringProperty("");
            this.chromo = new SimpleStringProperty("");
            this.strand = new SimpleStringProperty("");
            this.coding = new SimpleStringProperty("");
            this.length = new SimpleIntegerProperty(0);
            this.isoforms = new SimpleIntegerProperty(0);
            this.proteins = new SimpleIntegerProperty(0);
            // multiple time series comparison data
            this.deCmp = new SimpleStringProperty[DlgInputData.Params.MAX_GROUPS];
            this.clusterCmp = new SimpleIntegerProperty[DlgInputData.Params.MAX_GROUPS];
            this.deCmp[0] = this.de;
            this.clusterCmp[0] = this.cluster;
            for(int i = 1; i < DlgInputData.Params.MAX_GROUPS; i++) {
                this.deCmp[i] = new SimpleStringProperty("");
                this.clusterCmp[i] = new SimpleIntegerProperty(0);
            }
        }
        public String getName() { return name.get(); }
        public String getTranscript() { return transcript.get(); }
        public String getDE() { return de.get(); }
        public Integer getCluster() { return cluster.get(); }
        public String getClusterString() { return cluster.get() == 0? "" : ("" + cluster.get()); }
        public String getDECmp1() { return deCmp[1].get(); }
        public Integer getClusterCmp1() { return clusterCmp[1].get(); }
        public String getClusterStringCmp1() { return clusterCmp[1].get() == 0? "" : ("" + clusterCmp[1].get()); }
        public String getDECmp2() { return deCmp[2].get(); }
        public Integer getClusterCmp2() { return clusterCmp[2].get(); }
        public String getClusterStringCmp2() { return clusterCmp[2].get() == 0? "" : ("" + clusterCmp[2].get()); }
        public String getDECmp3() { return deCmp[3].get(); }
        public Integer getClusterCmp3() { return clusterCmp[3].get(); }
        public String getClusterStringCmp3() { return clusterCmp[3].get() == 0? "" : ("" + clusterCmp[3].get()); }
        public Double getValue1() { return value1.get(); }
        public Double getValue2() { return value2.get(); }
        public Double getValue3() { return value3.get(); }
        public Double getValue4() { return value4.get(); }
        public String getRegulated() { return regulated.get(); }

        public Integer getLength() { return length.get(); }
        public String getCategory() { return category.get(); }
        public void setCategory(String cat) { category.set(cat); }
        public String getAttributes() { return attributes.get(); }
        public void setAttributes(String attrs) { attributes.set(attrs); }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public Integer getProteins() { return proteins.get(); }
        public String getCoding() { return coding.get(); }
        public void setCoding(String val) { coding.set(val); }
        @Override
        public int compareTo(DEASelectionResults td) {
            return (id.get().compareToIgnoreCase(td.id.get()));
        }
    }
    // DEA data for genes, proteins, and transcripts
    public static class DEAResults {
        private DlgDEAnalysis.Params.Method method;
        private final double sigValue;
        public ArrayList<DEAResultsData> lstResults;
        public DEAResults(DlgDEAnalysis.Params.Method method, double sigValue) {
            this.method = method;
            this.sigValue = sigValue;
            lstResults = new ArrayList<>();
        }
        public HashMap<String, DEAResultsData> getHMTrans() {
            HashMap<String, DEAResultsData> hm = new HashMap<>();
            for(DEAResultsData rd : lstResults)
                hm.put(rd.id, rd);
            return hm;
        }
        public ArrayList<DataApp.RankedListEntry> getRankedList() {
            ArrayList<DataApp.RankedListEntry> lst = new ArrayList<>();
            double value;
            for(DEAResultsData rd : lstResults) {
                switch(method) {
                    case EDGER:
                        value = rd.values[EdgeRValues.FDR.ordinal()];
                        break;
                    case NOISEQ:
                        value = 1 - rd.values[NOISeqValues.probability.ordinal()];
                        break;
                    case MASIGPRO:
                        value = rd.values[maSigProValues.PValue.ordinal()];
                        break;
                    default:
                        value = 1.0;
                        break;
                }
                lst.add(new DataApp.RankedListEntry(rd.id, value));
            }
            Collections.sort(lst);
            return lst;
        }
        public DlgDEAnalysis.Params.Method getMethod() { return method; }
        public void addResultData(DEAResultsData rd) { lstResults.add(rd); }
    }
    // DEA results data, as returned by R script, for genes, proteins, and transcripts
    // the result values will change based on the method used for DEA - handled in DEAResults
    // May want to get rid of X1 and X2 and add (1-probability) for NOISeq since we use our own mean values
    enum NOISeqValues { X1mean, X2mean, probability, log2FC }
    enum EdgeRValues { log2FC, logCPM, PValue, FDR }
    enum maSigProValues { PValue, RSQD, Cluster }
    
    public static class DEAResultsData {
        public String id;           // trans, protein, or gene
        public boolean de;          // differentially expressed - depends of significance level
        public boolean upreg;       // up regulated  - only applicable if 'de' set
        public double values[];     // analysis results - fields vary based on which method was used

        public DEAResultsData(String id, boolean de, boolean upreg, double[] values) {
            this.id = id;
            this.de = de;
            this.upreg = upreg;
            this.values = values;
        }
    }
}
