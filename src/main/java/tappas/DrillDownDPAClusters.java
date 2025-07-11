/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.binding.Bindings;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import java.util.ArrayList;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownDPAClusters extends DlgBase {
    static int id = 0;

    LineChart chartMain;
    Button btnHelpTbl, btnExportChart;
    Pane paneMenu;
    ScrollPane spClusters;
    GridPane grdClusters;
    ImageView imgView;
    AnchorPane apClusters;

    int toolId;
    DataDPA dpaData;
    String dpaDataTypeName;
    String grpName;
    ArrayList<Integer> lstClusters;
    String exportFilename;
    
    public DrillDownDPAClusters(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public void showAndWait(String dpaDataTypeName, String grpName, ArrayList<Integer> lstClusters) {
        if(createToolDialog("DrillDownDPAClusters.fxml", "DPA Gene Data Visualization",  null)) {
            this.dpaData = new DataDPA(project);
            this.dpaDataTypeName = dpaDataTypeName;
            this.grpName = grpName;
            this.lstClusters = lstClusters;
            exportFilename = "tappAS_DPA_" + dpaDataTypeName + " ExpProfileClusters.png";

            // get control objects
            paneMenu = (Pane) scene.lookup("#paneMenu");
            spClusters = (ScrollPane) scene.lookup("#spClusters");
            apClusters = (AnchorPane) spClusters.getContent().lookup("#apClusters");

            // setup dialog
            setProjectName();
            dialog.setTitle("DPA " + dpaDataTypeName + " Expression Profile Clusters");
            grdClusters = new GridPane();
            int nclusters = lstClusters.size();
            int ncols = 1;
            if(nclusters >= 4)
                ncols = 2;
            int nrows = (nclusters + ncols - 1) / ncols;
            for(int col = 0; col < ncols; col++)
                grdClusters.addColumn(col);
            for(int row = 0; row < nrows; row++)
                grdClusters.addRow(row);
            int cnum = 1;
            for(int row = 0; row < nrows; row++) {
                for(int col = 0; col < ncols; col++) {
                    String filename = dpaData.getDPAClusterImageFilepath(grpName, cnum++);
                    imgView = new ImageView("file:" + filename);
                    imgView.fitWidthProperty().bind(Bindings.divide(Bindings.subtract(spClusters.widthProperty(), 5), ncols));
                    imgView.setPreserveRatio(true);
                    grdClusters.add(imgView, col, row);
                }
            }
            apClusters.getChildren().add(grdClusters);
            
            app.ctls.setupArchorPaneExport(apClusters, "DPA " + dpaDataTypeName + " Expression Profile Clusters", exportFilename, null);
            
            // setup menu buttons
            double yoffset = 3.0;
            btnExportChart = app.ctls.addImgButton(paneMenu, "export.png", "Export plot image...", yoffset, 32, true);
            btnExportChart.setOnAction((event) -> { onButtonExportChart(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DPA_Clusters.html", Tappas.getWindow()); });

            // process dialog - modeless
            dialog.showAndWait();
        }
    }
    @Override
    protected void onButtonExportChart() {
        SnapshotParameters sP = new SnapshotParameters();
        double x = apClusters.getWidth();
        double ratio = 3840/x;
        apClusters.setScaleX(ratio);
        apClusters.setScaleY(ratio);
        WritableImage img = apClusters.snapshot(sP, null);
        double newX = apClusters.getWidth();
        ratio = x/newX;
        apClusters.setScaleX(ratio);
        apClusters.setScaleY(ratio);
        
        //WritableImage img = apClusters.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("DPA " + dpaDataTypeName + " Expression Profile Clusters", exportFilename, img);
    }
}
