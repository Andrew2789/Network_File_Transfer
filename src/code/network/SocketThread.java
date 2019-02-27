package code.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public abstract class SocketThread extends Thread {
	private Thread t;
	private RunnableReporter onFail;
    private Runnable onSuccess, onDisconnect, onServerCreation;
	private ServerSocket serverSocket = null;
    protected List<ClientSocket> clientSockets = new ArrayList<>();
	protected static final int checkTimeout = 100, communicationTimeout = 10000;
	protected boolean exit = false, stopWaiting = false, ownsSocket = true;
	protected String ipAddress = null;
	protected int port, connections = 1;

    /**
     * Construct a new SocketThread as a client. The client will not attempt to connect to the specified server until the thread is started.
     *
     * @param ipAddress The IP address of the server to connect to.
     * @param port The port of the server to connect to.
     * @param onFail A runnable to run if the server cannot be connected to after the thread starts.
     * @param onSuccess A runnable to run if a connection is successfully established with the server.
     * @param onDisconnect A runnable to run when the socket connection is closed for whatever reason after previously being connected.
     */
	public SocketThread(String ipAddress, int connections, int port, RunnableReporter onFail, Runnable onSuccess, Runnable onDisconnect) {
		this.ipAddress = ipAddress;
		this.connections = connections;
		this.port = port;
        this.onFail = onFail;
        this.onSuccess = onSuccess;
        this.onDisconnect = onDisconnect;
	}

    /**
     * Construct a new SocketThread as a server. The server will not listen for connections until the thread is started.
     *
     * @param port
     * @param connections
     * @param onFail A runnable to run if the server cannot be started after the thread starts.
     * @param onSuccess A runnable to run if a connection is successfully established with the server.
     * @param onDisconnect A runnable to run when the socket connection is closed for whatever reason after previously being connected.
     */
	public SocketThread(int port, int connections, RunnableReporter onFail, Runnable onSuccess, Runnable onServerCreation, Runnable onDisconnect) {
		this.port = port;
		this.connections = connections;
        this.onFail = onFail;
        this.onSuccess = onSuccess;
        this.onServerCreation = onServerCreation;
        this.onDisconnect = onDisconnect;
	}

	public SocketThread(List<ClientSocket> clientSockets, Runnable onDisconnect) {
		this.clientSockets = clientSockets;
        this.onDisconnect = onDisconnect;
		ownsSocket = false;
	}

    public void stopWaiting() {
        stopWaiting = true;
    }

    public void exit() {
        exit = true;
    }

	abstract void afterConnection() throws InterruptedException, IOException;

	public boolean streamsReady() {
		return clientSockets.size() == connections;
	}

	protected void acceptConnection() throws IOException {
		stopWaiting = false;
		while (!exit && !stopWaiting) {
			try {
				Socket socket = serverSocket.accept();
				socket.setSoTimeout(checkTimeout);
				clientSockets.add(new ClientSocket(socket));
				return;
			} catch (SocketTimeoutException ignored) {
			}
		}
	}

	private boolean initializeSockets() throws IOException {
		if (ipAddress != null) {
			//If there is an ip address specified, this must be a client
			try {
                while (!exit && !stopWaiting && clientSockets.size() < connections) {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getByName(ipAddress), port), communicationTimeout);
                    socket.setSoTimeout(checkTimeout);
                    clientSockets.add(new ClientSocket(socket));
                }
			} catch (IOException e) {
				e.printStackTrace();
				onFail.run(e);
				return false;
			}
		} else {
			//If there is no ip address specified, this must be a server
			try {
				serverSocket = new ServerSocket(port);
				serverSocket.setSoTimeout(checkTimeout);
                onServerCreation.run();
			} catch (IOException e) {
				serverSocket = null;
				onFail.run(e);
				return false;
			}

			if (serverSocket != null) {
				while (!exit && !stopWaiting && clientSockets.size() < connections) {
					acceptConnection();
				}
			}
		}
        return !exit;
    }

	public void run() {
		try {
			if (ownsSocket) { //Only set up socket connections and io streams if owner, otherwise it should already be done
				if (initializeSockets()) {
					onSuccess.run();
					afterConnection();
				}
			} else {
				afterConnection();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			onDisconnect.run();

			for (ClientSocket clientSocket: clientSockets) {
				clientSocket.tearDown();
			}
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	protected String receiveString(DataInputStream in) throws IOException {
		String out = null;
		while (out == null && !exit) {
			try {
				out = in.readUTF();
			} catch (SocketTimeoutException ignored) {
			}
		}
		return out;
	}

	protected int receiveInt(DataInputStream in) throws IOException {
		Integer out = null;
		while (out == null && !exit) {
			try {
				out = in.readInt();
			} catch (SocketTimeoutException ignored) {
			}
		}
		if (out == null) {
			out = -1;
		}
		return out;
	}

	protected boolean receiveBool(DataInputStream in) throws IOException {
		Boolean out = null;
		while (out == null && !exit) {
			try {
				out = in.readBoolean();
			} catch (SocketTimeoutException ignored) {
			}
		}
		if (out == null) {
			out = false;
		}
		return out;
	}
}
