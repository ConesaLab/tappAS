/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tappas.DbProject.AFIdStatsData;
import tappas.DbProject.AFStatsData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

//
// TODO: Clean up and move some code to one or more new classes - way too big right now
//

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DataAnnotation extends AppObject {
    static public final int MIN_GOCNT = 5;
    static public final int MAX_GOCNT = 500;
    static public final int TRANS_CUTOFF_LEN = 100;
    static public final int PROTEIN_CUTOFF_LEN = 10;
    static public final int MAX_UTR_LENGTH_DIFF = 60;

    static enum AnnotationType { TRANS, PROTEIN, GENOMIC };
    static enum DiversityType { LENGTH, COUNT, TYPE_COUNT };
    static public enum IsoRegion { ALL, CODING, NONCODING };
    static public enum IsoType { ALL, CODING, NONCODING };
    
    public static final String PROVEAN_EXACT = "Exact_Principal_Isoform"; // update if TSV is changed to a value
    public static final double PEXACT_SCORE = Double.MAX_VALUE;
    static public final String DBCATNOID = "<NO_ID>";
    
    // Internal DB categories
    public static final String INTDB = "~Internal";
    public static final String INTCAT_FLUSHDATA = "~nt_flushdata";
    public static final String INTCAT_NODATA = "~nt_nodata";
    public static final String INTCAT_3UTR = "~nt_3UTR";
    public static final String INTCAT_5UTR = "~nt_5UTR";
    public static final String INTCAT_CDS = "~nt_CDS";
    public static final String INTCAT_SPLICEJUNCTION = "~nt_splice_junction";
    public static final String INTCAT_EXON = "~nt_exon";

    static public enum AnnotFeature {
        NONE, TRANSCRIPT, PROTEIN, GENOMIC,
        GENE, CDS, EXON, SPLICEJUNCTION
    }
    public static final String TAPPAS_PROTEIN = "tappASPID_";
    public static final String TAPPAS_SOURCE = "tappAS";
    public static final String STRUCTURAL_SOURCE = "TranscriptAttributes";
    public static final String POLYA_FEATURE = "polyA_Site";
    public static final String CDS_FEATURE = "CDS_Length";
    public static final String UTRLENGTH3_FEATURE = "3UTR_Length";
    public static final String UTRLENGTH5_FEATURE = "5UTR_Length";

    AnnotationFileDefs fileDefs;
    public ArrayList<AnnotFeatureDef> lstAnnotFeatureDefs = new ArrayList<>();
    // gene and protein descriptions
    private HashMap<String, String> hmGeneDescriptions = new HashMap<>();
    private HashMap<String, String> hmProteinDescriptions = new HashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, String>>> hmFeatureDescriptions = new HashMap<>();
    // genes with hashmap of transcripts
    private HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap<>();
    // transcripts with TransData
    private HashMap<String, TransData> hmTransData = new HashMap<>();
    // proteins to transcript map
    private HashMap<String, HashMap<String, Object>> hmProteinTrans = new HashMap<>();

    //
    // Data loaded on a need to basis
    //
    private HashMap<String, HashMap<String, Object>> hmAllAFs = new HashMap<>();
    private HashMap<String, AnnotSourceStats> hmAnnotSourceStats = new HashMap<>();
    private HashMap<String, HashMap<String, AFStatsData>> hmAFStats = new HashMap<>();
    private HashMap<String, ArrayList<String>> hmAFStatsPositional = new HashMap<>();
    private HashMap<String, ArrayList<String>> hmAFStatsPresence = new HashMap<>();
    private HashMap<String, HashMap<String, String>> hmAFStatsType = new HashMap<>();
    // transcript:source:feature:id:"count:positions string"
    // Ex: PB.1606.1:pfam:DOMAIN:PF17121:"2:P3-50;P63-92"
    private HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPos = new HashMap<>(); //FEA
    private HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdGenPos = new HashMap<>(); //NewGenomicPos
    
    public DataAnnotation(Project project) {
        super(project, null);
    }
    public void initialize() {
        clearData();
        
        // populate reserved source/feature list
        fileDefs = project.data.getAnnotationFileDefs();
        lstAnnotFeatureDefs.clear();
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.TRANSCRIPT, fileDefs.transcript.source, fileDefs.transcript.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.PROTEIN, fileDefs.protein.source, fileDefs.protein.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.GENOMIC, fileDefs.genomic.source, fileDefs.genomic.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.GENE, fileDefs.gene.source, fileDefs.gene.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.CDS, fileDefs.cds.source, fileDefs.cds.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.EXON, fileDefs.exon.source, fileDefs.exon.feature));
        lstAnnotFeatureDefs.add(new AnnotFeatureDef(AnnotFeature.SPLICEJUNCTION, fileDefs.sj.source, fileDefs.sj.feature));
    }
    
    //
    // Source:Feature Functions
    //
    public boolean isRsvdNoDiversityFeature(String db, String cat) { 
        return (getRsvdAnnotFeature(db, cat).equals(AnnotFeature.GENE) || getRsvdAnnotFeature(db, cat).equals(AnnotFeature.GENOMIC) ||
                getRsvdAnnotFeature(db, cat).equals(AnnotFeature.PROTEIN) || getRsvdAnnotFeature(db, cat).equals(AnnotFeature.CDS) ||
                getRsvdAnnotFeature(db, cat).equals(AnnotFeature.TRANSCRIPT) || getRsvdAnnotFeature(db, cat).equals(AnnotFeature.EXON));
    }
    public boolean isRsvdFeature(String db, String cat) { return (!getRsvdAnnotFeature(db, cat).equals(AnnotFeature.NONE) || isInternalFeature(db, cat)); }
    public boolean isInternalFeature(String db, String cat) { return cat.startsWith("~nt_"); }
    public AnnotFeatureDef getRsvdAnnotFeatureDef(AnnotFeature id) {
        AnnotFeatureDef def = null;
        for(AnnotFeatureDef d : lstAnnotFeatureDefs) {
            if(d.id.equals(id)) {
                def = new AnnotFeatureDef(d.id, d.source, d.feature);
                break;
            }
        }
        return def;
    }
    public AnnotFeature getRsvdAnnotFeature(String source, String feature) {
        AnnotFeature id = AnnotFeature.NONE;
        for(AnnotFeatureDef d : lstAnnotFeatureDefs) {
            if(d.source.equals(source) && d.feature.equals(feature)) {
                id = d.id;
                break;
            }
        }
        return id;
    }

    //
    // Public Functions
    //
    
    // Note: this is run on a background thread when a project is first opened
    //       no direct GUI access is allowed from any of these functions
    //       It is the responsibility of higher level code to make sure none of 
    //       the relevant functions in this class are called before the data is loaded
    public boolean loadBaseData() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        clearData();
        System.out.println("Loading base project annotation data...");
        hmGeneTrans = project.data.loadGeneTrans();
        DbProject.TransDataResults trd = project.data.loadTransData();
        //System.out.println("Assigning transcript values...");
        hmTransData = trd.hmTransData;
        //System.out.println("Assigning gene descriptions...");
        hmGeneDescriptions = trd.hmGeneDescriptions;
        //System.out.println("Assigning protein descriptions...");
        hmProteinDescriptions = trd.hmProteinDescriptions;
        hmFeatureDescriptions = project.data.loadFeatureDescriptions();
        hmAFStatsPresence = project.data.loadAFStatsPresence();
        hmAFStatsPositional = project.data.loadAFStatsPositional();
        hmAFStats = project.data.loadAFStats();
        hmAFStatsType = project.data.loadAFStatsType();
        
        if(!hmGeneTrans.isEmpty() && !hmTransData.isEmpty()) {
            // build protein to transcript map
            for(TransData td : hmTransData.values()) {
                if(!td.protein.isEmpty()) {
                    if(!hmProteinTrans.containsKey(td.protein)) {
                        HashMap<String, Object> hm = new HashMap<>();
                        hm.put(td.trans, null);
                        hmProteinTrans.put(td.protein, hm);
                    }
                    else
                        hmProteinTrans.get(td.protein).put(td.trans, null);
                }
            }
            result = true;
        }
        //result = true;

        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    public void clearData() {
        hmGeneTrans.clear();
        hmTransData.clear();
        hmProteinTrans.clear();
        hmGeneDescriptions.clear();
        hmTransAFIdPos.clear();
        hmAFStats.clear();
        hmAnnotSourceStats.clear();
    }
    
    public void clearFilteredData() {
        // clear statistical data
        hmAFStats.clear();
        hmAnnotSourceStats.clear();
    }
    
    // get annotation summary data for given transcript group filter - database:category:[entries, uniqueIds]
    public SummaryData getSummaryData(HashMap<String, HashMap<String, Object>> hmTGFilter, boolean includeReserved) {
        HashMap<String, String> hmTFilter = new HashMap<>();
        HashMap<String, Object> hmGeneCount = new HashMap<>();
        HashMap<String, Object> hmTransCount = new HashMap<>();
        boolean filter = (hmTGFilter != null && !hmTGFilter.isEmpty());
        if(filter) {
            for(String gene : hmTGFilter.keySet()) {
                HashMap<String, Object> hm = hmTGFilter.get(gene);
                for(String trans : hm.keySet())
                    hmTFilter.put(trans, gene);
            }
        }

        // this can take a few secs to load if not already loaded
        loadTransAFIdPos();
        HashMap<String, HashMap<String, HashMap<String, Integer>>> hmDBCatIdCounts = new HashMap<>();
        for(String trans : hmTransAFIdPos.keySet()) {
            if(!filter || hmTFilter.containsKey(trans)) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmTransDB = hmTransAFIdPos.get(trans);
                for(String db : hmTransDB.keySet()) {
                    HashMap<String, HashMap<String, String>> hmTransDBCat = hmTransDB.get(db);
                    for(String cat : hmTransDBCat.keySet()) {
                        if(includeReserved || !isRsvdFeature(db, cat)) {
                            // only update the gene/trans count hashmaps if we include the db cat
                            hmTransCount.put(trans, null);
                            hmGeneCount.put(hmTFilter.get(trans), null);
                            HashMap<String, String> hmTransDBCatId = hmTransDBCat.get(cat);
                            for(String id : hmTransDBCatId.keySet()) {
                                if(hmDBCatIdCounts.containsKey(db)) {
                                    HashMap<String, HashMap<String, Integer>> hmCat = hmDBCatIdCounts.get(db);
                                    if(hmCat.containsKey(cat)) {
                                        HashMap<String, Integer> hmId = hmCat.get(cat);
                                        if(hmId.containsKey(id))
                                            hmId.put(id, hmId.get(id) + TransFeatureIdValues.getCount(hmTransDBCatId.get(id)));
                                        else
                                            hmId.put(id, TransFeatureIdValues.getCount(hmTransDBCatId.get(id)));
                                    }
                                    else {
                                        HashMap<String, Integer> hmId = new HashMap<>();
                                        hmId.put(id, TransFeatureIdValues.getCount(hmTransDBCatId.get(id)));
                                        hmCat.put(cat, hmId);
                                    }
                                }
                                else {
                                    HashMap<String, HashMap<String, Integer>> hmCat = new HashMap<>();
                                    HashMap<String, Integer> hmId = new HashMap<>();
                                    hmId.put(id, TransFeatureIdValues.getCount(hmTransDBCatId.get(id)));
                                    hmCat.put(cat, hmId);
                                    hmDBCatIdCounts.put(db, hmCat);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        SummaryData data = new SummaryData(hmGeneCount.size(), hmTransCount.size(), hmDBCatIdCounts);
        return data;
    }
    public HashMap<String, AFStatsData> getSummaryDBStatsData(String dbFilter, HashMap<String, HashMap<String, Object>> hmTGFilter, boolean includeReserved) {
        HashMap<String, AFStatsData> hmDBCatStats = new HashMap<>();
        HashMap<String, String> hmTFilter = new HashMap<>();
        boolean filter = (hmTGFilter != null && !hmTGFilter.isEmpty());
        if(filter) {
            for(String gene : hmTGFilter.keySet()) {
                HashMap<String, Object> hm = hmTGFilter.get(gene);
                for(String trans : hm.keySet())
                    hmTFilter.put(trans, gene);
            }
        }

        loadTransAFIdPos();
        for(String trans : hmTransAFIdPos.keySet()) {
            if(!filter || hmTFilter.containsKey(trans)) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDB = hmTransAFIdPos.get(trans);
                for(String db : hmDB.keySet()) {
                    if(db.equals(dbFilter)) {
                        HashMap<String, HashMap<String, String>> hmCat = hmDB.get(db);
                        for(String cat : hmCat.keySet()) {
                            if(includeReserved || !isRsvdFeature(db, cat)) {
                                boolean updcnt = true;
                                HashMap<String, String> hmIds = hmCat.get(cat);
                                for(String id : hmIds.keySet()) {
                                    if(hmDBCatStats.containsKey(cat)) {
                                        // we are only going to track the category transcripts in the loop
                                        // we will use the end results to set the counts and idcounts
                                        AFStatsData sd = hmDBCatStats.get(cat);
                                        if(updcnt) {
                                            sd.transCount++;
                                            updcnt = false;
                                        }
                                        if(sd.hmIds.containsKey(id)) {
                                            // we found an existing category id, add entries for this transcript
                                            // and increment transcript count by one
                                            sd.hmIds.get(id).count += TransFeatureIdValues.getCount(hmIds.get(id));
                                            sd.hmIds.get(id).transCount++;
                                        }
                                        else
                                            sd.hmIds.put(id, new AFIdStatsData("", "", TransFeatureIdValues.getCount(hmIds.get(id)), 1));
                                    }
                                    else {
                                        AFStatsData sd = new AFStatsData(1, 1, 1);
                                        sd.hmIds.put(id, new AFIdStatsData("", "", TransFeatureIdValues.getCount(hmIds.get(id)), 1));
                                        hmDBCatStats.put(cat, sd);
                                        updcnt = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // update the category counts and idCounts from results
        for(String cat : hmDBCatStats.keySet()) {
            AFStatsData sd = hmDBCatStats.get(cat);
            sd.idCount = sd.hmIds.size();
            sd.count = 0;
            for(AFIdStatsData idd : sd.hmIds.values())
                sd.count += idd.count;
        }
        return hmDBCatStats;
    }
    public HashMap<String, AnnotSourceStats> getAnnotSourceStats(HashMap<String, Object> hmFilterTrans) {
        if(hmAnnotSourceStats.isEmpty())
            hmAnnotSourceStats = project.data.loadAnnotSourceStats();
        return hmAnnotSourceStats;
    }
    public ArrayList<String[]> getAnnotationData(String gene, HashMap<String, Object> hmTransFilter) {
        long tstart = System.nanoTime();
        ArrayList<String[]> lst = new ArrayList<>();
        try {
            System.out.println("Annotation data requested for gene '" + gene + "'");
            String fields[];
            String line, transid;
            ArrayList<String> lstAnnotations = project.data.loadGeneAnnotations(gene);
            int cnt = lstAnnotations.size();
            boolean findStart = false;
            boolean errflg = false;
            for(int lstidx = 0; lstidx < cnt && !errflg; lstidx++) {
                line = lstAnnotations.get(lstidx);
                fields = line.split("\t");
                //10 because I add length field
                if(fields.length == 10) {
                    AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(fields[1], fields[2]);
                    if(dbcatid == AnnotFeature.TRANSCRIPT) {
                        transid = fields[0];
                        if(hmTransFilter != null && !hmTransFilter.containsKey(transid)) {
                            // set to find next transcript
                            findStart = true;
                            continue;
                        }
                        lst.add(fields);
                    }
                    else {
                        // check if we are looking for start of transcript annotations
                        if(findStart)
                            continue;
                        else {
                            lst.clear();
                            errflg = true;
                            logger.logError("Bad annotation entry: " + line);
                            break;
                        }
                    }
                    
                    for(++lstidx; lstidx < cnt && !errflg; lstidx++) {
                        line = lstAnnotations.get(lstidx);
                        fields = line.split("\t");
                        if(fields.length == 10) {
                            AnnotFeature afid = project.data.getRsvdAnnotFeature(fields[1], fields[2]);
                            if(afid == AnnotFeature.TRANSCRIPT) {
                                lstidx--;
                                break;
                            }
                            else {
                                if(transid.equals(fields[0]))
                                    lst.add(fields);
                                else
                                    break;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to get gene annotation data: " + e.getMessage());
            lst.clear();
        }
        System.out.println("Returning " + lst.size() + " annotation entries.");
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return lst;
    }
    
    // Get transcripts annotations for the given transcripts list all belonging to the given gene
    public ArrayList<AnnotationDataEntry> getTransAnnotationData(String gene, ArrayList<String> transcripts) {
        HashMap<String, Object> hmTransFilter = new HashMap<>();
        for(String trans : transcripts)
            hmTransFilter.put(trans, null);
        ArrayList<String[]> lad = getAnnotationData(gene, hmTransFilter);
                
        // get gene annotation data
        long tstart = System.nanoTime();
        ArrayList<AnnotationDataEntry> lst = new ArrayList<>();
        ArrayList<AnnotationDataEntry> alst = new ArrayList<>();
        try {
            String[] fields;
            int cnt = lad.size();
            for(int lstidx = 0; lstidx < cnt; lstidx++) {
                fields = lad.get(lstidx);
                int start = 0;
                int end = 0;
                int length = 0;
                try { start = Integer.parseInt(fields[3]); } catch(Exception e) {}
                try { end = Integer.parseInt(fields[4]); } catch(Exception e) {}
                try { length = Integer.parseInt(fields[9]); } catch(Exception e) {}
                lst.clear();
                lst.add(new AnnotationDataEntry(fields[0], fields[1], fields[2], start, end, fields[5], fields[6], fields[7], fields[8], length));
                for(++lstidx; lstidx < cnt; lstidx++) {
                    fields = lad.get(lstidx);
                    AnnotFeature afid = project.data.getRsvdAnnotFeature(fields[1], fields[2]);
                    if(afid == AnnotFeature.TRANSCRIPT) {
                        lstidx--;
                        break;
                    }
                    else {
                        start = 0;
                        end = 0;
                        try { start = Integer.parseInt(fields[3]); } catch(Exception e) {}
                        try { end = Integer.parseInt(fields[4]); } catch(Exception e) {}
                        try { length = Integer.parseInt(fields[9]); } catch(Exception e) {}
                        lst.add(new AnnotationDataEntry(fields[0], fields[1], fields[2], start, end, fields[5], fields[6], fields[7], fields[8], length));
                    }
                }
                // some items are required to be in order so do it just in case
                // tsv is required to have the transcript section first followed by the genomic section and then protein section
                for(AnnotationDataEntry a : lst) {
                    if(getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.TRANSCRIPT)) {
                        alst.add(a);
                        break;
                    }
                }
                for(AnnotationDataEntry a : lst) {
                    if(getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.GENE)) {
                        alst.add(a);
                        break;
                    }
                }
                for(AnnotationDataEntry a : lst) {
                    if(getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.CDS)) {
                        alst.add(a);
                        break;
                    }
                }
                for(AnnotationDataEntry a : lst) {
                    if(getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.EXON))
                        alst.add(a);
                }
                for(AnnotationDataEntry a : lst) {
                    if(!getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.TRANSCRIPT) &&
                            !getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.GENE) &&
                            !getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.CDS) && 
                            !getRsvdAnnotFeature(a.db, a.category).equals(AnnotFeature.EXON))
                        alst.add(a);
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to get gene transcripts annotation data: " + e.getMessage());
            alst.clear();
        }
        System.out.println("Returning " + alst.size() + " entries.");
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return alst;
    }
    
    //
    // Get Transcript Information Functions
    // this information is loaded at initialization for all transcripts
    // 
    public HashMap<String, Object> getGeneTrans(String gene) {
        HashMap<String, Object> hmResults = new HashMap<>();
        if(hmGeneTrans.containsKey(gene)) {
            HashMap<String, Object> hm = hmGeneTrans.get(gene);
            for(String trans : hm.keySet())
                hmResults.put(trans, null);
        }
        return hmResults;
    }
    public HashMap<String, HashMap<String, Object>> getGeneTrans(boolean multipleOnly) {
        HashMap<String, HashMap<String, Object>> hmResults = new HashMap<>();
        for(String gene : hmGeneTrans.keySet()) {
            HashMap<String, Object> hm = new HashMap<>();
            if(!multipleOnly || hmGeneTrans.get(gene).size() > 1) {
                for(String trans : hmGeneTrans.get(gene).keySet())
                    hm.put(trans, null);
                hmResults.put(gene, hm);
            }
        }
        return hmResults;
    }
    public boolean hasGene(String gene) {
        return hmGeneTrans.containsKey(gene);
    }
    public boolean hasProtein(String protein) {
        return hmProteinTrans.containsKey(protein);
    }
    public boolean hasTranscript(String trans) {
        return hmTransData.containsKey(trans);
    }

    public String getGeneChromo(String gene) {
        String chromo = "";
        if(hmGeneTrans.containsKey(gene)) {
            for(String trans : hmGeneTrans.get(gene).keySet()) {
                TransData td = hmTransData.get(trans);
                if(td != null) {
                    chromo = td.chromo;
                    break;
                }
            }
        }
        return chromo;
    }
    public String getGeneStrand(String gene) {
        String strand = "";
        if(hmGeneTrans.containsKey(gene)) {
            for(String trans : hmGeneTrans.get(gene).keySet()) {
                TransData td = hmTransData.get(trans);
                if(td != null) {
                    strand = td.negStrand? "-" : "+";
                    break;
                }
            }
        }
        return strand;
    }
    public int getGeneProteinsCount(String gene) {
        int count = 0;
        if(hmGeneTrans.containsKey(gene))
            count = getTransProteinCount(hmGeneTrans.get(gene));
        return count;
    }
    public int getGeneTransCount(String gene) {
        int count = 0;
        if(hmGeneTrans.containsKey(gene))
            count = hmGeneTrans.get(gene).size();
        return count;
    }
    // Note: will return counts of 0 for genes that do not have the requested type
    public HashMap<String, Integer> getGeneTransCount(IsoType type) {
        HashMap<String, Integer> hmResults = new HashMap<>();
        HashMap<String, Object> hm;
        boolean coding;
        for(String gene : hmGeneTrans.keySet()) {
            int cnt = 0;
            hm = hmGeneTrans.get(gene);
            if(type == IsoType.ALL)
                cnt = hm.size();
            else {
                for(String trans : hm.keySet()) {
                    coding = isTransCoding(trans);
                    if((coding && type == IsoType.CODING) || (!coding && type == IsoType.NONCODING))
                        cnt++;
                }
            }
            hmResults.put(gene, cnt);
        }
        return hmResults;
    }
    public HashMap<String, HashMap<String, Object>> getAssignedProteinGenes() {
        HashMap<String, HashMap<String, Object>> hmResults = new HashMap<>();
        HashMap<String, Object> hm;
        String prot;
        boolean coding;
        for(String gene : hmGeneTrans.keySet()) {
            hm = hmGeneTrans.get(gene);
            for(String trans : hm.keySet()) {
                coding = isTransCoding(trans);
                if(coding) {
                    prot = getTransAssignedProtein(trans);
                    if(!hmResults.containsKey(prot))
                        hmResults.put(prot, new HashMap<>());
                    HashMap<String, Object> hmGenes = hmResults.get(prot);
                    if(!hmGenes.containsKey(gene))
                        hmGenes.put(gene, null);
                }
            }
        }
        return hmResults;
    }
    public String getGeneDescription(String gene) {
        String desc = "";
        if(hmGeneDescriptions.containsKey(gene))
            desc= hmGeneDescriptions.get(gene);
        return desc;
    }
    public String getProteinDescription(String protein) {
        String desc = "";
        if(hmProteinDescriptions.containsKey(protein))
            desc= hmProteinDescriptions.get(protein);
        return desc;
    }
    
    public String getFeatureDescription(String source, String feature, String ID) {
        String desc = "";
        if(hmFeatureDescriptions.containsKey(source)){
            HashMap<String, HashMap<String, String>> hmFeature = hmFeatureDescriptions.get(source);
            if(hmFeature.containsKey(feature)){
                HashMap<String, String> hmID = hmFeature.get(feature);
                if(hmID.containsKey(ID)){
                    desc = hmID.get(ID);
                }
            }
        }
        return desc;
    }
    
    public HashMap<String, HashMap<String, Object>> getGeneAssignedProteins(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        HashMap<String, HashMap<String, Object>> hmResults = new HashMap<>();
        HashMap<String, Object> hm;
        boolean coding;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                String prot;
                hm = hmGeneTrans.get(gene);
                for(String trans : hm.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        coding = isTransCoding(trans);
                        if(coding) {
                            prot = getTransAssignedProtein(trans);
                            if(!hmResults.containsKey(gene))
                                hmResults.put(gene, new HashMap<>());
                            HashMap<String, Object> hmGenes = hmResults.get(gene);
                            if(!hmGenes.containsKey(prot))
                                hmGenes.put(prot, null);
                        }
                    }
                }
            }
        }
        return hmResults;
    }
    // Note: will return counts of 0 for genes that do not have any assigned proteins
    public HashMap<String, Integer> getGeneAssignedProteinsCount(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        HashMap<String, Integer> hmResults = new HashMap<>();
        HashMap<String, Object> hm;
        boolean coding;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                String prot;
                hm = hmGeneTrans.get(gene);
                HashMap<String, Object> hmProts = new HashMap<>();
                for(String trans : hm.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        coding = isTransCoding(trans);
                        if(coding) {
                            prot = getTransAssignedProtein(trans);
                            if(!hmProts.containsKey(prot))
                                hmProts.put(prot, null);
                        }
                    }
                }
                hmResults.put(gene, hmProts.size());
            }
        }
        return hmResults;
    }
    public int getAssignedProteinsCount(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        HashMap<String, Object> hm;
        HashMap<String, String> hmDupProts = new HashMap<>();
        HashMap<String, Object> hmProts = new HashMap<>();
        String prot;
        boolean coding;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hm = hmGeneTrans.get(gene);
                for(String trans : hm.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        coding = isTransCoding(trans);
                        if(coding) {
                            prot = getTransAssignedProtein(trans);
                            if(!hmProts.containsKey(prot))
                                hmProts.put(prot, null);
                            else
                                hmDupProts.put(gene + "\t" + trans, prot);
                        }
                    }
                }
            }
        }
        for(String key : hmDupProts.keySet())
            System.out.println(key + "\t" + hmDupProts.get(key));
        System.out.println("Dup proteins: " + hmDupProts.size());
        return hmProts.size();
    }
    public HashMap<String, Integer> getAssignedProteinsLength(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        int cnt = 0;
        HashMap<String, Object> hm;
        HashMap<String, Integer> hmProts = new HashMap<>();
        String prot;
        SeqAlign.Position pos;

        boolean coding;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hm = hmGeneTrans.get(gene);
                for(String trans : hm.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        coding = isTransCoding(trans);
                        if(coding) {
                            prot = getTransAssignedProtein(trans);
                            if(!hmProts.containsKey(prot)) {
                                pos = hmTransData.get(trans).posCDS;
                                int len = pos.end - pos.start + 1;
                                if(pos.start == 0 || pos.end == 0)
                                    len = 0;
                                hmProts.put(prot, len/3);
                            }
                        }
                    }
                }
            }
        }
        return hmProts;
    }
    // will return a transcript length of 0 for transcripts not found in transcript length list
    public HashMap<String, HashMap<String, Integer>> getGeneTransLength(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        HashMap<String, HashMap<String, Integer>> hmResults = new HashMap<>();
        HashMap<String, Object> hmGT;
        HashMap<String, Integer> hm;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hmGT = hmGeneTrans.get(gene);
                hm = new HashMap<>();
                hmResults.put(gene, hm);
                for(String trans : hmGT.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        if(hmTransData.containsKey(trans))
                            hm.put(trans, hmTransData.get(trans).length);
                        else
                            hm.put(trans, 0);
                    }
                }
            }
        }
        return hmResults;
    }
    // will not include proteins for any transcript not in list
    public HashMap<String, HashMap<String, Integer>> getGeneProteinsLength(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        HashMap<String, HashMap<String, Integer>> hmResults = new HashMap<>();
        HashMap<String, Object> hmGT;
        HashMap<String, Integer> hm;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hmGT = hmGeneTrans.get(gene);
                hm = new HashMap<>();
                hmResults.put(gene, hm);
                for(String trans : hmGT.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        if(hmTransData.containsKey(trans)) {
                            TransData td = hmTransData.get(trans);
                            if(td.coding)
                                hm.put(td.protein, td.protLength);
                        }
                    }
                }
            }
        }
        return hmResults;
    }
    public int getTransLength(String trans) {
        int len = 0;
        if(hmTransData.containsKey(trans))
            len = hmTransData.get(trans).length;
        return len;
    }
    public int getTransGenomicLength(String trans) {
        int len = 0;
        if(hmTransData.containsKey(trans)) {
            ArrayList<SeqAlign.Position> lst = hmTransData.get(trans).lstExons;
            if(lst != null && !lst.isEmpty()) {
                int strpos = lst.get(0).start;
                int endpos = lst.get(lst.size() - 1).end;
                len = Math.abs(endpos - strpos) + 1;
            }
        }
        return len;
    }
    public int getTransProteinLength(String trans) {
        int len = 0;
        if(hmTransData.containsKey(trans)) {
            if(hmTransData.get(trans).coding) {
                SeqAlign.Position pos = hmTransData.get(trans).posCDS;
                if(pos.start != 0 && pos.end != 0) {
                    len = Math.floorDiv((pos.end - pos.start + 1), 3);
                    // need to adjust for stop codon
                    if(len > 0)
                        len--;
                }
            }
        }
        return len;
    }
    // only the length of transcripts found is returned in the list, i.e. it does not return 0 if not found
    // Warning: the lengths are not in any reliable order - this is meant for distribution computations
    public ArrayList<Integer> getTransLengthArray(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        ArrayList<Integer> lst = new ArrayList<>();
        HashMap<String, Object> hmGT;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hmGT = hmGeneTrans.get(gene);
                for(String trans : hmGT.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        if(hmTransData.containsKey(trans))
                            lst.add(hmTransData.get(trans).length);
                    }
                }
            }
        }
        return lst;
    }
    // only the CDS length of transcripts found is returned in the list, i.e. it does not return 0 if not found
    public ArrayList<Integer> getProteinsLengthArray(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        ArrayList<Integer> lst = new ArrayList<>();
        HashMap<String, Object> hmGT;
        if(hmFilterGeneTrans == null)
            hmFilterGeneTrans = new HashMap<>();
        boolean filter = !hmFilterGeneTrans.isEmpty();
        for(String gene : hmGeneTrans.keySet()) {
            if(!filter || hmFilterGeneTrans.containsKey(gene)) {
                hmGT = hmGeneTrans.get(gene);
                for(String trans : hmGT.keySet()) {
                    if(!filter || hmFilterGeneTrans.get(gene).containsKey(trans)) {
                        if(hmTransData.containsKey(trans)) {
                            SeqAlign.Position pos = hmTransData.get(trans).posCDS;
                            if(pos.start != 0 && pos.end != 0) {
                                int protlen = Math.floorDiv((pos.end - pos.start + 1), 3);
                                // adjust for stop codon
                                if(protlen > 0)
                                    protlen--;
                                lst.add(protlen);
                            }
                        }
                    }
                }
            }
        }
        return lst;
    }
    // Warning: will use the first transcript asigned to protein, there could be multiple ones
    public int getProteinLength(String protein, HashMap<String, Object> hmFilterTrans) {
        int protlen = 0;
        ArrayList<String> lstTrans = getProteinTrans(protein, hmFilterTrans);
        if(!lstTrans.isEmpty()) {
            TransData td = getTransData(lstTrans.get(0));
            SeqAlign.Position pos = td.posCDS;
            if(pos.start != 0 && pos.end != 0) {
                protlen = Math.floorDiv((pos.end - pos.start + 1), 3);
                // adjust for stop codon
                if(protlen > 0)
                    protlen--;
            }
        }
        return protlen;
    }
    public HashMap<String, HashMap<String, Integer>> getProteinTransLengths(HashMap<String, Object> hmProteins, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Integer>> hmProtTrans = new HashMap<>();
        for(String protein : hmProteins.keySet()) {
            ArrayList<String> lstTrans = getProteinTrans(protein, hmFilterTrans);
            for(String trans : lstTrans) {
                if(hmProtTrans.containsKey(protein))
                    hmProtTrans.get(protein).put(trans, getTransData(trans).length);
                else {
                    HashMap<String, Integer> hm = new HashMap<>();
                    hm.put(trans, getTransData(trans).length);
                    hmProtTrans.put(protein, hm);
                }
            }
        }
        return hmProtTrans;
    }
    
    public int getTransGeneCount(List<String> lstTrans, HashMap<String, Object> hmGeneFilter) {
        String gene;
        HashMap<String, Object> hmResults = new HashMap<>();
        for(String trans : lstTrans) {
            gene = getTransGene(trans);
            if(!gene.isEmpty() && hmGeneFilter.containsKey(gene)) {
                if(!hmResults.containsKey(gene))
                    hmResults.put(gene, null);
            }
        }
        return hmResults.size();
    }
    // multiple transcripts can code for the same protein
    // this returns the number of unique proteins not the number of coding transcripts
    public int getTransProteinCount(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Object> hmProteins = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                String protein = getTransProtein(trans);
                if(!protein.isEmpty())
                    hmProteins.put(protein, null);
            }
        }
        return hmProteins.size();
    }
    // will return 0 count for genes with no proteins
    public HashMap<String, Integer> getGeneProteinCounts(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Integer> hmGeneProteinCounts = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmGeneProteins = getGeneProteins(hmFilterTrans);
        for(String gene : hmGeneProteins.keySet())
            hmGeneProteinCounts.put(gene, hmGeneProteins.get(gene).size());
        return hmGeneProteinCounts;
    }
    public HashMap<String, HashMap<String, Object>> getGeneProteins(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneProteins = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                String gene = getTransGene(trans);
                if(!hmGeneProteins.containsKey(gene))
                    hmGeneProteins.put(gene, new HashMap());
                String protein = getTransProtein(trans);
                if(!protein.isEmpty()) {
                    HashMap<String, Object> hm = hmGeneProteins.get(gene);
                    hm.put(protein, null);
                }
            }
        }
        return hmGeneProteins;
    }
    public SeqAlign.Position getLocusPosition(DataApp.DataType dataType, String id) {
        SeqAlign.Position pos = new SeqAlign.Position(0, 0);
        HashMap<String, Object> hm;
        SeqAlign.Position posTrans;
        ArrayList<String> lst;
        switch(dataType) {
            case TRANS:
                if(hmTransData.containsKey(id))
                    pos = hmTransData.get(id).pos;
                break;
            case PROTEIN:
                lst = getProteinTrans(id, null);
                for(String trans : lst) {
                    if(hmTransData.containsKey(trans)) {
                        // using the first transcript that codes for this protein
                        // use getLocusPositions if all needed
                        pos = hmTransData.get(trans).pos;
                        break;
                    }
                }
                break;
            case GENE:
                hm = getGeneTrans(id);
                pos.start = Integer.MAX_VALUE;
                for(String trans : hm.keySet()) {
                    if(hmTransData.containsKey(trans)) {
                        posTrans = hmTransData.get(trans).pos;
                        if(pos.start > posTrans.start)
                            pos.start = posTrans.start;
                        if(pos.end < posTrans.end)
                            pos.end = posTrans.end;
                    }
                }
                if(pos.start == Integer.MAX_VALUE)
                    pos.start = 0;
                break;
        }
        return pos;
    }
    public int getTransJunctionsCount(String trans) {
        int juncs = 0;
        if(hmTransData.containsKey(trans)) {
            if(!hmTransData.get(trans).lstExons.isEmpty())
                juncs = hmTransData.get(trans).lstExons.size() - 1;
        }
        return juncs;
    }
    public int getTransExonsCount(String trans) {
        int exons = 0;
        if(hmTransData.containsKey(trans)) {
            if(!hmTransData.get(trans).lstExons.isEmpty())
                exons = hmTransData.get(trans).lstExons.size();
        }
        return exons;
    }
    public HashMap<String, Boolean> getTransCoding(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Boolean> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans))
                hmResults.put(trans, hmTransData.get(trans).coding);
        }
        return hmResults;
    }

    public HashMap<String, ChromoDistributionData> getGeneTransChromoDistribution(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, ChromoDistributionData> hmResults = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmGChromos = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmTChromos = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmPChromos = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        HashMap<String, Object> hmT, hmP, hmG;
        TransData td;
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                td = hmTransData.get(trans);
                
                // check if chromo already exists
                if(!hmTChromos.containsKey(td.chromo)) {
                    hmT = new HashMap<>();
                    hmT.put(trans, null);
                    hmTChromos.put(td.chromo, hmT);
                    hmG = new HashMap<>();
                    hmG.put(td.gene, null);
                    hmGChromos.put(td.chromo, hmG);
                    hmP = new HashMap<>();
                    if(td.coding) {
                        hmP.put(td.protein, null);
                        hmPChromos.put(td.chromo, hmP);
                    }
                    else
                        hmPChromos.put(td.chromo, new HashMap<>());
                }
                else {
                    hmT = hmTChromos.get(td.chromo);
                    hmT.put(trans, null);
                    hmG = hmGChromos.get(td.chromo);
                    hmG.put(td.gene, null);
                    hmP = hmPChromos.get(td.chromo);
                    if(td.coding)
                        hmP.put(td.protein, null);
                }
            }
        }
        for(String chr : hmTChromos.keySet()) {
            hmT = hmTChromos.get(chr);
            hmG = hmGChromos.get(chr);
            hmP = hmPChromos.get(chr);
            ChromoDistributionData cdd = new ChromoDistributionData(hmG.size(), hmT.size(), hmP.size());
            hmResults.put(chr, cdd);
        }
        return hmResults;
    }
    public boolean isGeneCoding(String gene) {
        boolean coding = false;
        if(hmGeneTrans.containsKey(gene)) {
            HashMap<String, Object> hmTrans = hmGeneTrans.get(gene);
            for(String trans : hmTrans.keySet()) {
                TransData td = getTransData(trans);
                if(td != null && td.coding) {
                    coding = true;
                    break;
                }
            }
        }
        return coding;
    }
    public boolean isTransCoding(String trans) {
        boolean coding = false;
        if(hmTransData.containsKey(trans))
            coding = hmTransData.get(trans).coding;
        return coding;
    }
    public String getTransGene(String trans) {
        String gene = "";
        if(hmTransData.containsKey(trans))
            gene = hmTransData.get(trans).gene;
        return gene;
    }
    public String getTransChromo(String trans) {
        String chromo = "";
        if(hmTransData.containsKey(trans))
            chromo = hmTransData.get(trans).chromo;
        return chromo;
    }
    public String getTransStrand(String trans) {
        String strand = "+";
        if(hmTransData.containsKey(trans))
            strand = hmTransData.get(trans).negStrand? "-" : "+";
        return strand;
    }
    public String getTransName(String trans) {
        String name = "";
        if(hmTransData.containsKey(trans))
            name = hmTransData.get(trans).transName;
        return name;
    }
    public String getTransProtein(String trans) {
        String protein = "";
        if(hmTransData.containsKey(trans))
            protein = hmTransData.get(trans).protein;
        return protein;
    }
    // must take into account filtering since multiple transcripts can have same protein
    public ArrayList<String> getProteinTrans(String protein, HashMap<String, Object> hmFilterTrans) {
        ArrayList<String> lst = new ArrayList<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        if(hmProteinTrans.containsKey(protein)) {
            HashMap<String, Object> hm = hmProteinTrans.get(protein);
            for(String trans : hm.keySet()) {
                if(!filter || hmFilterTrans.containsKey(trans))
                    lst.add(trans);
            }
        }
        return lst;
    }
    public HashMap<String, Integer> getProteinsTransCounts(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Integer> hmProts = new HashMap<>();
        for(String protein : hmProteinTrans.keySet()) {
            ArrayList<String> lst = getProteinTrans(protein, hmFilterTrans);
            if(!lst.isEmpty())
                hmProts.put(protein, lst.size());
        }
        return hmProts;
    }
    // must take into account filtering since multiple transcripts can have same protein
    // Warning: the first valid gene will be returned but there can be multiple ones
    public String getProteinGene(String protein, HashMap<String, Object> hmFilterTrans) {
        String gene = "";
        ArrayList<String> lstProteins = getProteinTrans(protein, hmFilterTrans);
        if(!lstProteins.isEmpty())
            gene = getTransGene(lstProteins.get(0));
        return gene;
    }
    public ArrayList<String> getProteinGenes(String protein, HashMap<String, Object> hmFilterTrans) {
        ArrayList<String> lstGenes = new ArrayList<>();
        ArrayList<String> lstTrans = getProteinTrans(protein, hmFilterTrans);
        HashMap<String, Object> hmGenes = new HashMap<>();
        for(String trans : lstTrans) {
            String gene = getTransGene(trans);
            hmGenes.put(gene, null);
        }
        for(String gene : hmGenes.keySet())
            lstGenes.add(gene);
        return lstGenes;
    }
    public String getTransAssignedProtein(String trans) {
        String protein = "";
        if(hmTransData.containsKey(trans))
            protein = hmTransData.get(trans).protein;
        return protein;
    }
    public ArrayList<String> getAlignmentCategoriesDisplayOrder() {
        ArrayList<String> lst = new ArrayList<>();
        HashMap<String, Object> hmCats = new HashMap<>();
        for(String trans : hmTransData.keySet()) {
            TransData td = hmTransData.get(trans);
            if(!hmCats.containsKey(td.alignCat)) {
                hmCats.put(td.alignCat, null);
                lst.add(td.alignCat);
            }
        }
        Collections.sort(lst, String.CASE_INSENSITIVE_ORDER);
        return lst;
    }
    public HashMap<String, HashMap<String, Object>> getTransAnnotFeatures(String trans) {
        HashMap<String, HashMap<String, Object>> hmTransAFs = new HashMap<>();
        loadTransAFIdPos();
        if(hmTransAFIdPos.containsKey(trans)) {
            HashMap<String, HashMap<String, HashMap<String, String>>> hmSources = hmTransAFIdPos.get(trans);
            for(String source : hmSources.keySet()) {
                HashMap<String, Object> hm = new HashMap<>();
                HashMap<String, HashMap<String, String>> hmFeatures = hmSources.get(source);
                for(String feature : hmFeatures.keySet())
                    hm.put(feature, null);
                hmTransAFs.put(source, hm);
            }
        }
        return hmTransAFs;
    }
    
    public int getIfTransHasAnnot(String trans, String source) {
        return project.data.getIfTransHasAnnot(trans, source);
    }
    
    public int getTotalAnnot() {
        return project.data.getTotalAnnot();
    }
    
    public int getAnnotCount(String annot) {
        return project.data.getAnnotCount(annot);
    }
    
    public int getAnnotCountByGene(String annot) {
        return project.data.getAnnotCountByGene(annot);
    }
    
    public HashMap<String, HashMap<String, String>> getTransAnnotFeatures(String trans, String source) {
        HashMap<String, HashMap<String, String>> hmAFIDs = new HashMap<>();
        loadTransAFIdPos();
        if(hmTransAFIdPos.containsKey(trans)) {
            HashMap<String, HashMap<String, HashMap<String, String>>> hmTransAFIDs = hmTransAFIdPos.get(trans);
            if(hmTransAFIDs.containsKey(source))
                hmAFIDs = hmTransAFIDs.get(source);
        }
        return hmAFIDs;
    }

    // we are looking for distribution data
    // cat:cnt is a unique value
    public int[] getFeatureDistributionPerTrans(String source, String feature, int maxCnt, HashMap<String, Object> hmFilterTrans) {
        int[] dist = new int[maxCnt+1];
        for(int i = 0; i <= maxCnt; i++)
            dist[i] = 0;
        loadTransAFIdPos();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();

        // transcript:source:feature:ids
        for(String trans : hmTransAFIdPos.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmTransAFIdPos.get(trans);
                if(hmDBs.containsKey(source)) {
                    HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(source);
                    // check if no value for cat given, all categories if so
                    if(feature.isEmpty()) {
                        int total = 0;
                        for(HashMap<String, String> hmIds : hmCats.values()) {
                            for(String idValues : hmIds.values())
                                total += TransFeatureIdValues.getCount(idValues);
                        }
                        if(total > maxCnt)
                            total = maxCnt;
                        dist[total]++;
                    }
                    else {
                        if(hmCats.containsKey(feature)) {
                            int total = 0;
                            for(String idValues : hmCats.get(feature).values())
                                total += TransFeatureIdValues.getCount(idValues);
                            if(total > maxCnt)
                                total = maxCnt;
                            dist[total]++;
                        }
                    }
                }
            }
        }
        return dist;
    }
    public TransData getTransData(String trans) {
        TransData td = null;
        if(hmTransData.containsKey(trans))
            td = hmTransData.get(trans);
        return td;
    }
    public ArrayList<TransData> getTransData(HashMap<String, Object> hmFilterTrans) {
        ArrayList<TransData> lstData = new ArrayList<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans))
                lstData.add(hmTransData.get(trans));
        }
        return lstData;
    }
    public HashMap<String, TransData> getTransDataHM(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, TransData> hmData = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans))
                hmData.put(trans, hmTransData.get(trans));
        }
        return hmData;
    }
    public HashMap<String, Integer> getAlignmentCategoriesTransCount(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, Integer> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                if(!hmResults.containsKey(td.alignCat))
                    hmResults.put(td.alignCat, 1);
                else
                    hmResults.put(td.alignCat, hmResults.get(td.alignCat) + 1);
            }
        }
        return hmResults;
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransLengths(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, ArrayList<Integer>> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                if(!hmResults.containsKey(td.alignCat))
                    hmResults.put(td.alignCat, new ArrayList<>());
                hmResults.get(td.alignCat).add(td.length);
            }
        }
        return hmResults;
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesProtLengths(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, ArrayList<Integer>> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                if(td.coding) {
                    if(!hmResults.containsKey(td.alignCat))
                        hmResults.put(td.alignCat, new ArrayList<>());
                    hmResults.get(td.alignCat).add((td.posCDS.end - td.posCDS.start + 1)/3);
                }
            }
        }
        return hmResults;
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransJunctions(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, ArrayList<Integer>> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                if(!hmResults.containsKey(td.alignCat))
                    hmResults.put(td.alignCat, new ArrayList<>());
                hmResults.get(td.alignCat).add(getTransJunctionsCount(trans));
            }
        }
        return hmResults;
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransExons(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, ArrayList<Integer>> hmResults = new HashMap<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                if(!hmResults.containsKey(td.alignCat))
                    hmResults.put(td.alignCat, new ArrayList<>());
                hmResults.get(td.alignCat).add(getTransJunctionsCount(trans));
            }
        }
        return hmResults;
    }
    public ArrayList<String> getAlignmentAttributes() {
        ArrayList<String> lst = new ArrayList<>();
        HashMap<String, Object> hmSubCats = new HashMap<>();
        for(String trans : hmTransData.keySet()) {
            TransData td = hmTransData.get(trans);
            if(!td.alignAttrs.isEmpty()) {
                String fields[] = td.alignAttrs.split(",");
                for(String subCat : fields) {
                    if(!hmSubCats.containsKey(subCat)) {
                        hmSubCats.put(subCat, null);
                        lst.add(subCat);
                    }
                }
            }
        }
        Collections.sort(lst, String.CASE_INSENSITIVE_ORDER);
        return lst;
    }
    public HashMap<String, Object> getAlignmentAttributesHM() {
        HashMap<String, Object> hmSubCats = new HashMap<>();
        for(String trans : hmTransData.keySet()) {
            TransData td = hmTransData.get(trans);
            if(!td.alignAttrs.isEmpty()) {
                String fields[] = td.alignAttrs.split(",");
                for(String subCat : fields) {
                    if(!hmSubCats.containsKey(subCat))
                        hmSubCats.put(subCat, null);
                }
            }
        }
        return hmSubCats;
    }
    public ArrayList<Integer> getTransJunctions(HashMap<String, Object> hmFilterTrans) {
        ArrayList<Integer> lst = new ArrayList<>();
        if(hmFilterTrans == null)
            hmFilterTrans = new HashMap<>();
        boolean filter = !hmFilterTrans.isEmpty();
        for(String trans : hmTransData.keySet()) {
            if(!filter || hmFilterTrans.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                lst.add(getTransJunctionsCount(trans));
            }
        }
        return lst;
    }
    public String getTransAlignmentCategory(String trans) {
        String cat = "";
        if(hmTransData.containsKey(trans))
            cat = hmTransData.get(trans).alignCat;
        return cat;
    }
    public String getTransAlignmentAttributes(String trans) {
        String subCats = "";
        if(hmTransData.containsKey(trans))
            subCats = hmTransData.get(trans).alignAttrs;
        return subCats;
    }
    public HashMap<String, Object> getTransAlignmentAttributesHM(String trans) {
        HashMap<String, Object> hmSubCats = new HashMap<>();
        if(hmTransData.containsKey(trans)) {
            if(!hmTransData.get(trans).alignAttrs.isEmpty()) {
                String fields[] = hmTransData.get(trans).alignAttrs.split(",");
                for(String subcat : fields)
                    hmSubCats.put(subcat, null);
            }
        }
        return hmSubCats;
    } 
    
    //
    // Diversity Data Functions
    //
    // Get transcripts terms diversity for the given transcripts list all belonging to the given gene
    // Include reserved categories if incReserved is set
    public TransTermsData getTransTermsData(String gene, ArrayList<String> transcripts, 
                                            HashMap<String, HashMap<String, Object>> hmTerms, boolean incReserved) {
        long tstart = System.nanoTime();
        ArrayList<Term> alst = new ArrayList<>();
        ArrayList<HashMap<String, Integer>> transTerms = new ArrayList<>();
        TransTermsData tgo = new TransTermsData(alst, transTerms);
        HashMap<String, String> genomicPos = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);
        try {
            System.out.println("Terms requested for gene " + gene + " and corresponding transcripts list.");
            System.out.println("hmTerms: " + hmTerms);
            boolean cds = false;
            boolean utr3 = false;
            boolean utr5 = false;
            boolean pas = false;
            for(String db : hmTerms.keySet()) {
                System.out.println("source: " + db);
                switch (db) {
                    case(STRUCTURAL_SOURCE):
                    {
                        HashMap<String, Object> hmFeatures = hmTerms.get(db);
                        for(String feature : hmFeatures.keySet()) {
                            switch(feature){
                                case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                                    cds = true;
                                    break;
                                case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                                    utr3 = true;
                                    break;
                                case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                                    utr5 = true;
                                    break;
                                case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                                    pas = true;
                                    break;
                            }
                        }
                    }
                }
            }
            System.out.println("flgs: " + cds + ", " + utr3 + ", " + utr5 + ", " + pas);
            String transId, id, name, key;
            HashMap<String, Object> hmCat;
            HashMap<String, Object> ids = new HashMap<>();
            HashMap<String, String> attrs;
            ArrayList<AnnotationDataEntry> lstTad = getTransAnnotationData(gene, transcripts);

            // must be in the order given and a structure for each even if empty
            int lstlen = lstTad.size();
            for(String transcript : transcripts) {
                HashMap<String, Integer> hmt = new HashMap<>();
                transTerms.add(hmt);
                AnnotationDataEntry tad = null;
                int idx = 0;
                for(AnnotationDataEntry ade : lstTad) {
                    if(getRsvdAnnotFeature(ade.db, ade.category).equals(AnnotFeature.TRANSCRIPT) && ade.transID.equals(transcript)) {
                        tad = ade;
                        break;
                    }
                    idx++;
                }
                if(tad == null) {
                    logger.logError("Transcript annotation not found for " + transcript + " in gene " + gene + ".");
                    continue;
                }
                int cdsStart, cdsEnd;
                int cdsLength = 0;
                int utr5Length = 0;
                int utr3Length = 0;
                int polyA_site = 0;
                transId = tad.transID;
                int transEnd = tad.end;
                boolean protein = false;
                if(hmTerms.containsKey(tad.db)) {
                    hmCat = hmTerms.get(tad.db);
                    if(hmCat.isEmpty() || hmCat.containsKey(tad.category)) {
                        attrs = getAttributes(tad.attrs);
                        // if we set ids at the transcript level, it will add too many unnecessary lines
                        id = "";
                        key = tad.db + tad.category + id;
                        if(!ids.containsKey(key)) {
                            ids.put(key, null);
                            if(attrs.containsKey(fileDefs.attrName))
                                name = attrs.get(fileDefs.attrName);
                            else if(attrs.containsKey(fileDefs.attrDesc))
                                name = attrs.get(fileDefs.attrDesc);
                            else
                                name = "";
                            alst.add(new Term(id, name, tad.db, tad.category));
                        }
                        if(!hmt.containsKey(key)){
                            if(tad.db.equals(STRUCTURAL_SOURCE)){
                                if(tad.category.equals(POLYA_FEATURE)){
                                    hmt.put(key, tad.start);
                                }else{
                                    hmt.put(key, tad.length);
                                }
                            }else{
                                hmt.put(key, 1);
                            }
                        }else{
                            if(tad.db.equals(STRUCTURAL_SOURCE)){
                                if(tad.category.equals(POLYA_FEATURE)){
                                    hmt.put(key, tad.start);
                                }else{
                                    hmt.put(key, tad.length);
                                }
                            }else{
                                hmt.put(key, hmt.get(key) + 1);
                            }
                        }
                    }
                }

                while(++idx < lstlen) {
                    tad = lstTad.get(idx);
                    if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.TRANSCRIPT))
                        break;

                    // always calculate coding/non-coding boundaries
                    if(!protein && getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.CDS)) {
                        cdsStart = tad.start;
                        cdsEnd = tad.end;
                        cdsLength = cdsEnd - cdsStart + 1;
                        utr5Length = cdsStart - 1;
                        utr3Length = transEnd - cdsEnd;
                        polyA_site = Integer.parseInt(genomicPos.get(tad.transID));
                    }
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.PROTEIN))
                        protein = true;
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.GENOMIC))
                        protein = false;

                    // make sure we stay on same transcript
                    if(transId.equals(tad.transID)) {
                        if(hmTerms.containsKey(tad.db)) {
                            hmCat = hmTerms.get(tad.db);
                            if(hmCat.isEmpty() || hmCat.containsKey(tad.category)) {
                                attrs = getAttributes(tad.attrs);
                                if(attrs.containsKey(fileDefs.attrId))
                                    id = attrs.get(fileDefs.attrId);
                                else
                                    id = "";
                                key = tad.db + tad.category + id;
                                if(!ids.containsKey(key)) {
                                    ids.put(key, null);
                                    if(attrs.containsKey(fileDefs.attrName))
                                        name = attrs.get(fileDefs.attrName);
                                    else if(attrs.containsKey(fileDefs.attrDesc))
                                        name = attrs.get(fileDefs.attrDesc);
                                    else
                                        name = "";
                                    alst.add(new Term(id, name, tad.db, tad.category));
                                }
                                if(!hmt.containsKey(key)){
                                    if(tad.db.equals(STRUCTURAL_SOURCE)){
                                        if(tad.category.equals(POLYA_FEATURE)){
                                            hmt.put(key, tad.start);
                                        }else{
                                            hmt.put(key, tad.length);
                                        }
                                    }else{
                                        hmt.put(key, 1);
                                    }
                                }else{
                                    if(tad.db.equals(STRUCTURAL_SOURCE)){
                                        if(tad.category.equals(POLYA_FEATURE)){
                                            hmt.put(key, tad.start);
                                        }else{
                                            hmt.put(key, tad.length);
                                        }
                                    }else{
                                        hmt.put(key, hmt.get(key) + 1);
                                    }
                                }
                            }
                        }
                        /*else {
                            if(!protein) {
                                if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.CDS)) {
                                    // process special cases he dont have special cases now
                                    if(cds || utr3 || utr5) {
                                        if(cds) {
                                            key = DlgFDAnalysis.FEATURE_CDS_LENGTH;
                                            id = key;
                                            if(!ids.containsKey(id)) {
                                                ids.put(id, null);
                                                alst.add(new Term(id, "", "", ""));
                                            }
                                            hmt.put(key, cdsLength);
                                        }
                                        if(utr5) {
                                            key = DlgFDAnalysis.FEATURE_5UTR_LENGTH;
                                            id = key;
                                            if(!ids.containsKey(key)) {
                                                ids.put(key, null);
                                                alst.add(new Term(id, "", "", ""));
                                            }
                                            hmt.put(key, utr5Length);
                                        }
                                        if(utr3) {
                                            key = DlgFDAnalysis.FEATURE_3UTR_LENGTH;
                                            id = key;
                                            if(!ids.containsKey(key)) {
                                                ids.put(key, null);
                                                alst.add(new Term(id, "", "", ""));
                                            }
                                            hmt.put(key, utr3Length);
                                        }
                                    }
                                }
                            }
                        }*/
                    }
                    else
                        break;
                }
            }
        }
        catch (Exception e) {
           logger.logError("Unable to get gene transcripts features: " + e.getMessage());
            alst.clear();
        }
        System.out.println("Returning " + alst.size() + " entries.");
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return tgo;
    }
    // Get transcripts terms diversity for the given transcripts list all belonging to the given gene
    // Only return transcript or protein region based on isProtein flag
    public TransTermsData getTransTermsDiversity(String gene, ArrayList<String> transcripts, 
            HashMap<String, HashMap<String, Object>> hmTerms, boolean isProtein) {
        long tstart = System.nanoTime();
        ArrayList<Term> alst = new ArrayList<>();
        ArrayList<HashMap<String, Integer>> transTerms = new ArrayList<>();
        TransTermsData tgo = new TransTermsData(alst, transTerms);
        HashMap<String, String> genomicPos = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);
        try {
            System.out.println("Terms requested for gene " + gene + " and corresponding transcripts list.");
            System.out.println("hmTerms: " + hmTerms);
            boolean cds = false;
            boolean utr3 = false;
            boolean utr5 = false;
            for(String db : hmTerms.keySet()) {
                System.out.println("source: " + db);
                switch (db) {
                    case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                        cds = true;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                        utr3 = true;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                        utr5 = true;
                        break;
                }
            }
            System.out.println("flgs: " + cds + ", " + utr3 + ", " + utr5);
            String transId, id, name, key;
            HashMap<String, Object> hmCat;
            HashMap<String, Object> ids = new HashMap<>();
            HashMap<String, String> attrs;
            ArrayList<AnnotationDataEntry> lstTad = getTransAnnotationData(gene, transcripts);

            // transcripts must be in the order given and a structure for each even if empty
            int lstlen = lstTad.size();
            for(String transcript : transcripts) {
                HashMap<String, Integer> hmt = new HashMap<>();
                transTerms.add(hmt);
                AnnotationDataEntry tad = null;
                int idx = 0;
                for(AnnotationDataEntry ade : lstTad) {
                    if(getRsvdAnnotFeature(ade.db, ade.category).equals(AnnotFeature.TRANSCRIPT) && ade.transID.equals(transcript)) {
                        tad = ade;
                        break;
                    }
                    idx++;
                }
                if(tad == null) {
                    logger.logError("Transcript annotation not found for " + transcript + " in gene " + gene + ".");
                    continue;
                }
                int cdsStart, cdsEnd;
                int cdsLength = 0;
                int utr5Start, utr5End;
                int utr5Length = 0;
                int utr3Start, utr3End;
                int utr3Length = 0;
                int polyA_site = 0;

                transId = tad.transID;
                int transEnd = tad.end;
                boolean protein = false;
                if(hmTerms.containsKey(tad.db)) {
                    hmCat = hmTerms.get(tad.db);
                    if(hmCat.isEmpty() || hmCat.containsKey(tad.category)) {
                        attrs = getAttributes(tad.attrs);
                        // if we set ids at the transcript level, it will add too many unnecessary lines
                        id = "";
                        key = tad.db + tad.category + id;
                        if(!ids.containsKey(key)) {
                            ids.put(key, null);
                            if(attrs.containsKey(fileDefs.attrName))
                                name = attrs.get(fileDefs.attrName);
                            else
                                name = "";
                            alst.add(new Term(id, name, tad.db, tad.category));
                        }
                        if(!hmt.containsKey(key))
                            hmt.put(key, 1);
                        else
                            hmt.put(key, hmt.get(key) + 1);
                    }
                }

                while(++idx < lstlen) {
                    tad = lstTad.get(idx);
                    if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.TRANSCRIPT))
                        break;

                    // always calculate coding/non-coding boundaries
                    if(!protein && getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.CDS)) {
                        cdsStart = tad.start;
                        cdsEnd = tad.end;
                        cdsLength = cdsEnd - cdsStart + 1;
                        utr5Start = 1;
                        utr5End = cdsStart - 1;
                        utr5Length = cdsStart - 1;
                        utr3Start = cdsEnd + 1;
                        utr3End = transEnd;
                        utr3Length = transEnd - cdsEnd;
                        polyA_site = Integer.parseInt(genomicPos.get(tad.transID));
                    }
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.PROTEIN))
                        protein = true;
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.GENOMIC))
                        protein = false;

                    // make sure we stay on same transcript
                    if(transId.equals(tad.transID)) {
                        if(hmTerms.containsKey(tad.db)) {
                            hmCat = hmTerms.get(tad.db);
                            if(hmCat.isEmpty() || hmCat.containsKey(tad.category)) {
                                if(protein == isProtein) {
                                    attrs = getAttributes(tad.attrs);
                                    if(attrs.containsKey(fileDefs.attrId))
                                        id = attrs.get(fileDefs.attrId);
                                    else
                                        id = "";
                                    key = tad.db + tad.category + id;
                                    if(!ids.containsKey(key)) {
                                        ids.put(key, null);
                                        if(attrs.containsKey(fileDefs.attrName))
                                            name = attrs.get(fileDefs.attrName);
                                        else
                                            name = "";
                                        alst.add(new Term(id, name, tad.db, tad.category));
                                    }
                                    if(!hmt.containsKey(key))
                                        hmt.put(key, 1);
                                    else
                                        hmt.put(key, hmt.get(key) + 1);
                                }
                            }
                        }
                    }
                    else
                        break;
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to get gene transcripts features: " + e.getMessage());
            alst.clear();
        }
        System.out.println("Returning " + alst.size() + " entries.");
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return tgo;
    }

    // return a hash map of all the features having diversity for given genes/isoforms
    // only genes where some transcripts have the feature and some do not are returned
    // returns: feature->gene->(trans, true/false) - where true is set for transcripts having the feature and false for those that do not
    //          feature = db + "\t" + cat + "\t" + id, caller must separate as needed
    public HashMap<String, HashMap<String, HashMap<String, Object>>> 
           getDiverseFeaturesUsingPresence(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                           HashMap<String, HashMap<String, Object>> hmFeatures) {
        // create diversity hashmap
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiverseFeatures = new HashMap<>();
        
        try {
            int totalGenes = hmGeneIsos.size();
            System.out.println("Cheking " + totalGenes + " genes for feature diversity using presence/absence method...");
            System.out.println("hmFeatures: " + hmFeatures);
            loadTransAFIdPos();
            
            // check all genes
            HashMap<String, Object> hmIsos;
            for(String gene: hmGeneIsos.keySet()) {
                // check if gene has more than 1 isoform, skip if not
                hmIsos = hmGeneIsos.get(gene);
                if(hmIsos.size() < 2)
                    continue;

                // check all requested features DBs
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
                HashMap<String, HashMap<String, String>> hmCats;
                HashMap<String, String> hmIds;
                for(String featureDB : hmFeatures.keySet()) {
                    // check all requested feature categories - a DB with empty hashmap means all categories
                    HashMap<String, Object> hmDBFeatures = hmFeatures.get(featureDB);
                    if(hmDBFeatures.isEmpty())
                        hmDBFeatures = project.data.getSourceFeaturesPresence(featureDB);
                    for(String featureCat : hmDBFeatures.keySet()) {
                        // check features for all gene transcripts
                        HashMap<String, Object> hmProcessed = new HashMap<>();
                        for(String trans : hmIsos.keySet()) {
                            // get all DBs for this transcript
                            hmDBs = hmTransAFIdPos.get(trans);
                            if(hmDBs != null) {
                                // get all DBCats for this transcript
                                hmCats = hmDBs.get(featureDB);
                                if(hmCats != null) {
                                    // get all DBCatIDs for this transcript
                                    hmIds = hmCats.get(featureCat);
                                    if(hmIds != null) {
                                        // check all IDs for this transcript, some transcripts will have IDs that others don't
                                        for(String id : hmIds.keySet()) {
                                            // check if we have not previously processed this id
                                            String strId = featureDB + "\t" + featureCat + "\t" + id;
                                            if(!hmProcessed.containsKey(strId)) {
                                                hmProcessed.put(strId, null);
                                                HashMap<String, Boolean> hmGeneDiversity = getGeneFeatureDiversity(hmIsos, featureDB, featureCat, id);
                                                boolean diverse = false;
                                                boolean firstflg = true;
                                                boolean chkValue = false;
                                                for(boolean value : hmGeneDiversity.values()) {
                                                    if(firstflg) {
                                                        firstflg = false;
                                                        chkValue = value;
                                                    }
                                                    else {
                                                        if(chkValue != value) {
                                                            diverse = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if(diverse) {
                                                    // check if feature not already in results
                                                    if(!hmDiverseFeatures.containsKey(strId))
                                                        hmDiverseFeatures.put(strId, new HashMap<>());
                                                    HashMap<String, Object> hm = new HashMap<>();
                                                    for(String item : hmGeneDiversity.keySet())
                                                        hm.put(item, hmGeneDiversity.get(item));
                                                    hmDiverseFeatures.get(strId).put(gene, hm);
                                                }else{
                                                    // check if feature not already in results
                                                    if(!hmDiverseFeatures.containsKey(strId))
                                                        hmDiverseFeatures.put(strId, new HashMap<>());
                                                    HashMap<String, Object> hm = new HashMap<>();
                                                    for(String item : hmGeneDiversity.keySet())
                                                        hm.put(item, hmGeneDiversity.get(item));
                                                    hmDiverseFeatures.get(strId).put(gene, hm);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            logger.logError("Feature diversity error: " + e.getMessage());
        }
        return hmDiverseFeatures;
    }
    // check all gene transcripts for the presence/absence of given feature id
    // returns <trans, true/flase> for all trans in hmIsos, true if the trans contains the given db:cat:id
    private HashMap<String, Boolean> getGeneFeatureDiversity(HashMap<String, Object> hmIsos, String db, String cat, String id) {
        HashMap<String, Boolean> hmGeneDiversity = new HashMap<>();
        for(String trans : hmIsos.keySet())
            hmGeneDiversity.put(trans, false);
        
        HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
        HashMap<String, HashMap<String, String>> hmCats;
        HashMap<String, String> hmIds;
        for(String trans : hmIsos.keySet()) {
            // get all DBs for this transcript
            hmDBs = hmTransAFIdPos.get(trans);
            if(hmDBs != null) {
                // get all DBCats for this transcript
                hmCats = hmDBs.get(db);
                if(hmCats != null) {
                    // get all DBCatIDs for this transcript
                    hmIds = hmCats.get(cat);
                    if(hmIds != null && hmIds.containsKey(id))
                        hmGeneDiversity.put(trans, true);
                }
            }
        }
        return hmGeneDiversity;
    }
    // return a hash map of all the features having diversity for given genes/isoforms
    // only of genes where some transcripts have the feature and some do not, or features do not overlap, are returned
    // returns: feature->gene->(trans, Object) 
    //             for (retTransPos == false)- set to Boolean: true for transcripts having the feature in the same location and false if not
    //             for (retTransPos == true)- set to IdPosition: relative position within transcript of the feature id
    //          feature = db +"\t" + cat + "\t" + id + "\t" + idValues, caller must separate as needed
    public HashMap<String, HashMap<String, HashMap<String, Object>>> 
           getDiverseFeaturesUsingGenomicPositionExons(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                           HashMap<String, HashMap<String, Object>> hmFeatures, boolean retTransPos,
                                           HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPosFiltered) {
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiverseFeatures = new HashMap<>();
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTAFIdPos;
        
        try {
            int totalGenes = hmGeneIsos.size();
            System.out.println("Cheking " + totalGenes + " genes for feature diversity using genomic position exons method...");
            System.out.println("hmFeatures: " + hmFeatures);
            
            // only load full hmTransAFIdPos if necessary, use filtered one if given
            if(hmTransAFIdPosFiltered != null)
                hmTAFIdPos = hmTransAFIdPosFiltered;
            else {
                loadTransAFIdPos();
                hmTAFIdPos = hmTransAFIdPos;
            }
            // genomic position, genpos, file generation
            //Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/Users/hdelrisco/Documents/pas.tsv"), "utf-8"));
            
            // check all genes
            HashMap<String, Object> hmIsos;
            for(String gene: hmGeneIsos.keySet()) {
                // check if gene has more than 1 isoform, skip if not
                hmIsos = hmGeneIsos.get(gene);
                // comment out for genpos generation
                if(hmIsos.size() < 2)
                    continue;

                // check all requested features DBs
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
                HashMap<String, HashMap<String, String>> hmCats;
                HashMap<String, String> hmIds;
                for(String featureDB : hmFeatures.keySet()) {
                    // check all requested feature categories - a DB with empty hashmap means all categories
                    HashMap<String, Object> hmDBFeatures = hmFeatures.get(featureDB);
                    if(hmDBFeatures.isEmpty())
                        hmDBFeatures = project.data.getSourceFeatures(featureDB);
                    for(String featureCat : hmDBFeatures.keySet()) {
                        // check features for all gene transcripts
                        HashMap<String, Object> hmProcessed = new HashMap<>();
                        for(String trans : hmIsos.keySet()) {
                            // get transcript data
                            TransData td = getTransData(trans);
                            if(td == null) {
                                System.out.println("WARNING: No transcript data for " + trans);
                                continue;
                            }
                            
                            // get all DBs for this transcript
                            hmDBs = hmTAFIdPos.get(trans);
                            if(hmDBs != null) {
                                // get all DBCats for this transcript
                                hmCats = hmDBs.get(featureDB);
                                if(hmCats != null) {
                                    // get all DBCatIDs for this transcript
                                    hmIds = hmCats.get(featureCat);
                                    if(hmIds != null) {
                                        // check all IDs for this transcript, some transcripts will have IDs that others don't
                                        for(String id : hmIds.keySet()) {
                                            String idValues = hmIds.get(id);
                                            ArrayList<IdPosition> lstPositions = TransFeatureIdValues.getPositions(idValues);
                                            for(IdPosition idp : lstPositions) {
                                                // this needs to be the aligned position otherwise it gets repeated based on where it is at in the transcript
                                                SeqAlign.Position genopos = idp.getGenomicPosition(td);
                                                // genpos
                                                //writer.write(getTransGene(trans) + "\t" + trans + "\t" + (td.negStrand? "-" : "+" ) + "\t" + td.chromo + "\t" + genopos.start + "\t" + genopos.end + "\n");
                                                String strId = featureDB + "\t" + featureCat + "\t" + id + "\t" + TransFeatureIdValues.getPosType(idp.posType) + genopos.start + "-" + genopos.end;

                                                // check if we have not previously processed this id
                                                if(!hmProcessed.containsKey(strId)) {
                                                    hmProcessed.put(strId, null);
                                                    HashMap<String, Boolean> hmGeneDiversity = getGeneFeatureDiversity(hmIsos, featureDB, featureCat, id, idp, td, true, hmTAFIdPos);
                                                    boolean diverse = false;
                                                    boolean firstflg = true;
                                                    boolean chkValue = false;
                                                    for(boolean value : hmGeneDiversity.values()) {
                                                        if(firstflg) {
                                                            firstflg = false;
                                                            chkValue = value;
                                                        }
                                                        else {
                                                            if(chkValue != value) {
                                                                diverse = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if(diverse) {
                                                        // check if feature not already in results
                                                        if(!hmDiverseFeatures.containsKey(strId))
                                                            hmDiverseFeatures.put(strId, new HashMap<>());
                                                        if(retTransPos) {
                                                            // add the feature id relative position for this transcript
                                                            // will do for each transcript as it is processed in the else statement below
                                                            HashMap<String, Object> hm = new HashMap<>();
                                                            hm.put(trans, idp);
                                                            hmDiverseFeatures.get(strId).put(gene, hm);
                                                        }
                                                        else {
                                                            // we only need to add the data for this gene once
                                                            HashMap<String, Object> hm = new HashMap<>();
                                                            for(String item : hmGeneDiversity.keySet())
                                                                hm.put(item, hmGeneDiversity.get(item));
                                                            hmDiverseFeatures.get(strId).put(gene, hm);
                                                        }
                                                    }
                                                }
                                                else {
                                                    // check if we are returning the transcript relative position values
                                                    if(retTransPos) {
                                                        // check if the strId was diverse for this gene
                                                        if(hmDiverseFeatures.containsKey(strId) && hmDiverseFeatures.get(strId).containsKey(gene))
                                                            hmDiverseFeatures.get(strId).get(gene).put(trans, idp);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                System.out.println("not here");
                            }
                        }
                    }
                }
            }
            //genpos
            //writer.close();
        } catch(Exception e) {
            logger.logError("Feature diversity error: " + e.getMessage());
        }
        return hmDiverseFeatures;
    }

    // return a hash map of all the features having diversity for given genes/isoforms
    // return complete feature and partial or exclude feature as afected with a margin of 3 aminoacid (9nc) [if we have a feature in range, another between the range count as the same]
    // returns: feature->gene->(trans, boolean) -> false if partial or not have, true if have the same feature (genomic position) 
    //             for (retTransPos == false)- set to Boolean: true for transcripts having the feature in the same location and false if not
    //             for (retTransPos == true)- set to IdPosition: relative position within transcript of the feature id
    //          feature = db +"\t" + cat + "\t" + id + "\t" + idValues, caller must separate as needed
    public HashMap<String, HashMap<String, HashMap<String, Object>>> getDiverseFeaturesUsingNewGenomicPosition(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                           HashMap<String, HashMap<String, Object>> hmFeatures, boolean retTransPos,
                                           HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPosFiltered) {
        
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiverseFeatures = new HashMap<>();
        HashMap<String, HashMap<String, Boolean>> hmAdded = new HashMap<>();
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTAFIdPos;
        HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
        
        try {
            int totalGenes = hmGeneIsos.size();
            System.out.println("Cheking " + totalGenes + " genes for feature diversity using genomic position exons method...");
            System.out.println("hmFeatures: " + hmFeatures);
            
            // only load full hmTransAFIdPos if necessary, use filtered one if given
            if(hmTransAFIdPosFiltered != null)
                hmTAFIdPos = hmTransAFIdPosFiltered;
            else {
                //load just features selected!!!
                loadTransAFIdGenPos();
                hmTAFIdPos = hmTransAFIdGenPos;
            }
            // genomic position, genpos, file generation
            //Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/Users/hdelrisco/Documents/pas.tsv"), "utf-8"));
            
            // check all genes
            HashMap<String, Object> hmIsos;
            for(String gene: hmGeneIsos.keySet()) {
                //System.out.println(gene);
//                if(gene.equals("Cfl2"))
//                    System.out.println(gene);
                // check if gene has more than 1 isoform, skip if not
                hmIsos = hmGeneIsos.get(gene);
                // comment out for genpos generation
                if(hmIsos.size() < 2)
                    continue;

                // check all requested features DBs
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
                HashMap<String, HashMap<String, String>> hmCats;
                HashMap<String, String> hmIds;
                for(String featureDB : hmFeatures.keySet()) {
                    // check all requested feature categories - a DB with empty hashmap means all categories
                    String type = "";
                    HashMap<String, Object> hmDBFeatures = hmFeatures.get(featureDB);
                    if(hmDBFeatures.isEmpty())
                        hmDBFeatures = project.data.getSourceFeaturesPosition(featureDB);
                    if(!hmDBFeatures.isEmpty()){
                        for(String featureCat : hmDBFeatures.keySet()) {
                            //check if feature type is protein and if the gene has more than 1
                            if(hmType.containsKey(featureDB) && hmType.get(featureDB).containsKey(featureCat)){
                                type = hmType.get(featureDB).get(featureCat);
                                if(type.equals("P"))
                                    if(project.data.getGeneProteinCount(gene) == 1)
                                        continue;
                            }
                            
                            // to calculate intersection positions
                            HashMap<String, HashMap<String, ArrayList<IdPosition>>> hmIDPositions = new HashMap<>();

                            for(String transcript : hmIsos.keySet()) {

                                // get transcript data
                                TransData td = getTransData(transcript);
                                if(td == null) {
                                    System.out.println("WARNING: No transcript data for " + transcript);
                                    continue;
                                }

                                // get all DBs for this transcript
                                hmDBs = hmTAFIdPos.get(transcript);
                                if(hmDBs != null) {
                                    // get all DBCats for this transcript
                                    hmCats = hmDBs.get(featureDB);
                                    if(hmCats != null) {
                                        // get all DBCatIDs for this transcript
                                        hmIds = hmCats.get(featureCat);
                                        if(hmIds != null) {
                                            // check all IDs for this transcript, some transcripts will have IDs that others don't
                                            for(String id : hmIds.keySet()) {
                                                String idValues = hmIds.get(id);
                                                ArrayList<IdPosition> lstPositions = TransFeatureIdValues.getPositions(idValues);
                                                //update pos by genomicpos  IdPosition idp.getGenomicPosition
                                                for(IdPosition a : lstPositions){
                                                    //lstPos = a.getGenomicExonPositions(td);
                                                    if(!featureCat.equals(POLYA_FEATURE))
                                                        a.pos = a.getGenomicPosition(td);
                                                }
                                                Collections.sort(lstPositions, new Comparator<IdPosition>() {
                                                    @Override
                                                    public int compare(IdPosition o1, IdPosition o2) {
                                                        if(o2.pos.start-o1.pos.start == 0){
                                                            return 0;
                                                        }else if(o2.pos.start-o1.pos.start > 0){
                                                            return -1;
                                                        }else{
                                                            return 1;
                                                        }
                                                    }
                                                });

                                                if(getGeneStrand(gene).equals("-")){
                                                    Collections.reverse(lstPositions);
                                                }

                                                HashMap<String, ArrayList<IdPosition>> auxID = new HashMap<>();
                                                if(hmIDPositions.containsKey(transcript))
                                                    auxID = hmIDPositions.get(transcript);
                                                
                                                auxID.put(id, lstPositions);
                                                hmIDPositions.put(transcript, auxID);
                                            }
                                        }
                                    }
                                }
                            }
                            //hashMap Trans - ArrayList(pos)

                            //return a hashmap with diverse positions (if we have the same feature_id in all transcripts in same position doesn't add it)
                            if(hmIDPositions.isEmpty())
                                continue;
                            for(String t : hmIDPositions.keySet()){
                                for(String feature_id : hmIDPositions.get(t).keySet()){
                                    ArrayList<IdPosition> lstGenomicPos = hmIDPositions.get(t).get(feature_id);
                                    for(IdPosition idp : lstGenomicPos){
                                        int start = idp.pos.start;
                                        int end = idp.pos.end;
                                        String tComplete = t; 
                                        //for each trans
                                        HashMap<String, Object> hsHasFeature = new HashMap<>();
                                        for(String tAux : hmIsos.keySet()){
                                            //if we are analysing Protein feature just search in coding transcripts
                                            if(type.equals("P") && !project.data.isTransCoding(tAux))
                                                continue;
                                            
                                            boolean found = false;
                                            //check for all transcript 

                                            //check if it is in hmIDPosition, if not 
                                            if(hmIDPositions.containsKey(tAux)){
                                                ArrayList<IdPosition> lstGenomicPosAux = hmIDPositions.get(tAux).get(feature_id);
                                                if(lstGenomicPosAux != null){
                                                    // the search can be optimice if when pos is lower or upper depending gene strand you can finish (cause is sorted)!!!
                                                    for(IdPosition idp_aux : lstGenomicPosAux){
                                                        // if same region keep the first
                                                        if((idp_aux.pos.start == start) && (end == idp_aux.pos.end)){
                                                            found = true;
                                                            break;
                                                        }
                                                        //If feature is about TranscriptsAttributes
                                                        // UTR length or polyA_Site between 100 bp
                                                        if(featureCat.equals(UTRLENGTH3_FEATURE) || featureCat.equals(UTRLENGTH5_FEATURE) || featureCat.equals(POLYA_FEATURE)){
                                                            if((start-100 <= idp_aux.pos.start && idp_aux.pos.start <= end+100) && (start-100 <= idp_aux.pos.end && idp_aux.pos.end <= end+100)){
                                                                tComplete = tAux;
                                                                start = idp_aux.pos.start;
                                                                end = idp_aux.pos.end;
                                                                found = true;
                                                                break;
                                                            }
                                                        // Other features    
                                                        //diff 3 aminoacids - 9 pb
                                                        //if ini left or ini right
                                                        //if((start-9 <= idp_aux.pos.start) && (idp_aux.pos.end <= end+9)){
                                                        //the feature have to be inside the range [star-9, end+9] 
                                                        }else{
                                                            if((start-9 <= idp_aux.pos.start && idp_aux.pos.start <= end+9) && (start-9 <= idp_aux.pos.end && idp_aux.pos.end <= end+9)){
                                                                tComplete = tAux;
                                                                start = idp_aux.pos.start;
                                                                end = idp_aux.pos.end;
                                                                found = true;
                                                                break;
                                                            }
                                                        }
                                                    } //por idp aux
                                                }
                                            }
                                            hsHasFeature.put(tAux, found);
                                        }

                                        //We need -> String feature - gene- trans . boolean
                                        String trans_res = tComplete;
                                        String strId = featureDB + "\t" + featureCat + "\t" + feature_id + "\t" + idp.posType + "_" + Integer.valueOf(start) + "-" + Integer.valueOf(end) + "_" + trans_res;
                                        String checkId = featureDB + "\t" + featureCat + "\t" + feature_id + "\t" + idp.posType + "_" + Integer.valueOf(start) + "-" + Integer.valueOf(end);
                                        
                                        //we check if that feature with that position has been added to actual gene
                                        if(hmAdded.containsKey(checkId) && hmAdded.get(checkId).containsKey(gene)){
                                            //if already diverse
                                            if(hmAdded.get(checkId).get(gene))
                                                continue;
                                            else{
                                                //if new is diverse
                                                if(hsHasFeature.containsValue(true) && hsHasFeature.containsValue(false)){
                                                    HashMap<String, HashMap<String, Object>> hsGene = new HashMap<String, HashMap<String, Object>>();
                                                    String old_key = "";
                                                    hsGene.put(gene, hsHasFeature);
                                                    for(String key : hmDiverseFeatures.keySet()){
                                                        if(key.contains(checkId)){
                                                            old_key = key;
                                                            break;
                                                        }
                                                    }
                                                    hmDiverseFeatures.remove(old_key);
                                                    hmDiverseFeatures.put(strId, hsGene);
                                                }else{
                                                    continue;
                                                }
                                            }
                                        }else{
                                            //Calculate if transcript have complete feature, partial or not
                                            HashMap<String, HashMap<String, Object>> hsGene = new HashMap<String, HashMap<String, Object>>();
                                            HashMap<String, Boolean> hsV = new HashMap<String, Boolean>();
                                            //to check
                                            if(hsHasFeature.containsValue(true) && hsHasFeature.containsValue(false))
                                                hsV.put(gene, true);
                                            else
                                                hsV.put(gene, false);
                                            hmAdded.put(checkId, hsV);
                                            //to results
                                            hsGene.put(gene, hsHasFeature);
                                            hmDiverseFeatures.put(strId, hsGene);
                                        }
                                    } //end search for positions
                                }//features_id
                            } //end search for trasncripts
                        }
                    }
                }
            }
            //genpos
            //writer.close();
        } catch(Exception e) {
            logger.logError("Feature diversity error: " + e.getMessage());
        }
        return hmDiverseFeatures;
    }
    

    // check all gene transcripts for the overlapping presence/absence of given feature id at given transcript's position
    // returns <trans, true/false> for all trans in hmIsos, true if the trans contains the given db:cat:id
    private HashMap<String, Boolean> getGeneFeatureDiversity(HashMap<String, Object> hmIsos, String db, String cat, 
                                                             String id, IdPosition idp, TransData td, boolean useExons,
                                                             HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPosFiltered) {
        HashMap<String, Boolean> hmGeneDiversity = new HashMap<>();
        for(String trans : hmIsos.keySet())
            hmGeneDiversity.put(trans, td.trans.equals(trans));
        // get genomic position, start-end, for given transcript
        SeqAlign.Position t1pos = idp.getGenomicPosition(td);
        SeqAlign.Position t2pos;
        ArrayList<SeqAlign.Position> lstPos1 = null;
        ArrayList<SeqAlign.Position> lstPos2;
        if(useExons)
            lstPos1 = idp.getGenomicExonPositions(td);
        HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
        HashMap<String, HashMap<String, String>> hmCats;
        HashMap<String, String> hmIds;
        for(String trans : hmIsos.keySet()) {
            // do not check the originating transcript
            if(!trans.equals(td.trans)) {
                TransData td2 = getTransData(trans);
                
                // get all DBs for this transcript
                hmDBs = hmTransAFIdPosFiltered.get(trans);
                if(hmDBs != null) {
                    // get all DBCats for this transcript
                    hmCats = hmDBs.get(db);
                    if(hmCats != null) {
                        // get all DBCatIDs for this transcript
                        hmIds = hmCats.get(cat);
                        if(hmIds != null && hmIds.containsKey(id)) {
                            // transcript has given id, now need to check
                            // the position all the features in this trans vs the requested one
                            String idValues = hmIds.get(id);
                            ArrayList<IdPosition> lstPositions = TransFeatureIdValues.getPositions(idValues);
                            for(IdPosition tidp : lstPositions) {
                                // check for genomic overlap ignoring exons, start-end
                                t2pos = tidp.getGenomicPosition(td2);
                                if((t2pos.start >= t1pos.start && t2pos.start <= t1pos.end) ||
                                        (t2pos.end >= t1pos.start && t2pos.end <= t1pos.end) ||
                                        (t2pos.start < t1pos.start && t2pos.end > t1pos.end)) {
                                    // check if we need to check for exon overlap
                                    if(useExons) {
                                        // get exon positions for this trans
                                        boolean fnd = false;
                                        lstPos2 = tidp.getGenomicExonPositions(td2);
                                        for(SeqAlign.Position pos2 : lstPos2) {
                                            for(SeqAlign.Position pos1 : lstPos1) {
                                                if((pos2.start >= pos2.start && pos2.start <= pos1.end) ||
                                                        (pos2.end >= pos1.start && pos2.end <= pos1.end) ||
                                                        (pos2.start < pos1.start && pos2.end > pos1.end)) {
                                                    // found an overlap, flag it as match
                                                    hmGeneDiversity.put(trans, true);
                                                    fnd = true;
                                                    break;
                                                }
                                            }
                                            // stop checking if overlap found
                                            if(fnd)
                                                break;
                                        }
                                        // stop checking this transaction if overlap found
                                        if(fnd)
                                            break;
                                    }
                                    else {
                                        // found an overlap, flag it as match and stop checking this transaction
                                        hmGeneDiversity.put(trans, true);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return hmGeneDiversity;
    }
    
    //input = feature->gene-><trans, true/false> true if it  contains feature
    public DiversityResultsInternal getDiversityDataFromHm(HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiversity, HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf){
        
        //DiversityResultsInternal Variables
        HashMap<String, HashMap<String, String>> hmType = getAFStatsType();
        DiversityResultsInternal dri = null;
        DiversityResults results = new DiversityResults();
        
        HashMap<String, HashMap<String, Boolean>> hmTotalProtein = new HashMap<>();
        HashMap<String, HashMap<String, Boolean>> hmTotalGenomic = new HashMap<>();
        HashMap<String, HashMap<String, Boolean>> hmTotalTranscript = new HashMap<>();
        
        HashMap<String, DiversityData> hmDiversityData = new HashMap<>();
        HashMap<String, DiversityData> hmDiversityPWData = new HashMap<>();
        
        for(String db : hmDiversity.keySet()){
            if(taf.abortFlag)
                    return null;
            
            String ddbb = db.split("\t")[0];
            String feat = db.split("\t")[1];
            String id = db.split("\t")[2];
            String group = hmType.get(ddbb).get(feat); //TRANS, PROTEIN, GENOMIC
            String key = ddbb + ":" + feat;
            
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group))
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group))
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group))
                type = AnnotationType.GENOMIC;
            
            //DiversityData (lstPWData)
            DiversityData dPWData = new DiversityData(key, 0, 0, 0, type);
            if(hmDiversityPWData.containsKey(key)){
                dPWData = hmDiversityPWData.get(key);
            }
            // End declaration DiversityData
            
            HashMap<String, Boolean> hmTotalGene = new HashMap<>();
            boolean diverse = false;
            for(String gene : hmDiversity.get(db).keySet()){
                diverse = false;
                boolean t = false;
                boolean f = false;
                for(int i = 0; i< hmDiversity.get(db).get(gene).keySet().size(); i++){
                    Set<String> lstIso = hmDiversity.get(db).get(gene).keySet();
                    String iso = (String)lstIso.toArray()[i];
                    if(hmDiversity.get(db).get(gene).get(iso).equals(true))
                        t = true;
                    else
                        f = true;
                    //loop to compare all PWData and create FDA graph
                    for(int j = 1+i; j< hmDiversity.get(db).get(gene).keySet().size(); j++){
                        Set<String> lstIso_aux = hmDiversity.get(db).get(gene).keySet();
                        String iso_aux = (String)lstIso_aux.toArray()[j];
//                      if(iso.equals(iso_aux))
//                            continue;
                        if(hmDiversity.get(db).get(gene).get(iso) == hmDiversity.get(db).get(gene).get(iso_aux))
                            dPWData.setSame(dPWData.getSame()+1);
                        else
                            dPWData.setDiff(dPWData.getDiff()+1);
                    }
                }                
                
                if(t && f)
                    diverse = true;
            
                switch(type){
                    case TRANS:
                        if(hmTotalTranscript.containsKey(key)){
                            HashMap<String, Boolean> hmTotalGeneAux = hmTotalTranscript.get(key);
                            //if that gene already added with true condition, keep it in true
                            if(hmTotalGeneAux.containsKey(gene) && hmTotalGeneAux.get(gene).equals(true))
                                break;
                            else{
                                hmTotalGeneAux.put(gene, diverse);
                                hmTotalTranscript.put(key, hmTotalGeneAux);
                            }
                        }
                        else{
                            hmTotalGene.put(gene, diverse);
                            hmTotalTranscript.put(key, hmTotalGene);
                        }
                        break;
                    case PROTEIN:
                        if(hmTotalProtein.containsKey(key)){
                            HashMap<String, Boolean> hmTotalGeneAux = hmTotalProtein.get(key);
                            //if that gene already added with true condition, keep it in true
                            if(hmTotalGeneAux.containsKey(gene) && hmTotalGeneAux.get(gene).equals(true))
                                break;
                            else{
                                hmTotalGeneAux.put(gene, diverse);
                                hmTotalProtein.put(key, hmTotalGeneAux);
                            }
                        }
                        else{
                            hmTotalGene.put(gene, diverse);
                            hmTotalProtein.put(key, hmTotalGene);
                        }
                        break;
                    case GENOMIC:
                        if(hmTotalGenomic.containsKey(key)){
                            HashMap<String, Boolean> hmTotalGeneAux = hmTotalGenomic.get(key);
                            //if that gene already added with true condition, keep it in true
                            if(hmTotalGeneAux.containsKey(gene) && hmTotalGeneAux.get(gene).equals(true))
                                break;
                            else{
                                hmTotalGeneAux.put(gene, diverse);
                                hmTotalGenomic.put(key, hmTotalGeneAux);
                            }
                        }
                        else{
                            hmTotalGene.put(gene, diverse);
                            hmTotalGenomic.put(key, hmTotalGene);
                        }
                        break;
                }
            }
            
            double diff_pctPW = (double)dPWData.getDiff()/(double)(dPWData.getDiff()+dPWData.getSame());
            diff_pctPW = Double.parseDouble(String.format("%.2f", ((double)Math.round(diff_pctPW*100))));
            dPWData.setDiffPCT(diff_pctPW);
            
            hmDiversityPWData.put(key, dPWData);
        }
        
        //careful to check various databases with same feature, dont count genes more than one time as varying or not
        HashMap<String, ArrayList<String>> hmGeneAnnoted = new HashMap<>();
        for(String key : hmTotalTranscript.keySet()){
            String feature = key.split(":")[1];
            String group = hmType.get(key.split(":")[0]).get(key.split(":")[1]);
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group))
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group))
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group))
                type = AnnotationType.GENOMIC;
            
            int d = 0, n = 0;
            DiversityData dData = new DiversityData(key, 0, 0, 0, type);
            for(String gene : hmTotalTranscript.get(key).keySet()){
                
                //check if gene already counted by another feature
                if(hmGeneAnnoted.containsKey(feature)){
                    ArrayList<String> genes = hmGeneAnnoted.get(feature);
                    // dont sum diff or same
                    if(genes.contains(gene))
                        continue;
                    // summ diff and same and add new gene
                    else{
                        genes.add(gene);
                        hmGeneAnnoted.put(feature, genes);
                        
                        if(hmTotalTranscript.get(key).get(gene))
                            d++;
                        else
                            n++;
                    }
                }else{
                    ArrayList<String> genes = new ArrayList<>();
                    genes.add(gene);
                    hmGeneAnnoted.put(feature, genes);
                    
                    if(hmTotalTranscript.get(key).get(gene))
                        d++;
                    else
                        n++;
                }
            }
            dData.setDiff(d);
            dData.setSame(n);
            double p = d/(double)(d+n);
            p = Double.parseDouble(String.format("%.2f", ((double)Math.round(p*100))));
            dData.setDiffPCT(p);
            
            hmDiversityData.put(key, dData);
        }
        
        //careful to check various databases with same feature, dont count genes more than one time as varying or not
        hmGeneAnnoted = new HashMap<>();
        for(String key : hmTotalProtein.keySet()){
            String feature = key.split(":")[1];
            String group = hmType.get(key.split(":")[0]).get(key.split(":")[1]);
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group))
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group))
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group))
                type = AnnotationType.GENOMIC;
            
            int d = 0, n = 0;
            DiversityData dData = new DiversityData(key, 0, 0, 0, type);
            for(String gene : hmTotalProtein.get(key).keySet()){
                //check if gene already counted by another feature
                if(hmGeneAnnoted.containsKey(feature)){
                    ArrayList<String> genes = hmGeneAnnoted.get(feature);
                    // dont sum diff or same
                    if(genes.contains(gene))
                        continue;
                    // summ diff and same and add new gene
                    else{
                        genes.add(gene);
                        hmGeneAnnoted.put(feature, genes);
                        
                        if(hmTotalProtein.get(key).get(gene))
                            d++;
                        else
                            n++;
                    }
                }else{
                    ArrayList<String> genes = new ArrayList<>();
                    genes.add(gene);
                    hmGeneAnnoted.put(feature, genes);
                    
                    if(hmTotalProtein.get(key).get(gene))
                        d++;
                    else
                        n++;
                }
            }
            dData.setDiff(d);
            dData.setSame(n);
            double p = d/(double)(d+n);
            p = Double.parseDouble(String.format("%.2f", ((double)Math.round(p*100))));
            dData.setDiffPCT(p);
            
            hmDiversityData.put(key, dData);
        }
        
        //careful to check various databases with same feature, dont count genes more than one time as varying or not
        hmGeneAnnoted = new HashMap<>();
        for(String key : hmTotalGenomic.keySet()){
            String feature = key.split(":")[1];
            String group = hmType.get(key.split(":")[0]).get(key.split(":")[1]);
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group))
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group))
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group))
                type = AnnotationType.GENOMIC;
            
            int d = 0, n = 0;
            DiversityData dData = new DiversityData(key, 0, 0, 0, type);
            for(String gene : hmTotalGenomic.get(key).keySet()){
                //check if gene already counted by another feature
                if(hmGeneAnnoted.containsKey(feature)){
                    ArrayList<String> genes = hmGeneAnnoted.get(feature);
                    // dont sum diff or same
                    if(genes.contains(gene))
                        continue;
                    // summ diff and same and add new gene
                    else{
                        genes.add(gene);
                        hmGeneAnnoted.put(feature, genes);
                        
                        if(hmTotalGenomic.get(key).get(gene))
                            d++;
                        else
                            n++;
                    }
                }else{
                    ArrayList<String> genes = new ArrayList<>();
                    genes.add(gene);
                    hmGeneAnnoted.put(feature, genes);
                    
                    if(hmTotalGenomic.get(key).get(gene))
                        d++;
                    else
                        n++;
                }
            }
            dData.setDiff(d);
            dData.setSame(n);
            double p = d/(double)(d+n);
            p = Double.parseDouble(String.format("%.2f", ((double)Math.round(p*100))));
            dData.setDiffPCT(p);
            
            hmDiversityData.put(key, dData);
        }
        
        for(String i : hmDiversityData.keySet()){
            results.lstData.add(hmDiversityData.get(i));
            results.lstPWData.add(hmDiversityPWData.get(i));
        }
        
        try {
            HashMap<String, Boolean> hmProteinGV = getVaryingGenes(hmTotalProtein);
            HashMap<String, Boolean> hmGenomicGV = getVaryingGenes(hmTotalGenomic);
            HashMap<String, Boolean> hmTransGV = getVaryingGenes(hmTotalTranscript);
            HashMap<String, Boolean> hmResult = new HashMap<>();
            
            for(String gene : hmProteinGV.keySet()) {
                if(!hmResult.containsKey(gene))
                    hmResult.put(gene, hmProteinGV.get(gene));
                else if(hmProteinGV.get(gene))
                    hmResult.put(gene, true);
            }
            for(String gene : hmGenomicGV.keySet()) {
                if(!hmResult.containsKey(gene))
                    hmResult.put(gene, hmGenomicGV.get(gene));
                else if(hmGenomicGV.get(gene))
                    hmResult.put(gene, true);
            }
            for(String gene : hmTransGV.keySet()) {
                if(!hmResult.containsKey(gene))
                    hmResult.put(gene, hmTransGV.get(gene));
                else if(hmTransGV.get(gene))
                    hmResult.put(gene, true);
            }
            
            DiversityRegionStats drProt = getDiversityRegionStats(hmProteinGV);
            DiversityRegionStats drGeno = getDiversityRegionStats(hmGenomicGV);
            DiversityRegionStats drTrans = getDiversityRegionStats(hmTransGV);
            DiversityRegionStats drTotal = getDiversityRegionStats(hmResult);
            System.out.println("hmResult size: " + hmResult.size() + ", drTotal cnt: " + drTotal.cnt + ", protg: " + hmTotalProtein.size() + ", transg: " + hmTotalTranscript.size() + ", genog: " + hmTotalGenomic.size());

            results.drProtein = drProt;
            results.drGenomic = drGeno;
            results.drTranscript = drTrans;
            results.drTotal = drTotal;

            // save results in return structure
            dri = new DiversityResultsInternal();
            dri.fdr = results;
            dri.hmProtein = hmTotalProtein;
            dri.hmGenomic = hmTotalGenomic;
            dri.hmTranscript = hmTotalTranscript;

        }catch(Exception e) {
            dri = null;
            logger.logError("Unable to generate diversity data: " + e.getMessage());
        }
        return dri;
    }
    
    //input = feature->gene-><trans, true/false> true if it  contains feature
    public HashMap<String, HashMap<String, Boolean>> getDiversityDataFromHmID(HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiversity, HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf, boolean genpos){
        
        //DiversityResultsInternal Variables
        //HashMap<String, HashMap<String, String>> hmType = getAFStatsType();
        HashMap<String, HashMap<String, Boolean>> hmResult = new HashMap<>();
        try{
            //We use using=presence so we dont have position column
            if(!genpos){
                for(String db : hmDiversity.keySet()){ //ID

                    HashMap<String, Boolean> hmGene = new HashMap<>();
                    hmResult.put(db, hmGene);

                    if(taf.abortFlag)
                            return null;

                    boolean diverse = false;
                    for(String gene : hmDiversity.get(db).keySet()){
                        diverse = false;
                        boolean t = false;
                        boolean f = false;
                        for(String iso : hmDiversity.get(db).get(gene).keySet()){
                            if(hmDiversity.get(db).get(gene).get(iso).equals(true))
                                t = true;
                            else
                                f = true;
                            if(t && f){
                                diverse = true;
                                break;
                            }
                        }

                        hmGene.put(gene, diverse);
                    }
                }
            }else{
            //we use genpos and we have to treat the position and convert to general method
                for(String db : hmDiversity.keySet()){ //ID
                    HashMap<String, Boolean> hmGene = new HashMap<>();
                    
                    String database = db.split("\t")[0];
                    String feat = db.split("\t")[1];
                    String id = db.split("\t")[2];
                    String name = database + "\t" + feat + "\t" + id;
                    
                    //if firts time put the hashname and get the first gene
                    if(!hmResult.containsKey(name)){
                        hmResult.put(name, hmGene);
                    //if already exist get all the genes and add a new one
                    }else{
                        hmGene = hmResult.get(name);
                    }
                    if(taf.abortFlag)
                        return null;

                    boolean diverse = false;
                    for(String gene : hmDiversity.get(db).keySet()){
                        diverse = false;
                        boolean t = false;
                        boolean f = false;
                        for(String iso : hmDiversity.get(db).get(gene).keySet()){
                            if(hmDiversity.get(db).get(gene).get(iso).equals(true))
                                t = true;
                            else
                                f = true;
                            if(t && f){
                                diverse = true;
                                break;
                            }
                        }
                        //if already has this gene only change if now the diverse is true and before false
                        if(hmGene.containsKey(gene)){
                            if(!hmGene.get(gene) && diverse)
                                hmGene.put(gene, diverse);
                        }else{
                            hmGene.put(gene, diverse);
                        }
                    }
                }
            }
        }catch(Exception e) {
            hmResult = null;
            logger.logError("Unable to generate diversity data: " + e.getMessage());
        }
        return hmResult;
    } 
    
    // Diversity data is divided into Transcript, Protein, and Genomic based on the section of the annotation itself
    public DiversityResultsInternal genAnnotationDiversityData(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans, 
        HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf) {
        
        HashMap<String, String> genomicPos = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);
        DiversityResultsInternal dri = null;
        DiversityResults results = new DiversityResults();
        try {
            int totalGenes = hmFilterGeneTrans.size();
            System.out.println("Processing diversity data for " + totalGenes + " genes...");
            System.out.println("hmTerms: " + hmTerms);
            logger.logInfo("Retrieving annotation data for " + totalGenes + " genes....");
            for(String db : hmTerms.keySet())
                System.out.println("Diversity source: " + db);
            if(taf.abortFlag)
                return null;

            HashMap<String, ArrayList<TransDivData>> hmDD = new HashMap<>();
            HashMap<String, Object> hmIsos;
            ArrayList<String> lstIsos;
            for(String gene: hmFilterGeneTrans.keySet()) {
                if(taf.abortFlag)
                    return null;

                //cnt++;
                hmIsos = hmFilterGeneTrans.get(gene);
                lstIsos = new ArrayList<>();
                //get trasncripts by gen
                for(String iso : hmIsos.keySet())
                    lstIsos.add(iso);

                TransDivData tdd = null;
                ArrayList<TransDivData> lstDD = new ArrayList<>();
                hmDD.put(gene, lstDD);
                ArrayList<AnnotationDataEntry> lstTad = getTransAnnotationData(gene, lstIsos);
                AnnotationType type = AnnotationType.TRANS;
                for(AnnotationDataEntry tad : lstTad) {
                    if(taf.abortFlag)
                        return null;
                    if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.TRANSCRIPT)) {
                        type = AnnotationType.TRANS;
                        tdd = new TransDivData(tad, project.data.isTransCoding(tad.transID));
                        lstDD.add(tdd);
                    }
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.PROTEIN))
                        type = AnnotationType.PROTEIN;
                    else if(getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.GENOMIC))
                        type = AnnotationType.GENOMIC;
                    else if(tdd != null) {
                        if(type.equals(AnnotationType.TRANS) && getRsvdAnnotFeature(tad.db, tad.category).equals(AnnotFeature.CDS)) {
                            tdd.cdsStart = tad.start;
                            tdd.cdsEnd = tad.end;
                            tdd.cdsLength = tad.end - tad.start + 1;
                            if(tad.start > 1) {
                                tdd.utr5Start = 1;
                                tdd.utr5End = tad.start - 1;
                            }
                            tdd.utr5Length = tad.start - 1;
                            if(tad.end < tdd.tad.end) {
                                tdd.utr3Start = tad.end + 1;
                                tdd.utr3End = tdd.tad.end;
                            }
                            tdd.utr3Length = tdd.tad.end - tad.end;
                            if(genomicPos.containsKey(tad.transID)){
                                tdd.polyA_site = Integer.parseInt(genomicPos.get(tad.transID));
                            }
                        }
                        else if(!isRsvdFeature(tad.db, tad.category)) {
                            boolean countonly = true;
                            String id = "";
                            HashMap<String, String> attrs = getAttributes(tad.attrs);
                            if(attrs.containsKey(fileDefs.attrId)) {
                                countonly = false;
                                id = attrs.get(fileDefs.attrId);
                            }

                            // We do not include entries where the DB or Category is not found (vs include with count of 0)
                            // by not doing so the genes where none of the isoforms have the db/cat are not included
                            if(hmTerms.containsKey(tad.db)) {
                                HashMap<String, Object> hmcats = hmTerms.get(tad.db);
                                if(hmcats.isEmpty() || hmcats.containsKey(tad.category)) {
                                    String key = tad.db + ":" + tad.category;
                                    HashMap<String, Object> hmKeyList = tdd.hmTransKeyList;
                                    HashMap<String, Integer> hmdb = tdd.hmTransDB;
                                    HashMap<String, HashMap<String, Integer>> hmxdb = tdd.hmXTransDB;
                                    // check if protein or genomic entry
                                    if(type.equals(AnnotationType.PROTEIN)) {
                                        hmKeyList = tdd.hmProteinKeyList;
                                        hmdb = tdd.hmProteinDB;
                                        hmxdb = tdd.hmXProteinDB;
                                    }
                                    else if(type.equals(AnnotationType.GENOMIC)) {
                                        hmKeyList = tdd.hmGenomicKeyList;
                                        hmdb = tdd.hmGenomicDB;
                                        hmxdb = tdd.hmXGenomicDB;
                                    }
                                    // process based on count only or id and count
                                    if(countonly) {
                                        if(!hmKeyList.containsKey(key))
                                            hmKeyList.put(key, null);
                                        if(!hmdb.containsKey(key))
                                            hmdb.put(key, 1);
                                        else
                                            hmdb.put(key, (hmdb.get(key) + 1));
                                    }
                                    else {
                                        if(!hmKeyList.containsKey(key))
                                            hmKeyList.put(key, null);
                                        if(!hmxdb.containsKey(key))
                                            hmxdb.put(key, new HashMap<>());
                                        HashMap<String, Integer> hmid = hmxdb.get(key);
                                        if(!hmid.containsKey(id))
                                            hmid.put(id, 1);
                                        else
                                            hmid.put(id, (hmid.get(id) + 1));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            logger.logInfo("Processed " + hmFilterGeneTrans.size() + " genes.");
            logger.logInfo("Calculating functional diversity...");

            int same, diff;
            boolean sameflg, varyingflg;
            double pct;
            HashMap<String, HashMap<String, Boolean>> hmTotalProtein = new HashMap<>();
            HashMap<String, HashMap<String, Boolean>> hmTotalGenomic = new HashMap<>();
            HashMap<String, HashMap<String, Boolean>> hmTotalTranscript = new HashMap<>();
            HashMap<String, HashMap<String, AFStatsData>> hmStats = project.data.getAFStats();
            try {
                for(String db : hmTerms.keySet()) {
                    if(taf.abortFlag)
                        return null;
                    switch (db) {
                        case STRUCTURAL_SOURCE:
                            {
                                HashMap<String, Object> hmFeatures = hmTerms.get(db);
                                if(hmFeatures.isEmpty()) {
                                    if(hmStats.containsKey(db)) {
                                        HashMap<String, AFStatsData> hm = hmStats.get(db);
                                        for(String cat : hm.keySet())
                                            hmFeatures.put(cat, null);
                                    }
                                }   
                                
                                for(String feature : hmFeatures.keySet()) {
                                    switch (feature) {
                                        case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                                        {
                                            DiversityData dd = getDiversityLength(DlgFDAnalysis.Params.FEATURE_CDS_LENGTH, hmDD, hmTotalTranscript);
                                            results.lstData.add(dd);
                                            dd = getPWDiversityLength(DlgFDAnalysis.Params.FEATURE_CDS_LENGTH, hmDD);
                                            results.lstPWData.add(dd);
                                            break;
                                        }
                                        case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                                        {
                                            DiversityData dd = getDiversityLength(DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH, hmDD, hmTotalTranscript);
                                            results.lstData.add(dd);
                                            dd = getPWDiversityLength(DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH, hmDD);
                                            results.lstPWData.add(dd);
                                            break;
                                        }
                                        case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                                        {
                                            DiversityData dd = getDiversityLength(DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH, hmDD, hmTotalTranscript);
                                            results.lstData.add(dd);
                                            dd = getPWDiversityLength(DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH, hmDD);
                                            results.lstPWData.add(dd);
                                            break;
                                        }
                                        case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                                        {
                                            DiversityData dd = getDiversityLength(DlgFDAnalysis.Params.FEATURE_PAS_POSITION, hmDD, hmTotalTranscript);
                                            results.lstData.add(dd);
                                            dd = getPWDiversityLength(DlgFDAnalysis.Params.FEATURE_PAS_POSITION, hmDD);
                                            results.lstPWData.add(dd);
                                            break;
                                        }
                                    }
                                } 
                                break;
                            }
                        default:
                        {
                            HashMap<String, Object> hmFeatures = hmTerms.get(db);
                            if(hmFeatures.isEmpty()) {
                                if(hmStats.containsKey(db)) {
                                    HashMap<String, AFStatsData> hm = hmStats.get(db);
                                    for(String cat : hm.keySet())
                                        hmFeatures.put(cat, null);
                                }
                            }
                            // process features at the pairwise isoform level
                            results.lstPWData.addAll(getPWFeatureDiversity(db, hmFeatures, hmDD));
                            // process features at the gene level
                            
                            HashMap<String, ArrayList<String>> hmGeneAnnoted = new HashMap<>();
                            for(String feature : hmFeatures.keySet()) {
                                String key = db + ":" + feature;
                                System.out.println("Calculating diversity for '" + key + "'");
                                AnnotationType[] types = { AnnotationType.PROTEIN, AnnotationType.TRANS, AnnotationType.GENOMIC };
                                for(AnnotationType type : types) {
                                    same = diff = 0;
                                    HashMap<String, Object> hmKeyList;
                                    HashMap<String, Integer> hmdb, hmdbCmp;
                                    HashMap<String, HashMap<String, Integer>> hmxdb, hmxdbCmp;
                                    HashMap<String, Boolean> hmTotalGene = new HashMap<>();
                                    for(String gene : hmDD.keySet()) {
                                        
                                        ArrayList<TransDivData> lstdd = hmDD.get(gene);
                                        hmdbCmp = null;
                                        hmxdbCmp = null;
                                        sameflg = true;
                                        int transcnt = 0;
                                        boolean haskey = false;
                                        // process all gene transcripts
                                        for(TransDivData tdd : lstdd) {
                                            switch(type) {
                                                case PROTEIN:
                                                    hmKeyList = tdd.hmProteinKeyList;
                                                    hmdb = tdd.hmProteinDB;
                                                    hmxdb = tdd.hmXProteinDB;
                                                    break;
                                                case GENOMIC:
                                                    hmKeyList = tdd.hmGenomicKeyList;
                                                    hmdb = tdd.hmGenomicDB;
                                                    hmxdb = tdd.hmXGenomicDB;
                                                    break;
                                                default:
                                                    hmKeyList = tdd.hmTransKeyList;
                                                    hmdb = tdd.hmTransDB;
                                                    hmxdb = tdd.hmXTransDB;
                                                    break;
                                            }
                                            // check if this transcript contains the current database being processed
                                            // if contained in hmDBList, it can be in either hmdb or hmxdb or both
                                            if(hmKeyList.containsKey(key)) {
                                                if(!haskey) {
                                                    haskey = true;
                                                    if(transcnt > 0) {
                                                        sameflg = false;
                                                        break;
                                                    }
                                                }
                                                // set initial reference
                                                if(hmdbCmp == null) {
                                                    hmdbCmp = hmdb;
                                                    hmxdbCmp = hmxdb;
                                                }
                                                else {
                                                    // check for key mismatch
                                                    if(hmdbCmp.containsKey(key) != hmdb.containsKey(key)) {
                                                        sameflg = false;
                                                        break;
                                                    }
                                                    // skip over if neither has it
                                                    if(hmdbCmp.containsKey(key)) {
                                                        // check for same counts
                                                        if(hmdbCmp.get(key) != hmdb.get(key)) {
                                                            sameflg = false;
                                                            break;
                                                        }
                                                    }

                                                    // check for database mismatch
                                                    if(hmxdbCmp.containsKey(key) != hmxdb.containsKey(key)) {
                                                        sameflg = false;
                                                        break;
                                                    }

                                                    // skip over if neither has it
                                                    if(hmxdbCmp.containsKey(key)) {
                                                        // check for matching id and counts
                                                        HashMap<String, Integer> hmxCmp1 = hmxdbCmp.get(key);
                                                        HashMap<String, Integer> hmxCmp2 = hmxdb.get(key);

                                                        // check if both have the same number of ids
                                                        if(hmxCmp1.size() != hmxCmp2.size()) {
                                                            sameflg = false;
                                                            break;
                                                        }
                                                        else {
                                                            // check all ids
                                                            for(String id : hmxCmp1.keySet()) {
                                                                if(!hmxCmp2.containsKey(id) || !hmxCmp1.get(id).equals(hmxCmp2.get(id))) {
                                                                    sameflg = false;
                                                                    break;
                                                                }
                                                            }
                                                            if(!sameflg)
                                                                break;
                                                        }
                                                    }
                                                }
                                            }
                                            else if(haskey) {
                                                sameflg = false;
                                                break;
                                            }
                                            transcnt++;
                                        }
                                        // don't update counts unless at least one transcript contained the DB
                                        // it doesn't make sense to include entries not containing the value
                                        if(haskey) {
                                            
                                            // check if gene is already annoted to this feature by another database // for example: multiples DOMAINS sources
                                            if(hmGeneAnnoted.containsKey(feature)){
                                                ArrayList<String> genes = hmGeneAnnoted.get(feature);
                                                // dont sum diff or same
                                                if(genes.contains(gene)){
                                                    varyingflg = true;
                                                    if(sameflg) {
                                                        varyingflg = false;
                                                    }
                                                    
                                                    hmTotalGene.put(gene, varyingflg);
                                                    switch(type) {
                                                        case PROTEIN:
                                                            hmTotalProtein.put(key, hmTotalGene);
                                                            break;
                                                        case GENOMIC:
                                                            hmTotalGenomic.put(key, hmTotalGene);
                                                            break;
                                                        default:
                                                            hmTotalTranscript.put(key, hmTotalGene);
                                                            break;
                                                    }
                                                    continue;
                                                //sum diff or same
                                                }else{
                                                    genes.add(gene);
                                                    hmGeneAnnoted.put(feature, genes);
                                                    
                                                    varyingflg = true;
                                                    if(sameflg) {
                                                        same++;
                                                        varyingflg = false;
                                                    }
                                                    else
                                                        diff++;
                                                    
                                                    hmTotalGene.put(gene, varyingflg);
                                                    switch(type) {
                                                        case PROTEIN:
                                                            hmTotalProtein.put(key, hmTotalGene);
                                                            break;
                                                        case GENOMIC:
                                                            hmTotalGenomic.put(key, hmTotalGene);
                                                            break;
                                                        default:
                                                            hmTotalTranscript.put(key, hmTotalGene);
                                                            break;
                                                    }
                                                }
                                            // sum diff and same and add feature to hm
                                            }else{
                                                ArrayList<String> genes = new ArrayList<>();
                                                genes.add(gene);
                                                hmGeneAnnoted.put(feature, genes);
                                                
                                                varyingflg = true;
                                                if(sameflg) {
                                                    same++;
                                                    varyingflg = false;
                                                }
                                                else
                                                    diff++;

                                                hmTotalGene.put(gene, varyingflg);
                                                switch(type) {
                                                    case PROTEIN:
                                                        hmTotalProtein.put(key, hmTotalGene);
                                                        break;
                                                    case GENOMIC:
                                                        hmTotalGenomic.put(key, hmTotalGene);
                                                        break;
                                                    default:
                                                        hmTotalTranscript.put(key, hmTotalGene);
                                                        break;
                                                }
                                            }
                                        }
                                    }

                                    // only write features with processed data
                                    if((same + diff) > 0) {
                                        pct = (double)diff / (same + diff) * 100;
                                        pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                                        DiversityData dd = new DiversityData(key, same, diff, pct, type);
                                        results.lstData.add(dd);
                                    }
                                }
                            }   break;
                        } //default case
                    }
                }
                System.out.println("Protein DBs: " + hmTotalProtein.size() + ", Transcript DBs: " + hmTotalTranscript.size());
                logger.logInfo("Formatting results...");

                // hmTotalCoding <term, <gene, varyingFlg>>
                HashMap<String, Boolean> hmProteinGV = getVaryingGenes(hmTotalProtein);
                HashMap<String, Boolean> hmGenomicGV = getVaryingGenes(hmTotalGenomic);
                HashMap<String, Boolean> hmTransGV = getVaryingGenes(hmTotalTranscript);
                HashMap<String, Boolean> hmResult = new HashMap<>();
                for(String gene : hmProteinGV.keySet()) {
                    if(!hmResult.containsKey(gene))
                        hmResult.put(gene, hmProteinGV.get(gene));
                    else if(hmProteinGV.get(gene))
                        hmResult.put(gene, true);
                }
                for(String gene : hmGenomicGV.keySet()) {
                    if(!hmResult.containsKey(gene))
                        hmResult.put(gene, hmGenomicGV.get(gene));
                    else if(hmGenomicGV.get(gene))
                        hmResult.put(gene, true);
                }
                for(String gene : hmTransGV.keySet()) {
                    if(!hmResult.containsKey(gene))
                        hmResult.put(gene, hmTransGV.get(gene));
                    else if(hmTransGV.get(gene))
                        hmResult.put(gene, true);
                }
                DiversityRegionStats drProt = getDiversityRegionStats(hmProteinGV);
                DiversityRegionStats drGeno = getDiversityRegionStats(hmGenomicGV);
                DiversityRegionStats drTrans = getDiversityRegionStats(hmTransGV);
                DiversityRegionStats drTotal = getDiversityRegionStats(hmResult);
                System.out.println("hmResult size: " + hmResult.size() + ", drTotal cnt: " + drTotal.cnt + ", protg: " + hmTotalProtein.size() + ", transg: " + hmTotalTranscript.size() + ", genog: " + hmTotalGenomic.size());

                results.drProtein = drProt;
                results.drGenomic = drGeno;
                results.drTranscript = drTrans;
                results.drTotal = drTotal;

                // save results in return structure
                dri = new DiversityResultsInternal();
                dri.fdr = results;
                dri.hmProtein = hmTotalProtein;
                dri.hmGenomic = hmTotalGenomic;
                dri.hmTranscript = hmTotalTranscript;
            }
            catch(Exception e) {
                dri = null;
                logger.logError("Unable to generate diversity data: " + e.getMessage());
            }
        } catch(Exception e) {
            logger.logError("Functional Diversity Analysis error: " + e.getMessage());
        }
        return dri;
    }
    private ArrayList<DiversityData> getPWFeatureDiversity(String source, HashMap<String, Object> hmFeatures, HashMap<String, ArrayList<TransDivData>> hmGenes) {
        ArrayList<DiversityData> lstPWData = new ArrayList<>();
        
        // process at the feature level
        for(String feature : hmFeatures.keySet()) {
            int same, diff;
            String key = source + ":" + feature;
            System.out.println("Calculating diversity for '" + key + "'");
            AnnotationType[] types = { AnnotationType.PROTEIN, AnnotationType.TRANS, AnnotationType.GENOMIC };
            for(AnnotationType type : types) {
                same = diff = 0;
                HashMap<String, Object> hmKeyList, hmKeyListCmp;
                HashMap<String, Integer> hmdb, hmdbCmp;
                HashMap<String, HashMap<String, Integer>> hmxdb, hmxdbCmp;
                for(String gene : hmGenes.keySet()) {
                    // get all transcripts for this gene
                    ArrayList<TransDivData> lstdd = hmGenes.get(gene);
                    
                    // check if gene has feature, skip whole gene if not
                    boolean haskey = false;
                    
                    //for each transcript
                    for(TransDivData tdv : lstdd) {
                        switch(type) {
                            case PROTEIN:
                                hmKeyList = tdv.hmProteinKeyList;
                                break;
                            case GENOMIC:
                                hmKeyList = tdv.hmGenomicKeyList;
                                break;
                            default:
                                hmKeyList = tdv.hmTransKeyList;
                                break;
                        }
                        if(hmKeyList.containsKey(key)) {
                            haskey = true;
                            break;
                        }
                    }
                    if(!haskey)
                        continue;
                    
                    // at least one isoform has the feature, compare all isoforms regardless
                    for(int idx = 0; idx < (lstdd.size() - 1); idx++) {
                        // get reference length to compare against
                        TransDivData tdd = lstdd.get(idx);
                        switch(type) {
                            case PROTEIN:
                                hmKeyList = tdd.hmProteinKeyList;
                                hmdb = tdd.hmProteinDB;
                                hmxdb = tdd.hmXProteinDB;
                                break;
                            case GENOMIC:
                                hmKeyList = tdd.hmGenomicKeyList;
                                hmdb = tdd.hmGenomicDB;
                                hmxdb = tdd.hmXGenomicDB;
                                break;
                            default:
                                hmKeyList = tdd.hmTransKeyList;
                                hmdb = tdd.hmTransDB;
                                hmxdb = tdd.hmXTransDB;
                                break;
                        }

                        // compare rest of transactions to reference
                        for(int cmpidx = idx+1; cmpidx < lstdd.size(); cmpidx++) {
                            TransDivData tddcmp = lstdd.get(cmpidx);
                            switch(type) {
                                case PROTEIN:
                                    hmKeyListCmp = tddcmp.hmProteinKeyList;
                                    hmdbCmp = tddcmp.hmProteinDB;
                                    hmxdbCmp = tddcmp.hmXProteinDB;
                                    break;
                                case GENOMIC:
                                    hmKeyListCmp = tddcmp.hmGenomicKeyList;
                                    hmdbCmp = tddcmp.hmGenomicDB;
                                    hmxdbCmp = tddcmp.hmXGenomicDB;
                                    break;
                                default:
                                    hmKeyListCmp = tddcmp.hmTransKeyList;
                                    hmdbCmp = tddcmp.hmTransDB;
                                    hmxdbCmp = tddcmp.hmXTransDB;
                                    break;
                            }
                            
                            // check if either one contains the term
                            if(hmKeyList.containsKey(key) || hmKeyListCmp.containsKey(key)) {
                                // check for term mismatch - one has and the other one doesn't
                                if(hmKeyList.containsKey(key) != hmKeyListCmp.containsKey(key))
                                    diff++;
                                else {
                                    // at this point they both have the term - find out where
                                    if(hmdb.containsKey(key) && hmdbCmp.containsKey(key)) {
                                        // check for same counts - not using Ids
                                        if(hmdb.get(key).equals(hmdbCmp.get(key)))
                                            same++;
                                        else
                                            diff++;
                                    }
                                    else {
                                        // confirm it's here, otherwise treat as different - should never happen
                                        if(hmxdb.containsKey(key) && hmxdbCmp.containsKey(key)) {
                                            // check for matching id and counts
                                            HashMap<String, Integer> hmxCmp1 = hmxdbCmp.get(key);
                                            HashMap<String, Integer> hmxCmp2 = hmxdb.get(key);

                                            // check if both have the same number of ids
                                            if(hmxCmp1.size() != hmxCmp2.size())
                                                diff++;
                                            else {
                                                // check all id counts
                                                boolean sameflg = true;
                                                for(String id : hmxCmp1.keySet()) {
                                                    if(!hmxCmp2.containsKey(id) || !hmxCmp1.get(id).equals(hmxCmp2.get(id))) {
                                                        sameflg = false;
                                                        diff++;
                                                        break;
                                                    }
                                                }
                                                if(sameflg)
                                                    same++;
                                            }
                                        }
                                        else
                                            diff++;
                                    }
                                }
                            }
                            else
                                same++;
                        }
                    }
                }

                // only write features with processed data
                if((same + diff) > 0) {
                    double pct = (double)diff / (same + diff) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    DiversityData dd = new DiversityData(key, same, diff, pct, type);
                    lstPWData.add(dd);
                }
            }
        }
        return lstPWData;
    }
    private HashMap<String, Boolean> getVaryingGenes(HashMap<String, HashMap<String, Boolean>> hmData) {
        HashMap<String, Boolean> hmResult = new HashMap<>();
        for(HashMap<String, Boolean> hm : hmData.values()) {
            for(String gene : hm.keySet()) {
                if(!hmResult.containsKey(gene))
                    hmResult.put(gene, hm.get(gene));
                else if(hm.get(gene))
                    hmResult.put(gene, true);
            }
        }
        return hmResult;
    }
    public DiversityRegionStats getDiversityRegionStats(HashMap<String, Boolean> hm) {
        DiversityRegionStats dv = new DiversityRegionStats();
        dv.cnt = hm.size();
        dv.vcnt = 0;
        for(Boolean vflg : hm.values()) {
            if(vflg)
                dv.vcnt++;
        }
        dv.vpct = 0.0;
        if(dv.cnt > 0) {
            dv.vpct = (double)dv.vcnt / dv.cnt * 100;
            dv.vpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(dv.vpct*100)/100.0)));
        }
        System.out.println("genes: " + dv.cnt + ", varying: " + dv.vcnt + ", pct: " + dv.vpct);
        return dv;
    }
    
    // get diversity length for CDS, 5'UTR, or 3'UTR - all are in the Transcript section
    // all these values will be consider varying if any one of the transcripts does not have it and others do
    // for example if the 5'UTR for transcript is 230 for all except one that has no 5'UTR then it is varying
    private DiversityData getDiversityLength(String term, HashMap<String, ArrayList<TransDivData>> hmGenes, 
            HashMap<String, HashMap<String, Boolean>> hmTotalTranscript) throws Exception {
        HashMap<String, String> genomicPos_3UTR = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),1,true);
        HashMap<String, String> genomicPos_5UTR = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),2,true);
        HashMap<String, String> genomicPos_CDS = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),3,true);
        HashMap<String, String> genomicPos_PAS = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);

        boolean sameflg, varyingflg;
        double pct;
        int same = 0;
        int diff = 0;
        int len, tdlen;
        String key = STRUCTURAL_SOURCE + ":" + term;
        HashMap<String, Boolean> hmTotalGene = new HashMap<>();
        for(String gene : hmGenes.keySet()) {
            // check if gene at least one isoform is protein coding, skip whole gene if not
            ArrayList<TransDivData> lstdd = hmGenes.get(gene);
            boolean coding = false;
            for(TransDivData tdv : lstdd) {
                if(tdv.coding) {
                    coding = true;
                    break;
                }
            }
            if(!coding)
                continue;

            len = -1;
            sameflg = true;
            int maxDiff = 0;
            for(TransDivData tdd : lstdd) {
                switch(term) {
                    case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                        tdlen = tdd.cdsLength;
                        maxDiff = 0;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                        tdlen = tdd.utr5Length;
                        maxDiff = MAX_UTR_LENGTH_DIFF;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                        tdlen = tdd.utr3Length;
                        maxDiff = MAX_UTR_LENGTH_DIFF;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                        tdlen = tdd.polyA_site;
                        maxDiff = MAX_UTR_LENGTH_DIFF;
                        break;
                    default:
                        throw new Exception("Invalid diversity length term: " + term);
                }

                // set initial reference value to compare against
                if(len == -1)
                    len = tdlen;
                else {
                    // first do a quick check for the actual sizes
                    // if they are different, there is no need to compare exons
                    if(Math.abs(len - tdlen) > maxDiff) {
                        //sameflg = false;
                        break;
                    }
                }
            }
            varyingflg = true;
            if(sameflg) {
                HashMap<String, Object> hm = new HashMap<>();
                for(TransDivData tdd : lstdd)
                    hm.put(tdd.tad.transID, null);
                switch(term) {
                    case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                        varyingflg = !areAllGeneUTRsSimilar(gene, hm, genomicPos_5UTR);
                        break;
                    case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                        varyingflg = !areAllGeneUTRsSimilar(gene, hm, genomicPos_3UTR);
                        break;
                    case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                        varyingflg = !areAllGenePASSimilar(gene, hm, genomicPos_PAS);
                        break;
                    case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                        varyingflg = !areAllGeneCDSIdentical(gene, hm, genomicPos_CDS);
                        break;
                }
                if(varyingflg)
                    diff++;
                else
                    same++;
            }
            else
                diff++;
            hmTotalGene.put(gene, varyingflg);
            hmTotalTranscript.put(key, hmTotalGene);
        }
        if((same + diff) > 0) {
            pct = (double)diff / (same + diff) * 100;
            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
        }
        else {
            pct = 0.0;
        }
        DiversityData dd = new DiversityData(key, same, diff, pct, AnnotationType.TRANS);
        return dd;
    }
    private DiversityData getPWDiversityLength(String term, HashMap<String, ArrayList<TransDivData>> hmGenes) throws Exception {
        HashMap<String, String> genomicPos_3UTR = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),1,true);
        HashMap<String, String> genomicPos_5UTR = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),2,true);
        HashMap<String, String> genomicPos_CDS = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),3,true);        
        HashMap<String, String> genomicPos_PAS = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),4,true);

        boolean varyingflg;
        double pct;
        int same = 0;
        int diff = 0;
        int tdlen = 0;
        int tdcmplen = 0;
        String key = STRUCTURAL_SOURCE + ":" + term;
        for(String gene : hmGenes.keySet()) {
            // check if gene at least one isoform is protein coding, skip whole gene if not
            ArrayList<TransDivData> lstdd = hmGenes.get(gene);
            boolean coding = false;
            for(TransDivData tdv : lstdd) {
                if(tdv.coding) {
                    coding = true;
                    break;
                }
            }
            if(!coding)
                continue;
            
            int maxDiff = 0;
            switch(term) {
                case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                    maxDiff = 0;
                    break;
                case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                    maxDiff = MAX_UTR_LENGTH_DIFF;
                    break;
                case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                    maxDiff = MAX_UTR_LENGTH_DIFF;
                    break;
                case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                    maxDiff = MAX_UTR_LENGTH_DIFF;
                    break;
                default:
                    throw new Exception("Invalid diversity length term: " + term);
            }
            for(int idx = 0; idx < (lstdd.size() - 1); idx++) {
                // get reference length to compare against
                TransDivData tdd = lstdd.get(idx);
                switch(term) {
                    case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                        tdlen = tdd.cdsLength;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                        tdlen = tdd.utr5Length;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                        tdlen = tdd.utr3Length;
                        break;
                    case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                        tdlen = tdd.polyA_site;
                        break;
                }
                
                // compare rest of transactions to reference
                for(int cmpidx = idx+1; cmpidx < lstdd.size(); cmpidx++) {
                    TransDivData tddcmp = lstdd.get(cmpidx);
                    switch(term) {
                        case DlgFDAnalysis.Params.FEATURE_CDS_LENGTH:
                            tdcmplen = tddcmp.cdsLength;
                            break;
                        case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                            tdcmplen = tddcmp.utr5Length;
                            break;
                        case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                            tdcmplen = tddcmp.utr3Length;
                            break;
                        case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                            tdcmplen = tddcmp.polyA_site;
                            break;
                    }
                    
                    // first do a quick check for the actual sizes
                    // if they are different, there is no need to compare exons
                    if(Math.abs(tdlen - tdcmplen) > maxDiff)
                        diff++;
                    else {
                        HashMap<String, Object> hm = new HashMap<>();
                        hm.put(tdd.tad.transID, null);
                        hm.put(tddcmp.tad.transID, null);
                        switch(term) {
                            case DlgFDAnalysis.Params.FEATURE_5UTR_LENGTH:
                                varyingflg = !areAllGeneUTRsSimilar(gene, hm, genomicPos_5UTR);
                                break;
                            case DlgFDAnalysis.Params.FEATURE_3UTR_LENGTH:
                                varyingflg = !areAllGeneUTRsSimilar(gene, hm, genomicPos_3UTR);
                                break;
                            case DlgFDAnalysis.Params.FEATURE_PAS_POSITION:
                                varyingflg = !areAllGenePASSimilar(gene, hm, genomicPos_PAS);
                                break;
                            default:
                                varyingflg = !areAllGeneCDSIdentical(gene, hm, genomicPos_CDS);
                                break;
                        }
                        if(varyingflg)
                            diff++;
                        else
                            same++;
                    }
                }
            }
        }
        if((same + diff) > 0) {
            pct = (double)diff / (same + diff) * 100;
            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
        }
        else {
            pct = 0.0;
        }
        DiversityData dd = new DiversityData(key, same, diff, pct, AnnotationType.TRANS);
        return dd;
    }

    //
    // File Loading Functions
    //
    public HashMap<String, FeatureItemsInfo> loadFeatureItemsInfo(String filepath) {
        HashMap<String, FeatureItemsInfo> hm = new HashMap<>();
        long tstart = System.nanoTime();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                System.out.println("Loaded features data time: " + duration + " ms");

                String fields[];
                String description;
                int lnum = 1;
                for(String line : lines) {
                    if(lnum >= 1) {
                        fields = line.split("\t");
                        if(fields.length == 5) {
                            if(!hm.containsKey(fields[1])) {
                                ArrayList<String> lst = new ArrayList<>();
                                lst.add(fields[0]);
                                description = fields[4].replaceAll("[\"]", "");
                                hm.put(fields[1], new FeatureItemsInfo(fields[0], description, fields[2], fields[3], lst));
                            }
                            else
                                hm.get(fields[1]).items.add(fields[0]);
                        }
                        else {
                            logger.logError("Invalid features in '" + filepath + "', line " + lnum + ".");
                            hm.clear();
                            break;
                        }
                    }
                    lnum += 1;
                }
            }
        }
        catch (Exception e) {
            hm.clear();
            logger.logError("Unable to load features from '" + filepath + "': " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return hm;
    }
    public HashMap<String, TermTransInfo> loadTransFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, TermTransInfo> hm = new HashMap<>();
        long tstart = System.nanoTime();
        try {
            // check if file already exists and generate if not
            if(!Files.exists(Paths.get(filepath)))
                genTransFeatures(filepath, hmFeatures, hmFilterTrans);
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                System.out.println("Loaded transcript features data time: " + duration + " ms");

                String fields[];
                String name;
                int lnum = 1;
                for(String line : lines) {
                    if(lnum >= 1) {
                        fields = line.split("\t");
                        if(fields.length == 5) {
                            if(!hm.containsKey(fields[1])) {
                                ArrayList<String> lst = new ArrayList<>();
                                lst.add(fields[0]);
                                name = fields[4].replaceAll("[\"]", "");
                                hm.put(fields[1], new TermTransInfo(fields[2], fields[3], name, lst));
                            }
                            else
                                hm.get(fields[1]).trans.add(fields[0]);
                        }
                        else {
                            logger.logError("Invalid transcript features in '" + filepath + "', line " + lnum + ".");
                            hm.clear();
                            break;
                        }
                    }
                    lnum += 1;
                }
            }
        }
        catch (Exception e) {
            hm.clear();
            logger.logError("Unable to load transcript features: " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return hm;
    }

    //
    // File Generation Functions
    //
    
    // generate gene transcripts file - usually to be passed to Rscript
    public boolean genGeneTransFile(String filepath, HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        boolean result = false;
        try {
            HashMap<String, HashMap<String, Integer>> hm = getGeneTransLength(hmFilterGeneTrans);
            long tstart = System.nanoTime();
            logger.logDebug("Writing gene transcripts file to " + filepath);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer.write("geneName\ttranscript\tlength\n");
                for(String gene : hm.keySet()) {
                    HashMap<String, Integer> hmtrans = hm.get(gene);
                    for(String trans : hmtrans.keySet())
                        writer.write(gene + "\t" + trans + "\t" + hmtrans.get(trans) + "\n");
                }
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logDebug("Generated gene transcripts file in " + duration + " ms");
            } catch (IOException e) {
                logger.logError("Unable to generate gene transcripts file: " + e.getMessage());
                result = false;
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to generate transcript length file: " + e.getMessage());
            result = false;
        }
        
        // always attempt to remove file if failure
        if(!result)
            Utils.removeFile(filepath);
        return result;
    }
    // generate gene proteins file - usually passed to Rscript
    // WARNING: protein name includes the gene name, e.g. gene_protein, which is required for DIU
    //          to keep proteins confined to a gene since multiple genes can code for the same protein
    public boolean genGeneProteinsFile(String filepath, HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        boolean result = false;
        try {
            HashMap<String, HashMap<String, Integer>> hm = getGeneProteinsLength(hmFilterGeneTrans);
            long tstart = System.nanoTime();
            logger.logDebug("Writing gene proteins file to " + filepath);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer.write("geneName\tprotein\tlength\n");
                for(String gene : hm.keySet()) {
                    HashMap<String, Integer> hmProteins = hm.get(gene);
                    for(String protein : hmProteins.keySet())
                        writer.write(gene + "\t" + (gene + "_" + protein) + "\t" + hmProteins.get(protein) + "\n");
                }
                result = true;
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                logger.logDebug("Generated gene proteins file in " + duration + " ms");
            } catch (IOException e) {
                logger.logError("Unable to generate gene proteins file: " + e.getMessage());
                result = false;
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to generate transcript length file: " + e.getMessage());
            result = false;
        }
        
        // always attempt to remove file if failure
        if(!result)
            Utils.removeFile(filepath);
        return result;
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getTransFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, 
                                                                                                      HashMap<String, Object> hmFilterTrans, 
                                                                                                      boolean incNameDesc, boolean expandGO) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        HashMap<String, Integer> hmTerms = new HashMap<>();
        boolean goFlg = false;
        if(expandGO) {
            for(String src : hmFeatures.keySet()) {
                if(src.equals(fileDefs.srcGO))
                    goFlg = true;
            }
        }
        long tstart = System.nanoTime();
        System.out.println("getTransFeatures: " + hmFeatures);
        try {
            // get all unique features at the transcript level
            hmResults = _getTransFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
            for(String trans : hmResults.keySet()) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmTransDBs = hmResults.get(trans);
                for(String db : hmTransDBs.keySet()) {
                    if(goFlg && db.equals(fileDefs.srcGO)) {
                        HashMap<String, HashMap<String, String>> hmTransCats = hmTransDBs.get(db);
                        for(String cat : hmTransCats.keySet()) {
                            HashMap<String, String> hmTransIDs = hmTransCats.get(cat);
                            for(String id : hmTransIDs.keySet()) {
                                if(hmTerms.containsKey(id))
                                    hmTerms.put(id, hmTerms.get(id) + 1);
                                else
                                    hmTerms.put(id, 1);
                            }
                        }
                    }
                }
            }
            System.out.println("Generated initial features.");
            
            // filter GO terms if needed
            if(goFlg)
                hmResults = filterGOTerms(hmResults, hmTerms);
        }
        catch (Exception e) {
            hmResults.clear();
            logger.logInfo("Transcript features, " + hmFeatures + ", processing code exception: " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return hmResults;
    }

    // get features for given transcripts - returns unique features only (no duplicates)
    // HashMap<> returned contains transcript:source:feature:id:"count:positions string"
    // GO terms are expanded, if requested, but not filtered so that they can be filtered at the proper level: gene, protein, transcript
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> _getTransFeatures(HashMap<String, HashMap<String, Object>> hmFilterFeatures, 
            HashMap<String, Object> hmFilterTrans, boolean incNameDesc, boolean expandGO) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        boolean result = false;
        int goExpandCnt = 0;
        long tstart = System.nanoTime();

        if(incNameDesc)
            loadAFStats();
        loadTransAFIdPos();
        GO go = new GO();
        try {
            result = true;
            int noidCnt = 0;
            HashMap<String, HashMap<String, HashMap<String, String>>> hmSources;
            HashMap<String, HashMap<String, String>> hmFeatures;
            HashMap<String, String> hmIds;
            HashMap<String, Object> hm;
            boolean filter = (hmFilterTrans != null && !hmFilterTrans.isEmpty());
            for(String trans : hmTransAFIdPos.keySet()) {
                // check if transcript requested
                if(!filter || hmFilterTrans.containsKey(trans)) {
                    hmSources = hmTransAFIdPos.get(trans);
                    for(String source : hmSources.keySet()) {
                        hmFeatures = hmSources.get(source);
                        // check if source requested
                        if(hmFilterFeatures.containsKey(source)) {
                            hm = hmFilterFeatures.get(source);
                            for(String feature : hmFeatures.keySet()) {
                                // check if feature requested
                                if(hm.isEmpty() || hm.containsKey(feature)) {
                                    hmIds = hmFeatures.get(feature);
                                    for(String id : hmIds.keySet()) {
                                        if(!id.isEmpty()) {
                                            String namedesc = "";
                                            if(incNameDesc) {
                                                // description will be set to desc if available, name if not (blank if neither)
                                                String name = hmAFStats.get(source).get(feature).hmIds.get(id).name;
                                                String desc = hmAFStats.get(source).get(feature).hmIds.get(id).desc;
                                                if(!desc.isEmpty())
                                                    namedesc = desc;
                                                else
                                                    namedesc = name;
                                                namedesc = namedesc.replaceAll("[\"]", "'");
                                            }
                                            HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs;
                                            HashMap<String, HashMap<String, String>> hmCats;
                                            HashMap<String, String> hmIDs;
                                            if(hmResults.containsKey(trans)) {
                                                hmDBs = hmResults.get(trans);
                                                if(hmDBs.containsKey(source)) {
                                                    hmCats = hmDBs.get(source);
                                                    if(hmCats.containsKey(feature)) {
                                                        hmIDs = hmCats.get(feature);
                                                        if(!hmIDs.containsKey(id))
                                                            hmIDs.put(id, namedesc);
                                                    }
                                                    else {
                                                        hmIDs = new HashMap<>();
                                                        hmIDs.put(id, namedesc);
                                                        hmCats.put(feature, hmIDs);
                                                    }
                                                }
                                                else {
                                                    hmCats = new HashMap<>();
                                                    hmIDs = new HashMap<>();
                                                    hmIDs.put(id, namedesc);
                                                    hmCats.put(feature, hmIDs);
                                                    hmDBs.put(source, hmCats);
                                                }
                                            }
                                            else {
                                                hmDBs = new HashMap<>();
                                                hmCats = new HashMap<>();
                                                hmIDs = new HashMap<>();
                                                hmIDs.put(id, namedesc);
                                                hmCats.put(feature, hmIDs);
                                                hmDBs.put(source, hmCats);
                                                hmResults.put(trans, hmDBs);
                                            }
                                            
                                            // check if this is a GO term and expansion requested
                                            if(expandGO && source.equals(fileDefs.srcGO)) {
                                                String gene = getTransGene(trans);
                                                if(gene.toLowerCase().equals("cap1"))
                                                    System.out.println("Expanding GO term: " + id);
                                                // get list of ancestors and process
                                                // ancestors may be in a different feature/cat so handle accordingly
                                                HashMap<String, GO.TermInfo> hmAncestors = go.getTermAncestors(id);
                                                for(String goid : hmAncestors.keySet()) {
                                                    GO.TermInfo ti = hmAncestors.get(goid);
                                                    String ns = go.getCategoryNamespace(ti.catidx);
                                                    String addFeature = "";
                                                    if(ns.equals("biological_process"))
                                                        addFeature = fileDefs.goP;
                                                    else if(ns.equals("molecular_function"))
                                                        addFeature = fileDefs.goF;
                                                    else if(ns.equals("cellular_component"))
                                                        addFeature = fileDefs.goC;
                                                    if(!addFeature.isEmpty()) {
                                                        // make sure ancestor's feature was requested
                                                        if(hm.isEmpty() || hm.containsKey(addFeature)) {
                                                            HashMap<String, String> hmAddFeatureIDs = hmIDs;
                                                            if(!feature.equals(addFeature)) {
                                                                if(hmCats.containsKey(addFeature))
                                                                    hmAddFeatureIDs = hmCats.get(addFeature);
                                                                else {
                                                                    hmAddFeatureIDs = new HashMap<>();
                                                                    hmCats.put(addFeature, hmAddFeatureIDs);
                                                                }
                                                            }
                                                            if(!hmAddFeatureIDs.containsKey(goid)) {
                                                                hmAddFeatureIDs.put(goid, ti.name);
                                                                goExpandCnt++;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else
                                            noidCnt++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(result) {
                if(goExpandCnt > 0)
                    System.out.println("Added " + goExpandCnt + " GO terms in expansion - at the transcript level");
                if(noidCnt > 0)
                    logger.logInfo("NOTE: " + noidCnt + " feature entries ignored - missing required 'id' attribute.");
                long duration = (System.nanoTime() - tstart)/1000000;
                logger.logDebug("Got transcript features in " + duration + " ms");
            }
        }
        catch (Exception e) {
            logger.logWarning("Transcript features annotation file processing code exception: " + e.getMessage());
            hmResults.clear();
        }
        
        return hmResults;
    }
    public boolean genTransFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        boolean result = false;
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = getTransFeatures(hmFeatures, hmFilterTrans, true, true);
        result = writeFeaturesFile(filepath, hmResults);
        return result;
    }
    public boolean genGeneFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        boolean result = false;
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = getGeneFeatures(hmFeatures, hmFilterTrans, true, true);
        result = writeFeaturesFile(filepath, hmResults);
        return result;
    }
    static int dspcount = 0;
    static boolean dspflag = true;
    public boolean geneHasFeatureId(String gene, String source, String feature, String featureId, HashMap<String, Object> hmGeneTrans) {
        boolean result = false;
        loadTransAFIdPos();
        if(dspflag) {
            logger.logDebug("TransAFIdPos has " + hmTransAFIdPos.size() + " transcripts.");
            logger.logDebug("geneHasFeatureId(" + gene + ", " + source + ", " + feature + ", " + featureId + ")" );
        }
        HashMap<String, HashMap<String, String>> hmFeatures;
        // transcript:source:feature:id:"count:positions string"
        for(String trans : hmGeneTrans.keySet()) {
            if(dspflag)
                logger.logDebug("Processing transcript " + trans);
            if(hmTransAFIdPos.containsKey(trans)) {
                if(dspflag)
                    logger.logDebug("Got matching transcript gene");
                if(hmTransAFIdPos.get(trans).containsKey(source)) {
                    if(dspflag)
                        logger.logDebug("Transcript has source");
                    hmFeatures = hmTransAFIdPos.get(trans).get(source);
                    if(hmFeatures.containsKey(feature) && hmFeatures.get(feature).containsKey(featureId)) {
                        //logger.logDebug("Transcript has feature and featureId");
                        result = true;
                        break;
                    }
                }
            }
        }
        if(dspflag)
            logger.logDebug("Gene " + gene + " check completed: " + result);
        if(++dspcount > 5)
            dspflag = false;
        return result;
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getGeneFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, 
                                                                                                      HashMap<String, Object> hmFilterTrans, 
                                                                                                      boolean incNameDesc, boolean expandGO) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        HashMap<String, Integer> hmTerms = new HashMap<>();
        boolean goFlg = false;
        if(expandGO) {
            for(String src : hmFeatures.keySet()) {
                if(src.equals(fileDefs.srcGO))
                    goFlg = true;
            }
        }
        long tstart = System.nanoTime();
        System.out.println("getGeneFeatures: " + hmFeatures);
        try {
            // get all unique features at the transcript level
            HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransFeatures;
            hmTransFeatures = _getTransFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
            String gene;
            for(String trans : hmTransFeatures.keySet()) {
                gene = getTransGene(trans);
                if(hmResults.containsKey(gene)) {
                    HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmResults.get(gene);
                    HashMap<String, HashMap<String, HashMap<String, String>>> hmTransDBs = hmTransFeatures.get(trans);
                    for(String db : hmTransDBs.keySet()) {
                        boolean dbgo = false;
                        if(goFlg && db.equals(fileDefs.srcGO))
                            dbgo = true;
                        if(hmDBs.containsKey(db)) {
                            HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                            HashMap<String, HashMap<String, String>> hmTransCats = hmTransDBs.get(db);
                            for(String cat : hmTransCats.keySet()) {
                                if(hmCats.containsKey(cat)) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    HashMap<String, String> hmTransIDs = hmTransCats.get(cat);
                                    for(String id : hmTransIDs.keySet()) {
                                        if(!hmIDs.containsKey(id)) {
                                            hmIDs.put(id, hmTransIDs.get(id));
                                            if(dbgo) {
                                                if(hmTerms.containsKey(id))
                                                    hmTerms.put(id, hmTerms.get(id) + 1);
                                                else
                                                    hmTerms.put(id, 1);
                                            }
                                        }
                                    }
                                }
                                else {
                                    // add all the IDs for this Cat - no need to check since they are unique
                                    hmCats.put(cat, hmTransCats.get(cat));
                                    if(dbgo) {
                                        HashMap<String, String> hmIDs = hmCats.get(cat);
                                        for(String id : hmIDs.keySet()) {
                                            if(hmTerms.containsKey(id))
                                                hmTerms.put(id, hmTerms.get(id) + 1);
                                            else
                                                hmTerms.put(id, 1);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            // add all the Cats, and IDs for this DB - no need to check since they are unique
                            hmDBs.put(db, hmTransDBs.get(db));
                            if(dbgo) {
                                HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                                for(String cat : hmCats.keySet()) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    for(String id : hmIDs.keySet()) {
                                        if(hmTerms.containsKey(id))
                                            hmTerms.put(id, hmTerms.get(id) + 1);
                                        else
                                            hmTerms.put(id, 1);
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    // add all the DBs, Cats, and IDs for this transcript - no need to check since they are unique
                    hmResults.put(gene, hmTransFeatures.get(trans));
                    if(goFlg) {
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmTransFeatures.get(trans);
                        for(String db : hmDBs.keySet()) {
                            if(db.equals(fileDefs.srcGO)) {
                                HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                                for(String cat : hmCats.keySet()) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    for(String id : hmIDs.keySet()) {
                                        if(hmTerms.containsKey(id))
                                            hmTerms.put(id, hmTerms.get(id) + 1);
                                        else
                                            hmTerms.put(id, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Generated initial gene features.");
            
            // filter GO terms if needed
            if(goFlg)
                hmResults = filterGOTerms(hmResults, hmTerms);
        }
        catch (Exception e) {
            hmResults.clear();
            logger.logInfo("Gene features, " + hmFeatures + ", processing code exception: " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return hmResults;
    }
    private HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> filterGOTerms(HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>>hmResults, HashMap<String, Integer> hmTerms) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmFilteredResults = new HashMap<>();

        try {
            int min = MIN_GOCNT;
            int max = MAX_GOCNT;
            int totalCnt = 0;
            int rmvCnt = 0;

            // check each GO term for required min/max counts
            for(String item : hmResults.keySet()) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmResults.get(item);
                for(String db : hmDBs.keySet()) {
                    if(db.equals(fileDefs.srcGO)) {
                        HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                        for(String cat : hmCats.keySet()) {
                            HashMap<String, String> hmIDs = hmCats.get(cat);
                            int idCnt = hmIDs.size();
                            int addCnt = 0;
                            int ridCnt = 0;
                            totalCnt += hmIDs.size();
                            for(String id : hmIDs.keySet()) {
                                int cnt = hmTerms.get(id);
                                if(App.isNoGOExpansionLimits() || (cnt >= min && cnt <= max)) {
                                    addCnt++;
                                    // xfer the term to filtered results
                                    HashMap<String, HashMap<String, HashMap<String, String>>> hmFDBs;
                                    HashMap<String, HashMap<String, String>> hmFCats;
                                    HashMap<String, String> hmFIDs;
                                    if(hmFilteredResults.containsKey(item)) {
                                        hmFDBs = hmFilteredResults.get(item);
                                        if(hmFDBs.containsKey(db)) {
                                            hmFCats = hmFDBs.get(db);
                                            if(hmFCats.containsKey(cat)) {
                                                hmFIDs = hmFCats.get(cat);
                                                hmFIDs.put(id, hmIDs.get(id));
                                            }
                                            else {
                                                hmFIDs = new HashMap<>();
                                                hmFIDs.put(id, hmIDs.get(id));
                                                hmFCats.put(cat, hmFIDs);
                                            }
                                        }
                                        else {
                                            hmFCats = new HashMap<>();
                                            hmFIDs = new HashMap<>();
                                            hmFIDs.put(id, hmIDs.get(id));
                                            hmFCats.put(cat, hmFIDs);
                                            hmFDBs.put(db, hmFCats);
                                        }
                                    }
                                    else {
                                        hmFDBs = new HashMap<>();
                                        hmFCats = new HashMap<>();
                                        hmFIDs = new HashMap<>();
                                        hmFIDs.put(id, hmIDs.get(id));
                                        hmFCats.put(cat, hmFIDs);
                                        hmFDBs.put(db, hmFCats);
                                        hmFilteredResults.put(item, hmFDBs);
                                    }
                                }
                                else {
                                    ridCnt++;
                                    rmvCnt++;
                                }
                            }
                            if((addCnt + ridCnt) != idCnt)
                                System.out.println(addCnt + " + " + rmvCnt + " != " + idCnt);
                            else if(addCnt > 0) {
                                int newCnt = hmFilteredResults.get(item).get(db).get(cat).size();
                                if(newCnt != addCnt)
                                    System.out.println(newCnt + " != " + addCnt);
                            }
                        }
                    }
                    else {
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmFDBs;
                        if(hmFilteredResults.containsKey(item))
                            hmFDBs = hmFilteredResults.get(item);
                        else {
                            hmFDBs = new HashMap<>();
                            hmFilteredResults.put(item, hmFDBs);
                        }
                        hmFDBs.put(db, hmDBs.get(db));
                    }
                }
            }
            System.out.println("Total initial GO terms at top level: " + totalCnt);
            totalCnt = 0;
            for(String gogene : hmFilteredResults.keySet()) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmFilteredResults.get(gogene);
                for(String db : hmDBs.keySet()) {
                    if(db.equals(fileDefs.srcGO)) {
                        HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                        for(String cat : hmCats.keySet()) {
                            HashMap<String, String> hmIDs = hmCats.get(cat);
                            totalCnt += hmIDs.size();
                        }
                    }
                }
            }
            System.out.println("Filtered GO terms at top level: " + totalCnt);
            if(rmvCnt > 0)
                System.out.println("Removed " + rmvCnt + " GO terms with min-max filtering");
        }
        catch (Exception e) {
            hmFilteredResults.clear();
            logger.logInfo("Filter GO Terms code exception: " + e.getMessage());
        }
        return hmFilteredResults;
    }

    public boolean genProteinFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = getProteinFeatures(hmFeatures, hmFilterTrans, true, true);
        boolean result = writeFeaturesFile(filepath, hmResults);
        return result;
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getProteinFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, 
                                                                                        HashMap<String, Object> hmFilterTrans, boolean incNameDesc, boolean expandGO) {
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        HashMap<String, Integer> hmTerms = new HashMap<>();
        long tstart = System.nanoTime();
        System.out.println("getProteinFeatures: " + hmFeatures);
        boolean goFlg = false;
        if(expandGO) {
            for(String db : hmFeatures.keySet()) {
                if(db.equals(fileDefs.srcGO))
                    goFlg = true;
            }
        }
        try {
            // get all unique features at the transcript level
            HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransFeatures;
            hmTransFeatures = _getTransFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
            String protein;
            for(String trans : hmTransFeatures.keySet()) {
                protein = getTransProtein(trans);
                if(hmResults.containsKey(protein)) {
                    HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmResults.get(protein);
                    HashMap<String, HashMap<String, HashMap<String, String>>> hmTransDBs = hmTransFeatures.get(trans);
                    for(String db : hmTransDBs.keySet()) {
                        boolean dbgo = false;
                        if(goFlg && db.equals(fileDefs.srcGO))
                            dbgo = true;
                        if(hmDBs.containsKey(db)) {
                            HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                            HashMap<String, HashMap<String, String>> hmTransCats = hmTransDBs.get(db);
                            for(String cat : hmTransCats.keySet()) {
                                if(hmCats.containsKey(cat)) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    HashMap<String, String> hmTransIDs = hmTransCats.get(cat);
                                    for(String id : hmTransIDs.keySet()) {
                                        if(!hmIDs.containsKey(id)) {
                                            hmIDs.put(id, hmTransIDs.get(id));
                                            if(dbgo) {
                                                if(hmTerms.containsKey(id))
                                                    hmTerms.put(id, hmTerms.get(id) + 1);
                                                else
                                                    hmTerms.put(id, 1);
                                            }
                                        }
                                    }
                                }
                                else {
                                    // add all the IDs for this Cat - no need to check since they are unique
                                    hmCats.put(cat, hmTransCats.get(cat));
                                    if(dbgo) {
                                        HashMap<String, String> hmIDs = hmCats.get(cat);
                                        for(String id : hmIDs.keySet()) {
                                            if(hmTerms.containsKey(id))
                                                hmTerms.put(id, hmTerms.get(id) + 1);
                                            else
                                                hmTerms.put(id, 1);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            // add all the Cats, and IDs for this DB - no need to check since they are unique
                            hmDBs.put(db, hmTransDBs.get(db));
                            if(dbgo) {
                                HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                                for(String cat : hmCats.keySet()) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    for(String id : hmIDs.keySet()) {
                                        if(hmTerms.containsKey(id))
                                            hmTerms.put(id, hmTerms.get(id) + 1);
                                        else
                                            hmTerms.put(id, 1);
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    // add all the DBs, Cats, and IDs for this transcript - no need to check since they are unique
                    hmResults.put(protein, hmTransFeatures.get(trans));
                    if(goFlg) {
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmTransFeatures.get(trans);
                        for(String db : hmDBs.keySet()) {
                            if(db.equals(fileDefs.srcGO)) {
                                HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                                for(String cat : hmCats.keySet()) {
                                    HashMap<String, String> hmIDs = hmCats.get(cat);
                                    for(String id : hmIDs.keySet()) {
                                        if(hmTerms.containsKey(id))
                                            hmTerms.put(id, hmTerms.get(id) + 1);
                                        else
                                            hmTerms.put(id, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Generated initial protein features.");
            
            // filter GO terms if needed
            if(goFlg)
                hmResults = filterGOTerms(hmResults, hmTerms);
        }
        catch (Exception e) {
            hmResults.clear();
            logger.logInfo("Protein features, " + hmFeatures + ", processing code exception: " + e.getMessage());
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        
        return hmResults;
    }

    //
    // INTERNAL FUNCTIONS
    //
    
    // all transcripts must be coding or will return false even if all are non-coding
    public boolean areAllGeneCDSIdentical(String gene, HashMap<String, Object> hm, HashMap<String, String> genomicPos) {
        boolean result = false;
        int minPOS=50000000;
        int maxPOS=0;
        for(String trans : hm.keySet()) {
            if(hmTransData.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                // only take into account coding ones
                if(td.posCDS.start != td.posCDS.end && td.lstExons.size() > 0) {
                    if(genomicPos.containsKey(trans)){
                        int transPASPOS = Integer.parseInt(genomicPos.get(trans));
                        if(transPASPOS > maxPOS)
                            maxPOS = transPASPOS;
                        if(transPASPOS < minPOS)
                            minPOS = transPASPOS;
                    }else{
                        continue;
                    }
                }else{
                        continue;
                    }
            }else {
                System.out.println("WARN: No data available for transcript " + trans);
                break;
            }
        }
        if(maxPOS-minPOS==0)
            result = true;
        return result;
    }
    
    // all transcripts must be coding or will return false even if all are non-coding
    public boolean areAllGeneUTRsSimilar(String gene, HashMap<String, Object> hm, HashMap<String, String> genomicPos) {
        boolean result = false;
        int minPOS=50000000;
        int maxPOS=0;
        for(String trans : hm.keySet()) {
            if(hmTransData.containsKey(trans)) {
                TransData td = hmTransData.get(trans);
                // only take into account coding ones
                if(td.posCDS.start != td.posCDS.end && td.lstExons.size() > 0) {
                    if(genomicPos.containsKey(trans)){
                        int transPASPOS = Integer.parseInt(genomicPos.get(trans));
                        if(transPASPOS > maxPOS)
                            maxPOS = transPASPOS;
                        if(transPASPOS < minPOS)
                            minPOS = transPASPOS;
                    }else{
                        continue;
                    }
                }else{
                        continue;
                    }
            }else {
                System.out.println("WARN: No data available for transcript " + trans);
                break;
            }
        }
        if(maxPOS-minPOS<MAX_UTR_LENGTH_DIFF)
            result = true;
        return result;
    }
    
    // all transcripts must be coding or will return false even if all are non-coding
    public boolean areAllGenePASSimilar(String gene, HashMap<String, Object> hm, HashMap<String, String> genomicPos) {
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
        if(maxPOS-minPOS<MAX_UTR_LENGTH_DIFF)
            result = true;
        return result;
    }
    
    private boolean writeFeaturesFile(String filepath, HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults) {
        boolean result = false;
        logger.logDebug("Writing features " + filepath + " transcripts file...");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            ArrayList<String> lst;
            for(String item : hmResults.keySet()) {
                HashMap<String, HashMap<String, HashMap<String, String>>> hmDBs = hmResults.get(item);
                for(String db : hmDBs.keySet()) {
                    HashMap<String, HashMap<String, String>> hmCats = hmDBs.get(db);
                    for(String cat : hmCats.keySet()) {
                        HashMap<String, String> hmIDs = hmCats.get(cat);
                        for(String id : hmIDs.keySet())
                            writer.write(item + "\t" + id + "\t" + db + "\t" + cat + "\t\"" + hmIDs.get(id) + "\"\n");
                    }
                }
            }
            result = true;
        } catch (IOException e) {
            logger.logInfo("ERROR: Term Transcripts, " + filepath + ", file code exception: " + e.getMessage());
            result = false;
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    
    // Support functions
    public static HashMap<String, String> getAttributes(String text) {
        HashMap<String, String> attrs = new HashMap<>();
        String val;
        String fields[] = text.split(";");
        for(String field : fields) {
            field = field.trim();
            String values[] = field.trim().split("=");
            // in case equal sign in string value - not sure if possible
            val = "";
            int cnt = values.length;
            for(int i = 1; i < cnt; i++)
                val += values[i];
            attrs.put(values[0], val);
        }
        return attrs;
    }
    
    //
    // Data Classes
    //
    private static class IndexData implements Comparable<IndexData>{
        public String geneName;
        public String geneID;
        public String transName;
        public String chromo;
        public String strand;
        public String protein;
        public String dbCats;
        public String alignCat;
        public String alignAttrs;
        public int length;
        public long posFile;

        SeqAlign.Position posCDS;
        ArrayList<SeqAlign.Position> lstExons;
        
        public IndexData(String name, String id, String trans, String chromo, String strand, int length,
                         String protein, String dbCats, String alignCat, String alignAttrs, long posFile) {
            this.geneName = name;
            this.geneID = id;
            this.transName = trans;
            this.chromo = chromo;
            this.strand = strand;
            this.protein = protein;
            this.dbCats = dbCats;
            this.alignCat = alignCat;
            this.alignAttrs = alignAttrs;
            this.length = length;
            this.posFile = posFile;

            // set defaults, actual data set later
            this.posCDS = new SeqAlign.Position(0,0);
            lstExons = new ArrayList<>();
        }
        public void addExon(SeqAlign.Position exon) { lstExons.add(exon); }
        public String getCDS() { return posCDS.start + "-" + posCDS.end; }
        public String getExons() {
            String str = "";
            for(SeqAlign.Position exon : lstExons) {
                if(!str.isEmpty())
                    str += ";";
                str += exon.start + "-" + exon.end;
            }
            return str;
        }
        @Override
        public int compareTo(IndexData td) {
            if (geneID.equals(td.geneID))
                return (transName.compareTo(td.transName));
            else
                return (geneID.compareTo(td.geneID));
        }
    }
    public static class TransData {
        String trans, transName;
        String gene;
        String protein;
        String alignCat, alignAttrs, chromo;
        int length, cdsLength, utr5Length, utr3Length, protLength, sjCount;
        boolean coding;
        boolean negStrand;
        SeqAlign.Position pos, posCDS;
        ArrayList<SeqAlign.Position> lstExons;
        TransData(String trans, String transName, String gene, String chromo, String strand, int length, String cds,
                String protein, String alignCat, String alignAttrs, String exons) {
            this.trans = trans;
            this.transName = transName;
            this.chromo = chromo;
            this.gene = gene;
            this.protein = protein.trim().equals(".")? "" : protein;
            this.alignCat = alignCat.equals(".")? "" : alignCat;
            this.alignAttrs = alignAttrs.equals(".")? "" : alignAttrs;
            this.length = length;
            this.negStrand = strand.equals("-");

            // parse CDS position and calculate length related values
            String[] fields;
            fields = cds.split("-");
            if(fields.length == 2)
                posCDS = new SeqAlign.Position(Integer.parseInt(fields[0].trim()), Integer.parseInt(fields[1].trim()));
            else
                posCDS = new SeqAlign.Position(0,0);
            if(posCDS.start != posCDS.end) {
                this.cdsLength = posCDS.end - posCDS.start + 1;
                this.coding = true;
                if(posCDS.start > 1)
                    this.utr5Length = posCDS.start - 1;
                else
                    this.utr5Length = 0;
                if(posCDS.end < length)
                    this.utr3Length = length - posCDS.end;
                else
                    this.utr5Length = 0;
                protLength = this.cdsLength / 3;
                // adjust for stop codon
                if(protLength > 0)
                    protLength--;
            }
            else {
                this.cdsLength = 0;
                this.coding = false;
                this.utr5Length = 0;
                this.utr3Length = 0;
                this.protLength = 0;
            }

            // parse exon positions
            String[] subfields;
            fields = exons.split(";");
            lstExons = new ArrayList<>();
            this.pos = new SeqAlign.Position(0, 0);
            pos.start = Integer.MAX_VALUE;
            for(String field : fields) {
                subfields = field.split("-");
                if(subfields.length == 2) {
                    SeqAlign.Position epos = new SeqAlign.Position(Integer.parseInt(subfields[0].trim()), Integer.parseInt(subfields[1].trim()));
                    lstExons.add(epos);
                    if(pos.start > epos.start)
                        pos.start = epos.start;
                    if(pos.end < epos.end)
                        pos.end = epos.end;
                }
            }
            this.sjCount = lstExons.isEmpty()? 0 : lstExons.size() - 1;
            if(pos.start == Integer.MAX_VALUE)
                pos.start = 0;
        }
    }
    // Note: score and phase are not used and are kept as strings - must convert to float and integer if needed
    public static class AnnotationDataEntry {
        public String transID;
        public String db;
        public String category;
        public int start;
        public int end;
        public String score;
        public String strand;
        public String phase;
        public String attrs;
        public int length;
        
        public AnnotationDataEntry(String transID, String db, String category, int start, int end, 
                String score, String strand, String phase, String attrs, int length) {
            this.transID = transID;
            this.db = db;
            this.category = category;
            this.start = start;
            this.end = end;
            this.score = score;
            this.strand = strand;
            this.phase = phase;
            this.attrs = attrs;
            this.length = length;
        }
    }
    public static class DBCatIDStatsData {
        int count;
        int transCount;
        // add name - deal with escaped commas and backslash?
        public DBCatIDStatsData(int count, int transCount) {
            this.count = count;
            this.transCount = transCount;
        }
        public String toTSV() {
            String str = "";
            str += count;
            str += "," + transCount;
            return str;
        }
        public static DBCatIDStatsData fromTSV(String values) {
            DBCatIDStatsData d = null;
            String fields[] = values.split(",");
            if(fields.length == 2) {
                int cnt = Integer.parseInt(fields[0].trim());
                int tcnt = Integer.parseInt(fields[1].trim());
                d = new DBCatIDStatsData(cnt, tcnt);
            }
            return d;
        }
    }
    public static class SummaryData {
        int genes;
        int transcripts;
        HashMap<String, HashMap<String, HashMap<String, Integer>>> hmDBCatIdCounts;
        
        public SummaryData(int genes, int transcripts, HashMap<String, HashMap<String, HashMap<String, Integer>>> hmDBCatIdCounts) {
            this.genes = genes;
            this.transcripts = transcripts;
            this.hmDBCatIdCounts = hmDBCatIdCounts;
        }
        public ObservableList<DBCategoryData> getDataList() {
            ObservableList<DBCategoryData> lst = FXCollections.observableArrayList();

            // db:cat:id[cnt]
            for(String db : hmDBCatIdCounts.keySet()) {
                HashMap<String, HashMap<String, Integer>> hmCat = hmDBCatIdCounts.get(db);
                for(String cat : hmCat.keySet()) {
                    HashMap<String, Integer> hmId = hmCat.get(cat);
                    int entries = 0;
                    for(String id : hmId.keySet())
                        entries += hmId.get(id);
                    lst.add(new DBCategoryData(db, cat, entries, hmId.size()));
                }
            }
            return lst;
        }
    }
    public static class DBCategoryData  implements Comparable<DBCategoryData>{
        public final SimpleStringProperty db;
        public final SimpleStringProperty cat;
        public final SimpleIntegerProperty count;
        public final SimpleIntegerProperty idCount;
 
        public DBCategoryData(String db, String cat, int count, int idCount) {
            this.db = new SimpleStringProperty(db);
            this.cat = new SimpleStringProperty(cat);
            this.count = new SimpleIntegerProperty(count);
            this.idCount = new SimpleIntegerProperty(idCount);
        }
        public String getDB() { return db.get(); }
        public String getCategory() { return cat.get(); }
        public Integer getCount() { return count.get(); }
        public Integer getIdCount() { return idCount.get(); }
        @Override
        public int compareTo(DBCategoryData td) {
            if (db.get().equals(td.db.get()))
                return (cat.get().compareToIgnoreCase(td.cat.get()));
            else
                return (db.get().compareToIgnoreCase(td.db.get()));
        }
    }
    public static class DiversityResultsInternal {
        HashMap<String, HashMap<String, Boolean>> hmTranscript = new HashMap<>();
        HashMap<String, HashMap<String, Boolean>> hmProtein = new HashMap<>();
        HashMap<String, HashMap<String, Boolean>> hmGenomic = new HashMap<>();
        DiversityResults fdr;
    }
    public static class DiversityResults {
        DiversityRegionStats drTranscript;
        DiversityRegionStats drProtein;
        DiversityRegionStats drGenomic;
        DiversityRegionStats drTotal;
        ArrayList<DiversityData> lstData;
        ArrayList<DiversityData> lstPWData;
        public DiversityResults() {
            drTranscript = new DiversityRegionStats();
            drProtein = new DiversityRegionStats();
            drGenomic = new DiversityRegionStats();
            drTotal = new DiversityRegionStats();
            lstData = new ArrayList<>();
            lstPWData = new ArrayList<>();
        }
        public DiversityResults(List<String> lstLines) throws Exception {
            String errmsg = "Invalid Functional Diversity results data.";
            int idx = 0;
            int cnt = lstLines.size();
            if(cnt >= 4) {
                drTranscript = new DiversityRegionStats(lstLines.get(idx++));
                drProtein = new DiversityRegionStats(lstLines.get(idx++));
                drGenomic = new DiversityRegionStats(lstLines.get(idx++));
                drTotal = new DiversityRegionStats(lstLines.get(idx++));
                lstData = new ArrayList<>();
                while(idx < cnt) {
                    String line = lstLines.get(idx++);
                    if(line.isEmpty())
                        break;
                    lstData.add(new DiversityData(line));
                }
                lstPWData = new ArrayList<>();
                while(idx < cnt)
                    lstPWData.add(new DiversityData(lstLines.get(idx++)));
            }
            else
                throw new Exception(errmsg);
        }        
        public String toTSV() {
            String str = "";
            str += drTranscript.toTSV() + "\n";
            str += drProtein.toTSV() + "\n";
            str += drGenomic.toTSV() + "\n";
            str += drTotal.toTSV() + "\n";
            for(DiversityData dd : lstData)
                str += dd.toTSV() + "\n";
            str += "\n";
            for(DiversityData dd : lstPWData)
                str += dd.toTSV() + "\n";
            return str;
        }
    }
    public static class DiversityData implements Comparable<DiversityData> {
        public String name;
        public int same;
        public int diff;
        public double diffpct;
        AnnotationType type;
        
        public DiversityData(String name, int same, int diff, double diffpct, AnnotationType type) {
            this.name = name;
            this.same = same;
            this.diff = diff;
            this.diffpct = diffpct;
            this.type = type;
        }
        
        public String getName(){return this.name;}
        public int getSame(){return this.same;}
        public int getDiff(){return this.diff;}
        public double getDiffPCT(){return this.diffpct;}
        public AnnotationType getType(){return this.type;}
        
        public void setSame(int n){this.same = n;}
        public void setDiff(int n){this.diff = n;}
        public void setDiffPCT(double n){this.diffpct = n;}
        
        public DiversityData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 5) {
                name = fields[0];
                same = Integer.parseInt(fields[1]);
                diff = Integer.parseInt(fields[2]);
                diffpct = Double.parseDouble(fields[3]);
                type = AnnotationType.valueOf(fields[4]);
            }
            else
                throw new Exception("Invalid Functional DiversityData value.");
        }
        public String toTSV() {
            String str = "";
            str += name;
            str += "\t" + same;
            str += "\t" + diff;
            str += "\t" + diffpct;
            str += "\t" + type.name();
            return str;
        }
        @Override
        public int compareTo(DiversityData dd) {
            return (name.compareToIgnoreCase(dd.name));
        }
    }
    public static class DiversityRegionStats {
        int cnt;
        int vcnt;
        double vpct;
        public DiversityRegionStats() {
            cnt = 0;
            vcnt = 0;
            vpct = 0.0;
        }
        public DiversityRegionStats(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 3) {
                cnt = Integer.parseInt(fields[0]);
                vcnt = Integer.parseInt(fields[1]);
                vpct = Double.parseDouble(fields[2]);
            }
            else
                throw new Exception("Invalid Functional DiversityRegionStats value.");
        }
        public String toTSV() {
            String str = "";
            str += cnt;
            str += "\t" + vcnt;
            str += "\t" + vpct;
            return str;
        }
    }
    public static class TransDivData {
        AnnotationDataEntry tad;
        int transLength;
        int cdsStart = 0;
        int cdsEnd = 0;
        int cdsLength = 0;
        int utr3Start = 0;
        int utr3End = 0;
        int utr3Length = 0;
        int utr5Start = 0;
        int utr5End = 0;
        int utr5Length = 0;
        int polyA_site = 0;
        
        boolean coding;
        public HashMap<String, Object> hmUtr3List = new HashMap<>();
        public HashMap<String, HashMap<String, Integer>> hmUtr3 = new HashMap<>();
        public HashMap<String, HashMap<String, HashMap<String, Integer>>> hmxUtr3 = new HashMap<>();
        public HashMap<String, Object> hmUtr5List = new HashMap<>();
        public HashMap<String, HashMap<String, Integer>> hmUtr5 = new HashMap<>();
        public HashMap<String, HashMap<String, HashMap<String, Integer>>> hmxUtr5 = new HashMap<>();
        // left over from old approach
        public HashMap<String, Object> hmTransKeyList = new HashMap<>();
        public HashMap<String, Object> hmProteinKeyList = new HashMap<>();
        public HashMap<String, Object> hmGenomicKeyList = new HashMap<>();
        public HashMap<String, Integer> hmTransDB = new HashMap<>();
        public HashMap<String, Integer> hmProteinDB = new HashMap<>();
        public HashMap<String, Integer> hmGenomicDB = new HashMap<>();
        public HashMap<String, HashMap<String, Integer>> hmXTransDB = new HashMap<>();
        public HashMap<String, HashMap<String, Integer>> hmXProteinDB = new HashMap<>();
        public HashMap<String, HashMap<String, Integer>> hmXGenomicDB = new HashMap<>();
        public TransDivData(AnnotationDataEntry tad, boolean coding) {
            this.tad = tad;
            this.coding = coding;
            this.transLength = tad.end - tad.start + 1;
        }
    }
    public static class Term implements Comparable<Term> {
        String id;
        String name;
        String db;
        String category;
        
        public Term(String id, String name, String db, String category) {
            this.id = id;
            this.name = name;
            this.db = db;
            this.category = category;
        }
        @Override
        public int compareTo(Term term) {
            if(db.equals(term.db))
                return (category.compareToIgnoreCase(term.category));
            else {
                if(db.equals(term.db))
                    return (db.compareToIgnoreCase(term.db));
                else
                    return (id.compareToIgnoreCase(term.id));
            }
        }
    }
    public static class FeatureItemsInfo {
        String feature, description, db, category;
        ArrayList<String> items;
        
        public FeatureItemsInfo(String feature, String description, String db, String category, ArrayList<String> items) {
            this.feature = feature;
            this.description = description;
            this.db = db;
            this.category = category;
            this.items = items;
        }
    }
    public static class TermTransInfo {
        String db, category, name;
        ArrayList<String> trans;
        
        public TermTransInfo(String db, String category, String name, ArrayList<String> trans) {
            this.db = db;
            this.category = category;
            this.name = name;
            this.trans = trans;
        }
    }
    public static class TransTermsData {
        ArrayList<Term> terms;
        ArrayList<HashMap<String, Integer>> transTerms;
        
        public TransTermsData(ArrayList<Term> terms, ArrayList<HashMap<String, Integer>> transTerms) {
            this.terms = terms;
            this.transTerms = transTerms;
        }
    }
    public static class ChromoDistributionData {
        int genes;
        int trans;
        int prots;
        public ChromoDistributionData(int genes, int trans, int prots) {
            this.genes = genes;
            this.trans = trans;
            this.prots = prots;
        }
    }
    // the idValues field has a format of "n:Xstr-end;Xstr-end..."
    // where n is the number of entries for the id where idValues was retrieved from,
    // X is the type of position: T - transcript, P - protein, G - genomic, N - none
    // str is the starting position and end is the ending position for T/P/G only
    // there can be one or more ids in the same transcript and it's reflected in the count value
    public static class TransFeatureIdValues {
        public static enum PosType {NONE, TRANS, PROTEIN, GENOMIC};
        
        int count;
        ArrayList<IdPosition> lstPos;
        public TransFeatureIdValues(String idValues) {
            count = TransFeatureIdValues.getCount(idValues);
            lstPos = getPositions(idValues);
        }
        @Override
        public String toString() {
            String idValues = count + ":";
            boolean first = true;
            for(IdPosition ip : lstPos) {
                idValues += (first? "" : ";") + getPosType(ip.posType);
                if(!ip.posType.equals(PosType.NONE))
                    idValues += ip.pos.start + "-" + ip.pos.end;
            }
            return idValues;
        }
        
        // static support functions
        public static int getCount(String idValues) {
            int cnt = 0;
            int idx = idValues.indexOf(":");
            if(idx != -1) {
                String cntval = idValues.substring(0, idx);
                cnt = Integer.parseInt(cntval);
            }
            return cnt;
        }
        public static PosType getPosType(char ptc) {
            PosType pt = PosType.NONE;
            switch(ptc) {
                case 'T':
                    pt = PosType.TRANS;
                    break;
                case 'P':
                    pt = PosType.PROTEIN;
                    break;
                case 'G':
                    pt = PosType.GENOMIC;
                    break;
            }
            return pt;
        }
        public static char getPosType(PosType pt) {
            char ptc = 'N';
            switch(pt) {
                case TRANS:
                    ptc = 'T';
                    break;
                case PROTEIN:
                    ptc = 'P';
                    break;
                case GENOMIC:
                    ptc = 'G';
                    break;
            }
            return ptc;
        }
        public static ArrayList<IdPosition> getPositions(String idValues) {
            ArrayList<IdPosition> lstPos = new ArrayList<>();
            int idx = idValues.indexOf(":");
            if(idx != -1) {
                String[] positions = idValues.substring(idx+1).split(";");
                for(String pos : positions) {
                    PosType pt = getPosType(pos.charAt(0));
                    if(!pt.equals(PosType.NONE)) {
                        // generate exception if invalid range
                        String posval = pos.substring(1);
                        String[] range = posval.split("-");
                        lstPos.add(new IdPosition(pt, new SeqAlign.Position(Integer.parseInt(range[0].trim()), Integer.parseInt(range[1].trim()))));
                    }
                    else
                        lstPos.add(new IdPosition(pt, new SeqAlign.Position(0, 0)));
                }
            }
            return lstPos;
        }
    }
    public static class IdPosition {
        TransFeatureIdValues.PosType posType;
        SeqAlign.Position pos;
        public IdPosition(TransFeatureIdValues.PosType posType, SeqAlign.Position pos) {
            this.posType = posType;
            this.pos = pos;
        }
        // WARNING: calling code must handle dealing with protein ids in non-coding transcripts if relevant
        //          this function will return a position of (0,0) in those cases and also if not in transcript range
        public SeqAlign.Position getGenomicPosition(TransData td) {
            ArrayList<SeqAlign.Position> lstPositions;

            // return current position if genomic or none (0,0)
            if(posType.equals(TransFeatureIdValues.PosType.GENOMIC) || posType.equals(TransFeatureIdValues.PosType.NONE))
                return pos;
            else if(posType.equals(TransFeatureIdValues.PosType.TRANS)) {
                // make sure annotation file provided exons
                if(!td.lstExons.isEmpty()) {
                    lstPositions = SeqAlign.getFeatureExons(td.negStrand, pos, td.lstExons);
                    if(!lstPositions.isEmpty()) {
                        SeqAlign.Position pexon = lstPositions.get(0);
                        int min = pexon.start;
                        int max = pexon.end;
                        for(SeqAlign.Position ep : lstPositions) {
                            if(ep.start < min)
                                min = ep.start;
                            if(ep.end > max)
                                max = ep.end;
                        }
                        return(new SeqAlign.Position(min, max));
                    }
                }
            }
            else if(posType.equals(TransFeatureIdValues.PosType.PROTEIN)) {
                // make sure annotation file provided exons and this transcript is coding
                if(!td.lstExons.isEmpty() && td.coding) {
                    // if CDS starts at 10 and pos.start = pos.end = 1, then pos is 10-12, 3 NTs from start of CDS, inclusive
                    SeqAlign.Position cdspos = new SeqAlign.Position(((pos.start - 1) * 3) + td.posCDS.start, (pos.end * 3 - 1) + td.posCDS.start);
                    lstPositions = SeqAlign.getFeatureExons(td.negStrand, cdspos, td.lstExons);
                    if(!lstPositions.isEmpty()) {
                        SeqAlign.Position pexon = lstPositions.get(0);
                        int min = pexon.start;
                        int max = pexon.end;
                        for(SeqAlign.Position ep : lstPositions) {
                            if(ep.start < min)
                                min = ep.start;
                            if(ep.end > max)
                                max = ep.end;
                        }
                        return(new SeqAlign.Position(min, max));
                    }
                }
            }
            
            // any left over will end up here
            return(new SeqAlign.Position(0,0));
        }
        // WARNING: calling code must handle dealing with protein ids in non-coding transcripts if relevant
        //          this function will return an empty list of positions in those cases
        public ArrayList<SeqAlign.Position> getGenomicExonPositions(TransData td) {
            ArrayList<SeqAlign.Position> lstPositions = new ArrayList<>();
            
            // return current position if genomic or none (0,0)
            if(posType.equals(TransFeatureIdValues.PosType.GENOMIC) || posType.equals(TransFeatureIdValues.PosType.NONE)) {
                lstPositions.add(pos);
            }
            else if(posType.equals(TransFeatureIdValues.PosType.TRANS)) {
                // make sure annotation file provided exons
                if(!td.lstExons.isEmpty())
                    lstPositions = SeqAlign.getFeatureExons(td.negStrand, pos, td.lstExons);
            }
            else if(posType.equals(TransFeatureIdValues.PosType.PROTEIN)) {
                // make sure annotation file provided exons and this transcript is coding
                if(!td.lstExons.isEmpty() && td.coding) {
                    // if CDS starts at 10 and pos.start = pos.end = 1, then pos is 10-12, 3 NTs from start of CDS, inclusive
                    SeqAlign.Position cdspos = new SeqAlign.Position(((pos.start - 1) * 3) + td.posCDS.start, (pos.end * 3 - 1) + td.posCDS.start);
                    lstPositions = SeqAlign.getFeatureExons(td.negStrand, cdspos, td.lstExons);
                }
            }
            
            return lstPositions;
        }
    }

    //
    // Annotation Features Functions
    //
    
    public HashMap<String, HashMap<String, Object>> getAnnotFeatures() {
        loadAFStats();
        HashMap<String, HashMap<String, Object>> hmSF = new HashMap<>();
        for(String source : hmAFStats.keySet()) {
            HashMap<String, Object> hm = new HashMap<>();
            HashMap<String, AFStatsData> hmf = hmAFStats.get(source);
            for(String feature : hmf.keySet())
                hm.put(feature, null);
            hmSF.put(source, hm);
        }
        return hmSF;
    }
    public HashMap<String, Object> getSourceFeatures(String source) {
        loadAFStats();
        HashMap<String, Object> hmFeatures = new HashMap<>();
        if(hmAFStats.containsKey(source)) {
            HashMap<String, AFStatsData> hmf = hmAFStats.get(source);
            for(String feature : hmf.keySet())
                hmFeatures.put(feature, null);
        }
        return hmFeatures;
    }
    public HashMap<String, Object> getSourceFeaturesPosition(String source) {
        loadAFStatsPositional();
        HashMap<String, Object> hmFeatures = new HashMap<>();
        if(hmAFStatsPositional.containsKey(source)) {
            ArrayList<String> hmf = hmAFStatsPositional.get(source);
            for(String feature : hmf)
                hmFeatures.put(feature, null);
        }
        return hmFeatures;
    }
    public HashMap<String, Object> getSourceFeaturesPresence(String source) {
        loadAFStatsPresence();
        HashMap<String, Object> hmFeatures = new HashMap<>();
        if(hmAFStatsPresence.containsKey(source)) {
            ArrayList<String> hmf = hmAFStatsPresence.get(source);
            for(String feature : hmf)
                hmFeatures.put(feature, null);
        }
        return hmFeatures;
    }
    public HashMap<String, HashMap<String, AFStatsData>> getAFStats() {
        loadAFStats();
        return hmAFStats;
    }
    public HashMap<String, ArrayList<String>> getAFStatsPositional() {
        loadAFStatsPositional();
        return hmAFStatsPositional;
    }
    public HashMap<String, ArrayList<String>> getAFStatsPresence() {
        loadAFStatsPresence();
        return hmAFStatsPresence;
    }
    public HashMap<String, HashMap<String, String>> getAFStatsType() {
        loadAFStatsType();
        return hmAFStatsType;
    }
    public HashMap<String, AFStatsData> getSourceFeaturesStats(String source) {
        loadAFStats();
        HashMap<String, AFStatsData> hmFeatures = new HashMap<>();
        if(hmAFStats.containsKey(source))
            hmFeatures = hmAFStats.get(source);
        return hmFeatures;
    }
    public HashMap<String, AFStatsData> getSummaryAFStatsData(String sourceFilter, boolean includeReserved) {
        HashMap<String, AFStatsData> hm = new HashMap<>();
        loadAFStats();
        return hm;
    }
    // load  annotation features stats data if not already done
    private void loadAFStats() {
        if(hmAFStats.isEmpty())
            hmAFStats = project.data.loadAFStats();
    }
    // load annotation positional features stats data if not already done
    private void loadAFStatsPositional() {
        if(hmAFStatsPositional.isEmpty())
            hmAFStatsPositional = project.data.loadAFStatsPositional();
    }
    private void loadAFStatsPresence() {
        if(hmAFStatsPresence.isEmpty())
            hmAFStatsPresence = project.data.loadAFStatsPresence();
    }
    private void loadAFStatsType() {
        if(hmAFStatsType.isEmpty())
            hmAFStatsType = project.data.loadAFStatsType();
    }
    // load transcript annotation feature ids data if not already done
    public boolean isTransAFIdPosLoaded() {
        return !hmTransAFIdPos.isEmpty();
    }
    public void loadTransAFIdPos(HashMap<String, HashMap<String, Object>> hmFeatures) {
        if(hmTransAFIdPos.isEmpty())
           hmTransAFIdPos = project.data.loadTransAFIdPos();
    }
    public void loadTransAFIdPos() {
        if(hmTransAFIdPos.isEmpty()) {
            logger.logDebug("Loading TransAFIdPos...");
            hmTransAFIdPos = project.data.loadTransAFIdPos();
            logger.logDebug("TransAFIdPos loading completed.");
        }
    }
    public void loadTransAFIdGenPos() {
        if(hmTransAFIdGenPos.isEmpty()) {
            logger.logDebug("Loading TransAFIdPos...");
            hmTransAFIdGenPos = project.data.loadTransAFIdGenPos();
            logger.logDebug("TransAFIdPos loading completed.");
        }
    }

    //
    // Data Classes
    //

    public static class AnnotSourceStats {
        int transCounts, geneCounts;
        public AnnotSourceStats(int transCounts, int geneCounts) {
            this.transCounts = transCounts;
            this.geneCounts = geneCounts;
        }
    }
    public static class AFStatsTblData implements Comparable<AFStatsTblData> {
        public final SimpleStringProperty source;
        public final SimpleStringProperty feature;
        public final SimpleIntegerProperty count;
        public final SimpleIntegerProperty idCount;
        public final SimpleIntegerProperty transCount;
 
        public AFStatsTblData(String source, String feature, int count, int idCount, int transCount) {
            this.source = new SimpleStringProperty(source);
            this.feature = new SimpleStringProperty(feature);
            this.count = new SimpleIntegerProperty(count);
            this.idCount = new SimpleIntegerProperty(idCount);
            this.transCount = new SimpleIntegerProperty(transCount);
        }
        public String getSource() { return source.get(); }
        public String getFeature() { return feature.get(); }
        public Integer getCount() { return count.get(); }
        public Integer getIdCount() { return idCount.get(); }
        public Integer getTransCount() { return transCount.get(); }
        @Override
        public int compareTo(AFStatsTblData td) {
            if (source.get().equals(td.source.get()))
                return (feature.get().compareToIgnoreCase(td.feature.get()));
            else
                return (source.get().compareToIgnoreCase(td.source.get()));
        }
    }
    public static class AnnotFeatureDef {
        AnnotFeature id;
        String source, feature;
        public AnnotFeatureDef(AnnotFeature id, String source, String feature) {
            this.id = id;
            this.source = source;
            this.feature = feature;
        }
    }
    public static class AnnotFeatureData {
        String name, desc;
        int count;
        public AnnotFeatureData(String name, String desc, int count) {
            this.name = name;
            this.desc = desc;
            this.count = count;
        }
    }
}
