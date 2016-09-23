package rega.genotype.ui.framework;

import rega.genotype.config.XmlPlaceholders;
import eu.webtoolkit.jwt.WLocalizedStrings;

public class RegaLocalizedString extends WLocalizedStrings{
	private XmlPlaceholders placeHolders;
	private GenotypeApplication app;
	public RegaLocalizedString() {
		app = GenotypeApplication.getGenotypeApplication();
		if (app != null){
			placeHolders = XmlPlaceholders.read(app.getToolConfig().getConfigurationFile());
		}

	}
	@Override
	public String resolveKey(String key) {
		if (key.equals("sequenceInput.example"))
			System.err.println();
		if (placeHolders != null && placeHolders.getPlaceholders().containsKey(key))
			return placeHolders.getPlaceholders().get(key).getValue();
		else if(app != null && key.equals("global-variable.tool-name")) 
			return app.getToolConfig().getToolMenifest().getName();
		else if(app != null && key.equals("global-variable.tool-version")) 
			return app.getToolConfig().getToolMenifest().getVersion();
		else if(app != null && key.equals("global-variable.virus-name")) 
			return app.getToolConfig().getToolMenifest().getTaxonomyId();

		else
			return null;
	}

}
