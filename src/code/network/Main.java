package code.network;

import code.gui.BodyController;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import code.gui.ConnectionController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    private static TransferControlThread transferControlThread = null;
    private static String location;
    private static int nextSendableRootId = -1, nextDownloadRootId = -1;

    private static ConnectionController connectionController;
    private static Scene connectionScene;

    private static BodyController bodyController;
    private static Scene bodyScene;


    public static String getLocation() {
    	return location;
	}

    /**
     * :(
     */
    public static void setConnectionController(ConnectionController connectionController) {
        Main.connectionController = connectionController;
    }

    /**
     * :(
     */
    public static void setBodyController(BodyController bodyController) {
        Main.bodyController = bodyController;
    }

    public static void clientTransferThreads(String ip, int port, Runnable onFail, Runnable onSuccess, Runnable onDisconnect) {
        transferControlThread = new TransferControlThread(ip, port, onFail, onSuccess, onDisconnect, bodyController);
        transferControlThread.start();
    }

    public static void hostTransferThreads(int port, Runnable onFail, Runnable onSuccess, Runnable onServerCreation, Runnable onDisconnect) {
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

	public static int getNextDownloadRootId() {
		nextDownloadRootId++;
		return nextDownloadRootId;
	}

    public static void downloadQueued() {
		transferControlThread.downloadQueued();
    }

    public static void downloadDequeued() {
    	transferControlThread.downloadDequeued();
	}

    /*
	public static boolean writeThreadActive() {
 		return writeThread != null && writeThread.isActive();
	}

	public static boolean readThreadActive() {
		return readThread != null && readThread.isActive();
	}*/

    public static boolean writeThreadActive() {
        return false;
    }

    public static boolean readThreadActive() {
        return false;
    }


        @Override
    public void start(Stage stage) {
        /*System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream("stderr.log")), true));
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("stdout.log")), true));*/
        stage.setTitle("Network File Transfer");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/image/wateryarrow.png")));
        try {
            connectionScene = new Scene(FXMLLoader.load(getClass().getResource("/resources/fxml/connection.fxml")), 1280, 720);
            //bodyScene = new Scene(FXMLLoader.load(getClass().getResource("/resources/fxml/body.fxml")), 1280, 720);
			location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
			stage.setScene(connectionScene);
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
