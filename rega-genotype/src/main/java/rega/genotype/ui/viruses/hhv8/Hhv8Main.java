package rega.genotype.ui.viruses.hhv8;

import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;

/**
 * HCV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */
@SuppressWarnings("serial")
public class Hhv8Main extends GenotypeMain {
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), new Hhv8Definition());

		return app;
	}
}