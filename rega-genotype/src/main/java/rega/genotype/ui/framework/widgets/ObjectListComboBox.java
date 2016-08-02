package rega.genotype.ui.framework.widgets;

import java.util.List;

import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WString;

public abstract class ObjectListComboBox<T> extends WComboBox {
	private ObjectListModel<T> model;

	public ObjectListComboBox(List<T> objects) {
		this.model = new ObjectListModel<T>(objects) {
			@Override
			public WString render(T t) {
				return ObjectListComboBox.this.render(t);
			}
			
		};
		this.setModel(model);
	}

	abstract protected WString render(T t);

	@Override
	public ObjectListModel<T> getModel() {
		return model;
	}
	
	public void setModel(ObjectListModel<T> model) {
		this.model = model;
		super.setModel(model);
	}

	public T getCurrentObject(){
		return model.getObject(getCurrentIndex());
	}

	public void setCurrentObject(T object){
		setCurrentIndex(model.indexOfObject(object));
	}
	
	public void setNullSelectable(boolean b) {
		model.setNullSelectable(b);
	}
}
