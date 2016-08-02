package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.viruses.hiv.HivMain;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
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
	private final WCheckBox currentVesionChB = new WCheckBox();


	//private ToolManifest manifest;
	private ToolConfig toolConfig;
	private ManifestForm manifestForm;

	public LocalConfigForm(final ToolConfig toolConfig, 
			ManifestForm manifestForm) {
		super(tr("admin.config.tool-config-dialog.config"));
		this.toolConfig = toolConfig;
		this.manifestForm = manifestForm;

		// read

		if (toolConfig != null){
			urlLE.setText(toolConfig.getPath() == null ? "" : toolConfig.getPath());
			autoUpdateChB.setChecked(toolConfig.isAutoUpdate());
			serviceChB.setChecked(toolConfig.isWebService());
			uiChB.setChecked(toolConfig.isUi());
			currentVesionChB.setChecked(toolConfig.isCurrentUsedVersion());
		}

		urlLE.setValidator(new ToolUrlValidator(false));

		// bind

		bindWidget("url", urlLE);
		bindWidget("update", autoUpdateChB);
		bindWidget("ui", uiChB);
		bindWidget("current-version", currentVesionChB);
		bindWidget("service", serviceChB);

		init();
	}

	public ToolConfig save(File toolDir) {
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

		toolConfig.setConfiguration(toolDir.getAbsolutePath() + File.separator);
		toolConfig.setAutoUpdate(autoUpdateChB.isChecked());
		toolConfig.setPath(urlLE.getText());
		toolConfig.setUi(uiChB.isChecked());
		toolConfig.setWebService(serviceChB.isChecked());
		if(toolConfig.getJobDir().isEmpty())
			toolConfig.genetareJobDir();

		toolConfig.setCurrentUsedVersion(currentVesionChB.isChecked());

		// make sure that current version is unique.
		for (ToolConfig c :Settings.getInstance().getConfig().getTools()) {
			ToolManifest m = c.getToolMenifest();
			if (c.isCurrentUsedVersion() && m != null 
					&& m.getId().equals(toolConfig.getId()) 
					&& !m.getVersion().equals(toolConfig.getVersion()))
				c.setCurrentUsedVersion(false);
		}

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

	public ToolConfig getToolConfig() {
		return toolConfig;
	}

	public void setToolConfig(ToolConfig toolConfig) {
		this.toolConfig = toolConfig;
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
						getToolConfig().getConfiguration())) 
					return new Result(State.Invalid, "A tool with same url already exist on local server.");
			}

			if ( manifestForm.isHivTool()
					&& !input.equals(HivMain.HIV_TOOL_ID)
					&& !input.isEmpty()) {
				return new Result(State.Invalid, "HIV tool url must be hiv.");
			}
			return super.validate(input);
		}
	}
}
