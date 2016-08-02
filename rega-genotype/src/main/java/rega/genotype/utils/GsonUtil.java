package rega.genotype.utils;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
/**
 * Gson util
 * @author michael
 */
public class GsonUtil {
	public static <C> C parseJson(String json, Class<C> c) {
		GsonBuilder builder = new GsonBuilder().setPrettyPrinting().serializeNulls();
		Gson gson = builder.create();
		return gson.fromJson(json, c);
	}

	public static <T> List<T> parseJson(String json, Type t) {
		GsonBuilder builder = new GsonBuilder().setPrettyPrinting().serializeNulls();
		Gson gson = builder.create();

		try {
			return gson.fromJson(json, t);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <C> String toJson(C c) {
		GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
		Gson gson = builder.create();
		return gson.toJson(c);
	}
}
