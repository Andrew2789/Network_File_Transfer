package code.gui;

import code.network.Main;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class TransferController implements Initializable {
    @FXML
    private Button pauseUpload, pauseDownload, removeUpload, removeDownload, clearCompletedUploads, clearCompletedDownloads, uploadFolder, uploadFile, openDownloadFolder;
    @FXML
    private TreeView uploadsTree, downloadsTree;

    public ObservableList getDownloads() {
        return downloadsTree.getRoot().getChildren();
    }

    public ObservableList getUploads() {
        return uploadsTree.getRoot().getChildren();
    }

    public void clearTrees() {
        uploadsTree.getRoot().getChildren().clear();
        downloadsTree.getRoot().getChildren().clear();
    }

    private FileTreeItem getSubfolder(File folder, boolean root) {
        LinkedList<FileTreeItem> subfolders = null;
        long size = 0;
        if (folder.isDirectory()) {
            subfolders = new LinkedList<>();
            for (File file : folder.listFiles()) {
                subfolders.add(getSubfolder(file, false));
            }
            for (FileTreeItem subfolder : subfolders) {
                size += subfolder.getSize();
            }
        } else {
            size = folder.length();
        }
        FileTreeItem newItem;
        if (root)
            newItem = new FileTreeItem(folder, size, Main.getNextUploadId());
        else
            newItem = new FileTreeItem(folder, size);

        if (subfolders != null) {
            for (FileTreeItem subfolder : subfolders) {
                newItem.getChildren().add(subfolder);
            }
        }
        return newItem;
    }

    public void uploadClicked() {
        removeUpload.setDisable(uploadsTree.getSelectionModel().getSelectedItem() == null || !((FileTreeItem) uploadsTree.getSelectionModel().getSelectedItem()).isRoot());
    }

    public void deleteSelectedUpload() {
        synchronized (uploadsTree.getRoot().getChildren()) {
            FileTreeItem toRemove = (FileTreeItem) uploadsTree.getSelectionModel().getSelectedItem();
            Main.sendableRemoved(toRemove.getId());
            toRemove.getParent().getChildren().remove(toRemove);
            uploadsTree.getSelectionModel().clearSelection();
        }
        removeUpload.setDisable(true);
    }

    public void downloadClicked() {
        removeDownload.setDisable(downloadsTree.getSelectionModel().getSelectedItem() == null);
    }

    public void pauseUpload() {

    }

    public void pauseDownload() {

    }

    public void removeUpload() {

    }

    public void removeDownload() {

    }

    public void clearCompletedUploads() {

    }

    public void clearCompletedDownloads() {

    }

    public void uploadFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Upload Folder");
        /*File defaultDirectory = new File("c:/dev/javafx");
        chooser.setInitialDirectory(defaultDirectory);*/
        File selected = chooser.showDialog(Main.getStage());
        if (selected != null) {
            FileTreeItem newItem = getSubfolder(selected, true);
            uploadsTree.getRoot().getChildren().add(newItem.toUpload());
        }
        Main.sendableAdded();
    }

    public void uploadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload File");
        File selected = chooser.showOpenDialog(Main.getStage());
        if (selected != null) {
            FileTreeItem newItem = getSubfolder(selected, true);
            uploadsTree.getRoot().getChildren().add(newItem.toUpload());
        }
        Main.sendableAdded();
    }

    public void openDownloadFolder() {

    }

    public void initialize(URL location, ResourceBundle resources) {
        downloadsTree.setRoot(new TreeItem<>("root"));
        uploadsTree.setRoot(new TreeItem<>("root"));
    }
}
