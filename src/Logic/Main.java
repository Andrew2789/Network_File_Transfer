package Logic;

import GUI.NftController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.LinkedList;

public class Main extends Application {
    private static TransferThread transferThread = null;
    private static TransferControlThread transferControlThread = null;
    private static int nextSendableRootId = -1;

    public static void clientTransferThreads(String ip, int lowerPort, NftController nftController) {
        transferThread = new TransferThread(ip, lowerPort, nftController);
        transferControlThread = new TransferControlThread(ip, lowerPort+1, nftController);
        transferThread.start();
        transferControlThread.start();
    }

    public static void hostTransferThreads(int lowerPort, NftController nftController) {
        transferThread = new TransferThread(lowerPort, nftController);
        transferControlThread = new TransferControlThread(lowerPort+1, nftController);
        transferThread.start();
        transferControlThread.start();
    }

    public static void killTransferThreads() {
        transferThread.exit();
        transferControlThread.exit();
        transferThread = null;
        transferControlThread = null;
    }

    public static int getNextSendableRootId(boolean increment) {
        if (increment) {
            nextSendableRootId++;
        }
        return nextSendableRootId;
    }

    public static void sendableAdded() {
        if (transferControlThread != null)
            transferControlThread.sendableAdded();
    }

    public static void sendableRemoved(int id, LinkedList<String> removedSendablePath) {
        transferControlThread.sendableRemoved(id, removedSendablePath);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/GUI/nft.fxml"));
        primaryStage.setTitle("Network File Transfer");
        Scene scene = new Scene(root, 1366, 768);
        scene.getStylesheets().add(getClass().getResource("/GUI/steelblue.css").toExternalForm());
        primaryStage.setScene(scene);
        //primaryStage.setMinWidth(800);
        //primaryStage.setMinHeight(430);
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
