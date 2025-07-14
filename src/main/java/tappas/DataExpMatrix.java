/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import tappas.DataApp.DataType;
import tappas.DataInputMatrix.ExpMatrixArray;
import tappas.DataInputMatrix.ExpMatrixCondition;
import tappas.DataInputMatrix.ExpMatrixData;
import tappas.DataInputMatrix.ExpMatrixParams;

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
public class DataExpMatrix extends AppObject {
    static public int DataTypeLen = DataType.values().length;

    // the input matrix data is the matrix provided by the user for this project
    ExpMatrixData inpMatrixData;
    // expression matrix is the filtered and normalized input matrix
    ExpMatrixData expMatrixData;
    // matrix data array will contain the 3 levels of expression data after the transcripts are filtered
    // the isoforms are read from the users file and genes and proteins are calculated from that
    ExpMatrixData[] matrixData = new ExpMatrixData[DataTypeLen];
    
    public DataExpMatrix(Project project) {
        super(project, null);
    }
    // initialization function
    public void initialize() {
        clearData();
    }
    public void clearData() {
        inpMatrixData = null;
        for(int i = 0; i < matrixData.length; i++)
            matrixData[i] = null;
        expMatrixData = null;
    }
    
    public boolean genResultMatrixFile(HashMap<String, Object> hmFilterTrans) {
        boolean result = false;
        
        // load input matrix data and filter using hmFilterTrans to write to result matrix
        ExpMatrixData emd = getExpressionData(true, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String filepath = project.data.getResultMatrixFilepath();
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                ExpMatrixParams params = project.data.getInputMatrixParams();
                String[] groups = params.getGroupNames();
                for(String group : groups) {
                    XYChart.Series series = new XYChart.Series();
                    series.setName(group);
                }
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
                logger.logError("genResultMatrixFile Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    public HashMap<String, HashMap<String, Object>> getInputMatrixFilterGeneTrans() {
        HashMap<String, HashMap<String, Object>> hmGT = new HashMap<>();
        ExpMatrixData emd = getExpressionData(true, DataType.TRANS, null);
        if(emd != null) {
            String trans, gene;
            for(ExpMatrixArray ema : emd.data) {
                trans = ema.getTranscript();
                gene = project.data.getTransGene(trans);
                if(hmGT.containsKey(gene))
                    hmGT.get(gene).put(trans, null);
                else {
                    HashMap<String, Object> hm = new HashMap<>();
                    hm.put(trans, null);
                    hmGT.put(gene, hm);
                }
            }
        }
        return hmGT;
    }
    public HashMap<String, Object> getInputMatrixFilterTrans() {
        HashMap<String, Object> hmTrans = new HashMap<>();
        ExpMatrixData emd = getExpressionData(true, DataType.TRANS, null);
        if(emd != null) {
            for(ExpMatrixArray ema : emd.data)
                hmTrans.put(ema.getTranscript(), null);
        }
        return hmTrans;
    }
    public HashMap<String, double[]> getInputMatrixMeanExpressionLevelsHM(DataType type, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[]> hmResults = new HashMap<>();
        
        // get expression data and setup sample counts for each condition
        ExpMatrixData emd = getExpressionData(true, type, hmFilterTrans);
        ObservableList<ExpMatrixArray> data = emd.data;
        int[] scnt = new int[emd.conditions.size()];
        int conditions = 0;
        for(ExpMatrixCondition emc : emd.conditions)
            scnt[conditions++] = emc.nsamples;

        // process matrix data
        for(ExpMatrixArray ema : data) {
            int offset = 0;
            double[] values = new double[conditions+1];
            for(int c = 0; c < conditions; c++) {
                double val = 0;
                int nsamples = scnt[c];
                for(int idx = offset; idx < (offset+scnt[c]); idx++)
                    val += ema.daSamples[idx];
                values[c] = val/nsamples;
                hmResults.put(ema.getId(), values);
                offset += scnt[c];
            }
            double val = 0;
            for(int c = 0; c < conditions; c++)
                val += values[c];
            values[conditions] = val/(conditions+1);
        }
        return hmResults;
    }
    
    // get isoset gene trans hashmap
    public HashMap<String, HashMap<String, Object>> getIsoSetGeneTrans(String isoSet) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmExp = getExpressed(null);
        switch (isoSet) {
            case "ALL":
                for(String gene : hmExp.keySet())
                    hmGeneTrans.put(gene, hmExp.get(gene));
                break;
            case "ALL_MISO":
                for(String gene : hmExp.keySet()) {
                    if(hmExp.get(gene).size() > 1)
                        hmGeneTrans.put(gene, hmExp.get(gene));
                }   break;
        }
        return hmGeneTrans;
    }    
    // get isoset gene hashmap
    public HashMap<String, Object> getIsoSetGenes(String isoSet) {
        HashMap<String, Object> hmGenes = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmExp = getExpressed(null);
        switch (isoSet) {
            case "ALL":
                for(String gene : hmExp.keySet())
                    hmGenes.put(gene, null);
                break;
            case "ALL_MISO":
                for(String gene : hmExp.keySet()) {
                    if(hmExp.get(gene).size() > 1)
                        hmGenes.put(gene, null);
                }   break;
        }
        return hmGenes;
    }    
    // get isoset trans hashmap
    public HashMap<String, Object> getIsoSetTrans(String isoSet) {
        HashMap<String, Object> hmTrans = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmExp = getExpressed(null);
        switch (isoSet) {
            case "ALL":
                for(String gene : hmExp.keySet()) {
                    for(String trans : hmExp.get(gene).keySet())
                        hmTrans.put(trans, null);
                }   break;
            case "ALL_MISO":
                for(String gene : hmExp.keySet()) {
                    if(hmExp.get(gene).size() > 1) {
                        for(String trans : hmExp.get(gene).keySet())
                            hmTrans.put(trans, null);
                    }
                }   break;
        }
        return hmTrans;
    }    
    // the array returned is always in the order of -1,0,...,n - from min to max
    // min is always set to -1 and max <= 9 depending on exp level range
    // returns null if no data or error
    public ExpressionLevelsDistribution getMeanLog10ExpressionLevelsDistribution(DataType type, HashMap<String, Object> hmFilterTrans) {
        ExpressionLevelsDistribution expLevelsDist = null;
        double[][] expLevels = getMeanExpressionLevels(type, hmFilterTrans);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int logLevel;
        double val;
        for(double[] conditionExpLevels : expLevels) {
            for(int i = 0; i < conditionExpLevels.length; i++) {
                val = conditionExpLevels[i];
                if(val < 0.1)
                    val = 0.1;
                conditionExpLevels[i] = Math.log10(val);
                logLevel = (int)conditionExpLevels[i];
                if(logLevel < min)
                    min = logLevel;
                if(logLevel > max)
                    max = logLevel;
            }
        }
        if(min != Integer.MAX_VALUE && max != Integer.MIN_VALUE) {
            if(min > -1)
                min = -1;
            if(min < -1) {
                logger.logDebug("Found expression level mean value to be less than -1, " + min);
                min = -1;
            }
            if(max > 9) {
                logger.logDebug("Found expression level mean value to be grater than 9, " + max);
                max = 9;
            }
            expLevelsDist = new ExpressionLevelsDistribution(expLevels.length, min, max);
            int condition = 0;
            for(double[] conditionExpLevels : expLevels) {
                for(int i = 0; i < conditionExpLevels.length; i++) {
                    logLevel = (int)conditionExpLevels[i];
                    // note that this relies on min always starting at -1
                    expLevelsDist.conditionLogLevels[condition][logLevel+1]++;
                }
                condition++;
            }
        }
        return expLevelsDist;
    }
    public double[][] getMeanExpressionLevels(DataType type, HashMap<String, Object> hmFilterTrans) {
        // get expression data and setup sample counts for each condition
        ExpMatrixData emd = getExpressionData(false, type, hmFilterTrans);
        ObservableList<ExpMatrixArray> data = emd.data;
        int[] scnt = project.data.getConditionsSamples();
        int conditions = scnt.length;
        double[][] values = new double[conditions][];
        int rows = data.size();
        for(int i = 0; i < conditions; i++)
            values[i] = new double[Math.max(rows, 1)];
        
        // process matrix data
        int ridx = 0;
        for(ExpMatrixArray ema : data) {
            int offset = 0;
            for(int c = 0; c < conditions; c++) {
                double val = 0;
                int nsamples = scnt[c];
                for(int idx = offset; idx < (offset+scnt[c]); idx++)
                    val += ema.daSamples[idx];
                values[c][ridx] = val/nsamples;
                offset += scnt[c];
            }
            ridx++;
        }
        return values;
    }
    public HashMap<String, double[]> getMeanExpressionLevelsHM(DataType type, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[]> hmResults = new HashMap<>();
        
        // get expression data and setup sample counts for each condition
        ExpMatrixData emd = getExpressionData(false, type, hmFilterTrans);
        ObservableList<ExpMatrixArray> data = emd.data;
        int[] scnt = project.data.getConditionsSamples();
        int conditions = scnt.length;

        // process matrix data
        for(ExpMatrixArray ema : data) {
            int offset = 0;
            double[] values = new double[conditions+1];
            for(int c = 0; c < conditions; c++) {
                double val = 0;
                int nsamples = scnt[c];
                for(int idx = offset; idx < (offset+scnt[c]); idx++)
                    val += ema.daSamples[idx];
                values[c] = val/nsamples;
                hmResults.put(ema.getId(), values);
                offset += scnt[c];
            }
            double val = 0;
            for(int c = 0; c < conditions; c++)
                val += values[c];
            values[conditions] = val/(conditions+1);    // what is this!!! divided by 3?
        }
        return hmResults;
    }
    
    public HashMap<String, double[][]> getMeanExpressionLevelsSD_HM(DataType type, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[][]> hmResults = new HashMap<>();
        
        // get expression data and setup sample counts for each condition
        ExpMatrixData emd = getExpressionData(false, type, hmFilterTrans);
        ObservableList<ExpMatrixArray> data = emd.data;
        int[] scnt = project.data.getConditionsSamples();
        int conditions = scnt.length;

        // process matrix data
        for(ExpMatrixArray ema : data) {
            int offset = 0;
            double[][] values = new double[2][conditions+1]; //1=values 2=SD
            for(int c = 0; c < conditions; c++) {
                double val = 0;
                double sd = 0;
                int nsamples = scnt[c];
                for(int idx = offset; idx < (offset+nsamples); idx++)
                    val += ema.daSamples[idx];
                values[0][c] = val/nsamples;
                for(int idx = offset; idx < (offset+nsamples); idx++){
                    sd += Math.pow(ema.daSamples[idx]-values[0][c], 2);
                }
                sd = Math.sqrt(sd/(nsamples-1))/Math.sqrt(nsamples);
                // SD = sigma/sqrt(N) -> sigma = sqrt(sum(X-X\)²/N-1) -> N-1 = población muestral
                values[1][c] = sd;
                hmResults.put(ema.getId(), values);
                offset += nsamples;
            }
            double val = 0;
            for(int c = 0; c < conditions; c++)
                val += values[0][c];
            values[0][conditions] = val/(conditions+1);    // what is this!!!
        }
        return hmResults;
    }
    // get expression matrix data for given expression data type
    // type - expression data type
    // categories - Hashmap of all transcript categories to be included
    //              These categories are based on the kind of transcripts alignment/match
    //              Each transcript contains a single category in the annotation file
    // return: expression matrix data, null if none available or error
    public ExpMatrixData getExpressionData(boolean useInputMatrix, DataType type, HashMap<String, Object> hmFilterTrans) {
        return getExpressionDataExt(useInputMatrix, type, false, hmFilterTrans);
    }
    private ExpMatrixData getExpressionDataExt(boolean useInputMatrix, DataType type, boolean geneProteins, HashMap<String, Object> hmFilterTrans) {
        ExpMatrixData expData = null;
        ExpMatrixData srcData;
        
        // load data if not previously loaded
        if(useInputMatrix) {
            if(inpMatrixData == null)
                inpMatrixData = loadExpressionDataFile(useInputMatrix);
            srcData = inpMatrixData;
        }
        else {
            if(expMatrixData == null)
                expMatrixData = loadExpressionDataFile(useInputMatrix);
            srcData = expMatrixData;
        }
        if(srcData != null) {
            ExpMatrixParams params = project.data.getInputMatrixParams();
            int nsamples = params.getTotalSamplesCount();
            ArrayList<ExpMatrixCondition> conds = project.data.dataInputMatrix.getExpressionMatrixConditions();
            int noncoding = 0;
            String gene, protein, cat;
            HashMap<String, double[]> hmGenes = new HashMap<>();
            HashMap<String, double[]> hmProteins = new HashMap<>();
            ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
            if(hmFilterTrans == null)
                hmFilterTrans = new HashMap<>();
            boolean filter = !hmFilterTrans.isEmpty();
            for(ExpMatrixArray ema : srcData.data) {
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
                        else {
                            ExpMatrixArray expema = new ExpMatrixArray(trans, "", trans, "", "", "", "", "", "", false, samples);
                            expema.length = new SimpleIntegerProperty(0);
                            data.add(expema);
                            //Tappas.msgWarn("Unable to retrieve transcript data for " + trans);
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
                        else
                            noncoding += 1;
                    }
                }
            }
            if(type == DataType.GENE) {
                String chromo, strand;
                Boolean coding;
                int isoforms;
                //number of proteins per gene
                //int proteins;
                for(String expgene : hmGenes.keySet()) {
                    chromo = project.data.getGeneChromo(expgene);
                    strand = project.data.getGeneStrand(expgene);
                    isoforms = project.data.getGeneTransCount(expgene);
                    //proteins = project.data.getGeneProteinCount(expgene);
                    coding = project.data.isGeneCoding(expgene);
                    ExpMatrixArray ema = new ExpMatrixArray(expgene, project.data.getGeneDescription(expgene), "", expgene, "", "", "", chromo, strand, coding, hmGenes.get(expgene));
                    ema.isoforms = new SimpleIntegerProperty(isoforms);
                    //ema.proteins = new SimpleIntegerProperty(proteins);
                    data.add(ema);
                }
            }
            else if(type == DataType.PROTEIN) {
                int length;
                for(String expprot : hmProteins.keySet()) {
                    String expgene, expprotein;
                    ExpMatrixArray ema;
                    if(geneProteins) {
                        String[] fields = expprot.split("\t");
                        expgene = fields[0];
                        expprotein = fields[1];
                        ema = new ExpMatrixArray(expgene + "_" + expprotein, project.data.getProteinDescription(expprotein), "", expgene, project.data.getGeneDescription(expgene), "", "", "", "", true, hmProteins.get(expprot));
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
                        ema = new ExpMatrixArray(expprot, project.data.getProteinDescription(expprot), exptrans, expgenes, expgenesdesc, "", "", "", "", true, hmProteins.get(expprot));
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
    public HashMap<String, Boolean> getExpressedIsoformsCoding(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Boolean> hm = new HashMap<>();
        HashMap<String, Boolean> hmAnnot = project.data.getTransCoding(null);
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            int errcnt = 0;
            String trans;
            for(ExpMatrixArray ema : emd.data) {
                trans = ema.getTranscript();
                if(hmAnnot.containsKey(trans))
                    hm.put(trans, hmAnnot.get(trans));
                else
                    errcnt++;
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find annotation data for " + errcnt + " transcript(s).");
        }
        return hm;
    }
    // contains all the genes in the distribution matrix - no filtering for low values, etc.
    public HashMap<String, Object> getExpressedGeneTrans(String gene, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Object> hmTrans = new HashMap<>();
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String transgene, trans;
            int errcnt = 0;
            for(ExpMatrixArray ema : emd.data) {
                trans = ema.getTranscript();
                transgene = project.data.getTransGene(trans);
                if(transgene.isEmpty())
                    errcnt++;
                else if(transgene.equals(gene))
                    hmTrans.put(trans, null);
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find gene for " + errcnt + " expressed transcript(s).");
        }
        return hmTrans;
    }
    public HashMap<String, Object> getExpressedGeneProteins(String gene, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Object> hmProts = new HashMap<>();
        ExpMatrixData emd = getExpressionData(false, DataType.PROTEIN, hmFilterTrans);
        if(emd != null) {
            String transgene, mtrans;
            int errcnt = 0;
            for(ExpMatrixArray ema : emd.data) {
                mtrans = ema.getTranscript();
                String fields[] = mtrans.trim().split(",");
                for(String trans : fields) {
                    transgene = project.data.getTransGene(trans);
                    if(transgene.isEmpty())
                        errcnt++;
                    else if(transgene.equals(gene))
                        hmProts.put(ema.id.get(), null);
                }
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find gene for " + errcnt + " expressed transcript(s).");
        }
        return hmProts;
    }
    public HashMap<String, HashMap<String, Object>> getExpressed(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
        HashMap<String, Object> hmTrans;

        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String transgene, trans;
            int errcnt = 0;
            for(ExpMatrixArray ema : emd.data) {
                trans = ema.getTranscript();
                transgene = project.data.getTransGene(trans);
                if(transgene.isEmpty())
                    errcnt++;
                else {
                    if(!hmGeneTrans.containsKey(transgene)) {
                        hmTrans = new HashMap<>();
                        hmTrans.put(trans, null);
                        hmGeneTrans.put(transgene, hmTrans);

                    }
                    else {
                        hmTrans = hmGeneTrans.get(transgene);
                        hmTrans.put(trans, null);
                    }
                }
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find gene for " + errcnt + " expressed transcript(s).");
        }
        return hmGeneTrans;
    }
    public HashMap<String, Object> getExpressedTrans(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Object> hmTrans = new HashMap<>();
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            if(hmFilterTrans == null)
                hmFilterTrans = new HashMap<>();
            boolean filter = !hmFilterTrans.isEmpty();
            for(ExpMatrixArray ema : emd.data) {
                String trans = ema.getTranscript();
                if(!filter || hmFilterTrans.containsKey(trans))
                    hmTrans.put(trans, null);
            }
        }
        return hmTrans;
    }
    public HashMap<String, Integer> getExpressedGeneIsoformsCount(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Integer> hmGeneIsoCnt = new HashMap<>();
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String gene;
            int errcnt = 0;
            for(ExpMatrixArray ema : emd.data) {
                gene = project.data.getTransGene(ema.getTranscript());
                if(!gene.isEmpty()) {
                    if(!hmGeneIsoCnt.containsKey(gene))
                        hmGeneIsoCnt.put(gene, 1);
                    else
                        hmGeneIsoCnt.put(gene, hmGeneIsoCnt.get(gene) + 1);
                }
                else
                    errcnt++;
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find gene for " + errcnt + " expressed transcript(s).");
        }
        return hmGeneIsoCnt;
    }
    public HashMap<String, Integer> getExpressedGeneAssignedProteinsCount(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = getExpressed(hmFilterTrans);
        HashMap<String, Integer> hmGeneProtCnts = project.data.getAnnotatedGeneAssignedProteinsCount(hmGeneTrans);
        return hmGeneProtCnts;
    }
    public int getExpressedAssignedProteinsCount(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = getExpressed(hmFilterTrans);
        int cnt = project.data.getAnnotatedAssignedProteinsCount(hmGeneTrans);
        return cnt;
    }
    public HashMap<String, HashMap<String, Integer>> getExpressedGeneIsoformsLength(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Integer>> hmGI = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> hmAGI = project.data.getGeneTransLength(null);
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String gene, trans;
            int errcnt = 0;
            HashMap<String, Integer> hm;
            for(ExpMatrixArray ema : emd.data) {
                trans = ema.getTranscript();
                gene = project.data.getTransGene(trans);
                if(!gene.isEmpty()) {
                    if(!hmGI.containsKey(gene))
                        hmGI.put(gene, new HashMap<>());
                    if(hmAGI.containsKey(gene)) {
                        hm = hmAGI.get(gene);
                        if(hm.containsKey(trans))
                            hmGI.get(gene).put(trans, hm.get(trans));
                        else
                            hmGI.get(gene).put(trans, 0);
                    }
                    else {
                        errcnt++;
                        hmGI.get(gene).put(trans, 0);
                    }
                }
                else
                    errcnt++;
            }
            if(errcnt > 0)
                logger.logInfo("Unable to find gene for " + errcnt + " expressed transcript(s).");
        }
        
        return hmGI;
    }
    // pass bins?
    public DataProject.DistributionData getExpressedTransDistribution(HashMap<String, Object> hmFilterTrans) {
        DataProject.DistributionData dd = null;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        ExpMatrixData emd = getExpressionData(false, DataType.TRANS, hmFilterTrans);
        if(emd != null) {
            String gene;
            int gcnt = 0;
            int tcnt = 0;
            ArrayList<String> lst = new ArrayList<>();
            HashMap<String, Integer> hm = new HashMap<>();
            for(ExpMatrixArray ema : emd.data) {
                gene = project.data.getTransGene(ema.getTranscript());
                if(!gene.isEmpty()) {
                    if(!hm.containsKey(gene)) {
                        hm.put(gene, 1);
                        gcnt++;
                    }
                    else
                        hm.put(gene, hm.get(gene) + 1);
                    if(hm.get(gene) > 25)
                        logger.logDebug("Gene with > 25 transcripts: " + gene);
                    tcnt++;
                }
                else
                    lst.add(ema.getTranscript());
            }
            int min = 1000000;
            int max = 0;
            for(Integer cnt : hm.values()) {
                if(cnt > max)
                    max = cnt;
                if(cnt < min)
                    min = cnt;
            }
            if(min == 1000000)
                min = max;
            logger.logDebug("Max isoforms per gene: " + max + " (25+), geneCnt: " + gcnt + ", transCnt: " + tcnt);
            if(max > 25)
                max = 25;
            int[] dist = new int[max];
            for(int i = 0; i < max; i++)
                dist[i] = 0;
            for(Integer cnt : hm.values()) {
                if(cnt <= 25)
                    dist[cnt - 1]++;
                else
                    dist[25 - 1]++;
            }
            for(int i = 0; i < max; i++)
                series.getData().add(new XYChart.Data<>((i < 24)? ("" + (i+1)) : ((i+1) + "+"), dist[i]));
            double mean = (double)tcnt / (double)gcnt;
            mean = Double.parseDouble(String.format("%.2f", ((double)Math.round(mean*100)/100.0)));
            dd = new DataProject.DistributionData(series, gcnt, tcnt, min, max, mean);
            if(lst.size() > 0) {
                logger.logWarning("Unable to find gene for " + lst.size() + " transcripts.");
                int cnt = lst.size();
                int dspcnt = Math.min(25, cnt);
                String trans = "Transcripts missing gene assignment in annotation file: \n    ";
                for(int i = 0; i < dspcnt; i++) {
                    trans += lst.get(i);
                    if(i < (dspcnt-2))
                        trans += ", ";
                }
                if(dspcnt < cnt)
                    trans += "\nOnly showing the first " + dspcnt + " transcripts.";
                logger.logInfo(trans);
            }
        }
        return dd;
    }
    public DataProject.DistributionData getExpressedProtDistribution(HashMap<String, Object> hmFilterTrans) {
        DataProject.DistributionData dd = null;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        HashMap<String, Integer> hmGeneProtCnts = getExpressedGeneAssignedProteinsCount(hmFilterTrans);
        if(hmGeneProtCnts.size() > 0) {
            int gcnt = hmGeneProtCnts.size();
            int tcnt = 0;
            HashMap<String, Integer> hm = new HashMap<>();
            for(Integer cnt : hmGeneProtCnts.values()) {
                tcnt += cnt;
            }
            int min = 1000000;
            int max = 0;
            for(Integer cnt : hmGeneProtCnts.values()) {
                if(cnt > max)
                    max = cnt;
                if(cnt < min)
                    min = cnt;
            }
            if(min == 1000000)
                min = max;
            logger.logDebug("Max proteins per gene: " + max + ", geneCnt: " + gcnt + ", protCnt: " + tcnt);
            int[] dist = new int[max+1];
            for(int i = 0; i <= max; i++)
                dist[i] = 0;
            for(Integer cnt : hmGeneProtCnts.values())
                dist[cnt]++;
            for(int i = 0; i <= max; i++)
                series.getData().add(new XYChart.Data<>("" + (i), dist[i]));
            double mean = (double)tcnt / (double)gcnt;
            mean = Double.parseDouble(String.format("%.2f", ((double)Math.round(mean*100)/100.0)));
            dd = new DataProject.DistributionData(series, gcnt, tcnt, min, max, mean);
        }
        return dd;
    }


    //
    // File Generation Functions
    //
    
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
                writer.write("\tReplicates\n");
                ExpMatrixParams params = project.data.getInputMatrixParams();
                writer.write(params.getFactors());
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logInfo("Generated expression matrix factors file in " + duration + " ms");
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
        if(!result)
            Utils.removeFile(Paths.get(filepath));
        
        return result;
    }
    // generate expression data file for DE Analysis, etc.
    public boolean genExpressionFile(DataType type, boolean geneProteins, HashMap<String, Object> hmFilterTrans, String filepath) {
        boolean result = false;
        ExpMatrixData emd = getExpressionDataExt(false, type, geneProteins, hmFilterTrans);
        if(emd != null) {
            String filename = DataProject.TRANS_MATRIX;
            switch(type) {
                case PROTEIN:
                    filename = DataProject.PROT_MATRIX;
                    break;
                case GENE:
                    filename = DataProject.GENE_MATRIX;
                    break;
            }
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                ExpMatrixParams params = project.data.getInputMatrixParams();
                writer.write(params.getHeader() + "\n");
                
                String line;
                double[] expsamples;
                int nsamples = params.getTotalSamplesCount();
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
                logger.logError("genExpressionFile Code exception: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }

    ////////////////////////
    // Internal Functions //
    ////////////////////////
    
    //
    // File Loading functions
    //

    // loads the filtered, normalized transcript expression data - only call if data not already loaded
    // this data will be used to generate protein and gene expression data
    private ExpMatrixData loadExpressionDataFile(boolean useInputMatrix) {
        ExpMatrixData emd = null;
        ExpMatrixParams params = project.data.getInputMatrixParams();
        int nsamples = params.getTotalSamplesCount();
        ArrayList<ExpMatrixCondition> conds = project.data.dataInputMatrix.getExpressionMatrixConditions();
        ObservableList<ExpMatrixArray> data = FXCollections.observableArrayList();
        Path filepath = useInputMatrix? Paths.get(project.data.getInputNormMatrixFilepath()) : Paths.get(project.data.getResultMatrixFilepath());
        if(Files.exists(filepath)) {
            try {
                long tstart = System.nanoTime();
                logger.logDebug("Reading expression matrix data from " + filepath.toString() + ".");
                List<String> lines = Files.readAllLines(filepath, StandardCharsets.UTF_8);
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logDebug("Loaded expression matrix data in " + duration + " ms");

                int lnum = 1;
                boolean result = true;
                String[] fields;
                HashMap<String, Object> transcripts = new HashMap<>();
                for(String line : lines) {
                    if(!line.trim().isEmpty() && !line.startsWith("#") && lnum > 1) {
                        fields = line.split("\t");
                        if(fields.length == (nsamples + 1)) {
                            double[] samples = new double[nsamples];
                            // R is sticking spaces between the tabs
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
                                logger.logError("Duplicate transcript found in expression matrix file, line " + lnum + ".");
                                result = false;
                                break;
                            }
                        }
                        else {
                            logger.logError("Invalid line, " + lnum + ", found in expression matrix file.");
                            result = false;
                            break;
                        }
                    }
                    lnum += 1;
                }

                if(result)
                    emd = new ExpMatrixData(conds, data);
            }
            catch (Exception e) {
                logger.logError("Unable to load expression matrix file data: " + e.getMessage());
            }
        }
        return emd;
    }
    
    //
    // Data Classes
    //

    public class ExpressionLevelsDistribution {
        int conditions;                 // number of conditions
        int min;                        // min log value, always -1
        int max;                        // max log value, up to 9
        int[][] conditionLogLevels;     // logLevels[condition][logValue]
        public ExpressionLevelsDistribution(int conditions, int min, int max) {
            this.conditions = conditions;
            this.min = min;
            this.max = max;
            this.conditionLogLevels = new int[conditions][];
            int logValues = max-min+1;
            for(int i = 0; i < conditions; i++) {
                this.conditionLogLevels[i] = new int[logValues];
                for(int j = 0; j < logValues; j++)
                    this.conditionLogLevels[i][j] = 0;
            }
        }
    }
}
