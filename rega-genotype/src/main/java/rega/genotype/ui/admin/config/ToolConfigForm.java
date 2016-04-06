package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.ui.admin.file_editor.FileEditorView;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPanel;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;

/**
 * Edit tool configuration form.
 * 
 * @author michael
 */
public class ToolConfigForm extends FormTemplate {
	public enum Mode {Add, Edit, NewVersion, Install}
	private final WText infoT = new WText();
	private FileEditorView fileEditor;
	private ManifestForm manifestForm;
	private LocalConfigForm localConfigForm;
	
	private Signal done = new Signal();

	public ToolConfigForm(final ToolConfig toolConfig) {
		super(tr("admin.config.tool-config-dialog"));
		
		final WPushButton publishB = new WPushButton("Publish");
		final WPushButton saveB = new WPushButton("Save All");
		final WPushButton cancelB = new WPushButton("Exit");

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;

		infoT.addStyleClass("form-error");
		
		// file editor
		
		File toolDir = new File(toolConfig.getConfiguration());

		fileEditor = new FileEditorView(toolDir);
		WPanel fileEditorPanel = new WPanel();
		fileEditorPanel.setTitle("File editor");
		fileEditorPanel.setCentralWidget(fileEditor);
		fileEditorPanel.addStyleClass("admin-panel");
		fileEditor.setHeight(new WLength(400));

		// manifest 
		manifestForm = new ManifestForm(toolConfig.getToolMenifest(), toolDir);
		
		// local config

		localConfigForm = new LocalConfigForm(toolConfig, toolDir);
		
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
		bindWidget("upload", fileEditorPanel);
		bindWidget("info", infoT);
		bindWidget("publish", publishB);
		bindWidget("save", saveB);
		bindWidget("cancel", cancelB);

		initInfoFields();
		validate();

		// signals 

		manifestForm.saved().addListener(manifestForm, new Signal1.Listener<File>() {
			public void trigger(File arg) {
				toolConfig.invalidateToolManifest();
				fileEditor.refresh();
			}
		});
		
		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				if (save(false) != null)
					done.trigger();
			}
		});

		publishB.clicked().addListener(publishB, new Signal.Listener() {
			public void trigger() {
				ToolConfig tool = save(true);
				if (tool != null) {
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
					} 
					if (FileUtil.zip(new File(tool.getConfiguration()), zip)){
						try {
							ToolRepoServiceRequests.publish(zip);
							done.trigger();				
						} catch (RegaGenotypeExeption e) {
							e.printStackTrace();
							infoT.setText("Error: could not publish the tool. " + e.getMessage());
						} catch (IOException e) {
							e.printStackTrace();
							infoT.setText("Error: could not publish the tool. " + e.getMessage());
						}
					} else {
						infoT.setText("Error could publish. Zipping went wrong.");
					}
				}
			}
		});

		cancelB.clicked().addListener(cancelB, new Signal.Listener() {
			public void trigger() {
				done.trigger();
			}
		});
	}

	@Override
	public boolean validate() {
		return manifestForm.validate() && localConfigForm.validate();
	}

	private ToolConfig save(boolean publishing) {
		if (!validate())
			return null;

		fileEditor.saveAll();

		ToolManifest manifest = manifestForm.save(publishing);
		if (manifest == null) {
			infoT.setText("Manifest could not be saved.");
			return null;
		}

		renameToolDir(manifest);

		ToolConfig tool = localConfigForm.save();
		if (tool == null) {
			infoT.setText("Local configuration could not be saved.");
			return null;
		}

		return tool;
	}

	private void renameToolDir(ToolManifest manifest) {
		String xmlDir = manifest.suggestXmlDirName();
		File toolDir = fileEditor.getRootDir();
		
		new File(xmlDir).mkdirs();
		new File(manifest.suggestJobDirName()).mkdirs();

		// make sure that the xml dir name is {id}{version} 
		if (!toolDir.getAbsolutePath().equals(xmlDir)) {
			FileUtil.moveDirRecorsively(toolDir, xmlDir);
		}

		localConfigForm.setXmlDir(new File(xmlDir));

		// TODO: refresh file editor
	}

	// classes

	public Signal done() {
		return done;
	}
}
