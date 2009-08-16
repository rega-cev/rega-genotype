package rega.genotype.ui.viruses.nrv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.witty.wt.SignalListener;
import net.sf.witty.wt.WMouseEvent;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;

public class NrvJobOverview extends AbstractJobOverview {
	private List<WMessage> headers = new ArrayList<WMessage>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	public NrvJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(lt("Name"));
		headers.add(lt("Length"));
		headers.add(lt("Report"));
		headers.add(lt("ORF 1"));
		headers.add(lt("ORF 2"));
		headers.add(lt("Genome"));
	}
	
	@Override
	public List<WWidget> getData(final SaxParser p) {
		data.clear();
		
		data.add(new WText(lt(p.getValue("genotype_result.sequence[name]"))));
		data.add(new WText(lt(p.getValue("genotype_result.sequence[length]"))));
		
		WText report = new WText(lt("Report"));
		report.setStyleClass("link");
		final int index = p.getSequenceIndex();
		report.clicked.addListener(new SignalListener<WMouseEvent>() {
			public void notify(WMouseEvent a) {
				getMain().detailsForm(jobDir, index);
			}
		});
		data.add(report);

		data.add(new WText(lt(NrvResults.getConclusion(p, "ORF1"))));
		data.add(new WText(lt(NrvResults.getConclusion(p, "ORF2"))));

		try {
			data.add(GenotypeLib.getWImageFromFile(getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, p.getSequenceIndex(), 
					"-",
					Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start")), 
					Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end")),
					0, "", null)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return data;
	}

	@Override
	public List<WMessage> getHeaders() {
		return headers;
	}

}
