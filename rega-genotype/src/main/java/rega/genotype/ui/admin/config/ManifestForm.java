package rega.genotype.ui.admin.config;

import java.io.File;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.file_editor.blast.TaxonomyButton;
import rega.genotype.ui.framework.Global;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.ObjectListModel;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WDate;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WString;
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
	private final TaxonomyButton taxonomyT = new TaxonomyButton();
	private final WComboBox toolTypeCB = new WComboBox();
	ObjectListModel<ToolType> toolTypeModel;
	private Signal1<File> saved = new Signal1<File>(); // The xml dir name may have to change
	private ToolManifest oldManifest;
	private File toolDir; // used for id, version validator.
	
	public enum ToolType {VirusTool, PanViralTool, HivTool, Template}

	public ManifestForm(final ToolManifest manifest, File toolDir, Mode mode) {
		super(tr("admin.config.tool-config-dialog.manifest"));
		this.oldManifest = manifest;
		this.toolDir = toolDir;

		toolTypeModel = new ObjectListModel<ManifestForm.ToolType>(ToolType.values()) {
			@Override
			public WString render(ToolType t) {
				switch (t) {
				case HivTool:
					return new WString("HIV tool");
				case PanViralTool:
					return new WString("Pan-viral tool");
				case VirusTool:
					return new WString("Virus tool (standard)");
				case Template:
					return new WString("Template (contains data to auto create other tools)");
				}
				return null;
			}
		};
		toolTypeCB.setModel(toolTypeModel);
		
		// read

		if (manifest != null) {
			nameLE.setText(manifest.getName());
			idLE.setText(manifest.getId());
			if (manifest.isBlastTool()) 
				toolTypeCB.setCurrentIndex(toolTypeModel.indexOfObject(ToolType.PanViralTool));
			else if (manifest.isHivTool())
				toolTypeCB.setCurrentIndex(toolTypeModel.indexOfObject(ToolType.HivTool));
			else if (manifest.isTemplate())
				toolTypeCB.setCurrentIndex(toolTypeModel.indexOfObject(ToolType.Template));
			else 
				toolTypeCB.setCurrentIndex(toolTypeModel.indexOfObject(ToolType.VirusTool));
			versionLE.setText(manifest.getVersion());
			taxonomyT.setTaxonomyIdText(manifest.getTaxonomyId());
		}

		idLE.setDisabled(mode == Mode.NewVersion);

		// validators

		nameLE.setValidator(new WValidator(true));
		idLE.setValidator(new ToolIdValidator(true));
		versionLE.setValidator(new ToolIdValidator(true));

		// info
		
		//hivChB.setToolTip("HIV Tool has special functionality and it must be deployed at URL = hiv.");
		
		// signal

		taxonomyT.finished().addListener(taxonomyT, new Signal1.Listener<String>() {
			public void trigger(String selectedTaxonomyId) {
				taxonomyT.setTaxonomyIdText(selectedTaxonomyId);
				dirtyHandler.increaseDirty();
			}
		});

		// bind

		bindWidget("name", nameLE);
		bindWidget("id", idLE);
		bindWidget("version", versionLE);
		bindWidget("tool-type", toolTypeCB);
		bindWidget("taxonomy-text", taxonomyT);

		init();
	}

	public ToolManifest save(boolean publishing) {
		if (!validate())
			return null;
			
		Config config = Settings.getInstance().getConfig();

		ToolManifest manifest = new ToolManifest();
		ToolType toolType = toolTypeModel.getObject(toolTypeCB.getCurrentIndex());

		manifest.setHivTool(toolType == ToolType.HivTool);
		manifest.setBlastTool(toolType == ToolType.PanViralTool);
		manifest.setTemplate(toolType == ToolType.Template);
		manifest.setName(nameLE.getText());
		manifest.setId(idLE.getText());
		manifest.setVersion(versionLE.getText());
		manifest.setTaxonomyId(taxonomyT.getValue());
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

	public boolean isHivTool() {
		ToolType toolType = toolTypeModel.getObject(toolTypeCB.getCurrentIndex());
		return toolType == ToolType.HivTool;
	}

	public File getToolDir() {
		return toolDir;
	}

	public void setToolDir(File toolDir) {
		this.toolDir = toolDir;
	}

	public String getScientificName() {
		return TaxonomyModel.getScientificName(taxonomyT.getText().toString());
	}
}
