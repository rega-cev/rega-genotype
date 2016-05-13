package rega.genotype.ui.admin.file_editor;

import java.io.File;

import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPanel;

public class SmartFileEditor extends WContainerWidget {
	private File toolDir;
	private BlastFileEditor blastFileEditor;

	public SmartFileEditor(File toolDir) {
		this.toolDir = toolDir;

		blastFileEditor = new BlastFileEditor(toolDir);
		
		WPanel fileEditorPanel = new WPanel(this);
		fileEditorPanel.setTitle("Blast");
		fileEditorPanel.setCentralWidget(blastFileEditor);
		fileEditorPanel.addStyleClass("admin-panel");
	}

	public void saveAll() {
		blastFileEditor.save();
	}

	public File getToolDir() {
		return toolDir;
	}

	public void setToolDir(File toolDir) {
		this.toolDir = toolDir;
		blastFileEditor.setToolDir(toolDir);
	}
}
