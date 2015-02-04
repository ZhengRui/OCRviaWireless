package com.example.bluestream;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static String TAG = "BlueStream: ";
    private SurfaceView mPreview;
    private SurfaceHolder mPreviewHolder;
    private Camera mCamera;
    private boolean mInPreview = false;
    private boolean mCameraConfigured = false;
    private ArrayList<String> pairedDev;
    private ArrayList<String> mArrayAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread btThread;
    private ConnectedThread btcThread;
	
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
     
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            Method m = null;
     
            Log.i(TAG, "Inside ConnectThread.");
            
            // check uuid of remote devices
            /*try {
            //	mmDevice.fetchUuidsWithSdp();
            	m = mmDevice.getClass().getMethod("getUuids", null);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            ParcelUuid[] uuids = null;
			try {
				uuids = (ParcelUuid[]) m.invoke(mmDevice, null);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            for (ParcelUuid uuid: uuids) {
                Log.i(TAG, "UUID: " + uuid.getUuid().toString());
            }*/
            
            
            // Get a BluetoothSocket to connect with the given BluetoothDevice

            // way 1: create socket through uuid
            // http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
            try {
			//	tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            	tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("1e0ca4ea-299d-4335-93eb-27fcfe7fa848"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            	
            // way 2: create socket through reflection
            /*try {
            	//mmDevice.fetchUuidsWithSdp();
				m = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
				tmp = (BluetoothSocket) m.invoke(mmDevice, 11);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

            mmSocket = tmp;
            Log.i(TAG, "Create socket done!");

        }
     
        public void run() {
            // Cancel discovery because it will slow down the connection
            // mBluetoothAdapter.cancelDiscovery();
            
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception          	
            	
            	Log.i(TAG, "Start to connect with bluetooth to " + mmSocket.getRemoteDevice().getName());
            	
                mmSocket.connect();
                Log.i(TAG, "Connection via bluetooth done!");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
            	
            	Log.e(TAG, "connection error msg: " , connectException);
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
     
            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            
            btcThread = new ConnectedThread(mmSocket);
            btcThread.start();
            
        }
     
        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
        	Log.i(TAG, "cancel() in connect thread.");
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
     
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
     
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
     
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
     
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
     
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                //    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                //            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
     
        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            //    mmOutStream.flush();
            } catch (IOException e) { }
        }
     
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
        	Log.i(TAG, " cancel() in connectED thread called.");
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, " onCreate() called.");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mPreview = (SurfaceView)findViewById(R.id.preview);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        
     // enable bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
        	Log.i(TAG, "Device does not support Bluetooth.");
        } else {
        	
        	pairedDev = new ArrayList<String>();
        	mArrayAdapter = new ArrayList<String>();
        	
	        if (!mBluetoothAdapter.isEnabled()) {
	            mBluetoothAdapter.enable();
	            Log.i(TAG, "Bluetooth enabled.");
	        }
	        
	        
	        
	        /*Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
	        // If there are paired devices
	        if (pairedDevices.size() > 0) {
	            // Loop through paired devices
	            for (BluetoothDevice device : pairedDevices) {
	                // Add the name and address to an array adapter to show in a ListView
	            	Log.i(TAG, "Find paired device: " + device.getName() + " " + device.getAddress());
	                pairedDev.add(device.getName() + "\n" + device.getAddress());

	                if (device.getName().equals("ubuntu-0"))
	                {
	                	ConnectThread btThread = new ConnectThread(device);
	                }
	            }
	        }*/
	        
	        // target my laptop directly through address and connect it
	        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("70:F3:95:4E:D2:C5");
	        Log.i(TAG, device.getName());
	        btThread = new ConnectThread(device);
	        btThread.start();
	        
	     //   Log.i(TAG, "hello " + (btcThread == null));
	        int i=0;
	        while (btcThread == null && i < 10) {
	        	try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	i++;
	        }
	        
	        if(btcThread != null) btcThread.write("Connection established, 1st message sent :) ".getBytes());
	        
	        /*   
	        // After pair ubuntu 0 with XT1033, don't need to discovery anymore
	        // sdptool browse local    can be used to check binded local service in laptop
	        mBluetoothAdapter.startDiscovery();

	        // Register the BroadcastReceiver
	        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
	        */
	        
        }
        
	}

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated( SurfaceHolder holder ) 
        {
        	Log.i(TAG, " surfaceCreated() called.");
        }

        public void surfaceChanged( SurfaceHolder holder, int format, int width, int height ) 
        {
        	Log.i(TAG, " surfaceChanged() called.");
            initPreview(width, height);
            startPreview();
        }

        public void surfaceDestroyed( SurfaceHolder holder ) 
        {
        	Log.i(TAG, " surfaceDestroyed() called.");
        }
    };
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(TAG, "OpenCV loaded successfully");
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
		}
	};
    
	boolean tstfrm = true;
    Camera.PreviewCallback frameIMGProcCallback = new Camera.PreviewCallback() {
		
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
		//	Log.i(TAG, String.valueOf(data.length));
			
			if(btcThread != null) {
				tstfrm = false;
				int dataHeight = 720;
				int dataWidth = 1280;
				int scale = 3;
				Mat YUVMat = new Mat(dataHeight + dataHeight / 2, dataWidth, CvType.CV_8UC1);				// resize using opencv4android
			    YUVMat.put(0, 0, data);																		// method = 1 in server
			    Mat BGRAMat = new Mat(dataHeight, dataWidth, CvType.CV_8UC1);
			    Imgproc.cvtColor(YUVMat, BGRAMat, Imgproc.COLOR_YUV420sp2GRAY);
			    Mat BGRAMat_LowRes = new Mat(dataHeight / scale, dataWidth / scale, CvType.CV_8UC1);
			    Imgproc.resize(BGRAMat, BGRAMat_LowRes, BGRAMat_LowRes.size(), 0, 0, Imgproc.INTER_LINEAR);
			    byte[] data_LowRes = new byte[(int) (BGRAMat_LowRes.total() * BGRAMat_LowRes.channels())];
			    BGRAMat_LowRes.get(0, 0, data_LowRes);
				btcThread.write(data_LowRes);
				
			//	btcThread.write(data);															// raw YUV420sp frame, method = 0 in server
			//	btcThread.write(Arrays.copyOfRange(data, 0, dataHeight * dataWidth));			// raw gray frame, method = 2 in server
			}
		}
	};
    
    private void initPreview(int width, int height) 
    {
        if ( mCamera != null && mPreviewHolder.getSurface() != null) {
            try 
            {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.setPreviewCallback(frameIMGProcCallback);
            }
            catch (Throwable t) 
            {
                Log.e(TAG, "Exception in initPreview()", t);
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }

            if ( !mCameraConfigured ) 
            {   
            	Camera.Parameters params = mCamera.getParameters();
            	Size size = mCamera.getParameters().getPreviewSize();
            	Log.i(TAG, "Preview size: "+ size.width + ", " + size.height);
            	params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            	mCamera.setParameters(params);
                mCameraConfigured = true;
            }
        }
    }
    
    private void startPreview() 
    {
        if ( mCameraConfigured && mCamera != null ) 
        {
            mCamera.startPreview();
            mInPreview = true;
        }
    }
    
    @Override
    public void onResume() 
    {
    	Log.i(TAG, " onResume() called.");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        mCamera = Camera.open();
        startPreview();
    }

    @Override
    public void onPause() 
    {
    	Log.i(TAG, " onPause() called.");
        if ( mInPreview )
            mCamera.stopPreview();
        
        mCamera.setPreviewCallback(null);
        
        mCamera.release();
        mCamera = null;
        mInPreview = false;
        super.onPause();
    }
    
    /*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/
    
    
 // Create a BroadcastReceiver for ACTION_FOUND
 // hcitool dev -- find my laptop's bluetooth address
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.i(TAG, "Find bluetooth device: " + device.getName() + " " + device.getAddress());
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    @Override
     protected void onDestroy() {
    	Log.i(TAG, " onDestroy() called.");
    //	unregisterReceiver(mReceiver);
    	if (btcThread != null)	btcThread.cancel();
    	super.onDestroy();
     }


    
    
}


