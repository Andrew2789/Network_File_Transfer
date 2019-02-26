package code.gui;

import code.network.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import javax.swing.*;
import java.io.File;

public class PreferencesController {
    @FXML
    private TextField downloadPathInput;

    public String getDownloadPath() {
        return downloadPathInput.getText();
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
