package Logic;

import GUI.NftController;
import java.awt.SystemTray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

public abstract class NetThread extends Thread {
	private Thread t;
	private ServerSocket serverSocket = null;
	protected Socket socket = null;
	protected static final int checkTimeout = 100;
	protected static final int commTimeout = 10000;
	protected static final int chunkSize = 65536;
	protected boolean exit = false;
	protected String ipAddress = null;
	protected int port, connectTimeout;
	protected NftController nftController;
	protected boolean ownsSocket = true;
	protected DataInputStream inputStream = null;
	protected DataOutputStream outputStream = null;

	public NetThread(String ipAddress, int port, NftController nftController) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.connectTimeout = 10000; //10 seconds
		this.nftController = nftController;
	}

	public NetThread(int port, NftController nftController) {
		this.port = port;
		this.nftController = nftController;
	}

	public NetThread(Socket socket, DataInputStream inputStream, DataOutputStream outputStream, NftController nftController) {
		this.socket = socket;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.nftController = nftController;
		ownsSocket = false;
	}

	public void exit() {
		exit = true;
	}

	abstract void afterConnection() throws InterruptedException, IOException;

	public boolean streamsReady() {
		return (inputStream != null && outputStream != null);
	}

	public void run() {
		if (ownsSocket) { //Only set up socket connections and io streams if owner, otherwise it should already be done
			if (ipAddress != null) {
				try {
					socket = new Socket();
					socket.connect(new InetSocketAddress(InetAddress.getByName(ipAddress), port), connectTimeout);
					socket.setSoTimeout(checkTimeout);
				} catch (IOException e) {
					socket = null;
					nftController.connectFailed();
					return;
				}
			} else {
				try {
					serverSocket = new ServerSocket(port);
					serverSocket.setSoTimeout(checkTimeout);
				} catch (IOException e) {
					serverSocket = null;
					nftController.listenFailed();
					return;
				}
			}

			try {
				//Accept connection if server
				if (serverSocket != null) {
					while (!exit && socket == null) {
						try {
							socket = serverSocket.accept();
							socket.setSoTimeout(checkTimeout);
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

				afterConnection();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			try {
				afterConnection();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
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
