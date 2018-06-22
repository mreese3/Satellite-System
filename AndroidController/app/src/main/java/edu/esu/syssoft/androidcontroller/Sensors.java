package edu.esu.syssoft.androidcontroller;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

public class Sensors extends ListActivity implements SensorEventListener {

    //global variables
    public static final String RETURN_PATH_INTENT_STRING = "syssoft.Sensors.Collection";
    public static final int SENSOR_REQUEST_CODE = 2;
    public static File myData = null;
    public static File myDataCollection = null;
    public SensorEventListener mSensorListener;
    public SensorManager sensorManager;
    public List<Sensor> listSensor;
    private Sensor mAccelerometer, mAmbientTemp, mGravity, mGyroscope,
            mLinearAcceleration, mMagFld, mPressure, mRelativeHumid, mRotVect;
    private int result = RESULT_OK;

    // Prevent spamming the dickens out of the ADB when Sensors does not have focus.
    private boolean currentlyRunning = false;
    private boolean safeToClose = true;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //creating Folder on device
        String filetime = utils.getCurrentTimeAsString();
        //String filename = "Sensors-" + filetime;
        String filename2 = "SensorsCollection" + filetime;

        File dir = new File(Environment.getExternalStorageDirectory() + "/Sensors");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }


        //File Output = new File(dir, filename);
        File Output2 = new File(dir, filename2);
        /*
        myData = new File(String.valueOf(Output));
        try {
            if (!myData.exists()) {
            myData.createNewFile();
            }
        } catch (IOException ioExp) {
            Log.d("AndroidSensorList::", "error in file creation");
        }
        */

        myDataCollection = new File(String.valueOf(Output2));
        try {
            if (!myDataCollection.exists()) {
                myDataCollection.createNewFile();
            }
        } catch (IOException ioExp) {
            Log.d("AndroidSensorList::", "error in file creation");
            result = RESULT_CANCELED;
            setResultAndDie();
        }

        //instance sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //generate list of all sensors on device we can use
        listSensor = sensorManager.getSensorList(Sensor.TYPE_ALL);
        //assign to list sensors we will be using
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAmbientTemp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        //usable, but not necessarily useful sensors below
        mGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMagFld = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRelativeHumid = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mRotVect = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Creating List view and writing data to a file

        List<String> listSensorType = new ArrayList<String>();
        for (int i = 0; i < listSensor.size(); i++) {
            //System.out.println("Inside list sensors:::::::");
            listSensorType.add((i + 1) + " " + listSensor.get(i).getName());
            //String sensorNames = listSensor.get(i).getName();
            //System.out.println(listSensor.get(i).getType());
            //writeToFile(listSensor.get(i).getName().getBytes(), sensorNames);

        }

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listSensorType));
        getListView().setTextFilterEnabled(true);
        Handler handler = new Handler();
        handler.postDelayed(
                new Runnable() {
                    public void run() {
                        while (!safeToClose) {
                            utils.sleepSecond();
                        }
                        setResultAndDie();
                    }
                }, 2000);

    }

    public void setResultAndDie() {
        currentlyRunning = false;
        Intent data = new Intent();
        data.putExtra(RETURN_PATH_INTENT_STRING, (myDataCollection.getAbsolutePath()));
        setResult(result, data);
        finish();
    }

    // Is this method required? I have no idea...
    /* private void writeToFile(byte[] data, String sensorNames) {
        System.out.println("----------------Inside writeToFile-----------------");

        try {
            String comma = "\n";
            byte[] bComma = comma.getBytes();
            OutputStream fo = new FileOutputStream(myData, true);
            fo.write(bComma);
            fo.write(data);
            fo.close();

        } catch (IOException e) {
            Log.e("AndroidSensorList::", "File write failed: " + e.toString());
            result = RESULT_CANCELED;
        }

    } */


    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mAmbientTemp, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mMagFld, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mRelativeHumid, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mRotVect, SensorManager.SENSOR_DELAY_NORMAL);
        currentlyRunning = true;

    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(mSensorListener);
        super.onPause();
        currentlyRunning = false;

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        //System.out.println("++++++++++++++++INSIDE onSensorChanged() ++++++++++++++++++++++");
        float x, y, z;

        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        writeDataTofile(event.sensor.getName(), x, y, z);

    }

        public void writeDataTofile(String sensorsName, float x, float y, float z) {

            String xVal = String.valueOf(x);
            String yVal = String.valueOf(y);
            String zVal = String.valueOf(z);
            byte[] bX_Value = xVal.getBytes();
            byte[] bY_Value = yVal.getBytes();
            byte[] bZ_Value = zVal.getBytes();
            String newLine = "\n";
            byte[] bnewLine = newLine.getBytes();
            String sSeparator = "||";
            byte[] bSeparator = sSeparator.getBytes();
            byte[] bSensorName = sensorsName.getBytes();
            try {
                safeToClose = false;
                OutputStream fo = new FileOutputStream(myDataCollection, true);
                fo.write(utils.getCurrentTimeAsString().getBytes());
                fo.write(bnewLine);
                fo.write(bSensorName);
                fo.write(bX_Value);
                fo.write(bSeparator);
                fo.write(bY_Value);
                fo.write(bSeparator);
                fo.write(bZ_Value);
                fo.write(bnewLine);
                fo.close();
            } catch (IOException e) {
                safeToClose = true;
                Log.e("AndroidSensorList::", "File write failed: " + e.toString());
                result = RESULT_CANCELED;
                setResultAndDie();
            }
            safeToClose = true;
        }
    }


