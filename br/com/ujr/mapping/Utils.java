package br.com.ujr.mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Utils {
	
	static File FILE_STATE_MAPPING = new File("state.mapping");
	static Map<String,String> SAVED_STATE = new HashMap<String,String>();
	
	static {
		initSavedStateFile();
	}
	
	private static void initSavedStateFile() {
		if ( !FILE_STATE_MAPPING.exists() ) {
			FileWriter fw = null;
			try {
				fw = new FileWriter(FILE_STATE_MAPPING);
			} catch (FileNotFoundException e) {
				throw new RuntimeException();
			} catch (IOException e) {
				throw new RuntimeException();
			} finally {
				try {
					if ( fw != null ) {
						fw.flush();
						fw.close();
					}
				} catch (IOException e) {}
			}
		} else {
			FileReader fr = null;
			BufferedReader br = null;
			try {
				fr = new FileReader(FILE_STATE_MAPPING);
				br = new BufferedReader(fr);
				String line = br.readLine();
				while ( line != null ) {
					String[] l = line.split("=");
					String key = l[0];
					String vlr = l[1];
					SAVED_STATE.put(key,vlr);
					line = br.readLine();
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException();
			} catch (IOException e) {
				throw new RuntimeException();
			} finally {
				try {
					if ( fr != null ) fr.close();
					if ( br != null ) br.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static String getExtension(File f) {
		return f.getName().substring(f.getName().indexOf(".") + 1);
	}
	
	public static void saveLastAccessFolderForFile(String s) {
		SAVED_STATE.put("getLastAccessFolderForFile", s);
	}
	
	public static String getLastAccessFolderForFile() {
		String r = SAVED_STATE.get("getLastAccessFolderForFile");
		return r != null ? r : "";
	}
	
	public static void saveState() {
		FileWriter fo = null;
		try {
			fo = new FileWriter(FILE_STATE_MAPPING);
			for(String key : SAVED_STATE.keySet()) {
				fo.write(key + "=" + SAVED_STATE.get(key));
			}
		} catch (IOException e) {
			throw new RuntimeException();
		} finally {
			if ( fo != null ) {
				try {
					fo.flush();
					fo.close();
				} catch (IOException e) {
				}
			}
		}
		
	}
	

}
