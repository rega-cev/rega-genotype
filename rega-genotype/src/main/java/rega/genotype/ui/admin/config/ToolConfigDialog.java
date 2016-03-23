package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WDialog;
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
public class ToolConfigDialog extends WDialog {
	private enum Mode {Add, Edit}
	private Mode mode;
	private final Template template = new Template(tr("admin.config.tool-config-dialog"));
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

	public ToolConfigDialog(final ToolConfig toolConfig, boolean isReadOnly) {
		show();

		setWidth(new WLength(600));
		
		mode = toolConfig == null ? Mode.Add : Mode.Edit; 
		getTitleBar().addWidget(new WText(mode == Mode.Add ? "Create Tool" : "Edit Tool"));

		final WPushButton publishB = new WPushButton("Publish", getFooter());
		final WPushButton newVersionB = new WPushButton("Create new Version", getFooter());
		final WPushButton cancelB = new WPushButton("Cancel", getFooter());

		final String baseDir = Settings.getInstance().getBaseDir() + File.separator;

		// read

		if (mode == Mode.Edit) {
			ToolManifest toolMenifest = toolConfig.getToolMenifest();
			if (toolMenifest != null) {
				nameLE.setText(toolMenifest.getName());
				idLE.setText(toolMenifest.getId());
				versionLE.setText(toolMenifest.getVersion());
				blastChB.setChecked(toolMenifest.isBlastTool());
			}
			urlLE.setText(toolConfig.getPath());
			autoUpdateChB.setChecked(toolConfig.isAutoUpdate());
			serviceChB.setChecked(toolConfig.isWebService());
			uiChB.setChecked(toolConfig.isUi());

			idLE.disable();
		} 

		// validators
		nameLE.setValidator(new WValidator(true));
		idLE.setValidator(new WValidator(true));
		versionLE.setValidator(new WValidator(true));
		urlLE.setValidator(new WValidator(true));

		// bind
		
		getContents().addWidget(template);
		template.bindWidget("name", nameLE);
		template.bindWidget("id", idLE);
		template.bindWidget("version", versionLE);
		template.bindWidget("url", urlLE);
		template.bindWidget("blast", blastChB);
		template.bindWidget("update", autoUpdateChB);
		template.bindWidget("ui", uiChB);
		template.bindWidget("service", serviceChB);
		template.bindWidget("upload", fileUpload);
		template.bindWidget("info", infoT);

		if (isReadOnly) {
			template.disable();
			publishB.hide();
			newVersionB.hide();
		}
		
		// TODO: fileUpload will be replaced by editors per-file.
		// TODO: show file list.
		fileUpload.setMultiple(true);
		fileUpload.getWFileUpload().fileTooLarge().addListener(fileUpload, new Signal.Listener() {
			public void trigger() {
				infoT.setText("File too large.");
			}
		});

		newVersionB.clicked().addListener(newVersionB, new Signal.Listener() {
			public void trigger() {
				if (createNewVersion() != null)
					accept();
			}
		});

		publishB.clicked().addListener(publishB, new Signal.Listener() {
			public void trigger() {
				ToolConfig tool = createNewVersion();
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
							accept();							
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
				reject();
			}
		});

		initTemplate();
	}

	private void initTemplate() {
		for(int i = template.getChildren().size() - 1; i  >= 0; --i) {
			WWidget w =  template.getChildren().get(i);
			if (w instanceof WFormWidget){
				String var = template.varName(w);
				template.bindWidget(var + "-info", new WText());
			}
		}
	}
	
	private boolean validate() {
		boolean ans = true;
	
		for(WWidget w: template.getChildren()) {
			if (w instanceof WFormWidget){
				WFormWidget fw  = (WFormWidget) w;
			
				if (fw.validate() != WValidator.State.Valid) {
					ans = false;
					infoT.setText("Some fildes have invalid values.");
				}
				String var = template.varName(w);
				WText info = (WText) template.resolveWidget(var + "-info");
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

	private ToolConfig createNewVersion() {
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

		// save ToolConfig
		ToolConfig newTool = new ToolConfig();
		newTool.setAutoUpdate(autoUpdateChB.isChecked());
		newTool.setConfiguration(xmlDir);
		newTool.setJobDir(jobDir);
		newTool.setPath(urlLE.getText());
		newTool.setUi(uiChB.isChecked());
		newTool.setWebService(serviceChB.isChecked());

		// save cofig
		if (!config.addTool(newTool))
			return null;

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
}
