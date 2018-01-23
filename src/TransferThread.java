import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TransferThread extends Thread {
	private Thread t;
	private Socket socket = null;
	private ServerSocket serverSocket = null;
	private static final int operationTimeout = 100;
	private static final int chunkSize = 65536;
	private boolean exit = false;
	private String ipAddress = null;
	private int port, connectTimeout;
	private NftController nftController;

	public TransferThread(String ipAddress, int port, NftController nftController) {
		this.ipAddress = ipAddress;
		this.port = port;
		this.connectTimeout = 3;
		this.nftController = nftController;
	}

	public TransferThread(int port, NftController nftController) {
		this.port = port;
		this.nftController = nftController;
	}

	public boolean setupDone() {
		return socket != null && socket.isConnected();
	}

	public void exit() {
		exit = true;
	}

	public void run() {
		if (ipAddress != null) {
			try {
				socket = new Socket();
				socket.setSoTimeout(operationTimeout);
				socket.connect(new InetSocketAddress(ipAddress, port), connectTimeout);
			} catch (IOException e) {
				socket = null;
				nftController.connectFailed();
				System.out.println("confail");
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
						System.out.println("timeout");
					}
				}
				if (exit) {
					return;
				}
			}

			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
			byte inputBuffer[] = new byte[chunkSize];

			while (!exit) {
				try {
					outputStream.writeByte(5);
					int a = inputStream.read(inputBuffer);
					System.out.println(a);
					System.out.println(inputBuffer[0]);
				} catch (SocketTimeoutException e) {
					System.out.println("timeout1");
				}
			}

		} catch (IOException e) {
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
