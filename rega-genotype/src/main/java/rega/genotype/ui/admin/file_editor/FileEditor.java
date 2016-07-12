package rega.genotype.ui.admin.file_editor;

import java.io.File;

import rega.genotype.singletons.Settings;
import rega.genotype.singletons.ToolEditingSynchronizer;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WTabWidget;

/**
 * Coordinate between simple and smart file editors -> make sure 
 * that they show the same data.
 * All the files are saved in a tmp work dir and copied back to 
 * tool dir when save is clicked. 
 * 
 * @author michael
 */
public class FileEditor extends WTabWidget {
	private static final int SMART_EDITOR_TAB = 0;
	private static final int SIMPLE_EDITOR_TAB = 1;
	
	private SimpleFileEditorView simpleFileEditor;
	private SmartFileEditor smartFileEditor;
	private File workDir; // tmp dir with tool copy for coordination between smart and simple editors. 

	public FileEditor(File toolDir) {
		
		workDir = GenotypeLib.createJobDir(
				Settings.getInstance().getBaseJobDir() + File.separator + "file_editor");

		ToolEditingSynchronizer.getInstance().readTool(workDir, toolDir);

		// view

		simpleFileEditor = new SimpleFileEditorView(workDir);
		smartFileEditor = new SmartFileEditor(workDir);

		addTab(smartFileEditor, "Tool editor");
		addTab(simpleFileEditor, "Simple file editor");

		currentChanged().addListener(this, new Signal1.Listener<Integer>() {
			public void trigger(Integer arg) {
				if (getCurrentIndex() == SMART_EDITOR_TAB) {// simple -> smart
					smartFileEditor.rereadFiles();
				} else { // smart -> simple
					smartFileEditor.saveAll();
					simpleFileEditor.rereadFiles();
				}
			}
		});

		smartFileEditor.editingInnerXmlElement().addListener(this, new Signal1.Listener<Integer>() {
			public void trigger(Integer arg) {
				setTabEnabled(1, arg == 1);
			}
		});
	}

	public SimpleFileEditorView getSimpleFileEditor() {
		return simpleFileEditor;
	}

	public SmartFileEditor getSmartFileEditor() {
		return smartFileEditor;
	}

	public void setReadOnly(boolean isReadOnly) {
		simpleFileEditor.setReadOnly(isReadOnly);
		smartFileEditor.setDisabled(isReadOnly);
	}

	public boolean saveAll(File toolDir) {
		if (getCurrentIndex() == SMART_EDITOR_TAB) { // most resent changes are in sent changes are in smart
			smartFileEditor.saveAll();
			simpleFileEditor.rereadFiles();// only the simple editor contains all the files
		}

		simpleFileEditor.saveAll();

		// copy the changes back to tool dir.
		return ToolEditingSynchronizer.getInstance().saveTool(workDir, toolDir);
	}
}
