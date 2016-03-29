package rega.genotype.ui.admin;

import rega.genotype.config.Config;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WXmlLocalizedStrings;

/**
 * Determine what widget to show in admin area.
 * 
 * @author michael
 */
public class AdminApplication extends WApplication{
	public static final String ADMIN_BASE_PATH = "admin";
	
	public AdminApplication(WEnvironment env) {
		super(env);
		
		WXmlLocalizedStrings resources = new WXmlLocalizedStrings();
		resources.use("/rega/genotype/ui/i18n/resources/common_resources");
		setLocalizedStrings(resources);

		useStyleSheet(new WLink("../../style/genotype-rivm.css"));
		useStyleSheet(new WLink("../../style/genotype-rivm-ie.css"), "IE lte 7");
		
		// auth
		
		final Config config = Settings.getInstance().getConfig();
		
		if (config != null 
				&& config.getGeneralConfig().getAdminPassword() != null) {
			
			// very simple auth.
			
			final WLineEdit pwdLE = new WLineEdit(getRoot());
			final WPushButton loginB = new WPushButton("Login", getRoot());
			final WText infoT = new WText(getRoot());

			pwdLE.setPlaceholderText("Admin password");

			loginB.clicked().addListener(loginB, new Signal.Listener() {
				public void trigger() {
					if (config.getGeneralConfig().getAdminPassword().equals(
							pwdLE.getText())){
						getRoot().clear();
						new AdminNavigation(getRoot());
					} else {
						infoT.setText("Wrong password.");
					}
				}
			});
		} else {
			// init pwd
			new AdminNavigation(getRoot());
		}
	}

	public static AdminApplication getAppInstance() {
		return (AdminApplication) WApplication.getInstance();
	}
}