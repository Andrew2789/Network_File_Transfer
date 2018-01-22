package Controller;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ResourceBundle;

public class NftController implements Initializable {
    @FXML
    private ScrollPane settingsScroll;
    @FXML
    public GridPane settingsPane;

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

    //Connection status elements
    @FXML
    private Label connectionStatus;

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

    public void hostingChanged() {
        ipInput.setDisable(hosting.isSelected());
        if (hosting.isSelected()) {
            connectButton.setText("Host");
            connectPrompt.setText("Host on");
            connectButton.setDisable(!hostListenReady());
        } else {
            connectButton.setText("Connect");
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
                connectButton.setDisable(!hostListenReady());
            } else {
                connectButton.setDisable(!clientConnectReady());
            }
        });
    }
}
