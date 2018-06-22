package edu.esu.syssoft.androidcontroller;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;



import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera;
import android.net.Uri;

import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.lang.Thread;


public class pictured extends Activity implements SurfaceHolder.Callback {
	public static final String RETURN_PATH_INTENT_STRING = "syssoft.pictured.picturePath";
	public static final String CAMERA_OPEN_CHOICE_STRING = "syssoft.pictured.cameraToOpen";

	// I tried to use an Enum, like a sane language, but Java enums
	// are apparently not implicit ints.
	public static final int CAMERA_BACK_REQUEST_CODE = 0;
	public static final int CAMERA_FRONT_REQUEST_CODE = 1;

	private int CAMERA_TO_OPEN = 0;
	private int result = RESULT_OK;
	// This is the file that goes to the calling application.
	private String returnpath = null;


	// When the preview is running, this is true.
	// When the preview crashes, !true.
	private boolean CameraIsReady = false;

	private SurfaceView drawingsurface;
	private Camera CameraInUse = null;
	private CameraController cController = new CameraController();
	private Context context = this;
	private SurfaceHolder holder;


	// Android garbage for UI. Not touching for now.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle b = getIntent().getExtras();
		CAMERA_TO_OPEN = b.getInt(CAMERA_OPEN_CHOICE_STRING);

		utils.msgoutput(context, "On Create()");
		// This is the only important code in onCreate (From a service standpoint.)
		// This gets the object associated with the preview SurfaceView.
		// and adds it to a SurfaceHolder. This SurfaceHolder is actually this whole
		// class, the addCallback(this) fires the SurfaceCreated method.
		setContentView(R.layout.activity_pictured);
		drawingsurface = (SurfaceView)findViewById(R.id.drawing_surface);
		holder = drawingsurface.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

	// End default methods.

