package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.witty.wt.WText;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.ui.util.Genome;

public class DefaultJobOverview extends AbstractJobOverview {
	private List<WMessage> headers = new ArrayList<WMessage>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	private Genome genome;
	
	public DefaultJobOverview(File jobDir, GenotypeResourceManager rm, Genome genome) {
		super(jobDir, rm);
		
		this.genome = genome;
		
		headers.add(lt("Name"));
		headers.add(lt("Length"));
		headers.add(lt("Report"));
		headers.add(lt("Assignment"));
		headers.add(lt("Support"));
		headers.add(lt("Genome"));
	}
	
	@Override
	public List<WWidget> getData(SaxParser p) {
		data.clear();
		
		data.add(new WText(lt(p.getValue("genotype_result.sequence['name']"))));
		data.add(new WText(lt(p.getValue("genotype_result.sequence['length']"))));
		String id;
		if(!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
			data.add(new WText(lt("Sequence error")));
			data.add(new WText(lt("NA")));
			data.add(new WText(lt("NA")));
		} else {
			id = p.getValue("genotype_result.sequence.conclusion.assigned.id");
			data.add(new WText(lt(id)));
			data.add(new WText(lt(p.getValue("genotype_result.sequence.conclusion.assigned.name"))));
			
			String support = p.getValue("genotype_result.sequence.conclusion.assigned.support");
			if(support==null) {
				support = "NA";
			}
			data.add(new WText(lt(support)));
			try {
				data.add(this.getWImageFromFile(genome.getSmallGenomePNG(jobDir, p.getSequenceIndex(), 
						id,
						Integer.parseInt(p.getValue("genotype_result.sequence.result[blast].start")), 
						Integer.parseInt(p.getValue("genotype_result.sequence.result[blast].end")),
						0, 
						"pure", 
						p.getValue("genotype_result.sequence.result[scan].data"))));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return data;
	}

	@Override
	public List<WMessage> getHeaders() {
		return headers;
	}

}
