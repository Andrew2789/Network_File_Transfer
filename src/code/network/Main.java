package code.network;

import code.gui.BodyController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class Main extends Application {
    private static TransferControlThread transferControlThread = null;
    private static String location;
    private static int nextSendableRootId = -1;

    private static Stage stage;
    private static Scene scene;
    private static Parent bodyScene, connectionScene;
    private static BodyController bodyController;

    public static Stage getStage() {
        return stage;
    }

    public static String getLocation() {
    	return location;
	}

    public static void setBodyController(BodyController bodyController) {
        Main.bodyController = bodyController;
    }

    public static void connected() {
        Platform.runLater(() -> scene.setRoot(bodyScene));
    }

    public static void disconnected() {
        Platform.runLater(() -> scene.setRoot(connectionScene));
    }

    public static void clientTransferThreads(String ip, int port, RunnableReporter onFail, Runnable onSuccess, Runnable onDisconnect) {
        if (transferControlThread != null) {
            killTransferThreads();
        }
        transferControlThread = new TransferControlThread(ip, port, onFail, onSuccess, onDisconnect, bodyController);
        transferControlThread.start();
    }

    public static void hostTransferThreads(int port, RunnableReporter onFail, Runnable onSuccess, Runnable onServerCreation, Runnable onDisconnect) {
        if (transferControlThread != null) {
            killTransferThreads();
        }
        transferControlThread = new TransferControlThread(port, onFail, onSuccess, onServerCreation, onDisconnect, bodyController);
        transferControlThread.start();
    }

    public static void killTransferThreads() {
		if (transferControlThread != null) {
			transferControlThread.exit();
		}
        transferControlThread = null;
    }

    public static int getNextSendableRootId() {
		nextSendableRootId++;
        return nextSendableRootId;
    }

    public static void sendableAdded() {
        if (transferControlThread != null)
            transferControlThread.sendableAdded();
    }

    public static void sendableRemoved(int id) {
        if (transferControlThread != null)
            transferControlThread.sendableRemoved(id);
    }

    public static boolean writeThreadActive() {
        return transferControlThread.writeThreadActive();
    }

    public static boolean readThreadActive() {
        return transferControlThread.readThreadActive();
    }


    @Override
    public void start(Stage stage) {
        /*System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream("stderr.log")), true));
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("stdout.log")), true));*/
        Main.stage = stage;
        stage.setTitle("Network File Transfer");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/image/wateryarrow.png")));
        try {
            connectionScene =FXMLLoader.load(getClass().getResource("/resources/fxml/connection.fxml"));
            bodyScene = FXMLLoader.load(getClass().getResource("/resources/fxml/body.fxml"));
			location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
			scene = new Scene(connectionScene, 1280, 720);
			stage.setScene(scene);
			stage.setResizable(true);
			stage.show();
		} catch (IOException | URISyntaxException e) {
        	e.printStackTrace();
        	stop();
		}
    }

    @Override
    public void stop() {
    	System.out.println("Exiting GUI, killing transfer threads");
        killTransferThreads();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
