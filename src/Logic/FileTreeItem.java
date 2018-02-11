package Logic;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import javafx.scene.control.TreeItem;

/**
 * @author Andrew Davidson (a.n.d.9489@gmail.com)
 * Custom TreeItem derivative class designed for use in a tree that represents a folder in a file system and its
 * contents, and stores enough information for file/folder paths to be retrieved from the tree.
 */
public class FileTreeItem extends TreeItem implements Serializable {
	private static final int NOT_ROOT = -1;
	private String path = null;
	private boolean folder;
	private int id = NOT_ROOT;

	/**
	 * Constructor for a sendable tree item from a File object
	 * @param file 		The file to construct a sendable tree item from
	 */
	public FileTreeItem(File file) {
		super(file.getName());
		folder = file.isDirectory();
	}

	/**
	 * Constructor for a receivable tree item from received data
	 * @param name 		Name of the receivable tree item
	 * @param folder 	Whether the receivable tree item is a folder or file
	 */
	public FileTreeItem(String name, boolean folder) {
		super(name);
		this.folder = folder;
	}

	/**
	 * Constructor for a root sendable tree item from a File object
	 * @param file		The file to construct a root sendable tree item from
	 * @param id		The id of the sendable tree item
	 */
	public FileTreeItem(File file, int id) {
		super(file.getName());
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
	public FileTreeItem(String name, boolean folder, int id) {
		super(name);
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
	public FileTreeItem(String name, boolean folder, int id, String path) {
		super(name);
		this.folder = folder;
		this.id = id;
		this.path = path;
	}

	public boolean isFolder() {
		return folder;
	}

	public boolean isRoot() {
		return !(id == NOT_ROOT);
	}

	public String getName() {
		return (String)getValue();
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
		copy = new FileTreeItem(getName(), isFolder(), id, String.format("%d/%s", rootId, String.join("/", pathString)));
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
			newItem = new FileTreeItem(currentItem.getName(), currentItem.isFolder());
			if (currentItem.isFolder()) {
				copySubfolders(newItem, currentItem);
			}
			copy.getChildren().add(newItem);
		}
	}
}
