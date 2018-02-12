package Logic;

import GUI.ProgressTreeCell;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * @author Andrew Davidson (a.n.d.9489@gmail.com)
 * Custom TreeItem derivative class designed for use in a tree that represents a folder in a file system and its
 * contents, and stores enough information for file/folder paths to be retrieved from the tree.
 */
public class FileTreeItem extends TreeItem implements Serializable {
	private static final int NOT_ROOT = -1;
	private String name;
	private String path = null;
	private long size;
	private boolean folder;
	private int id = NOT_ROOT;
	private static final String[] fileSizeUnits = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

	/**
	 * Constructor for a sendable tree item from a File object
	 * @param file 		The file/folder to construct a sendable tree item from
	 * @param size		The size of the file/folder
	 */
	public FileTreeItem(File file, long size) {
		super(String.format("%s (%s)", file.getName(), generate3SFSizeString(size)));
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
	public FileTreeItem(String displayName, String name, long size, boolean folder, boolean progressBar) {
		super();
		if (progressBar) {
			this.setValue(new ProgressTreeCell(displayName));
		} else {
			this.setValue(displayName);
		}
		this.name = name;
		this.size = size;
		this.folder = folder;
	}

	/**
	 * Constructor for a root sendable tree item from a File object
	 * @param file		The file to construct a root sendable tree item from
	 * @param id		The id of the sendable tree item
	 */
	public FileTreeItem(File file, long size, int id) {
		super(String.format("%s (%s)", file.getName(), generate3SFSizeString(size)));
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

	public String getDisplayName() {
		String displayName;
		try {
			displayName = (String)getValue();
		} catch (ClassCastException e) {
			displayName = ((ProgressTreeCell)getValue()).getText();
		}
		return displayName;
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
	 * Make a deep copy of a FileTreeItem used to represent a receivable folder
	 * @return A deep copy of this object
	 */
	public FileTreeItem makeReceivableCopy(int id) {
		FileTreeItem nextParent = this;
		LinkedList<String> pathString = new LinkedList<>();
		while (!nextParent.isRoot()) {
			nextParent = (FileTreeItem)nextParent.getParent();
			pathString.add(nextParent.getName());
		}
		int rootId = nextParent.getId();

		FileTreeItem copy;
		copy = new FileTreeItem(getDisplayName(), name, size, isFolder(), id, String.format("%d/%s", rootId, String.join("/", pathString)));
		copySubfolders(copy, this);
		return copy;
	}

	/**
	 * Copy subfolders of original to copy object
	 * @param copy		The copy object to have subfolders added
	 * @param original	The original object to copy subfolders from
	 */
	private void copySubfolders(FileTreeItem copy, FileTreeItem original) {
		FileTreeItem newItem;
		FileTreeItem currentItem;
		for (Object child : original.getChildren()) {
			currentItem = (FileTreeItem) child;
			newItem = new FileTreeItem(currentItem.getDisplayName(), currentItem.getName(), currentItem.getSize(), currentItem.isFolder(), true);
			if (currentItem.isFolder()) {
				copySubfolders(newItem, currentItem);
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
