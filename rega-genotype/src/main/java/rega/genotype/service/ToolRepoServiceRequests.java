package rega.genotype.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import rega.genotype.ui.util.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.utils.StreamUtils;

public class ToolRepoServiceRequests {

	public static class ToolRepoServiceExeption extends Exception{
		private static final long serialVersionUID = -7810905835977427799L;
		public ToolRepoServiceExeption(String msg) {
			super(msg);
		}
	}
	
	private static String generatePasswiord() {
		//TODO: Koen ??
		return "TODO";
	}

	public static boolean publish(final File zipFile) {
		URLConnection connection;
		try {
			connection = new URL(ToolRepoService.getReqPublishUrl()).openConnection();
			connection.setDoOutput(true); // Triggers POST.
			//connection.setRequestProperty("Accept-Charset", "UTF-8");
			//connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + "UTF-8");
			connection.setRequestProperty("Content-Type", "multipart/form-data"); // Allow to add a file
			connection.setRequestProperty(ToolRepoService.TOOL_PWD_PARAM, generatePasswiord());
			StreamUtils.copy(new FileInputStream(zipFile), 
					connection.getOutputStream());

			// Note: can throw java.io.FileNotFoundException if the server did not respond.
			InputStream response = connection.getInputStream();

			BufferedReader in = new BufferedReader(new InputStreamReader(response));
			String decodedString;
			while ((decodedString = in.readLine()) != null) {
				System.out.println(decodedString);
			}
			in.close();

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Requests the server for the manifests of all published tools.
	 * 
	 * @return manifests json string or null if did not work.
	 */
	public static String getManifests() {
		// TODO: maybe some users are interested only in current versions on a tool?
		URLConnection connection;
		String ans = null;
		try {
			connection = new URL(ToolRepoService.getReqManifestsUrl()).openConnection();
			connection.setDoOutput(true); // Triggers POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data"); // Allow to add a file
			connection.setRequestProperty(ToolRepoService.TOOL_PWD_PARAM, generatePasswiord());

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write("");
			writer.flush();
			
			// Note: can throw java.io.FileNotFoundException if the server did not respond.
			InputStream response = connection.getInputStream();

			// read manifests 
			BufferedReader in = new BufferedReader(new InputStreamReader(response));
			ans = FileUtil.toString(in);
			in.close();

		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return ans;
	}

	/**
	 * Ask the service for a specific tool.
	 * @param toolId
	 * @param toolVersion
	 * @return temp zip file with resived data
	 * @throws ToolRepoServiceExeption
	 * @throws IOException
	 */
	public static File getTool(String toolId, String toolVersion) throws ToolRepoServiceExeption, IOException {
		// TODO: add to config .. 
		File ans = new File(Settings.getInstance().getXmlDir(toolId, toolVersion));
		if (ans.exists()) {
			throw new ToolRepoServiceExeption("Tool: " + toolId + " version: " + toolVersion + " exists on local server.");
		} 
		URLConnection connection;

		connection = new URL(ToolRepoService.getReqToolUrl(toolId, toolVersion)).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		connection.setRequestProperty("Content-Type", "multipart/form-data"); // Allow to add a file
		connection.setRequestProperty(ToolRepoService.TOOL_PWD_PARAM, generatePasswiord());

		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write("");
		writer.flush();
		
		// Note: can throw java.io.FileNotFoundException if the server did not respond.
		InputStream response = connection.getInputStream();
		
		ans = File.createTempFile("tool", ".zip");

		FileOutputStream out = new FileOutputStream(ans);
		StreamUtils.copy(response, out);
		out.close();

		return ans;
	}

}
