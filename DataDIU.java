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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DataDIU extends AppObject {
    static public enum DSType { ALL, DS, NOTDS };

    public String getDIUFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DIU).toString(); }
    public String getDIUParamsFilepath(DataType dataType) { return Paths.get(getDIUFolder(), dataType.name().toLowerCase() + DataApp.PRM_EXT).toString(); }
    public String getDIUResultsFilepath(DataType dataType) { return Paths.get(getDIUFolder(), DataApp.RESULTS_NAME + dataType.name().toLowerCase() + DataApp.TSV_EXT).toString(); }
    public String getDIUDoneFilepath(DataType dataType) { return Paths.get(getDIUFolder(), DataApp.DONE_NAME + dataType.name().toLowerCase() + DataApp.TEXT_EXT).toString(); }
    public String getDIULogFilepath(DataType dataType) { return Paths.get(getDIUFolder(), DataApp.LOG_PREFIX + dataType.name().toLowerCase() + DataApp.LOG_EXT).toString(); }

    public DataDIU(Project project) {
        super(project, null);
    }
    // initialization function
    public void initialize() {
        clearData();
    }
    public boolean hasDIUData() { return (hasDIUData(DataApp.DataType.TRANS) || hasDIUData(DataApp.DataType.PROTEIN)); }
    public boolean hasDIUData(DataApp.DataType dataType) { return (Files.exists(Paths.get(getDIUDoneFilepath(dataType))) && Files.exists(Paths.get(getDIUResultsFilepath(dataType)))); }
    public void clearData() {
    }
    public void clearData(DataType type) {
    }
    
    public DlgDIUAnalysis.Params getDIUParams(DataType dataType) { 
        return DlgDIUAnalysis.Params.load(getDIUParamsFilepath(dataType), project);
    }
    
    public void clearDataDIU(boolean rmvPrm) {
        clearData();
        removeAllDIUResultFiles(rmvPrm);
    }
    public void clearDataDIU(DataType type, boolean rmvPrm) {
        clearData(type);
        removeDIUResultFiles(type, rmvPrm);
    }
    public void removeDIUResultFiles(DataType type, boolean rmvPrms) {
        Utils.removeFile(Paths.get(getDIUResultsFilepath(type)));
        Utils.removeFile(Paths.get(getDIUDoneFilepath(type)));
        
        // remove log file and parameters if requested
        Utils.removeFile(Paths.get(getDIULogFilepath(type)));
        if(rmvPrms)
            Utils.removeFile(Paths.get(getDIUParamsFilepath(type)));
    }
    public void removeAllDIUResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DIU), rmvPrms);
    }
    
    public boolean genDIUInputFiles(DataType dataType) {
        boolean result = false;
        if(!Files.exists(Paths.get(project.data.getExpFactorsFilepath())))
            project.data.copyExpFactorsFile(project.data.getExpFactorsFilepath());
        if(Files.exists(Paths.get(project.data.getExpFactorsFilepath()))) {
            switch(dataType) {
                case TRANS:
                    if(project.data.genGeneTransFile() && project.data.genExpressionRawFile(dataType, false) && project.data.genExpressionFile(dataType, false))
                        result = true;
                    break;
                case PROTEIN:
                    // must use gene_protein ids for DIU
                    if(project.data.genGeneProteinsFile() && project.data.genExpressionRawFile(dataType, true) && project.data.genExpressionFile(dataType, true))
                        result = true;
                    break;
            }
        }
        return result;
    }
    public ArrayList<String> getDIUGenes(DataType type, DSType dsType, double sigValue) {
        ArrayList<String> lstDS = new ArrayList<>();
        ArrayList<DIUResultsData> lstResults = getDIUResultsData(type, sigValue);
        for(DIUResultsData diur : lstResults) {
            if(dsType.equals(DSType.DS) && diur.isDS(sigValue))
                lstDS.add(diur.gene);
            else if(dsType.equals(DSType.NOTDS) && !diur.isDS(sigValue))
                lstDS.add(diur.gene);
            else if(dsType.equals(DSType.ALL))
                lstDS.add(diur.gene);
        }
        return lstDS;
    }
    public HashMap<String, HashMap<String, Object>> getDIUGeneTransFilter(DataType type, DSType dsType, double sigValue) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        ArrayList<String> lst = getDIUGenes(type, dsType, sigValue);
        HashMap<String, HashMap<String, Object>> hmGT = project.data.getResultsGeneTrans();
        for(String gene : lst) {
            if(hmGT.containsKey(gene)) {
                HashMap<String, Object> hm = new HashMap<>();
                hmGeneTrans.put(gene, hm);
                HashMap<String, Object> hmt = hmGT.get(gene);
                for(String trans : hmt.keySet())
                    hm.put(trans, null);
            }
        }
        return hmGeneTrans;
    }
    public ArrayList<DIUResultsData> getDIUResultsData(DataType type, double sigValue) {
        ArrayList<DIUResultsData> lstDS = new ArrayList<>();
        try {
            String filepath = getDIUResultsFilepath(type);
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);

                // if the results are from edgeR we get: gene pValue qValue podiumChange totalChange
                // if the results are from DEXSeq we get: gene qValue podiumChange totalChange
                // if the results are from maSigPro we get: gene adjPValue (will treat as qValue) podiumChange
                String gene;
                String[] fields;
                double pValue, qValue, totalChange;
                boolean podiumChange, ds;
                int idx = 0;
                int fldLength = -1;
                for(String line : lines) {
                    if(idx > 0) {
                        fields = line.split("\t");
                        if(fldLength == -1) {
                            fldLength = fields.length;
                            if(fldLength != 4 && fldLength != 5) {
                                logger.logError("Invalid number of columns, " + fldLength + ", in DIU gene results data.");
                                break;
                            }
                        }
                        if(fields.length == fldLength) {
                            // R is sticking spaces between the tabs
                            for(int i = 0; i < fields.length; i++)
                                fields[i] = fields[i].trim();
                            int fldIdx = 0;
                            gene = fields[fldIdx++];
                            if(fldLength == 5)
                                pValue = Double.parseDouble(fields[fldIdx++]);
                            else
                                pValue = 0;
                            qValue = Double.parseDouble(fields[fldIdx++]);
                            podiumChange = Boolean.valueOf(fields[fldIdx++]);
                            totalChange = (project.data.isTimeCourseExpType())? 0.0 : Double.parseDouble(fields[fldIdx++]);
                            String timePoints = (project.data.isTimeCourseExpType())? (fields[fldIdx++].equals(".")? "" : fields[fldIdx-1]) : "";
                            String geneDescription = project.data.getGeneDescription(gene);
                            lstDS.add(new DIUResultsData(gene, geneDescription, pValue, qValue, podiumChange, timePoints, totalChange, qValue < sigValue));
                        }
                        else {
                            logger.logError("Invalid line, " + line + ", in DIU gene results data.");
                            break;
                        }
                    }
                    idx++;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DS gene results data: " + e.getMessage());
        }
        return lstDS;
    }
    public ObservableList<DIUSelectionResults> getDIUSelectionResults(DataType dataType, double sigValue, boolean getGeneData) {
        ObservableList<DIUSelectionResults> lstResults = FXCollections.observableArrayList();
        ArrayList<DIUResultsData> lstDS = getDIUResultsData(dataType, sigValue);
        for(DIUResultsData dsrd : lstDS)
            lstResults.add(new DIUSelectionResults(false, dsrd));
        if(getGeneData) {
            String gene;
            HashMap<String, double[]> hmMEL;
            if(dataType.equals(DataType.PROTEIN)) {
                // generate gene protein mean expression values, just exclude non-coding transcripts
                HashMap<String, Object> hmResultTrans = project.data.getResultsTrans();
                HashMap<String, Object> hmFilterTrans = new HashMap();
                for(String trans : hmResultTrans.keySet()) {
                    if(project.data.isTransCoding(trans))
                        hmFilterTrans.put(trans, null);
                }
                hmMEL = project.data.getMeanExpressionLevelsHM(DataType.GENE, hmFilterTrans);
            }
            else {
                // get expression level for all gene transcripts
                hmMEL = project.data.getMeanExpressionLevelsHM(DataType.GENE, project.data.getResultsTrans());
            }
            for(DIUSelectionResults dr : lstResults) {
                gene = dr.getGene();
                dr.chromo = new SimpleStringProperty(project.data.getGeneChromo(gene));
                dr.strand = new SimpleStringProperty(project.data.getGeneStrand(gene));
                dr.isoforms = new SimpleIntegerProperty(project.data.getGeneTransCount(gene));
                dr.proteins = new SimpleIntegerProperty(project.data.getGeneProteinCount(gene));
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
                        logger.logWarning("Unable to find expression values for gene '" + gene + "'");
                }    
            }
        }
        return lstResults;
    }

    //
    // Data Classes
    //
    
    // DIU results data from script results output file (except ds which is calculated from data)
    // enum names for shared values must be identical - see use in subtabs
    enum DEXSeqValues { QValue, PodiumChange, TotalChange }
    enum EdgeRValues { PValue, QValue, PodiumChange, TotalChange }
    public static class DIUResultsData {
        public String gene;
        public String geneDescription;
        public double pValue;
        public double qValue;
        public boolean podiumChange;
        public String podiumTimes;
        public double totalChange;
        public boolean ds;

        public DIUResultsData(String gene, String geneDescription, double pValue, double qValue, boolean podiumChange, String podiumTimes, double totalChange, boolean ds) {
            this.gene = gene;
            this.geneDescription = geneDescription;
            this.pValue = pValue;
            this.qValue = qValue;
            this.podiumChange = podiumChange;
            this.podiumTimes = podiumTimes;
            this.totalChange = totalChange;
            this.ds = ds;
        }
        public boolean isDS(double sigValue) {
            return(qValue < sigValue);
        }
    }

    // specific analysis results
    public static class DIUSelectionResults extends SelectionDataResults implements Comparable<DIUSelectionResults> {
        public SimpleStringProperty chromo = null;
        public SimpleStringProperty strand = null;
        public SimpleIntegerProperty isoforms = null;
        public SimpleIntegerProperty proteins = null;
        public SimpleStringProperty coding = null;
        public SimpleDoubleProperty pValue = null;
        public SimpleDoubleProperty qValue = null;
        public SimpleDoubleProperty totalChange = null;
        public SimpleStringProperty podiumChange = null;
        public SimpleStringProperty podiumTimes = null;
        public SimpleStringProperty ds = null;
        
        public DIUSelectionResults(boolean selected, DIUResultsData dsra) {
            super(selected, DataType.GENE, dsra.gene, dsra.gene, dsra.geneDescription);
            DecimalFormat formatter = new DecimalFormat("#.####E0");
            this.pValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.pValue)));
            this.qValue = new SimpleDoubleProperty(Double.parseDouble(formatter.format(dsra.qValue)));
            this.podiumChange = new SimpleStringProperty(dsra.podiumChange? "YES" : "NO");
            this.podiumTimes = new SimpleStringProperty(dsra.podiumTimes);
            this.totalChange = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(dsra.totalChange*100)/100.0))));
            this.ds = new SimpleStringProperty(dsra.ds? "DIU" : "Not DIU");
        }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        public Integer getProteins() { return proteins.get(); }
        public String getCoding() { return coding.get(); }
        public Double getPValue() { return pValue.get(); }
        public Double getQValue() { return qValue.get(); }
        public String getPodiumChange() { return podiumChange.get(); }
        public String getPodiumTimes() { return podiumTimes.get(); }
        public Double getTotalChange() { return totalChange.get(); }
        public String getDS() { return ds.get(); }
        
        @Override
        public int compareTo(DIUSelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
}
