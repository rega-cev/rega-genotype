/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;
import rega.genotype.ui.util.GenomeAttributes;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Settings;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WString;

/**
 * Generic OrganismDefinition implementation.
 */
public class GenericDefinition implements OrganismDefinition, GenomeAttributes {
	private Genome genome = new Genome(this);
	private String organism;
	private String xmlFolder;

	public static class MenuItem {
		String label, path, messageId;
	}
	
	public static class ResultColumn {
		String label, field;
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
	
	public GenericDefinition(String organism) {
		this.organism = organism;
		this.updateInterval = 5;
		xmlFolder = Settings.getInstance().getXmlPath() + File.separator + organism + File.separator;
		colors = new HashMap<String, Color>();
		colors.put("-", new Color(0x53, 0xb8, 0x08));
		fontSize = 8;
		
		/*
		 * Read settings.
		 */
		try {
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(xmlFolder + File.separator + "config.xml");
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
			
			Element resultListE = root.getChild("result-list");
			if (resultListE != null) {
				resultColumns = new ArrayList<ResultColumn>();
				for (Object o : resultListE.getChildren("column")) {
					Element columnE = (Element) o;
					
					ResultColumn column = new ResultColumn();
					column.label = columnE.getChildText("label");
					column.field = columnE.getChildText("field");
					resultColumns.add(column);
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		GenericTool tool = new GenericTool(organism, jobDir);
		tool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
					 jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new GenericJobOverview(main, resultColumns);
	}
	
	public String getOrganismDirectory() {
		return getXmlFolder();
	}

	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable table) throws IOException {
		return new GenericTableGenerator(jobOverview, table);
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
		
		String phyloResult = result + "[@id='phylo-major']";
		if (p.elementExists(phyloResult)) {
			WString title = WString.tr("details.phylo-major.title");
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));

			String bestGenotype = GenotypeLib.getEscapedValue(p, phyloResult + "/best/id");
			
			String variantResult = result + "[@id='phylo-minor-" + bestGenotype + "']";
			if (p.elementExists(variantResult)) {
				WString variantTitle = WString.tr("details.phylo-minor.title").arg(bestGenotype);
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult, variantTitle, variantTitle, true));
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

	public String getOrganismName() {
		return organism;
	}

	public boolean haveDetailsNavigationForm() {
		return false;
	}

	public Genome getLargeGenome() {
		return getGenome();
	}

	public String getXmlFolder() {
		return xmlFolder;
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
}
