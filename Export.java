/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Export extends AppObject {
    public static final String webResourcePath = "/tappas/resources/web/";
    public static enum ExportSelection { Selected, Highlighted, All }

    public Export(App app) {
        super(app);
    }
    
    //
    // Export Setup Functions
    //
    
    public void setupChartExport(Chart chart, String name, String filename, MenuItem[] items, TabBase tabBase) {
        
        //Trying to save with background in white
        //chart.setStyle(".chart-plot-background {-fx-background-color: transparent;}");

        ContextMenu cm = new ContextMenu();
        if(items != null) {
            cm.getItems().addAll(Arrays.asList(items));
            cm.getItems().add(new SeparatorMenuItem());
        }
        MenuItem item = new MenuItem("Export image...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = chart.getWidth();
            double ratio = 3840/x;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            WritableImage img = chart.snapshot(sP, null);
            double newX = chart.getWidth();
            ratio = x/newX;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            exportImage(name, filename, img);
        });
        
        item = new MenuItem("Copy Image to Clipboard...");
        item.setOnAction((event) -> { Utils.copyToClipboardImage(chart); });
        cm.getItems().add(item);
        
        chart.setOnMouseClicked((event) -> {
            if(MouseButton.SECONDARY.equals(event.getButton())) {
              cm.show(Tappas.getStage(), event.getScreenX(), event.getScreenY());
            }  
        });
        
        // there are cases where we want to indicate which data the menu is for
        // for example in the Annotation Summary pie chart, click on a database
        chart.setUserData(tabBase);
        chart.setOnContextMenuRequested((event) -> {
            TabBase tb = (TabBase) chart.getUserData();
            if(tb != null)
                tb.onContextMenuRequested(chart, cm);
        });
    }
    public void setupCanvasExport(Canvas canvas, String name, String filename) {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export image...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = canvas.getWidth();
            double ratio = 3840/x;
            canvas.setScaleX(ratio);
            canvas.setScaleY(ratio);
            WritableImage img = canvas.snapshot(sP, null);
            double newX = canvas.getWidth();
            ratio = x/newX;
            canvas.setScaleX(ratio);
            canvas.setScaleY(ratio);
            exportImage(name, filename, img);
        });
        canvas.setOnMouseClicked((event) -> {
            if(MouseButton.SECONDARY.equals(event.getButton())) {
              cm.show(Tappas.getStage(), event.getScreenX(), event.getScreenY());
            }  
        });
    }
    public void setupTableExport(SubTabBase subtab, ContextMenu cm, boolean separator) {
        if(separator) {
            SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            cm.getItems().add(separatorMenuItem);
        }
        MenuItem item = new MenuItem("Export table data...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { subtab.onButtonExport(); });
    }
    public void setupTableExport(TableView tbl, ContextMenu cm, boolean separator, String filename1, String filename2) {
        if(separator) {
            SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            cm.getItems().add(separatorMenuItem);
        }
        MenuItem item = new MenuItem("Export selected table rows...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { exportTableData(tbl, false, filename1); });
        item = new MenuItem("Export ALL table rows...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { exportTableData(tbl, true, filename2); });
    }
    public void setupTableExport(DlgBase dlg, ContextMenu cm, boolean separator) {
        if(separator) {
            SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            cm.getItems().add(separatorMenuItem);
        }
        MenuItem item = new MenuItem("Export table data...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { dlg.onButtonExport();});
    }
    public void setupSimpleTableExport(TableView tbl, ContextMenu cm, boolean separator, String filename) {
        if(separator) {
            SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();
            cm.getItems().add(separatorMenuItem);
        }
        MenuItem item = new MenuItem("Export table data...");
        cm.getItems().add(item);
        item.setOnAction((event) -> { exportTableData(tbl, true, filename); });
    }
    
    //
    // Clipboard Handling Functions
    //
    public void addCopyToClipboardHandler(TableView tbl) {
        tbl.setOnKeyPressed((event) -> {
            StringBuilder clipboardString = new StringBuilder();
            ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
            int rowcnt = lstIdxs.size();
            if(rowcnt > 0) {
                ArrayList<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();
                ObservableList<TableColumn> subcols;
                int colcnt = cols.size();
                int colnum = 0;
                for(TableColumn col : cols) {
                    if(col.isVisible()) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add)
                                lstsubcols.add(colnum);
                        }
                    }
                    colnum++;
                }
                for(int row : lstIdxs) {
                    String line = "";
                    boolean firstcol = true;
                    for(int col = 0; col < colcnt; col++) {
                        if(cols.get(col).isVisible()) {
                            if(cols.get(col).getCellObservableValue(row) != null) {
                                line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue(row).getValue();
                                firstcol = false;
                            }
                            else if(lstsubcols.contains(col)) {
                                subcols = cols.get(col).getColumns();
                                for (TableColumn subcol : subcols) {
                                    if(subcol.isVisible() && subcol.getCellObservableValue(row) != null) {
                                        line += (firstcol? "" : "\t") + subcol.getCellObservableValue(row).getValue();
                                        firstcol = false;
                                    }
                                }
                            }
                        }
                    }
                    if(!line.isEmpty()) {
                        clipboardString.append(line);
                        clipboardString.append(System.lineSeparator());
                    }
                }
                
                final ClipboardContent content = new ClipboardContent();
                content.putString(clipboardString.toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
    }
    
    //
    // Export File Functions
    //
    public File getTableExportFileName(String filename) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify Export File Path and Name");
        fileChooser.setInitialFileName(filename);
        String expFolder = app.userPrefs.getExportDataFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setExportDataFolder("");
        }
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Export Files", "*.tsv"));
        File selectedFile = fileChooser.showSaveDialog(Tappas.getWindow());
        return selectedFile;
    }
    public File getImageExportFileName(String filename) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify PNG Image File Path and Name");
        fileChooser.setInitialFileName(filename);
        String expFolder = app.userPrefs.getExportImageFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setExportImageFolder("");
        }
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        File selectedFile = fileChooser.showSaveDialog(Tappas.getWindow());
        return selectedFile;
    }
    
    //
    // Export Functions
    //
    public void exportImage(String name, String filename, Image wi) {
        try {
            File f = getImageExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportImageFolder(f.getParent());
                Tappas.getScene().setCursor(Cursor.WAIT);
                try {
                    // can support JPEG, PNG, GIF, BMP and WBMP but PNG is best
                    ImageIO.write(SwingFXUtils.fromFXImage(wi, null),"png", f);
                } catch (IOException ex) {
                    logger.logError("Unable to export image '" + name + "' to file: " + ex.getMessage());
                }
            }
        }
        catch(Exception ex) {
            logger.logError("Unable to export image '" + name + "' to file: " + ex.getMessage());
        }
        finally {
            Tappas.getScene().setCursor(Cursor.DEFAULT);
        }
    }
    public void exportImageFile(String name, String filename, String imgFilename) {
        try {
            File f = getImageExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportImageFolder(f.getParent());
                Tappas.getScene().setCursor(Cursor.WAIT);
                // only png for now
                Utils.copyFile(Paths.get(imgFilename), f.toPath());
            }
        }
        catch(Exception ex) {
            logger.logError("Unable to export image '" + name + "' to file: " + ex.getMessage());
        }
        finally {
            Tappas.getScene().setCursor(Cursor.DEFAULT);
        }
    }
    public void exportTableData(TableView tbl, boolean all, String filename) {
        ObservableList<Integer> lstidx;
        if(all) {
            lstidx = FXCollections.observableArrayList();
            int cnt = tbl.getItems().size();
            for(int i = 0; i < cnt; i++)
                lstidx.add(i);
        }
        else // getSelectedIndices() is supposed to be ObservableList<Integer> but returns ObservableList which generates unchecked warning!
            lstidx = tbl.getSelectionModel().getSelectedIndices();
        int rowcnt = lstidx.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());
                String header = "";
                ArrayList<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();
                boolean first = true;
                int colcnt = cols.size();
                ObservableList<TableColumn> subcols;
                int colnum = 0;
                for(TableColumn col : cols) {
                    if(col.isVisible()) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add) {
                                lstsubcols.add(colnum);
                                for(int j = 0; j < subcnt; j++) {
                                    if(subcols.get(j).isVisible()) {
                                        header += (first? "#" : "\t") + col.getText() + "." + subcols.get(j).getText();
                                        first = false;
                                    }
                                }
                            }
                        }
                        else {
                            String coltxt = col.getText();
                            if(coltxt.isEmpty() && (col.getUserData() instanceof String))
                                coltxt = (String) col.getUserData();
                            header += (first? "#" : "\t") + coltxt;
                        }
                        first = false;
                    }
                    colnum++;
                }
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    String line;
                    for(int row = 0; row < rowcnt; row++) {
                        line = "";
                        boolean firstcol = true;
                        for(int col = 0; col < colcnt; col++) {
                            if(cols.get(col).isVisible()) {
                                if(cols.get(col).getCellObservableValue((int) lstidx.get(row)) != null) {
                                    line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue((int) lstidx.get(row)).getValue();
                                    firstcol = false;
                                }
                                else if(lstsubcols.contains(col)) {
                                    subcols = cols.get(col).getColumns();
                                    for (TableColumn subcol : subcols) {
                                        if(subcol.isVisible() && subcol.getCellObservableValue((int) lstidx.get(row)) != null) {
                                            line += (firstcol? "" : "\t") + subcol.getCellObservableValue((int) lstidx.get(row)).getValue();
                                            firstcol = false;
                                        }
                                    }
                                }
                            }
                        }
                        if(!line.isEmpty())
                            writer.write(line + "\n");
                    }
                } catch(Exception e) {
                    logger.logError("Unable to write table data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There are no data rows available for exporting.");
    }
    public void exportTableDataToFile(TableView tbl, boolean all, String filename) {
        List<Integer> lstidx;
        if(!all)
            lstidx = RowSelection.getSelectRowIdxs(tbl.getItems(), "tappas.DataApp$SelectionDataResults");
        else {
            lstidx = new ArrayList<>();
            int cnt = tbl.getItems().size();
            for(int i = 0; i < cnt; i++)
                lstidx.add(i);
        }
        int rowcnt = lstidx.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());

                String header = "";
                List<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();

                boolean first = true;
                int colcnt = cols.size();
                ObservableList<TableColumn> subcols;
                int colnum = 0;
                for(TableColumn col : cols) {
                    if(col.isVisible() && !col.getText().isEmpty() && !col.getText().equals("#")) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add) {
                                lstsubcols.add(colnum);
                                for(int j = 0; j < subcnt; j++) {
                                    if(subcols.get(j).isVisible()) {
                                        header += (first? "#" : "\t") + col.getText() + "." + subcols.get(j).getText();
                                        first = false;
                                    }
                                }
                            }
                        }
                        else {
                            String coltxt = col.getText();
                            if(coltxt.isEmpty() && (col.getUserData() instanceof String))
                                coltxt = (String) col.getUserData();
                            header += (first? "#" : "\t") + coltxt;
                        }
                        first = false;
                    }
                    colnum++;
                }
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    String line;
                    for(int row = 0; row < rowcnt; row++) {
                        line = "";
                        boolean firstcol = true;
                        for(int col = 0; col < colcnt; col++) {
                            if(cols.get(col).isVisible() && !cols.get(col).getText().isEmpty() && !cols.get(col).getText().equals("#")) {
                                if(cols.get(col).getCellObservableValue((int) lstidx.get(row)) != null) {
                                    line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue((int) lstidx.get(row)).getValue();
                                }
                                else if(lstsubcols.contains(col)) {
                                    subcols = cols.get(col).getColumns();
                                    for (TableColumn subcol : subcols) {
                                        if(subcol.isVisible() && subcol.getCellObservableValue((int) lstidx.get(row)) != null) {
                                            line += (firstcol? "" : "\t") + subcol.getCellObservableValue((int) lstidx.get(row)).getValue();
                                            firstcol = false;
                                        }
                                    }
                                }
                                firstcol = false;
                            }
                        }
                        if(!line.isEmpty())
                            writer.write(line + "\n");
                    }
                } catch(Exception e) {
                    logger.logError("Unable to write table data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There are no data rows available for exporting.");
    }
    public void exportTableDataToFile(TableView tbl, Export.ExportSelection selection, String filename) {
        List<Integer> lstidx;
        switch(selection) {
            case Selected:
                lstidx = RowSelection.getSelectRowIdxs(tbl.getItems(), "tappas.DataApp$SelectionDataResults");
                break;
            case Highlighted:
                lstidx = new ArrayList<>();
                ObservableList<Integer> lst = tbl.getSelectionModel().getSelectedIndices();
                lst.forEach((idx) -> {
                    lstidx.add(idx);
                });
                break;
            default:
                lstidx = new ArrayList<>();
                int cnt = tbl.getItems().size();
                for(int i = 0; i < cnt; i++)
                    lstidx.add(i);
        }
        int rowcnt = lstidx.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());

                String header = "";
                ArrayList<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();

                boolean first = true;
                int colcnt = cols.size();
                ObservableList<TableColumn> subcols;
                int colnum = 0;
                for(TableColumn col : cols) {
                    if(col.isVisible() && !col.getText().isEmpty() && !col.getText().equals("#")) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add) {
                                lstsubcols.add(colnum);
                                for(int j = 0; j < subcnt; j++) {
                                    if(subcols.get(j).isVisible()) {
                                        header += (first? "#" : "\t") + col.getText() + "." + subcols.get(j).getText();
                                        first = false;
                                    }
                                }
                            }
                        }
                        else {
                            String coltxt = col.getText();
                            if(coltxt.isEmpty() && (col.getUserData() instanceof String))
                                coltxt = (String) col.getUserData();
                            header += (first? "#" : "\t") + coltxt;
                        }
                        first = false;
                    }
                    colnum++;
                }
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    String line;
                    for(int row = 0; row < rowcnt; row++) {
                        line = "";
                        boolean firstcol = true;
                        for(int col = 0; col < colcnt; col++) {
                            if(cols.get(col).isVisible() && !cols.get(col).getText().isEmpty() && !cols.get(col).getText().equals("#")) {
                                if(cols.get(col).getCellObservableValue((int) lstidx.get(row)) != null) {
                                    line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue((int) lstidx.get(row)).getValue();
                                }
                                else if(lstsubcols.contains(col)) {
                                    subcols = cols.get(col).getColumns();
                                    for (TableColumn subcol : subcols) {
                                        if(subcol.isVisible() && subcol.getCellObservableValue((int) lstidx.get(row)) != null) {
                                            line += (firstcol? "" : "\t") + subcol.getCellObservableValue((int) lstidx.get(row)).getValue();
                                            firstcol = false;
                                        }
                                    }
                                }
                                firstcol = false;
                            }
                        }
                        if(!line.isEmpty())
                            writer.write(line + "\n");
                    }
                } catch(Exception e) {
                    logger.logError("Unable to write table data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There are no data rows available for exporting.");
    }
    public void exportTableDataListToFile(TableView tbl, boolean all, HashMap<String, String> hmColNames, String filename) {
        List<Integer> lstidx;
        if(!all)
            lstidx = RowSelection.getSelectRowIdxs(tbl.getItems(), "tappas.DataApp$SelectionDataResults");
        else {
            lstidx = new ArrayList<>();
            int cnt = tbl.getItems().size();
            for(int i = 0; i < cnt; i++)
                lstidx.add(i);
        }
        int rowcnt = lstidx.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());

                String header = "";
                ArrayList<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();
                boolean first = true;
                int colcnt = cols.size();
                ObservableList<TableColumn> subcols;
                int colnum = 0;
                for(TableColumn col : cols) {
                    //if(col.isVisible()) {
                    System.out.println(col.getText());
                    if(hmColNames.containsKey(col.getText().trim())) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add) {
                                lstsubcols.add(colnum);
                                for(int j = 0; j < subcnt; j++) {
                                    String subColName = hmColNames.get(col.getText().trim());
                                    if(subcols.get(j).isVisible() && (subColName.isEmpty() || subColName.equals(subcols.get(j).getText()))) {
                                        header += (first? "#" : "\t") + col.getText() + "." + subcols.get(j).getText();
                                        first = false;
                                    }
                                }
                            }
                        }
                        else {
                            String coltxt = col.getText();
                            if(coltxt.isEmpty() && (col.getUserData() instanceof String))
                                coltxt = (String) col.getUserData();
                            header += (first? "#" : "\t") + coltxt;
                        }
                        first = false;
                    }
                    colnum++;
                }
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    String line;
                    for(int row = 0; row < rowcnt; row++) {
                        line = "";
                        boolean firstcol = true;
                        for(int col = 0; col < colcnt; col++) {
                            if(hmColNames.containsKey(cols.get(col).getText().trim())) {
                                if(cols.get(col).getCellObservableValue((int) lstidx.get(row)) != null) {
                                    line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue((int) lstidx.get(row)).getValue();
                                }
                                else if(lstsubcols.contains(col)) {
                                    subcols = cols.get(col).getColumns();
                                    for (TableColumn subcol : subcols) {
                                        String subColName = hmColNames.get(cols.get(col).getText().trim());
                                        if(subcol.isVisible() && (subColName.isEmpty() || subColName.equals(subcol.getText())) &&
                                                                    subcol.getCellObservableValue((int) lstidx.get(row)) != null) {
                                            line += (firstcol? "" : "\t") + subcol.getCellObservableValue((int) lstidx.get(row)).getValue();
                                            firstcol = false;
                                        }
                                    }
                                }
                                firstcol = false;
                            }
                        }
                        if(!line.isEmpty())
                            writer.write(line + "\n");
                    }
                } catch(Exception e) {
                    logger.logError("Unable to write list data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There are no data rows available for exporting.");
    }
    public void exportTableDataListToFile(TableView tbl, Export.ExportSelection selection, HashMap<String, String> hmColNames, String filename) {
        List<Integer> lstidx;
        switch(selection) {
            case Selected:
                lstidx = RowSelection.getSelectRowIdxs(tbl.getItems(), "tappas.DataApp$SelectionDataResults");
                break;
            case Highlighted:
                lstidx = new ArrayList<>();
                ObservableList<Integer> lst = tbl.getSelectionModel().getSelectedIndices();
                for(int idx : lst)
                    lstidx.add(idx);
                break;
            default:
                lstidx = new ArrayList<>();
                int cnt = tbl.getItems().size();
                for(int i = 0; i < cnt; i++)
                    lstidx.add(i);
        }
        int rowcnt = lstidx.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());

                String header = "";
                ArrayList<Integer> lstsubcols = new ArrayList<>();
                ObservableList<TableColumn> cols = tbl.getColumns();
                boolean first = true;
                int colcnt = cols.size();
                ObservableList<TableColumn> subcols;
                int colnum = 0;
                for(TableColumn col : cols) {
                    System.out.println(col.getText());
                    if(hmColNames.containsKey(col.getText().trim())) {
                        subcols = col.getColumns();
                        if(subcols != null && subcols.size() > 0) {
                            int subcnt = subcols.size();
                            boolean add = false;
                            for(int j = 0; j < subcnt; j++) {
                                if(subcols.get(j).isVisible()) {
                                    add = true;
                                    break;
                                }
                            }
                            if(add) {
                                lstsubcols.add(colnum);
                                for(int j = 0; j < subcnt; j++) {
                                    String subColName = hmColNames.get(col.getText().trim());
                                    if(subcols.get(j).isVisible() && (subColName.isEmpty() || subColName.equals(subcols.get(j).getText()))) {
                                        header += (first? "#" : "\t") + col.getText() + "." + subcols.get(j).getText();
                                        first = false;
                                    }
                                }
                            }
                        }
                        else {
                            String coltxt = col.getText();
                            if(coltxt.isEmpty() && (col.getUserData() instanceof String))
                                coltxt = (String) col.getUserData();
                            header += (first? "#" : "\t") + coltxt;
                        }
                        first = false;
                    }
                    colnum++;
                }
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    String line;
                    for(int row = 0; row < rowcnt; row++) {
                        line = "";
                        boolean firstcol = true;
                        for(int col = 0; col < colcnt; col++) {
                            if(hmColNames.containsKey(cols.get(col).getText().trim())) {
                                if(cols.get(col).getCellObservableValue((int) lstidx.get(row)) != null) {
                                    line += (firstcol? "" : "\t") + cols.get(col).getCellObservableValue((int) lstidx.get(row)).getValue();
                                }
                                else if(lstsubcols.contains(col)) {
                                    subcols = cols.get(col).getColumns();
                                    for (TableColumn subcol : subcols) {
                                        String subColName = hmColNames.get(cols.get(col).getText().trim());
                                        if(subcol.isVisible() && (subColName.isEmpty() || subColName.equals(subcol.getText())) &&
                                                                    subcol.getCellObservableValue((int) lstidx.get(row)) != null) {
                                            line += (firstcol? "" : "\t") + subcol.getCellObservableValue((int) lstidx.get(row)).getValue();
                                            firstcol = false;
                                        }
                                    }
                                }
                                firstcol = false;
                            }
                        }
                        if(!line.isEmpty())
                            writer.write(line + "\n");
                    }
                } catch(Exception e) {
                    logger.logError("Unable to write list data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There are no data rows available for exporting.");
    }
    public void exportListToFile(ArrayList<String> lstItems, String filename) {
        int rowcnt = lstItems.size();
        if(rowcnt > 0) {
            File f = getTableExportFileName(filename);
            if(f != null) {
                app.userPrefs.setExportDataFolder(f.getParent());

                String header = "#item";
                Writer writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f.getPath(), false), "utf-8"));
                    writer.write(header + "\n");
                    for(String item : lstItems)
                        writer.write(item + "\n");
                } catch(Exception e) {
                    logger.logError("Unable to write list data to file: " + e.getMessage());
                } finally {
                   try {if(writer != null) writer.close();} catch (Exception e) {}
                }
            }
        }
        else
            app.ctls.alertInformation("Export Table Data", "There is no data available for exporting.");
    }
    public void exportFileToFile(String srcFilepath, String filename) {
        File f = getTableExportFileName(filename);
        if(f != null) {
            app.userPrefs.setExportDataFolder(f.getParent());
            Writer writer = null;
            try {
                Utils.copyFile(Paths.get(srcFilepath), f.toPath());
            } catch(Exception e) {
                logger.logError("Unable to export data to file: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) {}
            }
        }
    }
}
