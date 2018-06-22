package edu.esu.syssoft.androidcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class AndroidController extends Activity {
	public static final String LOGTAG = "AndroidController";

	// All of these fields are accessed by the Worker thread
	// and the UI/Main thread. If it doesn't need to be in the outer class,
	// don't put it here!

	// Anywhere you would use "this" in a method call,
	// I'm using context. Because that makes my intention clearer.
	private Context context = this;

	// Control (start/stop) Flags for the main Thread.
	private boolean mainThreadExecuteFlag = true;
	private boolean mainThreadIsRunning = false;

	// Control flags for the picture return method.
	// This handles the race-condition that exists between the base64encode,
	// and pictured return.
	private boolean rearCameraFinished = false;
	private boolean frontCameraFinished = false;
	private boolean sensorRecordFinished = false;

	// Services
	private BluetoothService bluetoothService = null;
	private GPSHandler gpsHandler = null;
	private Sensors sensors = null;

	// These byte arrays should always have data,
	// However, if the picture code fails, they just go to null.
	private String backCameraPicturePath = null;
	private String frontCameraPicturePath = null;
	private String sensorDataPath = null;

	// Bluetooth Constants and Settings
	public static final String BTADDR_RASPBERRYPI = "B8:27:EB:F0:E5:F0";
	public static final String BTADDR_USBDONGLE = "5C:F3:70:76:D7:4B";
	private static final UUID UUID_BTDONGLE = UUID.fromString("00005005-0000-1000-8000-0002ee000001");
	private static final UUID UUID_BTRPI = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private BluetoothDevice targetDevice;

	private static final UUID targetUUID = UUID_BTRPI;
	private String targetAddress = BTADDR_RASPBERRYPI;
	private boolean bluetoothIsOn = false;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_android_controller);
	}

	// These methods handle the start and stop. No guarantees yet on actual usefulness
	// but they should keep the application from dieing if something hits during a critical
	// section.
	protected void onPause() {
		super.onPause();
		mainThreadExecuteFlag = false;
	}


	protected void onResume() {
		super.onResume();
		// Initialize the GPS handler Service. (And pray to Cthulhu it works.)
		// If the application get preempted, I don't know what happens to this.
		// I think, for just the onPause/onStop, the service will continue to run.
		// If the GPSHandler hasn't been made yet, Create it.
		if (gpsHandler == null) {
			gpsHandler = new GPSHandler(context);
		}
		if (bluetoothService == null) {
			bluetoothService = new BluetoothService(context, new Handler(), targetUUID);
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter.isEnabled()) {
				bluetoothIsOn = true;
				targetDevice = adapter.getRemoteDevice(targetAddress);
			}
			else {
				utils.msgoutput(context, "Bluetooth is disabled! We do nothing!");
			}


		}

		mainThreadExecuteFlag = true;

		// Each time, we make a new Looper thread. This is ugly, but saves
		// on having to restore state. In other words, I save on conditional statements.
		// If the other thread is still running though, we just let that one continue,
		if (!mainThreadIsRunning) {
			new AndroidControllerLoop().execute();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == pictured.CAMERA_BACK_REQUEST_CODE) {
			if(resultCode == Activity.RESULT_OK){
				backCameraPicturePath = data.getStringExtra(pictured.RETURN_PATH_INTENT_STRING);
			}
			else {
				utils.msgoutput(context, "Failed to get picture, check output.");
				backCameraPicturePath = null;
			}
			rearCameraFinished = true;
		}
		else if (requestCode == pictured.CAMERA_FRONT_REQUEST_CODE) {
			if(resultCode == Activity.RESULT_OK){
				frontCameraPicturePath = data.getStringExtra(pictured.RETURN_PATH_INTENT_STRING);
			}
			else {
				utils.msgoutput(context, "Failed to get picture, check output.");
				frontCameraPicturePath = null;
			}
			frontCameraFinished = true;
		}
		else if (requestCode == Sensors.SENSOR_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				sensorDataPath = data.getStringExtra(Sensors.RETURN_PATH_INTENT_STRING);
			}
			else {
				utils.msgoutput(context, "Sensors failed. No usable data");
				sensorDataPath = null;
			}
			sensorRecordFinished = true;
		}
	}

	private class AndroidControllerLoop extends AsyncTask<Void, Void, Void> {
		// This is nonnull. See my note about the JSONException!
		private String currentNMEALocation = "";
		private String base64BackImage = null;
		private String base64FrontImage = null;
		private File sensorData = null;

		private JSONObject outputObject = null;

		private static final int MAIN_LOOP_SLEEP_TIME = 15000;


		@Override
		protected Void doInBackground(Void... params) {

			mainThreadIsRunning = true;
			boolean resetTimer = false;
			DumbTimer timer = new DumbTimer();
			// While we can run, we run, run, run, so far away.
			while (mainThreadExecuteFlag) {
				utils.msgoutput(context, "Timer is at " + Long.toString(timer.getCurrentTime()));
				setCurrentNMEALocation();

				if(timer.getCurrentTime() == 30 || timer.getCurrentTime() == 60
						|| timer.getCurrentTime() == 120 || timer.getCurrentTime() == 150) {
					byte[] sensorbytes = null;
					String dataToPack = "";
					getSensorData();
					if (sensorData != null) {
						sensorbytes = utils.getBytesFromPath(sensorData.getAbsolutePath());
						if (sensorbytes != null) {
							dataToPack = new String(sensorbytes);
						} else {
							utils.msgoutput(context, "No bytes of sensor data. Writing empty!");
						}
					}
					outputObject = JSONPacker.Pack(currentNMEALocation, dataToPack);

				}
				else if (timer.getCurrentTime() == 90) {
					takeRearPicture();
					outputObject = JSONPacker.Pack(currentNMEALocation, "Rear Camera", base64BackImage);

				}
				else if (timer.getCurrentTime() == 180) {
					// If we get to 60 seconds, we need to reset the clock.
					takeFrontPicture();
					outputObject = JSONPacker.Pack(currentNMEALocation, "Front Camera", base64FrontImage);
					resetTimer = true;
				}
				else {
					utils.msgoutput(context, "Only collecting Location data");
					outputObject = JSONPacker.Pack(currentNMEALocation);
				}

				if (outputObject != null) {
					utils.msgoutput(context, "Trying to push JSON");
					System.out.println(outputObject.toString());

					if (bluetoothIsOn) {
						tryToConnectBluetooth();
						bluetoothService.write(outputObject.toString().getBytes());
						bluetoothService.stop();
					}
					else {
						utils.msgoutput(context, "Bluetooth is Off! Not writing.");
					}

				}

				utils. msgoutput(context, "Sleep section. (Safe zone)");
				utils.sleepWait(MAIN_LOOP_SLEEP_TIME);
				// These allows us to keep track of how long we've been going.
				// That way, we can send the pictured calls out on the right interval.

				timer.incrementSecondsOrReset(MAIN_LOOP_SLEEP_TIME / 1000, resetTimer);
				resetTimer = false;

			}

			return null;
		}

		protected void onPostExecute(Void aVoid) {
			mainThreadIsRunning = false;
		}

		// These methods could all be placed in the main loop.
		// They are seperated out to make it clear what portions of the tasks are
		// critical sections.

		private void takeRearPicture() {


			utils.msgoutput(context, "Starting pictured rear");
			Intent i = new Intent(context, pictured.class);
			i.putExtra(pictured.CAMERA_OPEN_CHOICE_STRING, Camera.CameraInfo.CAMERA_FACING_BACK);
			startActivityForResult(i, pictured.CAMERA_BACK_REQUEST_CODE);



			DumbTimer timoutTimer = new DumbTimer();
			while (!rearCameraFinished && timoutTimer.getCurrentTime() < 60) {
				utils.msgoutput(context, ("Waiting for Rear Camera to return. "
							+ Long.toString(timoutTimer.getCurrentTime())
							+ "s elapsed."));
				timoutTimer.tick();
				utils.sleepSecond();
			}
			utils.msgoutput(context, "base64ing rear image!");
			byte[] backCameraPicture = null;
			// This is not technically required, but avoids an NPE in getBytesFromPath
			if (backCameraPicturePath != null) {
				backCameraPicture = utils.getBytesFromPath(backCameraPicturePath);
			}
			base64BackImage = utils.safeBase64Encode(backCameraPicture);

			// Reset camera state for next time.
			rearCameraFinished = false;
		}
		private void takeFrontPicture() {

			utils.msgoutput(context, "Starting pictured front");
			Intent i = new Intent(context, pictured.class);
			i.putExtra(pictured.CAMERA_OPEN_CHOICE_STRING, Camera.CameraInfo.CAMERA_FACING_FRONT);
			startActivityForResult(i, pictured.CAMERA_FRONT_REQUEST_CODE);

			DumbTimer timoutTimer = new DumbTimer();
			while (!frontCameraFinished && timoutTimer.getCurrentTime() < 60) {
				utils.msgoutput(context, ("Waiting on Front Camera to return. "
							 + Long.toString(timoutTimer.getCurrentTime())
							+ "s elapsed."));
				timoutTimer.tick();
				utils.sleepSecond();
			}
			utils.msgoutput(context, "base64ing front image!");
			byte[] frontCameraPicture = null;
			// This is not technically required, but avoids an NPE in getBytesFromPath
			if (frontCameraPicturePath != null) {
				frontCameraPicture = utils.getBytesFromPath(frontCameraPicturePath);
			}
			base64FrontImage = utils.safeBase64Encode(frontCameraPicture);

			// Reset camera state for next time.
			frontCameraFinished = false;
		}

		private void setCurrentNMEALocation() {
			// If the JSON packer gets null, it throws an exception and breaks everything.
			// Therefore, we need to be careful that nothing is null. (I hate empty strings...)
			if (gpsHandler.getGPSStatus()) {
				utils.msgoutput(context, "The GPS is usable, and the service is on.");
				currentNMEALocation = gpsHandler.getNMEAString();
				if (currentNMEALocation != null) {
					utils.msgoutput(context, "Current Location:" + currentNMEALocation);
				}
				else {
					utils.msgoutput(context, "Location was null. There's issues in GPS land.");
					currentNMEALocation = "";
				}
			}
			else {
				utils.msgoutput(context, "The GPS is off or unusable.");
				currentNMEALocation = "";
			}

		}

		private void getSensorData(){
			utils.msgoutput(context, "Starting Sensor logger");
			Intent s = new Intent (context, Sensors.class);
			startActivityForResult(s, Sensors.SENSOR_REQUEST_CODE);

			DumbTimer timoutTimer = new DumbTimer();
			while (!sensorRecordFinished && timoutTimer.getCurrentTime() < 60) {
				utils.msgoutput(context, ("Waiting on Sensors to return. "
						+ Long.toString(timoutTimer.getCurrentTime())
						+ "s elapsed."));
				timoutTimer.tick();
				utils.sleepSecond();
			}
			if (sensorDataPath != null) {
				sensorData = new File(sensorDataPath);
			}
			else {
				sensorData = null;
				utils.msgoutput(context, "Failure in sensors!");
			}

			sensorRecordFinished = false;

		}

		private void tryToConnectBluetooth() {
			if (bluetoothService.getState() == BluetoothService.CONNECTION_STATE.STATE_NONE) {
				utils.msgoutput(context, "Starting BluetoothService");
				bluetoothService.start();
			}
			if (bluetoothService.getState() != BluetoothService.CONNECTION_STATE.STATE_CONNECTED) {
				bluetoothService.connect(targetDevice, false);
				DumbTimer timer = new DumbTimer();
				while (bluetoothService.getState() == BluetoothService.CONNECTION_STATE.STATE_CONNECTING && timer.getCurrentTime() <= 30) {
					utils.msgoutput(context, bluetoothService.getState().toString());
					timer.tick();
					utils.sleepSecond();
				}
			}
			if (bluetoothService.getState() == BluetoothService.CONNECTION_STATE.STATE_LISTEN) {
				utils.msgoutput(context, "Failed connection");
			}

		}

	}


}

