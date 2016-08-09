package rega.genotype.ui.framework.widgets;

import java.util.HashSet;
import java.util.Set;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WSortFilterProxyModel;
import eu.webtoolkit.jwt.WStandardItemModel;

/**
 * Show items that contain searchText or there ancestors contain it. 
 */
public class StandardItemModelSearchProxy extends WSortFilterProxyModel {
	private String searchText = "";

	private Set<WModelIndex> visibleSrcIndexes = new HashSet<WModelIndex>();
	private Set<String> visibleLeafs;
	private int filterRole = ItemDataRole.UserRole + 1;

	public StandardItemModelSearchProxy(WStandardItemModel srcModel){
		setSourceModel(srcModel);
		setDynamicSortFilter(true);
	}
	
	// methods:
	
	/**
	 * Show items that contain searchText or there ancestors contain it. 
	 */
	public void setSearchText(String text){
		this.searchText = text;
		
		visibleSrcIndexes.clear();
		for (int r = 0; r < getSourceModel().getRowCount(); ++r)
			filter(getSourceModel().getIndex(r, 0));
		
		invalidate();
	}

	private boolean hasValidText(WModelIndex index) {
		if (visibleLeafs != null) {
			Object taxonomyId = getSourceModel().getData(index, filterRole);
			if (!visibleLeafs.contains(taxonomyId))
				return false;
		}
		return getSourceModel().getData(index).toString().toUpperCase().
				contains(searchText.toUpperCase());
	}

	private void filter(WModelIndex index) {
		if (index.getChild(0, 0) == null) {// leaf
			if (hasValidText(index)){
				visibleSrcIndexes.add(index);
				while (index.getParent() != null){
					index = index.getParent();
					visibleSrcIndexes.add(index);
				}
			}
		} else {			
			for (int r = 0; r < getSourceModel().getRowCount(index); ++r){
				filter(index.getChild(r, 0));
			}
		}
	}
	
	@Override
	protected boolean filterAcceptRow(int sourceRow, WModelIndex sourceParent) {
		WModelIndex srcIndex = getSourceModel().getIndex(sourceRow, 0, sourceParent);
		if (srcIndex == null)
			return true;

		return visibleSrcIndexes.contains(srcIndex);
	}

	public Set<String> getVisibleLeafs() {
		return visibleLeafs;
	}

	public void setVisibleLeafs(Set<String> visibleLeafs) {
		this.visibleLeafs = visibleLeafs;
	}

	public int getFilterRole() {
		return filterRole;
	}

	public void setFilterRole(int filterRole) {
		this.filterRole = filterRole;
	}
}
