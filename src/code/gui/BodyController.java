package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class BodyController implements Initializable {
	@FXML
	private GridPane bg;
	@FXML
	private CheckBox showLogs;
	@FXML
	private SplitPane logsDivider;
	@FXML
	private SpeedController speedController;
	@FXML
	private LogController logController;
	@FXML
	private PreferencesController preferencesController;
	@FXML
	private TransferController transferController;

	public ObservableList getDownloads() {
		return transferController.getDownloads();
	}

	public ObservableList getUploads() {
		return transferController.getUploads();
	}

	public String getDownloadPath() {
		return preferencesController.getDownloadPath();
	}

	public void uploadStopped() {
		speedController.uploadStopped();
	}

	public void downloadStopped() {
		speedController.downloadStopped();
	}

	public void updateUploadSpeed(long speed) {
		speedController.updateUploadSpeed(speed);
	}

	public void updateDownloadSpeed(long speed) {
		speedController.updateDownloadSpeed(speed);
	}

	public void requestFocus() {
		bg.requestFocus();
	}

	public void updateConnectionLog() {
	    logController.updateConnectionLog();
    }

    public void lockDownloadPath(boolean lock) {
        preferencesController.lockDownloadPath(lock);
    }

	public void showLogsChanged() {
		if (showLogs.isSelected()) {
			logsDivider.setDividerPosition(0, 0.7);
		} else {
			logsDivider.setDividerPosition(0, 1);
		}
	}

	public void confirmDisconnect() {
		Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to disconnect?", ButtonType.YES, ButtonType.NO);
		alert.showAndWait();

		if (alert.getResult() == ButtonType.YES) {
			disconnect();
		}
	}

	public void disconnect() {
		Main.killTransferThreads();
		Platform.runLater(() -> {
			transferController.clearTrees();

			uploadStopped();
			downloadStopped();

			Main.disconnected();
		});
	}

	public void addLogMessage(String message) {
		logController.addLogMessage(message);
	}

	public void addLogStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        addLogMessage(sw.toString());
    }

	public void initialize(URL location, ResourceBundle resources) {
		Main.body = this;
		preferencesController.setParent(this);
	}
}
