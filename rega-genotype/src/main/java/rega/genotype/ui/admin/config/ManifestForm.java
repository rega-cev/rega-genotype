package rega.genotype.ui.admin.config;

import java.io.File;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.file_editor.blast.TaxonomyWidget;
import rega.genotype.ui.framework.Global;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.StandardDialog;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WDate;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WText;
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
	private final WText taxonomyT = new WText();
	private final WCheckBox blastChB = new WCheckBox();
	private final WCheckBox hivChB = new WCheckBox();
	private Signal1<File> saved = new Signal1<File>(); // The xml dir name may have to change
	private ToolManifest oldManifest;
	private File toolDir; // used for id, version validator.

	public ManifestForm(final ToolManifest manifest, File toolDir, Mode mode) {
		super(tr("admin.config.tool-config-dialog.manifest"));
		this.oldManifest = manifest;
		this.toolDir = toolDir;

		// read

		if (manifest != null) {
			nameLE.setText(manifest.getName());
			idLE.setText(manifest.getId());
			blastChB.setChecked(manifest.isBlastTool());
			hivChB.setChecked(manifest.isHivTool());
			versionLE.setText(manifest.getVersion());
			setTaxonomyIdText(manifest.getTaxonomyId());
		}

		idLE.setDisabled(mode == Mode.NewVersion);

		// validators

		nameLE.setValidator(new WValidator(true));
		idLE.setValidator(new ToolIdValidator(true));
		versionLE.setValidator(new ToolIdValidator(true));

		// info
		
		hivChB.setToolTip("HIV Tool has special functionality and it must be deployed at URL = hiv.");
		
		//style 

		taxonomyT.setStyleClass("hoverable");

		// signals

		taxonomyT.clicked().addListener(taxonomyT, new Signal.Listener() {
			public void trigger() {
				final StandardDialog d = new StandardDialog("Choose taxonomy id");
				final TaxonomyWidget taxonomyWidget = new TaxonomyWidget();
				d.getContents().addWidget(taxonomyWidget);
				d.setWidth(new WLength(600));
				d.setHeight(new WLength(500));
				d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(WDialog.DialogCode arg) {
						if(arg == WDialog.DialogCode.Accepted) {
							setTaxonomyIdText(taxonomyWidget.getSelectedTaxonomyId());
							dirtyHandler.increaseDirty();
						}
					}
				});
			}
		});

		// bind

		bindWidget("name", nameLE);
		bindWidget("id", idLE);
		bindWidget("version", versionLE);
		bindWidget("blast", blastChB);
		bindWidget("hiv", hivChB);
		bindWidget("taxonomy-text", taxonomyT);

		init();
	}

	public ToolManifest save(boolean publishing) {
		if (!validate())
			return null;
			
		Config config = Settings.getInstance().getConfig();

		ToolManifest manifest = new ToolManifest();
		manifest.setHivTool(hivChB.isChecked());
		manifest.setBlastTool(blastChB.isChecked());
		manifest.setName(nameLE.getText());
		manifest.setId(idLE.getText());
		manifest.setVersion(versionLE.getText());
		manifest.setTaxonomyId(taxonomyT.getText().toString());
		manifest.setSoftwareVersion(Global.SOFTWARE_VERSION);
		if (publishing) {
			manifest.setPublicationDate(WDate.getCurrentDate().getDate());
			manifest.setPublisherName(config.getGeneralConfig().getPublisherName());
		} else if (oldManifest != null) {
			manifest.setPublicationDate(oldManifest.getPublicationDate());
			manifest.setPublisherName(oldManifest.getPublisherName());
		}

		if (toolDir == null) { // new tool
			toolDir = new File(Settings.getInstance().getBaseXmlDir(), manifest.getUniqueToolId());
			toolDir.mkdirs();
		}
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
					(toolDir == null // new tool.
					|| !toolConfigById.getConfigurationFile().getAbsolutePath().equals(toolDir.getAbsolutePath())))
				return new Result(State.Invalid, "A tool with same id and version already exist on local server.");

			return super.validate(input);
		}
	}

	private void setTaxonomyIdText(String taxonomyId) {
		if (taxonomyId == null || taxonomyId.isEmpty())
			taxonomyT.setText("(Empty)");
		else
			taxonomyT.setText(taxonomyId);
	}

	public boolean isHivTool() {
		return hivChB.isChecked();
	}

	public File getToolDir() {
		return toolDir;
	}

	public void setToolDir(File toolDir) {
		this.toolDir = toolDir;
	}
}
