package edu.esu.syssoft.androidcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import java.io.InputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {
    public enum CONNECTION_STATE {STATE_NONE, STATE_LISTEN, STATE_CONNECTING, STATE_CONNECTED};
    private Context context = null;
    private Handler handler = null;


	// Pre-enumerated constants from BluetoothChat.Constants.java
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE=3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	private static final String DEVICE_NAME = "device_name";
	private static final String TOAST = "toast";
    /**
     * This class does all the work for setting up and managing Bluetooth
     * connections with other devices. It has a thread that listens for
     * incoming connections, a thread for connecting with a device, and a
     * thread for performing data transmissions when connected.
     */

        // Debugging
        private static final String TAG = "AC-BluetoothService";

        // Name for the SDP record when creating server socket
        private static final String NAME_SECURE = "BluetoothSecure";
        private static final String NAME_INSECURE = "BluetoothInsecure";

        // Generic UUIDs for RFCOMM sockets.



        // THIS MUST MATCH THE SERVICE in sdptool browse local for RFCOMM
        // rctest to recieve.
        // rctest -P 24 -r -U 00005005-0000-1000-8000-0002ee000002 50:A4:C8:53:3B:1B
        private UUID MY_UUID_INSECURE = null;

        private UUID MY_UUID_SECURE = null;

        // Member fields
        private final BluetoothAdapter mAdapter;
        private final Handler mHandler;
        private AcceptThread mSecureAcceptThread;
        private AcceptThread mInsecureAcceptThread;
        private ConnectThread mConnectThread;
        private ConnectedThread mConnectedThread;
        private CONNECTION_STATE mState;

	private String localName = null;



        public BluetoothService(Context c, Handler handler, UUID targetUUID) {
                context = c;
                MY_UUID_INSECURE = targetUUID;
                MY_UUID_SECURE = MY_UUID_INSECURE;
            	mAdapter = BluetoothAdapter.getDefaultAdapter();
		        localName = mAdapter.getName();
            	mState = CONNECTION_STATE.STATE_NONE;
            	mHandler = handler;
        }

        private synchronized void setState(CONNECTION_STATE state) {
            Log.d(TAG, "setState() " + mState + " -> " + state);
            mState = state;

            // Give the new state to the Handler so the UI Activity can update
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state.ordinal(), -1).sendToTarget();
        }

        /**
         * Return the current connection state.
         */
        public synchronized CONNECTION_STATE getState() {
            return mState;
        }

        /**
         * Start the chat service. Specifically start AcceptThread to begin a
         * session in listening (server) mode. Called by the Activity onResume()
         */
        public synchronized void start() {
            Log.d(TAG, "start");

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            setState(CONNECTION_STATE.STATE_LISTEN);
            utils.msgoutput(context, "Setting State to listen in start()");
/*
            // Start the thread to listen on a BluetoothServerSocket
            if (mSecureAcceptThread == null) {
                mSecureAcceptThread = new AcceptThread(true);
                mSecureAcceptThread.start();
            }
            if (mInsecureAcceptThread == null) {
                mInsecureAcceptThread = new AcceptThread(false);
                mInsecureAcceptThread.start();
            } */

        }


        //Start the ConnectThread to initiate a connection to a remote device.
        public synchronized void connect(BluetoothDevice device, boolean secure) {
            Log.d(TAG, "connect to: " + device);
            // Cancel any thread attempting to make a connection
            if (mState == CONNECTION_STATE.STATE_CONNECTED) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(device, secure);
            utils.msgoutput(context, "Starting ConnectThread");
            mConnectThread.start();
            setState(CONNECTION_STATE.STATE_CONNECTING);
        }

        /**
         * Start the ConnectedThread to begin managing a Bluetooth connection
         *
         * @param socket The BluetoothSocket on which the connection was made
         * @param device The BluetoothDevice that has been connected
         */
        public synchronized void connected(BluetoothSocket socket, BluetoothDevice
                device, final String socketType) {
            Log.d(TAG, "connected, Socket Type:" + socketType);

            // Cancel the thread that completed the connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Cancel the accept thread because we only want to connect to one device
            if (mSecureAcceptThread != null ) {
                mSecureAcceptThread.cancel();
                mSecureAcceptThread = null;
            }
            if (mInsecureAcceptThread != null) {
                mInsecureAcceptThread.cancel();
                mInsecureAcceptThread = null;
            }

            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(socket, socketType);
            mConnectedThread.start();

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(CONNECTION_STATE.STATE_CONNECTED);
        }

        /**
         * Stop all threads
         */
        public synchronized void stop() {
            Log.d(TAG, "stop");

            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            if (mSecureAcceptThread != null) {
                mSecureAcceptThread.cancel();
                mSecureAcceptThread = null;
            }

            if (mInsecureAcceptThread != null) {
                mInsecureAcceptThread.cancel();
                mInsecureAcceptThread = null;
            }
            setState(CONNECTION_STATE.STATE_NONE);
        }

        /**
         * Write to the ConnectedThread in an unsynchronized manner
         *
         * @param out The bytes to write
         * @see ConnectedThread#write(byte[])
         */
        public void write(byte[] out) {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != CONNECTION_STATE.STATE_CONNECTED){
                    utils.msgoutput(context, "Not connected to anything! Can't write.");
                    return;
                }
                r = mConnectedThread;
            }
            // Perform the write unsynchronized
            r.write(out);
        }

        /**
         * Indicate that the connection attempt failed and notify the UI Activity.
         */
        private void connectionFailed() {
            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(TOAST, "Unable to connect device");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            // Start the service over to restart listening mode
            BluetoothService.this.start();
        }

        /**
         * Indicate that the connection was lost and notify the UI Activity.
         */
        private void connectionLost() {
            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(TOAST, "Device connection was lost");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            // Start the service over to restart listening mode
            BluetoothService.this.start();
        }

        /**
         * This thread runs while listening for incoming connections. It behaves
         * like a server-side client. It runs until a connection is accepted
         * (or until cancelled).
         */
        private class AcceptThread extends Thread {
            // The local server socket
            private final BluetoothServerSocket mmServerSocket;
            private String mSocketType;

            public AcceptThread(boolean secure) {
                BluetoothServerSocket tmp = null;
                mSocketType = secure ? "Secure" : "Insecure";

                // Create a new listening server socket
                try {
                    if (secure) {
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                                MY_UUID_SECURE);
                    } else
                    {
                        tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                                NAME_INSECURE, MY_UUID_INSECURE);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
                }
                mmServerSocket = tmp;
            }

            public void run() {
                Log.d(TAG, "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this);
                setName("AcceptThread" + mSocketType);

                BluetoothSocket socket = null;

                // Listen to the server socket if we're not connected
                while (mState != CONNECTION_STATE.STATE_CONNECTED) {
                    try {
                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        socket = mmServerSocket.accept();
                    } catch (IOException e) {
                        Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                        break;
                    }

                    // If a connection was accepted
                    if (socket != null) {
                        synchronized (BluetoothService.this) {
                            switch (mState) {
                                case STATE_LISTEN:
                                case STATE_CONNECTING:
                                    // Situation normal. Start the connected thread.
                                    connected(socket, socket.getRemoteDevice(),
                                            mSocketType);
                                    break;
                                case STATE_NONE:
                                case STATE_CONNECTED:
                                    // Either not ready or already connected. Terminate new socket.
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Could not close unwanted socket", e);
                                    }
                                    break;
                            }
                        }
                    }
                }
                Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

            }

            public void cancel() {
                Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
                }
            }
        }


        /**
         * This thread runs while attempting to make an outgoing connection
         * with a device. It runs straight through; the connection either
         * succeeds or fails.
         */
        private class ConnectThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final BluetoothDevice mmDevice;
            private String mSocketType;

            public ConnectThread(BluetoothDevice device, boolean secure) {
                mmDevice = device;
                BluetoothSocket tmp = null;
                mSocketType = secure ? "Secure" : "Insecure";

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    if (secure) {
                        tmp = device.createRfcommSocketToServiceRecord(
                                MY_UUID_SECURE);
                    } else {
                        tmp = device.createInsecureRfcommSocketToServiceRecord(
                                MY_UUID_INSECURE);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                }
                mmSocket = tmp;
            }

            public void run() {
                Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
                setName("ConnectThread" + mSocketType);

                // Always cancel discovery because it will slow down a connection
                mAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    utils.msgoutput(context, "Trying to connect");
                    mmSocket.connect();
                    utils.msgoutput(context, "left mmSocket.connect().");
                } catch (IOException e) {
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() " + mSocketType +
                                " socket during connection failure", e2);
                    }
                    connectionFailed();
                    return;
                }

                // Reset the ConnectThread because we're done
                synchronized (BluetoothService.this) {
                    mConnectThread = null;
                }

                // Start the connected thread
                connected(mmSocket, mmDevice, mSocketType);
            }

            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
                }
            }
        }

        /**
         * This thread runs during a connection with a remote device.
         * It handles all incoming and outgoing transmissions.
         */
        private class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket, String socketType) {
                Log.d(TAG, "create ConnectedThread: " + socketType);
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the BluetoothSocket input and output streams
                try {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "temp sockets not created", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                Log.i(TAG, "BEGIN mConnectedThread");
                byte[] buffer = new byte[1024];
                int bytes;

                // Keep listening to the InputStream while connected
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.read(buffer);

                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                        connectionLost();
                        // Start the service over to restart listening mode
                        BluetoothService.this.start();
                        break;
                    }
                }
            }

            /**
             * Write to the connected OutStream.
             *
             * @param buffer The bytes to write
             */
            public void write(byte[] buffer) {
                try {
                    mmOutStream.write(buffer);

                    // Share the sent message back to the UI Activity
                    mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                }
            }

            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "close() of connect socket failed", e);
                }
            }
        }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
