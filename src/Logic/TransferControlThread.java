package Logic;

import GUI.NftController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class TransferControlThread extends NetThread {

	private boolean sendablesAdded = true, sendablesRemoved = false;
	private int nextSendableId = 0;
	private LinkedHashMap<Integer, LinkedList<String>> removedSendables = new LinkedHashMap<>();

	public TransferControlThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	public TransferControlThread(int port, NftController nftController) {
		super(port, nftController);
	}

	private void sendNewSendables(DataOutputStream outputStream) throws IOException {
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
				outputStream.writeUTF(rootTreeItem.getName());
				if (rootTreeItem.isFolder()) {
					outputStream.writeInt(rootTreeItem.getChildren().size());
					sendSubfolders(rootTreeItem, outputStream);
				}
				treeIndex++;
			}
			nextSendableId = ((FileTreeItem) sendables.get(treeIndex-1)).getId() + 1;
		}
		sendablesAdded = false;
	}

	private void sendSubfolders(FileTreeItem parent, DataOutputStream outputStream) throws IOException {
		FileTreeItem currentItem;
		for (Object child : parent.getChildren()) {
			currentItem = (FileTreeItem) child;
			outputStream.writeBoolean(currentItem.isFolder());
			outputStream.writeUTF(currentItem.getName());
			if (currentItem.isFolder()) {
				outputStream.writeInt(currentItem.getChildren().size());
				sendSubfolders(currentItem, outputStream);
				System.out.println(currentItem.getName());
			}
		}
	}

	private void recvNewSendables(DataInputStream inputStream) throws IOException {
		//root node info
		int id = inputStream.readInt();
		boolean folder = inputStream.readBoolean();
		String path = inputStream.readUTF();
		FileTreeItem newReceivable = new FileTreeItem(path, folder, id);
		if (folder) {
			int children = inputStream.readInt();
			recvSubfolders(newReceivable, children, inputStream);
		}
		synchronized (nftController.getReceivables()) {
			nftController.getReceivables().add(newReceivable);
		}
	}

	private void recvSubfolders(FileTreeItem parent, int fileCount, DataInputStream inputStream) throws IOException {
		boolean folder;
		String name;
		int children;
		FileTreeItem newItem;
		for (int i = 0; i < fileCount; i++) {
			folder = inputStream.readBoolean();
			name = inputStream.readUTF();
			newItem = new FileTreeItem(name, folder);
			if (folder) {
				children = inputStream.readInt();
				recvSubfolders(newItem, children, inputStream);
			}
			parent.getChildren().add(newItem);
			System.out.println(newItem.getName());
		}
	}

	private void sendRemovedSendables(DataOutputStream outputStream) throws IOException {
		synchronized (removedSendables) {
			for (int id: removedSendables.keySet()) {
				outputStream.writeInt(2); //opcode
				outputStream.writeInt(id);
				if (removedSendables.get(id).size() == 1) {
					outputStream.writeBoolean(true);
				} else {
					outputStream.writeBoolean(false);
					for (String pathItem : removedSendables.get(id)) {
						outputStream.writeUTF(pathItem);
					}
				}
			}
		}
	}

	private void recvRemovedSendables(DataInputStream inputStream) throws IOException {
		boolean root = inputStream.readBoolean();
		if (root) {
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
		} else {

		}
	}

	@Override
	void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) throws InterruptedException, IOException {// CHANGE THIS LATER! if the connection dies, this should not be caught and this thread should terminate
		System.out.println("Transfer control thread connected");
		int opCode = 0;
		while (!exit) {
			/*Main loop, check if sendables or download queue changed and notify other.
			1: new sendable
			2: removed sendable
			3: new download queued
			4: removed download from queue
			5: echo
			*/
			if (sendablesAdded) {
				sendNewSendables(outputStream);
			}
			if (sendablesRemoved) {
				sendRemovedSendables(outputStream);
			}
			try {
				opCode = inputStream.readInt();
				switch (opCode) {
					case 1:
						recvNewSendables(inputStream);
						break;
					case 2:
						recvRemovedSendables(inputStream);
						break;
					case 5:
						outputStream.writeInt(5);
						break;
					default:
						throw new IllegalStateException("Invalid opcode received: " + opCode);
				}
			} catch (SocketTimeoutException e) {
			}
		}
		System.out.println("Exiting transfer control thread");
	}

	public void sendableAdded() {
		sendablesAdded = true;
	}

	public void sendableRemoved(int id, LinkedList<String> removedSendablePath) {
		sendablesRemoved = true;
		synchronized (removedSendables) {
			removedSendables.put(id, removedSendablePath);
		}
	}
}
