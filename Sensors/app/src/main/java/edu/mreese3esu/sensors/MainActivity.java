package edu.mreese3esu.sensors;

import android.app.ListActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends ListActivity implements SensorEventListener {

    //global variables
    public static File myData = null;
    public static File myDataCollection = null;
    public SensorEventListener mSensorListener ;
    public SensorManager sensorManager;
    public List<Sensor> listSensor;
    private  Sensor mAccelerometer, mAmbientTemp, mGravity, mGyroscope,
                    mLinearAcceleration, mMagFld, mPressure, mRelativeHumid, mRotVect;





    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //creating Folder on device

        String filename = "Sensors.txt";
        String filename2 = "Data.txt";
        File Output = new File(this.getFilesDir(), filename);
        File Output2 = new File(this.getFilesDir(),filename2);

        myData = new File(String.valueOf(Output));
        try{
            if(!myData.exists()){
                myData.createNewFile();
            }
        }catch(IOException ioExp){
            Log.d("AndroidSensorList::", "error in file creation");
        }

        myDataCollection = new File(String.valueOf(Output2));
        try{
            if(!myDataCollection.exists()){
                myDataCollection.createNewFile();
            }
        }catch(IOException ioExp){
            Log.d("AndroidSensorList::", "error in file creation");
        }

        //instance sensor manager
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
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
        for(int i=0; i<listSensor.size(); i++){
            System.out.println("Inside list sensors:::::::");
            listSensorType.add((i+1)+" "+listSensor.get(i).getName());
            String sensorNames = listSensor.get(i).getName();
            System.out.println(listSensor.get(i).getType());
            writeToFile(listSensor.get(i).getName().getBytes(),sensorNames );

        }

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listSensorType));
        getListView().setTextFilterEnabled(true);
    }


    private void writeToFile(byte[] data, String sensorNames) {
        System.out.println("----------------Inside writeToFile-----------------");

        try {
            String comma = "\n";
            byte[] bComma = comma.getBytes();
            OutputStream fo = new FileOutputStream(myData,true);
            fo.write(bComma);
            fo.write(data);
            fo.close();

        }
        catch (IOException e) {
            Log.e("AndroidSensorList::","File write failed: " + e.toString());
        }

    }


    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mAccelerometer,  SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mAmbientTemp,  SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mGravity,  SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mMagFld,  SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mPressure,   SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mRelativeHumid, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, mRotVect,  SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(mSensorListener);
        super.onPause();

    }





    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }





    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        System.out.println("++++++++++++++++INSIDE onSensorChanged() ++++++++++++++++++++++");
        //System.out.println("sensorName:"+sensorName);
        System.out.println("event.sensor.getName():"+event.sensor.getName());
        float x,y,z;

        x=event.values[0];
        y=event.values[1];
        z=event.values[2];
        writeDataTofile(event.sensor.getName(),x,y,z);

    }
    public void writeDataTofile(String sensorsName, float x, float y, float z){

        System.out.println(sensorsName + "::" + "X=" + x + "Y=" + y + "Z=" + z);

        String xVal= String.valueOf(x);
        String yVal= String.valueOf(y);
        String zVal= String.valueOf(z);
        byte[] bX_Value= xVal.getBytes();
        byte[] bY_Value= yVal.getBytes();
        byte[] bZ_Value= zVal.getBytes();
        String newLine = "\n";
        byte[] bnewLine = newLine.getBytes();
        String sSeparator="||";
        byte[] bSeparator=sSeparator.getBytes();
        byte[] bSensorName = sensorsName.getBytes();
        try{
            OutputStream fo = new FileOutputStream(myDataCollection,true);
            fo.write(bnewLine);
            fo.write(bSensorName);
            fo.write(bX_Value);
            fo.write(bSeparator);
            fo.write(bY_Value);
            fo.write(bSeparator);
            fo.write(bZ_Value);
            fo.write(bnewLine);
            fo.close();
        }catch(IOException e){
            Log.e("AndroidSensorList::","File write failed: " + e.toString());
        }
    }
}