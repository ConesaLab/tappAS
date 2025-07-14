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
import tappas.DlgInputData.Params.ExpMatrixGroup;
import tappas.DlgInputData.Params.ExpMatrixTime;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DataInputMatrix extends AppObject {
    ExpMatrixParams params;
    ExpMatrixData expInputMatrix;
    
    public DataInputMatrix(Project project) {
        super(project, null);
    }
    // initialization function
    public void initialize() {
        clearData();
        params = new ExpMatrixParams(project.data.getParams().getParams());
    }
    public void clearData() {
        expInputMatrix = null;
    }
    public ExpMatrixParams getInputMatrixParams() { return params; }
    public ArrayList<ExpMatrixCondition> getExpressionMatrixConditions() {
        ArrayList<ExpMatrixCondition> conds = new ArrayList<>();
        for(int c = 0; c < params.lstGroups.size(); c++)
            conds.add(new ExpMatrixCondition(params.lstGroups.get(c)));
        return conds;
    }
    public String[] getGroupNames() {
        return params.getGroupNames();
    }
    public String[] getExpTypeGroupNames() {
        return params.getExpTypeGroupNames();
    }
    public int getTimePoints() {
        return params.getTimePoints();
    }
    public String[] getTimePointNames() {
        return params.getTimePointNames();
    }
    public String[] getResultNames() {
        return params.getResultNames();
    }
    public String[] getGroupsTimeNames() {
        return params.getGroupsTimeNames();
    }
    public String[] getGroupTimeNames(int grpnum) {
        return params.getGroupTimeNames(grpnum);
    }
    // get original input expression counts for transcripts
    public ExpMatrixData getOriginalExpressionData() {
        return loadOriginalMatrixFile();
    }
    // get raw expression counts for transcripts - original expression matrix after filtering out non-annotated transcripts
    public ExpMatrixData getRawExpressionData(HashMap<String, Object> hmFilterTrans) {
        ExpMatrixData expData = null;
        
        // load data if not previously loaded
        if(expInputMatrix == null)
            loadInputMatrixFile();
        if(expInputMatrix != null) {
            int nsamples = params.getTotalSamplesCount();
            ArrayList<ExpMatrixCondition> conds = getExpressionMatrixConditions();
            ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
            boolean filter = (hmFilterTrans != null && !hmFilterTrans.isEmpty());
            for(ExpMatrixArray ema : expInputMatrix.data) {
                String trans = ema.getTranscript();
                if(!filter || hmFilterTrans.containsKey(trans)) {
                    double[] samples = new double[nsamples];
                    for(int i = 0; i < nsamples; i++)
                        samples[i] = ema.samples[i].get();
                    DataAnnotation.TransData td = project.data.getTransData(trans);
                    if(td != null)
                        data.add(new ExpMatrixArray(trans, "", "", td.gene, project.data.getGeneDescription(td.gene), td.alignCat, td.alignAttrs, td.chromo, (td.negStrand? "-" : "+"), td.coding, samples));
                }
            }
            Collections.sort(data);
            expData = new ExpMatrixData(conds, data);
        }
        
        return expData;
    }

    // get original raw expression counts for given data type
    public ExpMatrixData getRawExpressionData(DataType type, HashMap<String, Object> hmFilterTrans) {
        return getRawExpressionDataExt(type, false, hmFilterTrans);
    }
    private ExpMatrixData getRawExpressionDataExt(DataType type, boolean geneProteins, HashMap<String, Object> hmFilterTrans) {
        ExpMatrixData expData = null;
        
        // load data if not previously loaded
        if(expInputMatrix == null)
            loadInputMatrixFile();
        if(expInputMatrix != null) {
            int nsamples = params.getTotalSamplesCount();
            ArrayList<ExpMatrixCondition> conds = getExpressionMatrixConditions();
            String gene, protein;
            HashMap<String, double[]> hmGenes = new HashMap<>();
            HashMap<String, double[]> hmProteins = new HashMap<>();
            ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
            if(hmFilterTrans == null)
                hmFilterTrans = new HashMap<>();
            boolean filter = !hmFilterTrans.isEmpty();
            for(ExpMatrixArray ema : expInputMatrix.data) {
                // check if filtering by category and set process flag accordingly
                String trans = ema.getTranscript();
                if(!filter || hmFilterTrans.containsKey(trans)) {
                    double[] samples = new double[nsamples];
                    for(int i = 0; i < nsamples; i++)
                        samples[i] = ema.samples[i].get();
                    if(type == DataType.TRANS) {
                        DataAnnotation.TransData td = project.data.getTransData(trans);
                        if(td != null) {
                            ExpMatrixArray expema = new ExpMatrixArray(trans, "", trans, td.gene, project.data.getGeneDescription(td.gene), td.alignCat, td.alignAttrs, td.chromo, (td.negStrand? "-" : "+"), td.coding, samples);
                            expema.length = new SimpleIntegerProperty(project.data.getTransLength(trans));
                            data.add(expema);
                        }
                    }
                    else if(type == DataType.GENE) {
                        gene = project.data.getTransGene(trans);
                        if(!gene.isEmpty()) {
                            if(!hmGenes.containsKey(gene))
                                hmGenes.put(gene, samples);
                            else {
                                double[] gsamples = hmGenes.get(gene);
                                for(int i = 0; i < nsamples; i++)
                                    gsamples[i] = ((double)Math.round((gsamples[i] + samples[i])*100)/100.0);
                            }
                        }
                    }
                    else if(type == DataType.PROTEIN) {
                        if(project.data.isTransCoding(ema.getTranscript())) {
                            protein = project.data.getTransAssignedProtein(trans);
                            if(geneProteins) {
                                gene = project.data.getTransGene(trans);
                                protein = gene + "\t" + protein;
                            }
                            if(!hmProteins.containsKey(protein))
                                hmProteins.put(protein, samples);
                            else {
                                double[] gsamples = hmProteins.get(protein);
                                for(int i = 0; i < nsamples; i++)
                                    gsamples[i] = ((double)Math.round((gsamples[i] + samples[i])*100)/100.0);
                            }
                        }
                    }
                }
            }
            if(type == DataType.GENE) {
                String chromo, strand;
                Boolean coding;
                int isoforms;
                for(String expgene : hmGenes.keySet()) {
                    chromo = project.data.getGeneChromo(expgene);
                    strand = project.data.getGeneStrand(expgene);
                    isoforms = project.data.getGeneTransCount(expgene);
                    coding = project.data.isGeneCoding(expgene);
                    ExpMatrixArray ema = new ExpMatrixArray(expgene, project.data.getGeneDescription(expgene), "", "", "", "", "", chromo, strand, coding, hmGenes.get(expgene));
                    ema.isoforms = new SimpleIntegerProperty(isoforms);
                    data.add(ema);
                }
            }
            else if(type == DataType.PROTEIN) {
                String chromo, strand;
                Boolean coding;
                int length;
                for(String expprot : hmProteins.keySet()) {
                    String expgene, expprotein;
                    ExpMatrixArray ema;
                    if(geneProteins) {
                        String[] fields = expprot.split("\t");
                        expgene = fields[0];
                        expprotein = fields[1];
                        ema = new ExpMatrixArray(expgene + "_" + expprotein, "", "", expgene, project.data.getGeneDescription(expgene), "", "", "", "", true, hmProteins.get(expprot));
                    }
                    else {
                        expprotein = expprot;
                        ArrayList<String> lst = project.data.getProteinTrans(expprotein, hmFilterTrans);
                        HashMap<String, Object> hmProtGenes = new HashMap<>();
                        String exptrans = "";
                        String expgenes = "";
                        String expgenesdesc = "";
                        for(String trans : lst) {
                            exptrans += (exptrans.isEmpty()? "" : ",") + trans;

                            // the way the TSV is built, the same protein id can be from different transcripts
                            // and also from different genes
                            String transGene = project.data.getTransGene(trans);
                            if(!hmProtGenes.containsKey(transGene)) {
                                hmProtGenes.put(transGene, null);
                                expgenes += (expgenes.isEmpty()? "" : ",") + transGene;
                                expgenesdesc += (expgenesdesc.isEmpty()? "" : "; ") + project.data.getGeneDescription(transGene);
                            }
                        }
                        ema = new ExpMatrixArray(expprot, "", exptrans, expgenes, expgenesdesc, "", "", "", "", true, hmProteins.get(expprot));
                    }
                    length = project.data.getProteinLength(expprotein, hmFilterTrans);
                    ema.length = new SimpleIntegerProperty(length);
                    data.add(ema);
                }
            }
            Collections.sort(data);
            expData = new ExpMatrixData(conds, data);
        }
        
        return expData;
    }
    public HashMap<String, HashMap<String, Object>> getInputMatrixGeneTrans() {
        HashMap<String, HashMap<String, Object>> hmInputData = new HashMap<>();
        if(expInputMatrix == null)
            loadInputMatrixFile();
        if(expInputMatrix != null) {
            String gene, trans;
            for(ExpMatrixArray ema : expInputMatrix.data) {
                trans = ema.getTranscript();
                gene = project.data.getTransGene(trans);
                if(!gene.isEmpty()) {
                    if(hmInputData.containsKey(gene))
                        hmInputData.get(gene).put(trans, null);
                    else {
                        HashMap<String, Object> hm = new HashMap<>();
                        hm.put(trans, null);
                        hmInputData.put(gene, hm);
                    }
                }
            }            
        }
        return hmInputData;
    }

    // get original expression matrix data as provided by the user
    // however transcripts with no annotations were removed from the application copy
    public ExpMatrixData getInputMatrixData() {
        ExpMatrixData expData = null;
        
        // load data if not previously loaded
        if(expInputMatrix == null)
            loadInputMatrixFile();
        if(expInputMatrix != null) {
            int nsamples = params.getTotalSamplesCount();
            ArrayList<ExpMatrixCondition> conds = getExpressionMatrixConditions();
            ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
            for(ExpMatrixArray ema : expInputMatrix.data) {
                double[] samples = new double[nsamples];
                for(int i = 0; i < nsamples; i++)
                    samples[i] = ema.samples[i].get();
                String trans = ema.getTranscript();
                DataAnnotation.TransData td = project.data.getTransData(trans);
                if(td != null)
                    data.add(new ExpMatrixArray(trans, "", trans, td.gene, project.data.getGeneDescription(td.gene), td.alignCat, td.alignAttrs, td.chromo, (td.negStrand? "-" : "+"), td.coding, samples));
            }
            Collections.sort(data);
            expData = new ExpMatrixData(conds, data);
        }
        
        return expData;
    }

    //
    // File Generation Functions
    //

    public boolean copyExpFactorsFile(String filepath) {
        return Utils.copyFile(Paths.get(project.data.getInputFactorsFilepath()), Paths.get(filepath));
    }
    // generate expression factors file for DE Analysis, etc.
    public boolean genExpFactorsFile() {
        boolean result = false;
        String filepath = project.data.getInputFactorsFilepath();
        try {
            long tstart = System.nanoTime();
            logger.logInfo("Writing expression matrix factors file to " + filepath);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer.write("\tReplicate\n");
                writer.write(params.getFactors());
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logInfo("Generated expression matrix factors file in " + duration + " ms\n");
            } catch (IOException e) {
                logger.logError("Unable to generate expression matrix factors file: " + e.getMessage());
                result = false;
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to generate expression matrix factors file: " + e.getMessage());
            result = false;
        }
        if(result)
            result = genTimeFactorsFile();
        
        // remove file if anything failed
        if(!result)
            Utils.removeFile(Paths.get(filepath));
        
        return result;
    }
    public boolean copyTimeFactorsFile(String filepath) {
        return Utils.copyFile(Paths.get(project.data.getInputTimeFactorsFilepath()), Paths.get(filepath));
    }
    // generate time factors file for maSigPro
    private boolean genTimeFactorsFile() {
        boolean result = false;
        String filepath = project.data.getTimeFactorsFilepath();
        try {
            long tstart = System.nanoTime();
            logger.logInfo("Writing time course factors file to " + filepath);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                String header = "\tTime\tReplicate";
                for(ExpMatrixGroup emg : params.lstGroups)
                    header += "\t" + emg.name;
                header += "\n";
                writer.write(header);
                writer.write(params.getTimeFactors());
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logInfo("Generated time factors file in " + duration + " ms\n");
            } catch (IOException e) {
                logger.logError("Unable to generate time factors file: " + e.getMessage());
                result = false;
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to generate time factors file: " + e.getMessage());
            result = false;
        }
        
        // remove file if anything failed
        if(!result)
            Utils.removeFile(Paths.get(filepath));
        
        return result;
    }

    // get expression matrix file header
    public String getExpMatrixFileHeader() {
        return params.getHeader();
    }
    // generate expression data file for DIU Analysis. Use original raw counts
    public boolean genExpressionRawFile(DataType type, boolean geneProteins, HashMap<String, Object> hmFilterTrans, String filepath) {
        boolean result = false;
        ExpMatrixData emd = getRawExpressionDataExt(type, geneProteins, hmFilterTrans);
        if(emd != null) {
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                String header = params.getHeader();
                int nsamples = params.getTotalSamplesCount();
                writer.write(header + "\n");
                
                String line;
                double[] expsamples;
                for(ExpMatrixArray em : emd.data) {
                    expsamples = em.daSamples;
                    line = em.getId();
                    for(int i = 0; i < nsamples; i++)
                        line += "\t" + String.format("%.2f", ((double)Math.round(expsamples[i]*100)/100.0));
                    writer.write(line + "\n");
                }
                result = true;
            }
            catch (Exception e) {
                logger.logError("genExpressionRawFile Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    // process isoforms expression data matrix input file and make application copy
    // right now we change the header of the file:
    // from NSC   NSC   OLD   OLD
    // to   NSC_1 NSC_2 OLD_1 OLD_2
    public boolean genMasterExpressionFile(HashMap<String, String> hmParams) {
        boolean result = false;
        
        // clear all relevant files
        Utils.removeFile(Paths.get(project.data.getOriginalMatrixFilepath()));
        Utils.removeFile(Paths.get(project.data.getInputMatrixFilepath()));
        Utils.removeFile(Paths.get(project.data.getInputMatrixNATransFilepath()));
        
        // clear all existing data and get user entered parameters
        clearData();
        params = new ExpMatrixParams(hmParams);
        ArrayList<String> lstCols = params.getSampleNames();
        
        String header = params.getHeader();
        int nsamples = params.getTotalSamplesCount();
        logger.logInfo("Experiment type: " + params.getExperimentTypeName());
        if(params.experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison))
            logger.logInfo("Matrix has " + params.getGroupsCount() + " experimental groups with a total of " + nsamples + " samples.");
        else
            logger.logInfo("Matrix has " + params.getGroupsCount() + " experimental groups, " + params.getTotalTimes() + " time events, and a total of " + nsamples + " samples.");
        Writer writerAll = null;
        Writer writer = null;
        try {
            long tstart = System.nanoTime();
            // copy design file
            logger.logDebug("Copying experimental design file: " + params.srcEDFilepath);
            Utils.copyFile(Paths.get(params.srcEDFilepath), Paths.get(project.data.getDesignFilepath()));
            // copy expression matrix, original and input (filtered)
            logger.logDebug("Copying expression matrix input file: " + params.srcEMFilepath);
            List<String> lines = Files.readAllLines(Paths.get(params.srcEMFilepath), StandardCharsets.UTF_8);
            long tend = System.nanoTime();
            long duration = (tend - tstart)/1000000;
            logger.logDebug("Loaded expression matrix input data in " + duration + " ms");
            // we do not have the project DB populated at this point
            // we will use the transcripts from the annotation DB
            HashMap<String, Object> hmTrans = project.data.getAnnotationTrans();
            ArrayList<String> lstbad = new ArrayList<>();
            writerAll = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(project.data.getOriginalMatrixFilepath(), false), "utf-8"));
            writerAll.write(header + "\n");
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(project.data.getInputMatrixFilepath(), false), "utf-8"));
            writer.write(header + "\n");
            int lnum = 1;
            String[] fields;
            result = true;
            int[] colidx = new int[nsamples];
            int reqcols = nsamples + 1;
            for(String line : lines) {
                if(lnum == 1) {
                    line = line.trim();
                    fields = line.split("\t");
                    int srcidx = 0;
                    for(String colname : lstCols) {
                        int dstidx = 0;
                        boolean fnd = false;
                        for(String sample : fields) {
                            if(colname.equals(sample)) {
                                if(reqcols < (dstidx + 1))
                                    reqcols = dstidx + 1;
                                colidx[srcidx++] = dstidx + 1;
                                fnd = true;
                                break;
                            }
                            dstidx++;
                        }
                        if(!fnd) {
                            result = false;
                            logger.logError("Sample column, " + colname + ", not found in expression matrix.");
                            break;
                        }
                    }
                    if(result) {
                        String stridx = "";
                        for(int idx : colidx)
                            stridx += (stridx.isEmpty()? "" : ", ") + idx;
                        logger.logInfo("Sample column mapping: " + stridx);
                    }
                    else
                        break;
                }
                else if(!line.trim().isEmpty()) {
                    fields = line.split("\t");
                    if(fields.length >= reqcols) {
                        String outline = fields[0];
                        for(int idx : colidx)
                            outline += "\t" + fields[idx];
                        writerAll.write(outline + "\n");
                        if(hmTrans.containsKey(fields[0]))
                            writer.write(outline + "\n");
                        else
                            lstbad.add(fields[0]);
                    }
                    else {
                        result = false;
                        logger.logError("Invalid expression matrix input line, " + lnum + ".");
                        break;
                    }
                }
                lnum++;
            }
            if(result) {
                if(lstbad.size() > 0) {
                    // save list of transcripts with no annotation
                    String badTrans = "";
                    for(String trans : lstbad)
                        badTrans += trans + "\n";
                    Utils.saveTextToFile(badTrans, project.data.getInputMatrixNATransFilepath());
                    
                    // log warning message
                    app.logWarning("Unable to find annotation entry for " + lstbad.size() + " transcript(s). processExpMatrixFile.");
                    int badcnt = lstbad.size();
                    int dspcnt = Math.min(25, badcnt);
                    String trans = "Expression matrix transcripts missing annotation: \n    ";
                    for(int i = 0; i < dspcnt; i++) {
                        trans += lstbad.get(i);
                        if(i < (dspcnt-2))
                            trans += ", ";
                    }
                    if(dspcnt < badcnt)
                        trans += "\nOnly showing the first " + dspcnt + " transcripts.";
                    app.logInfo("All transcripts missing annotations WILL BE IGNORED.");
                    app.logInfo(trans);
                }
            }
        }
        catch (Exception e) {
            result = false;
            logger.logError("Unable to copy expression matrix input file: " + e.getMessage());
        } finally {
           try {if(writerAll != null) writerAll.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }

    ////////////////////////
    // Internal Functions //
    ////////////////////////
    
    //
    // File Loading functions
    //

    // load application copy of the original expression matrix provided by the user
    // however transcripts with no annotations were removed from the application copy
    private void loadInputMatrixFile() {
        // load data if not previously loaded
        if(expInputMatrix == null) {
            int nsamples = params.getTotalSamplesCount();
            ArrayList<ExpMatrixCondition> conds = getExpressionMatrixConditions();
            ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
            Path filepath = Paths.get(project.data.getInputMatrixFilepath());
            if(Files.exists(filepath)) {
                try {
                    long tstart = System.nanoTime();
                    logger.logDebug("Reading expression matrix input data from " + filepath.toString() + ".");
                    List<String> lines = Files.readAllLines(filepath, StandardCharsets.UTF_8);
                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    logger.logDebug("Loaded expression matrix input data in " + duration + " ms");
                    
                    int lnum = 1;
                    boolean result = true;
                    String[] fields;
                    HashMap<String, Object> transcripts = new HashMap<>();
                    for(String line : lines) {
                        if(!line.trim().isEmpty() && !line.startsWith("#") && lnum > 1) {
                            fields = line.split("\t");
                            if(fields.length == (nsamples + 1)) {
                                double[] samples = new double[nsamples];
                                for(int i = 0; i < fields.length; i++)
                                    fields[i] = fields[i].trim();
                                for(int i = 0; i < nsamples; i++)
                                    samples[i] = Double.parseDouble(fields[i+1]);
                                if(!transcripts.containsKey(fields[0])) {
                                    String gene = project.data.getTransGene(fields[0]);
                                    String geneDesc = project.data.getGeneDescription(gene);
                                    data.add(new ExpMatrixArray(fields[0], "", fields[0], gene, geneDesc, "", "", "", "", false, samples));
                                    transcripts.put(fields[0], null);
                                }
                                else {
                                    logger.logError("Duplicate transcript found in expression matrix input data file, line " + lnum + ".");
                                    result = false;
                                    break;
                                }
                            }
                            else {
                                logger.logError("Invalid line found in expression matrix input data file, line " + lnum + ".");
                                result = false;
                                break;
                            }
                        }
                        lnum++;
                    }

                    if(result)
                        expInputMatrix = new ExpMatrixData(conds, data);
                }
                catch (Exception e) {
                    logger.logError("Unable to load expression matrix input data file: " + e.getMessage());
                }
            }
        }
    }
    
    // load application copy of the original expression matrix provided by the user
    private ExpMatrixData loadOriginalMatrixFile() {
        ExpMatrixData emd = null;
        
        // load data
        int nsamples = params.getTotalSamplesCount();
        ArrayList<ExpMatrixCondition> conds = getExpressionMatrixConditions();
        ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
        Path filepath = Paths.get(project.data.getOriginalMatrixFilepath());
        if(Files.exists(filepath)) {
            try {
                long tstart = System.nanoTime();
                logger.logDebug("Reading expression matrix input data from " + filepath.toString() + ".");
                List<String> lines = Files.readAllLines(filepath, StandardCharsets.UTF_8);
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logDebug("Loaded expression matrix input data in " + duration + " ms");

                int lnum = 1;
                boolean result = true;
                String[] fields;
                HashMap<String, Object> transcripts = new HashMap<>();
                for(String line : lines) {
                    if(lnum > 1 && !line.trim().isEmpty() && !line.startsWith("#")) {
                        fields = line.split("\t");
                        if(fields.length == (nsamples + 1)) {
                            double[] samples = new double[nsamples];
                            for(int i = 0; i < fields.length; i++)
                                fields[i] = fields[i].trim();
                            for(int i = 0; i < nsamples; i++)
                                samples[i] = Double.parseDouble(fields[i+1]);
                            if(!transcripts.containsKey(fields[0])) {
                                data.add(new ExpMatrixArray(fields[0], "", fields[0], "", "", "", "", "", "", false, samples));
                                transcripts.put(fields[0], null);
                            }
                            else {
                                logger.logError("Duplicate transcript found in expression matrix input data file, line " + lnum + ".");
                                result = false;
                                break;
                            }
                        }
                        else {
                            logger.logError("Invalid line found in expression matrix input data file, line " + lnum + ".");
                            result = false;
                            break;
                        }
                    }
                    lnum++;
                }

                if(result) {
                    FXCollections.sort(data);
                    emd = new ExpMatrixData(conds, data);
                }
            }
            catch (Exception e) {
                logger.logError("Unable to load expression matrix input data file: " + e.getMessage());
            }
        }
        return emd;
    }

    //
    // Data Classes
    //
    public static class ExpMatrixData {
        public ArrayList<ExpMatrixCondition> conditions;
        public ObservableList<ExpMatrixArray> data;
        public ExpMatrixData(ArrayList<ExpMatrixCondition> conditions, ObservableList<ExpMatrixArray> data) {
            this.conditions = conditions;
            this.data = data;
        }
    }
    public static class ExpMatrixCondition {
        public String name;
        public int nsamples;
        public String[] sampleNames;
        public ExpMatrixGroup group;
        public ExpMatrixCondition(ExpMatrixGroup group) {
            this.group = group;
            // temp for compatibility?
            this.name = group.name;
            this.nsamples = group.getTotalSamplesCount();
            this.sampleNames = new String[nsamples];
            int i = 0;
            for(ExpMatrixTime emt : group.lstTimes) {
                for(String sname : emt.lstSampleNames)
                    sampleNames[i++] = sname;
            }
        }
    }
    // Note: Currently this class is used for all 3 data types: transcripts, proteins and genes
    public static class ExpMatrixArray  implements Comparable<ExpMatrixArray> {
        public final SimpleStringProperty id;
        public final SimpleStringProperty name;
        public final SimpleStringProperty transcript;
        public final SimpleStringProperty gene;
        public final SimpleStringProperty geneDesc;
        public final SimpleStringProperty category;
        public final SimpleStringProperty attributes;
        public final SimpleStringProperty chromo;
        public final SimpleStringProperty strand;
        public final SimpleStringProperty coding;
        public SimpleIntegerProperty length;
        public SimpleIntegerProperty isoforms;
        //public SimpleIntegerProperty proteins;

        public final SimpleDoubleProperty[] samples;
        public double[] daSamples;
 
        public ExpMatrixArray(String id, String name, String transcript, String gene, String geneDesc, String category, String attributes, String chromo, String strand, boolean coding, double[] samples) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.transcript = new SimpleStringProperty(transcript);
            this.gene = new SimpleStringProperty(gene);
            this.geneDesc = new SimpleStringProperty(geneDesc);
            this.category = new SimpleStringProperty(category);
            this.attributes = new SimpleStringProperty(attributes);
            this.chromo = new SimpleStringProperty(chromo);
            this.strand = new SimpleStringProperty(strand);
            this.coding = new SimpleStringProperty(coding? "YES" : "NO");
            this.length = new SimpleIntegerProperty(0);
            this.isoforms = new SimpleIntegerProperty(0);
            //this.proteins = new SimpleIntegerProperty(0);

            daSamples = samples;
            int cnt = samples.length;
            this.samples = new SimpleDoubleProperty[cnt];
            for(int i = 0; i < cnt; i++)
                this.samples[i] = new SimpleDoubleProperty(samples[i]);
        }
        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getTranscript() { return transcript.get(); }
        public String getGene() { return gene.get(); }
        public String getGeneDesc() { return geneDesc.get(); }
        public String getCategory() { return category.get(); }
        public String getAttributes() { return attributes.get(); }
        public String getChromo() { return chromo.get(); }
        public String getStrand() { return strand.get(); }
        public String getCoding() { return coding.get(); }
        public Integer getLength() { return length.get(); }
        public Integer getIsoforms() { return isoforms.get(); }
        //public Integer getProteins() { return proteins.get(); }
        @Override
        public int compareTo(ExpMatrixArray td) {
            return (id.get().compareTo(td.id.get()));
        }
    }
    public static class ExpMatrixParams {
        DlgInputData.Params params;
        DataApp.ExperimentType experimentType;
        ArrayList<ExpMatrixGroup> lstGroups = new ArrayList<>();
        String srcEMFilepath, srcEDFilepath;

        DataApp.ExperimentType dfltExperimentType = DataApp.ExperimentType.Two_Group_Comparison;
        public ExpMatrixParams(HashMap<String, String> hmParams) {
            params = new DlgInputData.Params(hmParams);
            experimentType = params.experimentType;
            lstGroups = params.lstGroups;
            srcEMFilepath = params.emFilepath;
            srcEDFilepath = params.edFilepath;
        }
        public String getExperimentTypeName() {
            return DlgInputData.Params.getExperimentTypeName(experimentType);
        }
        public String[] getExpTypeGroupNames() {
            switch(params.experimentType) {
                case Two_Group_Comparison:
                    return getGroupNames();
                case Time_Course_Multiple:
                case Time_Course_Single:
                    return getGroupsTimeNames();
            }
            return null;
        }
        // get names for all unique time points in ascending order
        public String[] getTimePointNames() {
            String[] names = new String[getTimePoints()];
            HashMap<String, Object> hmTimes = new HashMap<>();
            ArrayList<Integer> lstTimes = new ArrayList();
            for(ExpMatrixGroup emg : lstGroups) {
                for(ExpMatrixTime emt : emg.lstTimes)
                    hmTimes.put(emt.name, null);
            }
            for(String time : hmTimes.keySet())
                lstTimes.add(Integer.parseInt(time));
            Collections.sort(lstTimes);
            int idx = 0;
            for(int t : lstTimes)
                names[idx++] = "" + t;
            return names; 
        }
        // get names for all maSigPro results
        public String[] getResultNames() {
            String[] names = new String[lstGroups.size()];
            HashMap<String, Object> hmTimes = new HashMap<>();
            ArrayList<Integer> lstTimes = new ArrayList();
            int idx = 0;
            String control = lstGroups.get(0).name;
            names[idx++] = control;
            for(int i = 1; idx < lstGroups.size(); i++)
                names[idx++] = lstGroups.get(i).name + "vs" + control;
            return names; 
        }
        // total number is the number of unique time points
        public int getTimePoints() {
            HashMap<String, Object> hmTimes = new HashMap<>();
            for(ExpMatrixGroup emg : lstGroups) {
                for(ExpMatrixTime emt : emg.lstTimes)
                    hmTimes.put(emt.name, null);
            }
            return hmTimes.size(); 
        }
        public int getGroupsCount() { return lstGroups.size(); }
        public String[] getGroupNames() { 
            String[] names = null;
            int i = 0;
            if(!lstGroups.isEmpty()) {
                names = new String[lstGroups.size()];
                for(DlgInputData.Params.ExpMatrixGroup gt : params.lstGroups)
                    names[i++] = gt.name;
            }
            return names;
        }
        public String getGroupName(int grpnum) {
            String name = "";
            if(grpnum >= 0 && grpnum < lstGroups.size())
                name = lstGroups.get(grpnum).name;
            return name; 
        }
        public String[] getGroupsTimeNames() {
            int idx = 0;
            int cnt = 0;
            for(ExpMatrixGroup emg : lstGroups)
                cnt += emg.lstTimes.size();
            String[] names = new String[cnt];
            for(ExpMatrixGroup emg : lstGroups) {
                for(ExpMatrixTime emt : emg.lstTimes)
                    names[idx++] = emg.name + " T" + emt.name;
            }
            return names; 
        }
        public String[] getGroupTimeNames(int grpnum) {
            int idx = 0;
            String[] names = null;
            if(grpnum >= 0 && grpnum < lstGroups.size()) {
                ExpMatrixGroup emg = lstGroups.get(grpnum);
                names = new String[emg.lstTimes.size()];
                for(ExpMatrixTime emt : emg.lstTimes)
                    names[idx++] = emg.name + " T" + emt.name;
            }
            return names; 
        }
        public int getExpMatrixGroupCount(int grpnum) { 
            int cnt = 0;
            if(grpnum >= 0 && grpnum < lstGroups.size())
                cnt = lstGroups.get(grpnum).lstTimes.size();
            return cnt; 
        }
        public int getGroupTotalSamplesCount(int grpnum) {
            int cnt = 0;
            if(grpnum >= 0 && grpnum < lstGroups.size()) {
                ExpMatrixGroup ct = lstGroups.get(grpnum);
                cnt = ct.getTotalSamplesCount();
            }
            return cnt; 
        }
        public int getGroupExpMatrixTimeCount(int grpnum, int tnum) {
            int cnt = 0;
            if(grpnum >= 0 && grpnum < lstGroups.size()) {
                ExpMatrixGroup ct = lstGroups.get(grpnum);
                if(tnum >= 0 && tnum < ct.lstTimes.size())
                    cnt += ct.lstTimes.get(tnum).lstSampleNames.size();
            }
            return cnt; 
        }
        public int getTotalTimes() { 
            HashMap<String, Object> hmTimes = new HashMap<>();
            for(ExpMatrixGroup emg : lstGroups) {
                for(ExpMatrixTime emt : emg.lstTimes)
                    hmTimes.put(emt.name, null);
            }
            return hmTimes.size();
        }
        public int getTotalSamplesCount() {
            int cnt = 0;
            for(int cnum = 0; cnum < lstGroups.size(); cnum++)
                cnt += getGroupTotalSamplesCount(cnum);
            return cnt; 
        }
        public String getGroupSampleName(int grpnum, int snum) {
            String name = "";
            if(grpnum >= 0 && grpnum < lstGroups.size()) {
                ExpMatrixGroup emg = lstGroups.get(grpnum);
                for(ExpMatrixTime emt : emg.lstTimes) {
                    if(snum >= 0 && snum < emt.lstSampleNames.size()) {
                        name = emt.lstSampleNames.get(snum);
                        break;
                    }
                    else
                        snum -= emt.lstSampleNames.size();
                }
            }
            return name;
        }
        public String getGroupSampleTime(int grpnum, int snum) {
            String name = "";
            if(grpnum >= 0 && grpnum < lstGroups.size()) {
                ExpMatrixGroup emg = lstGroups.get(grpnum);
                for(ExpMatrixTime emt : emg.lstTimes) {
                    if(snum >= 0 && snum < emt.lstSampleNames.size()) {
                        name = emt.name;
                        break;
                    }
                    else
                        snum -= emt.lstSampleNames.size();
                }
            }
            return name;
        }
        // get all the sample names in sequential order based on group, time
        public ArrayList<String> getSampleNames() {
            ArrayList<String> lstNames = new ArrayList<>();
            for(ExpMatrixGroup gt : lstGroups) {
                for(ExpMatrixTime ts : gt.lstTimes) {
                    for(String name : ts.lstSampleNames)
                        lstNames.add(name);
                }
            }
            return lstNames;
        }
        // We are using the sample names from the design data
        // should match the order in which our working matrix is saved
        public String getHeader() {
            String header = "";
            for(ExpMatrixGroup gt : lstGroups) {
                for(ExpMatrixTime ts : gt.lstTimes) {
                    for(String name : ts.lstSampleNames)
                        header += (header.isEmpty()? "" : "\t") + name;
                }
            }
            return header;
        }
        public String getFactors() {
            String factors = "";
            int idx = 1;
            for(ExpMatrixGroup gt : params.lstGroups) {
                for(ExpMatrixTime ts : gt.lstTimes) {
                    // all samples in a given group-time are assigned to the same number (idx)
                    for(String name : ts.lstSampleNames)
                        factors += name + "\t" + idx + "\n";
                    idx++;
                }
            }
            return factors;
        }
        public String getTimeFactors() {
            String factors = "";
            int grpnum = 1;
            int repnum = 1;
            int grpcnt = params.lstGroups.size();
            for(ExpMatrixGroup emg : params.lstGroups) {
                for(ExpMatrixTime emt : emg.lstTimes) {
                    String selection = "";
                    for(int i = 1; i <= grpcnt; i++)
                        selection += "\t" + (grpnum == i? "1" : "0");
                    for(String sample : emt.lstSampleNames)
                        factors += sample + "\t" + emt.name + "\t" + repnum + selection + "\n";
                    repnum++;
                }
                grpnum++;
            }
            return factors;
        }
    }
}
