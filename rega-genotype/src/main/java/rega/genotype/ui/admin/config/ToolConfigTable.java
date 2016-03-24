package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.ui.admin.config.ToolConfigDialog.Mode;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolConfigTableModelSortProxy;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolInfo;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolState;
import rega.genotype.ui.framework.widgets.MsgDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
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

	public ToolConfigTable(WContainerWidget parent) {
		super(tr("admin.config.tool-config-table"), parent);

		bindWidget("info", infoT);

		List<ToolManifest> remoteManifests = getRemoteManifests();

		// get local tools
		List<ToolManifest> localManifests = getLocalManifests();
		
		// create table
		ToolConfigTableModel model = new ToolConfigTableModel(
				localManifests, remoteManifests);
		final WTableView table = new WTableView();
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);

		proxyModel = new ToolConfigTableModelSortProxy(model);
		table.setModel(proxyModel);
		
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
						FileUtil.unzip(f, 
								new File(Settings.getInstance().getXmlDir(
								manifest.getId(), manifest.getVersion())));
						if (f != null)
							edit(toolInfo, Mode.Install);
					}
				}
			}
		});
		
		addB.clicked().addListener(addB, new Signal.Listener() {
			public void trigger() {
				edit(null, Mode.Add);
			}
		});

		newVersionB.clicked().addListener(newVersionB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					edit(proxyModel.getToolInfo(
							table.getSelectedIndexes().first()), Mode.NewVersion);
				}
			}
		});

		table.doubleClicked().addListener(table, new Signal2.Listener<WModelIndex, WMouseEvent>() {

			public void trigger(WModelIndex index, WMouseEvent arg2) {
				if (index == null)
					return;
				edit(proxyModel.getToolInfo(index), Mode.Edit);
			}
		});

		editB.clicked().addListener(editB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					edit(proxyModel.getToolInfo(
							table.getSelectedIndexes().first()), Mode.Edit);
				}
			}
		});

		table.selectionChanged().addListener(this, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					installB.setEnabled(toolInfo.getState() == ToolState.RemoteNotSync);
				} else {
					installB.disable();
				}
			}
		});
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
		if (manifestsJson == null) {
			infoT.setText("Could not read remote tools");
		} else {
			remoteManifests = ToolManifest.parseJsonAsList(manifestsJson);
			if (remoteManifests == null)
				infoT.setText("Could not parss remote tools");
		}

		return remoteManifests;
	}

	private void edit(ToolInfo info, ToolConfigDialog.Mode mode) {
		ToolConfig config = info == null ? null : info.getConfig();
		ToolManifest manifest = info == null ? null : info.getManifest();
		ToolConfigDialog d = new ToolConfigDialog(config, manifest, mode);
		d.finished().addListener(d, new Signal1.Listener<WDialog.DialogCode>() {
			public void trigger(WDialog.DialogCode arg) {
				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
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
