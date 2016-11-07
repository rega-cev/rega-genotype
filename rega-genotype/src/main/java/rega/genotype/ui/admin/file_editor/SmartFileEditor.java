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
	private BlastFileEditor blastFileEditor = null;
	private UiFileEditor uiFileEditor = null;
	private DirtyHandler dirtyHandler = new DirtyHandler();
	private NgsModuleForm ngsModuleForm = null;
	private WPanel uiFileEditorPanel = null;
	private WPanel fileEditorPanel = null;
	private WPanel ngsPanel = null;
	private Signal1<Integer> editingInnerXmlElement = new Signal1<Integer>();

	public SmartFileEditor(final File workDir, final ManifestForm manifestForm) {
		this.workDir = workDir;

		if (manifestForm.getToolType() == ToolType.Ngs)
			createNgsModuleEditor();
		else
			createStandardEditors(manifestForm);

		showToolType(manifestForm.getToolType());
		manifestForm.toolTypeChanged().addListener(manifestForm, new Signal1.Listener<ToolType>() {
			public void trigger(ToolType toolType) {
				if (manifestForm.getToolType() == ToolType.Ngs)
					createNgsModuleEditor();
				else
					createStandardEditors(manifestForm);

				showToolType(toolType);
			}
		});
	}

	private void createStandardEditors(ManifestForm manifestForm) {
		if (blastFileEditor != null)
			return;

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

		blastFileEditor.editingInnerXmlElement().addListener(this, new Signal1.Listener<Integer>() {
			public void trigger(Integer arg) {
				editingInnerXmlElement.trigger(arg);
			}
		});
	}

	private void createNgsModuleEditor() {
		if (ngsModuleForm != null)
			return;

		ngsModuleForm = new NgsModuleForm(workDir);
		ngsPanel = new WPanel(this);
		ngsPanel.addStyleClass("admin-panel");
		ngsPanel.setTitle("NGS Module configuration");
		ngsPanel.setCentralWidget(ngsModuleForm);

		dirtyHandler.connect(ngsModuleForm.getDirtyHandler(), this);
		dirtyHandler.connect(ngsModuleForm);
	}

	private void showToolType(ToolType toolType) {
		if (fileEditorPanel != null) {
			fileEditorPanel.setHidden(toolType == ToolType.Ngs);
			uiFileEditorPanel.setHidden(toolType == ToolType.Ngs);
		}
		if (ngsPanel != null)
			ngsPanel.setHidden(toolType != ToolType.Ngs);
	}

	public boolean saveAll() {
		if (blastFileEditor != null)
			return blastFileEditor.save(workDir)&& uiFileEditor.save();
		else
			return true;
	}

	public boolean isDirty() {
		return dirtyHandler.isDirty();
	}

	public DirtyHandler getDirtyHandler() {
		return dirtyHandler;
	}

	public void rereadFiles() {
		if (blastFileEditor != null)
			blastFileEditor.rereadFiles();
	}

	public boolean validate() {
		if (blastFileEditor == null)
			return true;

		return blastFileEditor.validate();
	}

	public Signal1<Integer> editingInnerXmlElement() {
		return editingInnerXmlElement;
	}
}
