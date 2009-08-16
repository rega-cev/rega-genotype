package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.i8n.WArgMessage;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.recombination.RecombinationPlot;
import rega.genotype.ui.util.GenotypeLib;

public class DefaultRecombinationDetailsForm extends IDetailsForm {

	public DefaultRecombinationDetailsForm(){
		super();
		setStyleClass("recombinationDetails");
	}
	@Override
	public void fillForm(SaxParser p, OrganismDefinition od, File jobDir) {
		try {
			if(p.elementExists("genotype_result.sequence.result[scan]")) {
				initRecombinationSection(p, jobDir, "genotype_result.sequence.result[scan]", "pure", od);
			} else if(p.elementExists("genotype_result.sequence.result[crfscan]")) {
				initRecombinationSection(p, jobDir, "genotype_result.sequence.result[crfscan]", "crf", od);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initRecombinationSection(SaxParser p, File jobDir, String path, String type, OrganismDefinition od) throws UnsupportedEncodingException, IOException {
		addWidget(new WText(tr("defaultRecombinationAnalyses.sequenceName")));
		addWidget(new WText(lt(p.getValue("genotype_result.sequence['name']"))));
		addWidget(new WBreak());
		addWidget(GenotypeLib.getWImageFromFile(RecombinationPlot.getRecombinationPNG(jobDir, p.getSequenceIndex(), type, p.getValue(path+".data"), od)));
		addWidget(new WBreak());
		addWidget(new WText(tr("defaultRecombinationAnalyses.bootscanClusterSupport")));
		addWidget(new WText(lt(p.getValue(path+".support[best]"))));
		addWidget(new WText(tr("defaultRecombinationAnalyses.download")));
		addWidget(GenotypeLib.getAnchor("CSV", "application/excel", RecombinationPlot.getRecombinationCSV(jobDir, p.getSequenceIndex(), type, p.getValue(path+".data"))));
		addWidget(new WText(tr("defaultRecombinationAnalyses.download")));
		addWidget(GenotypeLib.getAnchor("PDF", "application/pdf", RecombinationPlot.getRecombinationPDF(jobDir, p.getSequenceIndex(), type, p.getValue(path+".data"), od)));
		addWidget(new WBreak());
		WArgMessage m = new WArgMessage("defaultRecombinationAnalyses.bootscanAnalysis");
		m.addArgument("${window}", p.getValue(path+".window"));
		m.addArgument("${step}", p.getValue(path+".step"));
		addWidget(new WText(m));
	}
	
	@Override
	public WMessage getComment() {
		return tr("defaultRecombinationAnalyses.comment");
	}

	@Override
	public WMessage getTitle() {
		return tr("defaultRecombinationAnalyses.title");
	}

	@Override
	public WMessage getExtraComment() {
		return null;
	}
}
