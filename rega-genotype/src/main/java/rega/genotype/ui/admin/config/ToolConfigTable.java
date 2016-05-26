package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.config.ToolUpdateService;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.ui.admin.AdminNavigation;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolConfigTableModelSortProxy;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolInfo;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolState;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.DownloadResource;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.StandardTableView;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.CheckState;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.SortOrder;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStackedWidget;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.UploadedFile;

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
		
		final WCheckBox versionChB = new WCheckBox("Show old versions");
		final WCheckBox remoteChB = new WCheckBox("Show remote tools");

		List<ToolManifest> remoteManifests = getRemoteManifests();

		// get local tools
		List<ToolManifest> localManifests = getLocalManifests();
		
		// create table
		ToolConfigTableModel model = new ToolConfigTableModel(
				localManifests, remoteManifests);
		final StandardTableView table = new StandardTableView();
		table.setSelectionMode(SelectionMode.SingleSelection);
		table.setSelectionBehavior(SelectionBehavior.SelectRows);
		table.setHeight(new WLength(400));


		proxyModel = new ToolConfigTableModelSortProxy(model);
		table.setModel(proxyModel);
		table.sortByColumn(2, SortOrder.AscendingOrder);

		table.setColumnWidth(ToolConfigTableModel.URL_COLUMN, new WLength(100));
		table.setColumnWidth(ToolConfigTableModel.NAME_COLUMN, new WLength(120));
		table.setColumnWidth(ToolConfigTableModel.ID_COLUMN, new WLength(100));
		table.setColumnWidth(ToolConfigTableModel.VERSION_COLUMN, new WLength(60));
		table.setColumnWidth(ToolConfigTableModel.DATE_COLUMN, new WLength(100));
		table.setColumnWidth(ToolConfigTableModel.PUBLISHER_COLUMN, new WLength(100));
		table.setColumnWidth(ToolConfigTableModel.STATE_COLUMN, new WLength(60));
		table.setColumnWidth(ToolConfigTableModel.UPTODATE_COLUMN, new WLength(100));
		table.setColumnWidth(ToolConfigTableModel.INSTALLED_COLUMN, new WLength(80));

		table.setTableWidth();
		setMaximumSize(table.getWidth(), WLength.Auto);

		final WPushButton addB = new WPushButton("Add");
		final WPushButton editB = new WPushButton("Edit");
		final WPushButton installB = new WPushButton("Install");
		final WPushButton newVersionB = new WPushButton("Create new version");
		final WPushButton uninstallB = new WPushButton("Uninstall");
		final WPushButton updateB = new WPushButton("Update");
		final WPushButton importB = new WPushButton("Import");
		
		// downloadB
		final DownloadResource downloadR = new DownloadResource("", "") {
			@Override
			public File downlodFile() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					File toolFile = toolInfo.getConfig().getConfigurationFile();
					if (toolFile == null || !toolFile.exists()
							|| toolInfo.getManifest() == null) {
						return null;
					}

					File zip = new File(Settings.getInstance().getBasePackagedToolsDir() 
							+ File.separator + toolInfo.getManifest().getUniqueToolId() + ".zip");
					if (zip.exists())
						zip.delete();
					zip.getParentFile().mkdirs();
					try {
						zip.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}

					FileUtil.zip(toolFile, zip);
					return zip;
				}

				return null;
			}
		};
		final WAnchor downloadB = new WAnchor();
		downloadB.setTarget(AnchorTarget.TargetDownload);
		downloadB.setText("Export");
		downloadB.setStyleClass("like-button");
		downloadB.setLink(new WLink(downloadR));
		downloadB.disable();

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
							Dialogs.infoDialog("Install faied", "Tool id = " + manifest.getId() 
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

						importTool(manifest, f, Mode.Install);
					}
				}
			}
		});

		uninstallB.clicked().addListener(uninstallB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					try {
						FileUtils.deleteDirectory(new File(toolInfo.getConfig().getConfiguration()));
					} catch (IOException e) {
						e.printStackTrace();
						Dialogs.infoDialog("Error", "Could not delete tool, Error: " + e.getMessage()); 
					}
					Settings.getInstance().getConfig().removeTool(toolInfo.getConfig());
					try {
						Settings.getInstance().getConfig().save();
					} catch (IOException e) {
						e.printStackTrace();
						assert(false);
						Dialogs.infoDialog("Info", "Global config not save, due to IO error.");
					}

					proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				}

			}
		});

		updateB.clicked().addListener(updateB, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					ToolUpdateService.update(getRemoteManifests(), toolInfo.getManifest().getId());
					proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				}
			}
		});

		importB.clicked().addListener(importB, new Signal.Listener() {
			public void trigger() {
				StandardDialog d= new StandardDialog("Import");
				final FileUpload fileUpload = new FileUpload();
				fileUpload.getWFileUpload().setFilters(".zip");
				d.getContents().addWidget(new WText("Choose import file"));
				d.getContents().addWidget(fileUpload);
				d.finished().addListener(d,  new Signal1.Listener<WDialog.DialogCode>() {
					public void trigger(DialogCode arg) {
						if (arg == DialogCode.Accepted) {
							if (fileUpload.getWFileUpload().getUploadedFiles().size() == 1) {
								UploadedFile f = fileUpload.getWFileUpload().getUploadedFiles().get(0);
								
								String manifestJson = FileUtil.getFileContent(new File(f.getSpoolFileName()), 
										ToolManifest.MANIFEST_FILE_NAME);
								ToolManifest manifest = ToolManifest.parseJson(manifestJson);
								if (manifest != null) {
									importTool(manifest, new File(f.getSpoolFileName()), Mode.Import);
								} else {
									Dialogs.infoDialog("Error", "Invalid tool file");
								}
							}
						}
					}
				});

				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
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
					ToolInfo info = proxyModel.getToolInfo(table.getSelectedIndexes().first());

					ToolConfig config = info.getConfig().copy();
					config.genetareJobDir();
					config.genetareConfigurationDir(
							info.getConfig().getToolMenifest().getId() + suggestNewVersion(info.getConfig(), 1));
					Settings.getInstance().getConfig().putTool(config);
					try {
						Settings.getInstance().getConfig().save();
						String oldVersionDir = info.getConfig().getConfiguration();
						FileUtil.copyDirContentRecorsively(new File(oldVersionDir), 
								config.getConfigurationFile());
					} catch (IOException e) {
						e.printStackTrace();
						assert(false); // coping to new dir should always work.
					}

					// the manifest was also copied
					if (config.getToolMenifest() != null) {
						config.getToolMenifest().setVersion(suggestNewVersion(config, 1));
						config.getToolMenifest().setPublicationDate(null);
						config.getToolMenifest().setPublisherName(null);
						config.getToolMenifest().save(config.getConfiguration());
					}

					proxyModel.refresh(getLocalManifests(), getRemoteManifests());

					AdminNavigation.setEditToolUrl(
							config.getToolMenifest().getId(),
							config.getToolMenifest().getVersion());
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
					newVersionB.setEnabled(toolInfo.getState() != ToolState.RemoteNotSync);

					// only the last version can be updated 
					updateB.setEnabled(!proxyModel.getToolConfigTableModel().isUpToDate(
							toolInfo.getManifest().getId()));

					// uninstall
					uninstallB.setEnabled(toolInfo.getState() != ToolState.RemoteNotSync);
					if (toolInfo.getState() == ToolState.Local || toolInfo.getState() == ToolState.Retracted)
						uninstallB.setText("Remove");
					else
						uninstallB.setText("Uninstall");

					if (toolInfo.getManifest() != null){
						File zip = new File(Settings.getInstance().getBasePackagedToolsDir() 
								+ File.separator + toolInfo.getManifest().getUniqueToolId() + ".zip");
						downloadR.setFileName(zip.getAbsolutePath());
						downloadR.suggestFileName(toolInfo.getManifest().getUniqueToolId() + ".zip");
						downloadB.enable();
					}
				} else {
					installB.disable();
					editB.disable();
					newVersionB.disable();
					uninstallB.disable();
					updateB.disable();
					downloadB.disable();
				}
			}
		});
		table.selectionChanged().trigger();

		versionChB.changed().addListener(this, new Signal.Listener() {
			public void trigger() {
				proxyModel.setFilterOldVersion(
						versionChB.getCheckState() != CheckState.Checked);
			}
		});
		versionChB.setChecked(false);
		proxyModel.setFilterOldVersion(true);

		remoteChB.changed().addListener(this, new Signal.Listener() {
			public void trigger() {
				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				proxyModel.setFilterNotRemote(
						remoteChB.getCheckState() != CheckState.Checked);
			}
		});
		remoteChB.setChecked(false);
		proxyModel.setFilterNotRemote(true);

		bindWidget("version-chb", versionChB);
		bindWidget("remote-chb", remoteChB);
		bindWidget("table", table);
		bindWidget("add", addB);
		bindWidget("edit", editB);
		bindWidget("install", installB);
		bindWidget("new-version", newVersionB);
		bindWidget("uninstall", uninstallB);
		bindWidget("update", updateB);
		bindWidget("import", importB);
		bindWidget("export", downloadB);

	}

	private void importTool(ToolManifest manifest, File f, ToolConfigForm.Mode mode) {
		// create tool config.
		FileUtil.unzip(f, new File(manifest.suggestXmlDirName()));
		if (f != null) {
			// create local config for installed tool
			ToolConfig config = new ToolConfig();
			config.setConfiguration(manifest.suggestXmlDirName());
			config.genetareJobDir();
			if (mode == Mode.Install)
				config.setPublished(true);
			Settings.getInstance().getConfig().putTool(config);
			try {
				Settings.getInstance().getConfig().save();
			} catch (IOException e) {
				e.printStackTrace();
				assert(false); // coping to new dir should always work.
			}

			proxyModel.refresh(getLocalManifests(), getRemoteManifests());
			
			// redirect to edit screen.
			AdminNavigation.setEditToolUrl(
					manifest.getId(),
					manifest.getVersion());
		} else {
			Dialogs.infoDialog("Error", "Invalid tool file.");
		}
	}

	private List<ToolManifest> getRemoteManifests() {
		// get remote tools
		List<ToolManifest> remoteManifests = new ArrayList<ToolManifest>();
		String manifestsJson = ToolRepoServiceRequests.getManifestsJson();
		if (manifestsJson == null || manifestsJson.isEmpty()) {
			infoT.setText("Could not read remote tools");
		} else {
			remoteManifests = ToolManifest.parseJsonAsList(manifestsJson);
			if (remoteManifests == null) {
				infoT.setText("Could not parss remote tools");
				return new ArrayList<ToolManifest>();
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
			stack.setCurrentWidget(c);
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

		ToolConfig config = null;
		switch (mode) {
		case Add:
			config = new ToolConfig();
			break;
		case Edit:		
			config = Settings.getInstance().getConfig().getToolConfigById(
					info.getManifest().getId(), info.getManifest().getVersion());
			break;
		default:
			break;
		}

		final ToolConfigForm d = new ToolConfigForm(config, mode);
		stack.addWidget(d);
		stack.setCurrentWidget(d);
		d.done().addListener(d, new Signal.Listener() {
			public void trigger() {
				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				AdminNavigation.setToolsTableUrl();
				showTable(); // in case that the url did not change (add tool)
			}
		});
	}

	private String suggestNewVersion(ToolConfig config, Integer sggestedVersion) {
		// find a version number that was not used yet.
		
		for (ToolManifest m: Settings.getInstance().getConfig().getManifests()) {
			if (m.getId().equals(config.getToolMenifest().getId())
					&& m.getVersion().equals(sggestedVersion.toString())){
				return suggestNewVersion(config, sggestedVersion + 1);
			}
		}

		return sggestedVersion.toString();
	}

	private List<ToolManifest> getLocalManifests() {
		List<ToolManifest> ans = new ArrayList<ToolManifest>();
		for (ToolConfig c: Settings.getInstance().getConfig().getTools()) {
			String json = FileUtil.readFile(new File(
					c.getConfigurationFile(), ToolManifest.MANIFEST_FILE_NAME));
			if (json != null) {
				ToolManifest m = ToolManifest.parseJson(json);
				if (m != null)
					ans.add(m);
			}
		}

		return ans;
	}
}
