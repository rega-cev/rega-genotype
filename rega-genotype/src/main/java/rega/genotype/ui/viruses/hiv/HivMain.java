/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WCombinedLocalizedStrings;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * HIV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */

@SuppressWarnings("serial")
public class HivMain extends GenotypeMain {
	@Override
	public WApplication createApplication(WEnvironment env) {
		GenotypeApplication app = new GenotypeApplication(env, this.getServletContext(), settings);
		
		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use("/rega/genotype/ui/viruses/hiv/resources");
		app.setLocalizedStrings(resources);
		
		app.useStyleSheet(new WLink("../style/hiv/genotype.css"));
		
		GenotypeWindow window = new GenotypeWindow(new HivDefinition());
		
		window.addForm("Tutorial", "tutorial", new DocumentationForm(window, tr("tutorial-doc")));
		window.addForm("Decision trees", "decision-trees", new DocumentationForm(window, tr("decision-trees-doc")));
		window.addForm("Subtyping Process", "subtyping-process", new DocumentationForm(window, tr("subtyping-process-doc")));
		window.addForm("Example Sequences", "example-sequences", new DocumentationForm(window, tr("example-sequences-doc")));
		window.addForm("Documentation", "documentation", new DocumentationForm(window, tr("documentation-text")));	
		window.addForm("Contact us", "contact-us", new DocumentationForm(window, tr("contact-us-doc")));
		window.addForm("How to cite", "how-to-cite", new DocumentationForm(window, tr("how-to-cite-doc")));
		
		window.init();

		app.getRoot().addWidget(window);
		
		return app;
	}
	
	private WString tr(String key) {
		return WString.tr(key);
	}
}
