/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgFileDownload extends DlgBase {
    ProgressBar pbProgress;
    Params params;
    Thread thread = null;
    boolean threadResult = false;
    long cntLoaded = 0;
    long cntTotal = 0;
    String inFile, outFolder;
    
    public DlgFileDownload(Project project, Params params, Window window) {
        super(project, window);
        this.params = params;
    }
    public HashMap<String, String> showAndWait(String inFile, String outFolder) {
        this.inFile = inFile;
        this.outFolder = outFolder;
        if(createDialog("FileDownload.fxml", "Downloading File...", false, true, null)) {
            // get control objects
            pbProgress = (ProgressBar) scene.lookup("#pbProgress");

            // initialize dialog
            dialog.initStyle(StageStyle.UTILITY);

            // setup dialog event handlers
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> hmParams = new Params(params.url, params.filePath, cntTotal, threadResult).getParams();
                return hmParams;
            });
            dialog.setOnShown(e-> { downloadFile(); });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return result.get();
        }
        return null;
    }
    
    //
    // File Download Functions
    //
    private void downloadFile() {
        // start thread and watchdog
        thread = new TaskThread();
        thread.start();
        startWatchdog();
    }
    private void startWatchdog() {
        Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                 Platform.runLater(() -> {
                     if(!thread.isAlive()) {
                         timer.cancel();
                         app.logInfo("File download exit value: " + (threadResult? "0" : "1"));
                         dialog.close();
                     }
                 });
            }
        }, 500, 1000);                            
    }
    private class TaskThread extends Thread {
        @Override
        public void run(){
            // make sure we have required parameters
            threadResult = false;
            cntLoaded = 0;
            cntTotal = 0;
            app.logInfo("File download thread is running...");
            if(!params.url.isEmpty() && !params.filePath.isEmpty()) {
                // prep UI
                updateMessage("Connecting to data server...");
                updateProgress(0.0);
                logger.logInfo("URL: " + params.url);
                logger.logInfo("File path: " + params.filePath);

                // establish server connection to file
                URLConnection conn = null;
                File f = null;
                FileOutputStream fos = null;
                try {
                    URL url = new URL(params.url);
                    conn = url.openConnection();
                    cntTotal = conn.getContentLengthLong();
                    logger.logInfo("Connection count: " + cntTotal);
                    if(cntTotal > 0 && cntTotal < Params.MAX_FILESIZE) {
                        // creat local folder/file and setup a stream
                        logger.logInfo("Creating path folder...");
                        Utils.createFilepathFolder(params.filePath);
                        logger.logInfo("Created path folder");
                        f = new File(params.filePath);
                        logger.logInfo("Created file");
                        fos = new FileOutputStream(f, false);
                        
                        // download data
                        String strTotal = getUnitString(cntTotal);
                        byte[] buf = new byte[1024 * 1024];
                        updateMessage("Downloaded " + getUnitString(cntLoaded) + " out of " + strTotal + "...");
                        InputStream is = conn.getInputStream();
                        int rdcnt = is.read(buf);
                        logger.logInfo("Started data download loop...");
                        while(rdcnt != -1) {
                            fos.write(buf, 0, rdcnt);
                            cntLoaded += rdcnt;
                            updateMessage("Downloaded " + getUnitString(cntLoaded) + " out of " + strTotal + "...");
                            updateProgress(Math.max(0.0, (double)cntLoaded/(double)cntTotal - 0.01));
                            rdcnt = is.read(buf);
                        }
                        try { is.close(); } catch (Exception e) {}
                        logger.logInfo("Done with data download loop...");
                        if(cntLoaded == cntTotal) {
                            // check if decompression requested
                            if(!inFile.isEmpty() && !outFolder.isEmpty()) {
                                // extract files, will log error if needed
                                updateMessage("Extracting files...");
                                threadResult = app.data.extractTarGzFiles(inFile, outFolder);
                            }
                            else
                                threadResult = true;
                            updateProgress(1);
                            if(threadResult)
                                updateMessage("File downloaded successfully: " + strTotal);
                        }
                        else {
                            updateMessage("Unable to download full file.\nDownloaded " + getUnitString(cntLoaded) + " out of " + strTotal);
                        }
                    }
                    else
                        updateMessage("Invalid file length: " + cntTotal);
                }
                catch(Exception e) {
                    if(conn == null) {
                        updateMessage("Unable to establish server connection: " + e.getMessage());
                        logger.logError("Unable to establish server connection: " + e.getMessage());
                    }
                    else {
                        updateMessage("Unable to download file: " + e.getMessage());
                        logger.logError("Unable to download file: " + e.getMessage());
                    }
                }
                finally {
                    // close file and delete if error
                    try {
                        if(fos != null)
                            fos.close();
                    }
                    catch(Exception e) {
                        System.out.println("Failed to delete download file fos for: " + params.filePath);
                    }
                    if(!threadResult) {
                        try {
                            if(f != null)
                                f.delete();
                        }
                        catch(Exception e) {
                            System.out.println("Failed to delete incomplete download file: " + params.filePath);
                        }
                    }
                }
            }
            else 
                updateMessage("Missing download file information.");
        }
    }
    private String getUnitString(long value) {
        String str;
        if(value > (1024L * 1024L * 1024L))
            str = (int)(value/(1024L * 1024L * 1024L)) + " GBs";
        else if(value > (1024L * 1024L))
            str = (int)(value/(1024L * 1024L)) + " MBs";
        else if(value > 1024L)
            str = (int)(value/1024L) + " KBs";
        else
            str = String.format("%d bytes", value);
        return str;
    }
    private void updateProgress(double value) {
        Platform.runLater(() -> {
            pbProgress.setProgress(value);
        });
    }
    private void updateMessage(String msg) {
        Platform.runLater(() -> {
            lblMsg.setText(msg);
        });
    }
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static long MAX_FILESIZE = (10L * 1024L * 1024L * 1024L);
        public static final String FILEURL_PARAM = "url";
        public static final String FILEPATH_PARAM = "file";
        public static final String FILESIZE_PARAM = "size";
        public static final String RESULT_PARAM = "result";

        public String url, filePath;
        public long fileSize;
        // result is not a forma parameter - it is the return OK flag for the dialog
        public boolean result;
        
        public Params() {
            url = "";
            filePath = "";
            fileSize= 0L;
            result = false;
        }
        public Params(String url, String filePath, long fileSize, boolean result) {
            this.url = url;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.result = result;
        }
        public Params(HashMap<String, String> hmParams) {
            this.url = hmParams.containsKey(FILEURL_PARAM)? hmParams.get(FILEURL_PARAM) : "";
            this.filePath = hmParams.containsKey(FILEPATH_PARAM)? hmParams.get(FILEPATH_PARAM) : "";
            this.fileSize = hmParams.containsKey(FILESIZE_PARAM)? Long.parseLong(hmParams.get(FILESIZE_PARAM)) : 0L;
            this.result = hmParams.containsKey(RESULT_PARAM)? Boolean.valueOf(hmParams.get(RESULT_PARAM)) : false;
            this.result = false;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(FILEURL_PARAM, url);
            hm.put(FILEPATH_PARAM, filePath);
            hm.put(FILESIZE_PARAM, String.format("%d", fileSize));
            hm.put(RESULT_PARAM, Boolean.toString(result));
            return hm;
        }
        // base class implements boolean save(String filepath)

        //
        // Static Functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
    }
}
