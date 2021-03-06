package code.gui;

import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * @author Andrew Davidson (a.n.d.9489@gmail.com)
 * Custom TreeItem derivative class designed for use in a tree that represents a folder in a file system and its
 * contents, and stores enough information for file/folder paths to be retrieved from the tree.
 */
public class FileTreeItem extends TreeItem {
	//Constants
	private static final int NOT_ROOT = -1;
	private static final String[] fileSizeUnits = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
	private static final String fileSymbol = "\uD83D\uDCC4";
	private static final String folderSymbol = "\uD83D\uDCC1";

	//Mandatory variables (set by all constructors)
	private String name;
	private long size;
	private boolean folder;

	//Optional variables (only set by some constructors)
	private int id = NOT_ROOT;
	private String path = null;
	private double progress;

	/**
	 * Constructor for a sendable tree item from a File object
	 * @param file 		The file/folder to construct a sendable tree item from
	 * @param size		The size of the file/folder
	 */
	public FileTreeItem(File file, long size) {
		super();
		setValue(new ProgressTreeCell(String.format("%s %s - %s", file.isDirectory() ? folderSymbol : fileSymbol, generate3SFSizeString(size), file.getName()), this));
		name = file.getName();
		this.size = size;
		path = htonPath(file.getAbsolutePath());
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
		super();
		setValue(new ProgressTreeCell(displayName, this));
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
		super(String.format("%s %s - %s", file.isDirectory() ? folderSymbol : fileSymbol, generate3SFSizeString(size), file.getName()));
		name = file.getName();
		this.size = size;
		path = htonPath(file.getAbsolutePath());
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
		super();
		setValue(new ProgressTreeCell(displayName, this));
		this.progress = 0;
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
		return progress;
	}

	/**
	 * Set the progress of the progress bar
	 * @param progress	The new progress value to set
	 */
	public void setProgress(double progress) {
		this.progress = progress;
		((ProgressTreeCell)getValue()).updateProgress();
	}

	public LinkedList<FileTreeItem> getPathFromRoot() {
		LinkedList<FileTreeItem> pathFromRoot = new LinkedList<>();
		try {
			pathFromRoot.add((FileTreeItem) this.getParent());
		} catch (ClassCastException e) {
			return null;
		}
		while (!pathFromRoot.getFirst().isRoot()) {
			pathFromRoot.addFirst((FileTreeItem) pathFromRoot.getFirst().getParent());
		}
		return pathFromRoot;
	}

	public void updateProgress(LinkedList<FileTreeItem> childPath) {
		if (folder) {
			double progress = 0;
			boolean foundChild = false;
			FileTreeItem childItem;
			childPath.removeFirst();
			for (Object child : getChildren()) {
				childItem = (FileTreeItem) child;
				if (childPath.size() > 0 && !foundChild && childItem == childPath.getFirst()) {
					childItem.updateProgress(childPath);
					foundChild = true;
				}
				if (size == 0) {
					progress += ((double) 1) / getChildren().size();
				} else {
					progress += ((double) childItem.getSize()) / size * childItem.getProgress();
				}
			}
			if (!foundChild && childPath.size() != 0) {
				throw new IllegalStateException("Not all children were found");
			}
			setProgress(progress);
 		}
	}

	/**
	 * Make a new download item from a receivable
	 * @return A new download root tree item
	 */
	public FileTreeItem toDownload(int id) {
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
		copySubfolders(copy, this, "");
		return copy;
	}

    /**
     * Make new upload item from a sendable id path
     * @return A new upload root tree item
     */
    public FileTreeItem toUpload() {
        LinkedList<String> pathComponents = new LinkedList<>(Arrays.asList(getPath().split("/")));
        pathComponents.removeLast();
        FileTreeItem copy = new FileTreeItem(getDisplayName(), getName(), getSize(), isFolder(), getId(), getPath());
        copySubfolders(copy, this, String.join(File.separator, pathComponents));
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

	public static String ntohPath(String path) {
        return String.join(Pattern.quote(File.separator), path.split("/"));
    }

    public static String htonPath(String path) {
        return String.join("/", path.split(Pattern.quote(File.separator)));
    }
}
