package rega.genotype.ui.viruses.generic;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import eu.webtoolkit.jwt.FileUtils;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WWidget;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.GenotypeLib;

public class GenericDetailsTemplate extends WTemplate {

	private GenotypeResultParser parser;
	private File jobDir;

	public GenericDetailsTemplate(WString text, GenotypeResultParser p, OrganismDefinition od, File jobDir) {
		super(text);

		this.parser = p;
		this.jobDir = jobDir;
	}	

	@Override
	public WWidget resolveWidget(String varName) {
		return super.resolveWidget(varName);
	}
	
	@Override
	public void resolveString(String varName, List<WString> args, Writer result) throws IOException {
		if (varName.equals("xpath"))
			result.write(GenotypeLib.getEscapedValue(parser, args.get(0).toString()));
		else if (varName.equals("resource")) {
			String resourceName = parser.getValue(args.get(0).toString());
			String s = FileUtils.resourceToString(jobDir + "/" + resourceName);
			result.write(s);
		} else
			super.resolveString(varName, args, result);
	}

//	WContainerWidget block = new WContainerWidget(this);
//	block.setId("");
//
//	WText t = new WText(tr("defaultSequenceAssignment.name-length")
//			.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))
//			.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length")), block);
//	t.setId("");
//
//	String blastConclusion = GenericResults.getBlastConclusion(p);
//	if (!blastConclusion.equals(GenericResults.NA)) {
//		String blastScore = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/score");
//		t = new WText(tr("etvSequenceAssignment.blast").arg(blastConclusion).arg(blastScore), block);
//		t.setId("");
//	}
//
//	GenericResults.Conclusion c = GenericResults.getConclusion(p);
//
//	if (c.majorAssignment == null || c.majorAssignment.isEmpty())
//		c.majorAssignment = NovResults.NA;
//	
//	WString motivation = new WString(c.majorMotivation);
//	motivation.arg(c.majorBootstrap);
//
//	t = new WText(tr("etvSequenceAssignment.phylo")
//			.arg("Serotype (VP1)")
//			.arg(c.majorAssignment)
//			.arg(motivation), block);
//	t.setId("");
//
//	if (c.variantDescription != null) {
//		motivation = new WString(c.variantMotivation);
//		motivation.arg(c.variantBootstrap);
//
//		t = new WText(tr("etvSequenceAssignment.phylo-variant")
//				.arg(c.variantDescription)
//				.arg(motivation), block);
//		t.setId("");
//	}
//
//	t = new WText("<h3>Genome region</h3>", block);
//	t.setId("");
//
//	String startV = p.getValue("/genotype_result/sequence/result[@id='blast']/start");
//	final int start = startV == null ? -1 : Integer.parseInt(startV);
//	String endV = p.getValue("/genotype_result/sequence/result[@id='blast']/end");
//	final int end = endV == null ? -1 : Integer.parseInt(endV);
//	final int sequenceIndex = p.getSequenceIndex();
//
//	WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
//		@Override
//		public void handleRequest(WebRequest request, WebResponse response) {
//			try {
//				if (getFileName().isEmpty()) {
//					File file = od.getGenome().getGenomePNG(jobDir, sequenceIndex, "-", start, end, 0, "etv", null);
//					setFileName(file.getAbsolutePath());
//				}
//
//				super.handleRequest(request, response);
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}				
//	});
//	
//	genome.setId("");
//	block.addWidget(genome);
//
//	if (start > 0 && end > 0) {
//		WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
//		refSeq.arg(start);
//		refSeq.arg(end);
//		refSeq.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/refseq"));
//
//		t = new WText(refSeq, block);
//		t.setId("");
//	}

}
