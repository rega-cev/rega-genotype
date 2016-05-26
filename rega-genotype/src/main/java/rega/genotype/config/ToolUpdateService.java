package rega.genotype.config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rega.genotype.config.Config.ToolConfig;
import rega.genotype.service.ToolRepoServiceRequests;
import rega.genotype.service.ToolRepoServiceRequests.ToolRepoServiceExeption;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;

public class ToolUpdateService {

	public static void update(List<ToolManifest> remoteManifests, String toolId) {
		ToolManifest lastPublishedRemoteManifest = ToolManifest.lastPublishedVesrsion(remoteManifests,toolId);
		File f = null;
		try {
			f = ToolRepoServiceRequests.getTool(
					lastPublishedRemoteManifest.getId(), lastPublishedRemoteManifest.getVersion());
		} catch (ToolRepoServiceExeption e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// copy tool content
		FileUtil.unzip(f, new File(lastPublishedRemoteManifest.suggestXmlDirName()));
		if (f != null) {
			ToolConfig localLastPublished = Settings.getInstance().getConfig().
					getLastPublishedToolConfig(lastPublishedRemoteManifest.getId());
			// create tool config.
			ToolConfig newConfig = new ToolConfig();
			newConfig.setAutoUpdate(localLastPublished.isAutoUpdate());
			newConfig.setUi(localLastPublished.isUi());
			newConfig.setWebService(localLastPublished.isWebService());
			newConfig.setPath(localLastPublished.getPath());
			newConfig.setConfiguration(lastPublishedRemoteManifest.suggestXmlDirName());
			newConfig.setJobDir(lastPublishedRemoteManifest.suggestJobDirName());
			newConfig.setPublished(true);

			Settings.getInstance().getConfig().putTool(newConfig);

			localLastPublished.setPath("");
			try {
				Settings.getInstance().getConfig().save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Auto updates all tools where auto update option is enabled,
	 * every UPDATE_INTERVAL_MIN minutes.
	 * 
	 * auto update installs the latest version of the tool from repo (if not exist locally 
	 * and the latest installed version has auto update enabled) 
	 * It will copy url, web service enabled, auto update enabled, url enabled 
	 * from the latest installed version. Last installed url will be removed.
	 * 
	 * TODO: Maybe auto update should be a property of a tool and not of {tool id, tool version}
	 */
	private static Logger logger = LoggerFactory.getLogger("auto-update");
	private static final long UPDATE_INTERVAL_MIN = 24*60; 
	public static void startAutoUpdateTask() {
		Thread thread = new Thread(new Runnable() {
			Object lock = new Object();
			public void run() {
				while(true) {
					Config config = Settings.getInstance().getConfig();
					if (config != null){
						String remoteManifestsJson = ToolRepoServiceRequests.getManifestsJson();
						if (remoteManifestsJson != null){
							List<ToolManifest> remoteManifests = ToolManifest.
									parseJsonAsList(remoteManifestsJson);
							if (remoteManifests != null)
								for (ToolManifest remoteManifest : remoteManifests) {
									ToolConfig localLastPublished = config.
											getLastPublishedToolConfig(remoteManifest.getId());
									if (localLastPublished != null
											&& localLastPublished.isAutoUpdate()
											&& ToolManifest.isLastPublishedVesrsion(remoteManifests, remoteManifest)
											&& localLastPublished.getToolMenifest().
											getPublicationDate().compareTo(
													remoteManifest.getPublicationDate()) < 0) {
										// This version is not yet installed.
										logger.info("Auto update of tool: id = " 
												+ remoteManifest.getId() + ", version = "
												+ remoteManifest.getVersion());
										update(remoteManifests, remoteManifest.getId());
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
