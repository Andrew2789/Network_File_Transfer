package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class SpeedController implements Initializable {

    @FXML
    private LineChart netSpeedGraph;
    @FXML
    private Label upSpeedLabel, downSpeedLabel;

    private XYChart.Series<Integer, Double> upSpeedSeries = new XYChart.Series<>(), downSpeedSeries = new XYChart.Series<>();

    private long uploadSpeedAvg = 0, downloadSpeedAvg = 0;
    private int uploadSpeedSeriesUpdate = 0, downloadSpeedSeriesUpdate = 0;
    private String uploadSymbol = "⬆", downloadSymbol = "⬇";
    private final int ticksPerGraphUpdate = 5, maxGraphDataPoints = 50;

    public void uploadStopped() {
        if (uploadSpeedAvg != 0) {
            uploadSpeedAvg = 0;
            if (upSpeedSeries.getData().size() > maxGraphDataPoints) {
                upSpeedSeries.getData().remove(0);
            }
            for (XYChart.Data<Integer, Double> data : upSpeedSeries.getData()) {
                data.setXValue(data.getXValue() + 1);
            }
            upSpeedSeries.getData().add(new XYChart.Data<>(0, (double) 0));
        }
    }

    public void downloadStopped() {
        if (downloadSpeedAvg != 0) {
            downloadSpeedAvg = 0;
            if (downSpeedSeries.getData().size() > maxGraphDataPoints) {
                downSpeedSeries.getData().remove(0);
            }
            for (XYChart.Data<Integer, Double> data : downSpeedSeries.getData()) {
                data.setXValue(data.getXValue() + 1);
            }
            downSpeedSeries.getData().add(new XYChart.Data<>(0, (double)0));
        }
    }

    public void updateUploadSpeed(long speed) {
        uploadSpeedAvg = (uploadSpeedAvg * (ticksPerGraphUpdate-1) + speed)/ticksPerGraphUpdate;
        Platform.runLater(() -> {
            upSpeedLabel.setText(uploadSymbol + " " + FileTreeItem.generate3SFSizeString(uploadSpeedAvg) + "/s");
            if (uploadSpeedSeriesUpdate == 0) {
                if (upSpeedSeries.getData().size() > maxGraphDataPoints) {
                    upSpeedSeries.getData().remove(0);
                }
                for (XYChart.Data<Integer, Double> data : upSpeedSeries.getData()) {
                    data.setXValue(data.getXValue() + 1);
                }
                if (Main.readThreadActive()) {
                    if (downSpeedSeries.getData().size() == maxGraphDataPoints) {
                        downSpeedSeries.getData().remove(0);
                    }
                    for (XYChart.Data<Integer, Double> data : downSpeedSeries.getData()) {
                        data.setXValue(data.getXValue() + 1);
                    }
                    downSpeedSeries.getData().add(new XYChart.Data<>(0, ((double)downloadSpeedAvg)/1000000));
                }
                upSpeedSeries.getData().add(new XYChart.Data<>(0, ((double)uploadSpeedAvg)/1000000));
                uploadSpeedSeriesUpdate = ticksPerGraphUpdate;
            }
            uploadSpeedSeriesUpdate--;
        });
    }

    public void updateDownloadSpeed(long speed) {
        downloadSpeedAvg = (downloadSpeedAvg * (ticksPerGraphUpdate-1) + speed)/ticksPerGraphUpdate;
        Platform.runLater(() -> {
            downSpeedLabel.setText(downloadSymbol + " " + FileTreeItem.generate3SFSizeString(downloadSpeedAvg) + "/s");
            if (downloadSpeedSeriesUpdate == 0) {
                if (!Main.writeThreadActive()) {
                    if (downSpeedSeries.getData().size() == maxGraphDataPoints) {
                        downSpeedSeries.getData().remove(0);
                    }
                    for (XYChart.Data<Integer, Double> data : downSpeedSeries.getData()) {
                        data.setXValue(data.getXValue() + 1);
                    }
                    downSpeedSeries.getData().add(new XYChart.Data<>(0, ((double)downloadSpeedAvg)/1000000));
                }
                downloadSpeedSeriesUpdate = ticksPerGraphUpdate;
            }
            downloadSpeedSeriesUpdate--;
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        netSpeedGraph.getData().clear();
        netSpeedGraph.getData().add(upSpeedSeries);
        netSpeedGraph.getData().add(downSpeedSeries);
    }
}
