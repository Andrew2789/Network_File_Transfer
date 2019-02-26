package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.util.ResourceBundle;

public class ConnectionController implements Initializable {
    @FXML
    private TextField ipInput, clientPortInput, hostPortInput, nameInput;
    @FXML
    private Button connectButton, hostButton;
    @FXML
    private TextArea log;

    private static final int PORT_MIN = 1024;

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            if (log.getText().isEmpty()) {
                log.setText(message);
            } else {
                log.setText(log.getText() + "\n" + message);
            }
        });
    }

    public void connectClicked() {
        String ipAddress = ipInput.getCharacters().toString();
        int port = Integer.parseInt(clientPortInput.getCharacters().toString());

        Main.clientTransferThreads(ipAddress, port,
                () -> addLogMessage("Failed to connect."),
                () -> {
                    addLogMessage("Successfully connected.");
                    Main.connected();
                },
                () -> addLogMessage("Disconnected."));
        /*if (Main.clientTransferThreads(nameInput.getCharacters().toString(), ipAddress, port)) {
            Main.showGame();
        } else {
            errorLabel.setText("Error: Failed to connect");
            errorLabel.setVisible(true);
        }*/
    }

    public void hostClicked() {
        int port = Integer.parseInt(hostPortInput.getCharacters().toString());

        Main.hostTransferThreads(port,
                () -> addLogMessage("Failed to connect."),
                () -> {
                    addLogMessage("Successfully connected.");
                    Main.connected();
                },
                () -> addLogMessage("Server created, listening..."),
                () -> addLogMessage("Disconnected."));
        /*if (!Main.createServer(port)) {
            errorLabel.setText("Error: Failed to host");
            errorLabel.setVisible(true);
        }

            if (!Main.joinGame(nameInput.getCharacters().toString(), "127.0.0.1", port)) {
                errorLabel.setText("Error: Hosted, but failed to connect");
                errorLabel.setVisible(true);
                Main.killThreads();
            }

        Main.showGameSetup();*/
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
