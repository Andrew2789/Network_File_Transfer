package Java.GUI;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;

public class ProgressTreeCell extends AnchorPane {
	private ProgressBar progressBar;
	private Label text;
	private String labelText;
	private FileTreeItem parent;

	public ProgressTreeCell(String labelText, FileTreeItem parent) {
		super();
		this.parent = parent;
		this.labelText = labelText;
		this.maxHeight(Double.MAX_VALUE);
		this.maxWidth(Double.MAX_VALUE);
		this.setMinHeight(28);

		progressBar = new ProgressBar();
		progressBar.setProgress(0);
		progressBar.setMinHeight(26);
		progressBar.maxWidth(Double.MAX_VALUE);
		progressBar.maxHeight(Double.MAX_VALUE);
		this.getChildren().add(progressBar);
		setTopAnchor(progressBar, 0.0);
		setLeftAnchor(progressBar, 0.0);
		setRightAnchor(progressBar, 0.0);
		setBottomAnchor(progressBar, 0.0);

		text = new Label("0%  "+labelText);
		text.maxWidth(Double.MAX_VALUE);
		text.maxHeight(Double.MAX_VALUE);
		this.getChildren().add(text);
		setTopAnchor(text, 0.0);
		setLeftAnchor(text, 3.0);
		setRightAnchor(text, 0.0);
		setBottomAnchor(text, 0.0);
	}

	public String getText() {
		return labelText;
	}

	public void updateProgress() {
		Platform.runLater(() -> {
			double progress = parent.getProgress();
			progressBar.setProgress(progress);
			text.setText(String.format("%.1f%%  %s", progress * 100, labelText));
		});
	}
}
