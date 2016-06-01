package rega.genotype.ui.admin;

import java.util.List;
import java.util.regex.Pattern;

import rega.genotype.config.Config;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.admin.config.GlobalConfigForm;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.config.ToolConfigTable;
import rega.genotype.ui.framework.widgets.Template;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WHBoxLayout;
import eu.webtoolkit.jwt.WMenu;
import eu.webtoolkit.jwt.WStackedWidget;

/**
 * Admin menu
 * @author michael
 */
public class AdminNavigation extends WContainerWidget {	
	public static String URL_PATH_ADMIN = "amdin";
	public static String URL_PATH_TOOLS = "tools";
	public static String URL_PATH_EDIT = "edit";

	public static String URL_PARAM_ID = "id";
	public static String URL_PARAM_VERSION = "version";

	private ToolConfigTable toolConfigTable;
	
	public AdminNavigation(WContainerWidget root) {
		super(root);

	    WStackedWidget contents = new WStackedWidget();
	    WMenu menu = new WMenu(contents);
	    menu.addStyleClass("admin-menu");

	    menu.setInternalPathEnabled();
	    menu.setInternalBasePath("/");

	    // Global config must be created first.
	    if (Settings.getInstance().getConfig() != null) {
	    	final WStackedWidget stack = new WStackedWidget();
	    	toolConfigTable = new ToolConfigTable(stack);
	    	stack.addWidget(toolConfigTable);
	    	menu.addItem("Tools", stack).setPathComponent("tools");

	    	// support url navigation
	    	WApplication.getInstance().internalPathChanged().addListener(
	    			this, new Signal1.Listener<String>() {
	    				public void trigger(String internalPath) {
	    					onInternalPathChanged(internalPath);
	    				}
	    			});
	    	onInternalPathChanged(WApplication.getInstance().getInternalPath());

	    	/*
	    	if (ToolRepoServiceRequests.pingHost()) {
	    		// refresh tool configs state since some tool could have been retracted.
	    		List<ToolManifest> remoteManifests = ToolRepoServiceRequests.getRemoteManifests();
	    		Settings.getInstance().getConfig().refreshToolCofigState(remoteManifests);
	    	}
	    	*/
	    }

	    Config conf = Settings.getInstance().getConfig() == null ? new Config() : Settings.getInstance().getConfig();
	    menu.addItem("Global config", new GlobalConfigForm(conf)).setPathComponent("global");
	    menu.addItem("Help", new Template(tr("admin.help"))).setPathComponent("help");

		WHBoxLayout layout = new WHBoxLayout();
		setLayout(layout);
		layout.addWidget(menu);
		layout.addWidget(contents, 1);
	}

	private void onInternalPathChanged(String internalPath) {
		String path[] =  Pattern.compile("/").
				split(internalPath.length()>1 ? internalPath.substring(1) : internalPath);
		if (path.length >= 1 && path[0].equals("tools")) {//http://localhost:8080/rega-genotype/admin/tools
			if (path.length == 1)
				toolConfigTable.showTable();
			else if (path.length == 4){ 
				String id = path[1];
				String version = path[2];
				String action = path[3];
				if (action.equals(URL_PATH_EDIT)) //http://localhost:8080/rega-genotype/admin/tools/edit/{id}/{version}
					toolConfigTable.showEditTool(id, version, Mode.Edit);
			} else
				toolConfigTable.showTable();
		} else {
			// manu will take care of that.
		}
	}

	public static void setEditToolUrl(String toolId, String toolVersion) {
		String path = "/" + URL_PATH_TOOLS + "/" + toolId + "/" + toolVersion + "/" + URL_PATH_EDIT;
		if (!WApplication.getInstance().getInternalPath().equals(path)) {
			WApplication.getInstance().setInternalPath(path);
			WApplication.getInstance().internalPathChanged().trigger(path);
		}
	}

	public static void setToolsTableUrl() {
		String path = "/" + URL_PATH_TOOLS;
		if (!WApplication.getInstance().getInternalPath().equals(path)) {
			WApplication.getInstance().setInternalPath(path);
			WApplication.getInstance().internalPathChanged().trigger(path);
		}
	}
}
