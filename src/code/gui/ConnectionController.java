package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class ConnectionController implements Initializable {
    @FXML
    private BorderPane bg;
    @FXML
    private TextField ipInput, clientPortInput, hostPortInput, nameInput;
    @FXML
    private Button connectButton, hostButton;
    @FXML
    private TextArea log;

    private static final int PORT_MIN = 1024;

    public void requestFocus() {
        bg.requestFocus();
    }

    public void setControlsDisabled(boolean disable) {
        ipInput.setDisable(disable);
        clientPortInput.setDisable(disable);
        hostPortInput.setDisable(disable);
        nameInput.setDisable(disable);
        if (disable) {
            connectButton.setDisable(true);
            hostButton.setDisable(true);
        } else {
            connectButton.setDisable(!clientConnectReady());
            hostButton.setDisable(!hostListenReady());
        }
    }

    public void setLog(String logText) {
        Platform.runLater(() -> {
            log.setText(logText);
        });
    }

    public void connectClicked() {
        String ipAddress = ipInput.getCharacters().toString();
        int port = Integer.parseInt(clientPortInput.getCharacters().toString());
        setControlsDisabled(true);

        Main.clientTransferThreads(ipAddress, port,
                (e) -> {
                    Main.body.addLogMessage("Failed to connect.");
                    Main.body.addLogStackTrace(e);
                    setControlsDisabled(false);},
                () -> {
                    Main.body.addLogMessage("Successfully connected.");
                    Main.connected();
                    setControlsDisabled(false);},
                () -> {
                    Main.body.addLogMessage("Disconnected.");
                    Main.disconnected();
                    setControlsDisabled(false);
                });
    }

    public void hostClicked() {
        int port = Integer.parseInt(hostPortInput.getCharacters().toString());
        setControlsDisabled(true);

        Main.hostTransferThreads(port,
                (e) -> {
                    Main.body.addLogMessage("Failed to listen.");
                    Main.body.addLogStackTrace(e);
                    setControlsDisabled(false);},
                () -> {
                    Main.body.addLogMessage("Successfully connected.");
                    Main.connected();
                    setControlsDisabled(false);},
                () -> Main.body.addLogMessage("Server created, listening..."),
                () -> {
                    Main.body.addLogMessage("Disconnected.");
                    Main.disconnected();
                    setControlsDisabled(false);
                });
    }

    private boolean updatePortField(String newValue) {
        if (!newValue.isEmpty()) {
            try {
                int fieldValue = Integer.parseInt(newValue);
                if (fieldValue < 0 || fieldValue > 65536 || newValue.charAt(0) == '0') {
                    return true;
                }
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    private boolean clientConnectReady() {
        if (nameInput.getCharacters().toString().isEmpty()) {
            return false;
        }
        String ipAddress = ipInput.getCharacters().toString();
        String port = clientPortInput.getCharacters().toString();
        if (ipAddress.isEmpty() || port.isEmpty())
            return false;

        int portValue = Integer.parseInt(port);
        if (portValue < PORT_MIN || (ipAddress.length() - ipAddress.replace(".", "").length() != 3))
            return false;

        return ipAddress.lastIndexOf('.') != ipAddress.length() - 1;
    }

    private boolean hostListenReady() {
        if (nameInput.getCharacters().toString().isEmpty()) {
            return false;
        }
        try {
            return Integer.parseInt(hostPortInput.getCharacters().toString()) >= PORT_MIN;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main.connection = this;

        ipInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (clientConnectReady()) {
                    connectClicked();
                }
            }
        });

        clientPortInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (clientConnectReady()) {
                    connectClicked();
                }
            }
        });

        hostPortInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (hostListenReady()) {
                    hostClicked();
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
                            if (i != octets.length - 1) {
                                ((StringProperty) observable).setValue(oldValue);
                                break;
                            }
                        } else {
                            int octetValue = Integer.parseInt(octets[i]);
                            if (octetValue < 0 || octetValue > 255 || newValue.charAt(0) == '0') {
                                ((StringProperty) observable).setValue(oldValue);
                                break;
                            }
                        }
                } catch (NumberFormatException e) {
                    ((StringProperty) observable).setValue(oldValue);
                }
            }
            connectButton.setDisable(!clientConnectReady());
        });

        clientPortInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatePortField(newValue)) {
                ((StringProperty) observable).setValue(oldValue);
            }
            connectButton.setDisable(!clientConnectReady());
        });

        hostPortInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatePortField(newValue)) {
                ((StringProperty) observable).setValue(oldValue);
            }
            hostButton.setDisable(!hostListenReady());
        });

        nameInput.textProperty().addListener(((observable, oldValue, newValue) -> {
            connectButton.setDisable(!clientConnectReady());
            hostButton.setDisable(!hostListenReady());
        }));
    }
}
