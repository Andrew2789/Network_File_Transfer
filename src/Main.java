import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static TransferThread transferThread = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/nft.fxml"));
        primaryStage.setTitle("Network File Transfer");
        Scene scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
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
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
