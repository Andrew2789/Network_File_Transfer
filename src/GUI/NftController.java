package GUI;

import Logic.FileTreeItem;
import Logic.Main;
import java.io.File;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;
import javax.swing.JFileChooser;

public class NftController implements Initializable {
    //Session setup elements
    @FXML
    private TextField nicknameInput;
    @FXML
    private TextField ipInput;
	@FXML
	private TextField port1Input;
	@FXML
	private TextField port2Input;
    @FXML
    private Label connectPrompt;
    @FXML
    private CheckBox hosting;
    @FXML
    private Button connectButton;
    @FXML
    private Button listenButton;

    //Connection status elements
    @FXML
    private Label connectionStatus;
    @FXML
    private Button cancelButton;
    @FXML
	private Button disconnectButton;

    //Preferences elements
    @FXML
    private CheckBox rememberFolders;
	@FXML
	private CheckBox sslEncryption;
	@FXML
	private CheckBox darkTheme;

    //Network speed elements
    @FXML
    private LineChart netSpeedGraph;
    @FXML
    private Label upSpeedLabel;
    @FXML
    private Label downSpeedLabel;

    //Main tree displays
    @FXML
    private TreeView uploadsTree;
    @FXML
    private TreeView downloadsTree;
    @FXML
    private TreeView sendableTree;
    @FXML
    private TreeView receivableTree;
    @FXML
    private Button queueDownload;
    @FXML
    private Button dequeueDownload;
    @FXML
	private Button clearCompleted;
    @FXML
    private Button addSendable;
    @FXML
    private Button removeSendable;

    private static final int portMin = 1024;
    private static final int portMax = 65534;

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

	private void setSettingsEnabled(boolean enabled) {
        nicknameInput.setDisable(!enabled);
        port1Input.setDisable(!enabled);
        port2Input.setDisable(!enabled);
        hosting.setDisable(!enabled);
    }

    public void connectClicked() {
        connectionStatus.setText("Connecting...");
        connectButton.setDisable(true);
        ipInput.setDisable(true);
        setSettingsEnabled(false);
        cancelButton.setVisible(true);
        Main.clientTransferThreads(ipInput.getCharacters().toString(), Integer.parseInt(port1Input.getCharacters().toString()),
				Integer.parseInt(port2Input.getCharacters().toString()), this);
    }

	public void connectFailed() {
		Platform.runLater(() -> {
			connectionStatus.setText("Failed to connect");
			connectButton.setDisable(false);
			ipInput.setDisable(false);
			setSettingsEnabled(true);
			cancelButton.setVisible(false);
		});
	}

	public void connectOrListenSucceeded() {
		Platform.runLater(() -> {
			connectionStatus.setText("Connected");
			cancelButton.setVisible(false);
			disconnectButton.setVisible(true);
			uploadsTree.setDisable(false);
			downloadsTree.setDisable(false);
			receivableTree.setDisable(false);
		});
	}

    public void listenClicked() {
        connectionStatus.setText("Listening...");
        listenButton.setDisable(true);
        setSettingsEnabled(false);
        cancelButton.setVisible(true);
		Main.hostTransferThreads(Integer.parseInt(port1Input.getCharacters().toString()), Integer.parseInt(port2Input.getCharacters().toString()), this);
    }

    public void listenFailed() {
		Platform.runLater(() -> {
			connectionStatus.setText("Could not host that port");
			listenButton.setDisable(false);
			setSettingsEnabled(true);
			cancelButton.setVisible(false);
		});
    }

    public void cancelClicked() {
		Main.killTransferThreads();
        if (hosting.isSelected()) {
            listenButton.setDisable(false);
            connectionStatus.setText("Listen attempt cancelled");
        } else {
            connectButton.setDisable(false);
            connectionStatus.setText("Connect attempt cancelled");
        }
        cancelButton.setVisible(false);
        setSettingsEnabled(true);
    }

    public void disconnectClicked() {
		Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure you want to disconnect?", ButtonType.YES, ButtonType.NO);
		alert.showAndWait();

		if (alert.getResult() == ButtonType.YES) {
			Main.killTransferThreads();
			connectionStatus.setText("Disconnected");
			if (hosting.isSelected()) {
				listenButton.setDisable(false);
			} else {
				connectButton.setDisable(false);
			}
			disconnectButton.setVisible(false);
			uploadsTree.setDisable(true);
			downloadsTree.setDisable(true);
			receivableTree.setDisable(true);
			setSettingsEnabled(true);
		}
	}

    public void hostingChanged() {
        ipInput.setDisable(hosting.isSelected());
        connectButton.setVisible(!hosting.isSelected());
        listenButton.setVisible(hosting.isSelected());
        if (hosting.isSelected()) {
            connectPrompt.setText("Listen on");
            listenButton.setDisable(!hostListenReady());
        } else {
            connectPrompt.setText("Connect to");
            connectButton.setDisable(!clientConnectReady());
        }
    }

