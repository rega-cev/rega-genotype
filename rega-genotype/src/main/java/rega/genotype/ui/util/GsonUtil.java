package rega.genotype.ui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
/**
 * Gson util
 * @author michael
 */
public class GsonUtil {
	public static <C> C parseJson(String json, Class<C> c) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.fromJson(json, c);
	}

	public static <C> String toJson(C c) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(c);
	}
}
