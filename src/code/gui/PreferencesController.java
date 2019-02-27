package code.gui;

import code.network.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class PreferencesController {
    @FXML
    private TextField downloadPathInput;

    private String path = "Downloads";

    private BodyController parent;

    public String getDownloadPath() {
        return path;
    }

    public void setParent(BodyController parent) {
        this.parent = parent;
    }

    public void lockDownloadPath(boolean lock) {
        path = downloadPathInput.getText();
        Platform.runLater(() -> downloadPathInput.setDisable(lock));
    }

    public void disconnect() {
        parent.confirmDisconnect();
    }

    public void chooseNewDownloadPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        File selected = chooser.showDialog(Main.getStage());
        if (selected != null) {
            downloadPathInput.setText(selected.getAbsolutePath());
        }
    }
}
