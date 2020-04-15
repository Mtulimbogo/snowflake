package muon.app.ui.components.session.files.local;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import muon.app.App;
import muon.app.common.FileInfo;
import muon.app.common.FileSystem;
import muon.app.common.local.LocalFileSystem;
import muon.app.ui.components.session.SessionInfo;
import muon.app.ui.components.session.files.AbstractFileBrowserView;
import muon.app.ui.components.session.files.FileBrowser;
import muon.app.ui.components.session.files.view.AddressBar;
import muon.app.ui.components.session.files.view.DndTransferData;
import muon.app.ui.components.session.files.view.DndTransferHandler;
import util.PathUtils;

public class LocalFileBrowserView extends AbstractFileBrowserView {
	private LocalMenuHandler menuHandler;
	private FileBrowser fileBrowser;
	private DndTransferHandler transferHandler;
	private LocalFileSystem fs;
	private JPopupMenu addressPopup;
	private JComboBox<String> cmbOptions = new JComboBox<>(
			new String[] { "Transfer normally", "Transfer in background" });

	public LocalFileBrowserView(FileBrowser fileBrowser, String initialPath,
			PanelOrientation orientation) {
		super(orientation, fileBrowser);// new Color(255, 255,
										// 240));
		this.fileBrowser = fileBrowser;
		this.menuHandler = new LocalMenuHandler(fileBrowser, this);
		this.menuHandler.initMenuHandler(this.folderView);
		this.transferHandler = new DndTransferHandler(this.folderView, null,
				this, DndTransferData.DndSourceType.LOCAL);
		this.folderView.setTransferHandler(transferHandler);
		this.folderView.setFolderViewTransferHandler(transferHandler);
		this.addressPopup = menuHandler.createAddressPopup();
		if (initialPath != null) {
			this.path = initialPath;
		}
		fileBrowser.getHolder().EXECUTOR.submit(() -> {
			try {
				this.fs = new LocalFileSystem();
				if (this.path == null) {
					path = fs.getHome();
				}
				List<FileInfo> list = fs.list(path);
				SwingUtilities.invokeLater(() -> {
					addressBar.setText(path);
					folderView.setItems(list);
					tabTitle.getCallback().accept(PathUtils.getFileName(path));
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void createAddressBar() {
		addressBar = new AddressBar(File.separatorChar, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedPath = e.getActionCommand();
				addressPopup.setName(selectedPath);
				MouseEvent me = (MouseEvent) e.getSource();
				addressPopup.show(me.getComponent(), me.getX(), me.getY());
				System.out.println("clicked");
			}
		});
		if (App.getGlobalSettings().isShowPathBar()) {
			addressBar.switchToPathBar();
		} else {
			addressBar.switchToText();
		}
	}

	@Override
	public String toString() {
		return "Local files [" + this.path + "]";
	}

	public String getHostText() {
		return "Local files";
	}

	public String getPathText() {
		return (this.path == null || this.path.length() < 1 ? "" : this.path);
	}

	@Override
	public void render(String path, boolean useCache) {
		this.render(path);
	}

	@Override
	public void render(String path) {
		this.path = path;
		fileBrowser.getHolder().EXECUTOR.submit(() -> {
			fileBrowser.disableUi();
			try {
				if (this.path == null) {
					this.path = fs.getHome();
				}
				List<FileInfo> list = fs.list(this.path);
				SwingUtilities.invokeLater(() -> {
					addressBar.setText(this.path);
					folderView.setItems(list);
					tabTitle.getCallback()
							.accept(PathUtils.getFileName(this.path));
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
			fileBrowser.enableUi();
		});
	}

	@Override
	public void openApp(FileInfo file) {
		// PlatformAppLauncher.shellLaunch(file.getPath());
	}

	@Override
	public boolean createMenu(JPopupMenu popup, FileInfo[] files) {
		menuHandler.createMenu(popup, files);
		return true;
	}

	protected void up() {
		String s = new File(path).getParent();
		if (s != null) {
			addBack(path);
			render(s);
		}
	}

	protected void home() {
		addBack(path);
		render(null);
	}

	@Override
	public void install(JComponent c) {

	}

	public boolean handleDrop(DndTransferData transferData) {
		System.out.println(
				"### " + transferData.getSource() + " " + this.hashCode());
		if (transferData.getSource() == this.hashCode()) {
			return false;
		}
		if (App.getGlobalSettings().isConfirmBeforeMoveOrCopy()
				&& JOptionPane.showConfirmDialog(null,
						"Move/copy files?") != JOptionPane.YES_OPTION) {
			return false;
		}
		try {
//			if (JOptionPane.showOptionDialog(holder,
//					new Object[] { "Please select a transfer mode",
//							cmbOptions },
//					"Transfer options", JOptionPane.OK_CANCEL_OPTION,
//					JOptionPane.PLAIN_MESSAGE, null, null,
//					null) != JOptionPane.OK_OPTION) {
//				return false;
//			}
			boolean backgroundTransfer = false;// cmbOptions.getSelectedIndex()
												// == 1;
			System.out.println("Dropped: " + transferData);
			int sessionHashCode = transferData.getInfo();
			if (sessionHashCode == 0)
				return true;
			SessionInfo info = fileBrowser.getInfo();
			if (info != null && info.hashCode() == sessionHashCode) {
				if (backgroundTransfer) {
					FileSystem sourceFs = null;// new SshFileSystem(new
												// SshModalUserInteraction(holder.getInfo()));
					FileSystem targetFs = new LocalFileSystem();
					fileBrowser.newFileTransfer(sourceFs, targetFs,
							transferData.getFiles(),
							transferData.getCurrentDirectory(), this.path,
							this.hashCode(), -1, true);
					return true;
				}
				FileSystem sourceFs = fileBrowser.getSSHFileSystem();
				if (sourceFs == null) {
					return false;
				}
				FileSystem targetFs = this.fs;
				fileBrowser.newFileTransfer(sourceFs, targetFs,
						transferData.getFiles(),
						transferData.getCurrentDirectory(), this.path,
						this.hashCode(), -1, false);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public FileSystem getFileSystem() throws Exception {
		return new LocalFileSystem();
	}
}
