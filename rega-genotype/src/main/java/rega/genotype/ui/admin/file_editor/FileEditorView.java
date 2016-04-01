package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.ui.util.FileUpload;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
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

	public FileEditorView(final File root) {
		super();
		this.rootDir = root;

		final FileUpload fileUpload = new FileUpload();
		final FileTreeTable fileTree = new FileTreeTable(root, false, false);
		
		fileTree.resize(200, 380);

		layout.getElementAt(0, 0).addWidget(new WText("Upload files from disc: "));
		layout.getElementAt(0, 1).addWidget(fileUpload);

		layout.getElementAt(1, 0).addWidget(fileTree);
		layout.getElementAt(1, 1).addWidget(fileTabs);

		// signals

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

		fileTree.selctionChanged().addListener(fileTree, new Signal.Listener() {
			public void trigger() {
				File currentFile = fileTree.getCurrentFile();
				if (currentFile != null)
					fileTabs.showTab(currentFile);
			}
		});
	}

	public void saveAll() {
		fileTabs.saveAll();
	}

	// classes

	public File getRootDir() {
		return rootDir;
	}

	public static class FileTabs extends WTabWidget {

		private Map<File, FileEditor> openEditors = new HashMap<File, FileEditor>();

		public FileTabs() {
			tabClosed().addListener(this,  new Signal1.Listener<Integer>() {
				public void trigger(Integer arg) {
					WWidget widget = getWidget(arg);
					for (Map.Entry<File, FileEditor> e: openEditors.entrySet()){
						if (e.getValue().equals(widget)){
							openEditors.remove(e.getKey());
							break;
						}
					}
					removeTab(widget);
				}
			});
		}

		public void showTab(final File file){
			//if (FileUtil.getFileExtension(file).equals("xml"))
			if (openEditors.containsKey(file))
				setCurrentWidget(openEditors.get(file));
			else {
				final FileEditor editor = new FileEditor(file);
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
	}
}
