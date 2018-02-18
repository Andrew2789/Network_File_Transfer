package Logic;

import GUI.ProgressTreeCell;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Andrew Davidson (a.n.d.9489@gmail.com)
 * Custom TreeItem derivative class designed for use in a tree that represents a folder in a file system and its
 * contents, and stores enough information for file/folder paths to be retrieved from the tree.
 */
public class FileTreeItem extends TreeItem {
	private static final int NOT_ROOT = -1;
	private String name;
	private String path = null;
	private long size;
	private boolean folder;
	private int id = NOT_ROOT;
	private static final String[] fileSizeUnits = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
	private static final String fileSymbol = "\uD83D\uDCC4";
	private static final String folderSymbol = "\uD83D\uDCC1";

	/**
	 * Constructor for a sendable tree item from a File object
	 * @param file 		The file/folder to construct a sendable tree item from
	 * @param size		The size of the file/folder
	 */
	public FileTreeItem(File file, long size) {
		super(String.format("%s %s (%s)", file.isDirectory() ? folderSymbol : fileSymbol, file.getName(), generate3SFSizeString(size)));
		name = file.getName();
		this.size = size;
		folder = file.isDirectory();
	}

	/**
	 * Constructor for a receivable tree item from received data
	 * @param displayName Name displayed on the TreeView
	 * @param name 		Name of the receivable tree item
	 * @param size		Size of the receivable tree item
	 * @param folder 	Whether the receivable tree item is a folder or file
	 */
	public FileTreeItem(String displayName, String name, long size, boolean folder) {
		super(displayName);
		this.name = name;
		this.size = size;
		this.folder = folder;
	}

	/**
	 * Constructor for a upload/download tree item
	 * @param displayName Name displayed on the TreeView
	 * @param name 		Name of the upload/download tree item
	 * @param size		Size of the upload/download tree item
	 * @param path		Relative path of the upload/download tree item
	 * @param folder 	Whether the upload/download tree item is a folder or file
	 */
	public FileTreeItem(String displayName, String name, long size, boolean folder, String path) {
		super(new ProgressTreeCell(displayName));
		this.name = name;
		this.size = size;
		this.path = path;
		this.folder = folder;
	}

	/**
	 * Constructor for a root sendable tree item from a File object
	 * @param file		The file to construct a root sendable tree item from
	 * @param id		The id of the sendable tree item
	 */
	public FileTreeItem(File file, long size, int id) {
		super(String.format("%s %s (%s)", file.isDirectory() ? folderSymbol : fileSymbol, file.getName(), generate3SFSizeString(size)));
		name = file.getName();
		this.size = size;
		path = file.getAbsolutePath();
		folder = file.isDirectory();
		this.id = id;
	}

	/**
	 * Constructor for a root receivable tree item from received data
	 * @param name		Name of the root receivable tree item
	 * @param folder	Whether the root receivable tree item is a folder or file
	 * @param id		ID number of the root receivable tree item
	 */
	public FileTreeItem(String displayName, String name, long size, boolean folder, int id) {
		super(displayName);
		this.name = name;
		this.size = size;
		this.folder = folder;
		this.id = id;
	}

