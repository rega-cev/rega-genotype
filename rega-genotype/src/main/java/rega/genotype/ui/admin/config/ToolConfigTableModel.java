package rega.genotype.ui.admin.config;

import java.util.ArrayList;
import java.util.List;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WSortFilterProxyModel;

public class ToolConfigTableModel extends WAbstractTableModel {
	String[] headers = { "URL", "Name", "ID", "Version", "Publication date", "Publisher" , "State" };
	List<ToolInfo> rows = new ArrayList<ToolInfo>();

	public enum ToolState {
		Local {           // The tool is not published 
			@Override
			String str() {return "Local";}
		}, RemoteSync {   // local and remote are in sync
			@Override
			String str() {return "Remote in sync with local";}
		}, RemoteNotSync { // published but not installed.
			@Override
			String str() {return "Remote NOT in sync with local";}
		};  

		abstract String str();
	}
	
	public static class ToolInfo {
		private ToolState state = null;
		private ToolConfig config = null;
		private ToolManifest manifest = null;
		public ToolState getState() {
			return state;
		}
		public void setState(ToolState state) {
			this.state = state;
		}
		public ToolConfig getConfig() {
			return config;
		}
		public void setConfig(ToolConfig config) {
			this.config = config;
		}
		public ToolManifest getManifest() {
			return manifest;
		}
		public void setManifest(ToolManifest manifest) {
			this.manifest = manifest;
		}
	}

	public ToolConfigTableModel(
			List<ToolManifest> localManifests,
			List<ToolManifest> remoteManifests) {
		Config config = Settings.getInstance().getConfig();
		// add locals
		for (ToolManifest m: localManifests) {
			ToolInfo info = new ToolInfo();
			info.setConfig(find(m.getId(), m.getVersion(), config));
			info.setManifest(m);
			ToolManifest published = find(m.getId(), m.getVersion(), remoteManifests);
			info.setState(published != null ? ToolState.RemoteSync : ToolState.Local); 
			rows.add(info);
		}
		// add remote not synced
		for (ToolManifest m: remoteManifests) {
			ToolManifest local = find(m.getId(), m.getVersion(), localManifests);
			if (local == null) {
				ToolInfo info = new ToolInfo();
				info.setManifest(m);
				info.setState(ToolState.RemoteNotSync);
				rows.add(info);
			} // else was add before.
		}
	}

	@Override
	public Object getHeaderData(int section, Orientation orientation, int role) {
		if (role == ItemDataRole.DisplayRole 
				&& orientation == Orientation.Horizontal
				&& section < headers.length) {
			return headers[section];
		} else
			return super.getHeaderData(section, orientation, role);
	}

	@Override
	public int getColumnCount(WModelIndex parent) {
		return headers.length;
	}

	@Override
	public int getRowCount(WModelIndex parent) {
		return rows.size();
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		ToolInfo info = getToolInfo(index.getRow());
		if (role == ItemDataRole.DisplayRole) {
			//	String[] headers = { "URL", "Name", "ID", "Version", "Date", "Publisher" , "State" };

			switch (index.getColumn()) {
			case 0:					
				return info.getConfig() == null ? null : info.getConfig().getPath();
			case 1:
				return info.getManifest() == null ? null : info.getManifest().getName();
			case 2:
				return info.getManifest() == null ? null : info.getManifest().getId();
			case 3:
				return info.getManifest() == null ? null : info.getManifest().getVersion();
			case 4:
				return "TODO date"; 
				//return toolConfig.getToolMenifest().getCreationDate();
			case 5:
				return "TODO publisher"; 
			case 6:
				return info.getState().str(); // local, uptodate, 
			default:
				break;
			}
		} else if (role == ItemDataRole.LinkRole) {
			if (index.getColumn() == 0 && info.getConfig() != null)
				return new WLink("typingtool/" + info.getConfig().getPath());
		}
		return null;
	}

	public void refresh() {
		layoutAboutToBeChanged().trigger();
		layoutChanged().trigger();
	}

	public ToolInfo getToolInfo(int row) {
		 return rows.get(row);
	}

	private ToolManifest find(String toolId, String version, 
			List<ToolManifest> manifests) {
		// TODO!: this can not cover the case of a user that creates a tool with 
		// the same id, version as an existing tool in the repo. 
		for (ToolManifest m: manifests) {
			if (m.getId().equals(toolId) && m.getVersion().equals(version))
				return m;
		}
		return null;
	}

	private ToolConfig find(String toolId, String version, 
			Config config) {
		for (ToolConfig c: config.getTools()) {
			ToolManifest m = c.getToolMenifest();
			if (m != null && m.getId().equals(toolId)
					&& m.getVersion().equals(version))
				return c;
		}
		return null;
	}

	// classes

	// Note: will become framework class.
	public static class ToolConfigTableModelSortProxy extends WSortFilterProxyModel{

		public ToolConfigTableModelSortProxy(ToolConfigTableModel model) {
			setSourceModel(model);
			setDynamicSortFilter(true);
		}

		public ToolInfo getToolInfo(WModelIndex proxyIndex) {
			 return ((ToolConfigTableModel) getSourceModel()).
					 getToolInfo(mapToSource(proxyIndex).getRow());
		}

		public void refresh() {
			((ToolConfigTableModel) getSourceModel()).refresh();
		}
	}
}
