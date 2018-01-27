package Logic;

import java.io.File;
import javafx.scene.control.TreeItem;

public class FileTreeItem extends TreeItem {
	private static final int NOT_ROOT = -1;
	private boolean folder;
	private int id;

	public FileTreeItem(File file) {
		super(file.getName());
		folder = file.isDirectory();
		id = NOT_ROOT;
	}

	public FileTreeItem(File file, int id) {
		super(file.getAbsolutePath());
		folder = file.isDirectory();
		this.id = id;
	}

	public FileTreeItem(String name, boolean folder) {
		super(name);
		this.folder = folder;
		this.id = NOT_ROOT;
	}

	public FileTreeItem(String path, boolean folder, int id) {
		super(path);
		this.folder = folder;
		this.id = id;
	}

	public boolean isFolder() {
		return folder;
	}

	public boolean isRoot() {
		return !(id == NOT_ROOT);
	}

	public String getIdentifier() {
		return (String)getValue();
	}

	public int getId() {
		if (isRoot())
			return id;
		else
			throw new IllegalStateException("Non-root items have no id");
	}
}
