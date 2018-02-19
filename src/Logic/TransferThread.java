package Logic;

import GUI.NftController;
import java.awt.SystemTray;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import javafx.beans.Observable;
import javafx.collections.ObservableList;

public class TransferThread extends NetThread {
	private static final int progressBarRefreshTime = 0;
	private boolean active = false;
	private boolean writing;
	private long totalTransferred = 0;

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

	private LinkedList<FileTreeItem> readNextInQueue() {
		LinkedList<FileTreeItem> toTransfer;
		ObservableList tree;
		if (writing) {
			tree = nftController.getUploads();
		} else {
			tree = nftController.getDownloads();
		}
		synchronized (tree) {
			int rootIndex = 0;
			FileTreeItem nextItem;
			do {
				nextItem = (FileTreeItem) tree.get(rootIndex);
				rootIndex++;
			} while (nextItem.getProgress() != 0 && rootIndex < tree.size());
			if (rootIndex == tree.size() && nextItem.getProgress() != 0) {
				active = false;
				return null;
			}
			toTransfer = new LinkedList<>();
			System.out.println("Read this root from queue: " + nextItem.getPath() + File.separatorChar + nextItem.getName());
			addSubfolders(nextItem, toTransfer);
		}
		return toTransfer;
	}

	private void addSubfolders(FileTreeItem parent, LinkedList<FileTreeItem> output) {
		output.add(parent);
		for (Object child : parent.getChildren()) {
			addSubfolders((FileTreeItem) child, output);
		}
	}

	private void uploadNext(byte[] buffer) throws IOException {
		FileInputStream fileInputStream;
		LinkedList<FileTreeItem> toTransfer = readNextInQueue();
		if (toTransfer == null) {
			return;
		}

		int read;
		long totalRead, lastRefresh = System.currentTimeMillis();
		LinkedList<String> pathList;
		for (FileTreeItem file : toTransfer) {
			System.out.println("Beginning upload of " + file.getName());
			try {
				if (!file.isFolder()) {
					fileInputStream = new FileInputStream(String.join(File.separator, file.getPath().split("/")) + File.separatorChar + file.getName());
					totalRead = 0;
					while (totalRead < file.getSize()) {
						if (exit) {
							return;
						}
						if (file.getSize() - totalRead < chunkSize) {
							read = fileInputStream.read(buffer, 0, (int) (file.getSize() - totalRead));
						} else {
							read = fileInputStream.read(buffer);
						}
						totalRead += read;
						totalTransferred += read;
						outputStream.write(buffer, 0, read);

						if (System.currentTimeMillis() - lastRefresh > progressBarRefreshTime) {
							file.setProgress(((double) totalRead) / file.getSize());
							pathList = new LinkedList<>(Arrays.asList(file.getPath().split("/")));
							pathList.removeFirst();
							toTransfer.getFirst().updateProgress(pathList);
							lastRefresh = System.currentTimeMillis();
						}
					}
					file.setProgress(1);
					fileInputStream.close();
				}
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
		System.out.println("Upload complete");
		toTransfer.getFirst().updateProgress();
	}

	private void downloadNext(byte[] buffer) throws IOException {
		FileOutputStream fileOutputStream;
		LinkedList<FileTreeItem> toTransfer = readNextInQueue();
		if (toTransfer == null) {
			return;
		}

		String downloadPath = nftController.getDownloadPath();
		File newFolder;
		boolean rootIsFile = false;
		if (toTransfer.getFirst().isFolder()) {
			newFolder = new File(downloadPath + File.separatorChar + toTransfer.getFirst().getName());
		} else {
			newFolder = new File(downloadPath);
			rootIsFile = true;
		}
		newFolder.mkdirs();
		FileTreeItem root = toTransfer.getFirst();
		if (root.isFolder()) {
			toTransfer.removeFirst();
		}

		String fullPath;
		int read;
		long totalRead, lastRefresh = System.currentTimeMillis();
		LinkedList<String> pathList;
		for (FileTreeItem file : toTransfer) {
			System.out.println("Beginning download of " + file.getName());
			try {
				if (rootIsFile) {
					fullPath = downloadPath + File.separatorChar + file.getName();
				} else {
					fullPath = downloadPath + File.separatorChar + String.join(File.separator, file.getPath().split("/")) + File.separatorChar + file.getName();
				}
				if (file.isFolder()) {
					newFolder = new File(fullPath);
					System.out.println(newFolder.getAbsolutePath() + newFolder.exists());
					if (!newFolder.mkdirs() && !newFolder.exists()) {
						throw new SecurityException("Unable to make dir " + newFolder.getAbsolutePath());
					}
				} else {
					fileOutputStream = new FileOutputStream(fullPath);
					totalRead = 0;
					while (totalRead < file.getSize()) {
						if (exit) {
							return;
						}
						if (file.getSize() - totalRead < chunkSize) {
							read = inputStream.read(buffer, 0, (int) (file.getSize() - totalRead));
						} else {
							read = inputStream.read(buffer);
						}
						totalRead += read;
						totalTransferred += read;
						fileOutputStream.write(buffer, 0, read);

						if (System.currentTimeMillis() - lastRefresh > progressBarRefreshTime) {
							file.setProgress(((double) totalRead) / file.getSize());
							pathList = new LinkedList<>(Arrays.asList(file.getPath().split("/")));
							pathList.removeFirst();
							toTransfer.getFirst().updateProgress(pathList);
							lastRefresh = System.currentTimeMillis();
						}
					}
					file.setProgress(1);
					fileOutputStream.close();
				}
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
		System.out.println("Download complete");
		root.updateProgress();
	}

	@Override
	void afterConnection() throws IOException, InterruptedException {
		System.out.print("Transfer thread connected: ");
		if (writing) {
			System.out.println("Writing");
		} else {
			System.out.println("Reading");
		}

		socket.setSoTimeout(commTimeout);
		byte[] buffer = new byte[chunkSize];
		while (!exit) {
			if (active) {
				if (writing) {
					while (active && !exit) {
						uploadNext(buffer);
					}
				} else {
					while (active && !exit) {
						downloadNext(buffer);
					}
				}
			} else {
				sleep(250);
			}
		}

		System.out.print("Exiting transfer thread: ");
		if (writing) {
			System.out.println("Writing");
		} else {
			System.out.println("Reading");
		}
	}
}
