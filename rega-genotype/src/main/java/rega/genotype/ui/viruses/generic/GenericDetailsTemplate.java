package rega.genotype.ui.viruses.generic;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import eu.webtoolkit.jwt.FileUtils;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WTemplate;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.GenotypeLib;

public class GenericDetailsTemplate extends WTemplate {

	private GenotypeResultParser parser;
	private File jobDir;
	private OrganismDefinition od;

	public GenericDetailsTemplate(WString text, GenotypeResultParser p, OrganismDefinition od, File jobDir) {
		super(text);

		this.parser = p;
		this.od = od;
		this.jobDir = jobDir;
	}	

	@Override
	public WWidget resolveWidget(String varName) {
		if (varName.equals("genome-img")) {
			WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
				@Override
				public void handleRequest(WebRequest request, WebResponse response) {
					String typeVirusImage = "0";
					File f = new File(od.getXmlPath()+"/genome_"+parser.getValue("/genotype_result/sequence/result[@id='blast']/cluster/concluded-id").replaceAll("\\d", "")+".png");
					try {
						if (f.exists()){
							typeVirusImage = parser.getValue("/genotype_result/sequence/result[@id='blast']/cluster/concluded-id").replaceAll("\\d", "").replaceAll("\\d", "");
						}
						if (getFileName().isEmpty()) {
							String startV = parser.getValue("/genotype_result/sequence/result[@id='blast']/start");
							int start = startV == null ? -1 : Integer.parseInt(startV);
							String endV = parser.getValue("/genotype_result/sequence/result[@id='blast']/end");
							int end = endV == null ? -1 : Integer.parseInt(endV);
							int sequenceIndex = parser.getSequenceIndex();
							File file = od.getGenome().getGenomePNG(jobDir, sequenceIndex, "-", start, end, typeVirusImage, "etv", null);
							setFileName(file.getAbsolutePath());
						}

						super.handleRequest(request, response);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}				
			});
	
			genome.setId("");
			bindWidget(varName, genome);
		}

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
		} else if (varName.equals("eval-xpath")) {
			String value = parser.getValue(args.get(0).toString());
			setCondition("if-defined", value != null);
			setCondition("if-not-defined", value == null);
		} else
			super.resolveString(varName, args, result);
	}
}
