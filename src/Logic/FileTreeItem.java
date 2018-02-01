package Logic;

import java.io.File;
import javafx.scene.control.TreeItem;

/**
 * @author Andrew Davidson (a.n.d.9489@gmail.com)
 * Custom TreeItem derivative class designed for use in a tree that represents a folder in a file system and its
 * contents, and stores enough information for file/folder paths to be retrieved from the tree.
 */
public class FileTreeItem extends TreeItem {
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
}