    private boolean updateNumberField(String newValue, int max) {
        boolean updateValue = true;
        if (!newValue.isEmpty()) {
            try {
                int fieldValue = Integer.parseInt(newValue);
                if (fieldValue < 0 || fieldValue > max || newValue.charAt(0) == '0') {
                    updateValue = false;
                }
            } catch (NumberFormatException e) {
                updateValue = false;
            }
        }
        return updateValue;
    }

    private boolean clientConnectReady() {
        String ipAddress = ipInput.getCharacters().toString();
		String port1 = port1Input.getCharacters().toString();
		String port2 = port1Input.getCharacters().toString();
        if (ipAddress.isEmpty() || port1.isEmpty() || port2.isEmpty())
            return false;

		int port1Value = Integer.parseInt(port1);
		int port2Value = Integer.parseInt(port2);
        if (port1Value < portMin || port2Value < portMin || (ipAddress.length() - ipAddress.replace(".", "").length() != 3))
            return false;

        return ipAddress.lastIndexOf('.') != ipAddress.length()-1;
    }

    private boolean hostListenReady() {
        try {
            return (Integer.parseInt(port1Input.getCharacters().toString()) >= portMin) &&
					(Integer.parseInt(port2Input.getCharacters().toString()) >= portMin);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void newSendable() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int ret = chooser.showOpenDialog(null);
        if(ret == JFileChooser.APPROVE_OPTION) {
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
			for (File file: folder.listFiles()) {
				subfolders.add(getSubfolder(file, false));
			}
			for (FileTreeItem subfolder: subfolders) {
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
			for (FileTreeItem subfolder: subfolders) {
				newItem.getChildren().add(subfolder);
			}
		}
		return newItem;
    }

    public void sendableClicked() {
    	removeSendable.setDisable(sendableTree.getSelectionModel().getSelectedItem() == null || !((FileTreeItem)sendableTree.getSelectionModel().getSelectedItem()).isRoot());
	}

    public void deleteSelectedSendable() {
		synchronized (sendableTree.getRoot().getChildren()) {
			FileTreeItem toRemove = (FileTreeItem)sendableTree.getSelectionModel().getSelectedItem();
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
				downloadsTree.getRoot().getChildren().add(toQueue.makeReceivableCopy(Main.getNextDownloadRootId()));
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
		ipInput.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (clientConnectReady() && !hosting.isSelected()) {
					connectClicked();
				}
			}
		});

		port1Input.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (clientConnectReady() && !hosting.isSelected()) {
					connectClicked();
				} else if (hostListenReady() && hosting.isSelected()) {
					listenClicked();
				}
			}
		});

		port2Input.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				if (clientConnectReady() && !hosting.isSelected()) {
					connectClicked();
				} else if (hostListenReady() && hosting.isSelected()) {
					listenClicked();
				}
			}
		});

        ipInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                String[] octets = newValue.split("\\.");
                int numPeriods = newValue.length() - newValue.replace(".", "").length();
                if (octets.length > 4 || numPeriods > 3 || numPeriods > octets.length) {
                    ((StringProperty) observable).setValue(oldValue);
                    return;
                }
                try {
                    for (int i = 0; i < octets.length; i++)
                        if (octets[i].isEmpty()) {
                            if (i != octets.length-1) {
                                ((StringProperty)observable).setValue(oldValue);
                                break;
                            }
                        } else {
                            int octetValue = Integer.parseInt(octets[i]);
                            if (octetValue < 0 || octetValue > 255 || newValue.charAt(0) == '0') {
                                ((StringProperty)observable).setValue(oldValue);
                                break;
                            }
                        }
                } catch (NumberFormatException e) {
                    ((StringProperty)observable).setValue(oldValue);
                }
            }
            connectButton.setDisable(!clientConnectReady());
        });

		port1Input.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!updateNumberField(newValue, portMax)) {
				((StringProperty)observable).setValue(oldValue);
			}
			if (hosting.isSelected()) {
				listenButton.setDisable(!hostListenReady());
			} else {
				connectButton.setDisable(!clientConnectReady());
			}
		});

		port2Input.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!updateNumberField(newValue, portMax)) {
				((StringProperty)observable).setValue(oldValue);
			}
			if (hosting.isSelected()) {
				listenButton.setDisable(!hostListenReady());
			} else {
				connectButton.setDisable(!clientConnectReady());
			}
		});

        downloadsTree.setRoot(new TreeItem<String>("root"));
        downloadsTree.setShowRoot(false);
        uploadsTree.setRoot(new TreeItem<String>("root"));
        uploadsTree.setShowRoot(false);
        sendableTree.setRoot(new TreeItem<String>("root"));
        sendableTree.setShowRoot(false);
        receivableTree.setRoot(new TreeItem<String>("root"));
        receivableTree.setShowRoot(false);
    }
}
