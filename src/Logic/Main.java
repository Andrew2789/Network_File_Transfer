package Logic;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static TransferThread transferThread = null;
    public static TransferControlThread transferControlThread = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/nft.fxml"));
        primaryStage.setTitle("Network File Transfer");
        Scene scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(getClass().getResource("/GUI/dark.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(430);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (transferThread != null) {
            transferThread.exit();
        }
        if (transferControlThread != null) {
            transferControlThread.exit();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
