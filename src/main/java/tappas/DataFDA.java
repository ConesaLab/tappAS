/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import tappas.DataAnnotation.AnnotationType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DataFDA extends AppObject {
    public static final String FDA_RESULTS_SUMMARY = "result_summary";
    public static final String FDA_RESULTS_ID = "result_id";
    public static final String FDA_RESULTS_DETAILS = "result_details";
    public static final String FDA_RESULTS_GENE_DETAILS = "result_gene_details";
    public static final String FDA_FEATUREIDMAP_NAME = "fda_feature_id_map";
    public static final String FDA_COMBINED_NAME = "IDCOMBINED";
    public String getFDAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA).toString(); }
//    public String getFDAParamsFilepath() { return Paths.get(getFDAFolder(), DataApp.PRM_FDA).toString();  }
//    public String getFDALogFilepath() { return Paths.get(getFDAFolder(), DataApp.LOG_NAME).toString(); }
//    public String getFDAResultsSummaryFilepath() { return Paths.get(getFDAFolder(), FDA_RESULTS_SUMMARY).toString(); }
//    public String getFDAResultsDetailsFilepath() { return Paths.get(getFDAFolder(), FDA_RESULTS_DETAILS).toString(); }
//    public String getFDAResultsGeneDetailsFilepath() { return Paths.get(getFDAFolder(), FDA_RESULTS_GENE_DETAILS).toString(); }
//    public String getFDAFeatureIdMapFilepath() { return Paths.get(project.data.getProjectDataFolder(), FDA_FEATUREIDMAP_NAME + DataApp.TSV_EXT).toString(); }

    public String getFDAParamsFilepath(String id) { return Paths.get(getFDAFolder(), DataApp.PRM_FDA + id + DataApp.PRM_EXT).toString();  }
    public String getFDAParamsCombinedResultsFilepath() { return Paths.get(getFDAFolder(), DataApp.PRM_FDA + FDA_COMBINED_NAME + DataApp.PRM_EXT).toString();  }
    public String getFDALogFilepath(String id) { return Paths.get(getFDAFolder(), DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getFDAResultsSummaryFilepath(String id) { return Paths.get(getFDAFolder(), FDA_RESULTS_SUMMARY + "." + id + DataApp.TSV_EXT).toString(); }
    public String getFDAResultsFilepath(String id) { return Paths.get(getFDAFolder(), FDA_RESULTS_ID + "." + id + DataApp.TSV_EXT).toString(); }
    public String getFDAResultsDetailsFilepath(String id) { return Paths.get(getFDAFolder(), FDA_RESULTS_DETAILS + "." + id + DataApp.TSV_EXT).toString(); }
    public String getFDAResultsGeneDetailsFilepath(String id) { return Paths.get(getFDAFolder(), FDA_RESULTS_GENE_DETAILS + "." + id + DataApp.TSV_EXT).toString(); }
    public String getFDAFeatureIdMapFilepath(String id) { return Paths.get(project.data.getProjectDataFolder(), FDA_FEATUREIDMAP_NAME + DataApp.TSV_EXT).toString(); }
    public String getFDADoneFilepath(String id) { return Paths.get(getFDAFolder(), DataApp.DONE_NAME + "." + id + DataApp.TEXT_EXT).toString();  }

    
    public DataFDA(Project project) {
        super(project, null);
    }

    public void initialize() {
        clearData();
    }
    public boolean hasFDAData(String id) { 
        Path summary =  Paths.get(getFDAFolder(), FDA_RESULTS_SUMMARY + "." + id + DataApp.TSV_EXT);
        Path detail = Paths.get(getFDAFolder(), FDA_RESULTS_DETAILS + "." + id + DataApp.TSV_EXT);
        return (Files.exists(summary) && Files.exists(detail));
    }
    
    public boolean hasAnyFDAData() {
        File fdaFolder = new File(Paths.get(getFDAFolder()).toString());
        FilenameFilter filter = (File dir, String name) -> (name.startsWith(DataApp.RESULTS_NAME) && name.endsWith(DataApp.RESULTS_EXT));
        File[] files = fdaFolder.listFiles(filter);
        return (files != null && files.length > 0);
    }

    public boolean hasTwoFDAIdData(){
        boolean res = false;
        HashMap<String, HashMap<String, String>> hmFeatures = getFDAIdDataFeatures();
        for(String feature : hmFeatures.keySet()){
            //genPos and Presence
            System.out.println(feature + hmFeatures.get(feature).size());
            if(hmFeatures.get(feature).size()>1)
                res = true;
        }
        return res;
    }

    public HashMap<String, HashMap<String, String>> getFDAIdDataFeatures(){
        File fdaFolder = new File(Paths.get(getFDAFolder()).toString());
        // Feature - GenPos/Presence - ID
        HashMap<String, HashMap<String,String>> hmFeatures = new HashMap<>();
        HashMap<String,String> hmUsing;
        HashMap<String,String> hmAux;
        boolean res = false;
        ArrayList<String> aux;
        //we have to have data
        if(hasAnyFDAData()){
            FilenameFilter filter = (File dir, String name) -> (name.startsWith(DataApp.RESULTS_NAME) && name.endsWith(DataApp.RESULTS_EXT));
            File[] files = fdaFolder.listFiles(filter);
            //at least 2 results data
            if(files.length > 1){
                for(int i = 0; i < files.length; i++) {
                    String name = files[i].getName();
                    int sidx = name.indexOf('.');
                    if(sidx != -1) {
                        int eidx = name.indexOf(".", ++sidx);
                        if(eidx != -1) {
                            String id = name.substring(sidx, eidx);
                            // get fda params to see if we have two ID analyses
                            HashMap<String, String> hm = getFDAParams(id);
                            if(hm != null && hm.containsKey(DlgFDAnalysis.Params.METHOD_PARAM)) {
                                //if method is ID
                                if (hm.get(DlgFDAnalysis.Params.METHOD_PARAM).equals(DlgFDAnalysis.Params.Method.ID.name())) {
                                    //check using
                                    String feature = hm.get(DlgFDAnalysis.Params.FEATURE_PARAM + "1");
                                    feature = feature.replace("\t","/");
                                    String using = hm.get(DlgFDAnalysis.Params.USING_PARAM);

                                    //check if that feature already exists
                                    if(hmFeatures.containsKey(feature)) {
                                        hmAux = hmFeatures.get(feature);
                                        //if the same using nothing
                                        if(!hmAux.containsKey(using))
                                            hmAux.put(using, id);
                                    }else {
                                        hmUsing = new HashMap<>();
                                        hmUsing.put(using, id);
                                        hmFeatures.put(feature, hmUsing);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        return hmFeatures;
    }
    
    public ArrayList<DataApp.EnumData> getFDAResultsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        HashMap<String, String> hmNames = new HashMap<>();
        
        File fdaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.RESULTS_NAME) && name.endsWith(DataApp.RESULTS_EXT);        
        File[] files = fdaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String fdaName = id;
                        HashMap<String, String> hm = getFDAParams(id);
                        if(hm != null && hm.containsKey(DlgFDAnalysis.Params.NAME_PARAM))
                            fdaName = hm.get(DlgFDAnalysis.Params.NAME_PARAM);
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
    public ArrayList<DataApp.EnumData> getFDAParamsList() {
        ArrayList<DataApp.EnumData> lst = new ArrayList<>();
        File fdaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA).toString());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataApp.PRM_FDA_NAME) && name.endsWith(DataApp.PRM_EXT);        
        File[] files = fdaFolder.listFiles(filter);
        if(files != null) {
            for(int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                int sidx = name.indexOf('.');
                if(sidx != -1) {
                    int eidx = name.indexOf(".", ++sidx);
                    if(eidx != -1) {
                        String id = name.substring(sidx, eidx);
                        String fdaName = id;
                        HashMap<String, String> hm = getFDAParams(id);
                        if(hm != null && hm.containsKey(DlgFDAnalysis.Params.NAME_PARAM))
                            fdaName = hm.get(DlgFDAnalysis.Params.NAME_PARAM);
                        lst.add(new DataApp.EnumData(id, fdaName));
                    }
                }
            }
        }
        return lst;
    }
    
    public void clearData() {
    }
    
    public void clearDataFDA(String id, boolean rmvPrm) {
        clearData();
        //removeAllFDAResultFiles(rmvPrm);
        removeFDAFiles(id, rmvPrm);
    }
    public void removeAllFDAResultFiles(boolean rmvPrms) {
        Utils.removeAllFolderFiles(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA), rmvPrms);
    }
    
    public void removeFDAFiles(String id, boolean rmvPrms) {
        ArrayList<String> lstFiles = getFDAFilesList(id, rmvPrms);
        for(String filepath : lstFiles)
            Utils.removeFile(Paths.get(filepath));
    }
    
    public void setFDAParams(HashMap<String, String> hmp, String id) { 
        if(hmp != null)
            Utils.saveParams(hmp, getFDAParamsFilepath(id));
    }
    public HashMap<String, String> getFDAParams(String id) {
        HashMap<String, String> hm = new HashMap<>();
        Utils.loadParams(hm, getFDAParamsFilepath(id));
        return hm;
    }
    public boolean genDiversityData(HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf, HashMap<String, String> hmParameters, DlgFDAnalysis.Params fdaParams, String id) {
        boolean result = false;
        try {
            logger.logInfo("Building diversity data isoforms sets...");
            HashMap<String, HashMap<String, Object>> hmTGGeneTrans = project.data.getResultsGeneTrans();
            HashMap<String, HashMap<String, Object>> hmFilterGeneTrans = new HashMap<>();
            //For adding genes from list modify this section!!!
            
            //if Genes from file
            if(hmParameters.get(DlgFDAnalysis.Params.TESTLIST_PARAM).equals(DlgFDAnalysis.Params.TestList.FROMFILE.name())){
                ArrayList<String> file_genes = Utils.loadSingleItemListFromFile(hmParameters.get(DlgFDAnalysis.Params.TESTFROMFILE_PARAM), true);
                for(String gene : file_genes) {
                    if(hmTGGeneTrans.containsKey(gene)){
                        HashMap<String, Object> hm = hmTGGeneTrans.get(gene);
                        if(hm.size() > 0)
                            hmFilterGeneTrans.put(gene, hm);
                    }else{
                        logger.logInfo("Gene: " + gene + " not found.");
                    }
                }
            }
            
            if(taf.abortFlag)
                return false;

            //depend on method...
            boolean genpos = false;
            if(fdaParams.using.equals(DlgFDAnalysis.Params.Using.GENPOS)){
                genpos = true;
            }
            
            //List of genes to use
            if(hmFilterGeneTrans.isEmpty()){
                hmFilterGeneTrans = project.data.getResultsGeneTrans();
            }
            
            //Use genpos and work as a normal analysis
            if(hmParameters.get(DlgFDAnalysis.Params.METHOD_PARAM).equals(DlgFDAnalysis.Params.Method.CATEGORY.name())){
                // get genes with diversity
                // feature->gene-><trans, true/false> true if it  contains feature
                HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity;
                if(genpos)
                    hmGeneDiversity = project.data.getDiverseFeaturesUsingNewGenomicPosition(hmFilterGeneTrans, fdaParams.hmFeatures, false, null);
                else //presence
                    hmGeneDiversity = project.data.getDiverseFeaturesUsingPresence(hmFilterGeneTrans, fdaParams.hmFeatures);

                writeGeneFeatureDbg(hmGeneDiversity, getFDAFeatureIdMapFilepath(id) + ".dbg");

                //logger.logInfo("FDA using " + hmFilterGeneTrans.size() + " genes with multiple isoforms from " + hmTGGeneTrans.size() + " total genes.");
                logger.logInfo("FDA using " + hmGeneDiversity.size() + " features with multiple isoforms.");
                //DataAnnotation.DiversityResultsInternal dri = project.data.genAnnotationDiversityData(hmFilterGeneTrans, hmTerms, taf);
                //New Diversity Method from hmGeneDiversity
                DataAnnotation.DiversityResultsInternal dri = project.data.getDiversityDataFromHm(hmGeneDiversity, hmTerms, taf);
                if(taf.abortFlag)
                    return false;

                if(dri != null) {
                    logger.logInfo("Saving results to file....");

                    if(saveFDAResultsData(dri, hmFilterGeneTrans, id))
                        result = true;
                }
            }else{
            //We have to work with features ID and use the using method selected just for the features than can work with it
                HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity;
                if(genpos)
                    hmGeneDiversity = project.data.getDiverseFeaturesUsingNewGenomicPosition(hmFilterGeneTrans, fdaParams.hmFeatures, false, null);
                else //presence
                    hmGeneDiversity = project.data.getDiverseFeaturesUsingPresence(hmFilterGeneTrans, fdaParams.hmFeatures);
                
                writeGeneFeatureDbg(hmGeneDiversity, getFDAFeatureIdMapFilepath(id) + ".dbg");
                
                //logger.logInfo("FDA using " + hmFilterGeneTrans.size() + " genes with multiple isoforms from " + hmTGGeneTrans.size() + " total genes.");
                logger.logInfo("FDA using " + hmGeneDiversity.size() + " features with multiple isoforms.");
                //DataAnnotation.DiversityResultsInternal dri = project.data.genAnnotationDiversityData(hmFilterGeneTrans, hmTerms, taf);
                //New Diversity Method from hmGeneDiversity
                HashMap<String, HashMap<String, Boolean>> dri = project.data.getDiversityDataFromHmID(hmGeneDiversity, hmTerms, taf, genpos);
                if(taf.abortFlag)
                    return false;

                if(dri != null) {
                    logger.logInfo("Saving results to file....");

                    if(saveFDAResultsData(dri, hmFilterGeneTrans, id))
                        result = true;
                }
            } 
        } catch(Exception e) {
            result = false;
            logger.logError("Functional Diversity Analysis error: " + e.getMessage());
        }
        return result;
    }
    
    public boolean saveFDAResultsData(DataAnnotation.DiversityResultsInternal dri, HashMap<String, HashMap<String, Object>> hmFilterGeneTrans, String id) {
        boolean result = false;
        DataAnnotation.DiversityResults fdResults = dri.fdr;
        
        // save diversity summary results data
        String tsv = fdResults.toTSV();
        String filepath = getFDAResultsSummaryFilepath(id);
        Utils.createFilepathFolder(filepath);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            writer.write(tsv);
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to create Functional Diversity file '" + filepath + "': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { result = false;}
        }

        // save gene and isoforms detailed data
        if(result) {
            result = false;
            filepath = getFDAResultsDetailsFilepath(id);
            writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                //String isoforms;
                HashMap<String, Object> hmIsos;
                for(String gene : hmFilterGeneTrans.keySet()) {
                    hmIsos = hmFilterGeneTrans.get(gene);
                    if(hmIsos.size() < 2)
                        continue;
                    String transcript = "";
                    for(String term : dri.hmTranscript.keySet()) {
                        HashMap<String, Boolean> hmr = dri.hmTranscript.get(term);
                        if(hmr.containsKey(gene)) {
                            if(!transcript.isEmpty())
                                transcript += ",";
                            transcript += term + ":" + (hmr.get(gene)? "V" : "S");
                        }
                    }
                    String protein = "";
                    for(String term : dri.hmProtein.keySet()) {
                        HashMap<String, Boolean> hmr = dri.hmProtein.get(term);
                        if(hmr.containsKey(gene)) {
                            if(!protein.isEmpty())
                                protein += ",";
                            protein += term + ":" + (hmr.get(gene)? "V" : "S");
                        }
                    }
                    String genomic = "";
                    for(String term : dri.hmGenomic.keySet()) {
                        HashMap<String, Boolean> hmr = dri.hmGenomic.get(term);
                        if(hmr.containsKey(gene)) {
                            if(!genomic.isEmpty())
                                genomic += ",";
                            genomic += term + ":" + (hmr.get(gene)? "V" : "S");
                        }
                    }
                    if(transcript.isEmpty())
                        transcript = ".";
                    if(protein.isEmpty())
                        protein = ".";
                    if(genomic.isEmpty())
                        genomic = ".";
                    writer.write(gene + "\t" + transcript + "\t" + protein + "\t" + genomic + "\n");
                }
                result = true;
            } catch (IOException e) {
                logger.logError("Unable to create Functional Diversity file '" + filepath + "': " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        
        // always attempt to remove file if failure
        if(!result)
            Utils.removeFile(filepath);
        return result;
    }
    
    // BY ID (HashMap as input - Source/Feature/ID - Gene - Diverse/NotDiverse)
    public boolean saveFDAResultsData(HashMap<String, HashMap<String, Boolean>> dri, HashMap<String, HashMap<String, Object>> hmFilterGeneTrans, String id) {
        //by now just one feature
        boolean result = false;
        HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
        ArrayList<String> lstCounts = new ArrayList<>();
        AnnotationType type = AnnotationType.TRANS;
        
        for(String ddbb : dri.keySet()){
            String db = ddbb.split("\t")[0];
            String feat = ddbb.split("\t")[1];
            String fid = ddbb.split("\t")[2];
            String name = db + "\t" + feat + "\t" + fid;
            
            String group = hmType.get(db).get(feat);
            if(AnnotationType.TRANS.name().startsWith(group))
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group))
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group))
                type = AnnotationType.GENOMIC;
            
            HashMap<String, Boolean> hmGene = dri.get(ddbb);
            int cntDiverse = 0;
            int cntNotDiverse = 0;
            for(String gene : hmGene.keySet()){
                if(hmGene.get(gene))
                    cntDiverse++;
                else
                    cntNotDiverse++;
            }
            
            lstCounts.add(ddbb + "\t" + cntDiverse + "\t" + cntNotDiverse + "\n");
        }
        
        String filepath = getFDAResultsSummaryFilepath(id);
        Utils.createFilepathFolder(filepath);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            for(String i : lstCounts)
                writer.write(i);
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to create Functional Diversity file '" + filepath + "': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { result = false;}
        }
        
        // save id and gene detailed data
        if(result) {
            result = false;
            filepath = getFDAResultsDetailsFilepath(id);
            String filepath_genes = getFDAResultsGeneDetailsFilepath(id);
            writer = null;
            Writer writer_genes = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer_genes = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath_genes), "utf-8"));
                //ID
                writer_genes.write("ID\tGene\tCat\n");
                String msg_genes = "";
                for(String ddbb : dri.keySet()){
                    
                    String db = ddbb.split("\t")[0];
                    String feat = ddbb.split("\t")[1];
                    String fid = ddbb.split("\t")[2];
                    String fid_name = db + "/" + feat + "/" + fid;
                    
                    String transcript = ".";
                    String protein = ".";
                    String genomic = ".";
                    
                    HashMap<String, Boolean> hmGene = dri.get(ddbb);
                    String gene_result = "";
                    
                    int cntDiverse = 0;
                    int cntNotDiverse = 0;
                    
                    for(String gene : hmGene.keySet()){
                        if(hmGene.get(gene)){
                           cntDiverse++;
                           msg_genes += fid_name + "\t" + gene + "\tV\n";
                        }
                        else{
                           cntNotDiverse++;
                           msg_genes += fid_name + "\t" + gene + "\tS\n";
                        }
                    }
                    
                    gene_result = "VARYING:" + cntDiverse + ",NOT VARYING:" + cntNotDiverse;
                    switch(type){
                        case TRANS:{
                            writer.write(fid_name + "\t" + gene_result + "\t" + protein + "\t" + genomic + "\n");
                            break;
                        }
                        case PROTEIN:{
                            writer.write(fid_name + "\t" + transcript + "\t" + gene_result + "\t" + genomic + "\n");
                            break;
                        }
                        case GENOMIC:{
                            writer.write(fid_name + "\t" + transcript + "\t" + protein + "\t" + gene_result + "\n");
                            break;
                        }
                    }
                }
                writer_genes.write(msg_genes);
                result = true;
            } catch (IOException e) {
                logger.logError("Unable to create Functional Diversity file '" + filepath + "': " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close(); if(writer_genes != null) writer_genes.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        
        // always attempt to remove file if failure
        if(!result)
            Utils.removeFile(filepath);
        return result;
    }
    
    public DataAnnotation.DiversityResults getFDAResultsSummaryData(String id) {
        DataAnnotation.DiversityResults dr = new DataAnnotation.DiversityResults();
        String filepath = getFDAResultsSummaryFilepath(id);
        if(Files.exists(Paths.get(filepath))) {
            try {
                List<String> lstLines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                dr = new DataAnnotation.DiversityResults(lstLines);
            }
            catch (Exception e) {
                dr = new DataAnnotation.DiversityResults();
                logger.logError("Unable to load existing Functional Diversity data from file: " + e.getMessage());
            }
        }
        return dr;
    }
    public DiversityResults getFDAResults(String method, String id) {
        DiversityResults dr = new DiversityResults();
        String filepath = getFDAResultsDetailsFilepath(id);
        if(Files.exists(Paths.get(filepath))) {
            try {
                if(method.equals(DlgFDAnalysis.Params.Method.CATEGORY.name())){
                
                    List<String> lstLines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                    HashMap<String, Boolean> hmGeneSources;
                    HashMap<String, Object> hmSources;
                    String type;
                    String fields[], subFields[];
                    int lnum = 1;
                    for(String line : lstLines) {
                        fields = line.split("\t");
                        if(fields.length == 4) {
                            DiversityGeneResults dgr = new DiversityGeneResults();
                            dr.hmGeneResults.put(fields[0], dgr);
                            //dgr.isoforms = fields[1]; - not used
                            for(int idx = 1; idx < 4; idx++) {
                                if(idx == 1)
                                    type = AnnotationType.TRANS.name();
                                else if(idx == 2)
                                    type = AnnotationType.PROTEIN.name();
                                else
                                    type = AnnotationType.GENOMIC.name();
                                hmSources = null;
                                hmGeneSources = null;
                                if(!fields[idx].equals(".")) {
                                    subFields = fields[idx].split(",");
                                    for(String src : subFields) {
                                        String[] vals = src.split(":");
                                        if(vals.length == 3) {
                                            // update global sources list if needed
                                            if(hmSources == null) {
                                                if(dr.hmSources.containsKey(type))
                                                    hmSources = dr.hmSources.get(type);
                                                else {
                                                    hmSources = new HashMap<>();
                                                    dr.hmSources.put(type, hmSources);
                                                }
                                            }
                                            String key = vals[0] + ":" + vals[1];
                                            if(!hmSources.containsKey(key))
                                                hmSources.put(key, null);

                                            // update gene sources results
                                            if(hmGeneSources == null) {
                                                // don't need to check only do this once per gene, just create a new one
                                                hmGeneSources = new HashMap<>();
                                                dgr.hmResults.put(type, hmGeneSources);
                                            }
                                            hmGeneSources.put(key, vals[2].equals("V"));
                                        }
                                        else
                                            throw new Exception("Invalid content in line " + lnum +".");
                                    }
                                }
                            }
                        }
                        else {
                            dr = new DiversityResults();
                            logger.logError("Invalid content in Functional Diversity gene isoforms data file, line " + lnum + ".");
                            break;
                        }
                        lnum++;
                    }
                //ID load
                }else{
                    List<String> lstLines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                    HashMap<String, Integer> hmID;
                    HashMap<String, Object> hmColumns;
                    String type;
                    String fields[], subFields[];
                    int lnum = 1;
                    for(String line : lstLines) {
                        fields = line.split("\t");
                        if(fields.length == 4) {
                            DiversityIDResults dgr = new DiversityIDResults();
                            dr.hmIDResults.put(fields[0], dgr);
                            //dgr.isoforms = fields[1]; - not used
                            for(int idx = 1; idx < 4; idx++) {
                                if(idx == 1)
                                    type = AnnotationType.TRANS.name();
                                else if(idx == 2)
                                    type = AnnotationType.PROTEIN.name();
                                else
                                    type = AnnotationType.GENOMIC.name();
                                hmColumns = null;
                                hmID = null;
                                if(!fields[idx].equals(".")) {
                                    subFields = fields[idx].split(",");
                                    for(String src : subFields) {
                                        String[] vals = src.split(":");
                                        if(vals.length == 2) { //VARYING + QUANTITY
                                            if(hmColumns == null) {
                                                if(dr.hmSources.containsKey(type))
                                                    hmColumns = dr.hmSources.get(type);
                                                else {
                                                    hmColumns = new HashMap<>();
                                                    dr.hmSources.put(type, hmColumns);
                                                }
                                            }
                                            String key = vals[0];
                                            if(!hmColumns.containsKey(key))
                                                hmColumns.put(key, null);

                                            // update gene sources results
                                            if(hmID == null) {
                                                // don't need to check only do this once per gene, just create a new one
                                                hmID = new HashMap<>();
                                                dgr.hmResults.put(type, hmID);
                                            }
                                            hmID.put(key, Integer.parseInt(vals[1]));
                                        }
                                        else
                                            throw new Exception("Invalid content in line " + lnum +".");
                                    }
                                }
                            }
                        }
                        else {
                            dr = new DiversityResults();
                            logger.logError("Invalid content in Functional Diversity gene isoforms data file, line " + lnum + ".");
                            break;
                        }
                        lnum++;
                    }
                }
            }
            catch (Exception e) {
                dr = new DiversityResults();
                logger.logError("Unable to load existing Functional Diversity gene isoforms data from file: " + e.getMessage());
            }
        }
        String path = getFDADoneFilepath(id);
        if(!Utils.createDoneFile(path, id, logger))
            logger.logError("Unable to create Done File for Functional Diversity");
        return dr;
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
    
    //Internal Functions
    
    private ArrayList<String> getFDAFilesList(String id, boolean rmvPrms) {
        ArrayList<String> lst = new ArrayList<>();
        File fdaFolder = new File(Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_FDA).toString());
        FilenameFilter filter = (File dir, String name) -> {
            if(rmvPrms)
                return (name.endsWith(DataApp.PRM_EXT) || name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
            else
                return (name.endsWith(DataApp.RESULTS_EXT) || name.endsWith(DataApp.TEXT_EXT));
        };        
        File[] files = fdaFolder.listFiles(filter);
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
    public static class DiversityResults {
        HashMap<String, HashMap<String, Object>> hmSources = new HashMap<>();
        HashMap<String, DiversityGeneResults> hmGeneResults = new HashMap<>();
        HashMap<String, DiversityIDResults> hmIDResults = new HashMap<>();
    }
    public static class DiversityGeneResults {
        HashMap<String, HashMap<String, Boolean>> hmResults = new HashMap<>();
    }
    public static class DiversityIDResults {
        HashMap<String, HashMap<String, Integer>> hmResults = new HashMap<>();
    }
    public static class DataSelectionResults extends DataApp.SelectionDataResults implements Comparable<DataSelectionResults> {
        public Project project;
        public final SimpleStringProperty[] sources;
        public final SimpleIntegerProperty[] intsources;
        public String[] strSources;
        public Integer[] intSources;
        public double pValue;
        public double adjPValue;

        //To String parameters (CAT method)
        public DataSelectionResults(Project project, boolean selected, String gene, String geneDescription, String[] strSources) {
            super(selected, DataApp.DataType.GENE, gene, gene, geneDescription);
            this.project = project;
            this.strSources = strSources;
            int cnt = strSources.length;
            sources = new SimpleStringProperty[cnt];
            intsources = null;
            for(int i = 0; i < cnt; i++)
                this.sources[i] = new SimpleStringProperty(strSources[i]);
        }
        
        //To Integer parameters (ID method)
        public DataSelectionResults(Project project, boolean selected, String gene, String geneDescription, Integer[] strSources) {
            super(selected, DataApp.DataType.GENE, gene, gene, geneDescription);
            this.project = project;
            this.intSources = strSources;
            int cnt = strSources.length;
            intsources = new SimpleIntegerProperty[cnt];
            sources = null;
            for(int i = 0; i < cnt; i++)
                this.intsources[i] = new SimpleIntegerProperty(strSources[i]);
        }

        // internal use - called from row selection
        public String getSource(String strIdx) { return sources[Integer.parseInt(strIdx.trim())].get(); }
        public int getIntSource(String strIdx) { return intsources[Integer.parseInt(strIdx.trim())].get(); }
        public double getPValue() { return pValue; }
        public double getAdjPValue() { return adjPValue; }

        @Override
        public int compareTo(DataSelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }

    public static class DataSelectionCombinedResults extends DataApp.SelectionDataResults implements Comparable<DataSelectionResults> {
        public Project project;
        
        public final SimpleStringProperty id;
        public final SimpleStringProperty description;
        
        public SimpleDoubleProperty pValue_genomic;
        public SimpleDoubleProperty adjPValue_genomic;
        public SimpleIntegerProperty varying_genomic;
        public SimpleIntegerProperty notVarying_genomic;

        public SimpleDoubleProperty pValue_presence;
        public SimpleDoubleProperty adjPValue_presence;
        public SimpleIntegerProperty varying_presence;
        public SimpleIntegerProperty notVarying_presence;
        
        public SimpleDoubleProperty[] genomic_data = new SimpleDoubleProperty[2];
        public SimpleDoubleProperty[] presence_data = new SimpleDoubleProperty[2];

        public SimpleIntegerProperty[] genomic_varying = new SimpleIntegerProperty[2];
        public SimpleIntegerProperty[] presence_varying = new SimpleIntegerProperty[2];
        
        public DataSelectionCombinedResults(Project project, boolean selected, String id, String idDescription, String pValue_genomic, String adjPValue_genomic,
                                            String varying_genomic, String notVarying_genomic, String pValue_presence, String adjPValue_presence, String varying_presence, String notVarying_presence) {
            super(selected, DataApp.DataType.GENE, id, id, idDescription);
            this.project = project;
            this.id = new SimpleStringProperty(id);
            this.description = new SimpleStringProperty(idDescription);

            this.pValue_genomic = new SimpleDoubleProperty(Double.parseDouble(pValue_genomic));
            this.adjPValue_genomic = new SimpleDoubleProperty(Double.parseDouble(adjPValue_genomic));
            this.varying_genomic = new SimpleIntegerProperty(Integer.parseInt(varying_genomic));
            this.notVarying_genomic = new SimpleIntegerProperty(Integer.parseInt(notVarying_genomic));

            this.pValue_presence = new SimpleDoubleProperty(Double.parseDouble(pValue_presence));
            this.adjPValue_presence = new SimpleDoubleProperty(Double.parseDouble(adjPValue_presence));
            this.varying_presence = new SimpleIntegerProperty(Integer.parseInt(varying_presence));
            this.notVarying_presence = new SimpleIntegerProperty(Integer.parseInt(notVarying_presence));
        
            this.genomic_data[0] = this.pValue_genomic;
            this.genomic_data[1] = this.adjPValue_genomic;
            this.genomic_varying[0] = new SimpleIntegerProperty(Integer.parseInt(varying_genomic));
            this.genomic_varying[1] = new SimpleIntegerProperty(Integer.parseInt(notVarying_genomic));
            
            this.presence_data[0] = this.pValue_presence;
            this.presence_data[1] = this.adjPValue_presence;
            this.presence_varying[0] = new SimpleIntegerProperty(Integer.parseInt(varying_presence));
            this.presence_varying[1] = new SimpleIntegerProperty(Integer.parseInt(notVarying_presence));
        }

        // internal use - called from row selection
        public String getId() { return id.get(); }
        public String getDescription() { return description.get(); }
        
        public double getPValueGenomic() { return pValue_genomic.doubleValue(); }
        public double getAdjPValueGenomic() { return adjPValue_genomic.doubleValue(); }
        public int getVaryingGenomic() { return varying_genomic.getValue(); }
        public int getNotVaryingGenomic() { return notVarying_genomic.getValue(); }
        
        public double getPValuePresence() { return pValue_presence.doubleValue(); }
        public double getAdjPValuePresence() { return adjPValue_presence.doubleValue(); }
        public int getVaryingPresence() { return varying_presence.getValue(); }
        public int getNotVaryingPresence() { return notVarying_presence.getValue(); }

        @Override
        public int compareTo(DataSelectionResults td) {
            return (gene.get().compareTo(td.gene.get()));
        }
    }
}
