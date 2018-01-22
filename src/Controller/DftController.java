package Controller;

import java.net.URL;
import java.util.ResourceBundle;

import Logic.TransferThread;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

public class DftController implements Initializable {
	@FXML
	private static AnchorPane setup;
	@FXML
	private static GridPane transfer;
	@FXML
	private static SetupController setupController;
	@FXML
	private static TransferController transferController;

	private static TransferThread transferThread = null;

	public static TransferThread getTransferThread() {
		return transferThread;
	}

	public static void createTransferThread(TransferThread transferThread) {
		DftController.transferThread = transferThread;
		DftController.transferThread.setControllers(setupController, transferController);
		DftController.transferThread.start();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setup.setVisible(false);
		transfer.setVisible(true);
	}
}
