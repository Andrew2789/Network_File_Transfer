package Java.Logic;

import Java.GUI.FileTreeItem;
import Java.GUI.NftController;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import javafx.collections.ObservableList;

public class TransferControlThread extends NetThread {
	private boolean sendablesAdded = true, sendablesRemoved = false, downloadsQueued = false, downloadsDequeued = false;
	private int nextSendableId = 0, nextDownloadId = 0;
	private final LinkedList<Integer> removedSendables = new LinkedList<>();

	TransferControlThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	TransferControlThread(int port, NftController nftController) {
		super(port, nftController);
	}

	private void sendNewSendables() throws IOException {
		if (nftController.getSendables().size() == 0)
			return;

		int treeIndex = 0;
		synchronized (nftController.getSendables()) {
			ObservableList sendables = nftController.getSendables();
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

	private void recvNewSendables() throws IOException {
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
		synchronized (nftController.getReceivables()) {
			nftController.getReceivables().add(newReceivable);
			System.out.println("Finished receiving " + newReceivable.getName());
		}
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

	private void sendRemovedSendables() throws IOException {
		synchronized (removedSendables) {
			for (int id: removedSendables) {
				outputStream.writeInt(2); //opcode
				outputStream.writeInt(id);
			}
		}
	}

	private void recvRemovedSendables() throws IOException {
		FileTreeItem childItem;
		int id = inputStream.readInt();
		synchronized (nftController.getReceivables()) {
			for (Object child: nftController.getReceivables()) {
				childItem = (FileTreeItem)child;
				if (childItem.getId() == id) {
					nftController.getReceivables().remove(childItem);
					break;
				}
			}
		}
	}

	private void sendQueuedDownloads() throws IOException {
		int treeIndex = 0;
		synchronized (nftController.getDownloads()) {
			ObservableList downloads = nftController.getDownloads();
			int downloadsLength = downloads.size();
			while (((FileTreeItem) downloads.get(treeIndex)).getId() < nextDownloadId && treeIndex < downloadsLength) {
				treeIndex++;
			}
			FileTreeItem rootTreeItem;
			while (treeIndex < downloadsLength) {
				rootTreeItem = (FileTreeItem) downloads.get(treeIndex);
				outputStream.writeInt(3); //opcode
				outputStream.writeInt(rootTreeItem.getId());
				outputStream.writeUTF(rootTreeItem.getPath());
				treeIndex++;
				System.out.println("Finished sending " + rootTreeItem.getName());
			}
			nextDownloadId = ((FileTreeItem) downloads.get(treeIndex-1)).getId() + 1;
		}
		downloadsQueued = false;
		Main.startDownload(nftController);
	}

	private void recvQueuedDownloads() throws IOException {
		int id = inputStream.readInt();
		String path = inputStream.readUTF();
		FileTreeItem newUpload = FileTreeItem.idPathToUpload(path, id, nftController.getSendables());
		synchronized (nftController.getUploads()) {
			nftController.getUploads().add(newUpload);
			System.out.println("Finished receiving " + newUpload.getName());
		}
		Main.startUpload();
	}

	private void sendDequeuedDownloads() throws IOException {

	}

	private void recvDequeuedDownloads() throws IOException {

	}

	@Override
	void afterConnection() throws IOException {// CHANGE THIS LATER! if the connection dies, this should not be caught and this thread should terminate
		System.out.println("Transfer control thread connected");
		outputStream.writeUTF(nftController.getNickname());
		nftController.setPeerNickname(inputStream.readUTF());

		int opCode;
		while (!exit) {
			/*Main loop, check if sendables or download queue changed and notify other.
			1: new sendable
			2: removed sendable
			3: new download queued
			4: removed download from queue
			5: echo
			*/
			if (sendablesAdded) {
				sendNewSendables();
			}
			if (sendablesRemoved) {
				sendRemovedSendables();
			}
			if (downloadsQueued) {
				sendQueuedDownloads();
			}
			if (downloadsDequeued) {
				sendDequeuedDownloads();
			}
			try {
				opCode = inputStream.readInt();
				socket.setSoTimeout(commTimeout);
				try {
					switch (opCode) {
						case 1:
							recvNewSendables();
							break;
						case 2:
							recvRemovedSendables();
							break;
						case 3:
							recvQueuedDownloads();
							break;
						case 4:
							recvDequeuedDownloads();
							break;
						case 5:
							outputStream.writeInt(5);
							break;
						default:
							throw new IllegalStateException("Invalid opcode received: " + opCode);
					}
					socket.setSoTimeout(checkTimeout);
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

	void downloadQueued() {
		downloadsQueued = true;
	}

	void downloadDequeued() {
		downloadsDequeued = true;
	}
}
