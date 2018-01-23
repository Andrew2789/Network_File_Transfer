import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class NftController implements Initializable {
    //Session setup elements
    @FXML
    private TextField nicknameInput;
    @FXML
    private TextField ipInput;
    @FXML
    private TextField portInput;
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

    //Preferences elements
    @FXML
    private CheckBox rememberFolders;
    @FXML
    private CheckBox pref2;

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

    private static final int portMin = 1024;
    private static final int portMax = 65535;

    private void setSettingsEnabled(boolean enabled) {
        nicknameInput.setDisable(!enabled);
        portInput.setDisable(!enabled);
        hosting.setDisable(!enabled);
    }

    public void connectClicked() {
        connectionStatus.setText("Connecting...");
        connectButton.setDisable(true);
        ipInput.setDisable(true);
        setSettingsEnabled(false);
        cancelButton.setVisible(true);
        Main.transferThread = new TransferThread(ipInput.getCharacters().toString(), Integer.parseInt(portInput.getCharacters().toString()), this);
        Main.transferThread.start();
    }

    public void connectFailed() {
        connectionStatus.setText("Failed to connect");
        connectButton.setDisable(false);
        ipInput.setDisable(false);
        setSettingsEnabled(true);
        cancelButton.setVisible(false);
    }

    public void listenClicked() {
        connectionStatus.setText("Listening...");
        listenButton.setDisable(true);
        setSettingsEnabled(false);
        cancelButton.setVisible(true);
        Main.transferThread = new TransferThread(Integer.parseInt(portInput.getCharacters().toString()), this);
        Main.transferThread.start();
    }

    public void listenFailed() {
        connectionStatus.setText("Could not host that port");
        listenButton.setDisable(false);
        setSettingsEnabled(true);
        cancelButton.setVisible(false);
    }

    public void cancelClicked() {
        Main.transferThread.exit();
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
        String port = portInput.getCharacters().toString();
        if (ipAddress.isEmpty() || port.isEmpty())
            return false;

        int portValue = Integer.parseInt(port);
        if (portValue < portMin || (ipAddress.length() - ipAddress.replace(".", "").length() != 3))
            return false;

        return ipAddress.lastIndexOf('.') != ipAddress.length()-1;
    }

    private boolean hostListenReady() {
        try {
            return Integer.parseInt(portInput.getCharacters().toString()) >= portMin;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void initialize(URL location, ResourceBundle resources) {
        /**
        client.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (clientConnectReady()) {
                    clientConnectClicked();
                }
            }
        });

        host.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (hostListenReady()) {
                    hostListenClicked();
                }
            }
        });**/

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

        portInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!updateNumberField(newValue, portMax)) {
                ((StringProperty)observable).setValue(oldValue);
            }
            if (hosting.isSelected()) {
                listenButton.setDisable(!hostListenReady());
            } else {
                connectButton.setDisable(!clientConnectReady());
            }
        });
    }
}
