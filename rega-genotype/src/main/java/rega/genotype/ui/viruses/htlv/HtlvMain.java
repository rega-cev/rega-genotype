package rega.genotype.ui.viruses.htlv;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.viruses.hiv.HivDefinition;

public class HtlvMain extends GenotypeMain {


	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new HtlvDefinition());
		return app;
	}

}
