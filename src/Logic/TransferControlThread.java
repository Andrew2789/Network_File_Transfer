package Logic;

import GUI.NftController;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import javafx.collections.ObservableList;

public class TransferControlThread extends NetThread {
	private boolean sendablesAdded;
	private int lastProcessed = 0;

	public TransferControlThread(String ipAddress, int port, NftController nftController) {
		super(ipAddress, port, nftController);
	}

	public TransferControlThread(int port, NftController nftController) {
		super(port, nftController);
	}

	private void sendNewSendables(DataOutputStream outputStream) throws IOException {
		int treeIndex = 0;
		LinkedList<RootTreeItem> toCommunicate = new LinkedList<>();
		synchronized (nftController.getSendables()) {
			ObservableList sendables = nftController.getSendables();
			int sendableLength = sendables.size();
			while (((RootTreeItem)sendables.get(treeIndex)).getId() < lastProcessed && treeIndex < sendableLength) {
				treeIndex++;
			}
			while (treeIndex < sendableLength) {
				toCommunicate.add((RootTreeItem)sendables.get(treeIndex));
				treeIndex++;
			}
		}
		lastProcessed = toCommunicate.getLast().getId();

		for (RootTreeItem rootTreeItem: toCommunicate) {
			outputStream.writeInt(1); //opcode
			outputStream.writeInt(rootTreeItem.getId());
			outputStream.writeUTF(rootTreeItem.getPath());
		}
	}

	private void recvNewSendables(DataInputStream inputStream) throws IOException {
		int id = inputStream.readInt();
		String path = inputStream.readUTF();
		RootTreeItem newReceivable = new RootTreeItem(path, id);
		nftController.addReceivable(newReceivable);
	}

	private void sendRemovedSendables(DataOutputStream outputStream) {

	}

	private void recvRemovedSendables(DataInputStream inputStream) {

	}

	@Override
	void afterConnection(DataInputStream inputStream, DataOutputStream outputStream) throws InterruptedException, IOException {// CHANGE THIS LATER! if the connection dies, this should not be caught and this thread should terminate
		System.out.println("Transfer control thread connected");
		int opCode = 0;
		while (!exit) {
			/*Main loop, check if sendables or download queue changed and notify other.
			1: new sendable
			2: removed sendable
			3:
			*/
			if (sendablesAdded) {
				sendNewSendables(outputStream);
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
}
