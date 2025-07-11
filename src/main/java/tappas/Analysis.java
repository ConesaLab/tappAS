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
import javafx.scene.chart.XYChart;
import tappas.DataApp.DataType;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Analysis extends AppObject {
    // analysis data access instances
    private DataFDA dataFDA;    // multiple subtabs
    private DataDEA dataDEA;
    private DataDIU dataDIU;
    private DataDFI dataDFI;    // multiple subtabs
    private DataDPA dataDPA;
    private DataUTRL dataUTRL;
    private DataGSEA dataGSEA;  // multiple subtabs
    private DataFEA dataFEA;    // multiple subtabs
    
    public Analysis(Project project) {
        super(project, null);
    }
    
    public void initialize() {
        logger.logDebug("Initializing analysis data...");
        dataFDA = new DataFDA(project);
        dataDEA = new DataDEA(project);
        dataDIU = new DataDIU(project);
        dataDFI = new DataDFI(project);
        dataDPA = new DataDPA(project);
        dataUTRL = new DataUTRL(project);
        dataGSEA = new DataGSEA(project);
        dataFEA = new DataFEA(project);

        createFolders();
        dataDEA.initialize();
        dataDIU.initialize();
        dataDPA.initialize();
        dataUTRL.initialize();
        // there can be multiple open subtabs for FEA/GSEA and FDA/DFI
        // they provide analysis functions but hold no analysis data
        dataFDA.initialize();
        dataDFI.initialize();
        dataGSEA.initialize();
        dataFEA.initialize();
        logger.logDebug("Analysis data initialization completed.");
    }
    public void createFolders() {
        // create folders if needed
        Utils.createFolder(dataFDA.getFDAFolder());
        Utils.createFolder(dataDEA.getDEAFolder());
        Utils.createFolder(dataDIU.getDIUFolder());
        Utils.createFolder(dataDFI.getDFIFolder());
        Utils.createFolder(dataDPA.getDPAFolder());
        Utils.createFolder(dataUTRL.getUTRLFolder());
        Utils.createFolder(dataGSEA.getGSEAFolder());
        Utils.createFolder(dataFEA.getFEAFolder());
    }

    //
    // All Analysis
    public boolean hasAnyAnalysisData() {
        return dataFDA.hasAnyFDAData() || hasAnyDAData() || hasAnyFAData() || hasAnyEAData();
    }
    
    //
    // FDA
    //
    public String getFDAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA).toString(); }
    public String getFDALogFilepath(String id) { return dataFDA.getFDALogFilepath(id); }
    public String getFDAParamsFilepath(String id) { return dataFDA.getFDAParamsFilepath(id); }
    public String getFDAParamsCombinedResultsFilepath() { return dataFDA.getFDAParamsCombinedResultsFilepath(); }
    public boolean hasFDAData(String id) { return dataFDA.hasFDAData(id);}
    public boolean hasAnyFDAData() { return dataFDA.hasAnyFDAData(); }
    public boolean hasTwoFDAIdData() {return dataFDA.hasTwoFDAIdData();}
    public void clearDataFDA(String id, boolean rmvPrm) { dataFDA.clearDataFDA(id, rmvPrm);}
    public void setFDAParams(HashMap<String, String> hmParameters, String id) { dataFDA.setFDAParams(hmParameters, id);}
    public HashMap<String, String> getFDAParams(String id) { return dataFDA.getFDAParams(id);}
    public void removeFDAFiles(String id, boolean rmvPrms) { dataFDA.removeFDAFiles(id, rmvPrms); }
    
    public ArrayList<DataApp.EnumData> getFDAParamsList() { return dataFDA.getFDAParamsList(); }
    public ArrayList<DataApp.EnumData> getFDAResultsList() { return dataFDA.getFDAResultsList(); }
    
    
    //
    // Differential Analysis (DA = DEA + DIU)
    //
    public boolean hasDEAData() { return dataDEA.hasDEAData(); }
    public boolean hasDEAData(DataType dataType) { return dataDEA.hasDEAData(dataType); }
    public void clearDataDEA(boolean rmvprm) { dataDEA.clearDataDEA(rmvprm); }
    public void clearDataDEA(DataType dataType, boolean rmvprm) { dataDEA.clearDataDEA(dataType, rmvprm); }
    public boolean hasAnyDAData() { return (dataDEA.hasDEAData() || dataDIU.hasDIUData()); }
    public ObservableList<DataDEA.DASelectionResults> getDASelectionResults() {
        long tstart = System.nanoTime();

        // get DIU results and compute DS using given sigValue
        logger.logDebug("Reading DIU result data...");
        HashMap<String, DataDIU.DIUSelectionResults> hmTransDIU = new HashMap<>();
        HashMap<String, DataDIU.DIUSelectionResults> hmProtDIU = new HashMap<>();
        if(dataDIU.hasDIUData(DataType.TRANS)) {
            DataType type = DataType.TRANS;
            //DlgDIUnalysis.Params diuParams = DlgDIUAnalysis.Params.load(dataDIU.getDIUParamsFilepath(type));
            DlgDIUAnalysis.Params diuParams = dataDIU.getDIUParams(DataType.TRANS);
            ObservableList<DataDIU.DIUSelectionResults> lstDIU = dataDIU.getDIUSelectionResults(type, diuParams.sigValue, true);
            for(DataDIU.DIUSelectionResults diur : lstDIU)
                hmTransDIU.put(diur.getGene(), diur);
        }
        if(dataDIU.hasDIUData(DataType.PROTEIN)) {
            DataType type = DataType.PROTEIN;
            //DlgDIUnalysis.Params diuParams = DlgDIUAnalysis.Params.load(dataDIU.getDIUParamsFilepath(type));
            DlgDIUAnalysis.Params diuParams = dataDIU.getDIUParams(DataType.PROTEIN);
            ObservableList<DataDIU.DIUSelectionResults> lstDIU = dataDIU.getDIUSelectionResults(type, diuParams.sigValue, true);
            for(DataDIU.DIUSelectionResults diur : lstDIU)
                hmProtDIU.put(diur.getGene(), diur);
        }
        // get transcripts DEA results and compute DE using given sigValue
        logger.logDebug("Reading DEA result data...");
        ObservableList<DataDEA.DASelectionResults> lst = FXCollections.observableArrayList();
        DlgDEAnalysis.Params deaParams = dataDEA.getDEAParams(DataType.TRANS);
// groupname if used for time series!!! - not sure if this msgs are still valid (refers to the ""?)
        DataDEA.DEAResults deaResults = dataDEA.getDEAResults(DataType.TRANS, "", deaParams.sigValue, deaParams.FCValue);
        HashMap<String, DataDEA.DEAResultsData> hmDEATrans = deaResults.getHMTrans();
        HashMap<String, HashMap<String, DataDEA.DEAResultsData>> hmDEAGeneTrans = new HashMap<>();
        for(String deaTrans : hmDEATrans.keySet()) {
            String gene = project.data.getTransGene(deaTrans);
            if(!gene.isEmpty()) {
                if(hmDEAGeneTrans.containsKey(gene))
                    hmDEAGeneTrans.get(gene).put(deaTrans, hmDEATrans.get(deaTrans));
                else {
                    HashMap<String, DataDEA.DEAResultsData> hm = new HashMap<>();
                    hm.put(deaTrans, hmDEATrans.get(deaTrans));
                    hmDEAGeneTrans.put(gene, hm);
                }
            }
        }
        
        // get proteins DEA results and compute DE using given sigValue
        deaParams = dataDEA.getDEAParams(DataType.PROTEIN);
// groupname if used for time series!!!
        deaResults = dataDEA.getDEAResults(DataType.PROTEIN, "", deaParams.sigValue, deaParams.FCValue);
        HashMap<String, DataDEA.DEAResultsData> hmDEAProt = deaResults.getHMTrans();
        HashMap<String, Object> hmFilterTrans = project.data.getResultsTrans();
        HashMap<String, HashMap<String, DataDEA.DEAResultsData>> hmDEAGeneProt = new HashMap<>();
        for(String deaProt : hmDEAProt.keySet()) {
            String gene = project.data.getProteinGene(deaProt, hmFilterTrans);
            if(!gene.isEmpty()) {
                if(hmDEAGeneProt.containsKey(gene))
                    hmDEAGeneProt.get(gene).put(deaProt, hmDEAProt.get(deaProt));
                else {
                    HashMap<String, DataDEA.DEAResultsData> hm = new HashMap<>();
                    hm.put(deaProt, hmDEAProt.get(deaProt));
                    hmDEAGeneProt.put(gene, hm);
                }
            }
        }
        
        // get genes DEA results and compute DE using given sigValue
        deaParams = dataDEA.getDEAParams(DataType.GENE);
        // groupname if used for time series!!!
        deaResults = dataDEA.getDEAResults(DataType.GENE, "", deaParams.sigValue, deaParams.FCValue);
        HashMap<String, DataDEA.DEAResultsData> hmDEAGene = deaResults.getHMTrans();
        
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        logger.logDebug("Loaded DE statistical test result data in " + duration + " ms");

        tstart = System.nanoTime();
        logger.logDebug("Reading DE supporting data...");
        String geneTransDS, geneProtDS, geneDE, protDE, transDE, protSwitching, transSwitching; //, featuresDS;
        HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = project.data.getResultsGeneTrans();
        HashMap<String, HashMap<String, Object>> hmGeneProts;
        hmGeneProts = project.data.getGeneAssignedProteins(hmFilterGeneTrans);
        tend = System.nanoTime();
        duration = (tend - tstart)/1000000;
        logger.logDebug("Loaded DE supporting data in " + duration + " ms");
        
        tstart = System.nanoTime();
        logger.logDebug("Processing results by gene...");
        for(String gene : hmFilterGeneTrans.keySet()) {
            geneTransDS = geneProtDS = geneDE = protDE = transDE = protSwitching = transSwitching = ""; //featuresDS = "";
            
            // get DIU results
            if(hmTransDIU.containsKey(gene)){
                geneTransDS = hmTransDIU.get(gene).getDS();
                transSwitching = hmTransDIU.get(gene).getPodiumChange();
            }
            if(hmProtDIU.containsKey(gene)){
                geneProtDS = hmProtDIU.get(gene).getDS();
                protSwitching = hmProtDIU.get(gene).getPodiumChange();
            }
            // get DEA results
            if(hmDEAGeneTrans.containsKey(gene)) {
                HashMap<String, DataDEA.DEAResultsData> hm = hmDEAGeneTrans.get(gene);
                for(String deaTrans : hm.keySet()) {
                    DataDEA.DEAResultsData rd = hm.get(deaTrans);
                    if(rd.de)
                        transDE += (transDE.isEmpty()? "" : ",") + deaTrans;
                }
            }
            if(hmDEAGeneProt.containsKey(gene)) {
                HashMap<String, DataDEA.DEAResultsData> hm = hmDEAGeneProt.get(gene);
                for(String deaProt : hm.keySet()) {
                    DataDEA.DEAResultsData rd = hm.get(deaProt);
                    if(rd.de)
                        protDE += (protDE.isEmpty()? "" : ",") + deaProt;
                }
            }
            if(hmDEAGene.containsKey(gene)) {
                DataDEA.DEAResultsData rd = hmDEAGene.get(gene);
                if(rd.de)
                    geneDE = "DE";
                else
                    geneDE = "Not DE";
            }
            
            // get total counts and add results to list
            int transCnt = hmFilterGeneTrans.get(gene).size();
            int protCnt = 0;
            if(hmGeneProts.containsKey(gene))
                protCnt = hmGeneProts.get(gene).size();
            String geneDescription = project.data.getGeneDescription(gene);
            DataDEA.DASelectionResults dsr = new DataDEA.DASelectionResults(false, DataType.GENE, gene, gene, geneDescription, geneTransDS, geneProtDS, geneDE, protCnt, protDE, protSwitching, transCnt, transDE, transSwitching); //, featuresDS);
            dsr.chromo = new SimpleStringProperty(project.data.getGeneChromo(gene));
            dsr.strand = new SimpleStringProperty(project.data.getGeneStrand(gene));
            dsr.isoforms = new SimpleIntegerProperty(project.data.getGeneTransCount(gene));
            dsr.coding = new SimpleStringProperty(project.data.isGeneCoding(gene)? "YES" : "NO");
            lst.add(dsr);
        }
        tend = System.nanoTime();
        duration = (tend - tstart)/1000000;
        logger.logDebug("Processed DE results in " + duration + " ms");
        return lst;
    }
    public void clearDataDA() {
        dataDEA.clearDataDEA(true);
        dataDIU.clearDataDIU(true);
        removeAllDAResultFiles(true);
    }
    public void removeAllDAResultFiles(boolean rmvPrms) {
        dataDEA.removeAllDEAResultFiles(rmvPrms);
        dataDIU.removeAllDIUResultFiles(rmvPrms);
    }
    //
    // DEA
    //
    public String getDEALogFilepath(DataType dataType) { return dataDEA.getDEALogFilepath(dataType); }
    public void setDEAParams(HashMap<String, String> hmParameters, DataType dataType) { 
        dataDEA.setDEAParams(hmParameters, dataType);
    }
    public DlgDEAnalysis.Params getDEAParams(DataType dataType) { 
        return dataDEA.getDEAParams(dataType);
    }
    public String getDEAParamsFilepath(DataType dataType) { return dataDEA.getDEAParamsFilepath(dataType); }
    public HashMap<String, Boolean> getGeneIsoformsDEFlags(String gene) { return dataDEA.getGeneIsoformsDEFlags(gene); }
    public HashMap<String, Boolean> getGeneProteinsDEFlags(String gene) { return dataDEA.getGeneProteinsDEFlags(gene); }

    //
    // DIU
    //
    public boolean hasDIUData() { return dataDIU.hasDIUData(); }
    public boolean hasAnyDIUData() { return hasDIUData(); }
    public boolean hasDIUDataTrans() { return dataDIU.hasDIUData(DataType.TRANS); }
    public boolean hasDIUDataProtein() { return dataDIU.hasDIUData(DataType.PROTEIN); }
    public void clearDataDIU(boolean rmvPrm) {
        dataDIU.clearDataDIU(rmvPrm);
    }
    public void clearDataDIU(DataType dataType, boolean rmvPrm) {
        dataDIU.clearDataDIU(dataType, rmvPrm);
    }
    public String getDIUParamsFilepath(DataType dataType) { return dataDIU.getDIUParamsFilepath(dataType); }
    public ArrayList<DataDIU.DIUResultsData> getDIUResultsData(DataType type, double sigValue) { return dataDIU.getDIUResultsData(type, sigValue); }

    //
    // DFI
    //
    public boolean hasDFIData(String id) { return dataDFI.hasDFIData(id); }
    public void clearDataDFI(String id, boolean rmvPrm) { dataDFI.clearDataDFI(id, rmvPrm); }
    public void clearAllDataDFI(boolean rmvPrm) { dataDFI.clearDataDFI(rmvPrm); }
    public String getDFIParamsFilepath(String id) { return dataDFI.getDFIParamsFilepath(id); }
    public String getDFILogFilepath(String id) { return dataDFI.getDFILogFilepath(id); }
    public void setDFIParams(HashMap<String, String> hmParameters, String id) { dataDFI.setDFIParams(hmParameters, id);}
    public HashMap<String, String> getDFIParams(String id) { return dataDFI.getDFIParams(id);}
    public void removeDFIFiles(String id, boolean rmvPrms) { dataDFI.removeDFIFiles(id, rmvPrms); }
    
    public boolean hasAnyDFIData() { return dataDFI.hasAnyDFIData(); }
    
    public ArrayList<DataApp.EnumData> getDFIParamsList() { return dataDFI.getDFIParamsList(); }
    public ArrayList<DataApp.EnumData> getDFIResultsList() { return dataDFI.getDFIResultsList(); }
    
    //
    // DPA
    //
    public boolean hasDPAData() { return dataDPA.hasDPAData(); }
    public void clearDataDPA(boolean rmvPrm) { dataDPA.clearDataDPA(rmvPrm); }
    public String getDPAParamsFilepath() { return dataDPA.getDPAParamsFilepath(); }

    //
    // UTRL
    //
    public boolean hasUTRLData() { return dataUTRL.hasUTRLData(); }
    public void clearDataUTRL(boolean rmvPrm) { dataUTRL.clearDataUTRL(rmvPrm); }
    public String getUTRLParamsFilepath() { return dataUTRL.getUTRLParamsFilepath(); }

    // FA = DFI + DPA + UTRL
    public boolean hasAnyFAData() { return (dataDPA.hasDPAData() || dataDFI.hasAnyDFIData() || dataUTRL.hasUTRLData()); }
    public void clearDataFA() {
        dataDFI.clearDataDFI(true);
        dataDPA.clearDataDPA(true);
        dataUTRL.clearDataUTRL(true);
        removeAllFAResultFiles(true);
    }
    public void removeAllFAResultFiles(boolean rmvPrms) {
        dataDFI.removeAllDFIResultFiles(rmvPrms);
        dataDPA.clearDataDPA(rmvPrms);
        dataUTRL.clearDataUTRL(rmvPrms);
    }
    //
    // EA = FEA + GSEA
    //
    public boolean hasAnyEAData() { return (dataGSEA.hasAnyGSEAData() || dataFEA.hasAnyFEAData()); }
    public void clearDataEA() {
        dataGSEA.clearDataGSEA(true);
        dataFEA.clearDataFEA(true);
        removeAllEAResultFiles(true);
    }
    public void removeAllEAResultFiles(boolean rmvPrms) {
        dataGSEA.removeAllGSEAResultFiles(rmvPrms);
        dataFEA.removeAllFEAResultFiles(rmvPrms);
    }
    
    //
    // FEA
    //
    public boolean hasAnyFEAData() { return dataFEA.hasAnyFEAData(); }
    public void clearDataFEA(String id, boolean rmvPrm) { dataFEA.clearDataFEA(id, rmvPrm); }
    public String getFEAParamsFilepath(String id) { return dataFEA.getFEAParamsFilepath(id); }
    public void removeFEAFiles(String id, boolean rmvPrms) { dataFEA.removeFEAFiles(id, rmvPrms); }
    public ArrayList<DataApp.EnumData> getFEAParamsList() { return dataFEA.getFEAParamsList(); }
    public ArrayList<DataApp.EnumData> getFEAResultsList() { return dataFEA.getFEAResultsList(); }
    public void removeFEAClusterFiles(String id) { dataFEA.removeFEAClusterFiles(id); }
    public void setFEAClusterParams(HashMap<String, String> hmp, String id) { dataFEA.setFEAClusterParams(hmp, id); }

    //
    // GSEA
    //
    public boolean hasAnyGSEAData() { return dataGSEA.hasAnyGSEAData(); }
    public void clearDataGSEA(String id, boolean rmvPrm) { dataGSEA.clearDataGSEA(id, rmvPrm); }
    public String getGSEAParamsFilepath(String id) { return dataGSEA.getGSEAParamsFilepath(id); }
    public void removeGSEAFiles(String id, boolean rmvPrms) { dataGSEA.removeGSEAFiles(id, rmvPrms); }
    public ArrayList<DataApp.EnumData> getGSEAParamsList() { return dataGSEA.getGSEAParamsList(); }
    public ArrayList<DataApp.EnumData> getGSEAResultsList() { return dataGSEA.getGSEAResultsList(); }
    public void removeGSEAClusterFiles(String id) { dataGSEA.removeGSEAClusterFiles(id); }
    public void setGSEAClusterParams(HashMap<String, String> hmp, String id) { dataGSEA.setGSEAClusterParams(hmp, id); }

    //
    // Data Classes
    //
    public static class DEACombinedParams {
        double transSigValue;
        double proteinSigValue;
        double geneSigValue;
    }
    public static class ChromoDistributionData {
        public int genes;
        public int trans;
        public XYChart.Series<String, Number> series;
        public ChromoDistributionData(XYChart.Series<String, Number> series, int genes, int trans) {
            this.series = series;
            this.genes = genes;
            this.trans = trans;
        }
    }
    public static class DEGeneLists {
        public ObservableList<ListNameData> NDE;
        public ObservableList<ListNameData> DE;
        public ObservableList<ListNameData> NoneDE;
        public ObservableList<ListNameData> AllDE;
        public ObservableList<ListNameData> AIE;
        public DEGeneLists() {
            NDE = FXCollections.observableArrayList();
            DE = FXCollections.observableArrayList();
            NoneDE = FXCollections.observableArrayList();
            AllDE = FXCollections.observableArrayList();
            AIE = FXCollections.observableArrayList();
        }
    }
    public static class ListNameData implements Comparable<ListNameData>{
        public final SimpleStringProperty name;
 
        public ListNameData(String name) {
            this.name = new SimpleStringProperty(name);
        }
        public String getName() { return name.get(); }
        @Override
        public int compareTo(ListNameData td) {
            return (name.get().compareTo(td.name.get()));
        }
    }
    public static class DataSelectionResults extends DataApp.SelectionDataResults implements Comparable<DataSelectionResults> {
        public Project project;
        public final SimpleStringProperty name;
        public final SimpleStringProperty filtered;
        public final SimpleStringProperty transcript;
        public SimpleStringProperty protein;
        public SimpleIntegerProperty length;
        public SimpleStringProperty category;
        public SimpleStringProperty attributes;
        public SimpleStringProperty chromo;
        public SimpleStringProperty strand;
        public SimpleIntegerProperty isoforms;
        public SimpleIntegerProperty proteins;
        public SimpleStringProperty coding;
        public SeqAlign.Position pos;
        public final SimpleDoubleProperty[] samples;
        public double[] daSamples;
        
        // name: name/desc of given id
        // transcript: transcript if id is for protein
        // gene and gene description: for transcripts and proteins
        public DataSelectionResults(Project project, boolean selected, DataType dataType, DataInputMatrix.ExpMatrixArray data) {
                //DataDEA.DEAResultsData drd, String name, String transcript, String gene, String geneDescription) {
            super(selected, dataType, data.getId(), data.getGene(), data.getGeneDesc());
            this.project = project;
            this.name = new SimpleStringProperty(data.getName());
            this.filtered = new SimpleStringProperty("NO");
            this.transcript = new SimpleStringProperty(data.getTranscript());
            this.protein = null;
            this.category = new SimpleStringProperty(data.getCategory());
            this.attributes = new SimpleStringProperty(data.getAttributes());
            this.chromo = new SimpleStringProperty(data.getChromo());
            this.strand = new SimpleStringProperty(data.getStrand());
            this.coding = new SimpleStringProperty(data.getCoding());
            this.length = new SimpleIntegerProperty(data.getLength());
            this.isoforms = new SimpleIntegerProperty(data.getIsoforms());
            //this.proteins = new SimpleIntegerProperty(data.getProteins());

            daSamples = data.daSamples;
            int cnt = daSamples.length;
            samples = new SimpleDoubleProperty[cnt];
            for(int i = 0; i < cnt; i++)
                this.samples[i] = new SimpleDoubleProperty(daSamples[i]);
            pos = new SeqAlign.Position(0, 0);
        }
        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getFiltered() { return filtered.get(); }
        public void setFiltered(String value) { filtered.set(value); }
        public String getTranscript() { return transcript.get(); }
        public String getProtein() { 
            if(protein == null)
                protein = new SimpleStringProperty(project.data.getTransProtein(id.get()));
            return protein.get(); 
        }
        public Integer getLength() { return length.get(); }
        public String getCategory() { return category.get(); }
        public void setCategory(String cat) { category.set(cat); }
        public String getAttributes() { return attributes.get(); }
        public void setAttributes(String attrs) { attributes.set(attrs); }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        //public Integer getProteins() { return proteins.get(); }
        public String getCoding() { return coding.get(); }
        public void setCoding(String val) { coding.set(val); }
        // internal use - called from row selection
        public Double getSample(String strIdx) { return samples[Integer.parseInt(strIdx.trim())].get(); }
        @Override
        public int compareTo(DataSelectionResults td) {
            return (id.get().compareToIgnoreCase(td.id.get()));
        }
    }
}
