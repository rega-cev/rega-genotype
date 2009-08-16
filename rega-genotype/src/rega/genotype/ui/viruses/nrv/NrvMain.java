package rega.genotype.ui.viruses.nrv;

import net.sf.witty.wt.WApplication;
import net.sf.witty.wt.WEnvironment;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;

public class NrvMain extends GenotypeMain {
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new NrvDefinition());

		return app;
	}
}
