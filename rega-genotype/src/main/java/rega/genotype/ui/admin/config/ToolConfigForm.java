package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.ui.framework.Global;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;
import eu.webtoolkit.jwt.WValidator.Result;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.UploadedFile;

/**
 * Edit tool configuration form.
 * 
 * @author michael
 */
public class ToolConfigForm extends Template {
	public  enum Mode {Add, Edit, NewVersion, Install}
	private Mode mode;
	private final WText infoT = new WText();
	private final FileUpload fileUpload = new FileUpload();
	private final WLineEdit nameLE = new WLineEdit();
	private final WLineEdit idLE = new WLineEdit();
	private final WLineEdit versionLE = new WLineEdit();
	private final WLineEdit urlLE = new WLineEdit();
	private final WCheckBox blastChB = new WCheckBox();
	private final WCheckBox autoUpdateChB = new WCheckBox();
	private final WCheckBox serviceChB = new WCheckBox();
	private final WCheckBox uiChB = new WCheckBox();
	private Signal done = new Signal();

	public ToolConfigForm(final ToolConfig toolConfig,
			final ToolManifest manifest, Mode mode) {
		super(tr("admin.config.tool-config-dialog"));

		this.mode = mode;
		
		setWidth(new WLength(600));

		final WPushButton publishB = new WPushButton("Publish");
		final WPushButton saveB = new WPushButton("Save");
		final WPushButton cancelB = new WPushButton("Cancel");

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;

		// read

		if (manifest != null) {
			nameLE.setText(manifest.getName());
			idLE.setText(manifest.getId());
			versionLE.setText(manifest.getVersion());
			blastChB.setChecked(manifest.isBlastTool());
		}
		if (toolConfig != null){
			urlLE.setText(toolConfig.getPath());
			autoUpdateChB.setChecked(toolConfig.isAutoUpdate());
			serviceChB.setChecked(toolConfig.isWebService());
			uiChB.setChecked(toolConfig.isUi());
		}

		idLE.setDisabled(mode != Mode.Add);
		versionLE.setDisabled(mode == Mode.Edit || mode == Mode.Install);

		// validators
		nameLE.setValidator(new WValidator(true));
		if (mode == Mode.Add || mode == Mode.NewVersion) {
			idLE.setValidator(new ToolIdValidator(true));
			versionLE.setValidator(new ToolIdValidator(true));
			urlLE.setValidator(new ToolUrlValidator(false));
		} else {
			idLE.setValidator(new WValidator(true));
			versionLE.setValidator(new WValidator(true));
		}

		// bind
		
		bindWidget("name", nameLE);
		bindWidget("id", idLE);
		bindWidget("version", versionLE);
		bindWidget("url", urlLE);
		bindWidget("blast", blastChB);
		bindWidget("update", autoUpdateChB);
		bindWidget("ui", uiChB);
		bindWidget("service", serviceChB);
		bindWidget("upload", fileUpload);
		bindWidget("info", infoT);
		bindWidget("publish", publishB);
		bindWidget("save", saveB);
		bindWidget("cancel", cancelB);

		// TODO: fileUpload will be replaced by editors per-file.
		
		String uploadedFiles = new String();
		if (manifest != null){
		File baseXmlDir = new File(
				Settings.getInstance().getXmlDir(manifest.getId(), manifest.getVersion()));
		if (baseXmlDir.exists() && baseXmlDir.listFiles() != null) {
			for (File f: baseXmlDir.listFiles()){
				uploadedFiles += "<div>" + f.getName() + "</div>";
			}
		}
		}
		bindString("upload-list", uploadedFiles);

		fileUpload.setMultiple(true);
		fileUpload.getWFileUpload().fileTooLarge().addListener(fileUpload, new Signal.Listener() {
			public void trigger() {
				infoT.setText("File too large.");
			}
		});

		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				if (save() != null)
					done.trigger();
			}
		});

		publishB.clicked().addListener(publishB, new Signal.Listener() {
			public void trigger() {
				ToolConfig tool = save();
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

		initTemplate();
	}

	private void initTemplate() {
		for(int i = getChildren().size() - 1; i  >= 0; --i) {
			WWidget w =  getChildren().get(i);
			if (w instanceof WFormWidget){
				String var = varName(w);
				bindWidget(var + "-info", new WText());
			}
		}
	}
	
	private boolean validate() {
		boolean ans = true;
	
		for(WWidget w: getChildren()) {
			if (w instanceof WFormWidget){
				WFormWidget fw  = (WFormWidget) w;
			
				if (fw.validate() != WValidator.State.Valid) {
					ans = false;
					infoT.setText("Some fildes have invalid values.");
				}
				String var = varName(w);
				WText info = (WText) resolveWidget(var + "-info");
				if (info != null && fw.getValidator() != null) {
					if (fw.getValueText() == null)
						info.setText("");
					else {
						Result r = fw.getValidator().validate(fw.getValueText());
						info.setText(r == null ? "" :r.getMessage());
					}
				}
			}
		}
		return ans;
	}

	private ToolConfig save() {
		if (!validate())
			return null;

		Config config = Settings.getInstance().getConfig();
		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;
		
		String xmlDir = Settings.getInstance().getXmlDir(
				idLE.getText(), versionLE.getText());
		String jobDir = Settings.getInstance().getJobDir(
				idLE.getText(), versionLE.getText());

		new File(xmlDir).mkdirs();
		new File(jobDir).mkdirs();

		// save xml files
		for (UploadedFile f: fileUpload.getWFileUpload().getUploadedFiles()) {
			String[] split = f.getClientFileName().split(File.separator);
			String fileName = split[split.length - 1];
			FileUtil.storeFile(new File(f.getSpoolFileName()), xmlDir + fileName);
		}

		// save manifest
		ToolManifest manifest = new ToolManifest();
		manifest.setBlastTool(blastChB.isChecked());
		manifest.setName(nameLE.getText());
		manifest.setId(idLE.getText());
		manifest.setVersion(versionLE.getText());
		manifest.setPublisherName(config.getGeneralConfig().getPublisherName());
		manifest.setSoftwareVersion(Global.SOFTWARE_VERSION);

		// save ToolConfig
		ToolConfig newTool = config.getToolConfigById(manifest.getId(), manifest.getVersion());
		if (newTool == null) {
			assert(mode != Mode.Edit);
			newTool = new ToolConfig();
			if (!config.addTool(newTool))
				return null;
		}

		newTool.setAutoUpdate(autoUpdateChB.isChecked());
		newTool.setConfiguration(xmlDir);
		newTool.setJobDir(jobDir);
		newTool.setPath(urlLE.getText());
		newTool.setUi(uiChB.isChecked());
		newTool.setWebService(serviceChB.isChecked());

		try {
			manifest.save(xmlDir);
			config.save(baseDir);
		} catch (IOException e) {
			e.printStackTrace();
			infoT.setText("Error: Config file could not be properlly updated.");
			return null;
		}
		return newTool;
	}

	// classes

	public Signal done() {
		return done;
	}

	private class ToolIdValidator extends WValidator {
		ToolIdValidator(boolean isMandatory) {
			super(isMandatory);
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			if (config.getToolConfigById(idLE.getText(), versionLE.getText()) != null)
				return new Result(State.Invalid, "A tool with same id and version already exist on local server.");

			return super.validate(input);
		}
	}

	private class ToolUrlValidator extends WValidator {
		ToolUrlValidator(boolean isMandatory) {
			super(isMandatory);
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			if (config.getToolConfigByUrlPath(urlLE.getText()) != null)
				return new Result(State.Invalid, "A tool with same url already exist on local server.");

			return super.validate(input);
		}
	}
}
