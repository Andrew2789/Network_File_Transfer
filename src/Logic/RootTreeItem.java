package Logic;

import javafx.scene.control.TreeItem;

import java.io.File;

public class RootTreeItem extends TreeItem {
	private int id;
	private File file;

	public RootTreeItem(File file, int id) {
		super(file.getName());
		this.id = id;
		this.file = file;
	}

	public void setId(int id) {
		this.id = id;
	}

	public File getFile() {
		return file;
	}

	public int getId() {
		return id;
	}
}
