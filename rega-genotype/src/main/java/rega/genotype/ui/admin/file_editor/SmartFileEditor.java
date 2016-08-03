package rega.genotype.ui.admin.file_editor;

import java.io.File;

import rega.genotype.ui.admin.file_editor.blast.BlastFileEditor;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPanel;

public class SmartFileEditor extends WContainerWidget {
	private File workDir;
	private BlastFileEditor blastFileEditor;
	private DirtyHandler dirtyHandler = new DirtyHandler();

	public SmartFileEditor(File workDir) {
		this.workDir = workDir;

		blastFileEditor = new BlastFileEditor(workDir, dirtyHandler);

		WPanel fileEditorPanel = new WPanel(this);
		fileEditorPanel.setTitle("Blast");
		fileEditorPanel.setCentralWidget(blastFileEditor);
		fileEditorPanel.addStyleClass("admin-panel");
	}

	public boolean saveAll() {
		return blastFileEditor.save(workDir);
	}

	public boolean isDirty() {
		return dirtyHandler.isDirty();
	}

	public DirtyHandler getDirtyHandler() {
		return dirtyHandler;
	}

	public void rereadFiles() {
		blastFileEditor.rereadFiles();
	}

	public boolean validate() {
		return blastFileEditor.validate();
	}

	public Signal1<Integer> editingInnerXmlElement() {
		return blastFileEditor.editingInnerXmlElement();
	}
}