	/**
	 * Constructor for a root download/upload tree item
	 * @param name		Name of the root download/upload tree item
	 * @param folder	Whether the root download/upload tree item is a folder or file
	 * @param id		ID number of the root receivable/sendable associated with the download/upload root tree item
	 * @param path		Relative path to the download/upload from the associated root receivable/sendable
	 */
	public FileTreeItem(String displayName, String name, long size, boolean folder, int id, String path) {
		super(new ProgressTreeCell(displayName));
		this.name = name;
		this.size = size;
		this.folder = folder;
		this.id = id;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public long getSize() {
		return size;
	}

	public boolean isFolder() {
		return folder;
	}

	public boolean isRoot() {
		return !(id == NOT_ROOT);
	}

	public String getPath() {
		if (path != null)
			return path;
		else
			throw new IllegalStateException("Non-root sendables or all receivables have no path");
	}

	public int getId() {
		if (isRoot())
			return id;
		else
			throw new IllegalStateException("Non-root items have no id");
	}

	/**
	 * Get the name of this tree item displayed on the GUI
	 * @return The display name
	 */
	public String getDisplayName() {
		String displayName;
		try {
			displayName = (String)getValue();
		} catch (ClassCastException e) {
			displayName = ((ProgressTreeCell)getValue()).getText();
		}
		return displayName;
	}

	public double getProgress() {
		return ((ProgressTreeCell)getValue()).getProgress();
	}

	/**
	 * Set the progress of the progress bar
	 * @param progress The new progress value to set
	 */
	public void setProgress(double progress) {
		((ProgressTreeCell)getValue()).setProgress(progress);
	}

	public void updateProgress() {
		if (isFolder()) {
			FileTreeItem childItem;
			double progress = 0;
			for (Object child : getChildren()) {
				childItem = (FileTreeItem)child;
				if (childItem.isFolder()) {
					childItem.updateProgress();
				}
				progress += ((double)childItem.getSize())/size*childItem.getProgress();
			}
			setProgress(progress);
		}
	}

	/**
	 * Make a new download item from a receivable
	 * @return A new download root tree item
	 */
	public FileTreeItem receivableToDownload(int id) {
		FileTreeItem nextParent = this;
		LinkedList<String> pathString = new LinkedList<>();
		pathString.add(nextParent.getName());
		while (!nextParent.isRoot()) {
			nextParent = (FileTreeItem)nextParent.getParent();
			pathString.addFirst(nextParent.getName());
		}
		int rootId = nextParent.getId();

		FileTreeItem copy;
		copy = new FileTreeItem(getDisplayName(), name, size, isFolder(), id, String.format("%d//%s", rootId, String.join("/", pathString)));
		pathString.removeLast();
		copySubfolders(copy, this, String.join("/", pathString));
		return copy;
	}

	/**
	 * Make new upload item from a sendable id path
	 * @return A new upload root tree item
	 */
	public static FileTreeItem idPathToUpload(String path, int uploadId, ObservableList sendables) {
		String[] pathComponents = path.split("//");
		int sendableId = Integer.parseInt(pathComponents[0]);
		LinkedList<String> pathComponentsList = new LinkedList<>(Arrays.asList(pathComponents[1].split("/")));
		pathComponentsList.removeFirst();
		FileTreeItem rootItem = null;
		FileTreeItem childItem;
		synchronized (sendables) {
			for (Object rootSendable : sendables) {
				rootItem = (FileTreeItem) rootSendable;
				if (rootItem.getId() == sendableId) {
					break;
				}
			}
			if (rootItem == null) {
				throw new IllegalStateException("Could not find sendable with ID " + sendableId);
			}
			childItem = rootItem;
			boolean found;
			for (String pathComponent: pathComponentsList) {
				found = false;
				for (Object child : childItem.getChildren()) {
					if (((FileTreeItem) child).getName().equals(pathComponent)) {
						childItem = ((FileTreeItem) child);
						found = true;
						break;
					}
				}
				if (!found) {
					throw new IllegalStateException("Sendable path not mappable to sendable.");
				}
			}
		}
		String rootPath;
		if (pathComponentsList.size() > 0) {
			pathComponentsList.removeLast();
			rootPath = rootItem.getPath() + File.separatorChar + String.join(File.separator, pathComponentsList);
		}
		else {
			LinkedList rootPathList = new LinkedList<>(Arrays.asList(rootItem.getPath().split(Pattern.quote(File.separator))));
			rootPathList.removeLast();
			rootPath = String.join(File.separator, rootPathList);
		}
		FileTreeItem copy;
		copy = new FileTreeItem(childItem.getDisplayName(), childItem.getName(), childItem.getSize(), childItem.isFolder(), uploadId, rootPath);
		copySubfolders(copy, childItem, rootPath);
		return copy;
	}

	/**
	 * Copy subfolders of original to copy object
	 * @param copy		The copy object to have subfolders added
	 * @param original	The original object to copy subfolders from
	 */
	private static void copySubfolders(FileTreeItem copy, FileTreeItem original, String path) {
		FileTreeItem newItem;
		FileTreeItem currentItem;
		path = path + File.separatorChar + original.getName();
		for (Object child : original.getChildren()) {
			currentItem = (FileTreeItem) child;
			newItem = new FileTreeItem(currentItem.getDisplayName(), currentItem.getName(), currentItem.getSize(), currentItem.isFolder(), path);
			if (currentItem.isFolder()) {
				copySubfolders(newItem, currentItem, path);
			}
			copy.getChildren().add(newItem);
		}
	}

	/**
	 * Generates a display string from a file size
	 * @param size		The file size to generate a display string from
	 * @return			The display string
	 */
	public static String generate3SFSizeString(long size) {
		if (size == 0) {
			return "0B";
		}
		int sizeLength = String.valueOf(size).length();
		double doubleSize = ((double)size)/(Math.pow(1024, (sizeLength-1) / 3));
		String unit = fileSizeUnits[(sizeLength-1) / 3];
		return String.format("%.3G%s", doubleSize, unit);
	}
}
