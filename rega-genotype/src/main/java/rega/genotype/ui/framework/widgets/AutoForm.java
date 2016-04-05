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

import eu.webtoolkit.jwt.WCheckBox;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WIntValidator;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WValidator;

/**
 * Simple editor for java class.
 * Automatically create a form that will edit the class fields.
 * 
 * @author michael
 * @param <T> class
 */
public class AutoForm <T> extends WTable{
	
	private Map<Field, WFormWidget> widgetMap = new HashMap<Field, WFormWidget>();
	private T t;

 	public AutoForm(T t){	
 		this.t = t;
 		
 		// add fields
 
		int row = 0;
		List<Field> fields = getFields();
		for (Field field : fields) {
			getElementAt(row, 0).addWidget(new WText(styleFiledName(field.getName())));
			if (field.getType() == String.class) {
				String value = (String)doGet(field, t);
				WLineEdit le = new WLineEdit(value == null ? "" : value);
				le.setValidator(new WValidator(true));
				le.setWidth(new WLength(300));
				getElementAt(row, 1).addWidget(le);
				widgetMap.put(field, le);
			} else if (field.getType() == boolean.class){
				Boolean value = (Boolean)doGet(field, t);
				WCheckBox chb = new WCheckBox();
				chb.setChecked(value);
				getElementAt(row, 1).addWidget(chb);
				widgetMap.put(field, chb);
			} else if (field.getType() == int.class){
				Integer value = (Integer)doGet(field, t);
				WLineEdit le = new WLineEdit(value == null ? "" : value.toString());
				le.setWidth(new WLength(300));
				WIntValidator v = new WIntValidator();
				v.setMandatory(true);
				le.setValidator(v);
				getElementAt(row, 1).addWidget(le);
				widgetMap.put(field, le);
			} else 
				System.err.println("WARNING: AutoForm encountered new field type: " + field.getType());

			row++;
		}
		// addValidators();
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
 		for (WFormWidget w: widgetMap.values()) 
 			if (w.validate() != WValidator.State.Valid)
 				return false;
 
 		return true;
 	}
 
	public boolean save() {	
		if (!validate())
			return false;

		List<Field> fields = getFields();
		for (Field field : fields) {
			if (field.getType() == String.class) {
				String value = widgetMap.get(field).getValueText();
				doSet(field, t, value);
			} else if (field.getType() == boolean.class) {
				boolean value = ((WCheckBox)widgetMap.get(field)).isChecked();
				doSet(field, t, value);
			} else if (field.getType() == int.class) {
				String value = widgetMap.get(field).getValueText();
				doSet(field, t, Integer.parseInt(value));
			}
		}

		return true;
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
}
