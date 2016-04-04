package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.ui.admin.AdminNavigation;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolConfigTableModelSortProxy;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolInfo;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolState;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.CheckState;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.SortOrder;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WText;

/**
 * Show list of tools that can be edited with ToolConfigDialog.
 * 
 * @author michael
 */
public class ToolConfigTable extends Template{
	private ToolConfigTableModelSortProxy proxyModel;
	private WText infoT = new WText();
	private WStackedWidget stack;
	
	public ToolConfigTable(WStackedWidget stack) {
		super(tr("admin.config.tool-config-table"));
		this.stack = stack;

		bindWidget("info", infoT);
		
		final WCheckBox versionChB = new WCheckBox("Show latest versions");
		
		List<ToolManifest> remoteManifests = getRemoteManifests();

		// get local tools
		List<ToolManifest> localManifests = getLocalManifests();
		
		// create table
		ToolConfigTableModel model = new ToolConfigTableModel(
				localManifests, remoteManifests);
		final WTableView table = new WTableView();
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);
		table.setHeight(new WLength(400));


		proxyModel = new ToolConfigTableModelSortProxy(model);
		table.setModel(proxyModel);
		table.sortByColumn(2, SortOrder.AscendingOrder);

		for (int c = 0; c < model.getColumnCount(); ++c)
			table.setColumnWidth(c, model.getColumnWidth(c));

		final WPushButton addB = new WPushButton("Add");
		final WPushButton editB = new WPushButton("Edit");
		final WPushButton installB = new WPushButton("Install");
		final WPushButton newVersionB = new WPushButton("Create new version");

		installB.disable();

		installB.clicked().addListener(installB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					
					if (toolInfo.getState() == ToolState.RemoteNotSync) {
						ToolManifest manifest = toolInfo.getManifest();
						if (Settings.getInstance().getConfig().getToolConfigById(
								manifest.getId(), manifest.getVersion()) != null) {
							new MsgDialog("Install faied", "Tool id = " + manifest.getId() 
									+ ", version = " + manifest.getVersion() 
									+ " alredy exists on local server."); 
							return;
						}

						File f = null;
						try {
							f = ToolRepoServiceRequests.getTool(manifest.getId(), manifest.getVersion());
						} catch (ToolRepoServiceExeption e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						// create tool config.
						FileUtil.unzip(f, new File(manifest.suggestXmlDirName()));
						if (f != null) {
							AdminNavigation.setInstallUrl(
									toolInfo.getManifest().getId(),
									toolInfo.getManifest().getVersion());
						}
					}
				}
			}
		});
		
		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				AdminNavigation.setNewToolUrl();
			}
		});

		newVersionB.clicked().addListener(newVersionB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(table.getSelectedIndexes().first());
					AdminNavigation.setNewVersionToolUrl(
							toolInfo.getManifest().getId(),
							toolInfo.getManifest().getVersion());
				}
			}
		});

		table.doubleClicked().addListener(table, new Signal2.Listener<WModelIndex, WMouseEvent>() {

			public void trigger(WModelIndex index, WMouseEvent arg2) {
				if (index == null)
					return;
				ToolInfo toolInfo = proxyModel.getToolInfo(index);
				if (toolInfo.getState() == ToolState.RemoteNotSync)
					return;

				AdminNavigation.setEditToolUrl(
						toolInfo.getManifest().getId(),
						toolInfo.getManifest().getVersion());
			}
		});

		editB.clicked().addListener(editB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(table.getSelectedIndexes().first());
					AdminNavigation.setEditToolUrl(
							toolInfo.getManifest().getId(),
							toolInfo.getManifest().getVersion());
				}
			}
		});

		table.selectionChanged().addListener(this, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					installB.setEnabled(toolInfo.getState() == ToolState.RemoteNotSync);
					editB.setEnabled(toolInfo.getState() != ToolState.RemoteNotSync);
				} else {
					installB.disable();
					editB.disable();
				}
			}
		});

		versionChB.changed().addListener(this, new Signal.Listener() {
			public void trigger() {
				proxyModel.setFilterOldVersion(
						versionChB.getCheckState() == CheckState.Checked);
			}
		});
		versionChB.setChecked();
		versionChB.checked().trigger();

		bindWidget("version-chb", versionChB);
		bindWidget("table", table);
		bindWidget("add", addB);
		bindWidget("edit", editB);
		bindWidget("install", installB);
		bindWidget("new-version", newVersionB);
	}

	private List<ToolManifest> getRemoteManifests() {
		// get remote tools
		List<ToolManifest> remoteManifests = new ArrayList<ToolManifest>();
		String manifestsJson = ToolRepoServiceRequests.getManifests();
		if (manifestsJson == null || manifestsJson.isEmpty()) {
			infoT.setText("Could not read remote tools");
		} else {
			remoteManifests = ToolManifest.parseJsonAsList(manifestsJson);
			if (remoteManifests == null) {
				infoT.setText("Could not parss remote tools");
			}
		}

		return remoteManifests;
	}

	public void showCreateNewTool() {
		edit(null, ToolConfigForm.Mode.Add);
	}

	public void showEditTool(String toolId, String toolVersion, ToolConfigForm.Mode mode) {
		showTable(); // remove prev shown tool		

		ToolInfo toolInfo = proxyModel.getToolConfigTableModel().
				getToolInfo(toolId, toolVersion);

		if (toolInfo == null) {
			WContainerWidget c = new WContainerWidget();
			c.addWidget(new WText("Tool: id = " + toolId +
					", version = " + toolVersion + "does not exist."));
			WPushButton back = new WPushButton("Back", c);
			stack.addWidget(c);
			back.clicked().addListener(back, new Signal.Listener() {
				public void trigger() {
					showTable();
				}
			});
		} else {
			edit(toolInfo, mode);
		}
	}
	
	public void showTable() {
		while (stack.getChildren().size() > 1) {
			stack.removeWidget(stack.getWidget(1));
		}
	}

	private void edit(ToolInfo info, ToolConfigForm.Mode mode) {
		if (stack.getChildren().size() > 1) {
			return; // someone clicked too fast.
		}
		ToolConfig config = info == null ? null : info.getConfig();
		ToolManifest manifest = info == null ? null : info.getManifest();

		final ToolConfigForm d = new ToolConfigForm(config, manifest, mode);
		stack.addWidget(d);
		stack.setCurrentWidget(d);
		d.done().addListener(d, new Signal.Listener() {
			public void trigger() {
				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				AdminNavigation.setToolsTableUrl();
			}
		});
	}

	private List<ToolManifest> getLocalManifests() {
		List<ToolManifest> ans = new ArrayList<ToolManifest>();
		File xmlBaseDir = new File(Settings.getInstance().getBaseXmlDir());
		xmlBaseDir.mkdirs();
		for (File toolDir: xmlBaseDir.listFiles()){
			if (toolDir.listFiles() != null)
				for (File f: toolDir.listFiles()){
					if (ToolManifest.MANIFEST_FILE_NAME.equals(f.getName())){
						String json = FileUtil.readFile(f);
						if (json != null) {
							ToolManifest m = ToolManifest.parseJson(json);
							if (m != null)
								ans.add(m);
						}
					}
				}
		}

		return ans;
	}
}
