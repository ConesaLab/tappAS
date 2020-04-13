/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
    WARNING: Do not add log message calls to logger or could end up in endless loop
*/

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Logger {
    public enum Level { Debug, Info, Warning, Error }
    
    private final App app;
    private String logdate = "";
    private String logpath = "";
    private Level level = Level.Info;
    
    public Logger(App app, String logpath, Level level) {
        this.app = app;
        this.logpath = logpath;
        this.level = level;
    }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    public void logInfo(String msg) { logMsg(Level.Info, msg); }
    public void logWarning(String msg) { logMsg(Level.Warning, msg); }
    public void logError(String msg) { logMsg(Level.Error, msg); }
    public void logDebug(String msg) { logMsg(Level.Debug, msg); }
    
    public void logMsg(Level msgLevel, String msg) {
        // ignore if level too low for current setting
        if(msgLevel.ordinal() < level.ordinal())
            return;
        
        // display UI log button if warning or error logged and show enabled
        if(msgLevel.equals(Level.Error) || msgLevel.equals(Level.Warning))
            showSeeAppLog(true);
        
        // compose message
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        String datestr = date.toString();
        String line = "";
        if(!datestr.equals(logdate)) {
            logdate = datestr;
            line = "\n" + date.toString() + " " + date.format(DateTimeFormatter.ofPattern("E")) + " " + time.format(DateTimeFormatter.ofPattern("H:mm")) + "\n";
        }
        line += getTime(time) + " - " + getMsgLevel(msgLevel) + msg;

        // always end with a new line
        if(line.charAt(line.length() - 1) != '\n')
            line += "\n";
        
        // call function to display message on log UI
        updateLogDisplay(line);
        
        // update log file
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logpath, true), "utf-8"));
            writer.write(line);
        } catch(Exception e) {
            updateLogDisplay("logMsg exception: " + e.getMessage());
        } finally {
            try {
                if(writer != null)
                    writer.close();
            } catch (Exception e) {
                System.out.println("Close log file exception: " + e.getMessage());
            }
        }
    }
    public void logClear() {
        clearLogDisplay();
        Path path = Paths.get(logpath);
        if(Files.exists(path)) {
            try { Files.delete(path); } catch (IOException e) {
                updateLogDisplay("logClear delete file code Exception: " + e.getMessage());
            }
        }
    }
    // NOTE: should add msg level to call or use a derived logger instead - low priority
    public String getLogMsgLine(String msg, boolean first) {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        String line = "";
        if(first)
            line = "\n" + date.toString() + " " + date.format(DateTimeFormatter.ofPattern("E")) + " " + time.format(DateTimeFormatter.ofPattern("H:mm")) + "\n";
        line += getTime(time) + " - " + msg;

        // always end with a new line
        if(line.charAt(line.length() - 1) != '\n')
            line += "\n";
        return line;
    }

    //
    // UI log display related functions - Override as needed
    //
    public void updateLogDisplay(String msg) {
        // call application to display message on log UI by default
        app.updateLogDisplay(msg);
    }
    public void clearLogDisplay() {
        // clear application display by default
        app.clearLogDisplay();
    }
    public void showSeeAppLog(boolean show) {
        // show 'See App Log' button on top tool bar
        app.ctlr.showSeeAppLog(show);
    }
    
    //
    // Static Functions
    //
    public static void logMsgToFile(String msg, String filepath) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, true), "utf-8"));
            writer.write(msg);
        } catch(Exception e) {
            System.out.println("logMsgToFile exception: " + e.getMessage());
        } finally {
            try {
                if(writer != null)
                    writer.close();
            } catch (Exception e) {
                System.out.println("Close log file exception: " + e.getMessage());
            }
        }
    }
    public static String getLogFromFile(String filepath, int maxLines) {
        // load log file if available
        String log = "";
        try {
            Path path = Paths.get(filepath);
            if(Files.exists(path)) {
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                int nlines = lines.size();
                int idx = 0;
                if(nlines > maxLines)
                    idx = nlines - maxLines;
                for(; idx < nlines; idx++)
                    log += lines.get(idx) + "\n";
            }
        } catch(IOException e) {
            System.out.println("getLogFromFile Exception: " + e.getMessage());
        }
        return log;
    }
    
    //
    // Internal Functions
    //
    private String getTime(LocalTime time) {
        String strtime;
        if(level == Level.Debug)
            strtime = time.toString();
        else
            strtime = time.format(DateTimeFormatter.ofPattern("H:mm"));
        return strtime;
    }
    private String getMsgLevel(Level msgLevel) {
        String str = "";
        switch(msgLevel) {
            case Debug:
                str = "[DBG] ";
                break;
            case Warning:
                str = "[WARN] ";
                break;
            case Error:
                str = "[ERROR] ";
                break;
            // NOTE: no display for [Info] for less clutter
        }
        return str;
    }
}
