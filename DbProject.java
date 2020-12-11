/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.ProgressBar;
import tappas.DataAnnotation.AnnotSourceStats;

import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DbProject extends DbBase {
    public static final int dbVerMajor = 0;
    public static final int dbVerMinor = 9;
    public static final String dbAnnotation = "dba";
    public static final String tblDBInfo = "tblDBInfo";
    public static final String tblAnnotations = dbAnnotation + "." + DbAnnotation.tblAnnotations;
    public static final String tblProject = "tblProject";
    // these tables keep data associated with all transcripts in result_matrix.tsv
    public static final String tblTransData = "tblTransData";
    public static final String tblTrans = "tblTranscripts";
    public static final String tblGeneTrans = "tblGeneTranscripts";
    public static final String tblTransFiltered = "tblTranscriptsFiltered";
    public static final String tblGeneTransFiltered = "tblGeneTranscriptsFiltered";
    public static final String tblProteins = "tblProteins";
    public static final String tblGenes = "tblGenes";
    public static final String tblAnnotFeatures = "tblAnnotFeatures";
    public static final String tblTransAFIds = "tblTransAFIds";
    // statistics table only take into account data for filtered transcripts - rebuild each time filtering takes place
    public static final String tblAFStats = "tblAnnotFeaturesStats";
    public static final String tblAnnotSourceStats = "tblAnnotSourceStats";
    public static final String tblAFIdStats = "tblAFIdsStats";
    
    // SQL statements
    public static final String preAttachDbA = "ATTACH DATABASE '";
    public static final String postAttachDbA = "' AS dba";
    private static final String sqlCreateDBInfo = "BEGIN;" +
                 " CREATE TABLE " + tblDBInfo + " (" +
                 " DBApath  TEXT NOT NULL, " +
                 " VerMajor INT, " +
                 " VerMinor INT" +
                 " );" + 
                 " COMMIT;";
    private final String sqlInsDBInfo = "INSERT INTO " + tblDBInfo + "(DBApath, VerMajor, VerMinor) values('?'," + dbVerMajor + "," + dbVerMinor + ");";
    private final String sqlSelDBInfo = "SELECT * FROM " + tblDBInfo + ";";
    private static final String sqlCreateAnnotFeatures = "BEGIN;" +
                 " CREATE TABLE " + tblAnnotFeatures + " (" +
                 " Source  TEXT NOT NULL, " +
                 " Feature TEXT NOT NULL," +
                 " Category TEXT," +
                 " PosType TEXT NOT NULL" +
                 " );" +
                 " CREATE INDEX Source_idx ON " + tblAnnotFeatures + "(Source); " +
                 " CREATE INDEX Feature_idx ON " + tblAnnotFeatures + "(Feature); " +
                 " COMMIT;";
    private static final String sqlInsAnnotFeatures = "INSERT INTO " + tblAnnotFeatures + "(Source, Feature, Category, PosType) " +
                 " SELECT DISTINCT Source, Feature, S > 0 and E > 0, PosType FROM " +
                 " (Select Source, Feature, CAST(Start as numeric) as S, CAST(End as numeric) as E, PosType FROM " + tblAnnotations +
                 " WHERE " + tblAnnotations + ".SeqName IN(SELECT Id FROM " + tblTrans + ") " +
                 " AND Source = (SELECT DISTINCT(Source)) ORDER BY Source, Feature);";
    private static final String sqlCreateTransAFIds = "BEGIN;" +
                 " CREATE TABLE " + tblTransAFIds + " (" +
                 " SeqName    TEXT NOT NULL, " +
                 " Source     TEXT NOT NULL, " +
                 " Feature    TEXT NOT NULL, " + 
                 " Id         TEXT, " + 
                 " TotalCount INT" + 
                 " );" + 
                 " CREATE INDEX SeqNameTransAFIds_idx ON " + tblTransAFIds + "(SeqName); " + 
                 " CREATE INDEX SourceTransAFIds_idx ON " + tblTransAFIds + "(Source); " + 
                 " CREATE INDEX FeatureTransAFIds_idx ON " + tblTransAFIds + "(Feature); " + 
                 " CREATE INDEX IdTransAFIds_idx ON " + tblTransAFIds + "(Id); " + 
                 " COMMIT;";
    private static final String sqlSelTransAFIds = "SELECT * FROM " + tblTransAFIds + ";";
    private static final String sqlCountTransAFIds = "SELECT COUNT(SeqName) FROM " + tblTransAFIds + ";";
    private static final String sqlCountFeature = "SELECT SUM(TotalEntries) FROM " + tblAFStats + " WHERE Feature LIKE 'X';";
    //private static final String sqlCountFeature = "SELECT TotalEntries FROM" + tblAFStats + " WHERE Feature LIKE 'X';";!!!
    private static final String sqlCountFeatureByGene = "SELECT COUNT(DISTINCT Gene) FROM " + tblGeneTrans + " WHERE Transcript IN (SELECT SeqName FROM " + tblTransAFIds + " WHERE Feature LIKE 'X');";
    
    private static final String sqlInsTransAFIds = "INSERT INTO " + tblTransAFIds + "(SeqName, Source, Feature, Id, TotalCount) " +
                 " SELECT SeqName, Source, Feature, Id, COUNT(*) AS TotalCount FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") GROUP BY SeqName, Source, Feature, Id;";
    private static final String sqlSelTransAFSizes = "SELECT Source, Feature, PosType, Length FROM " + tblAnnotations +
                 " WHERE PosType IN('T', 'P') AND SeqName IN(SELECT Id FROM " + tblTrans + ");";

    // statistical tables - for filtered transcripts only
    private static final String sqlCreateAnnotSourceStats = "DROP TABLE IF EXISTS " + tblAnnotSourceStats + "; BEGIN;" +
                 " CREATE TABLE " + tblAnnotSourceStats + " (" +
                 " Source          TEXT NOT NULL, " +
                 " TransCount      INT, " + 
                 " GeneCount       INT" + 
                 " );" + 
                 " CREATE INDEX AnnotSourceStats_idx ON " + tblAnnotSourceStats + "(Source); " + 
                 " COMMIT;";
    private static final String sqlSelAnnotSourceStats = "SELECT * FROM " + tblAnnotSourceStats + ";";
    private static final String sqlInsAnnotSourceStats = "INSERT INTO " + tblAnnotSourceStats + "(Source, TransCount, GeneCount) " +
                 " SELECT Source, COUNT(DISTINCT(SeqName)), COUNT(DISTINCT(SELECT Gene FROM " + tblGeneTransFiltered + " WHERE Transcript=SeqName)) FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Id FROM " + tblTransFiltered + ") GROUP BY Source;";
    private static final String sqlCreateAFStats = "DROP TABLE IF EXISTS " + tblAFStats + "; BEGIN;" +
                 " CREATE TABLE " + tblAFStats + " (" +
                 " Source          TEXT NOT NULL, " +
                 " Feature         TEXT NOT NULL, " + 
                 " TotalEntries    INT, " + 
                 " UniqueIdEntries INT, " + 
                 " TransCount      INT" + 
                 " );" + 
                 " CREATE INDEX SourceStats_idx ON " + tblAFStats + "(Source); " + 
                 " CREATE INDEX FeatureStats_idx ON " + tblAFStats + "(Feature); " + 
                 " COMMIT;";
    private static final String sqlSelAFStats = "SELECT * FROM " + tblAFStats + ";";
    private static final String sqlInsAFStats = "INSERT INTO " + tblAFStats + "(Source, Feature, TotalEntries, UniqueIdEntries, TransCount) " +
                 " SELECT Source, Feature, COUNT(*), COUNT(DISTINCT(Id)), COUNT(DISTINCT(SeqName)) FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Id FROM " + tblTransFiltered + ") GROUP BY Source, Feature;";
    private static final String sqlCreateAFIdStats = "DROP TABLE IF EXISTS " + tblAFIdStats + "; BEGIN;" +
                 " CREATE TABLE " + tblAFIdStats + " (" +
                 " Source     TEXT NOT NULL, " +
                 " Feature    TEXT NOT NULL, " + 
                 " Id         TEXT, " + 
                 " Name       TEXT, " + 
                 " Desc       TEXT, " + 
                 " TotalCount INT, " + 
                 " TransCount INT" + 
                 " );" + 
                 " CREATE INDEX SourceIdStats_idx ON " + tblAFIdStats + "(Source); " + 
                 " CREATE INDEX FeatureIdStats_idx ON " + tblAFIdStats + "(Feature); " + 
                 " CREATE INDEX IdStats_idx ON " + tblAFIdStats + "(Id); " + 
                 " COMMIT;";
    private static final String sqlSelAFIdStats = "SELECT * FROM " + tblAFIdStats + ";";
    private static final String sqlSelAFIdStatsByFeature = "SELECT * FROM " + tblAFIdStats + " WHERE Feature LIKE '?';";
    private static final String sqlSelAFIdStatsBySource = "SELECT * FROM " + tblAFIdStats + " WHERE Source LIKE '?';";
    private static final String sqlSelAFStatsBySourceAndFeature = "SELECT * FROM " + tblAFIdStats + " WHERE Source LIKE '?' AND Feature LIKE '!';";
    private static final String sqlInsAFIdStats = "INSERT INTO " + tblAFIdStats + "(Source, Feature, Id, Name, Desc, TotalCount, TransCount) " +
                 " SELECT Source, Feature, Id, Name, Desc, COUNT(*) AS TotalCount, COUNT(DISTINCT(SeqName)) AS TransCount FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Id FROM " + tblTransFiltered + ") GROUP BY Source, Feature, Id;";

    // direct select query to tblAnnotations
    // Note: Must order by RowId or will break code
    /*private static final String sqlSelGeneAnnotations = "SELECT * FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Transcript FROM " + tblGeneTrans + " WHERE Gene = '?') ORDER BY RowId";
    */
    //Now must be ordered by SeqName because the new insertions are at the end of the annotfile and don't follow the order!!!
    private static final String sqlSelGeneAnnotations = "SELECT * FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Transcript FROM " + tblGeneTrans + " WHERE Gene = '?') ORDER BY SeqName";
    
    private static final String sqlCreateTrans = "BEGIN;" +
                 " CREATE TABLE " + tblTrans + " (Id TEXT PRIMARY KEY NOT NULL);" + 
                 " COMMIT;";
    private final String sqlInsTrans = "INSERT INTO " + tblTrans + "(Id) values(?)";
    // duplicate table schema of tblAnnotations - only contains specific annotation features
    private static final String sqlCreateTransData = "BEGIN;" +
                 " CREATE TABLE " + tblTransData + " (" +
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
                 " CREATE INDEX TDSeqName_idx ON " + tblTransData + "(SeqName); " + 
                 " CREATE INDEX TDSource_idx ON " + tblTransData + "(Source); " + 
                 " CREATE INDEX TDFeature_idx ON " + tblTransData + "(Feature); " + 
                 " COMMIT;";
    private final String sqlInsTransData = "INSERT INTO " + tblTransData +
                 " SELECT * FROM " + tblAnnotations + 
                 " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") AND Source || '-' || Feature IN(?) ORDER BY RowId;";
    private final String sqlSelTransData = "SELECT * FROM " + tblTransData + " ORDER BY SeqName";
    private static final String sqlCreateGeneTrans = "BEGIN;" +
                 " CREATE TABLE " + tblGeneTrans + " (" +
                 " Gene               TEXT NOT NULL, " +
                 " Transcript         TEXT NOT NULL" + 
                 " );" + 
                 " CREATE INDEX Gene_idx ON " + tblGeneTrans + "(Gene); " + 
                 " CREATE INDEX Trans_idx ON " + tblGeneTrans + "(Transcript); " + 
                 " COMMIT;";
    private final String sqlInsGeneTrans = "INSERT INTO " + tblGeneTrans + "(Gene, Transcript) SELECT Id AS Gene, SeqName AS Transcript FROM " + tblAnnotations +
                 " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") " +
                 " AND Source = '<S>' AND Feature = '<F>' ORDER BY Id;";
    private final String sqlSelGeneTrans = "SELECT * FROM " + tblGeneTrans + ";";
    private static final String sqlCreateProteins = "BEGIN;" +
                 " CREATE TABLE " + tblProteins + " (Id TEXT PRIMARY KEY NOT NULL);" + 
                 " COMMIT;";
    private final String sqlInsProteins = "INSERT INTO " + tblProteins + "(Id) SELECT DISTINCT(Id) FROM " + tblAnnotations +
                 " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") " +
                 " AND Source = '<S>' AND Feature = '<F>' ORDER BY Id;";
    private static final String sqlCreateGenes = "BEGIN;" +
                 " CREATE TABLE " + tblGenes + " (Id TEXT PRIMARY KEY NOT NULL);" + 
                 " COMMIT;";
    private final String sqlInsGenes = "INSERT INTO " + tblGenes + "(Id) SELECT DISTINCT(Id) FROM " + tblAnnotations +
                 " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") " +
                 " AND Source = '<S>' AND Feature = '<F>' ORDER BY Id;";

    // filtered transcripts associated tables
    private static final String sqlCreateTransFiltered = "DROP TABLE IF EXISTS " + tblTransFiltered + "; BEGIN;" +
                 " CREATE TABLE " + tblTransFiltered + " (Id TEXT PRIMARY KEY NOT NULL);" + 
                 " COMMIT;";
    private final String sqlInsTransFiltered = "INSERT INTO " + tblTransFiltered + "(Id) values(?)";
    private static final String sqlCreateGeneTransFiltered = "DROP TABLE IF EXISTS " + tblGeneTransFiltered + "; BEGIN;" +
                 " CREATE TABLE " + tblGeneTransFiltered + " (" +
                 " Gene               TEXT NOT NULL, " +
                 " Transcript         TEXT NOT NULL" + 
                 " );" + 
                 " CREATE INDEX GeneFiltered_idx ON " + tblGeneTransFiltered + "(Gene); " + 
                 " CREATE INDEX TransFiltered_idx ON " + tblGeneTransFiltered + "(Transcript); " + 
                 " COMMIT;";
    private final String sqlInsGeneTransFiltered = "INSERT INTO " + tblGeneTransFiltered + "(Gene, Transcript) SELECT Gene, Transcript FROM " + tblGeneTrans +
                 " WHERE Transcript IN(SELECT Id FROM " + tblTransFiltered + ");";

    private static final String sqlGetFeaturePresence = "SELECT DISTINCT Source, Feature, PosType FROM " + tblAnnotFeatures + " WHERE PosType NOT LIKE 'N' AND Source NOT LIKE '" + STRUCTURAL_SOURCE + "' AND Source NOT LIKE 'tappAS';";
    private static final String sqlGetFeaturePosition = "SELECT DISTINCT Source, Feature, PosType FROM " + tblAnnotFeatures + " WHERE Category LIKE '1' AND Source NOT LIKE 'tappAS'";


    //Get description for Features ID
    private static final String sqlSelFeatureDescription = "SELECT Source, Feature, Id, Name, Desc FROM " + tblAFIdStats + " WHERE Source NOT LIKE 'tappAS';";
    
    // class data
    private boolean dbaAttached = false;
    private String dbapath = "";
    private String sqlAttachDbA = "";
    private int verMajor = 0;
    private int verMinor = 0;

    // class instantiation
    public DbProject(Project project) {
        super(project);
    }
    
    // open DB, DB file must already exist - call createDB if not
    public boolean openDB(String dbpath) {
        boolean result = false;
        this.dbpath = dbpath;

        if(super.open()) {
            try {
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery(sqlSelDBInfo);
                if(rs.next()) {
                    dbapath = rs.getString("DBApath");
                    verMajor = rs.getInt("VerMajor");
                    verMinor = rs.getInt("VerMinor");
                    sqlAttachDbA = preAttachDbA + dbapath + postAttachDbA;
                    
                    // check if same major version, if not then DB structure changed - must handle properly in the future
                    // fail the call for now if so!!!
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
    
    // Create project DATABASE and CREATE/populate initial tables
    // Should only be called when a new project is created or re-load input data
    // WARNING: will delete existing db file if present
    public boolean createDB(String dbpath, String dbapath, ArrayList<String> lstTrans, double offset, double featuresPart, ProgressBar pbProgress){
        boolean result = false;
        long tstart = System.nanoTime();
        this.dbpath = dbpath;
        this.dbapath = dbapath;
        this.sqlAttachDbA = preAttachDbA + dbapath + postAttachDbA;
        
        // remove existing database if present
        Utils.removeFile(Paths.get(dbpath));

        // create project db and attach annotation db
        if(attach()) {
            // create all required tables
            if(createTable(tblDBInfo, sqlCreateDBInfo) && createTable(tblTransData, sqlCreateTransData) &&
               createTable(tblTrans, sqlCreateTrans) && createTable(tblGeneTrans, sqlCreateGeneTrans) &&
               createTable(tblTransFiltered, sqlCreateTransFiltered) && createTable(tblGeneTransFiltered, sqlCreateGeneTransFiltered) &&
               createTable(tblProteins, sqlCreateProteins) && createTable(tblGenes, sqlCreateGenes) && 
               createTable(tblAnnotFeatures, sqlCreateAnnotFeatures) && createTable(tblTransAFIds, sqlCreateTransAFIds) &&
               // these are statistical tables - will be updated if transcripts are filtered
               createTable(tblAnnotSourceStats, sqlCreateAnnotSourceStats) && createTable(tblAFStats, sqlCreateAFStats) && 
               createTable(tblAFIdStats, sqlCreateAFIdStats)) {
                double val = offset + 0.05 * featuresPart;
                app.ctls.updateProgress(pbProgress, val);
                // populate tables accordingly
                if(populateDBInfo()) {
                    val = offset + 0.1 * featuresPart;
                    app.ctls.updateProgress(pbProgress, val);

                    // must populate trans and gene tables first - will be used to populate other tables
                    if(populateTransTable(lstTrans) && populateGeneTransTable()) {
                        val = offset + 0.2 * featuresPart;
                        app.ctls.updateProgress(pbProgress, val);
                        if(populateTransFilteredTable(lstTrans) && populateGeneTransFilteredTable()) {
                            val = offset + 0.3 * featuresPart;
                            app.ctls.updateProgress(pbProgress, val);
                            if(populateTransDataTable() && populateIdsTable()) {
                                val = offset + 0.45 * featuresPart;
                                app.ctls.updateProgress(pbProgress, val);
                                if(populateTransAFIdsTable()) {
                                    val = offset + 0.6 * featuresPart;
                                    app.ctls.updateProgress(pbProgress, val);
                                    if(populateAnnotFeatureTable()) {
                                        val = offset + 0.8 * featuresPart;
                                        app.ctls.updateProgress(pbProgress, val);
                                        // these are statistical tables - will be updated if transcripts are filtered
                                        if(populateFilteredStatsTables()) {
                                            // generate block features list - features with sizes > cutoff length
                                            // used in gene data visualization to determine how features are displayed - symbol or block
                                            if(genFeatureSizes()) {
                                                val = offset + featuresPart;
                                                app.ctls.updateProgress(pbProgress, val);
                                                result = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            close();
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        
        // remove db file if not successful
        if(!result)
            Utils.removeFile(Paths.get(dbpath));
        return result;
    }
    @Override
    protected void close() {
        super.close();
        dbaAttached = false;
    }
    protected boolean attach() {
        boolean result = false;
        
        // open the database connection, if not already done, and attach the annotation database
        if(super.open()) {
            try {
                if(!dbaAttached) {
                    Statement stmt = c.createStatement();
                    stmt.execute(sqlAttachDbA);
                    stmt.close();
                    // test to see if attachment worked - change if you have a better way
                    stmt = c.createStatement();
                    ResultSet rs = stmt.executeQuery( "SELECT * FROM " + tblAnnotations + " LIMIT 3;" );
                    dbaAttached = rs.next();
                    try { rs.close(); } catch(Exception ce) { logger.logWarning(ce.getClass().getName() + " Close result set exception: " + ce.getMessage()); }
                }
                result = dbaAttached;
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        return result;
    }
    
    // get annotation sources stats data
    public HashMap<String, AnnotSourceStats> loadAnnotSourceStats(){
        HashMap<String, AnnotSourceStats> hmResults = new HashMap<>();
        ResultSet rs = getRS(sqlSelAnnotSourceStats);
        try{
            if(rs.isBeforeFirst()) {
                try {
                    long tstart = System.nanoTime();
                    System.out.println("Processing annotation sources...");
                    while(rs.next())
                        hmResults.put(rs.getString(1), new AnnotSourceStats(rs.getInt(2), rs.getInt(3)));
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    hmResults.clear();
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }
    // get annotation features stats data
    public HashMap<String, HashMap<String, AFStatsData>> loadAFStats(){
        HashMap<String, HashMap<String, AFStatsData>> hmResults = new HashMap<>();
        ResultSet rs = getRS(sqlSelAFStats);
        try{
            if(rs.isBeforeFirst()) {
                String source, feature, id;
                HashMap<String, AFStatsData> hm;
                try {
                    long tstart = System.nanoTime();
                    System.out.println("Processing feature Ids...");
                    while(rs.next()) {
                        source = rs.getString(1);
                        feature = rs.getString(2);
                        if(hmResults.containsKey(source))
                            hm = hmResults.get(source);
                        else {
                            hm = new HashMap<>();
                            hmResults.put(source, hm);
                        }
                        hm.put(feature, new AFStatsData(rs.getInt(3), rs.getInt(4), rs.getInt(5)));
                    }
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");

                    // now add the individual feature id stats
                    rs = getRS(sqlSelAFIdStats);
                    if(rs.isBeforeFirst()) {
                        tstart = System.nanoTime();
                        System.out.println("Processing feature Ids...");

                        while(rs.next()) {
                            source = rs.getString(1);
                            feature = rs.getString(2);
                            id = rs.getString(3);
                            // source-feature  should always be present, code exception if not
                            hmResults.get(source).get(feature).hmIds.put(id, new AFIdStatsData(rs.getString(4), rs.getString(5), rs.getInt(6), rs.getInt(7)));
                        }
                        rs.close();

                        tend = System.nanoTime();
                        duration = (tend - tstart)/1000000;
                        System.out.println("Total time: " + duration + " ms");
                    }
                } catch ( Exception e ) {
                    hmResults.clear();
                    try { if(rs.isBeforeFirst()) rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        close();
        return hmResults;
    }
    // get presence features stats data
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

    // get positional features stats data
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

//    public HashMap<String, HashMap<String, AFStatsData>> loadAFStatsPositional(){
//        HashMap<String, HashMap<String, AFStatsData>> hmResults = new HashMap<>();
//        ResultSet rs = getRS(sqlSelAFStats);
//        try{
//            if(rs.isBeforeFirst()) {
//                String source, feature, id;
//                HashMap<String, AFStatsData> hm;
//                try {
//                    long tstart = System.nanoTime();
//                    System.out.println("Processing feature Ids...");
//                    while(rs.next()) {
//                        source = rs.getString(1);
//                        feature = rs.getString(2);
//                        if(hmResults.containsKey(source))
//                            hm = hmResults.get(source);
//                        else {
//                            hm = new HashMap<>();
//                            hmResults.put(source, hm);
//                        }
//                        hm.put(feature, new AFStatsData(rs.getInt(3), rs.getInt(4), rs.getInt(5)));
//                    }
//                    rs.close();
//
//                    long tend = System.nanoTime();
//                    long duration = (tend - tstart)/1000000;
//                    System.out.println("Total time: " + duration + " ms");
//
//                    // now add the individual feature id stats
//                    rs = getRS(sqlSelAFIdStats);
//                    if(rs.isBeforeFirst()) {
//                        tstart = System.nanoTime();
//                        System.out.println("Processing feature Ids...");
//
//                        while(rs.next()) {
//                            source = rs.getString(1);
//                            feature = rs.getString(2);
//                            id = rs.getString(3);
//                            // source-feature  should always be present, code exception if not
//                            hmResults.get(source).get(feature).hmIds.put(id, new AFIdStatsData(rs.getString(4), rs.getString(5), rs.getInt(6), rs.getInt(7)));
//                        }
//                        rs.close();
//
//                        tend = System.nanoTime();
//                        duration = (tend - tstart)/1000000;
//                        System.out.println("Total time: " + duration + " ms");
//                    }
//                } catch ( Exception e ) {
//                    hmResults.clear();
//                    try { if(rs.isBeforeFirst()) rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
//                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
//                }
//            }
//        } catch (SQLException ex) {
//            System.out.println("Query hasn't results, error: " + ex);
//        }
//        close();
//        return hmResults;
//    }
    // get gene/transcripts
    public HashMap<String, HashMap<String, Object>> loadGeneTrans(){
        HashMap<String, HashMap<String, Object>> hmResults = new HashMap<>();
        ResultSet rs = getRS(sqlSelGeneTrans);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing gene/trans...");

                String gene, trans;
                HashMap<String, Object> hm;
                try {
                    while(rs.next()) {
                        gene = rs.getString("Gene");
                        trans = rs.getString("Transcript");
                        if(hmResults.containsKey(gene))
                            hm = hmResults.get(gene);
                        else {
                            hm = new HashMap<>();
                            hmResults.put(gene, hm);
                        }
                        hm.put(trans, null);
                    }
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }
    // get gene annotations
    public ArrayList<String> loadGeneAnnotations(String gene){
        ArrayList<String> lst = new ArrayList<>();
        if(attach()) {
            String sql = sqlSelGeneAnnotations.replace("?", gene);
            ResultSet rs = getRS(sql);
            System.out.println(sql);
            try{
                if(rs.isBeforeFirst()) {
                    long tstart = System.nanoTime();
                    System.out.println("Processing gene/trans...");
                    String row;
                    try {
                        while(rs.next()) {
                            row = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s", 
                                    rs.getString("SeqName"), rs.getString("Source"), rs.getString("Feature"),
                                    rs.getInt("Start"), rs.getInt("End"), ".",
                                    rs.getString("Strand"), ".", rs.getString("Attrs"), rs.getInt("Length"));
                            lst.add(row);
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        System.out.println("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
            close();
        }
        return lst;
    }    
    
    //
    // Insert Queries
    //
    
    // populate DB information table
    private boolean populateDBInfo() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating database information table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsDBInfo.replace("?", dbapath));
                System.out.println("Inserted " + rowcnt + " rows into " + tblDBInfo);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("Unable to add DB information record." );
            } catch ( Exception e ) {

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }

    // populate transcript ids table from given transcripts list
    private boolean populateTransTable(ArrayList<String> lstTrans) {
        boolean result = false;
        long tstart = System.nanoTime();
        if(lstTrans.isEmpty()) {
            logger.logError("No transcripts in list for DB table." );
            return result;
        }
        
        System.out.println("Populating transcripts table...");
        if(open()) {
            try {
                c.setAutoCommit(false);
                PreparedStatement ps = c.prepareStatement(sqlInsTrans);
                int batchcnt = 0;
                for (String trans : lstTrans) {
                    ps.setString(1, trans);
                    ps.addBatch();
                    if ((++batchcnt % 10000) == 0) {
                        ps.executeBatch();
                        batchcnt = 0;
                    }        
                }
                if(batchcnt > 0)
                    ps.executeBatch();
                c.commit(); 
                result = true;
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
            finally {
                try { if(c != null) c.setAutoCommit(true); } catch(Exception e) { System.out.println(e.getClass().getName() + " Set auto commit exception within exception: " + e.getMessage()); }
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    // right now we are only copying the commonly used annotation to the project table
    // when we need to look at other features, we must attach to the main annotation table to retrieve them
    private boolean populateTransDataTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating trans data table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
                String features = "'" + defs.transcript.source + "-" + defs.transcript.feature + "'";
                features += ",'" + defs.cds.source + "-" + defs.cds.feature + "'";
                features += ",'" + defs.gene.source + "-" + defs.gene.feature + "'";
                features += ",'" + defs.protein.source + "-" + defs.protein.feature + "'";
                features += ",'" + defs.exon.source + "-" + defs.exon.feature + "'";
                String sql = sqlInsTransData.replace("?", features);
                int rowcnt = stmt.executeUpdate(sql);
                System.out.println("Inserted " + rowcnt + " rows into " + tblTransData);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("No DB transcript data records added." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    
    // populate protein and gene ids tables from given list
    private boolean populateIdsTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating protein and gene ids table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                String sql = sqlInsProteins.replace("<S>", project.data.getAnnotationFileDefs().protein.source);
                sql = sql.replace("<F>", project.data.getAnnotationFileDefs().protein.feature);
                int rowcnt = stmt.executeUpdate(sql);
                System.out.println("Inserted " + rowcnt + " rows into " + tblProteins);
                if(rowcnt > 0) {
                    sql = sqlInsGenes.replace("<S>", project.data.getAnnotationFileDefs().gene.source);
                    sql = sql.replace("<F>", project.data.getAnnotationFileDefs().gene.feature);
                    rowcnt = stmt.executeUpdate(sql);
                    System.out.println("Inserted " + rowcnt + " rows into " + tblGenes);
                    if(rowcnt > 0)
                        result = true;
                    else
                        logger.logError("No DB gene ids records added." );
                }else{
                    logger.logInfo("No DB protein records added." ); //before was an error but can be data without proteins
                    sql = sqlInsGenes.replace("<S>", project.data.getAnnotationFileDefs().gene.source);
                    sql = sql.replace("<F>", project.data.getAnnotationFileDefs().gene.feature);
                    rowcnt = stmt.executeUpdate(sql);
                    System.out.println("Inserted " + rowcnt + " rows into " + tblGenes);
                    if(rowcnt > 0)
                        result = true;
                    else
                        logger.logError("No DB gene ids records added.");
                }
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    
    // populate gene/trans table
    private boolean populateGeneTransTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating gene/trans table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                String sql = sqlInsGeneTrans.replace("<S>", project.data.getAnnotationFileDefs().gene.source);
                sql = sql.replace("<F>", project.data.getAnnotationFileDefs().gene.feature);
                int rowcnt = stmt.executeUpdate(sql);
                System.out.println("Inserted " + rowcnt + " rows into " + tblGeneTrans);
                if(rowcnt > 0)
                    result = true;
                else{
                    //Warning remove and download again all references
                    logger.logError("2. Error loading gene-transcripts from database\n\nIf you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\\n\\nYou can find more information in our website: app.tappas.org/downloads");
                    app.ctls.alertWarning("2. Error loading gene-transcripts from database", "If you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\n\nYou can find more information in our website: app.tappas.org/download");
                    close();
                }
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }

    // populate annotation feature table
    private boolean populateAnnotFeatureTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        System.out.println("Populating annotation feature tables...");

        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsAnnotFeatures);
                System.out.println("Inserted " + rowcnt + " rows into " + tblAnnotFeatures);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("No DB annotation feature records added." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    // populate annotation feature statistical tables based on filtered transcripts
    private boolean populateFilteredStatsTables() {
        boolean result = false;
        long tstart = System.nanoTime();
        System.out.println("Populating annotation feature statistica tables...");

        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsAnnotSourceStats);
                System.out.println("Inserted " + rowcnt + " rows into " + tblAnnotSourceStats);
                if(rowcnt > 0) {
                    rowcnt = stmt.executeUpdate(sqlInsAFStats);
                    System.out.println("Inserted " + rowcnt + " rows into " + tblAFStats);
                    if(rowcnt > 0) {
                        rowcnt = stmt.executeUpdate(sqlInsAFIdStats);
                        System.out.println("Inserted " + rowcnt + " rows into " + tblAFIdStats);
                        if(rowcnt > 0)
                            result = true;
                        else
                            logger.logError("No DB annotation features statistic records added." );
                    }
                    else
                        logger.logError("No DB annotation features statistic records added." );
                }
                else
                    logger.logError("No DB annotation feature statistic records added." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    // populate transcript source-feature-ids table
    private boolean populateTransAFIdsTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating transcripts annotation feature ids table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsTransAFIds);
                System.out.println("Inserted " + rowcnt + " rows into " + tblTransAFIds);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("No DB transcript annotation feature id records added." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }

    // populate transcript ids table from given transcripts list
    private boolean populateTransFilteredTable(ArrayList<String> lstTrans) {
        boolean result = false;
        long tstart = System.nanoTime();
        if(lstTrans.isEmpty()) {
            logger.logError("No filtered transcripts in list for DB table." );
            return result;
        }
        
        System.out.println("Populating filtered transcripts table...");
        if(open()) {
            try {
                c.setAutoCommit(false);
                PreparedStatement ps = c.prepareStatement(sqlInsTransFiltered);
                int batchcnt = 0;
                for (String trans : lstTrans) {
                    ps.setString(1, trans);
                    ps.addBatch();
                    if ((++batchcnt % 10000) == 0) {
                        ps.executeBatch();
                        batchcnt = 0;
                    }        
                }
                if(batchcnt > 0)
                    ps.executeBatch();
                c.commit(); 
                result = true;
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
            finally {
                try { if(c != null) c.setAutoCommit(true); } catch(Exception e) { System.out.println(e.getClass().getName() + " Set auto commit exception within exception: " + e.getMessage()); }
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    private boolean populateGeneTransFilteredTable() {
        boolean result = false;
        long tstart = System.nanoTime();
        
        System.out.println("Populating gene/trans filtered table...");
        if(open()) {
            try {
                Statement stmt = c.createStatement();
                int rowcnt = stmt.executeUpdate(sqlInsGeneTransFiltered);
                System.out.println("Inserted " + rowcnt + " rows into " + tblGeneTransFiltered);
                if(rowcnt > 0)
                    result = true;
                else
                    logger.logError("No DB filtered gene transcript records added." );
            } catch ( Exception e ) {
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );

                // close database connection - something is messed up
                close();
            }
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Total time: " + duration + " ms");
        return result;
    }
    
    //
    // Data Classes
    //
    
    public static class AFIdStatsData {
        String name, desc;
        int count;
        int transCount;
        //String name; name or description if no name available OR do we want separate fields
        
        public AFIdStatsData(String name, String desc, int count, int transCount) {
            this.name = name;
            this.desc = desc;
            this.count = count;
            this.transCount = transCount;
        }
    }
    public static class AFStatsData {
        int count;
        int idCount;
        int transCount;
        HashMap<String, AFIdStatsData> hmIds;
        
        public AFStatsData(int count, int idCount, int transCount) {
            this.count = count;
            this.idCount = idCount;
            this.transCount = transCount;
            hmIds = new HashMap<>();
        }
    }

    // The detailed features data is currently only in the master annotation DB 
    // could add to project DB for loading performance improvement but will increase disk space use
    private static final String sqlSelTransAFIdPos = "SELECT SeqName, Source, Feature, Id, PosType, Start, End FROM " + tblAnnotations + " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") ORDER by SeqName, Source, Feature, Id;";
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdPos(){
        // transcript:source:feature:id(count:positions string)
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        logger.logDebug("Querying DB for TransAFIdPos recordset...");
        if(attach()) {
            ResultSet rs = getRS(sqlSelTransAFIdPos);
            logger.logDebug("Got TransAFIdPos recordset.");
            try{
                if(rs.isBeforeFirst()) {
                    long tstart = System.nanoTime();
                    logger.logDebug("Processing transcript annotation feature id positions...");
                    // this is a time consuming query - may look into speeding up somehow
                    // on a good/fast computer takes about 3 or 4 seconds
                    try {
                        int tcnt, scnt, fcnt;
                        tcnt = scnt = fcnt = 0;
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmSource = new HashMap<>();
                        HashMap<String, HashMap<String, String>> hmFeature = new HashMap<>();
                        HashMap<String, String> hmId = new HashMap<>();
                        String trans, source, feature, id, idpos;
                        String dbtrans, dbsource, dbfeature, dbid;
                        trans = source = feature = id = idpos = "";
                        int idposcnt = 0;
                        int dspcnt = 0;
                        while(rs.next()) {
                            // SeqName, Source, Feature, Id, PosType, Start, End                    
                            dbtrans = rs.getString(1);
                            dbsource = rs.getString(2);
                            dbfeature = rs.getString(3);
                            dbid = rs.getString(4);

                            // check if not the same transcript
                            if(!dbtrans.equals(trans)) {
                                tcnt++;
                                trans = dbtrans;
                                source = feature = id = "";
                                hmSource = new HashMap<>();
                                hmResults.put(trans, hmSource);
                            }

                            // check if not the same source
                            if(!dbsource.equals(source)) {
                                scnt++;
                                source = dbsource;
                                feature = id = "";
                                hmFeature = new HashMap<>();
                                hmSource.put(source, hmFeature);
                            }

                            // check if not the same feature
                            if(!dbfeature.equals(feature)) {
                                fcnt++;
                                feature = dbfeature;
                                id = "";
                                hmId = new HashMap<>();
                                hmFeature.put(feature, hmId);
                            }

                            // check if not the same id
                            if(!dbid.equals(id)) {
                                id = dbid;
                                idpos = "";
                                idposcnt = 1;
                            }
                            else {
                                idpos += ";";
                                idposcnt++;
                            }
                            String posType = rs.getString(5);
                            if(posType.equals("N")) // get N!!!
                                idpos += posType;
                            else
                                idpos += posType + rs.getString(6) + "-" + rs.getString(7);
                            hmId.put(id, idposcnt + ":" + idpos);

                            if((++dspcnt % 1000) == 0)
                                logger.logDebug("Processed " + dspcnt + " featureID/transcripts...");
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        logger.logDebug("TransCnt: " + tcnt + ", SourceCnt: " + scnt + ", FeatureCnt: " + fcnt);
                        logger.logDebug("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage());}
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
        }
        return hmResults;
    }
    
    private static final String sqlSelTransAFIdGenPos = "SELECT SeqName, Source, Feature, Id, PosType, Start, End FROM " + tblAnnotations + " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") AND Start NOT LIKE '0' and End NOT LIKE '0' AND Source NOT LIKE 'tappAS' ORDER by SeqName, Source, Feature, Id;";
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdGenPos(){
        // transcript:source:feature:id(count:positions string)
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        logger.logDebug("Querying DB for TransAFIdPos recordset...");
        if(attach()) {
            ResultSet rs = getRS(sqlSelTransAFIdGenPos);
            logger.logDebug("Got TransAFIdPos recordset.");
            try{
                if(rs.isBeforeFirst()) {
                    long tstart = System.nanoTime();
                    logger.logDebug("Processing transcript annotation feature id positions...");
                    // this is a time consuming query - may look into speeding up somehow
                    // on a good/fast computer takes about 3 or 4 seconds
                    try {
                        int tcnt, scnt, fcnt;
                        tcnt = scnt = fcnt = 0;
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmSource = new HashMap<>();
                        HashMap<String, HashMap<String, String>> hmFeature = new HashMap<>();
                        HashMap<String, String> hmId = new HashMap<>();
                        String trans, source, feature, id, idpos;
                        String dbtrans, dbsource, dbfeature, dbid;
                        trans = source = feature = id = idpos = "";
                        int idposcnt = 0;
                        int dspcnt = 0;
                        while(rs.next()) {
                            // SeqName, Source, Feature, Id, PosType, Start, End                    
                            dbtrans = rs.getString(1);
                            dbsource = rs.getString(2);
                            dbfeature = rs.getString(3);
                            dbid = rs.getString(4);

                            // check if not the same transcript
                            if(!dbtrans.equals(trans)) {
                                tcnt++;
                                trans = dbtrans;
                                source = feature = id = "";
                                hmSource = new HashMap<>();
                                hmResults.put(trans, hmSource);
                            }

                            // check if not the same source
                            if(!dbsource.equals(source)) {
                                scnt++;
                                source = dbsource;
                                feature = id = "";
                                hmFeature = new HashMap<>();
                                hmSource.put(source, hmFeature);
                            }

                            // check if not the same feature
                            if(!dbfeature.equals(feature)) {
                                fcnt++;
                                feature = dbfeature;
                                id = "";
                                hmId = new HashMap<>();
                                hmFeature.put(feature, hmId);
                            }

                            // check if not the same id
                            if(!dbid.equals(id)) {
                                id = dbid;
                                idpos = "";
                                idposcnt = 1;
                            }
                            else {
                                idpos += ";";
                                idposcnt++;
                            }
                            String posType = rs.getString(5);
                            if(posType.equals("N")) // get N!!!
                                idpos += posType;
                            else
                                idpos += posType + rs.getString(6) + "-" + rs.getString(7);
                            hmId.put(id, idposcnt + ":" + idpos);

                            if((++dspcnt % 1000) == 0)
                                logger.logDebug("Processed " + dspcnt + " featureID/transcripts...");
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        logger.logDebug("TransCnt: " + tcnt + ", SourceCnt: " + scnt + ", FeatureCnt: " + fcnt);
                        logger.logDebug("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage());}
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
        }
        return hmResults;
    }
    
    private static final String ifTransHasAnnot = "select count(SeqName) from " + tblTransAFIds + " where SeqName LIKE \"X\" and Source LIKE \"Y\";";
    public int getIfTransHasAnnot(String trans, String source){
        int value = 0;
        logger.logDebug("Querying DB for " + source + "in tblTransAFIds...");
        if(attach()) {
            String sql = ifTransHasAnnot.replace("X", trans);
            sql = sql.replace("Y", source);
            ResultSet rs = getRS(sql);
            logger.logDebug("Got if feature is present.");
            try{
                if(rs.isBeforeFirst()) {
                    String dbquantity;
                    while(rs.next()) {
                        // Quantity                    
                        dbquantity = rs.getString(1);
                        value = Integer.parseInt(dbquantity);
                    }
                    rs.close();
                }
            } catch ( Exception e ) {
                try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            close();
            }
        return value;
    }

    private static final String transHasAnnot = "select SeqName from " + tblTransAFIds + " where Source LIKE \"Y\";";
    public HashMap<String, Object> getTransWithAnnot(String source){
        HashMap<String, Object> res = new HashMap<String, Object>();
        logger.logDebug("Querying DB for " + source + "in tblTransAFIds...");
        if(attach()) {
            String sql = transHasAnnot.replace("Y", source);
            ResultSet rs = getRS(sql);
            logger.logDebug("Got if feature is present.");
            try{
                if(rs.isBeforeFirst()) {
                    String trans;
                    while(rs.next()) {
                        // Names
                        trans = rs.getString(1);
                        res.put(trans, null);
                    }
                    rs.close();
                }
            } catch ( Exception e ) {
                try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            close();
        }
        return res;
    }
    
    public int getTotalAnnot(){
        int value = 0;
        logger.logDebug("Querying DB for total Annot...");
        if(attach()) {
            String sql = sqlCountTransAFIds;
            ResultSet rs = getRS(sql);
            try{
                if(rs.isBeforeFirst()) {
                    String dbquantity;
                    while(rs.next()) {
                        // Quantity                    
                        dbquantity = rs.getString(1);
                        value = Integer.parseInt(dbquantity);
                    }
                    rs.close();
                }
            } catch ( Exception e ) {
                try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            close();
            }
        return value;
    }
    
    //sqlCountFeature
    public int getAnnotCount(String annot){
        int value = 0;
        logger.logDebug("Querying DB for total Annot...");
        if(attach()) {
            String sql = sqlCountFeature.replace("X", annot);
            ResultSet rs = getRS(sql);
            try{
                if(rs.isBeforeFirst()) {
                    String dbquantity;
                    while(rs.next()) {
                        // Quantity                    
                        dbquantity = rs.getString(1);
                        value = Integer.parseInt(dbquantity);
                    }
                    rs.close();
                }
            } catch ( Exception e ) {
                try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            close();
            }
        return value;
    }
    
    public int getAnnotCountByGene(String annot){
        int value = 0;
        logger.logDebug("Querying DB for total Annot...");
        if(attach()) {
            String sql = sqlCountFeatureByGene.replace("X", annot);
            ResultSet rs = getRS(sql);
            try{
                if(rs.isBeforeFirst()) {
                    String dbquantity;
                    while(rs.next()) {
                        // Quantity                    
                        dbquantity = rs.getString(1);
                        value = Integer.parseInt(dbquantity);
                    }
                    rs.close();
                }
            } catch ( Exception e ) {
                try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            close();
            }
        return value;
    }
    
    // only get requested transcripts to improve performance and memory usage - intended to be used for a single gene transcripts
    private static final String sqlSelTransAFIdPosFiltered = "SELECT SeqName, Source, Feature, Id, PosType, Start, End FROM " + tblAnnotations + " WHERE SeqName IN(?) ORDER by SeqName, Source, Feature, Id;";
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdPos(ArrayList<String> lstTrans){
        // transcript:source:feature:id(count:positions string)
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        logger.logDebug("Querying DB for TransAFIdPos filtered recordset...");
        if(attach()) {
            String strTrans = "";
            for(String id : lstTrans)
                strTrans += (strTrans.isEmpty()? "" : ",") + "'" + id + "'";
            String sql = sqlSelTransAFIdPosFiltered.replace("?", strTrans);
            ResultSet rs = getRS(sql);
            logger.logDebug("Got TransAFIdPos filtered recordset.");
            try{
                if(rs.isBeforeFirst()) {
                    long tstart = System.nanoTime();
                    logger.logDebug("Processing transcript annotation feature id positions...");
                    try {
                        int tcnt, scnt, fcnt;
                        tcnt = scnt = fcnt = 0;
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmSource = new HashMap<>();
                        HashMap<String, HashMap<String, String>> hmFeature = new HashMap<>();
                        HashMap<String, String> hmId = new HashMap<>();
                        String trans, source, feature, id, idpos;
                        String dbtrans, dbsource, dbfeature, dbid;
                        trans = source = feature = id = idpos = "";
                        int idposcnt = 0;
                        int dspcnt = 0;
                        while(rs.next()) {
                            // SeqName, Source, Feature, Id, PosType, Start, End                    
                            dbtrans = rs.getString(1);
                            dbsource = rs.getString(2);
                            dbfeature = rs.getString(3);
                            dbid = rs.getString(4);

                            // check if not the same transcript
                            if(!dbtrans.equals(trans)) {
                                tcnt++;
                                trans = dbtrans;
                                source = feature = id = "";
                                hmSource = new HashMap<>();
                                hmResults.put(trans, hmSource);
                            }

                            // check if not the same source
                            if(!dbsource.equals(source)) {
                                scnt++;
                                source = dbsource;
                                feature = id = "";
                                hmFeature = new HashMap<>();
                                hmSource.put(source, hmFeature);
                            }

                            // check if not the same feature
                            if(!dbfeature.equals(feature)) {
                                fcnt++;
                                feature = dbfeature;
                                id = "";
                                hmId = new HashMap<>();
                                hmFeature.put(feature, hmId);
                            }

                            // check if not the same id
                            if(!dbid.equals(id)) {
                                id = dbid;
                                idpos = "";
                                idposcnt = 1;
                            }
                            else {
                                idpos += ";";
                                idposcnt++;
                            }
                            String posType = rs.getString(5);
                            if(posType.equals("N")) // hardcoded N - get "N"?
                                idpos += posType;
                            else
                                idpos += posType + rs.getString(6) + "-" + rs.getString(7);
                            hmId.put(id, idposcnt + ":" + idpos);

                            if((++dspcnt % 1000) == 0)
                                logger.logDebug("Processed " + dspcnt + " featureID/transcripts...");
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        logger.logDebug("TransCnt: " + tcnt + ", SourceCnt: " + scnt + ", FeatureCnt: " + fcnt);
                        logger.logDebug("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
        }
        return hmResults;
    }
    
    public HashMap<String, HashMap<String, HashMap<String, String>>> loadFeatureDescriptions(){
        HashMap<String, HashMap<String, HashMap<String, String>>> hmSource = new HashMap<>();
        ResultSet rs = getRS(sqlSelFeatureDescription);
        //Get Source / Feature / ID / Name /Description [but we elect just one, name or desc]
        try{
            if(rs.isBeforeFirst()) {
                String source, feature, id, name, desc;
                long tstart = System.nanoTime();
                System.out.println("Processing features descriptions...");
                try {
                    while(rs.next()) {
                        HashMap<String, HashMap<String, String>> hmFeature = new HashMap<>();
                        HashMap<String, String> hmID = new HashMap<>();
                        
                        source = rs.getString(1);
                        feature = rs.getString(2);
                        id = rs.getString(3);
                        name = rs.getString(4);
                        desc = rs.getString(5);
                        
                        if(hmSource.containsKey(source)){
                            hmFeature = hmSource.get(source);
                            if(hmFeature.containsKey(feature)){
                                hmID = hmFeature.get(feature);
                                if(desc.isEmpty())
                                    hmID.put(id, name);
                                else
                                    hmID.put(id, desc);
                            }else{
                                if(desc.isEmpty())
                                    hmID.put(id, name);
                                else
                                    hmID.put(id, desc);
                                hmFeature.put(feature, hmID);
                            }
                        }else{
                            if(desc.isEmpty())
                                hmID.put(id, name);
                            else
                                hmID.put(id, desc);
                            hmFeature.put(feature, hmID);
                            hmSource.put(source, hmFeature);
                        }
                    }
                long tend = System.nanoTime();
                long duration = (tend - tstart)/1000000;
                System.out.println("Total time: " + duration + " ms");
                } catch (SQLException ex) {
                    System.out.println("Query hasn't results, error: " + ex);
                }
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmSource;
    }
    
    // only get requested features to improve performance and memory usage
    private static final String sqlSelTransSrcAFIdPos = "SELECT SeqName, Source, Feature, Id, PosType, Start, End FROM " + tblAnnotations + " WHERE SeqName IN(SELECT Id FROM " + tblTrans + ") ORDER by SeqName, Source, Feature, Id;";
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdPos(HashMap<String, HashMap<String, Object>> hmFeatures){
        // transcript:source:feature:id(count:positions string)
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmResults = new HashMap<>();
        if(attach()) {
            ResultSet rs = getRS(sqlSelTransAFIdPos);
            try{
                if(rs.isBeforeFirst()) {
                    long tstart = System.nanoTime();
                    System.out.println("Processing transcript annotation feature id positions...");
                    try {
                        int tcnt, scnt, fcnt;
                        tcnt = scnt = fcnt = 0;
                        HashMap<String, HashMap<String, HashMap<String, String>>> hmSource = new HashMap<>();
                        HashMap<String, HashMap<String, String>> hmFeature = new HashMap<>();
                        HashMap<String, String> hmId = new HashMap<>();
                        String trans, source, feature, id, idpos;
                        String dbtrans, dbsource, dbfeature, dbid;
                        trans = source = feature = id = idpos = "";
                        int idposcnt = 0;
                        while(rs.next()) {
                            // SeqName, Source, Feature, Id, PosType, Start, End                    
                            dbtrans = rs.getString(1);
                            dbsource = rs.getString(2);
                            dbfeature = rs.getString(3);
                            dbid = rs.getString(4);

                            // check if not the same transcript
                            if(!dbtrans.equals(trans)) {
                                tcnt++;
                                trans = dbtrans;
                                source = feature = id = "";
                                hmSource = new HashMap<>();
                                hmResults.put(trans, hmSource);
                            }

                            // check if not the same source
                            if(!dbsource.equals(source)) {
                                scnt++;
                                source = dbsource;
                                feature = id = "";
                                hmFeature = new HashMap<>();
                                hmSource.put(source, hmFeature);
                            }

                            // check if not the same feature
                            if(!dbfeature.equals(feature)) {
                                fcnt++;
                                feature = dbfeature;
                                id = "";
                                hmId = new HashMap<>();
                                hmFeature.put(feature, hmId);
                            }

                            // check if not the same id
                            if(!dbid.equals(id)) {
                                id = dbid;
                                idpos = "";
                                idposcnt = 1;
                            }
                            else {
                                idpos += ";";
                                idposcnt++;
                            }
                            String posType = rs.getString(5);
                            if(posType.equals("N")) // get N!!!
                                idpos += posType;
                            else
                                idpos += posType + rs.getString(6) + "-" + rs.getString(7);
                            hmId.put(id, idposcnt + ":" + idpos);
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        System.out.println("TransCnt: " + tcnt + ", SourceCnt: " + scnt + ", FeatureCnt: " + fcnt);
                        System.out.println("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
        }
        return hmResults;
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, Integer>>>> loadTransAFIds(){
        // transcript:source:feature:id,count
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, Integer>>>> hmResults = new HashMap<>();

        ResultSet rs = getRS(sqlSelTransAFIds);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing transcript annotation feature ids...");

                try {
                    HashMap<String, HashMap<String, HashMap<String, Integer>>> hmSource = new HashMap<>();
                    HashMap<String, HashMap<String, Integer>> hmFeature = new HashMap<>();
                    HashMap<String, Integer> hmId = new HashMap<>();
                    String trans, source, feature, id;
                    String dbtrans, dbsource, dbfeature;
                    trans = source = feature = id = "";
                    int cnt;
                    while(rs.next()) {
                        dbtrans = rs.getString(1);
                        dbsource = rs.getString(2);
                        dbfeature = rs.getString(3);
                        cnt = rs.getInt(4);

                        // check if not the same transcript
                        if(!dbtrans.equals(trans)) {
                            trans = dbtrans;
                            source = feature = id = "";
                            hmSource = new HashMap<>();
                            hmResults.put(trans, hmSource);
                        }

                        // check if not the same source
                        if(!dbsource.equals(source)) {
                            source = dbsource;
                            feature = id = "";
                            hmFeature = new HashMap<>();
                            hmSource.put(source, hmFeature);
                        }

                        // check if not the same feature
                        if(!dbfeature.equals(feature)) {
                            feature = dbfeature;
                            id = "";
                            hmId = new HashMap<>();
                            hmFeature.put(feature, hmId);
                        }

                        // ids are always unique
                        hmId.put(id, cnt);
                    }
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }
    public HashMap<String, HashMap<String, HashMap<String, Integer>>> loadTransAnnotFeatures(){
        // transcript:source:feature,count
        HashMap<String, HashMap<String, HashMap<String, Integer>>> hmResults = new HashMap<>();

        ResultSet rs = getRS(sqlSelTransAFIds);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing transcript annotation feature ids...");

                try {
                    HashMap<String, HashMap<String, Integer>> hmSource = new HashMap<>();
                    HashMap<String, Integer> hmFeature = new HashMap<>();
                    String trans, source, feature;
                    String dbtrans, dbsource, dbfeature;
                    trans = source = feature = "";
                    int cnt;
                    int featureCnt = 0;
                    while(rs.next()) {
                        dbtrans = rs.getString(1);
                        dbsource = rs.getString(2);
                        dbfeature = rs.getString(3);
                        cnt = rs.getInt(4);

                        // check if not the same transcript
                        if(!dbtrans.equals(trans)) {
                            trans = dbtrans;
                            source = feature = "";
                            hmSource = new HashMap<>();
                            hmResults.put(trans, hmSource);
                        }

                        // check if not the same source
                        if(!dbsource.equals(source)) {
                            source = dbsource;
                            feature = "";
                            hmFeature = new HashMap<>();
                            hmSource.put(source, hmFeature);
                        }

                        // check if not the same feature
                        if(!dbfeature.equals(feature)) {
                            feature = dbfeature;
                            featureCnt = 0;
                        }
                        featureCnt += cnt;
                        hmFeature.put(feature, featureCnt);
                    }
                    rs.close();

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return hmResults;
    }
    public HashMap<String, String> loadTransAnnotFeaturesString(){
        // transcript, source(feature,feature,...);source(feature...)
        HashMap<String, String> hmResults = new HashMap<>();

        ResultSet rs = getRS(sqlSelTransAFIds);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing transcript annotation feature ids...");

                try {
                    String afs = "";
                    int fcnt = 0;
                    String trans, source, feature;
                    String dbtrans, dbsource, dbfeature;
                    trans = source = feature = "";
                    while(rs.next()) {
                        dbtrans = rs.getString(1);
                        dbsource = rs.getString(2);
                        dbfeature = rs.getString(3);

                        // check if not the same transcript
                        if(!dbtrans.equals(trans)) {
                            if(!trans.isEmpty()) {
                                if(!afs.isEmpty())
                                    afs += ")";
                                hmResults.put(trans, afs);
                            }
                            trans = dbtrans;
                            source = feature = afs = "";
                        }

                        // check if not the same source
                        if(!dbsource.equals(source)) {
                            source = dbsource;
                            feature = "";
                            fcnt = 0;
                            afs += (afs.isEmpty()? "" : ");") + source + "(";
                        }

                        // check if not the same feature
                        if(!dbfeature.equals(feature)) {
                            feature = dbfeature;
                            afs += (fcnt++ == 0? "" : ",") + feature;
                        }
                    }
                    rs.close();

                    if(!trans.isEmpty()) {
                        if(!afs.isEmpty())
                            afs += ")";
                        hmResults.put(trans, afs);
                    }

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
            
        return hmResults;
    }
    public TransDataResults loadTransData(){
        TransDataResults trd = new TransDataResults();

        ResultSet rs = getRS(sqlSelTransData);
        try{
            if(rs.isBeforeFirst()) {
                long tstart = System.nanoTime();
                System.out.println("Processing transcript data...");
                try {
                    AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
                    String dbtrans, dbsource, dbfeature;
                    String trans, transName, gene, geneDesc, chromo, strand, cds;
                    String protein, proteinDesc, alignCat, alignAttrs, exons;
                    trans = transName = gene = chromo = strand = cds = "";
                    protein = alignCat = alignAttrs = exons = "";
                    DataAnnotation.AnnotationType at = null;
                    int length = 0;
                    HashMap<String, String> attrs;
                    while(rs.next()) {
                        dbtrans = rs.getString(2);
                        dbsource = rs.getString(3);
                        dbfeature = rs.getString(4);

                        // check if not the same transcript
                        if(!dbtrans.equals(trans)) {
                            // check if we already have an active transaction
                            if(!trans.isEmpty()) {
                                // add transaction data
                                DataAnnotation.TransData td = new DataAnnotation.TransData(trans, transName, gene, chromo, strand, length, cds,
                                                                  protein, alignCat, alignAttrs, exons);
                                trd.hmTransData.put(trans, td);
                            }

                            // process new transaction entry
                            trans = dbtrans;
                            at = DataAnnotation.AnnotationType.TRANS;
                            if(dbsource.equals(defs.transcript.source) && dbfeature.equals(defs.transcript.feature)) {
                                // clear values
                                gene = chromo = cds = "";
                                protein = alignCat = alignAttrs = exons = "";
                                // get transcript entry specific values
                                transName = rs.getString(9);
                                strand = rs.getString(7);
                                length = rs.getInt(13);
                                attrs = DataAnnotation.getAttributes(rs.getString(8));
                                if(attrs.containsKey(defs.attrPriClass))
                                    alignCat = attrs.get(defs.attrPriClass);
                                if(attrs.containsKey(defs.attrSecClass))
                                    alignAttrs = attrs.get(defs.attrSecClass);
                            }
                            else {
                                trd.hmTransData.clear();
                                //Warning remove and download again all references
                                logger.logError("3. Error loading gene-transcripts from database\n\nIf you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\\n\\nYou can find more information in our website: app.tappas.org/downloads");
                                app.ctls.alertWarning("3. Error loading gene-transcripts from database", "If you was using tappAS v0.99.06 or below before you downloaded v0.99.07, you have to delete all your projects and references that you can find in your 'tappasWorkspace/Projects' and 'tappasWorkspace/References'.\n\nYou can find more information in our website: app.tappas.org/download");
                                close();
                            }
                        }
                        else {
                            if(dbsource.equals(defs.cds.source) && dbfeature.equals(defs.cds.feature)) {
                                cds = rs.getString(5) + "-" + rs.getString(6);
                            }                        
                            else if(dbsource.equals(defs.gene.source) && dbfeature.equals(defs.gene.feature)) {
                                gene = rs.getString(9);
                                geneDesc = rs.getString(11);
                                if(!geneDesc.isEmpty())
                                    trd.hmGeneDescriptions.put(gene, geneDesc);
                            }                        
                            else if(dbsource.equals(defs.protein.source) && dbfeature.equals(defs.protein.feature)) {
                                protein = rs.getString(9);
                                at = DataAnnotation.AnnotationType.PROTEIN;
                                proteinDesc = rs.getString(11);
                                if(!proteinDesc.isEmpty())
                                    trd.hmProteinDescriptions.put(protein, proteinDesc);
                            }                        
                            else if(dbsource.equals(defs.genomic.source) && dbfeature.equals(defs.genomic.feature)) {
                                at = DataAnnotation.AnnotationType.GENOMIC;
                            }                        
                            else if(dbsource.equals(defs.exon.source) && dbfeature.equals(defs.exon.feature)) {
                                if(chromo.isEmpty()) {
                                    attrs = DataAnnotation.getAttributes(rs.getString(8));
                                    if(attrs.containsKey(defs.attrChr))
                                        chromo = attrs.get(defs.attrChr);
                                }
                                exons += (exons.isEmpty()? "" : ";") + rs.getString(5) + "-" + rs.getString(6);
                            }
                        }
                    }
                    rs.close();

                    // check if we still have an active transaction
                    if(!trans.isEmpty()) {
                        // add transaction data
                        DataAnnotation.TransData td = new DataAnnotation.TransData(trans, transName, gene, chromo, strand, length, cds,
                                                          protein, alignCat, alignAttrs, exons);
                        trd.hmTransData.put(trans, td);
                    }

                    long tend = System.nanoTime();
                    long duration = (tend - tstart)/1000000;
                    System.out.println("Total time: " + duration + " ms");
                } catch ( Exception e ) {
                    trd.hmTransData.clear();
                    trd.hmGeneDescriptions.clear();
                    trd.hmProteinDescriptions.clear();
                    try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                    logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage());
                }
                close();
            }
        } catch (SQLException ex) {
            System.out.println("Query hasn't results, error: " + ex);
        }
        return trd;
    }
    
    public boolean genFeatureSizes(){
        boolean result = false;
        int rows = 0;
        HashMap<String, HashMap<String, FeatureInfo>> hmFeatures = new HashMap<>();

        if(attach()) {
            ResultSet rs = getRS(sqlSelTransAFSizes);
            try{
                if(rs.isBeforeFirst()) {
                    result = true;
                    long tstart = System.nanoTime();
                    System.out.println("Processing feature sizes data...");
                    try {
                        String dbsource, dbfeature;
                        while(rs.next()) {
                            rows++;
                            dbsource = rs.getString(1);
                            dbfeature = rs.getString(2);

                            // we are going to track the length of regular features to
                            // determine if we want to display them as symbols or a blocks
                            // transcritps and proteins have a different cutoff size (NTs vs AAs)
                            // because their overall display length affects how much area symbols typically cover
                            if(!project.data.isRsvdFeature(dbsource, dbfeature)) {
                                String type = rs.getString(3);
                                int flength = rs.getInt(4);
                                boolean oversized = false;
                                // select statement limits to T and P
                                switch(type) {
                                    case "T":
                                        oversized = flength > DataAnnotation.TRANS_CUTOFF_LEN;
                                        break;
                                    case "P":
                                        oversized = flength > DataAnnotation.PROTEIN_CUTOFF_LEN;
                                        break;
                                    // genomic features are always displayed like genome browsers: blocks in their own track
                                    // do not include or it will break the code, see AnnotationSymbols draw function
                                }
                                if(!hmFeatures.containsKey(dbsource)) {
                                    HashMap<String, FeatureInfo> hm = new HashMap<>();
                                    FeatureInfo fi = new FeatureInfo();
                                    fi.posType = type;
                                    fi.cnt = 1;
                                    fi.min = fi.max = fi.total = flength;
                                    fi.oversized = oversized? 1 : 0;
                                    hm.put(dbfeature, fi);
                                    hmFeatures.put(dbsource, hm);
                                }
                                else {
                                    HashMap<String, FeatureInfo> hmFeature = hmFeatures.get(dbsource);                                 
                                    if(!hmFeature.containsKey(dbfeature)) {
                                        FeatureInfo fi = new FeatureInfo();
                                        fi.posType = type;
                                        fi.cnt = 1;
                                        fi.min = fi.max = fi.total = flength;
                                        fi.oversized = oversized? 1 : 0;
                                        hmFeature.put(dbfeature, fi);
                                    }
                                    else {
                                        FeatureInfo fi = hmFeature.get(dbfeature);
                                        if(!fi.posType.equals(type))
                                            System.out.println("Same sourcr:feature assigned to different types");
                                        fi.cnt += 1;
                                        if(fi.min > flength)
                                            fi.min = flength;
                                        if(fi.max < flength)
                                            fi.max = flength;
                                        fi.total += flength;
                                        if(oversized)
                                            fi.oversized++;
                                    }
                                }
                            }
                        }
                        rs.close();

                        long tend = System.nanoTime();
                        long duration = (tend - tstart)/1000000;
                        System.out.println("Total time: " + duration + " ms");
                    } catch ( Exception e ) {
                        result = false;
                        hmFeatures.clear();
                        try { rs.close(); } catch(Exception ce) { System.out.println(ce.getClass().getName() + " Close result set exception within exception: " + ce.getMessage()); }
                        logger.logError(e.getClass().getName() + " Code Exception: " + e.getMessage() );
                    }
                    close();
                }
            } catch (SQLException ex) {
                System.out.println("Query hasn't results, error: " + ex);
            }
            // save results to files
            if(result) {
                String txt = "source\tfeature\tposType\tmin\tmax\tsumTotal\toversized\tcntTotal\tpctOversized\n";
                String txtBlock = "";
                for(String src : hmFeatures.keySet()) {
                    HashMap<String, FeatureInfo> hmInfo = hmFeatures.get(src);
                    for(String feature : hmInfo.keySet()) {
                        FeatureInfo fi = hmInfo.get(feature);
                        double pct = ((double)fi.oversized/(double)fi.cnt) * 100.0;
                        if(pct >= 50)
                            txtBlock += src + "\t" + feature + "\n";
                        String spct = String.format("%.02f", pct);
                        txt += src + "\t" + feature + "\t" + fi.posType + "\t" + fi.min + "\t" + fi.max + "\t" + fi.total + "\t" + fi.oversized+ "\t" + fi.cnt +  "\t" + spct + "\n";
                    }
                }
                if(!txt.isEmpty()) {
                    Utils.saveTextToFile(txt, Paths.get(project.data.getProjectDataFolder(), DataProject.ANNOTATION_FEATURE_SIZES).toString());
                    if(!txtBlock.isEmpty())
                        Utils.saveTextToFile(txtBlock, Paths.get(project.data.getProjectDataFolder(), DataProject.ANNOTATION_BLOCK_FEATURES).toString());
                }
                else
                    logger.logWarning("Unable to get feature sizes to determine display format for data visualization.");
            }
        }
        return result;
    }
        
    //
    // Data Classes
    //
    public static class TransDataResults {
        HashMap<String, DataAnnotation.TransData> hmTransData = new HashMap<>();
        HashMap<String, String> hmGeneDescriptions = new HashMap<>();
        HashMap<String, String> hmProteinDescriptions = new HashMap<>();
    }
    public static class FeatureInfo {
        String posType;
        public int min, max, total, oversized, cnt;
        public FeatureInfo() {
            this.posType = "";
            this.min = this.max = this.oversized = this.cnt = 0;
        }
    }
}
