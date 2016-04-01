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

	public ToolConfigForm(final ToolConfig toolConfig,
			final ToolManifest manifest, Mode mode) {
		super(tr("admin.config.tool-config-dialog"));
		
		setWidth(new WLength(600));

		final WPushButton publishB = new WPushButton("Publish");
		final WPushButton saveB = new WPushButton("Save All");
		final WPushButton cancelB = new WPushButton("Exit");

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;
		
		// file editor
		
		File toolDir = null;
		switch (mode) {
		case Add:
			try {
				toolDir = FileUtil.createTempDirectory("tmp-xml-dir");
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); 
			}
			 break; 
		case NewVersion:
			String oldVersionDir = Settings.getInstance().getXmlDir(
					manifest.getId(), manifest.getVersion());
			try {
				toolDir = FileUtil.createTempDirectory("tmp-xml-dir");
				FileUtil.copyDirContentRecorsively(new File(oldVersionDir), toolDir.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); 
			}
			break;
		case Edit:
			String dataDirStr = Settings.getInstance().getXmlDir(
					manifest.getId(), manifest.getVersion());
			toolDir = new File(dataDirStr);
			break;
		default:
			break;
		}
		fileEditor = new FileEditorView(toolDir);
		WPanel fileEditorPanel = new WPanel();
		fileEditorPanel.setWidth(new WLength(830));
		fileEditorPanel.setTitle("File editor");
		fileEditorPanel.setCentralWidget(fileEditor);

		// manifest and config 
		manifestForm = new ManifestForm(manifest, mode);
		localConfigForm = new LocalConfigForm(toolConfig, mode);
		
		// bind

		WPanel mamifestPanel = new WPanel();
		mamifestPanel.setTitle("Tool Manifest");
		mamifestPanel.setCentralWidget(manifestForm);

		WPanel configPanel = new WPanel();
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

		ToolConfig tool = localConfigForm.save(manifest);
		if (tool == null) {
			infoT.setText("Local configuration could not be saved.");
			return null;
		}

		return tool;
	}

	private void renameToolDir(ToolManifest manifest) {
		String xmlDir = Settings.getInstance().getXmlDir(
				manifest.getId(), manifest.getVersion());
		File toolDir = fileEditor.getRootDir();

		// make sure that the xml dir name is {id}{version} 
		if (!toolDir.getAbsolutePath().equals(xmlDir)) {
			FileUtil.moveDirRecorsively(toolDir, xmlDir);
		}

		// TODO: refresh file editor and local config.
	}

	// classes

	public Signal done() {
		return done;
	}
}
