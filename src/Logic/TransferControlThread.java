package Logic;

import GUI.NftController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TransferControlThread extends NetThread {
	public TransferControlThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	public TransferControlThread(int port, NftController nftController) {
		super(port, nftController);
	}

	@Override
	void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) {
		System.out.println("Transfer control thread connected");
		for (File file: nftController.getSendablesToSync()) {
			System.out.print(file.getName());
			System.out.print(", ");
		}
		System.out.println("Exiting transfer control thread");
	}
}
