package rega.genotype.ui.framework;

import eu.webtoolkit.jwt.WLocalizedStrings;

public class RegaLocalizedString extends WLocalizedStrings{
	private GenotypeApplication app;
	public RegaLocalizedString() {
		app = GenotypeApplication.getGenotypeApplication();

	}
	@Override
	public String resolveKey(String key) {
		if (key.equals("sequenceInput.example"))
			System.err.println();

		if(app != null && key.equals("global-variable.tool-name")) 
			return app.getToolConfig().getToolMenifest().getName();
		else if(app != null && key.equals("global-variable.tool-version")) 
			return app.getToolConfig().getToolMenifest().getVersion();
		else if(app != null && key.equals("global-variable.virus-name")) 
			return app.getToolConfig().getToolMenifest().getTaxonomyId();
		else
			return null;
	}

}
