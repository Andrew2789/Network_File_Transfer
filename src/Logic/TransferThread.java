package Logic;

import GUI.NftController;
import java.awt.SystemTray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TransferThread extends NetThread {
	public TransferThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	public TransferThread(int port, NftController nftController) {
		super(port, nftController);
	}

	@Override
	void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) {
		System.out.println("Transfer thread connected");
		byte inputBuffer[] = new byte[chunkSize];

		while (!exit) {
			try {
				try {
					outputStream.writeByte(5);
					int a = inputStream.read(inputBuffer);
					System.out.println(a);
					System.out.println(inputBuffer[0]);
				} catch (SocketTimeoutException e) {
					System.out.println("timeout1");
				}
			} catch (IOException e) {
				System.out.println("IO Exception!");
				e.printStackTrace();
			}
		}
		System.out.println("Exiting transfer thread");
	}
}
