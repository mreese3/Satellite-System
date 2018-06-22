package edu.esu.syssoft.androidcontroller;

import android.content.Context;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.RandomAccessFile;
import java.util.Locale;

public class utils {

	// Log a message, and try to show it as well. This can be called from subthreads,
	// but will not display a toast on those threads..
	public static void msgoutput(Context context, String text) {
		Log.v(AndroidController.LOGTAG, text);

		// If this is true, then we are supposed to be on a UI thread.
		// If it's not, then we can't use Toast.
		if (Looper.myLooper() == Looper.getMainLooper()) {
			try {
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			} catch (RuntimeException e) {
				e.printStackTrace();
				Log.v(AndroidController.LOGTAG, "Exception toasting from msgoutput. Most likely not UI thread.");
			}
		}
	}



	// Snarky comments aside, this does exactly what it says on the tin.
	public static String getCurrentTimeAsString() {
		// This could be one-lined. but I like to be able to read
		// my code later. Thanks AndroidStudio, for reminding me
		// why the IOCCC exists.
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.ENGLISH);
		Date today = new Date();
		return (df.format(today));
	}


	// This method only exists because exception handling is a pain in the but, and makes
	// simple code cumbersome to read.
	public static void sleepWait(long ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e){
			e.printStackTrace();
		}
	}

	public static void sleepSecond() {
		sleepWait(1000);
	}


	public static String safeBase64Encode(byte[] bytes) {
		if (bytes != null) {
			// Default is RFC 2045 Compliant Base64.
			 return Base64.encodeToString(bytes, Base64.DEFAULT);
		}
		else {
			return "";
		}
	}

	// Exceptions are important, but not something I can do anything to in the main
	// Therefore, if garbage is passed into here, (which it shouldn't), we just silently
	// return null. Have fun!
	public static byte[] getBytesFromPath(String path) {
		byte[] bytes;
		try {
			RandomAccessFile randomAccessFile = new RandomAccessFile(path, "r");
			bytes = new byte[((int) randomAccessFile.length())];
			randomAccessFile.read(bytes);
		}
		catch (Exception e) {
			// This is a rare case where it makes sense, and is safe,
			// to run a catch everything exception. Nothing I do at this point
			// will affect the execution of the main, since null checking is already]
			// in place.
			e.printStackTrace();
			return null;
		}
		return bytes;

	}
}
