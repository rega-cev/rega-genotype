package rega.genotype.ui.framework.widgets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.webtoolkit.jwt.EventSignal1;
import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;

/**
 * Simple editor for java class.
 * Automatically create a simple form with fixed layout that will edit the class fields.
 * 
 * @author michael
 * @param <T> class
 */
public class AutoForm <T> extends WContainerWidget {
	private WText header = new WText(this); 
	private WTable layout = new WTable(this);
	private WPushButton saveB = new WPushButton("Save", this);
	private Map<Field, WidgetContainer> widgetMap = new HashMap<Field, WidgetContainer>();
	private T t;

 	public AutoForm(T t){	
 		this.t = t;
 		
 		layout.addStyleClass("auto-form-table");
 
 		// add fields
 
		int row = 0;
		List<Field> fields = getFields();
		for (Field field : fields) {
			WText info = new WText();
			info.addStyleClass("auto-form-info ");
			WText header = new WText();
			layout.getElementAt(row, 0).addWidget(header);
			layout.getElementAt(row, 0).setColumnSpan(5);
			row++;

			layout.getElementAt(row, 0).addWidget(new WText(styleFiledName(field.getName())));
			if (field.getType() == String.class) {
				String value = (String)doGet(field, t);
				WLineEdit le = new WLineEdit(value == null ? "" : value);
				le.setValidator(new WValidator(true));
				le.setWidth(new WLength(600));
				layout.getElementAt(row, 1).addWidget(le);
				widgetMap.put(field, new WidgetContainer(le, info, header));
			} else if (field.getType() == boolean.class){
				Boolean value = (Boolean)doGet(field, t);
				WCheckBox chb = new WCheckBox();
				chb.setChecked(value);
				layout.getElementAt(row, 1).addWidget(chb);
				widgetMap.put(field, new WidgetContainer(chb, info, header));
			} else if (field.getType() == int.class){
				Integer value = (Integer)doGet(field, t);
				WLineEdit le = new WLineEdit(value == null ? "" : value.toString());
				le.setWidth(new WLength(300));
				WIntValidator v = new WIntValidator();
				v.setMandatory(true);
				le.setValidator(v);
				layout.getElementAt(row, 1).addWidget(le);
				widgetMap.put(field, new WidgetContainer(le, info, header));
			} else 
				System.err.println("WARNING: AutoForm encountered new field type: " + field.getType());

			row++;
			
			layout.getElementAt(row, 0).addWidget(info);
			layout.getElementAt(row, 0).setColumnSpan(5);
			row++;

		}
		// addValidators();
 	}

 	public void setHeader(CharSequence text) {
 		header.setText(text);
 	}

 	private List<Field> getFields() {
 		List<Field> ans = new ArrayList<Field>();
		Field[] fields = t.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (!getIgnoredFields().contains(field.getName()))
				ans.add(field);
		}
		return ans;
 	}

 	private Field getField(String fieldName) {
 		for (Field field : getFields()) {
 			if (field.getName().equals(fieldName))
 				return field;
 		}
 
 		return null;
 	}
 
 	protected boolean setFieldInfo(String fieldName, String text) {
 		if (widgetMap.get(getField(fieldName)) == null)
 			return false;
 		else
 			widgetMap.get(getField(fieldName)).info.setText(text);

 		return true;
 	}

 	protected boolean setHeader(String fieldName, String text) {
 		if (widgetMap.get(getField(fieldName)) == null)
 			return false;
 		else
 			widgetMap.get(getField(fieldName)).header.setText(text);

 		return true;
 	}

 	/**
 	 * Display text for field names.
 	 * Default to styled java field name 
 	 * @return
 	 */
 	protected Map<String, String> getFieldDisplayNames() {
 		return new HashMap<String, String>();
 	}

//  protected boolean addValidators() {}
// 	protected boolean addValidator(WValidator v, String fieldName) {
// 		WFormWidget w = widgetMap.get(fieldName);
// 		if (w == null)
// 			return false;
// 		else
// 			w.setValidator(v);
// 
// 		return true;
//	}

 	private String styleFiledName(String name) {
 		Map<String, String> fieldDisplayNames = getFieldDisplayNames();
 		if (fieldDisplayNames.containsKey(name))
 			return fieldDisplayNames.get(name);
 		
 		String ans = "";
 		for (int i = 0; i < name.length(); ++i) {
 			if (i == 0){
 				ans += Character.toUpperCase(name.charAt(i));
 				continue;
 			} else if (Character.isUpperCase(name.charAt(i)))
 				ans += " ";

 			ans += name.charAt(i);
 		}

 		return ans;
 	}
 	
 	/**
 	 * define class fields that should not be modified by the UI.
 	 * @return
 	 */
 	protected Set<String> getIgnoredFields() {
 		return new HashSet<String>();
 	}
 	
 	public boolean validate() {
 		for (WidgetContainer c: widgetMap.values()) 
 			if (c.widget.validate() != WValidator.State.Valid)
 				return false;
 
 		return true;
 	}
 
	public boolean save() {	
		if (!validate())
			return false;

		List<Field> fields = getFields();
		for (Field field : fields) {
			if (field.getType() == String.class) {
				String value = widgetMap.get(field).widget.getValueText();
				doSet(field, t, value);
			} else if (field.getType() == boolean.class) {
				boolean value = ((WCheckBox)widgetMap.get(field).widget).isChecked();
				doSet(field, t, value);
			} else if (field.getType() == int.class) {
				String value = widgetMap.get(field).widget.getValueText();
				doSet(field, t, Integer.parseInt(value));
			}
		}

		return true;
	}

	public EventSignal1<WMouseEvent> saveClicked() {
		return saveB.clicked();
	}

	private Object doGet(Field field, T t){
		for (Method method : t.getClass().getMethods()){
			if (method.getName().startsWith("get")
					&& method.getName().length() == (field.getName().length() + 3)
					&& method.getName().toLowerCase().endsWith(field.getName().toLowerCase())){
				try{
					return method.invoke(t);
				}catch (IllegalAccessException e){
					e.printStackTrace();					
				}catch (InvocationTargetException e){
					e.printStackTrace();					
				}
			}
		}
		return null;
	}

	private boolean doSet(Field field, T t, Object value){
		for (Method method : t.getClass().getMethods()){
			if (method.getName().startsWith("set")
					&& method.getName().length() == (field.getName().length() + 3)
					&& method.getName().toLowerCase().endsWith(field.getName().toLowerCase())){
				try{
					method.invoke(t, value);
					return true;
				}catch (IllegalAccessException e){
					System.err.println("Error: AutoForm field: " + field.getName() + ", value: " + value);
					e.printStackTrace();
				}catch (InvocationTargetException e){
					System.err.println("Error: AutoForm field: " + field.getName() + ", value: " + value);
					e.printStackTrace();					
				}
			}
		}
		return false;
	}

	// classes

	private static class WidgetContainer {
		WFormWidget widget;
		WText info;
		WText header;
		public WidgetContainer(WFormWidget widget, WText info, WText header) {
			this.widget = widget;
			this.info = info;
			this.header = header;
		}
	}
}
