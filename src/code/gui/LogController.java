package code.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogController {

    @FXML
    private TextArea log;

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            if (log.getText().isEmpty()) {
                log.setText(message);
            } else {
                log.setText(log.getText() + "\n" + message);
            }
        });
    }
}
