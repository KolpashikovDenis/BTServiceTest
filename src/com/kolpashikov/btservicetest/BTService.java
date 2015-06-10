package com.kolpashikov.btservicetest;

/*
 *  1 1 1 1  1 1 1 1  1 1 1 1  1 1 1 1 
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

public class BTService extends Service {	
	final String uuid = "993d463c-d711-11e4-b9d6-1681e6b88ec1";
	final String LOG = "mLogService";
	final String LIFECYCLE = "LifeCycle";
	static final String BROADCAST_ACTION = "com.kolpashikov.btservicetest";
	static final String BROADCAST_MSG = "message";
	static final String BROADCAST_STATE = "connectionstate";
		
	public static final int CONNECTION_ABORTED = 1;
	public static final int CONNECTION_ESTABLISHED = 2;
	
	static final int SERVICE_STARTED = 0x10;
	static final int SERVICE_STOPPED = 0x20;
	public static final int NONE_SUPPORTED = 0x00000000;
	public static final int GSM_SUPPORTED  = 0x00000001;
	public static final int CDMA_SUPPORTED = 0x00000002;
	public static final int GPS_SUPPORTED  = 0x00000004;
	public static final int AGPS_SUPPORTED = 0x00000008;
	public static final int WIFI_SUPPORTED = 0x00000010;
	public static final int BT_SUPPORTED   = 0x00000020;	
	
	BluetoothAdapter btAdapter;
	BluetoothServerSocket btServerSocket;
	BluetoothSocket btSocket;
	int mStartId;
	MainThread t;
	
	public boolean isServiceRunning = false; 
	/*
	 * Эта переменная содержит некоторую информацию об устройстве
	 */
	static int DEVICE_INFORMATION = 0;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		String s = "BTService service: onBind";
		Log.d(LOG, s);
		
		return null;
	}
	
	public void onCreate(){
		super.onCreate();
		String s = "BTService service: onCreate";
		Log.d(LOG, s);
		btAdapter = BluetoothAdapter.getDefaultAdapter();		
	}
	
	public int onStartCommand(Intent intent, int flags, int startId){
		String s = "BTService service: onStartCommand";
		
		Log.d(LOG, s);
		mStartId = startId;
		/* TODO: Собираем информацию об устройстве имеется ли GSM-модуль, 
		 * Bluetooth-модуль, GPS\GLONASS-модуль и т.д.
		 */
		PackageManager pm = getApplicationContext().getPackageManager();
		if(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
			DEVICE_INFORMATION = DEVICE_INFORMATION & GSM_SUPPORTED;
		
		t = new MainThread(startId, uuid);
		t.start();
		isServiceRunning = true;
		
		Intent i = new Intent(BROADCAST_ACTION);
		i.putExtra(BROADCAST_STATE, SERVICE_STARTED);
		sendBroadcast(i);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void onDestroy(){
		super.onDestroy();
		String s = "BTService service: onDestroy";
		Log.d(LOG, s);
		isServiceRunning = false;
		t.stopThread();
		Intent i = new Intent(BROADCAST_ACTION);
		i.putExtra(BROADCAST_STATE, SERVICE_STOPPED);
		sendBroadcast(i);
		//stopSelf(mStartId);		
	}

/***********************************************************
 *  
 * Вложенные классы, MainThread extends Thread
 * 
**************************************************************/
	public class MainThread extends Thread{
		
		int startId;
		UUID uuid;
		String deviceName;
				
		InputStream inStream;
		OutputStream outStream;
		int bytes;
		byte[] buffer;
		
		private boolean isRunning = true;
		boolean isSocketConnected = false;
		
		public MainThread(int _startId, String _uuid){
			startId = _startId;
			uuid = UUID.fromString(_uuid);
			deviceName = btAdapter.getName();
			
			BluetoothServerSocket tmp = null;
			try{
				tmp = btAdapter.listenUsingRfcommWithServiceRecord("AndroidBluetoothAdmin", uuid);
				buffer = new byte[1024];
				Log.d(LOG, "BTService, from thread: constructor");
				isRunning = true;			
			}catch(IOException e){
				isRunning = false;
				tmp = null;
			}
			btServerSocket = tmp;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(LOG, "From thread: start run");
			Intent intentMsg = new Intent(BROADCAST_ACTION);
			
			while(isRunning){
				try{
					if(btServerSocket != null){
						btSocket = btServerSocket.accept();
						Log.d(LOG, deviceName + " btServerSocket accepted");
						isSocketConnected = true;
						try{
							inStream = btSocket.getInputStream();
							outStream = btSocket.getOutputStream();
							/* TODO: Здесь реализовать отправку информации об устройстве
							 * поключившемуся клиенту, дабы показать, что может данное устройство
							 * имеется ли GSM-модуль, Bluetooth-модуль, GPS\GLONASS-модуль и т.д.
						     */
							while(isRunning){
								bytes = inStream.read(buffer);
								String s = new String(buffer, 0, bytes);
								intentMsg.putExtra(BROADCAST_STATE, CONNECTION_ESTABLISHED);
								intentMsg.putExtra(BROADCAST_MSG, s);
								sendBroadcast(intentMsg);
								processMsg(s);
								Log.d(LOG, deviceName + ", from BTService: " + s);
							}				
						}catch(IOException e){
							// TODO: Реализовать посылку сообщения о разрыве коннекта в MainActivity
							isSocketConnected = false;
							Intent intent = new Intent(BROADCAST_ACTION);
							intent.putExtra(BROADCAST_MSG, e.getMessage());
							intent.putExtra(BROADCAST_STATE, CONNECTION_ABORTED);
							sendBroadcast(intent);
						}
					}				
				}catch(IOException e){ }				
			}// while(true)...
			stopSelf();
			Log.d(LOG, deviceName + "BTService from thread: run stop");
		}// public void run()
		
		void processMsg(String msg){
			if(msg.contains("Vibrate")){
				long[] pattern = {1000, 2000, 4000, 8000, 16000};
				Vibrator vib = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);				
				vib.vibrate(pattern, 0);
				vib.vibrate(1000);
				try{
					TimeUnit.MILLISECONDS.sleep(1000);
				}catch(InterruptedException e){	}
				vib.cancel();
			} else if(msg.contains("GetContacts")){
				
			} else if(msg.contains("GetHistory")){
				
			} else if(msg.contains("Beep")){
				MediaPlayer m = new MediaPlayer();
				try{
					AssetFileDescriptor descriptor = getAssets().openFd("sound01.mp3");
					m.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
							descriptor.getLength());
					descriptor.close();
					
					m.prepare();
					m.setLooping(false);
					m.start();
				}catch(Exception e){ }
			}
			
		}
		
		public void write(byte[] buffer){
			try{				
				outStream.write(buffer);
			}catch(IOException e){}
			Log.d(LOG, deviceName + "BTService from thread: write bytes");
		}
		
		public void stopThread(){			
			isRunning = false;
			try{
				if(isSocketConnected){
					String s = "close";
					write(s.getBytes());				
					btSocket.close();		
				}
				btServerSocket.close();
			}catch(IOException e){
				Log.d(LOG, deviceName + " IOException: " + e.getMessage());
			}
		}
		
	}

}
