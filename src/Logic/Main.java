package Logic;

import GUI.NftController;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class Main extends Application {
    private static TransferThread writeThread = null;
    private static TransferThread readThread = null;
    private static TransferControlThread transferControlThread = null;
    private static String location;
    private static int nextSendableRootId = -1;
    private static int nextDownloadRootId = -1;
    private static Scene scene;

    public static String getLocation() {
    	return location;
	}

    public static void setStyleSheet(String newSheet) {
    	scene.getStylesheets().remove(0);
    	scene.getStylesheets().add(newSheet);
	}

    public static void clientTransferThreads(String ip, int port1, int port2, NftController nftController) {
        writeThread = new TransferThread(ip, port1, nftController);
        transferControlThread = new TransferControlThread(ip, port2, nftController);
        writeThread.start();
        transferControlThread.start();
    }

    public static void hostTransferThreads(int port1, int port2, NftController nftController) {
        writeThread = new TransferThread(port1, nftController);
        transferControlThread = new TransferControlThread(port2, nftController);
        writeThread.start();
        transferControlThread.start();
    }

    public static void killTransferThreads() {
    	if (writeThread != null) {
			writeThread.exit();
		}
		if (readThread != null) {
			readThread.exit();
		}
		if (transferControlThread != null) {
			transferControlThread.exit();
		}
        writeThread = null;
        readThread = null;
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

    public static void startUpload() {
        writeThread.startTransfer();
    }

    public static void startDownload(NftController nftController) {
        if (readThread == null) {
            while (!writeThread.streamsReady()) {
                try {
                	System.out.println("waiting for streams to be ready");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
            readThread = new TransferThread(writeThread, nftController);
			readThread.start();
        }
        readThread.startTransfer();
    }

	public static boolean writeThreadActive() {
    	if (writeThread == null) {
    		return false;
		}
 		return writeThread.isActive();
	}

	public static boolean readThreadActive() {
		if (readThread == null) {
			return false;
		}
		return readThread.isActive();
	}

    @Override
    public void start(Stage stage) {
        /*System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream("stderr.log")), true));
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("stdout.log")), true));*/
        stage.setTitle("Network File Transfer");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/GUI/img/wateryarrow.png")));
        try {
			location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
			Parent root = FXMLLoader.load(getClass().getResource("/GUI/nft.fxml"));
			Scene scene = new Scene(root, 1366, 768);
			stage.setScene(scene);
			stage.show();
			scene.getStylesheets().add("/GUI/darkblue.css");
			this.scene = scene;
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
