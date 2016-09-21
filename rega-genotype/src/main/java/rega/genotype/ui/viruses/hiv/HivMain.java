/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.JDOMException;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.FileManifest;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.FileManifest.FileType;
import rega.genotype.ui.forms.DocumentationForm;
import rega.genotype.ui.framework.GenotypeApplication;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.exeptions.RegaGenotypeExeption;
import rega.genotype.ui.util.FileServlet;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * HIV implementation of the genotype application.
 * 
 * @author simbre1
 *
 */

@SuppressWarnings("serial")
public class HivMain extends GenotypeMain {
	public static final String HIV_TOOL_ID = "hiv"; // TODO: Temporary till hiv becomes generic tool.

	@Override
	public WApplication createApplication(WEnvironment env) {
		
		if (getSettings().getConfig().getToolConfigByUrlPath(HIV_TOOL_ID) == null) {
			WApplication app = new WApplication(env);
			app.getRoot().addWidget(
					new WText("Typing tool for " + HIV_TOOL_ID
							+ " was not found."));
			return app;
		}

		GenotypeApplication app;
		try {
			app = new GenotypeApplication(env, this.getServletContext(), getSettings(), HIV_TOOL_ID);
		} catch (RegaGenotypeExeption e) {
			e.printStackTrace();
			WApplication a = new WApplication(env);
			a.getRoot().addWidget(new WText(e.getMessage()));
			return a;
		}

		ToolConfig toolConfig = getSettings().getConfig().getToolConfigByUrlPath(HIV_TOOL_ID);
		
		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		resources.use(toolConfig.getConfiguration() + File.separator + "resources");
		app.setLocalizedStrings(resources);

		List<FileManifest> fileManifests;
		try {
			fileManifests = ConfigXmlReader.readFileManifests(new File(toolConfig.getConfiguration()));
		} catch (JDOMException e) {
			e.printStackTrace();
			fileManifests = new ArrayList<FileManifest>();
		} catch (IOException e) {
			//e.printStackTrace(); use default css.
			fileManifests = new ArrayList<FileManifest>();
		}
		if (!fileManifests.isEmpty()) {
			for (FileManifest fm: fileManifests) {
				WLink link = new WLink(FileServlet.getFileUrl(toolConfig.getPath()) 
						+ "/" + fm.getFileName());
				if (fm.getFileType() == FileType.CSS_IE)
					app.useStyleSheet(link, "IE lte 7");
				else
					app.useStyleSheet(link);
			}
		} else { // support old tools
			app.useStyleSheet(new WLink("../style/hiv/genotype.css"));
		
			app.useStyleSheet(new WLink("../style/wt.css"));               // do not use Wt's inline stylesheet...
			app.useStyleSheet(new WLink("../style/wt_ie.css"), "IE lt 7"); // do not use Wt's inline stylesheet...
		}
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
