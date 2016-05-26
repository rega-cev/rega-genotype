package rega.genotype.ui.admin.config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.ToolManifest;
import rega.genotype.singletons.Settings;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WSortFilterProxyModel;

public class ToolConfigTableModel extends WAbstractTableModel {
	public static final int URL_COLUMN = 0;
	public static final int NAME_COLUMN = 1;
	public static final int ID_COLUMN = 2;
	public static final int VERSION_COLUMN = 3;
	public static final int DATE_COLUMN = 4;
	public static final int PUBLISHER_COLUMN = 5;
	public static final int STATE_COLUMN = 6;
	public static final int UPTODATE_COLUMN = 7;
	public static final int INSTALLED_COLUMN = 8;

	String[] headers = { "URL", "Name", "ID", "Version", "Publication date", "Publisher" , "State", "Up to date", "Installed"};
	List<ToolInfo> rows = new ArrayList<ToolInfo>();
	private List<ToolManifest> localManifests;
	private List<ToolManifest> remoteManifests;

	public enum ToolState {
		Local {           // The tool is not published 
			@Override
			String str() {return "Local";}
		}, RemoteSync {   // local and remote are in sync
			@Override
			String str() {return "Installed";}
		}, RemoteNotSync { // published but not installed.
			@Override
			String str() {return "Published";}
		}, Retracted { // published then installed and then removed.
			@Override
			String str() {return "Retracted";}
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
		refresh(localManifests, remoteManifests);
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
			switch (index.getColumn()) {
			case URL_COLUMN:					
				return info.getConfig() == null ? null : info.getConfig().getPath();
			case NAME_COLUMN:
				return info.getManifest() == null ? null : info.getManifest().getName();
			case ID_COLUMN:
				return info.getManifest() == null ? null : info.getManifest().getId();
			case VERSION_COLUMN:
				return info.getManifest() == null ? null : info.getManifest().getVersion();
			case DATE_COLUMN:
				return info.getManifest() == null ? null : formtDate(info.getManifest().getPublicationDate());
			case PUBLISHER_COLUMN:
				return info.getManifest() == null ? null : info.getManifest().getPublisherName();
			case STATE_COLUMN:
				return info.getState() == ToolState.Retracted ? "Retracted" : 
					info.getState() == ToolState.Local ? "Local" : "Published";
			case UPTODATE_COLUMN:
				if (info.getManifest() == null || info.getManifest().getId() == null)
					return "No";
				else 
					return isUpToDate(info.getManifest().getId()) ? "Yes" : "No";
			case INSTALLED_COLUMN:
					return info.getState() != ToolState.RemoteNotSync ? "Yes" : "No";
			default:
				break;
			}
		} else if (role == ItemDataRole.EditRole) {
			switch (index.getColumn()) {
			case DATE_COLUMN:
				return info.getManifest() == null ? null : formtDate(info.getManifest().getPublicationDate());
			default:
				return getData(index, ItemDataRole.DisplayRole);
			}
		} else if (role == ItemDataRole.LinkRole) {
			if (index.getColumn() == 0 && info.getConfig() != null)
				return new WLink("typingtool/" + info.getConfig().getPath());
		} else if (role == ItemDataRole.StyleClassRole) {
			if (info.getState() == ToolState.RemoteSync 
					|| info.getState() == ToolState.Local
					|| info.getState() == ToolState.Retracted)
				return "";
			else
				return "tools-table-unistalled-raw";
		}
		return null;
	}

	private String formtDate(Date date) {
		if (date == null)
			return null;

		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		return  format.format(date);
	}

