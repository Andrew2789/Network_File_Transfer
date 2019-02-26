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
	//Connection status elements
	@FXML
	private Label connectionStatus;
	@FXML
	private Button disconnectButton;


	//Main tree displays
	@FXML
	private TreeView uploadsTree, downloadsTree, sendableTree, receivableTree;
	@FXML
	private Button queueDownload, removeSendable;


	@FXML
	private SpeedController speedController;
	@FXML
	private LogController logController;
	@FXML
	private PreferencesController preferencesController;
	@FXML
	private TransferController transferController;


	public ObservableList getSendables() {
		return sendableTree.getRoot().getChildren();
	}

	public ObservableList getReceivables() {
		return receivableTree.getRoot().getChildren();
	}

	public ObservableList getDownloads() {
		return downloadsTree.getRoot().getChildren();
	}

	public ObservableList getUploads() {
		return uploadsTree.getRoot().getChildren();
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
			connectionStatus.setText("Disconnected");
			disconnectButton.setVisible(false);
			uploadsTree.setDisable(true);
			downloadsTree.setDisable(true);
			receivableTree.setDisable(true);

			uploadsTree.getRoot().getChildren().clear();
			downloadsTree.getRoot().getChildren().clear();
			receivableTree.getRoot().getChildren().clear();

			uploadStopped();
			downloadStopped();
		});
	}

	public void newSendable() {
		JFileChooser chooser = new JFileChooser(".");
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int ret = chooser.showOpenDialog(null);
		if (ret == JFileChooser.APPROVE_OPTION) {
			File[] files = chooser.getSelectedFiles();
			synchronized (sendableTree.getRoot().getChildren()) {
				for (File file : files) {
					FileTreeItem newItem = getSubfolder(file, true);
					sendableTree.getRoot().getChildren().add(newItem);
				}
			}
			Main.sendableAdded();
		}
	}

	private FileTreeItem getSubfolder(File folder, boolean root) {
		LinkedList<FileTreeItem> subfolders = null;
		long size = 0;
		if (folder.isDirectory()) {
			subfolders = new LinkedList<>();
			for (File file : folder.listFiles()) {
				subfolders.add(getSubfolder(file, false));
			}
			for (FileTreeItem subfolder : subfolders) {
				size += subfolder.getSize();
			}
		} else {
			size = folder.length();
		}
		FileTreeItem newItem;
		if (root)
			newItem = new FileTreeItem(folder, size, Main.getNextSendableRootId());
		else
			newItem = new FileTreeItem(folder, size);

		if (subfolders != null) {
			for (FileTreeItem subfolder : subfolders) {
				newItem.getChildren().add(subfolder);
			}
		}
		return newItem;
	}

	public void sendableClicked() {
		removeSendable.setDisable(sendableTree.getSelectionModel().getSelectedItem() == null || !((FileTreeItem) sendableTree.getSelectionModel().getSelectedItem()).isRoot());
	}

	public void deleteSelectedSendable() {
		synchronized (sendableTree.getRoot().getChildren()) {
			FileTreeItem toRemove = (FileTreeItem) sendableTree.getSelectionModel().getSelectedItem();
			Main.sendableRemoved(toRemove.getId());
			toRemove.getParent().getChildren().remove(toRemove);
			sendableTree.getSelectionModel().clearSelection();
		}
		removeSendable.setDisable(true);
	}

	public void receivableClicked() {
		queueDownload.setDisable(receivableTree.getSelectionModel().getSelectedItem() == null);
	}

	public void queueSelectedForDownload() {
		synchronized (downloadsTree.getRoot().getChildren()) {
			synchronized (receivableTree.getRoot().getChildren()) {
				FileTreeItem toQueue = (FileTreeItem) receivableTree.getSelectionModel().getSelectedItem();
				downloadsTree.getRoot().getChildren().add(toQueue.receivableToDownload(Main.getNextDownloadRootId()));
				receivableTree.getSelectionModel().clearSelection();
			}
		}
		Main.downloadQueued();
		queueDownload.setDisable(true);
	}

	public void cancelSelectedDownload() {

	}

	public void clearCompletedDownloads() {

	}

	public void initialize(URL location, ResourceBundle resources) {
		downloadsTree.setRoot(new TreeItem<>("root"));
		uploadsTree.setRoot(new TreeItem<>("root"));
		sendableTree.setRoot(new TreeItem<>("root"));
		receivableTree.setRoot(new TreeItem<>("root"));
	}
}
