package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.admin.file_editor.FileEditor;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Icon;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.StandardButton;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMessageBox;
import eu.webtoolkit.jwt.WPanel;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

/**
 * Edit tool configuration form.
 * 
 * @author michael
 */
public class ToolConfigForm extends FormTemplate {
	public enum Mode {Add, Edit, NewVersion, Install, Import}
	private final WText infoT = new WText();
	private ManifestForm manifestForm;
	private FileEditor fileEditor;
	private LocalConfigForm localConfigForm;
	private File toolDir;

	private Signal done = new Signal();

	public ToolConfigForm(final ToolConfig toolConfig, Mode mode) {
		super(tr("admin.config.tool-config-dialog"));
		
		final WPushButton publishB = new WPushButton("Publish");
		final WPushButton retractB = new WPushButton("Retract");
		final WPushButton saveB = new WPushButton("Save");
		final WPushButton cancelB = new WPushButton("Close");

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;

		infoT.addStyleClass("form-error");
		
		// file editor
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
		}

		retractB.setDisabled(!toolConfig.isPublished() || toolConfig.isRetracted());
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
		bindWidget("retract", retractB);
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
					Dialogs.infoDialog("Info", "Saved successfully.");
					dirtyHandler.setClean();
					if (fileEditor == null) // was not yet created.
						createFileEditors(savedToolConfig.getConfigurationFile());
				} else
					Dialogs.infoDialog("Info", "Could not save see validation errors.");
			}
		});

		retractB.clicked().addListener(retractB, new Signal.Listener() {
			public void trigger() {
				final WMessageBox d = new WMessageBox("Warning",
						"Retract will delete the tool from the public repository.", Icon.NoIcon,
						EnumSet.of(StandardButton.Ok, StandardButton.Cancel));
				d.show();
				d.setWidth(new WLength(300));
				d.buttonClicked().addListener(d,
						new Signal1.Listener<StandardButton>() {
					public void trigger(StandardButton e1) {
						if(e1 == StandardButton.Ok){
							try {
								ToolRepoServiceRequests.retract(
										toolConfig.getToolMenifest().getId(), 
										toolConfig.getToolMenifest().getVersion());
								toolConfig.setPublished(false);
								toolConfig.setRetracted(true);
								Settings.getInstance().getConfig().save();
								d.setText("Successfully retracted.");
								d.setButtons(StandardButton.Cancel);
							} catch (RegaGenotypeExeption e) {
								e.printStackTrace();
								d.setText("Error: could not retract the tool. " + e.getMessage());
								d.setButtons(StandardButton.Cancel);
							} catch (IOException e) {
								e.printStackTrace();
								d.setText("Error: could not retract the tool. " + e.getMessage());
								d.setButtons(StandardButton.Cancel);
							}
						}
						else
							d.remove();
					}
				});
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
			fileEditor = new FileEditor(toolDir);

			bindWidget("upload", fileEditor);

			dirtyHandler.connect(fileEditor.getSimpleFileEditor().getDirtyHandler(), 
					fileEditor.getSimpleFileEditor());
			dirtyHandler.connect(fileEditor.getSmartFileEditor().getDirtyHandler(),
					fileEditor.getSmartFileEditor());
		}
	}
	
	private ToolConfig save(boolean publishing) {
		if (!validate())
			return null;

		if (fileEditor != null)
			if (!fileEditor.saveAll())
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
		File xmlDir = new File(manifest.suggestXmlDirName());
		if (xmlDir.exists())
			return false;

		xmlDir.mkdirs();
		new File(manifest.suggestJobDirName()).mkdirs();

		// make sure that the xml dir name is {id}{version} 
		if (!toolDir.getAbsolutePath().equals(xmlDir)) {
			FileUtil.moveDirRecorsively(toolDir, xmlDir);
		}

		manifestForm.setToolDir(xmlDir);

		return true;
	}

	// classes

	public Signal done() {
		return done;
	}
}
