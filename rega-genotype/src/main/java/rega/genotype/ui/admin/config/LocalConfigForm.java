package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WValidator;

/**
 * Form for editing tool local configuration (url on local server,
 * has auto update, has ui, has web service).
 * 
 * @author michael
 */
public class LocalConfigForm  extends FormTemplate {

	private final WLineEdit urlLE = initLineEdit();
	private final WCheckBox autoUpdateChB = new WCheckBox();
	private final WCheckBox serviceChB = new WCheckBox();
	private final WCheckBox uiChB = new WCheckBox();
	private final WPushButton saveB = new WPushButton("save");

	//private ToolManifest manifest;
	private ToolConfig toolConfig;
	private File xmlDir;

	public LocalConfigForm(final ToolConfig toolConfig, File xmlDir) {
		super(tr("admin.config.tool-config-dialog.config"));
		this.toolConfig = toolConfig;
		this.xmlDir = xmlDir;

		// read

		if (toolConfig != null){
			urlLE.setText(toolConfig.getPath() == null ? "" : toolConfig.getPath());
			autoUpdateChB.setChecked(toolConfig.isAutoUpdate());
			serviceChB.setChecked(toolConfig.isWebService());
			uiChB.setChecked(toolConfig.isUi());
		}

		urlLE.setValidator(new ToolUrlValidator(false));

		// bind

		bindWidget("url", urlLE);
		bindWidget("update", autoUpdateChB);
		bindWidget("ui", uiChB);
		bindWidget("service", serviceChB);
		bindWidget("save", saveB);

		initInfoFields();
		validate();

		// signals

		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				if (save() != null)
					new MsgDialog("Info", "Local configuration saved.");
				else
					new MsgDialog("Error", "Could not save local configuration.");
			}
		});
	}

	public ToolConfig save() {
		if (!validate())
			return null;

		Config config = Settings.getInstance().getConfig();

		// save ToolConfig
		if (toolConfig == null) {
			toolConfig = new ToolConfig();
			try {
				toolConfig.setJobDir(FileUtil.createTempDirectory(
						"tool-job-dir", new File(Settings.getInstance().getBaseJobDir())).getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		toolConfig.setConfiguration(xmlDir.getAbsolutePath() + File.separator);
		toolConfig.setAutoUpdate(autoUpdateChB.isChecked());
		toolConfig.setPath(urlLE.getText());
		toolConfig.setUi(uiChB.isChecked());
		toolConfig.setWebService(serviceChB.isChecked());

		config.putTool(toolConfig);

		try {
			config.save();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return toolConfig;
	}


	private WLineEdit initLineEdit() {
		WLineEdit le = new WLineEdit();
		le.setWidth(new WLength(200));
		return le;
	}

	public File getXmlDir() {
		return xmlDir;
	}

	public void setXmlDir(File xmlDir) {
		this.xmlDir = xmlDir;
	}

	private class ToolUrlValidator extends WValidator {
		ToolUrlValidator(boolean isMandatory) {
			super(isMandatory);
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			ToolConfig toolConfigByUrl = config.getToolConfigByUrlPath(urlLE.getText());

			if (toolConfigByUrl != null && toolConfigByUrl.getToolMenifest() != null
					&& !toolConfigByUrl.getPath().isEmpty()) {
				if (!toolConfigByUrl.getConfiguration().equals(
						toolConfig.getConfiguration())) 
					return new Result(State.Invalid, "A tool with same url already exist on local server.");
			}

			return super.validate(input);
		}
	}
}
