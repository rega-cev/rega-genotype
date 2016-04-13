package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
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
public class FileEditorView extends WContainerWidget{
	private WTable layout = new WTable(this);
	private FileTabs fileTabs = new FileTabs();
	private File rootDir;
	private FileTreeTable fileTree;

	public FileEditorView(final File root) {
		super();
		this.rootDir = root;

		final WPushButton addB = new WPushButton("Add files");
		final WPushButton removeB = new WPushButton("Remove");

		fileTree = new FileTreeTable(root, false, false);
		
		Template fileTreeTemplate = new Template(tr("admin.config.file-editor.file-tree"));
		fileTreeTemplate.bindWidget("tree", fileTree);
		fileTreeTemplate.bindWidget("add", addB);
		fileTreeTemplate.bindWidget("remove", removeB);

		fileTree.resize(200, 370);

		layout.getElementAt(0, 0).addWidget(fileTreeTemplate);
		layout.getElementAt(0, 1).addWidget(fileTabs);
		
		layout.getElementAt(0, 1).setWidth(new WLength("100%"));

		// signals

		fileTree.selctionChanged().addListener(fileTree, new Signal.Listener() {
			public void trigger() {
				File currentFile = fileTree.getCurrentFile();
				if (currentFile != null)
					fileTabs.showTab(currentFile);
			}
		});

		removeB.clicked().addListener(removeB, new Signal.Listener() {
			public void trigger() {
				File file = fileTree.getCurrentFile();
				if (file != null) {
					fileTabs.removeFile(file);
					file.delete();
					fileTree.refresh();
				}
			}
		});

		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				WDialog d = new StandardDialog("Add files");

				final FileUpload fileUpload = new FileUpload();

				fileUpload.setMultiple(true);
				fileUpload.uploadedFile().addListener(fileUpload, new Signal1.Listener<File>() {
					public void trigger(File arg) {
						for (UploadedFile f: fileUpload.getWFileUpload().getUploadedFiles()) {
							String[] split = f.getClientFileName().split(File.separator);
							String fileName = split[split.length - 1];
							try {
								Files.copy(new File(f.getSpoolFileName()).toPath(),
										new File(root + File.separator + fileName).toPath());
							} catch (IOException e) {
								e.printStackTrace();
								new MsgDialog("Error", "<div>Some files could not be copied (maybe they already exist).</div>" +
										"<div> Error message: "+ e.getMessage() + "</div>");
							}
						}

						fileTree.refresh();
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
	// classes

	public File getRootDir() {
		return rootDir;
	}

	public static class FileTabs extends WTabWidget {

		private Map<File, FileEditor> openEditors = new HashMap<File, FileEditor>();
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
			for (Map.Entry<File, FileEditor> e: openEditors.entrySet()){
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
				final FileEditor editor = new FileEditor(file);
				editor.setDisabled(isReadOnly);
				addTab(editor, file.getName());
				openEditors.put(file, editor);
				setCurrentWidget(editor);
				
				setTabCloseable(getIndexOf(editor), true);

				editor.changed().addListener(editor, new Signal.Listener() {
					public void trigger() {
						setTabText(getIndexOf(editor), "*" + file.getName());
					}
				});

				editor.saved().addListener(editor, new Signal.Listener() {
					public void trigger() {
						setTabText(getIndexOf(editor), file.getName());
					}
				});

			}
		}

		public void saveAll() {
			for(FileEditor e: openEditors.values())
				e.save();
		}

		public void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
			
		}
	}

	public void setReadOnly(boolean isReadOnly) {
		fileTabs.setReadOnly(isReadOnly);
	}
}
