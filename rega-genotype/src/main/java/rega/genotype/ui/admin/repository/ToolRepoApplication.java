package rega.genotype.ui.admin.repository;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * WApplication for a repository of all published Typingtools
 * 
 * @author michael
 */
public class ToolRepoApplication extends WApplication{
	public static final String REPOSITORY_BASE_PATH = "repository";
	
	public ToolRepoApplication(WEnvironment env) {
		super(env);
		
		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		setLocalizedStrings(resources);

//		useStyleSheet(new WLink("../../style/genotype-rivm.css"));
//		useStyleSheet(new WLink("../../style/genotype-rivm-ie.css"), "IE lte 7");
		
		// For now it is simple.
		new ToolRepoTable(getRoot());
	}

	public static ToolRepoApplication getAppInstance() {
		return (ToolRepoApplication) WApplication.getInstance();
	}
}