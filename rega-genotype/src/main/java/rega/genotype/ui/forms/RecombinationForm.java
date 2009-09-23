package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.Element;

import rega.genotype.ui.data.GenotypeResultParser.SkipToSequenceParser;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

public class RecombinationForm extends AbstractForm{
	public static final String URL = "recombination";
	
	private SkipToSequenceParser p;
	private WContainerWidget recombinationImage;
	private WTable recombinationTable;
	
	public RecombinationForm(GenotypeWindow main) {
		super(main, "recombination-form");
		setStyleClass("recombination-form");
		
		recombinationImage = new WContainerWidget(this);
		recombinationTable = new WTable(this);
	}
	
	public static String recombinationPath(File jobDir, int sequenceIndex){
		return AbstractJobOverview.reportPath(jobDir, sequenceIndex)+"/"+URL;
	}
	
	WString init(File jobDir, String selectedSequenceIndex) {
		recombinationImage.clear();
		recombinationTable.clear();
		
		p = new SkipToSequenceParser(Integer.parseInt(selectedSequenceIndex));
		p.parseFile(jobDir);
		
		String id;
		if (!p.elementExists("/genotype_result/sequence/conclusion")) {
			id = "-";
		} else {
			id = p.getEscapedValue("/genotype_result/sequence/conclusion/assigned/id");
		}
		
		int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		
		WImage genome;
		Element recombination = p.getElement("/genotype_result/sequence/result[@id='scan']/recombination");
		try {
			genome = GenotypeLib.getWImageFromFile(
					getMain().getOrganismDefinition().getLargeGenome().getGenomePNG(
							jobDir, p.getSequenceIndex(), id, start, end, 0, "pure",
							p.getValue("/genotype_result/sequence/result[@id='scan']/data"),
							recombination));
			recombinationImage.addWidget(genome);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(recombination != null){
			List regions = recombination.getChildren("region");
			int perColumn = 2;
			for(int i = 0; i < regions.size(); ++i){
				recombinationTable.getElementAt(i/perColumn, i%perColumn)
					.addWidget(createRegionWidget(jobDir, i, (Element)regions.get(i), start));
			}
		}
		
		return null;
	}
	
	protected WWidget createRegionWidget(File jobDir, int number, Element region, int start){
		WContainerWidget container = new WContainerWidget();
		
		Element result = (Element)region.getChild("result");
		Element best = (Element)result.getChild("best");
		
		container.setStyleClass("region");
		container.addWidget(new WText("( "+ (number+1) +" ) "+ best.getChildText("name")));
		container.addWidget(new WBreak());
		container.addWidget(new WText("From: "+ (start+Integer.parseInt(region.getChildText("start")))));
		container.addWidget(new WBreak());
		container.addWidget(new WText("To: "+ (start+Integer.parseInt(region.getChildText("end")))));
		container.addWidget(new WBreak());
		container.addWidget(new WText("Support: "+ best.getChildText("support")));
		container.addWidget(new WBreak());
		
		container.addWidget(
				GenotypeLib.getWImageFromFile(
						GenotypeLib.getTreePNG(
								jobDir,
								new File(jobDir.getAbsolutePath() + File.separatorChar + result.getChildText("tree")))));
		
		return container;
	}
}
