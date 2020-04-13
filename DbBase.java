/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DbBase extends AppObject {
    String dbpath = "";
    Connection c = null;

    public DbBase(Project project) {
        super(project, null);
    }
    public void initialize() {
    }
    
    //
    // Internal Functions
    //
    
    // Open database connection - create db if needed
    // (delete db file to create from scratch)
    protected boolean open() {
        return connect();
    }
    // Close database connection
    protected void close() {
        if(c != null) {
            try {
                c.close();
            } catch ( Exception e ) {
                logger.logInfo(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
            c = null;
        }
    }
    // connect to db, automatically creates empty DB file if not present
    protected boolean connect() {
        if(c == null) {
            try {
                // no longer needed in recent versions but it's harmless
                Class.forName("org.sqlite.JDBC");
                
                // set properties, like user and password, if needed
                // Properties props = new Properties();
                c = DriverManager.getConnection("jdbc:sqlite:" + dbpath);
            } catch ( Exception e ) {
                logger.logInfo( "ERROR: Unable to connect to DB " + dbpath + ".");
                logger.logInfo(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }
        return (c != null);
    }
    // create table - will remove existing one if present
    public boolean createTable(String tblName, String sqlCreate) {
        boolean result = false;
        if(connect()) {
            Statement stmt = null;
            try {
                // remove table if it exists - caller should have checked if needed
                dropTable(tblName);
                stmt = c.createStatement();
                stmt.executeUpdate(sqlCreate);
                stmt.close();
                result = true;
            } catch ( Exception e ) {
                try { if(stmt != null) stmt.close(); } catch(Exception ce) { System.out.println("Code exception closing Statement object while handling exception."); }
                logger.logInfo( "ERROR: Unable to create DB table " + tblName + "." + "Instruction: " + sqlCreate);
                logger.logInfo( e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }
        return result;
    }
    
    // check if given table exits
    public boolean tableExists(String tblName) {
        boolean result = false;
        
        if(connect()) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = c.createStatement();
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tblName + "';";
                rs = stmt.executeQuery(sql);
                result = rs.next();
                stmt.close();
                rs.close();
            } catch ( Exception e ) {
                try { if(stmt != null) stmt.close(); } catch(Exception ce) { System.out.println("Code exception closing Statement object while handling exception.");; }
                try { if(rs.isBeforeFirst()) rs.close(); } catch(Exception ce) { System.out.println("Code exception closing RS object while handling exception."); }
                logger.logInfo(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }
        return result;
    }
    // drop given table if it exists
    public void dropTable(String tblName) {
        if(connect()) {
            Statement stmt = null;
            try {
                if(tableExists(tblName)) {
                    stmt = c.createStatement();
                    String sql = "DROP TABLE " + tblName;
                    stmt.executeUpdate(sql);    
                    stmt.close();
                }
            } catch ( Exception e ) {
                try { if(stmt != null) stmt.close(); } catch(Exception ce) { System.out.println("Code exception closing Statement object while handling exception."); }
                logger.logInfo(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }
    }
    
    // get result set
    protected ResultSet getRS(String sqlSelect) {
        ResultSet rs = null;
        long tstart = System.nanoTime();
        logger.logDebug("Running select query: " + sqlSelect);
        
        // open the database connection
        if(open()) {
            Statement stmt = null;
            try {
                stmt = c.createStatement();
                rs = stmt.executeQuery(sqlSelect);
                // do not do stmt.close(); or will lose rs
            } catch ( Exception e ) {
                try { if(stmt != null) stmt.close(); } catch(Exception ce) {;}
                try { if(rs.isBeforeFirst()) rs.close(); } catch(Exception ce) {;}
                rs = null;
                logger.logInfo(e.getClass().getName() + " Code Exception: " + e.getMessage() );
            }
        }

        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        logger.logDebug("Total time: " + duration + " ms");
        return rs;
    }
}
