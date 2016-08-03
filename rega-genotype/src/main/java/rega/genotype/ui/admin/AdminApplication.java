package rega.genotype.ui.admin;

import rega.genotype.config.Config;
import rega.genotype.singletons.Settings;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WCssTheme;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTable;
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

		useStyleSheet(new WLink("style/admin.css"));

		setTheme(new WCssTheme("polished"));

		// auth
		
		final Config config = Settings.getInstance().getConfig();
		
		WTable logo = new WTable(getRoot());
		WText logoT = new WText("<h2>Typing Tools Manager</h2>", logo.getElementAt(0, 0));
		logoT.addStyleClass("admin-logo-text");
		WImage img = new WImage(new WLink(WLink.Type.Url, "./pics/dna_long.png"), logo.getElementAt(0, 1));
		img.addStyleClass("admin-logo");
		
		if (config != null 
				&& config.getGeneralConfig().getAdminPassword() != null
				&& !config.getGeneralConfig().getAdminPassword().equals("pwd-for-lazy-developerS*!") ) {
			
			// very simple auth.
			final WContainerWidget loginC = new WContainerWidget(getRoot());
			new WText("Enter admin password", loginC);
			final WLineEdit pwdLE = new WLineEdit(loginC);
			final WPushButton loginB = new WPushButton("Login", loginC);
			final WText infoT = new WText(loginC);

			pwdLE.setAttributeValue("type", "password");
			pwdLE.setMargin(5, Side.Left);
			pwdLE.setFocus();
			pwdLE.setPlaceholderText("Admin password");

			loginB.clicked().addListener(loginB, new Signal.Listener() {
				public void trigger() {
					if (config.getGeneralConfig().getAdminPassword().equals(
							pwdLE.getText())){
						getRoot().removeWidget(loginC);
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