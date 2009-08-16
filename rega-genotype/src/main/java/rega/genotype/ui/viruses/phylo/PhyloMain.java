package rega.genotype.ui.viruses.phylo;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.viruses.htlv.HtlvDefinition;

public class PhyloMain extends GenotypeMain {

	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new PhyloDefinition());
		return app;
	}

}
