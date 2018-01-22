package Controller;

import Logic.SetupState;
import Logic.TransferThread;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

public class SetupController implements Initializable {
	@FXML
	private Button menuConnect;
	@FXML
	private Button menuHost;

	@FXML
	private Button clientConnect;
	@FXML
	private Button clientBack;
	@FXML
	private TextField clientIpInputField;
	@FXML
	private TextField clientPortInputField;
	@FXML
	private TextField clientTimeoutInputField;

	@FXML
	private Button hostListen;
	@FXML
	private Button hostBack;
	@FXML
	private TextField hostPortInputField;

	@FXML
	private Button connectingBack;
	@FXML
	private ProgressIndicator connectingSpinner;
	@FXML
	private Label connectingTitle;
	@FXML
	private Label connectingText;

	@FXML
	private GridPane menu;
	@FXML
	private GridPane client;
	@FXML
	private GridPane host;
	@FXML
	private GridPane connecting;

	private static final int portMin = 1024;
	private static final int portMax = 65535;
	private static final int timeoutMax = 99;

	private SetupState prevState = SetupState.MAIN;

	public void menuConnectClicked() {
		menu.setVisible(false);
		client.setVisible(true);
		prevState = SetupState.MAIN;
	}

	public void menuHostClicked() {
		menu.setVisible(false);
		host.setVisible(true);
		prevState = SetupState.MAIN;
	}

	public void clientConnectClicked() {
		connectingTitle.setText("Connecting");
		connectingText.setText(String.format("Attempting to establish connection to %s...",
				String.format("%s:%s", clientIpInputField.getCharacters().toString(), clientPortInputField.getCharacters().toString())));
		connectingSpinner.setVisible(true);
		connectingBack.setVisible(false);
		prevState = SetupState.CLIENT;

		client.setVisible(false);
		connecting.setVisible(true);
		DftController.createTransferThread(new TransferThread(clientIpInputField.getCharacters().toString(),
			Integer.parseInt(clientPortInputField.getCharacters().toString()),
			Integer.parseInt(clientTimeoutInputField.getCharacters().toString())));
	}

	public void clientConnectFailed() {
		connectingTitle.setText("Failed to connect");
		connectingText.setText(String.format("Could not establish a connection to %s.",
				String.format("%s:%s", clientIpInputField.getCharacters().toString(), clientPortInputField.getCharacters().toString())));
		connectingSpinner.setVisible(false);
		connectingBack.setVisible(true);
	}

	public void clientBackClicked() {
		client.setVisible(false);
		menu.setVisible(true);
		prevState = SetupState.CLIENT;
	}

	public void hostListenClicked() {
		connectingTitle.setText("Connecting");
		connectingText.setText(String.format("Attempting to host on port %s...", hostPortInputField.getCharacters().toString()));
		connectingSpinner.setVisible(true);
		connectingBack.setVisible(false);
		prevState = SetupState.HOST;

		host.setVisible(false);
		connecting.setVisible(true);
		DftController.createTransferThread(new TransferThread(Integer.parseInt(hostPortInputField.getCharacters().toString())));
	}

	public void hostListenFailed() {
		connectingText.setText(String.format("Error: Could not host on port %s.", hostPortInputField.getCharacters().toString()));
		connectingSpinner.setVisible(false);
		connectingBack.setVisible(true);
	}

	public void hostBackClicked() {
		host.setVisible(false);
		menu.setVisible(true);
		prevState = SetupState.HOST;
	}

	public void connectingBackClicked() {
		connecting.setVisible(false);
		switch (prevState) {
			case CLIENT:
				client.setVisible(true);
				break;
			case HOST:
				host.setVisible(true);
				break;
		}
		prevState = SetupState.CONNECTING;
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
		String ipAddress = clientIpInputField.getCharacters().toString();
		String port = clientPortInputField.getCharacters().toString();
		String timeout = clientTimeoutInputField.getCharacters().toString();
		if (ipAddress.isEmpty() || port.isEmpty() || timeout.isEmpty())
			return false;

		int portValue = Integer.parseInt(port);
		if (portValue < portMin || (ipAddress.length() - ipAddress.replace(".", "").length() != 3))
			return false;

		return ipAddress.lastIndexOf('.') != ipAddress.length()-1;
	}

	private boolean hostListenReady() {
		try {
			return Integer.parseInt(hostPortInputField.getCharacters().toString()) >= portMin;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

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
		});

		clientIpInputField.textProperty().addListener((observable, oldValue, newValue) -> {
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
			clientConnect.setDisable(!clientConnectReady());
		});

		clientPortInputField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!updateNumberField(newValue, portMax)) {
				((StringProperty)observable).setValue(oldValue);
			}
			clientConnect.setDisable(!clientConnectReady());
		});

		clientTimeoutInputField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!updateNumberField(newValue, timeoutMax)) {
				((StringProperty)observable).setValue(oldValue);
			}
			clientConnect.setDisable(!clientConnectReady());
		});

		hostPortInputField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!updateNumberField(newValue, portMax)) {
				((StringProperty)observable).setValue(oldValue);
			}
			hostListen.setDisable(!hostListenReady());
		});
	}
}
