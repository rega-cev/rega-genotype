package rega.genotype.ui.framework.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WModelIndex;

public abstract class ObjectListModelBase<T> extends WAbstractTableModel implements ObjectModel<T> {
	protected List<T> objects;
	protected boolean nullSelectable = false;

	public ObjectListModelBase(List<T> objects) {
		this.objects = objects;
	}

	public ObjectListModelBase(T[] objects) {
		this.objects = Arrays.asList(objects);
	}

	public void addObject(T object){
		addObject(object, objects.size());
	}

	public void addObject(T object, int index){
		rowsAboutToBeInserted().trigger(null, index, index);
		if(nullSelectable)
			objects.add(index - 1, object);
		else
			objects.add(index, object);
		rowsInserted().trigger(null, index, index);
	}

	public boolean removeObject(T object){
		int index = indexOfObject(object);
		boolean ans = false;

		rowsAboutToBeRemoved().trigger(null, index, index);
		ans = objects.remove(object);
		rowsRemoved().trigger(null, index, index);

		return ans;
	}

	public T removeObject(int index){
		T ans;
		rowsAboutToBeRemoved().trigger(null, index, index);
		if(nullSelectable)
			ans = objects.remove(index - 1);
		else
			ans = objects.remove(index);
		rowsRemoved().trigger(null, index, index);

		return ans;
	}

	//Return ordered list of the removed objects.
	public List<T> removeObjects(Set<Integer> indexes){
		List<T> ans = new ArrayList<T>();
		TreeSet<Integer> sortedIndexes = new TreeSet<Integer>(indexes);
		for(Integer i: sortedIndexes.descendingSet()){
			ans.add(0, removeObject(i));
		}

		return ans;
	}

	public void moveObject(int from, int to){
		T obj = removeObject(from);
		addObject(obj, to);
	}

	public void clear(){
		rowsAboutToBeRemoved().trigger(null, 0, objects.size());
		objects.clear();
		rowsRemoved().trigger(null, 0, objects.size());
	}

	public void setObjects(List<T> newObjects){
		clear();

		if (newObjects == null || newObjects.isEmpty())
			return;

		rowsAboutToBeInserted().trigger(null, 0, newObjects.size());
		this.objects = new ArrayList<T>(newObjects);
		rowsInserted().trigger(null, 0, getRowCount());
	}

	@Override
	public int getColumnCount(WModelIndex parent) {
		if (parent == null)
			return 1;
		else
			return 0;
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		if (role == ObjectModel.EntityRole) {
			if(nullSelectable)
				return objects.get(index.getRow() + 1);
			return objects.get(index.getRow());
		} else
			return null;
	}


	@Override
	public int getRowCount(WModelIndex parent) {
		if(nullSelectable)
			return objects.size() + 1 ;
		return objects.size();
	}

	public T getObject(int row){
		if (row >= 0) {
			if(nullSelectable) {
				if(row == 0) return null;
				return objects.get(row - 1);
			}
			return objects.get(row);
		}
		else
			return null;
	}

	public int indexOfObject(T object) {
		int start = 0;
		if(nullSelectable)
			start = 1;
		if (object != null) {
			for (int row = start; row < objects.size() + start; ++row)
				// search object
				if (getObject(row) == null && object == null)
					return row;
				else if (getObject(row) != null && object != null
						&& getObject(row).equals(object))
					return row;
		}			
		// object not found OR null - select no row (the -1 row).
		return -1;
	}

	public List<T> getObjectsList() {
		return objects;
	}
	
	public Collection<T> getObjects() {
		return getObjectsList();
	}

	public boolean containsObject(T object){
		return objects.contains(object);
	}

	public Collection<T> getObjects(SortedSet<WModelIndex> indexSet) {
		Set<T> result = new HashSet<T>();
		for (WModelIndex i : indexSet)
			result.add(getObject(i.getRow()));
		return result;
	}

	public void refresh() {
		layoutAboutToBeChanged().trigger();
		layoutChanged().trigger();
	}
	
	public void setNullSelectable(boolean b) {
		this.nullSelectable = b;
	}
}
