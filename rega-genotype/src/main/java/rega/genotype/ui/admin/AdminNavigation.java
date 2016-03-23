package rega.genotype.ui.admin;

import rega.genotype.ui.admin.config.GlobalConfigForm;
import rega.genotype.ui.admin.config.ToolConfigTable;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WHBoxLayout;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WStackedWidget;

/**
 * Admin menu
 * @author michael
 */
public class AdminNavigation extends WContainerWidget {	
	public AdminNavigation(WContainerWidget root) {
		super(root);

	    WStackedWidget contents = new WStackedWidget();
	    WMenu menu = new WMenu(contents);
	    menu.setWidth(new WLength(150));
	    menu.addItem("Tools", new ToolConfigTable(null));
	    menu.addItem("Global config", new GlobalConfigForm());

		WHBoxLayout layout = new WHBoxLayout();
		setLayout(layout);
		layout.addWidget(menu);
		layout.addWidget(contents, 1);
	}
}
