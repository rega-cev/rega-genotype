package rega.genotype.ui.admin.file_editor;

import java.io.File;

import rega.genotype.ui.admin.config.ManifestForm;
import rega.genotype.ui.admin.config.NgsModuleForm;
import rega.genotype.ui.admin.config.ManifestForm.ToolType;
import rega.genotype.ui.admin.file_editor.blast.BlastFileEditor;
import rega.genotype.ui.admin.file_editor.ui.UiFileEditor;
import rega.genotype.ui.framework.widgets.DirtyHandler;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WPanel;

public class SmartFileEditor extends WContainerWidget {
	private File workDir;
	private BlastFileEditor blastFileEditor;
	private UiFileEditor uiFileEditor;
	private DirtyHandler dirtyHandler = new DirtyHandler();
	private NgsModuleForm ngsModuleForm;
	private WPanel uiFileEditorPanel;
	private WPanel fileEditorPanel;
	private WPanel ngsPanel;

	public SmartFileEditor(File workDir, ManifestForm manifestForm) {
		this.workDir = workDir;

		blastFileEditor = new BlastFileEditor(workDir, manifestForm, dirtyHandler);

		fileEditorPanel = new WPanel(this);
		fileEditorPanel.setTitle("Blast");
		fileEditorPanel.setCentralWidget(blastFileEditor);
		fileEditorPanel.addStyleClass("admin-panel");

		uiFileEditor = new UiFileEditor(workDir, dirtyHandler);
		dirtyHandler.connect(uiFileEditor);
		

		uiFileEditorPanel = new WPanel(this);
		uiFileEditorPanel.setCollapsible(true);
		uiFileEditorPanel.setCollapsed(true);
		uiFileEditorPanel.setTitle("UI");
		uiFileEditorPanel.setCentralWidget(uiFileEditor);
		uiFileEditorPanel.addStyleClass("admin-panel");

		ngsModuleForm = new NgsModuleForm(workDir);
		ngsPanel = new WPanel(this);
		ngsPanel.addStyleClass("admin-panel");
		ngsPanel.setTitle("NGS Module configuration");
		ngsPanel.setCentralWidget(ngsModuleForm);

		showToolType(manifestForm.getToolType());
		manifestForm.toolTypeChanged().addListener(manifestForm, new Signal1.Listener<ToolType>() {
			public void trigger(ToolType toolType) {
				showToolType(toolType);
			}
		});
	}

	private void showToolType(ToolType toolType) {
		fileEditorPanel.setHidden(toolType == ToolType.Ngs);
		uiFileEditorPanel.setHidden(toolType == ToolType.Ngs);
		ngsPanel.setHidden(toolType != ToolType.Ngs);
	}

	public boolean saveAll() {
		return blastFileEditor.save(workDir)&& uiFileEditor.save();
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
