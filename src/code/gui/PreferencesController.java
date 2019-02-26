package code.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import javax.swing.*;

public class PreferencesController {
    @FXML
    private CheckBox showLogs;
    @FXML
    private Button chooseDownloadPath;
    @FXML
    private TextField downloadPathInput;

    public String getDownloadPath() {
        return downloadPathInput.getText();
    }

    public void chooseNewDownloadPath() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            downloadPathInput.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}
