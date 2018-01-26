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

public abstract class NetThread extends Thread {
	protected Thread t;
	protected Socket socket = null;
	protected ServerSocket serverSocket = null;
	protected static final int operationTimeout = 50;
	protected static final int chunkSize = 65536;
	protected boolean exit = false;
	protected String ipAddress = null;
	protected int port, connectTimeout;
	protected NftController nftController;

	public NetThread(String ipAddress, int port, NftController nftController) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.connectTimeout = 3;
		this.nftController = nftController;
	}

	public NetThread(int port, NftController nftController) {
		this.port = port;
		this.nftController = nftController;
	}

	public boolean setupDone() {
		return socket != null && socket.isConnected();
	}

	public void exit() {
		exit = true;
	}

	abstract void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) throws InterruptedException, IOException;

	public void run() {
		if (ipAddress != null) {
			try {
				socket = new Socket();
				socket.setSoTimeout(operationTimeout);
				socket.connect(new InetSocketAddress(ipAddress, port), connectTimeout);
			} catch (IOException e) {
				socket = null;
				nftController.connectFailed();
				return;
			}
		} else {
			try {
				serverSocket = new ServerSocket();
				serverSocket.setSoTimeout(operationTimeout);
				serverSocket.bind(new InetSocketAddress("", port));
			} catch (IOException e) {
				serverSocket = null;
				nftController.listenFailed();
				return;
			}
		}

		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		try {
			//Accept connection if server
			if (serverSocket != null) {
				while (!exit && socket == null) {
					try {
						socket = serverSocket.accept();
						socket.setSoTimeout(operationTimeout);
					} catch (SocketTimeoutException e) {
					}
				}
				if (exit) {
					return;
				}
			}

			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());

			nftController.connectOrListenSucceeded();
			afterConnection(inputStream, outputStream);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}
}
