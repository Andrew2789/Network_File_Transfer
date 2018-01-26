package Logic;

import javafx.scene.control.TreeItem;

import java.io.File;

public class RootTreeItem extends TreeItem {
	private int id;

	public RootTreeItem(File file, int id) {
		super(file.getAbsolutePath());
		this.id = id;
	}

	public RootTreeItem(String path, int id) {
		super(path);
		this.id = id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPath() {
		return (String)getValue();
	}

	public int getId() {
		return id;
	}
}
