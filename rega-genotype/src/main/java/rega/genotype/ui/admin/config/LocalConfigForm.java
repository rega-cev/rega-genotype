package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.utils.Settings;
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
	//private ToolManifest manifest;
	private Mode mode;
	private ToolConfig toolConfig;

	public LocalConfigForm(final ToolConfig toolConfig, Mode mode) {
		super(tr("admin.config.tool-config-dialog.config"));
		this.toolConfig = toolConfig;
		this.mode = mode;

		// read

		if (toolConfig != null){
			urlLE.setText(toolConfig.getPath());
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

		initInfoFields();
		validate();
	}

	public ToolConfig save(final ToolManifest manifest) {
		Config config = Settings.getInstance().getConfig();

		// save ToolConfig
		ToolConfig newTool = config.getToolConfigById(manifest.getId(), manifest.getVersion());
		if (newTool == null) {
			assert(mode != Mode.Edit);
			newTool = new ToolConfig();
			if (!config.addTool(newTool))
				return null;
		}

		String xmlDir = Settings.getInstance().getXmlDir(
				manifest.getId(), manifest.getVersion());
		String jobDir = Settings.getInstance().getJobDir(
				manifest.getId(), manifest.getVersion());

		newTool.setAutoUpdate(autoUpdateChB.isChecked());
		newTool.setConfiguration(xmlDir);
		newTool.setJobDir(jobDir);
		newTool.setPath(urlLE.getText());
		newTool.setUi(uiChB.isChecked());
		newTool.setWebService(serviceChB.isChecked());

		try {
			manifest.save(xmlDir);
			config.save(Settings.getInstance().getBaseDir() + File.separator);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return newTool;
	}


	private WLineEdit initLineEdit() {
		WLineEdit le = new WLineEdit();
		le.setWidth(new WLength(200));
		return le;
	}

	private class ToolUrlValidator extends WValidator {
		ToolUrlValidator(boolean isMandatory) {
			super(isMandatory);
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			ToolConfig toolConfigByUrl = config.getToolConfigByUrlPath(urlLE.getText());

			if (toolConfigByUrl != null && toolConfigByUrl.getToolMenifest() != null) {
				if (mode == Mode.NewVersion || mode == Mode.Add)
					// new tool check all urls
					return new Result(State.Invalid, "A tool with same url already exist on local server.");
				else { // Edit
					// TODO: can manifest change ?? 
					// existing tool the url can stay the same. 
					if (!toolConfigByUrl.getToolMenifest().isSameSignature(
							toolConfig.getToolMenifest())) 
						return new Result(State.Invalid, "A tool with same url already exist on local server.");
				}
			}

			return super.validate(input);
		}
	}
}