	public void refresh(List<ToolManifest> localManifests,
			List<ToolManifest> remoteManifests) {
		
		this.localManifests = localManifests;
		this.remoteManifests = remoteManifests;

		rows.clear();
		Config config = Settings.getInstance().getConfig();
		// add locals
		for (ToolManifest m: localManifests) {
			ToolInfo info = new ToolInfo();
			ToolConfig localConfig = find(m.getId(), m.getVersion(), config);
			info.setConfig(localConfig);
			info.setManifest(m);
			// ToolManifest published = find(m.getId(), m.getVersion(), remoteManifests);
			if (localConfig == null)
				info.setState(ToolState.Local);
			else {
				if (localConfig.isRetracted())
					info.setState(ToolState.Retracted);
				else if (localConfig.isPublished())
					info.setState(ToolState.RemoteSync);
				else
					info.setState(ToolState.Local);
			}
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

		layoutAboutToBeChanged().trigger();
		layoutChanged().trigger();
	}

	public ToolInfo getToolInfo(int row) {
		 return rows.get(row);
	}

	public ToolInfo getToolInfo(String toolId, String toolVersion) {
		 for (ToolInfo info: rows) {
			 if (info.manifest.getId().equals(toolId)
					 && info.manifest.getVersion().equals(toolVersion))
				 return info;
		 }

		 return null;
	}
	
	private ToolManifest find(String toolId, String version, 
			List<ToolManifest> manifests) {
		// TODO!: this can not cover the case of a user that creates a tool with 
		// the same id, version as an existing tool in the repo. 
		if (manifests != null)
			for (ToolManifest m: manifests) {
				if (m.getId().equals(toolId) && m.getVersion().equals(version))
					return m;
			}
		return null;
	}

	private ToolConfig find(String toolId, String version, Config config) {
		for (ToolConfig c: config.getTools()) {
			ToolManifest m = c.getToolMenifest();
			if (m != null && m.getId().equals(toolId)
					&& m.getVersion().equals(version))
				return c;
		}
		return null;
	}

	public boolean isUpToDate(String toolId) {
		ToolConfig locaLastPublished = Settings.getInstance().getConfig().
				getLastPublishedToolConfig(toolId);
		return locaLastPublished == null || ToolManifest.isLastPublishedVesrsion(
				remoteManifests, locaLastPublished.getToolMenifest());
	}

	public boolean isLastPublishedVesrsion(int row) {
		ToolInfo info = getToolInfo(row);
		Date publicationDate = info.getManifest().getPublicationDate();
		if (info.getState() == ToolState.Local || publicationDate == null)
			return true; // not published yet
		else {
			for (ToolInfo otherInfo: rows) {
				if (otherInfo.getState() != ToolState.Local
						&& otherInfo.getManifest().getId().equals(info.getManifest().getId())
						&& otherInfo.getManifest().getPublicationDate() != null
						&& otherInfo.getManifest().getPublicationDate().compareTo(publicationDate) > 0)
					return false;
			}
		}
		return true;
	}

	
	// classes

	// Note: will become framework class.
	public static class ToolConfigTableModelSortProxy extends WSortFilterProxyModel{
		private boolean filterOldVersion = true;
		private boolean filterRemote;
		
		public ToolConfigTableModelSortProxy() {
			setSortRole(ItemDataRole.EditRole);
		}

		@Override
		protected boolean filterAcceptRow(int sourceRow,
				WModelIndex sourceParent) {
			if (filterRemote) {
				ToolInfo info = getToolConfigTableModel().getToolInfo(sourceRow);
				if (info.getState() == ToolState.RemoteNotSync)
					return false;
			} if (filterOldVersion) {
				if(!getToolConfigTableModel().isLastPublishedVesrsion(sourceRow))
					return false;
			}  
			return super.filterAcceptRow(sourceRow, sourceParent);
		}

		public ToolConfigTableModelSortProxy(ToolConfigTableModel model) {
			setSourceModel(model);
			setDynamicSortFilter(true);
		}

		public ToolConfigTableModel getToolConfigTableModel() {
			return (ToolConfigTableModel) getSourceModel();
		}

		public ToolInfo getToolInfo(WModelIndex proxyIndex) {
			 return getToolConfigTableModel().
					 getToolInfo(mapToSource(proxyIndex).getRow());
		}

		public void refresh(
				List<ToolManifest> localManifests,
				List<ToolManifest> remoteManifests) {
			getToolConfigTableModel().refresh(
					localManifests, remoteManifests);
		}

		public boolean isFilterOldVersion() {
			return filterOldVersion;
		}

		public void setFilterOldVersion(boolean filterOldVersion) {
			this.filterOldVersion = filterOldVersion;
			invalidate();
		}

		public void setFilterNotRemote(boolean filterRemote) {
			this.filterRemote = filterRemote;
			invalidate();
		}
	}
}
