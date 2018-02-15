package Logic;

import GUI.NftController;
import java.awt.SystemTray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

public class TransferThread extends NetThread {
	private boolean active = false;
	private boolean writing;

	public TransferThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
		writing = true;
	}

	public TransferThread(int port, NftController nftController) {
		super(port, nftController);
		writing = true;
	}

	public TransferThread(TransferThread master, NftController nftController) {
		super(master.getSocket(), master.getReadStream(), null, nftController);
		writing = false;
	}

	private Socket getSocket() {
		return socket;
	}

	private DataInputStream getReadStream() {
		return inputStream;
	}

	public void startTransfer() {
		active = true;
	}

	private LinkedList<String> readNextInQueue() {
		return null;
	}

	private void uploadNext(byte[] buffer) {
		FileInputStream fileInputStream;
		LinkedList<String> toTransfer = readNextInQueue();
		for (String fileName: toTransfer) {
			try {
				fileInputStream = new FileInputStream(fileName);
				//outputStream.writeByte(5);
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
	}

	private void downloadNext(byte[] buffer) {
		FileOutputStream fileOutputStream;
		LinkedList<String> toTransfer = readNextInQueue();
		for (String fileName: toTransfer) {
			try {
				fileOutputStream = new FileOutputStream(fileName);
				//int a = inputStream.read(inputBuffer);
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
	}

	@Override
	void afterConnection() throws IOException {
		System.out.print("Transfer thread connected: ");
		if (writing) {
			System.out.println("Writing");
		} else {
			System.out.println("Reading");
		}

		byte[] buffer = new byte[chunkSize];
		while (!exit) {
			if (active) {
				if (writing) {
					while (active) {
						uploadNext(buffer);
					}
				} else {
					while (active) {
						downloadNext(buffer);
					}
				}
			} else {
				try {
					sleep(250);
				} catch (InterruptedException e) {
				}
			}
		}

		System.out.println("Exiting transfer thread: ");
		if (writing) {
			System.out.println("Writing");
		} else {
			System.out.println("Reading");
		}
	}
}
