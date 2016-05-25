package rega.genotype.ui.admin.file_editor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
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
	private File toolDir;

	public FileEditor(File toolDir) {
		
		this.toolDir = toolDir;
		workDir = GenotypeLib.createJobDir(
				Settings.getInstance().getBaseJobDir() + File.separator + "file_editor");

		try {
			FileUtil.copyDirContentRecorsively(toolDir, workDir);
		} catch (IOException e) {
			e.printStackTrace();
			assert(false);
		}

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

	public boolean saveAll() {
		if (getCurrentIndex() == SMART_EDITOR_TAB) { // most resent changes are in sent changes are in smart
			smartFileEditor.saveAll();
			simpleFileEditor.rereadFiles();// only the simple editor contains all the files
		}

		simpleFileEditor.saveAll();

		// copy the changes back to tool dir.
		try {
			// TODO: 
			// 1. lock ()! 
			// 2. copy toolDir to temp dir (in case that move does not works)!
			FileUtils.deleteDirectory(toolDir);
			FileUtils.moveDirectory(workDir, toolDir);
			workDir.mkdirs();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
