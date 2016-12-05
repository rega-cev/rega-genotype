package rega.genotype.ui.admin.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.config.ToolUpdateService;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.admin.AdminNavigation;
import rega.genotype.ui.admin.config.ToolConfigForm.Mode;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolConfigTableModelSortProxy;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolInfo;
import rega.genotype.ui.admin.config.ToolConfigTableModel.ToolState;
import rega.genotype.ui.admin.file_editor.ui.ToolVerificationWidget;
import rega.genotype.ui.framework.widgets.Dialogs;
import rega.genotype.ui.framework.widgets.DownloadResource;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.StandardTableView;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.CheckState;
import eu.webtoolkit.jwt.SelectionBehavior;
import eu.webtoolkit.jwt.SelectionMode;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.SortOrder;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WDialog.DialogCode;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WMenuItem;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPopupMenu;
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
		stack.addStyleClass("stack");

		bindWidget("info", infoT);
		
		final WCheckBox versionChB = new WCheckBox("Show old versions");
		final WCheckBox remoteChB = new WCheckBox("Show remote tools");

		List<ToolManifest> remoteManifests = new ArrayList<ToolManifest>();

		// get local tools
		List<ToolManifest> localManifests = getLocalManifests();
		
		// create table
		ToolConfigTableModel model = new ToolConfigTableModel(
				localManifests, remoteManifests);
		final StandardTableView table = new StandardTableView();
		table.setSelectionMode(SelectionMode.ExtendedSelection);
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

		// tools menu

		final WContainerWidget toolsMenuC = new WContainerWidget();
		toolsMenuC.setInline(true);
		final WPopupMenu toolsPopup = new WPopupMenu();
		toolsPopup.addStyleClass("popup");
		final WPushButton toolB = new WPushButton("Tools", toolsMenuC);
		//toolB.setIcon(new WLink("pics/suggest-dropdown.png"));
		toolB.setMenu(toolsPopup);

		final WMenuItem create = toolsPopup.addItem("Create new tool");
		final WMenuItem newVersion = toolsPopup.addItem("Create new version");
		final WMenuItem autoCreate = toolsPopup.addItem("Auto create");
		toolsPopup.addSeparator();
		final WMenuItem edit = toolsPopup.addItem("Edit");
		toolsPopup.addSeparator();
		final WMenuItem importItem = toolsPopup.addItem("Import");
		final WMenuItem export = toolsPopup.addItem("Export");


		newVersion.setToolTip("Create new version of selected tool");
		autoCreate.setToolTip("Auto create a tool from selected template");
		edit.setToolTip("Edit currently selected tools.");
		importItem.setToolTip("Import a new tool.");
		export.setToolTip("Export selected tool. Can be used to send a tool by email.");

		// repo menu

		final WContainerWidget repoMenuC = new WContainerWidget();
		repoMenuC.setInline(true);
		final WPopupMenu repoPopup = new WPopupMenu();
		repoPopup.addStyleClass("popup");
		final WPushButton repoB = new WPushButton("Repository", repoMenuC);
		//toolB.setIcon(new WLink("pics/suggest-dropdown.png"));
		repoB.setMenu(repoPopup);

		final WMenuItem install = repoPopup.addItem("Install");
		final WMenuItem uninstall = repoPopup.addItem("Uninstall");
		final WMenuItem update = repoPopup.addItem("Update");

		install.setToolTip("Download selected tool from remote repository to local server.");
		uninstall.setToolTip("Delete selected tool from local server.");
		update.setToolTip("Download the latest version of selected tool to local server.");

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

					FileUtil.zip(toolFile, zip, 
							Config.TRANSIENT_DATABASES_FOLDER_NAME);
					return zip;
				}

				return null;
			}
		};

		export.getAnchor().setTarget(AnchorTarget.TargetDownload);
		export.getAnchor().setLink(new WLink(downloadR));
		export.getAnchor().addStyleClass("standard-text", true);
		export.disable();

		install.disable();

		install.clicked().addListener(install, new Signal.Listener() {
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

		uninstall.clicked().addListener(uninstall, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() > 0) {
					String toolsStr = "";
					for (WModelIndex index: table.getSelectedIndexes()){
						if (!toolsStr.isEmpty())
							toolsStr += " ,";
						toolsStr +=  proxyModel.getToolInfo(index).getManifest().getName();
					}

					Dialogs.removeDialog(toolsStr).finished().addListener(
							table, new Signal1.Listener<DialogCode>() {
						public void trigger(DialogCode arg) {
							if (arg == DialogCode.Accepted) {
								List<ToolInfo> infos = new ArrayList<ToolConfigTableModel.ToolInfo>();
								for (WModelIndex index: table.getSelectedIndexes())
									infos.add(proxyModel.getToolInfo(index));

								for (ToolInfo toolInfo: infos){
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
						}
					});
				}
			}
		});

		update.clicked().addListener(update, new Signal.Listener() {
			public void trigger() {
				if (table.getSelectedIndexes().size() == 1) {
					ToolInfo toolInfo = proxyModel.getToolInfo(
							table.getSelectedIndexes().first());
					ToolUpdateService.update(getRemoteManifests(), toolInfo.getManifest().getId());
					proxyModel.refresh(getLocalManifests(), getRemoteManifests());
				}
			}
		});

		importItem.clicked().addListener(importItem, new Signal.Listener() {
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
								final UploadedFile f = fileUpload.getWFileUpload().getUploadedFiles().get(0);
								
								final String manifestJson = FileUtil.getFileContent(new File(f.getSpoolFileName()), 
										ToolManifest.MANIFEST_FILE_NAME);
								final ToolManifest manifest = ToolManifest.parseJson(manifestJson);
								if (manifest != null) {
									Config config = Settings.getInstance().getConfig();
									if (config.getToolConfigById(manifest.getId(), manifest.getVersion()) != null) {
										StandardDialog d = new StandardDialog("Warning");
										d.getContents().addWidget(new WText("Tool id: " + manifest.getId() + ", version: " + manifest.getVersion() 
												+ " already exists. Replace existing tool?"));
										d.setWidth(new WLength(300));
										d.finished().addListener(d, new Signal1.Listener<DialogCode>() {
											public void trigger(DialogCode arg) {
												if (arg == DialogCode.Accepted)
													importTool(manifest, new File(f.getSpoolFileName()), Mode.Import);
											}
										});
									} else
										importTool(manifest, new File(f.getSpoolFileName()), Mode.Import);
								} else {
									Dialogs.infoDialog("Error", "Invalid tool file: No manifest");
								}
							}
						}
					}
				});

				proxyModel.refresh(getLocalManifests(), getRemoteManifests());
			}
		});

		create.clicked().addListener(create, new Signal.Listener() {
			public void trigger() {
				edit(null, Mode.Add);
			}
		});

		newVersion.clicked().addListener(newVersion, new Signal.Listener() {
			public void trigger() {
				createNewersion(table, false);
			}
		});

		autoCreate.clicked().addListener(autoCreate, new Signal.Listener() {
			public void trigger() {
				createNewersion(table, true);
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

		edit.clicked().addListener(edit, new Signal.Listener() {
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
					install.setDisabled(toolInfo.getState() != ToolState.RemoteNotSync);
					edit.setDisabled(toolInfo.getState() == ToolState.RemoteNotSync);
					newVersion.setDisabled(toolInfo.getState() == ToolState.RemoteNotSync);
					autoCreate.setDisabled(!toolInfo.getManifest().isTemplate());

					// only the last version can be updated 
					update.setDisabled(proxyModel.getToolConfigTableModel().isUpToDate(
							toolInfo.getManifest().getId()));

					// uninstall
					uninstall.setDisabled(toolInfo.getState() == ToolState.RemoteNotSync);
					if (toolInfo.getState() == ToolState.Local || toolInfo.getState() == ToolState.Retracted)
						uninstall.setText("Remove");
					else
						uninstall.setText("Uninstall");
					
					if (toolInfo.getManifest() != null && toolInfo.getConfig() != null){
						File zip = new File(Settings.getInstance().getBasePackagedToolsDir() 
								+ File.separator + toolInfo.getManifest().getUniqueToolId() + ".zip");
						downloadR.setFileName(zip.getAbsolutePath());
						downloadR.suggestFileName(toolInfo.getManifest().getUniqueToolId() + ".zip");
						export.enable();
					}
				} else if (table.getSelectedIndexes().size() > 1) {
					uninstall.enable();
					uninstall.setText("Remove/Uninstall");
					for (WModelIndex index: table.getSelectedIndexes()){
						final ToolInfo toolInfo = proxyModel.getToolInfo(index);
						if (toolInfo.getState() == ToolState.RemoteNotSync)
							uninstall.disable();
					}
				} else {
					install.disable();
					edit.disable();
					newVersion.disable();
					uninstall.disable();
					update.disable();
					export.disable();
					autoCreate.disable();
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
				List<ToolManifest> remoteManifests = getRemoteManifests();
				proxyModel.refresh(getLocalManifests(), remoteManifests);
				proxyModel.setFilterNotRemote(
						remoteChB.getCheckState() != CheckState.Checked);
				Settings.getInstance().getConfig().refreshToolCofigState(remoteManifests);
			}
		});
		remoteChB.setChecked(false);
		remoteChB.setToolTip("Show tool from remote repository and synchronize the state existing tools with the remote repository.");
		proxyModel.setFilterNotRemote(true);

		bindWidget("repo-menu", repoMenuC);
		bindWidget("tools-menu", toolsMenuC);
		bindWidget("version-chb", versionChB);
		bindWidget("remote-chb", remoteChB);
		bindWidget("table", table);
	}

	private void importTool(ToolManifest manifest, File f, ToolConfigForm.Mode mode) {
		// create tool config.
		FileUtil.unzip(f, new File(manifest.suggestXmlDirName()));
		if (f != null) {
			// create local config for installed tool
			ToolConfig config = new ToolConfig();
			config.setConfiguration(manifest.suggestXmlDirName());
			config.genetareJobDir(manifest.suggestXmlDirName());
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

	public void showToolNotFound(String toolId, String toolVersion) {
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
	}
	public void showEditTool(String toolId, String toolVersion, ToolConfigForm.Mode mode) {
		showTable(); // remove prev shown tool		

		ToolInfo toolInfo = proxyModel.getToolConfigTableModel().
				getToolInfo(toolId, toolVersion);

		if (toolInfo == null) 
			showToolNotFound(toolId, toolVersion);
		else
			edit(toolInfo, mode);
	}

	public void showToolVerify(String toolId, String toolVersion, String jobId) {
		ToolInfo toolInfo = proxyModel.getToolConfigTableModel().
				getToolInfo(toolId, toolVersion);
		if (toolInfo == null) 
			showToolNotFound(toolId, toolVersion);
		else {
			File verificationWorkDir;
			if (jobId == null) 
				verificationWorkDir = GenotypeLib.createJobDir(toolInfo.getConfig().getVerificationDir());
			else 
				verificationWorkDir = new File(toolInfo.getConfig().getVerificationDir(), jobId);

			if (!verificationWorkDir.exists())
				verificationWorkDir.mkdirs();
			ToolVerificationWidget verificationWidget = new ToolVerificationWidget(
					toolInfo.getConfig(), verificationWorkDir);
			
			stack.addWidget(verificationWidget);
			stack.setCurrentWidget(verificationWidget);
			verificationWidget.done().addListener(verificationWidget, new Signal.Listener() {
				public void trigger() {
					proxyModel.refresh(getLocalManifests(), getRemoteManifests());
					AdminNavigation.setToolsTableUrl();
					showTable(); // in case that the url did not change (add tool)
				}
			});
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

	private void createNewersion(final StandardTableView table, boolean fromTemplate) {
		if (table.getSelectedIndexes().size() == 1) {
			ToolInfo info = proxyModel.getToolInfo(table.getSelectedIndexes().first());

			ToolConfig config = info.getConfig().copy();
			String suggestedDirName = info.getConfig().getToolMenifest().getId() +
					suggestNewVersion(info.getConfig(), 1);
			config.genetareJobDir(suggestedDirName);
			config.genetareConfigurationDir(suggestedDirName);

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
				if (fromTemplate) {
					config.getToolMenifest().setName("");
					config.getToolMenifest().setId("");
					config.getToolMenifest().setTemplate(false);
				}

				config.getToolMenifest().save(config.getConfiguration());
			}

			proxyModel.refresh(getLocalManifests(), getRemoteManifests());

			AdminNavigation.setEditToolUrl(
					config.getToolMenifest().getId(),
					config.getToolMenifest().getVersion());
		}
	}
}
