package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.ui.admin.file_editor.SimpleFileEditorView;
import rega.genotype.ui.admin.file_editor.SmartFileEditor;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WPanel;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTabWidget;
import eu.webtoolkit.jwt.WText;

/**
 * Edit tool configuration form.
 * 
 * @author michael
 */
public class ToolConfigForm extends FormTemplate {
	public enum Mode {Add, Edit, NewVersion, Install, Import}
	private final WText infoT = new WText();
	private SimpleFileEditorView fileEditor;
	private ManifestForm manifestForm;
	private LocalConfigForm localConfigForm;
	
	private Signal done = new Signal();
	private SmartFileEditor smartFileEditor;

	public ToolConfigForm(final ToolConfig toolConfig, Mode mode) {
		super(tr("admin.config.tool-config-dialog"));
		
		final WPushButton publishB = new WPushButton("Publish");
		final WPushButton saveB = new WPushButton("Save");
		final WPushButton cancelB = new WPushButton("Close");

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;

		infoT.addStyleClass("form-error");
		
		// file editor
		File toolDir;
		if (toolConfig.getConfiguration().isEmpty()){ // create new tool.
			toolDir = null;
		} else {
			toolDir = new File(toolConfig.getConfiguration());
		}

		createFileEditors(toolDir);

		// manifest 
		manifestForm = new ManifestForm(toolConfig.getToolMenifest(), toolDir, mode);
		
		// local config

		localConfigForm = new LocalConfigForm(toolConfig, manifestForm);
		
		// disable 

		if (toolConfig.isPublished()) {
			publishB.disable();
			manifestForm.disable();
			fileEditor.setReadOnly(true);
			smartFileEditor.disable();
		} 

		saveB.disable();

		// bind

		WPanel mamifestPanel = new WPanel();
		mamifestPanel.addStyleClass("admin-panel");
		mamifestPanel.setTitle("Tool Manifest");
		mamifestPanel.setCentralWidget(manifestForm);

		WPanel configPanel = new WPanel();
		configPanel.addStyleClass("admin-panel");
		configPanel.setTitle("Tool local configuration");
		configPanel.setCentralWidget(localConfigForm);
		
		bindWidget("manifest", mamifestPanel);
		bindWidget("config", configPanel);
		bindWidget("info", infoT);
		bindWidget("publish", publishB);
		bindWidget("save", saveB);
		bindWidget("cancel", cancelB);

		init();

		// signals 

		manifestForm.saved().addListener(manifestForm, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				toolConfig.invalidateToolManifest();
				localConfigForm.getToolConfig().invalidateToolManifest();
			}
		});

		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				ToolConfig savedToolConfig = save(false);
				if (savedToolConfig != null) {
					new MsgDialog("Info", "Saved successfully.");
					dirtyHandler.setClean();
					if (fileEditor == null) // was not yet created.
						createFileEditors(savedToolConfig.getConfigurationFile());
				} else
					new MsgDialog("Info", "Could not save see validation errors.");
			}
		});

		publishB.clicked().addListener(publishB, new Signal.Listener() {
			public void trigger() {
				ToolConfig tool = save(true);
				if (tool != null) {
					boolean publishFailed = false;
					// create zip file 
					ToolManifest manifest = tool.getToolMenifest();
					File zip = new File(baseDir + "published" + File.separator + manifest.getUniqueToolId() + ".zip");
					if (zip.exists()) 
						zip.delete();
					try {
						zip.getParentFile().mkdirs();
						zip.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						infoT.setText("Error could publish.");
						publishFailed = true;
					} 
					if (!publishFailed && FileUtil.zip(new File(tool.getConfiguration()), zip)){
						try {
							ToolRepoServiceRequests.publish(zip);
							tool.setPublished(true);
							Settings.getInstance().getConfig().save();
							done.trigger();				
						} catch (RegaGenotypeExeption e) {
							e.printStackTrace();
							infoT.setText("Error: could not publish the tool. " + e.getMessage());
							publishFailed = true;
						} catch (IOException e) {
							e.printStackTrace();
							infoT.setText("Error: could not publish the tool. " + e.getMessage());
							publishFailed = true;
						}
					} else {
						infoT.setText("Error could publish. Zipping went wrong.");
						publishFailed = true;
					}
					if (publishFailed){
						manifest.setPublicationDate(null);
						manifest.save(tool.getConfiguration());
					}
				}
			}
		});

		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				if (dirtyHandler.isDirty()){
					final WMessageBox d = new WMessageBox("Warning",
							"There are some unsaved changes. Close anyway?", Icon.NoIcon,
							EnumSet.of(StandardButton.Ok, StandardButton.Cancel));
					d.show();
					d.setWidth(new WLength(300));
					d.buttonClicked().addListener(d,
							new Signal1.Listener<StandardButton>() {
						public void trigger(StandardButton e1) {
							if(e1 == StandardButton.Ok){
								done.trigger();
							}
							d.remove();
						}
					});

				} else
					done.trigger();
			}
		});

		// dirty

		dirtyHandler.connect(manifestForm.getDirtyHandler(), manifestForm);
		dirtyHandler.connect(localConfigForm.getDirtyHandler(), localConfigForm);
		dirtyHandler.dirty().addListener(this, new Signal.Listener() {
			public void trigger() {
				saveB.enable();
			}
		});
	}

	@Override
	public boolean validate() {
		return manifestForm.validate() && localConfigForm.validate();
	}

	private void createFileEditors(File toolDir) {
		if (toolDir == null){
			// when creating a new tool file editors are created only
			// after tool manifest is saved for the first time.
			bindEmpty("upload"); 
		} else {
			fileEditor = new SimpleFileEditorView(toolDir);
			smartFileEditor = new SmartFileEditor(toolDir);

			final WTabWidget fileEditorTabs = new WTabWidget();
			fileEditorTabs.addTab(smartFileEditor, "Tool editor");
			fileEditorTabs.addTab(fileEditor, "Simple file editor");

			bindWidget("upload", fileEditorTabs);

			dirtyHandler.connect(fileEditor.getDirtyHandler(), fileEditor);
			dirtyHandler.connect(smartFileEditor.getDirtyHandler(), smartFileEditor);
		}
	}
	
	private ToolConfig save(boolean publishing) {
		if (!validate())
			return null;

		if (fileEditor != null && fileEditor.getDirtyHandler().isDirty())
			fileEditor.saveAll();

		if (smartFileEditor != null && smartFileEditor.isDirty())
			if (!smartFileEditor.saveAll())
				return null;

		if (publishing || manifestForm.isDirty()){
			ToolManifest manifest = manifestForm.save(publishing);
			if (manifest == null) {
				infoT.setText("Manifest could not be saved.");
				return null;
			}

			renameToolDir(manifest);
		}

		ToolConfig tool = localConfigForm.save(manifestForm.getToolDir());
		if (tool == null) {
			infoT.setText("Local configuration could not be saved.");
			return null;
		}

		return tool;
	}

	private boolean renameToolDir(ToolManifest manifest) {
		String xmlDir = manifest.suggestXmlDirName();
		if (new File(xmlDir).exists())
			return false;

		File toolDir = fileEditor.getToolDir();

		new File(xmlDir).mkdirs();
		new File(manifest.suggestJobDirName()).mkdirs();

		// make sure that the xml dir name is {id}{version} 
		if (!toolDir.getAbsolutePath().equals(xmlDir)) {
			FileUtil.moveDirRecorsively(toolDir, xmlDir);
		}

		manifestForm.setToolDir(new File(xmlDir));
		smartFileEditor.setToolDir(new File(xmlDir));
		fileEditor.setToolDir(new File(xmlDir));

		// TODO: refresh file editor

		return true;
	}

	// classes

	public Signal done() {
		return done;
	}
}
