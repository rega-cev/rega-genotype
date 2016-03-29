package rega.genotype.ui.admin;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import rega.genotype.ui.admin.config.GlobalConfigForm;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.config.ToolConfigTable;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WApplication;
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
	public static String URL_PATH_ADMIN = "amdin";
	public static String URL_PATH_TOOLS = "tools";
	public static String URL_PATH_NEW = "new";
	public static String URL_PATH_EDIT = "edit";

	public static String URL_PARAM_ID = "id";
	public static String URL_PARAM_VERSION = "version";

	public AdminNavigation(WContainerWidget root) {
		super(root);

	    WStackedWidget contents = new WStackedWidget();
	    WMenu menu = new WMenu(contents);
	    menu.setWidth(new WLength(150));

	    menu.setInternalPathEnabled();
	    menu.setInternalBasePath("/");

	    // Global config must be created first.
	    if (Settings.getInstance().getConfig() != null) {
	    	final WStackedWidget stack = new WStackedWidget();
	    	final ToolConfigTable toolConfigTable = new ToolConfigTable(stack);
	    	stack.addWidget(toolConfigTable);
	    	menu.addItem("Tools", stack).setPathComponent("tools");

	    	// support url navigation
	    	WApplication.getInstance().internalPathChanged().addListener(
	    			this, new Signal1.Listener<String>() {
	    				public void trigger(String internalPath) {
	    					String path[] =  Pattern.compile("/").
	    							split(internalPath.length()>1 ? internalPath.substring(1) : internalPath);
	    					if (path.length >= 1 && path[0].equals("tools")) {
	    						//http://localhost:8080/rega-genotype/admin/tools
	    						if (path.length == 1)
	    							toolConfigTable.showTable();
	    						//http://localhost:8080/rega-genotype/admin/tools/edit/{id}/{version}
	    						else if (path.length == 4 && path[1].equals(URL_PATH_EDIT)){
	    							String id = path[2];
	    							String version = path[3];
	    							toolConfigTable.showEditTool(id, version, Mode.Edit);
	    							//http://localhost:8080/rega-genotype/admin/tools/new/{id}/{version}
	    						} else if (path.length == 4 && path[1].equals(URL_PATH_NEW)){
	    							String id = path[2];
	    							String version = path[3];
	    							toolConfigTable.showEditTool(id, version, Mode.NewVersion);
	    							//http://localhost:8080/rega-genotype/admin/tools/new
	    						} else if (path.length == 2 && path[1].equals(URL_PATH_NEW)){
	    							toolConfigTable.showCreateNewTool();
	    						} else
	    							toolConfigTable.showTable();
	    					} else {
	    						// manu will take care of that.
	    					}
	    				}
	    			});
	    }

	    menu.addItem("Global config", new GlobalConfigForm()).setPathComponent("global");

		WHBoxLayout layout = new WHBoxLayout();
		setLayout(layout);
		layout.addWidget(menu);
		layout.addWidget(contents, 1);
	}

	public static void setEditToolUrl(String toolId, String toolVersion) {
		String path = "/" + URL_PATH_TOOLS + "/" + URL_PATH_EDIT + "/" + toolId + "/" + toolVersion;
		if (!WApplication.getInstance().getInternalPath().equals(path)) {
			WApplication.getInstance().setInternalPath(path);
			WApplication.getInstance().internalPathChanged().trigger(path);
		}
	}

	public static void setNewVersionToolUrl(String toolId, String toolVersion) {
		String path = "/" + URL_PATH_TOOLS + "/" + URL_PATH_NEW + "/" + toolId + "/" + toolVersion;
		if (!WApplication.getInstance().getInternalPath().equals(path)) {
			WApplication.getInstance().setInternalPath(path);
			WApplication.getInstance().internalPathChanged().trigger(path);
		}
	}

	public static void setNewToolUrl() {
		String path = "/" + URL_PATH_TOOLS + "/" + URL_PATH_NEW;
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
