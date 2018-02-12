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
	private boolean active = false;

	public TransferThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	public TransferThread(int port, NftController nftController) {
		super(port, nftController);
	}

	public void startUpload() {
		active = true;
	}

	@Override
	void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
		System.out.println("Transfer thread connected");

		/*
		byte inputBuffer[] = new byte[chunkSize];
					outputStream.writeByte(5);
					int a = inputStream.read(inputBuffer);
		}*/

		while (!exit) {
			if (active) {
				try {
					int opCode = inputStream.readInt();
					socket.setSoTimeout(10000);
					try {
						socket.setSoTimeout(operationTimeout);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (SocketTimeoutException e) {
				}
			} else {
				try {
					sleep(250);
				} catch (InterruptedException e) {

				}
			}
		}
		System.out.println("Exiting transfer thread");
	}
}
