package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.Element;

import rega.genotype.Constants.Mode;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.viruses.recombination.RegionUtils;
import rega.genotype.viruses.recombination.RegionUtils.Region;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

public class RecombinationForm extends AbstractForm{
	public static final String URL = "recombination";

	private GenotypeResultParser p;
	
	public RecombinationForm(GenotypeWindow main) {
		super(main);
	}
	
	public static String recombinationPath(File jobDir, int sequenceIndex, String type){
		return AbstractJobOverview.reportPath(jobDir, sequenceIndex) + "/" + URL + "-" + type;
	}
	
	WString init(final File jobDir, Integer selectedSequenceIndex, final String type) {
		this.clear();

		Template t = new Template(tr("recombination-form"), this);
		
		t.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		
		p = new GenotypeResultParser(selectedSequenceIndex);
		p.parseResultFile(jobDir, Mode.Classical);
		
		final String id;
		if (!p.elementExists("/genotype_result/sequence/conclusion")) {
			id = "-";
		} else {
			id = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/id");
		}
		
		final int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		final int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		
		Element recombination = p.getElement("/genotype_result/sequence/result[@id='scan-" + type + "']/recombination");
		final List<Region> regions = RegionUtils.getSupportedRegions(recombination, start);

		final int sequenceIndex = p.getSequenceIndex();

		WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				String typeVirusImage = "0";
				File f = new File(getMain().getOrganismDefinition().getXmlPath()+"/genome_"+p.getValue("/genotype_result/sequence/result[@id='blast']/cluster/concluded-id").replaceAll("\\d", "")+".png");
				try {
					if (f.exists()){
						typeVirusImage = p.getValue("/genotype_result/sequence/result[@id='blast']/cluster/concluded-id").replaceAll("\\d", "").replaceAll("\\d", "");
					}
					if (getFileName().isEmpty()) {
						File file = getMain().getOrganismDefinition().getLargeGenome().getGenomePNG(
								jobDir, sequenceIndex, id, start, end, typeVirusImage, type,
								p.getValue("/genotype_result/sequence/result[@id='scan-" + type + "']/data"),
								regions);
						setFileName(file.getAbsolutePath());
					}
					super.handleRequest(request, response);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}				
		});
		t.bindWidget("recombination-image", genome);
		
		WTable recombinationTable = new WTable();
		if(recombination != null){
			final int perColumn = 2;
			for (Region region : regions) {
				int regionIndex = regions.indexOf(region);
				recombinationTable.getElementAt(regionIndex / perColumn, regionIndex % perColumn)
					.addWidget(createRegionWidget(jobDir, regionIndex, region, start));
			}
		}
		t.bindWidget("recombination-table", recombinationTable);
		
		return null;
	}

	protected WWidget createRegionWidget(final File jobDir, int number, final Region region, int start){
		WContainerWidget container = new WContainerWidget();
		
		container.setStyleClass("region");
		container.addWidget(new WText("( " + (number + 1) + " ) " + region.assignment));
		container.addWidget(new WBreak());
		container.addWidget(new WText("From: " + region.start));
		container.addWidget(new WBreak());
		container.addWidget(new WText("To: " + region.end));
		container.addWidget(new WBreak());
		container.addWidget(new WText("Support: "+ region.support));
		container.addWidget(new WBreak());
		
		container.addWidget(GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				if (getFileName().isEmpty()) {
					File file = GenotypeLib.getTreePNG(jobDir, new File(jobDir.getAbsolutePath() + File.separatorChar + region.tree));
					setFileName(file.getAbsolutePath());
				}
				super.handleRequest(request, response);
			}				
		}));
		
		return container;
	}

	@Override
	public void handleInternalPath(String internalPath) {

	}
}
