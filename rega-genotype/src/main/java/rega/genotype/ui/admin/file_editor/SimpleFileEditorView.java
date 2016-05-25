package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.config.ToolManifest;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.UploadedFile;

/**
 * Display and edit files from given folder.
 * 
 * @author michael
 */
public class SimpleFileEditorView extends WContainerWidget{
	private WTable layout = new WTable(this);
	private FileTabs fileTabs = new FileTabs();
	private FileTreeTable fileTree;
	private WPushButton addB;
	private WPushButton removeB;
	private DirtyHandler dirtyHandler = new DirtyHandler();
	private Signal1<File[]> changed = new Signal1<File[]>();

	public SimpleFileEditorView(final File workDir) {
		super();

		addB = new WPushButton("Add files");
		removeB = new WPushButton("Remove");

		final WAnchor downloadB = new WAnchor();
		downloadB.setTarget(AnchorTarget.TargetDownload);
		downloadB.setText("Download");
		downloadB.setStyleClass("like-button");
		downloadB.disable();
		
		ArrayList<String> excludedFileNames = new ArrayList<String>();
		excludedFileNames.add(ToolManifest.MANIFEST_FILE_NAME);
		fileTree = new FileTreeTable(workDir, excludedFileNames, false, false);
		
		Template fileTreeTemplate = new Template(tr("admin.config.file-editor.file-tree"));
		fileTreeTemplate.bindWidget("tree", fileTree);
		fileTreeTemplate.bindWidget("add", addB);
		fileTreeTemplate.bindWidget("remove", removeB);
		fileTreeTemplate.bindWidget("download", downloadB);

		fileTree.resize(300, 300);

		layout.getElementAt(0, 0).addWidget(fileTreeTemplate);
		layout.getElementAt(0, 1).addWidget(fileTabs);
		
		layout.getElementAt(0, 1).setWidth(new WLength("100%"));

		// signals

		fileTree.selctionChanged().addListener(fileTree, new Signal.Listener() {
			public void trigger() {
				List<File> currentFiles = fileTree.getCurrentFiles();
				if (currentFiles.size() == 1
						&& currentFiles.get(0).exists()) {
					File currentFile = currentFiles.get(0);
					fileTabs.showTab(currentFile);
					WFileResource resource = new WFileResource("", currentFile.getAbsolutePath());
					resource.suggestFileName(currentFile.getName());
					downloadB.setLink(new WLink(resource));
					downloadB.enable();
				} else {
					downloadB.disable();
				}
			}
		});

		removeB.clicked().addListener(removeB, new Signal.Listener() {
			public void trigger() {
				for(File file: fileTree.getCurrentFiles())
					if (file != null) {
						fileTabs.removeFile(file);
						file.delete();
					}
				fileTree.refresh();
				dirtyHandler.increaseDirty();
			}
		});

		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				WDialog d = new StandardDialog("Add files");

				final FileUpload fileUpload = new FileUpload();

				fileUpload.setMultiple(true);
				
				d.finished().addListener(d,  new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(DialogCode arg) {
						if (arg == DialogCode.Accepted) {
							for (UploadedFile f: fileUpload.getWFileUpload().getUploadedFiles()) {
								String[] split = f.getClientFileName().split(File.separator);
								String fileName = split[split.length - 1];
								File destFile = new File(workDir + File.separator + fileName);
								if (destFile.exists())
									destFile.delete();
								try {
									Files.copy(new File(f.getSpoolFileName()).toPath(), destFile.toPath());
								} catch (IOException e) {
									e.printStackTrace();
									Dialogs.infoDialog("Error", "<div>Some files could not be copied (maybe they already exist).</div>" +
											"<div> Error message: "+ e.getMessage() + "</div>");
								}
							}

							fileTree.refresh();
							dirtyHandler.increaseDirty();
						}
					}
				});

				d.getContents().addWidget(fileUpload);
			}
		});
	}

	public void saveAll() {
		fileTabs.saveAll();
	}

	public void refresh() {
		fileTree.refresh();
	}
	
	public void setReadOnly(boolean isReadOnly) {
		fileTabs.setReadOnly(isReadOnly);
		addB.disable();
		removeB.disable();
	}
	// classes

	private class FileTabs extends WTabWidget {

		private Map<File, SimpleFileEditor> openEditors = new HashMap<File, SimpleFileEditor>();
		private boolean isReadOnly;

		public FileTabs() {
			tabClosed().addListener(this,  new Signal1.Listener<Integer>() {
				public void trigger(Integer arg) {
					closeTab(arg);
				}
			});
		}

		private void closeTab(Integer index) {
			WWidget widget = getWidget(index);
			for (Map.Entry<File, SimpleFileEditor> e: openEditors.entrySet()){
				if (e.getValue().equals(widget)){
					openEditors.remove(e.getKey());
					break;
				}
			}
			removeTab(widget);
		}

		public void removeFile(File file) {
			if (openEditors.containsKey(file)) {
				closeTab(getIndexOf(openEditors.get(file)));
			}
		}

		public void showTab(final File file){
			//if (FileUtil.getFileExtension(file).equals("xml"))
			if (openEditors.containsKey(file))
				setCurrentWidget(openEditors.get(file));
			else {
				final SimpleFileEditor editor = new SimpleFileEditor(file);
				editor.setDisabled(isReadOnly);
				addTab(editor, file.getName());
				openEditors.put(file, editor);
				setCurrentWidget(editor);
				
				setTabCloseable(getIndexOf(editor), true);

				editor.changed().addListener(editor, new Signal.Listener() {
					public void trigger() {
						setTabText(getIndexOf(editor), "*" + file.getName());
						dirtyHandler.increaseDirty();
 						changed.trigger(new File[]{file});
					}
				});
			}
		}

		public void saveAll() {
			for(SimpleFileEditor e: openEditors.values())
				e.save();
		}

		public void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		public void refreshFiles() {
			for (SimpleFileEditor editor: openEditors.values()) {
				editor.rereadFile();
			}
		}
	}

	public DirtyHandler getDirtyHandler() {
		return dirtyHandler;
	}

	public Signal1<File[]> changed() {
		return changed;
	}

	public void rereadFiles() {
		fileTree.refresh();
		fileTabs.refreshFiles();
	}
}