	// Constructors for SurfaceHolderCallback. These do nothing, because takePicture and jpegCallback do the work for us.

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
				   int height) {
		//CAMERA HANDLES THIS METHOD
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		utils.msgoutput(context, "surfaceCreated()");

		// CameraController remains an AsyncTask.
		// AsyncTasks handle InterruptedExceptions.
		// Basically, using an AsyncTask for this allows me to have assurance
		// finish() is called, only after this has returned.
		try {
			cController.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
			utils.msgoutput(context, "Main pictured thread already running. Closing");
			finish();
		}


	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		utils.msgoutput(context, "surfaceDestroyed()");
		// Everything can burn.
		// The camera has to be released regardless.

		// It's not really important, but we need to make sure that the flag is off,
		// before we try to stop the preview. This is a side effect of threading.\
		// I'm catching the exeception anyway, but the less bug vectors the better.
		CameraIsReady = false;

		try {
			CameraInUse.stopPreview();
			CameraInUse.release();

			utils.msgoutput(context, "Released camera and Preview stopped");
		}
		catch (NullPointerException e) {
			e.printStackTrace();
			utils.msgoutput(context, "Camera was null on Surface Destroyed.");
		}
		catch (RuntimeException e) {
			e.printStackTrace();
			utils.msgoutput(context, "Camera already released!");
		}


	}


	// See arch document.
	// In brief, this class replaces the previous Runnable based loop,
	// with a series of blocking calls
	// This subclass will close the activity when the picture code has executed.
	// An AsyncTask call taks <Params, Progress, and Return> types.
	private class CameraController extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			CameraInit CInit = new CameraInit();
			Thread CInitThread = new Thread(CInit, "CameraInitThread");
			CInitThread.start();
			try {
				CInitThread.join();
			}
			catch (InterruptedException e) {
				// Basically empty catch block, because I can't do anything if
				// my wait is interrupted.
				e.printStackTrace();
				CameraIsReady = false;
			}

			if (CameraIsReady) {
				TakePictureLooper PictLoop = new TakePictureLooper();
				Thread PictLoopThread = new Thread(PictLoop, "PictureLooper");
				PictLoopThread.start();
				try {
					PictLoopThread.join();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				utils.msgoutput(context, "Failed to Initialize the Camera, dieing.");
			}

			// This is a special case of Java being braindead. Abbreviated, Void is an
			// object you can't make, but still an object. Therefore, I need to return a
			// reference to an imaginary fake object.
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			// When this loop is done, we quit and send data upwards.
			if (returnpath == null) {
				result = RESULT_CANCELED;
				utils.msgoutput(context, "The output file was null. We failed!");
			}
			utils.msgoutput(context, "Leaving pictured");
			Intent data = new Intent();
			data.putExtra(RETURN_PATH_INTENT_STRING, returnpath);
			setResult(result, data);
			// We would set mainthread back to true here, if this was a
			// self-contained activity. Because a new activity should be made each time,
			// we don't, to prevent weird race conditions involved with the finish() call
			finish();
		}
	}

	private class CameraInit implements Runnable {

		@Override
		public void run() {
			CameraIsReady = previewInit();
			if (CameraIsReady) {
				CameraInUse.startPreview();
			}
			else {
				utils.msgoutput(context, "Camera is not ready, Major Issue.");
			}

		}

		// This method starts the preview. If all goes well, then life is sunshine and rainbows.
		// Otherwise, all hades comes forth, and everybody dies.
		private boolean previewInit() {
			boolean exitflag = false;
			boolean opened = false;
			utils.msgoutput(context, "NumberOfCameras = " + Camera.getNumberOfCameras());
			if (Camera.getNumberOfCameras() > 0) {
				utils.msgoutput(context, "Trying to open the Camera!");

				try {
					CameraInUse = Camera.open(CAMERA_TO_OPEN);
					// If we throw up at the open call, we need to stop!
					// It is feasible, but not likely, for the Camera to be defined,
					// but useless. This prevents that.
					opened = true;
				}
				catch (RuntimeException e) {
					utils.msgoutput(context, "Failed to open camera.");
					e.printStackTrace();
				}

				// If the Camera object is empty, or the holder doesn't exist, we're shot.
				if (CameraInUse != null && holder != null && opened) {
					try {
						utils.msgoutput(context, "Camera opened");
						CameraInUse.setPreviewDisplay(holder);



						exitflag = true;
					} catch (Exception e) {
						e.printStackTrace();
						utils.msgoutput(context, "Camera crashed opening the Preview");
						// I used to release the camera here, but that propagated weirdly up.
						// If the camera is trashed, we need to finish() correctly.

					}

				}
				else {
					//booo, failed!
					if (CameraInUse == null) {
						utils.msgoutput(context, "CameraInUse is null! (Does that Camera exist?)");
					}
					if (holder == null) {
						utils.msgoutput(context, "holder is null!");
					}
				}
			}
			else {
				utils.msgoutput(context, "No Cameras! Nothing to do. Dying!");
			}

			return exitflag;
		}

	}

	private class TakePictureLooper implements Runnable {
		boolean exitflag = true;
		boolean callbackFault = false;

		public void run() {
			if ( CameraInUse != null) {
				exitflag = capturePicture();
				if (exitflag) {
					// This line is surprisingly important.
					// Continued output to the user keeps Android from thinking
					// the program is hung.
					utils.msgoutput(context, ("Took picture "));

					DumbTimer timer = new DumbTimer();
					while (!callbackFault && timer.getCurrentTime() < 30 && !CameraIsReady) {
						timer.tick();
						utils.sleepSecond();
					}

				}
				else {
					utils.msgoutput(context, "Error in TakePicture, Quitting!");
					}
			}
			utils.msgoutput(context, "Picture thread finished, returning control.");
		}


		private boolean capturePicture() {

			boolean exitflag = true;
			if (CameraIsReady) {
			try {
				CameraInUse.takePicture(null, null, jpegCallback);
				CameraIsReady = false;
			}
			catch (Exception e) {
				e.printStackTrace();
				utils.msgoutput(context, "Take picture threw an Exception. Stopping the loop");
				exitflag = false;
			}
			}
			else {
				utils.msgoutput(context, "Camera was not ready when we were.");
				exitflag = false;
			}

			return exitflag;
		}

		// Callback method for the Camera. This is a class attribute (Just a huge one.)
		// So I'm putting it up here.
		private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
			// Set the quality for the JPEG image. We'll go super compressed for spacesaving.
			private static final int JPEGQUALITY = 20;
			public void onPictureTaken(byte[] data, Camera camera){
				utils.msgoutput(context, "In onPictureTaken()");
				if (data != null) {
					Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
					utils.msgoutput(context, "Camera data is NOT null");
					if (bitmap != null) {
						utils.msgoutput(context, "bitmap data is NOT null");
						File dir = new File(Environment.getExternalStorageDirectory() + "/pictured");
						if (!dir.isDirectory()) {
							dir.mkdir();
						}

						File file = new File(dir, ("pictured - " + utils.getCurrentTimeAsString() + ".jpg"));
						try {
							utils.msgoutput(context, "Trying file save!");
							FileOutputStream fileOutputStream = new FileOutputStream(file);
							bitmap.compress(Bitmap.CompressFormat.JPEG, JPEGQUALITY, fileOutputStream);
							fileOutputStream.flush();
							fileOutputStream.close();
							updateGallery(file);
							returnpath = file.getAbsolutePath();
						} catch (Exception e) {
							e.printStackTrace();
						}

					}

				}

				//At the end of the Callback, we can restart the preview.
				// After the preview is running, we can throw the Camera flag back to true.
				try {
					CameraInUse.startPreview();
					CameraIsReady = true;
				}
				catch (RuntimeException e) {
					e.printStackTrace();
					callbackFault = true;
					utils.msgoutput(context, "Could not restart Preview. Something has probably interrupted us");

				}

			}
		};


		private void updateGallery(File file) {
			// This refreshes the Gallery.
			Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			mediaScanIntent.setData(Uri.fromFile(file));
			sendBroadcast(mediaScanIntent);
			utils.msgoutput(context, "Updated gallery");
		}
	}


	}
//END CLASS
