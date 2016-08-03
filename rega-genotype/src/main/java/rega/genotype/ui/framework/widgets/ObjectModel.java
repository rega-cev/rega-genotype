package rega.genotype.ui.framework.widgets;

import java.util.Collection;
import java.util.SortedSet;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WModelIndex;

public interface ObjectModel<T> {
	public static final int EntityRole = ItemDataRole.UserRole + 100;
	public T getObject(int row);	
	public int indexOfObject(T object);
	Collection<T> getObjects(SortedSet<WModelIndex> indexSet);
	Iterable<T> getObjects();
	public void refresh();
}
