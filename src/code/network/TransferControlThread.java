package code.network;

import code.gui.FileTreeItem;
import code.gui.BodyController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import javafx.collections.ObservableList;

public class TransferControlThread extends SocketThread {
	private boolean sendablesAdded = true, sendablesRemoved = false;
	private int nextSendableId = 0;
    private final LinkedList<Integer> removedSendables = new LinkedList<>();
	private BodyController bodyController;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private TransferThread writeThread = null, readThread = null;

	TransferControlThread(String ipAddress, int port, Runnable onFail, Runnable onSuccess, Runnable onDisconnect, BodyController bodyController) {
		super(ipAddress, 2, port, onFail, onSuccess, onDisconnect);
		this.bodyController = bodyController;
	}

	TransferControlThread(int port, Runnable onFail, Runnable onSuccess, Runnable onServerCreation, Runnable onDisconnect, BodyController bodyController) {
		super(port, 2, onFail, onSuccess, onServerCreation, onDisconnect);
        this.bodyController = bodyController;
	}

	public boolean writeThreadActive() {
		return writeThread != null && writeThread.isActive();
	}

	public boolean readThreadActive() {
		return readThread != null && readThread.isActive();
	}

	private void sendUploadNames() throws IOException {
		if (bodyController.getUploads().size() == 0)
			return;

		int treeIndex = 0;
		synchronized (bodyController.getUploads()) {
			ObservableList sendables = bodyController.getUploads();
			int sendableLength = sendables.size();
			while (((FileTreeItem) sendables.get(treeIndex)).getId() < nextSendableId && treeIndex < sendableLength) {
				treeIndex++;
			}
			FileTreeItem rootTreeItem;
			while (treeIndex < sendableLength) {
				rootTreeItem = (FileTreeItem) sendables.get(treeIndex);
				outputStream.writeInt(1); //opcode
				//root node info
				outputStream.writeInt(rootTreeItem.getId());
				outputStream.writeBoolean(rootTreeItem.isFolder());
				outputStream.writeUTF(rootTreeItem.getDisplayName());
				outputStream.writeUTF(rootTreeItem.getName());
				outputStream.writeLong(rootTreeItem.getSize());
				if (rootTreeItem.isFolder()) {
					outputStream.writeInt(rootTreeItem.getChildren().size());
					sendSubfolders(rootTreeItem);
				}
				treeIndex++;
				System.out.println("Finished sending " + rootTreeItem.getName());
			}
			nextSendableId = ((FileTreeItem) sendables.get(treeIndex-1)).getId() + 1;
		}
		sendablesAdded = false;
        writeThread.startTransfer();
	}

	private void sendSubfolders(FileTreeItem parent) throws IOException {
		FileTreeItem currentItem;
		System.out.println(String.format("Sending folder %s subitems %d", parent.getName(), parent.getChildren().size()));
		for (Object child : parent.getChildren()) {
			currentItem = (FileTreeItem) child;
			System.out.println(String.format("Sending subitem %s from %s", currentItem.getName(), parent.getName()));
			outputStream.writeBoolean(currentItem.isFolder());
			outputStream.writeUTF(currentItem.getDisplayName());
			outputStream.writeUTF(currentItem.getName());
			outputStream.writeLong(currentItem.getSize());
			if (currentItem.isFolder()) {
				outputStream.writeInt(currentItem.getChildren().size());
				sendSubfolders(currentItem);
			}
		}
	}

	private void recvDownloadNames() throws IOException {
		//root node info
		int id = inputStream.readInt();
		boolean folder = inputStream.readBoolean();
		String displayName = inputStream.readUTF();
		String name = inputStream.readUTF();
		long size = inputStream.readLong();
		FileTreeItem newReceivable = new FileTreeItem(displayName, name, size, folder, id);
		if (folder) {
			int children = inputStream.readInt();
			recvSubfolders(newReceivable, children);
		}
		synchronized (bodyController.getDownloads()) {
			bodyController.getDownloads().add(newReceivable.receivableToDownload(id));
			System.out.println("Finished receiving " + newReceivable.getName());
		}
        readThread.startTransfer();
	}

	private void recvSubfolders(FileTreeItem parent, int fileCount) throws IOException {
		boolean folder;
		String displayName, name;
		long size;
		int children;
		FileTreeItem newItem;
		System.out.println(String.format("Receiving folder %s subitems %d", parent.getName(), fileCount));
		for (int i = 0; i < fileCount; i++) {
			folder = inputStream.readBoolean();
			displayName = inputStream.readUTF();
			name = inputStream.readUTF();
			size = inputStream.readLong();
			newItem = new FileTreeItem(displayName, name, size, folder);
			System.out.println(String.format("Receiving subitem %s from %s", newItem.getName(), parent.getName()));
			if (folder) {
				children = inputStream.readInt();
				recvSubfolders(newItem, children);
			}
			parent.getChildren().add(newItem);
		}
	}

	private void sendRemovedUploads() throws IOException {
		synchronized (removedSendables) {
			for (int id: removedSendables) {
				outputStream.writeInt(2); //opcode
				outputStream.writeInt(id);
			}
		}
	}

	private void recvRemovedDownloads() throws IOException {
		FileTreeItem childItem;
		int id = inputStream.readInt();
		synchronized (bodyController.getDownloads()) {
			for (Object child: bodyController.getDownloads()) {
				childItem = (FileTreeItem)child;
				if (childItem.getId() == id) {
					bodyController.getDownloads().remove(childItem);
					break;
				}
			}
		}
	}

	@Override
	void afterConnection() throws IOException {// CHANGE THIS LATER! if the connection dies, this should not be caught and this thread should terminate
		System.out.println("Transfer control thread connected");
		inputStream = clientSockets.get(0).in;
		outputStream = clientSockets.get(0).out;
		/*outputStream.writeUTF(bodyController.getNickname());
		bodyController.setPeerNickname(inputStream.readUTF());*/
        writeThread = new TransferThread(clientSockets.get(1), () -> {}, true, bodyController);
        writeThread.start();
        readThread = new TransferThread(clientSockets.get(1), () -> {}, false, bodyController);
        readThread.start();

		int opCode;
		while (!exit) {
			if (sendablesAdded) {
				sendUploadNames();
			}
			if (sendablesRemoved) {
				sendRemovedUploads();
			}
			try {
				opCode = inputStream.readInt();
				clientSockets.get(0).socket.setSoTimeout(communicationTimeout);
				try {
					switch (opCode) {
				 		case 1:
							recvDownloadNames();
							break;
						case 2:
							recvRemovedDownloads();
							break;
						case 3:
							outputStream.writeInt(3);
							break;
						default:
							throw new IllegalStateException("Invalid opcode received: " + opCode);
					}
                    clientSockets.get(0).socket.setSoTimeout(checkTimeout);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (SocketTimeoutException e) {
			}
		}
		System.out.println("Exiting transfer control thread");
	}

	void sendableAdded() {
		sendablesAdded = true;
	}

	void sendableRemoved(int id) {
		sendablesRemoved = true;
		synchronized (removedSendables) {
			removedSendables.add(id);
		}
	}

	@Override
	public void exit() {
        if (writeThread != null) {
            writeThread.exit();
        }
        if (readThread != null) {
            readThread.exit();
        }
        writeThread = null;
        readThread = null;

        exit = true;
    }
}
