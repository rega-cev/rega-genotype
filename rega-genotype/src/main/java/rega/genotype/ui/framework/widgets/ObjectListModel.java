package rega.genotype.ui.framework.widgets;

import java.util.List;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WModelIndex;

public abstract class ObjectListModel<T> extends ObjectListModelBase<T> {

	public abstract Object render(T t);

	public ObjectListModel(T[] objects) {
		super(objects);
	}

	public ObjectListModel(List<T> objects) {
		super(objects);
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		if (role == ItemDataRole.DisplayRole && index.getColumn() == 0)  {
			return render(getObject(index.getRow()));
		} else 
			return super.getData(index, role);
	}
}
