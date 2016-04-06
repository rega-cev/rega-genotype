package rega.genotype.ui.admin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WEnvironment;
import eu.webtoolkit.jwt.WtServlet;

/**
 * Servlet for admin area.
 * @author michael
 */
public class AdminMain extends WtServlet{
	private static final long serialVersionUID = 1L;

	@Override
	public WApplication createApplication(WEnvironment env) {
		return new AdminApplication(env);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// init the settings
		Settings.getInstance(config.getServletContext()).getConfig();

		getConfiguration().setMaximumRequestSize(2000*1024*1024); // 2000 MB maximum file requests, should match servlet config
		
		initAutoUpdateTask();

		super.init(config);
	}

	public static AdminMain getAdminMainInstance() {
		return (AdminMain) getInstance();
	}

	/**
	 * Auto updates all tools where auto update option is enabled,
	 * every UPDATE_INTERVAL_MIN minutes.
	 * 
	 * auto update installs the latest version of the tool from repo (if not exist locally) 
	 * It will copy url, web service enabled, auto update enabled, url enabled 
	 * from the latest installed version. Last installed url will be removed.
	 * 
	 * TODO: Maybe auto update should be a property of a tool and not of {tool id, tool version}
	 */
	private static Logger logger = LoggerFactory.getLogger("auto-update");
	private static final long UPDATE_INTERVAL_MIN = 24*60; 
	private void initAutoUpdateTask() {
		Thread thread = new Thread(new Runnable() {
			Object lock = new Object();
			public void run() {
				while(true) {
					Config config = Settings.getInstance().getConfig();
					if (config != null){
						String remoteManifestsJson = ToolRepoServiceRequests.getManifests();
						if (remoteManifestsJson != null){
							List<ToolManifest> remoteManifests = ToolManifest.
									parseJsonAsList(remoteManifestsJson);
							for (ToolManifest remoteManifest : remoteManifests) {
								ToolConfig localLastPublished = config.
										getLastPublishedToolConfigById(remoteManifest.getId());
								if (localLastPublished != null
										&& localLastPublished.isAutoUpdate()
										&& localLastPublished.getToolMenifest().
										getPublicationDate().compareTo(
												remoteManifest.getPublicationDate()) < 0) {
									// This version is not yet installed.
									logger.info("Auto update of tool: id = " 
											+ remoteManifest.getId() + ", version = "
											+ remoteManifest.getVersion());
									File f = null;
									try {
										f = ToolRepoServiceRequests.getTool(
												remoteManifest.getId(), remoteManifest.getVersion());
									} catch (ToolRepoServiceExeption e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}

									// copy tool content
									FileUtil.unzip(f, new File(remoteManifest.suggestXmlDirName()));
									if (f != null) {
										// create tool config.
										ToolConfig newConfig = new ToolConfig();
										newConfig.setAutoUpdate(localLastPublished.isAutoUpdate());
										newConfig.setUi(localLastPublished.isUi());
										newConfig.setWebService(localLastPublished.isWebService());
										newConfig.setPath(localLastPublished.getPath());
										newConfig.setConfiguration(remoteManifest.suggestXmlDirName());
										newConfig.setJobDir(remoteManifest.suggestJobDirName());

										config.putTool(newConfig);

										localLastPublished.setPath("");
										try {
											config.save();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
					}
					// sleep
					synchronized (lock) {
						try {
							lock.wait(60000 * UPDATE_INTERVAL_MIN);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});
		thread.start();
	}
	
}
