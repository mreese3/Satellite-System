package edu.esu.syssoft.androidcontroller;

import android.graphics.Bitmap;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

// This class is a series of Pack methods, which return JSONObjects.

public class JSONPacker {

	public static String createFile(JSONObject jsonObject) {
		String path = "";
		File dir = new File(Environment.getExternalStorageDirectory() + "/AndroidController");
		if (!dir.isDirectory()) {
			dir.mkdir();
		}

		File file = new File(dir, ("json - " + utils.getCurrentTimeAsString() + ".json"));

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			// Write JSONObject to file, same as you would in utils.msgoutput()
			fileOutputStream.write((jsonObject.toString().getBytes()));
			fileOutputStream.flush();
			fileOutputStream.close();
			path = file.getAbsolutePath();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return path;
	}

	public static JSONObject Pack(String nmea, String cameraName, String base64Picture) {
		JSONObject retObject = new JSONObject();
		try {

			retObject.put("Timestamp", utils.getCurrentTimeAsString());
			retObject.put("NMEALocation", nmea);
			retObject.put("CameraName", cameraName);
			retObject.put("JpegImage", base64Picture);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return retObject;
	}
	public static JSONObject Pack(String nmea) {
		JSONObject retObject = new JSONObject();
		try {
			retObject.put("Timestamp", utils.getCurrentTimeAsString());
			retObject.put("NMEALocation", nmea);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return retObject;
	}

	public static JSONObject Pack(String nmea, String sensorData) {
		JSONObject retObject = new JSONObject();
		try {
			retObject.put("Timestamp", utils.getCurrentTimeAsString());
			retObject.put("NMEALocation", nmea);
			retObject.put("Sensor Data", sensorData);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return retObject;
	}
}
