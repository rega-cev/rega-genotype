package rega.genotype.singletons;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;

import rega.genotype.ui.util.GenotypeLib;

/**
 * Make sure that only 1 tool is saved at time in all the system.
 * Note: Typing Tools are edited in a temporary dir. When saving 
 * the tool dir is replaced by the temporary dir. 
 * 
 * @author michael
 */
public class ToolEditingSynchronizer {
	private static ToolEditingSynchronizer instance = null;
	// <tool dir, lock> : lock per tool
	private Map<File, ReentrantReadWriteLock> locks = new HashMap<File, ReentrantReadWriteLock>();

	public ToolEditingSynchronizer() {
		instance = this;
	}

	public static ToolEditingSynchronizer getInstance() {
		return instance;
	}

	private ReentrantReadWriteLock getLock(File toolDir) {
		if (!locks.containsKey(toolDir))
			locks.put(toolDir, new ReentrantReadWriteLock());

		return locks.get(toolDir);
	}

	/**
	 * Copy the toolDir tmpWorkDir
	 */
	public boolean readTool(File tmpWorkDir, File toolDir) {
		getLock(toolDir).readLock().lock();
		try {
			FileUtils.copyDirectory(toolDir, tmpWorkDir);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			getLock(toolDir).readLock().unlock();
		}

		return true; 
	}

	/**
	 * Typing Tools are edited in a temporary dir. When saving 
	 * the tool dir is replaced by the temporary dir. 
	 * @param tmpWorkDir temporary dir in which the tool was edited.
	 * @param toolDir 
	 * @return
	 */
	public boolean saveTool(File tmpWorkDir, File toolDir) {
		File rollBackDir = GenotypeLib.createJobDir(
				Settings.getInstance().getBaseJobDir() + File.separator + "tmp_roolback");

		if (!readTool(rollBackDir, toolDir))
			return false;

		getLock(toolDir).writeLock().lock();

		try {
			FileUtils.deleteDirectory(toolDir);
			FileUtils.moveDirectory(tmpWorkDir, toolDir);
			tmpWorkDir.mkdirs(); // need to continue working on the tool.
		} catch (IOException e) {
			e.printStackTrace();

			// roll back
			try {
				FileUtils.copyDirectory(rollBackDir, toolDir);
			} catch (IOException e1) {
				e1.printStackTrace();
				return false;
			}

			return false;
		} finally {
			getLock(toolDir).writeLock().unlock();
		}

		try {
			FileUtils.deleteDirectory(rollBackDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
}
