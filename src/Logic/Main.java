package Logic;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/FXML/nft.fxml"));
        primaryStage.setTitle("Direct File Transfer");
        Scene scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(getClass().getResource("/CSS/dark.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(430);
        primaryStage.show();
    }

    @Override
    public void stop() {
        TransferThread.exit();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
