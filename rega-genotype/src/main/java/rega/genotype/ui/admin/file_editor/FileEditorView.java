package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WHBoxLayout;
import eu.webtoolkit.jwt.WTabWidget;

/**
 * Display and edit files from given folder.
 * 
 * @author michael
 */
public class FileEditorView extends WContainerWidget{
	private WHBoxLayout hLayout = new WHBoxLayout();
	private FileTabs fileTabs = new FileTabs();

	public FileEditorView(File root) {
		super();

		final FileTreeTable fileTable = new FileTreeTable(root);
		hLayout.addWidget(fileTable);
		hLayout.addWidget(fileTabs, 1);

		fileTable.selctionChanged().addListener(fileTable, new Signal.Listener() {
			public void trigger() {
				File currentFile = fileTable.getCurrentFile();
				if (currentFile != null)
					fileTabs.showTab(currentFile);
			}
		});
	}

	// classes

	public static class FileTabs extends WTabWidget {

		Map<File, FileEditor> openEditors = new HashMap<File, FileEditor>();

		public void showTab(File file){
			if (openEditors.containsKey(file))
				setCurrentWidget(openEditors.get(file));
			else {
				FileEditor editor = new FileEditor(file);
				addTab(editor, file.getName());
				openEditors.put(file, editor);
			}
		}
	}
}
