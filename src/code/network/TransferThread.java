package code.network;

import code.gui.FileTreeItem;
import code.gui.BodyController;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import javafx.collections.ObservableList;

public class TransferThread extends SocketThread {
	private static final int progressBarRefreshTime = 100, transferSpeedRefreshFreq = 5, chunkSize = 65536;
	private boolean active = false;
	private boolean writing;
    private BodyController bodyController;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

	TransferThread(ClientSocket clientSocket, Runnable onDisconnect, boolean writing, BodyController bodyController) {
		super(Collections.singletonList(clientSocket), onDisconnect);
        this.bodyController = bodyController;
		this.writing = writing;
	}

	public boolean isActive() {
		return active;
	}

	public void startTransfer() {
		active = true;
	}

	private LinkedList<FileTreeItem> readNextInQueue() {
		LinkedList<FileTreeItem> toTransfer;
		ObservableList tree;
		if (writing) {
			tree = bodyController.getUploads();
		} else {
			tree = bodyController.getDownloads();
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

	private void updateProgress(double progress, FileTreeItem file, FileTreeItem root) {
		file.setProgress(progress);
		LinkedList<FileTreeItem> pathList = file.getPathFromRoot();
		root.updateProgress(pathList);
	}

	private void uploadNext(byte[] buffer) throws IOException {
		FileInputStream fileInputStream;
		LinkedList<FileTreeItem> toTransfer = readNextInQueue();
		if (toTransfer == null) {
			return;
		}

		int read;
		long totalRead, currentTime, lastRefresh;
		long readSinceLastUpdate = 0, lastSpeedUpdate = System.currentTimeMillis();
		for (FileTreeItem file : toTransfer) {
			System.out.println("Beginning upload of " + file.getName());
			try {
				if (file.isFolder()) {
					if (file.getChildren().size() == 0) {
						updateProgress(1, file, toTransfer.getFirst());
					}
				} else {
					fileInputStream = new FileInputStream(String.join(File.separator, file.getPath().split("/")) + File.separatorChar + file.getName());
					totalRead = 0;
					lastRefresh = System.currentTimeMillis();
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
						readSinceLastUpdate += read;
						outputStream.write(buffer, 0, read);

						currentTime = System.currentTimeMillis();
						if (currentTime - lastSpeedUpdate > progressBarRefreshTime*transferSpeedRefreshFreq) {
							bodyController.updateUploadSpeed((long)(readSinceLastUpdate/(((double)(currentTime - lastSpeedUpdate))/1000)));
							readSinceLastUpdate = 0;
							lastSpeedUpdate = currentTime;
						}

						if (currentTime - lastRefresh > progressBarRefreshTime) {
							updateProgress(((double) totalRead) / file.getSize(), file, toTransfer.getFirst());
							lastRefresh = currentTime;
						}
					}
					updateProgress(1, file, toTransfer.getFirst());
					fileInputStream.close();
				}
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
		System.out.println("Upload complete");
		bodyController.uploadStopped();
		//toTransfer.getFirst().updateProgress();
	}

	private void downloadNext(byte[] buffer) throws IOException {
		FileOutputStream fileOutputStream;
		LinkedList<FileTreeItem> toTransfer = readNextInQueue();
		if (toTransfer == null) {
			return;
		}

		String downloadPath = bodyController.getDownloadPath();
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
		long totalRead, currentTime, lastRefresh;
		long readSinceLastUpdate = 0, lastSpeedUpdate = System.currentTimeMillis();
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
					if (file.getChildren().size() == 0) {
						updateProgress(1, file, root);
					}
				} else {
					fileOutputStream = new FileOutputStream(fullPath);
					totalRead = 0;
					lastRefresh = System.currentTimeMillis();
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
						readSinceLastUpdate += read;
						fileOutputStream.write(buffer, 0, read);

						currentTime = System.currentTimeMillis();
						if (currentTime - lastSpeedUpdate > progressBarRefreshTime*transferSpeedRefreshFreq) {
							bodyController.updateDownloadSpeed((long)(readSinceLastUpdate/((double)(currentTime - lastSpeedUpdate)/1000)));
							readSinceLastUpdate = 0;
							lastSpeedUpdate = currentTime;
						}

						if (currentTime - lastRefresh > progressBarRefreshTime) {
							updateProgress(((double) totalRead) / file.getSize(), file, root);
							lastRefresh = currentTime;
						}
					}
					updateProgress(1, file, root);
					fileOutputStream.close();
				}
			} catch (FileNotFoundException e) {
				System.err.println("File not found!");
				e.printStackTrace();
			}
		}
		System.out.println("Download complete");
		bodyController.downloadStopped();
		//root.updateProgress();
	}

	@Override
	void afterConnection() throws IOException, InterruptedException {
		System.out.print("Transfer thread connected: ");
		if (writing) {
			System.out.println("Writing");
		} else {
			System.out.println("Reading");
		}
		inputStream = clientSockets.get(0).in;
		outputStream = clientSockets.get(0).out;

		clientSockets.get(0).socket.setSoTimeout(communicationTimeout);
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
