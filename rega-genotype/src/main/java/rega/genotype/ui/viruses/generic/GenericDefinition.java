/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.tools.blast.BlastTool;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.FileManifest;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.forms.details.DefaultRecombinationDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;
import rega.genotype.ui.util.GenomeAttributes;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.util.DataTable;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WString;

/**
 * Generic OrganismDefinition implementation.
 */
public class GenericDefinition implements OrganismDefinition, GenomeAttributes {
	private Genome genome = new Genome(this);

	public static class MenuItem {
		String label, path, messageId;
	}
	
	public static class ResultColumn {
		String label, field;
		int colSpan;
	}
	
	private List<MenuItem> menuItems = new ArrayList<MenuItem>();
	private int updateInterval;
	private Map<String, Color> colors;
	private int fontSize;
	private int genomeEnd;
	private int genomeImageEndX;
	private int genomeImageStartX;
	private int genomeStart;
	
	private List<ResultColumn> resultColumns = null;
	private List<ResultColumn> downloadColumns = null;
	private ToolConfig toolConfig;
	private List<FileManifest> fileManifests; 
	
	public GenericDefinition(ToolConfig toolConfig) throws JDOMException, IOException {
		this.toolConfig = toolConfig;
		this.updateInterval = 5000;
		colors = new HashMap<String, Color>();
		colors.put("-", new Color(0x53, 0xb8, 0x08));
		fontSize = 8;
		
		/*
		 * Read settings.
		 */
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(getXmlPath() + File.separator + "config.xml");
		Element root = document.getRootElement();
		Element menuE = root.getChild("menu");
		for (Object o : menuE.getChildren()) {
			Element itemE = (Element) o;
			MenuItem item = new MenuItem();
			item.label = itemE.getChildText("label");
			item.messageId = itemE.getChildText("message-id");
			item.path = itemE.getChildText("path");
			menuItems.add(item);
		}

		Element genomeE = root.getChild("genome");
		if (genomeE != null) {
			for (Object o : genomeE.getChildren("color")) {
				Element colorE = (Element) o;
				WColor c = new WColor(colorE.getText());
				String a = colorE.getAttributeValue("assignment");
				colors.put(a, new Color(c.getRed(), c.getGreen(), c.getBlue()));
			}

			genomeStart = Integer.parseInt(genomeE.getChildText("start"));
			genomeEnd = Integer.parseInt(genomeE.getChildText("end"));
			genomeImageStartX = Integer.parseInt(genomeE.getChildText("image-start"));
			genomeImageEndX = Integer.parseInt(genomeE.getChildText("image-end"));
		}

		resultColumns = readColumnList(root, "result-list");
		downloadColumns = readColumnList(root, "result-download");

		fileManifests = ConfigXmlReader.readFileManifests(new File(getXmlPath()));
	}

	private List<ResultColumn> readColumnList(Element root, String tag) {
		List<ResultColumn> columns = null;
		Element resultListE = root.getChild(tag);
		if (resultListE != null) {
			columns = new ArrayList<ResultColumn>();
			for (Object o : resultListE.getChildren("column")) {
				Element columnE = (Element) o;
				
				ResultColumn column = new ResultColumn();
				column.label = columnE.getChildText("label");
				column.field = columnE.getChildText("field");
				String colSpan = columnE.getChildText("colSpan");
				if (colSpan != null)
					column.colSpan = Integer.valueOf(colSpan);
				else
					column.colSpan = 1;
				columns.add(column);
			}
		}
		return columns;
	}

	public void startAnalysis(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
		GenotypeTool tool;
		if (getToolConfig().getToolMenifest().isBlastTool())
			tool = new BlastTool(toolConfig, workingDir);
		else
			tool = new GenericTool(toolConfig, workingDir);

		tool.analyze(workingDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
					 workingDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new GenericJobOverview(main, resultColumns);
	}

	public AbstractDataTableGenerator getDataTableGenerator(SequenceFilter sequenceFilter, DataTable table) throws IOException {
		return new GenericTableGenerator(sequenceFilter, table, downloadColumns);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new GenericSequenceAssignmentForm();
	}

	public String getProfileScanType(GenotypeResultParser p) {
		return null;
	}

	private void addPhyloDetailForms(GenotypeResultParser p, List<IDetailsForm> forms) {
		String result = "/genotype_result/sequence/result";

		List<Element> phyloMajorResults = p.getElements(result + "[@id='phylo-major']");

		for (Element e : phyloMajorResults) {
			String region = e.getAttributeValue("region");

			String regionPredicate;
			String regionDescription;
			if (region != null) {
				regionPredicate = " and @region='" + region + "'";
				regionDescription = region;
			} else {
				regionPredicate = " and not(@region)";
				regionDescription = "complete genome";
			}

			String phyloResult = result + "[@id='phylo-major'" + regionPredicate + "]";

			WString title = WString.tr("details.phylo-major.title").arg(regionDescription);
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));
	
			String bestGenotype = GenotypeLib.getEscapedValue(p, phyloResult + "/best/id");

			String variantResult = result + "[@id='phylo-minor-" + bestGenotype + "'" + regionPredicate + "]";
			if (p.elementExists(variantResult)) {
				WString variantTitle = WString.tr("details.phylo-minor.title").arg(bestGenotype).arg(regionDescription);
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult, variantTitle, variantTitle, true));
			}
				
			String scanResult = result + "[@id='phylo-major-scan'" + regionPredicate + "]";
			if (p.elementExists(scanResult)) {
				title = WString.tr("details.phylo-major-scan.title").arg(regionDescription);
				forms.add(new DefaultRecombinationDetailsForm(scanResult, "major", title));
			}
		}
	}
	
	public List<IDetailsForm> getSupportingDetailsforms(GenotypeResultParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();
		addPhyloDetailForms(p, forms);
		return forms;
	}
	
	public int getUpdateInterval(){
		return updateInterval;
	}

	public boolean haveDetailsNavigationForm() {
		return false;
	}

	public Genome getLargeGenome() {
		return getGenome();
	}

	public Map<String, Color> getColors() {
		return colors;
	}

	public int getFontSize() {
		return fontSize;
	}

	public int getGenomeEnd() {
		return genomeEnd;
	}

	public int getGenomeImageEndX() {
		return genomeImageEndX;
	}

	public int getGenomeImageEndY() {
		return 0;
	}

	public int getGenomeImageStartX() {
		return genomeImageStartX;
	}

	public int getGenomeImageStartY() {
		return 0;
	}

	public int getGenomeStart() {
		return genomeStart;
	}

	public OrganismDefinition getOrganismDefinition() {
		return this;
	}

	public List<MenuItem> getMenuItems() {
		return menuItems;
	}

	public List<String> getRecombinationResultXPaths() {
		return null;
	}

	public String getJobDir() {
		return toolConfig.getJobDir();
	}

	public String getXmlPath() {
		return toolConfig.getConfiguration();
	}

	public ToolConfig getToolConfig() {
		return toolConfig;
	}

	public List<FileManifest> getFileManifests() {
		return fileManifests;
	}

	public void setFileManifests(List<FileManifest> fileManifests) {
		this.fileManifests = fileManifests;
	}
}
