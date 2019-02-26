package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class BodyController implements Initializable {
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

	public void disconnectClicked() {
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
		});
	}

	public void initialize(URL location, ResourceBundle resources) {
		Main.setBodyController(this);
	}
}
