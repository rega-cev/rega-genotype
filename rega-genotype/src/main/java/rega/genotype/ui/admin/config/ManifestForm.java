package rega.genotype.ui.admin.config;

import java.io.File;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.ui.framework.Global;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WDate;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WValidator;

/**
 * Form for editing tool manifest (tool name and signature).
 * 
 * @author michael
 */
public class ManifestForm extends FormTemplate{
	private final WLineEdit nameLE = initLineEdit();
	private final WLineEdit idLE = initLineEdit();
	private final WLineEdit versionLE = initLineEdit();
	private final WCheckBox blastChB = new WCheckBox();
	private final WPushButton saveB = new WPushButton("save");
	private Signal1<File> saved = new Signal1<File>(); // The xml dir name may have to change
	private ToolManifest oldManifest;
	private File toolDir;

	public ManifestForm(final ToolManifest manifest, File toolDir) {
		super(tr("admin.config.tool-config-dialog.manifest"));
		this.oldManifest = manifest;
		this.toolDir = toolDir;

		// read

		if (manifest != null) {
			nameLE.setText(manifest.getName());
			idLE.setText(manifest.getId());
			blastChB.setChecked(manifest.isBlastTool());
			versionLE.setText(manifest.getVersion());
		}

		// validators

		nameLE.setValidator(new WValidator(true));
		idLE.setValidator(new ToolIdValidator(true));
		versionLE.setValidator(new ToolIdValidator(true));

		// bind

		bindWidget("name", nameLE);
		bindWidget("id", idLE);
		bindWidget("version", versionLE);
		bindWidget("blast", blastChB);
		bindWidget("save", saveB);

		initInfoFields();
		validate();

		// signals

		saveB.clicked().addListener(saveB, new Signal.Listener() {
			public void trigger() {
				if (save(false) != null)
					new MsgDialog("Info", "Manifest saved.");
				else
					new MsgDialog("Error", "Could not save manifest.");
			}
		});
	}

	public ToolManifest save(boolean publishing) {
		if (!validate())
			return null;
			
		Config config = Settings.getInstance().getConfig();

		ToolManifest manifest = new ToolManifest();
		manifest.setBlastTool(blastChB.isChecked());
		manifest.setName(nameLE.getText());
		manifest.setId(idLE.getText());
		manifest.setVersion(versionLE.getText());
		manifest.setPublisherName(config.getGeneralConfig().getPublisherName());
		manifest.setSoftwareVersion(Global.SOFTWARE_VERSION);
		if (publishing)
			manifest.setPublicationDate(WDate.getCurrentDate().getDate());
		else if (oldManifest != null)
			manifest.setPublicationDate(oldManifest.getPublicationDate());

		manifest.save(toolDir.getAbsolutePath());

		saved().trigger(toolDir);
		
		return manifest;
	}

	private WLineEdit initLineEdit() {
		WLineEdit le = new WLineEdit();
		le.setWidth(new WLength(200));
		return le;
	}

	public Signal1<File> saved() {
		return saved;
	}

	public ToolManifest getOldManifest() {
		return oldManifest;
	}

	private class ToolIdValidator extends WValidator {
		ToolIdValidator(boolean isMandatory) {
			super(isMandatory);
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			ToolConfig toolConfigById = config.getToolConfigById(idLE.getText(), versionLE.getText());
			
			if (toolConfigById != null && 
					!toolConfigById.getConfigurationFile().getAbsolutePath().equals(toolDir.getAbsolutePath()))
				return new Result(State.Invalid, "A tool with same id and version already exist on local server.");

			return super.validate(input);
		}
	}

}
