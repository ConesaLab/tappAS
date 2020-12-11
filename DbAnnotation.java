/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.ProgressBar;
import tappas.DataAnnotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static tappas.DataAnnotation.*;
import static tappas.DbProject.tblAFIdStats;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DbAnnotation  extends DbBase {
    public static final int dbVerMajor = 0;
    public static final int dbVerMinor = 9;
    public static final String tblDBInfo = "tblDBInfo";
    public static final String tblAnnotations = "tblAnnotations";
    public static final String tblAnnotFeatures = "tblAnnotFeatures";

    // SQL statements
    private final String sqlSelStructuralInfo = "SELECT SeqName, Feature, Start, End, Strand, Length FROM " + tblAnnotations + " WHERE SeqName IN(Select SeqName From " + tblAnnotations + " Where Source='tappAS' and Feature LIKE 'CDS') and Source='tappAS' and (Feature LIKE 'CDS' OR Feature LIKE 'transcript') ORDER BY SeqName, Feature DESC;";
    private final String sqlSelTranscriptGenomicPosition = "SELECT SeqName, Start, End FROM " + tblAnnotations + " WHERE Feature LIKE 'exon'";
    private static final String sqlCreateDBInfo = "BEGIN;" +
            " CREATE TABLE " + tblDBInfo + " (" +
            " Filepath             TEXT, " +
            " VerMajor              INT, " +
            " VerMinor              INT" +
            " );" +
            " COMMIT;";
    private final String sqlInsDBInfo = "INSERT INTO " + tblDBInfo + "(Filepath, VerMajor, VerMinor) values('?'," + dbVerMajor + "," + dbVerMinor + ");";
    private final String sqlSelDBInfo = "SELECT * FROM " + tblDBInfo + ";";
    private static final String sqlCreateAnnotFeatures = "BEGIN;" +
            " CREATE TABLE " + tblAnnotFeatures + " (" +
            " Source          TEXT NOT NULL, " +
            " Feature         TEXT NOT NULL" +
            " );" +
            " CREATE INDEX SourceStats_idx ON " + tblAnnotFeatures + "(Source); " +
            " CREATE INDEX FeatureStats_idx ON " + tblAnnotFeatures + "(Feature); " +
            " COMMIT;";
    private static final String sqlInsAnnotFeatures = "INSERT INTO " + tblAnnotFeatures + "(Source, Feature) " +
            " SELECT DISTINCT Source, Feature FROM " + tblAnnotations + ";";
    private static final String sqlSelAnnotFeatures = "SELECT * FROM " + tblAnnotFeatures + " ORDER BY Source, Feature;";
    private static final String sqlCreateAnnotations = "BEGIN;" +
            " CREATE TABLE " + tblAnnotations + " (" +
            // must be able to retrieve rows back in original file order - use row id
            " RowId                INT     NOT NULL PRIMARY KEY, " +   // 1
            " SeqName              TEXT    NOT NULL, " +               // 2
            " Source               TEXT    NOT NULL, " +               // 3
            " Feature              TEXT    NOT NULL, " +               // 4
            " Start                INT, " +                            // 5
            " End                  INT, " +                            // 6
            " Strand               TEXT, " +                           // 7
            " Attrs                TEXT, " +                           // 8
            // extracted major attributes - some columns may be empty for some features
            " Id                   TEXT, " +                           // 9
            " Name                 TEXT, " +                           // 10
            " Desc                 TEXT, " +                           // 11
            // inferred/calculated data
            " PosType              TEXT, " +                           // 12
            " Length               INT" +                              // 13
            " );" +
            " CREATE INDEX SeqName_idx ON " + tblAnnotations + "(SeqName); " +
            " CREATE INDEX Source_idx ON " + tblAnnotations + "(Source); " +
            " CREATE INDEX Feature_idx ON " + tblAnnotations + "(Feature); " +
            " COMMIT;";
    private final String sqlInsAnnotations = "INSERT INTO " + tblAnnotations + "(RowId, SeqName, Source, Feature, Start, End, Strand, Attrs, Id, Name, Desc, PosType, Length) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private final String sqlSelTrans = "SELECT DISTINCT(SeqName) FROM " + tblAnnotations;
    //private final String sqlSelTransStrand = "SELECT DISTINCT(SeqName), Strand FROM " + tblAnnotations + " WHERE Strand LIKE '+' OR Strand LIKE '-';";
    private final String sqlSelTransStrand = "SELECT DISTINCT(T1.SeqName), T1.Strand, T2.Start, T2.End FROM " +  tblAnnotations + " as T1 LEFT JOIN (SELECT * FROM " + tblAnnotations + " as T3 WHERE T3.Feature LIKE 'CDS') as T2 ON (T1.SeqName = T2.SeqName) WHERE T1.Strand LIKE '+' OR T1.Strand LIKE '-';";
    private final String sqlSelTransWithCDS = "SELECT DISTINCT(SeqName), Strand, Start, End FROM " +  tblAnnotations + " WHERE Feature LIKE 'CDS' AND Start-End !=0;";
    private final String sqlSelTransLength = "SELECT SeqName, Length FROM " + tblAnnotations + " WHERE Feature = 'transcript'";
    private final String sqlSelLengthAnnotations = "SELECT max(rowId) FROM " + tblAnnotations + ";";
    private final String sqlSelExistStructural = "SELECT SeqName FROM " + tblAnnotations + " WHERE Source LIKE '" + STRUCTURAL_SOURCE + "' LIMIT 1";

    //Structural_Information|3UTR_Length|T
    //COILS|COILED|P
    //Source|Feature|PosType (Transcript, Protein, Genomic or None)
    private static final String sqlGetFeatureType = "SELECT DISTINCT Source, Feature, PosType FROM " + tblAnnotations + " WHERE Source NOT LIKE 'tappAS';";
    //Get Positional Features DFI or FDA
    private static final String sqlGetFeaturePosition = "SELECT DISTINCT Source, Feature, PosType FROM " + tblAnnotations + " WHERE Start NOT LIKE '0' and End NOT LIKE '0' AND Source NOT LIKE 'tappAS'";
    //Get Presence Features FDA
    private static final String sqlGetFeaturePresence = "SELECT DISTINCT Source, Feature, PosType FROM " + tblAnnotations + " WHERE PosType NOT LIKE 'N' AND Source NOT LIKE '" + STRUCTURAL_SOURCE + "' AND Source NOT LIKE 'tappAS';";
    //private final String sqlGetFeaturePresence = "SELECT * FROM " + tblAnnotations + ";";
    private static final String sqlSelAFIdStats = "SELECT * FROM " + tblAFIdStats + ";";
    private static final String sqlSelAFIdStatsByFeature = "SELECT * FROM " + tblAFIdStats + " WHERE Feature LIKE '?';";
    private static final String sqlSelAFIdStatsBySource = "SELECT * FROM " + tblAFIdStats + " WHERE Source LIKE '?';";
    private static final String sqlSelAFStatsBySourceAndFeature = "SELECT * FROM " + tblAFIdStats + " WHERE Source LIKE '?' AND Feature LIKE '!';";

    // class data
    private String folder = "";
    private String filepath = "";
    private int verMajor = 0;
    private int verMinor = 0;

    // class instantiation
    public DbAnnotation(Project project) {
        super(project);
    }

    // open DB, DB file must already exist - call createDB if not
    public boolean openDB(String dbpath) {
        boolean result = false;
        this.dbpath = dbpath;

        if(open()) {
            try {
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(sqlSelDBInfo);
                if(rs.next()) {
                    // WARNING: once a DB is created, the file can no longer be expected to exist
                    //          it is just a reference and currently is not shown anywhere
                    filepath = rs.getString("Filepath");
                    verMajor = rs.getInt("VerMajor");
                    verMinor = rs.getInt("VerMinor");

                    // check if same major version, if not then DB structure changed - must decide what to do
                    // fail this call for now
                    if(verMajor == dbVerMajor)
                        result = true;
                }
                stmt.close();
                rs.close();

            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }
        return result;
    }
    // Create database and create/populate initial tables
    // Should only be called when a new user defined annotation file is requested
    // This database is read only once it is created - the project database has the project specific data
    public boolean createDB(String folder, String dbpath, String filepath, double offset, double featuresPart, ProgressBar pbProgress) {
        boolean result = false;
        this.folder = folder;
        this.dbpath = dbpath;
        this.filepath = filepath;
        this.verMajor = dbVerMajor;
        this.verMinor = dbVerMinor;

        // remove existing database if present
        Utils.removeFile(Paths.get(dbpath));
        if(open()) {
            // create tables
            if(createTable(tblDBInfo, sqlCreateDBInfo) && createTable(tblAnnotations, sqlCreateAnnotations) &&
                    createTable(tblAnnotFeatures, sqlCreateAnnotFeatures)) {
                double val = offset + 0.05 * featuresPart;
                app.ctls.updateProgress(pbProgress, val);
                if(populateDBInfo()) {
                    val = offset + 0.1 * featuresPart;
                    app.ctls.updateProgress(pbProgress, val);
                    if(populateAnnotations(filepath, val, (0.9 * featuresPart), pbProgress)) {
                        val = offset + featuresPart;
                        app.ctls.updateProgress(pbProgress, val);
                        result = true;
                    }
                }
            }
        }

        // remove db file if not successful
        if(!result)
            Utils.removeFile(Paths.get(dbpath));
        return result;
    }
    public HashMap<String, HashMap<String, Object>> loadAnnotFeatures(){
        // source:feature
        HashMap<String, HashMap<String, Object>> hmResults = new HashMap<>();

        ResultSet rs = getRS(sqlSelAnnotFeatures);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                logger.logDebug("Processing annotation features...");

                try {
                    String source;
                    String dbsource, dbfeature;
                    source = "";
                    while(rs.next()) {
                        dbsource = rs.getString(1);
                        dbfeature = rs.getString(2);

                        // check if not the same source
                        if(!dbsource.equals(source)) {
                            source = dbsource;
                            HashMap<String, Object> hmFeature = new HashMap<>();
                            hmResults.put(source, hmFeature);
                        }
                        hmResults.get(dbsource).put(dbfeature, null);
                    }
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    logger.logDebug("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println("RS close exception within exception" + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                    // close database connection - something is messed up
                    close();
                }
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }

    public boolean existStructural(){
        boolean result = false;
        ResultSet rs = getRS(sqlSelExistStructural);
        try{
            if(rs.isBeforeFirst())
                result = true;
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return result;
    }

    public boolean genTransLengthFile(String folder){
        boolean result = false;
        System.out.println("genTransLengthFile(" + folder + ")");
        HashMap<String, Integer> hmTransLength = new HashMap<>();
        ResultSet rs = getRS(sqlSelTransLength);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                logger.logDebug("Loading transcripts length...");

                try {
                    while(rs.next())
                        hmTransLength.put(rs.getString("SeqName"), rs.getInt("Length"));
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    logger.logDebug("Total time: " + duration + " ms");

                    result = writeTransLengthFile(folder, hmTransLength);
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println("RS close exception within exception" + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return result;
    }

    public boolean getStructureFile(String folder, HashMap<String, List<Long>> genomicPos){
        boolean result = false;
        System.out.println("structuralFile(" + folder + ")");
        //HashMap<String, Integer> hmStructural = new HashMap<>();
        List<String> lsStructural = new ArrayList<>();
        lsStructural.add("#SeqName" +"\t"+ "Length3" +"\t"+ "Length5" +"\t"+ "LengthCDS" +"\t"+ "PosPAS" + "\t" + "TotalLength");

        ResultSet rs = getRS(sqlSelStructuralInfo);
        int contFiltered=0;
        int contCDS=0;
        try{
            if(rs.isBeforeFirst()) {
                logger.logDebug("Loading structural information...");
                try {
                    while(rs.next()){
                        int length5 = 0, length3 = 0, lengthTrans = 0, lengthCDS = 0, totalLength = 0;
                        long posPAS = -1;
                        if(rs.getString("Feature").equals("transcript")){
                            lengthTrans = rs.getInt("Length");
                            if(rs.next()){
                                if(rs.getString("Feature").equals("CDS")){ //next row is CDS (2 by 2 in while loop)
                                    //both Strand work identically
                                    length5 = rs.getInt("Start")-1;
                                    length3 = lengthTrans - rs.getInt("End");
                                    lengthCDS = rs.getInt("Length");
                                    totalLength = length5 + length3 + lengthCDS;
                                    if(genomicPos.get(rs.getString("SeqName"))!=null){
                                        //genomic pos posPAS = genomicPos.get(rs.getString("SeqName")).get(1);
                                        posPAS = totalLength;
                                        //save info
                                        lsStructural.add(rs.getString("SeqName") +"\t"+ String.valueOf(length3) +"\t"+ String.valueOf(length5) +"\t"+ String.valueOf(lengthCDS) +"\t"+ String.valueOf(posPAS) +"\t"+ String.valueOf(totalLength));
                                        //if not have PAS there are filtered
                                    }else{
                                        contFiltered++;
                                    }
                                }else{
                                    contCDS++;
                                }
                            }
                        }
                    }
                    rs.close();

                    result = writeStructureFile(folder, lsStructural);
                    //result = writeTransLengthFile(folder, hmStructural); //use other method to write but it works!!!
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println("RS close exception within exception" + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return result;
    }

    // get TranscriptGenomicPosition just MIN and MAX pos!
    public HashMap<String, List<Long>> getTranscriptGenomicPosition(){
        HashMap<String, List<Long>> hmPositions = new HashMap<>();
        ResultSet rs = getRS(sqlSelTranscriptGenomicPosition);
        try{
            if(rs.isBeforeFirst()) {
                try {
                    while(rs.next()){
                        long posIni = rs.getLong("Start");
                        long posEnd = rs.getLong("End");
                        List<Long> lst = new ArrayList<>();
                        lst.add(posIni);
                        lst.add(posEnd);
                        if(hmPositions.get(rs.getString("SeqName"))!=null){
                            //check min and max
                            List<Long> old_list = hmPositions.get(rs.getString("SeqName"));
                            if(old_list.get(0)>posIni){
                                old_list.set(0, posIni);
                            }
                            if(old_list.get(1)<posEnd){
                                old_list.set(1, posEnd);
                            }
                            hmPositions.put(rs.getString("SeqName"), old_list);
                        }else{
                            //System.out.println(rs.getString("SeqName"));
                            hmPositions.put(rs.getString("SeqName"), lst);
                        }
                    }
                    rs.close();
                }catch ( Exception e ) {
                    hmPositions.clear();
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmPositions;
    }

    public String insertNewAnnotSQL(String listTrans){
        //delete last ","
        logger.logDebug("Adding new Annotation to database...");
        String aux=listTrans.substring(0, listTrans.length()-1);
        String res = "INSERT INTO " + tblAnnotations + " VALUES " + aux + ";";
        return res;
    }

    public String insertNewFeatures(String feature){
        logger.logDebug("Adding new Feature to database...");
        String res = "INSERT INTO " + tblAnnotFeatures + " VALUES ('" + STRUCTURAL_SOURCE + "', '" + feature + "');";
        return res;
    }

    // This method allow insert polyASite, 3'UTR length, 5'UTR length, CDS length to annotations.db to generate project.db correctly
    // the integer i is used to select the feature to load
    // 1=3UTR length, 2 = 5UTR length, 3=CDS length, 4=polyASite
    public boolean insertNewAnnot(int i){
        boolean result = false;
        ResultSet rs = getRS(sqlSelTransWithCDS);
        String lstTrans = "";
        String feature = "";
        String length = null, start = null, end = null, desc = "", total = "";
        if(1<=i && i<=4){
            switch(i){
                case(1): feature = UTRLENGTH3_FEATURE; desc = "3UTR Length"; break;
                case(2): feature = UTRLENGTH5_FEATURE; desc = "5UTR Length";break;
                case(3): feature = CDS_FEATURE; desc = "CDS Length";break;
                case(4): feature = POLYA_FEATURE; desc = "PolyA Site";break;
                default: break;
            }
            try{
                if(rs.isBeforeFirst()) {
                    try {
                        ResultSet rsLength = getRS(sqlSelLengthAnnotations);
                        int count = rsLength.getInt(1);
//                        int cc = 0;
//                        double div = 0.0;
//                        double eq = 0.1;
                        HashMap<String, String> genomicPos = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),i,true);
                        HashMap<String, String> totalLength = Utils.loadGenomicPositions(project.data.getStructuralInfoFilePath(),5,true);
                        System.out.println("Processing GTF information to generate " + feature + " database row...");
                        while(rs.next()){
                            //count transcripts processed
//                            cc++;
//                            div = (double) Math.round((cc/genomicPos.size()) * 100) / 100;
//                            if(div > eq){
//                                System.out.println("Transcripts processed: " + c + "/" + genomicPos.size());
//                                eq = eq + 0.1;
//                            }
                            count++;
                            //check if is coding
                            String seq = rs.getString("SeqName");


                            switch(i){
                                case(1): length = genomicPos.get(rs.getString("SeqName"));
                                    total = totalLength.get(rs.getString("SeqName"));
                                    if(length==null)
                                        continue;
                                    start = Integer.toString(Integer.parseInt(rs.getString("End"))+1);
                                    end = total;
                                    break;
                                case(2): length = genomicPos.get(rs.getString("SeqName"));
                                    if(length==null)
                                        continue;
                                    start = "1";
                                    end = Integer.toString(Integer.parseInt(rs.getString("Start"))-1);
                                    break;
                                case(3): length = genomicPos.get(rs.getString("SeqName"));
                                    if(length==null)
                                        continue;
                                    start = rs.getString("Start");
                                    end = rs.getString("End");
                                    break;
                                case(4): length = "1";
                                    //genomic pos start = genomicPos.get(rs.getString("SeqName"));
                                    //genomic pos end = genomicPos.get(rs.getString("SeqName"));
                                    start = end = totalLength.get(rs.getString("SeqName")); // transcript length
                                    if(start==null)
                                        continue;
                                    break;
                            }

                            lstTrans = lstTrans + "('"+  String.valueOf(count)+"','"+ //rowID
                                    rs.getString("SeqName")+"','"+ //SeqName
                                    STRUCTURAL_SOURCE + "','"+ //Source
                                    feature + "','"+ //Feature
                                    start +"','"+ //Start
                                    end +"','"+ //End
                                    rs.getString("Strand")+"','"+ //Strand
                                    "ID="+desc+"; "+"Name="+desc+"; Desc="+desc+"','"+ //Atrib
                                    desc+"','"+ //Id
                                    desc+"','"+ //Name
                                    desc+"','"+ //Desc
                                    "T','"+ //posType
                                    length + "'),"; //length
                            /*System.out.println("('"+  String.valueOf(count)+"','"+ //rowID
                                                        rs.getString("SeqName")+"','"+ //SeqName
                                                        "Structural_Information','"+ //Source
                                                        feature + "','"+ //Feature
                                                        start +"','"+ //Start
                                                        end +"','"+ //End
                                                        rs.getString("Strand")+"','"+ //Strand
                                                        "ID="+desc+"; "+"Name="+desc+"; Desc="+desc+"','"+ //Atrib
                                                        desc+"','"+ //Id
                                                        desc+"','"+ //Name
                                                        desc+"','"+ //Desc
                                                        "T','"+ //posType
                                                        length + "')" //length
                            );*/
                        }
                        System.out.println("Inserting " + feature + " information to database...");
                        //insert all transcripts
                        Statement stmt = c.createStatement();
                        int insertNewAnnot = stmt.executeUpdate(insertNewAnnotSQL(lstTrans));
                        if(insertNewAnnot > 0){
                            result = true;
                        }else{
                            result = false;
                            logger.logError("Unable to add annotation DB information." );
                        }
                        //insert source
                        stmt = c.createStatement();
                        int insertNewFeatures = stmt.executeUpdate(insertNewFeatures(feature));
                        if(insertNewFeatures > 0){
                            result = true;
                        }else{
                            result = false;
                            logger.logError("Unable to add source DB information." );
                        }
                        //Close DB
                        rs.close();
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println("RS close exception within exception" + ce.getMessage()); }
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
            logger.logInfo("Insertion of feature: '"+ feature +"' was successful.");
        }

        return result;
    }

    //Get Feature Information
    public HashMap<String, ArrayList<String>> loadAFStatsPositional(){
        ResultSet rs = getRS(sqlGetFeaturePosition);
        HashMap<String, ArrayList<String>> hmResults = new HashMap<String, ArrayList<String>>();
        //Get Positional Source and Id
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing positional features...");
                String source, feature;
                try {
                    while(rs.next()) {
                        ArrayList<String> feat = new ArrayList<String>();
                        source = rs.getString(1);
                        feature = rs.getString(2);
                        if(hmResults.containsKey(source)){
                            feat = hmResults.get(source);
                            if(!feat.contains(feature)){
                                feat.add(feature);
                                hmResults.put(source, feat);
                            }
                        }
                        else {
                            feat.add(feature);
                            hmResults.put(source, feat);
                        }
                    }
                    rs.close();
                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch (SQLException ex) {
                    System.out.println("Query hasn't results, error: " + ex);
                }
                close();
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }

    public HashMap<String, ArrayList<String>> loadAFStatsPresence(){
        ResultSet rs = getRS(sqlGetFeaturePresence);
        HashMap<String, ArrayList<String>> hmResults = new HashMap<String, ArrayList<String>>();
        //Get Positional Source and Id
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing presence features...");
                String source, feature;
                try {
                    while(rs.next()) {
                        ArrayList<String> feat = new ArrayList<String>();
                        source = rs.getString(1);
                        feature = rs.getString(2);
                        if(hmResults.containsKey(source)){
                            feat = hmResults.get(source);
                            if(!feat.contains(feature)){
                                feat.add(feature);
                                hmResults.put(source, feat);
                            }
                        }
                        else {
                            feat.add(feature);
                            hmResults.put(source, feat);
                        }
                    }
                    rs.close();
                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch (SQLException ex) {
                    System.out.println("Query hasn't results, error: " + ex);
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }

    public HashMap<String, HashMap<String, String>> loadAFStatsType(){
        HashMap<String, HashMap<String, String>> hmResults = new HashMap<>();
        ResultSet rs = getRS(sqlGetFeatureType);
        //Get Positional Source and Id
        try{
            if(rs.isBeforeFirst()) {
                String source, feature, type;
                try {
                    while(rs.next()) {
                        HashMap<String, String> hmType = new HashMap<>();
                        source = rs.getString(1);
                        feature = rs.getString(2);
                        type = rs.getString(3);
                        if(hmResults.containsKey(source)){
                            HashMap<String, String> hmAux = hmResults.get(source);
                            hmAux.put(feature, type);
                            hmResults.put(source, hmAux);
                        }else{
                            hmType.put(feature, type);
                            hmResults.put(source, hmType);
                        }
                    }
                } catch (SQLException ex) {
                    System.out.println("Query hasn't results, error: " + ex);
                }
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }

    //
    // Internal Functions
    //

    // populate DB information table
    private boolean populateDBInfo() {
        boolean result = false;
        long tstart = System.nanoTime();

        logger.logDebug("Populating database information table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsDBInfo.replace("?", filepath));
                logger.logDebug("Inserted " + rowcnt + " rows into " + tblDBInfo);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("Unable to add annotation DB information record." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        logger.logDebug("Total time: " + duration + " ms");
        return result;
    }

    // populate main annotations table
    private boolean populateAnnotations(String filepath, double offset, double featuresPart, ProgressBar pbProgress) {
        boolean result = false;
        long tstart = System.nanoTime();

        AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
        logger.logDebug("Populating annotations table from: " + filepath);
        if(open()) {
            HashMap<String, Integer> hmTransLength = new HashMap<>();
            BufferedReader br = null;
            try {
                c.setAutoCommit(false);
                PreparedStatement ps = c.prepareStatement(sqlInsAnnotations);
                int lnum = 0;
                int batchcnt = 0;
                String line;
                String[] fields;
                int rowid, start, end, length;
                boolean noPos;
                char posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.TRANS);
                String seqName, source, feature, strand, id, name, desc;
                HashMap<String, String> attrs;
                File f = new File(filepath);
                long size = f.length();
                long nrows = size / 80;    // approximation of total number of rows
                logger.logDebug("Approximate Rows: " + nrows + ", ProgressOffset: " + offset + ", ProgressPart: " + featuresPart);
                br = Files.newBufferedReader(Paths.get(filepath), StandardCharsets.UTF_8);
                boolean errflg = false;
                for (rowid = 1; (line = br.readLine()) != null && !errflg;) {
                    if(!line.trim().isEmpty() && !line.startsWith("#")) {
                        fields = line.split("\t");
                        if(fields.length == 9) {
                            // set seqName, Source and Feature values
                            seqName = fields[0];
                            source = fields[1];
                            feature = fields[2];
//                            if(feature.equals("PTM"))
//                                System.out.println("Hi");
                            ps.setInt(1, rowid++);
                            ps.setString(2, seqName);
                            ps.setString(3, source);
                            ps.setString(4, feature);
                            if(hasInvalidChars(seqName) || hasInvalidChars(source) || hasInvalidChars(feature)) {
                                logger.logError("Invalid annotation line (invalid character used for seqName/source/feature). Line number: " + lnum + ".");
                                errflg = true;
                                break;
                            }

                            // set position related valued
                            start = end = 0;
                            try { start = Integer.parseInt(fields[3]); } catch(Exception e) {}
                            try { end = Integer.parseInt(fields[4]); } catch(Exception e) {}
                            noPos = (start == 0 || end == 0);
                            length = noPos? 0 : (end - start + 1);
                            // new treatment for GTF with posType at the end of Description field
                            //String[] tp = fields[8].split(";");
                            String t = fields[8].substring(fields[8].length()-1);
                            if(DataAnnotation.AnnotFeature.TRANSCRIPT.name().startsWith(t)){
                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.TRANS);
                                hmTransLength.put(seqName, length);
                            }else if(DataAnnotation.AnnotFeature.PROTEIN.name().startsWith(t)){
                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.PROTEIN);
                            }else if(DataAnnotation.AnnotFeature.GENOMIC.name().startsWith(t)){
                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.GENOMIC);
                            }else if(DataAnnotation.AnnotFeature.NONE.name().startsWith(t)){
                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.NONE);
                            }

//                            if(project.data.getRsvdAnnotFeature(source, feature).equals(DataAnnotation.AnnotFeature.TRANSCRIPT)) {
//                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.TRANS);
//                                hmTransLength.put(seqName, length);
//                            }
//                            else if(project.data.getRsvdAnnotFeature(source, feature).equals(DataAnnotation.AnnotFeature.PROTEIN))
//                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.PROTEIN);
//                            else if(project.data.getRsvdAnnotFeature(source, feature).equals(DataAnnotation.AnnotFeature.GENOMIC))
//                                posType = TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.GENOMIC);

                            ps.setInt(5, start);
                            ps.setInt(6, end);
                            ps.setString(12, Character.toString(posType));
//                            ps.setString(12, Character.toString(noPos? TransFeatureIdValues.getPosType(DataAnnotation.TransFeatureIdValues.PosType.NONE) : posType));
                            ps.setInt(13, length);

                            // set strand and attributes
                            strand = fields[6].equals("+") || fields[6].equals("-")? fields[6] : "";
                            ps.setString(7, strand);
                            //We dont want to add the text PosType to transcripts descriptions - just to identify the type on the database
                            int len = fields[8].length() - "PosType=X".length() - 2; // -2 because we want to delete de last "; "
                            ps.setString(8, fields[8].substring(0, len));
                            id = name = desc = "";
                            attrs = DataAnnotation.getAttributes(fields[8].substring(0, len));
                            if(attrs.containsKey(defs.attrId)) {
                                id = attrs.get(defs.attrId);
                                if(hasInvalidIdChars(id, source, feature, defs)) {
                                    logger.logError("Invalid annotation line (invalid character used for ID). Line number: " + lnum + ".\n    " + line + "\n");
                                    errflg = true;
                                    break;
                                }
                            }
                            if(attrs.containsKey(defs.attrName))
                                name = attrs.get(defs.attrName);
                            if(attrs.containsKey(defs.attrDesc))
                                desc = attrs.get(defs.attrDesc);
                            // should we remove the id, name, desc from attrs column once stable?
                            // would need to modify all the code where attrs is used!
                            ps.setString(9, id);
                            ps.setString(10, name);
                            ps.setString(11, desc);
                            ps.addBatch();
                            if ((++batchcnt % 1000) == 0) {
                                ps.executeBatch();
                                batchcnt = 0;
                                app.ctls.updateProgress(pbProgress, (offset + (Math.min(1.0, (double)rowid/(double)nrows) * featuresPart)));
                            }
                        }
                        else {
                            logger.logError("Invalid annotation line. Line number: " + lnum + ".");
                            errflg = true;
                            break;
                        }
                    }
                    if(++lnum % 100000 == 0)
                        logger.logDebug("Processed " + lnum + " lines...");
                }
                //Check if is problematic closing the file!!! (populating annotation file)
                try { br.close(); } catch(IOException e) {
                    // just log a warning, file was read properly
                    logger.logWarning("Closing populating annotation file exception '" + e.getMessage() + "'");
                }

                if(!errflg) {
                    if(batchcnt > 0)
                        ps.executeBatch();
                    c.commit();

                    if(!hmTransLength.isEmpty()) {
                        if(writeTransLengthFile(folder, hmTransLength))
                            result = true;
                    }
                    else{
                        //Warning remove and download again all references
                        logger.logError("1. Error loading gene-transcripts from database\n\nIf you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\\n\\nYou can find more information in our website: app.tappas.org/downloads");
                        app.ctls.alertWarning("1. Error loading gene-transcripts from database", "If you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\n\nYou can find more information in our website: app.tappas.org/download");
                    }
                }
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            finally {
                try { if(c != null) c.setAutoCommit(true); } catch(Exception e) { System.out.println("setAutoCommit exception within exception" + e.getMessage()); }
            }
        }

        if(result) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsAnnotFeatures);
                logger.logDebug("Inserted " + rowcnt + " rows into " + tblAnnotFeatures);
                if(rowcnt <= 0) {
                    result = false;
                    logger.logError("No DB annotation feature records added." );
                }
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }

        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        logger.logDebug("Total time: " + duration + " ms");
        return result;
    }
    // do not allow characters that are used internally to separate fields or cause problems with R scripts
    private boolean hasInvalidChars(String value) {
        // single quotes are allowed, e.g. 3'UTRMotif
        return (value.contains(" ") || value.contains(",") || value.contains(";") || value.contains(":") || value.contains("\""));
    }
    // Note that ":" is an invalid feature id character since it causes issues with DEXSeq ids
    // It is allowed for "GO:" but not for anything else - change if needed but will need to deal with R scripts
    private boolean hasInvalidIdChars(String value, String source, String feature, AnnotationFileDefs defs) {
        boolean invalid = false;
        if(value.contains(" ") || value.contains(",") || value.contains(";") || value.contains("'") || value.contains("\""))
            invalid = true;
        else if(value.contains(":") && !source.equals(defs.srcGO))
            invalid = true;
        return invalid;
    }
    private boolean writeTransLengthFile(String folder, HashMap<String, Integer> hmTransLength) {
        boolean result = false;
        logger.logDebug("Writing annotation transcript length file...");
        String fp = Paths.get(folder, DataProject.TRANS_LENGTHS).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fp), "utf-8"));
            for(String trans : hmTransLength.keySet())
                writer.write(trans + "\t" + hmTransLength.get(trans) + "\n");
            logger.logDebug("Annotation transcript length file written.");
            result = true;
        } catch (IOException e) {
            logger.logError("Annotation transcript length file code exception: " + e.getMessage());
            result = false;
        } finally {
            try { if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception" + e.getMessage()); }
        }
        return result;
    }

    private boolean writeStructureFile(String folder, List<String> lsStructural) {
        boolean result = false;
        logger.logDebug("Writing structural file...");
        String fp = Paths.get(folder, DataProject.STRUCTURAL_INFO).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fp), "utf-8"));
            for(String str_info : lsStructural){
                System.out.println(str_info);
                writer.write(str_info + "\n");
            }
            logger.logDebug("Annotation structural file written.");
            result = true;
        } catch (IOException e) {
            logger.logError("Annotation structural file code exception: " + e.getMessage());
            result = false;
        } finally {
            try { if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception" + e.getMessage()); }
        }
        return result;
    }

    // get all annotation transcripts
    public HashMap<String, Object> loadTrans(){
        HashMap<String, Object> hmResults = new HashMap<>();
        ResultSet rs = getRS(sqlSelTrans);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                logger.logDebug("Loading trans...");

                String gene, trans;
                HashMap<String, Object> hm;
                try {
                    while(rs.next())
                        hmResults.put(rs.getString("SeqName"), null);
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    logger.logDebug("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println("RS close exception within exception" + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }
}