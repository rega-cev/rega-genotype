package rega.genotype.ui.forms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;

public class DefaultJobOverview extends AbstractJobOverview {
	private List<WMessage> headers = new ArrayList<WMessage>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	public DefaultJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(lt("Name"));
		headers.add(lt("Length"));
		headers.add(lt("Report"));
		headers.add(lt("Assignment"));
		headers.add(lt("Support"));
		headers.add(lt("Genome"));
	}
	
	@Override
	public List<WWidget> getData(final SaxParser p) {
		data.clear();
		
		data.add(new WText(lt(p.getValue("genotype_result.sequence['name']"))));
		data.add(new WText(lt(p.getValue("genotype_result.sequence['length']"))));
		
		WText report = new WText(lt("Report"));
		report.setStyleClass("link");
		report.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				getMain().detailsForm(jobDir, p.getSequenceIndex());
			}
		});
		data.add(report);
		
		String id;
		if(!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
			data.add(new WText(lt("NA")));
			data.add(new WText(lt("NA")));
		} else {
			id = p.getValue("genotype_result.sequence.conclusion.assigned.id");
			data.add(new WText(lt(p.getValue("genotype_result.sequence.conclusion.assigned.name"))));
			
			String support = p.getValue("genotype_result.sequence.conclusion.assigned.support");
			if(support==null) {
				support = "NA";
			}
			data.add(new WText(lt(support)));
			try {
				data.add(GenotypeLib.getWImageFromFile(getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, p.getSequenceIndex(), 
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
