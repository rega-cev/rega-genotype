package rega.genotype.ui.admin.config;

import java.io.File;

import rega.genotype.Constants;
import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.NgsModule;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.file_editor.blast.TaxonomyButton;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.ObjectListModel;
import rega.genotype.ui.framework.widgets.StandardDialog;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WDate;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WPushButton;
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
	private final WLineEdit commitLE = initLineEdit();
	private final TaxonomyButton taxonomyT = new TaxonomyButton();
	private final WComboBox toolTypeCB = new WComboBox();
	ObjectListModel<ToolType> toolTypeModel;
	private Signal1<File> saved = new Signal1<File>(); // The xml dir name may have to change
	private ToolManifest oldManifest;
	private File toolDir; // used for id, version validator.
	private Signal1<ToolType> toolTypeChanged = new Signal1<ManifestForm.ToolType>();

	public enum ToolType {VirusTool, PanViralTool, HivTool, Template, Ngs}

	public ManifestForm(final ToolManifest manifest, File toolDir, Mode mode) {
		super(tr("admin.config.tool-config-dialog.manifest"));
		this.oldManifest = manifest;
		this.toolDir = toolDir;

		final WPushButton viewCommitsB = new WPushButton("View changes");

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
					return new WString("Template (for other tools)");
				case Ngs:
					return new WString("NGS Module");
				}
				return null;
			}
		};
		toolTypeCB.setModel(toolTypeModel);

		toolTypeCB.changed().addListener(toolTypeCB, new Signal.Listener() {
			public void trigger() {
				toolTypeChanged.trigger(toolTypeModel.getObject(toolTypeCB.getCurrentIndex()));
			}
		});

		
		// read

		if (manifest != null) {
			nameLE.setText(manifest.getName());
			idLE.setText(manifest.getId());
			toolTypeCB.setCurrentIndex(toolTypeModel.indexOfObject(toolType(manifest)));
			versionLE.setText(manifest.getVersion());
			taxonomyT.setTaxonomyIdText(manifest.getTaxonomyId());
			commitLE.setText(manifest.getCommitMessage());
		}

		idLE.setDisabled(mode == Mode.NewVersion);

		// validators

		nameLE.setValidator(new WValidator(true));
		idLE.setValidator(new ToolIdValidator(true, ValidatorType.ID));
		versionLE.setValidator(new ToolIdValidator(true, ValidatorType.Version));

		// info
		
		//hivChB.setToolTip("HIV Tool has special functionality and it must be deployed at URL = hiv.");
		
		// signal

		taxonomyT.finished().addListener(taxonomyT, new Signal1.Listener<String>() {
			public void trigger(String selectedTaxonomyId) {
				taxonomyT.setTaxonomyIdText(selectedTaxonomyId);
				dirtyHandler.increaseDirty();
			}
		});

		viewCommitsB.clicked().addListener(viewCommitsB, new Signal.Listener() {
			public void trigger() {
				StandardDialog d = new StandardDialog("Changes");
				d.getContents().addWidget(new ChangesView(idLE.getText()));
				d.setResizable(true);
			}
		});

		// bind

		bindWidget("name", nameLE);
		bindWidget("id", idLE);
		bindWidget("version", versionLE);
		bindWidget("commit", commitLE);
		bindWidget("tool-type", toolTypeCB);
		bindWidget("taxonomy-text", taxonomyT);
		bindWidget("view-commits", viewCommitsB);

		init();
	}

	public static ToolType toolType(ToolManifest manifest) {
		if (manifest == null)
			return ToolType.VirusTool;
		else if (manifest.isBlastTool()) 
			return ToolType.PanViralTool;
		else if (manifest.isHivTool())
			return ToolType.HivTool;
		else if (manifest.isTemplate())
			return ToolType.Template;
		else if (manifest.isNgsModule())
			return ToolType.Ngs;
		else 
			return ToolType.VirusTool;
	}

	public ToolManifest save(boolean publishing) {
		if (!validate())
			return null;
			
		Config config = Settings.getInstance().getConfig();

		ToolManifest manifest = new ToolManifest();
		ToolType toolType = toolTypeModel.getObject(toolTypeCB.getCurrentIndex());

		manifest.setNgsModule(toolType == ToolType.Ngs);
		manifest.setHivTool(toolType == ToolType.HivTool);
		manifest.setBlastTool(toolType == ToolType.PanViralTool);
		manifest.setTemplate(toolType == ToolType.Template);
		manifest.setName(nameLE.getText());
		manifest.setId(idLE.getText());
		manifest.setVersion(versionLE.getText());
		manifest.setCommitMessage(commitLE.getText());
		manifest.setTaxonomyId(taxonomyT.getValue());
		manifest.setSoftwareVersion(Constants.SOFTWARE_VERSION);
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

	enum ValidatorType {ID, Version};
	private class ToolIdValidator extends WValidator {
		private ValidatorType validatorType;
		ToolIdValidator(boolean isMandatory, ValidatorType validatorType) {
			super(isMandatory);
			this.validatorType = validatorType;
		}
		@Override
		public Result validate(String input) {
			Config config = Settings.getInstance().getConfig();
			ToolConfig toolConfigById = config.getToolConfigById(idLE.getText(), versionLE.getText());

			if (toolConfigById != null && 
					(toolDir == null // new tool.
					|| !toolConfigById.getConfigurationFile().getAbsolutePath().equals(toolDir.getAbsolutePath())))
				return new Result(State.Invalid, "A tool with same id and version already exist on local server.");

			if (validatorType == ValidatorType.ID
					&& getToolType() == ToolType.Ngs
					&& !idLE.getText().equals(NgsModule.NGS_MODULE_ID)) 
				return new Result(State.Invalid, "NGS Module id must be \"NGS_Module\". Only 1 NGS module can be used in the system.");

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
		return TaxonomyModel.getInstance().getScientificName(taxonomyT.getText().toString());
	}

	public Signal1<ToolType> toolTypeChanged() {
		return toolTypeChanged;
	}

	public ToolType getToolType() {
		return toolTypeModel.getObject(toolTypeCB.getCurrentIndex());
	}
}
