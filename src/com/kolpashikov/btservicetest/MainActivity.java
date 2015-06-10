package com.kolpashikov.btservicetest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	static final String CONNECTION_STATE = "connectionstate";
	static final String LOG = "mLogService";
	static final String BROADCAST_ACTION = "com.kolpashikov.btservicetest";
	static final String BROADCAST_MSG = "message";
	static final String BROADCAST_STATE = "connectionstate";
	public static final int CONNECTION_ABORTED = 1;
	public static final int CONNECTION_ESTABLISHED = 2;
	public static final int CONNECTION_MSG = 3;
	static final int SERVICE_STARTED = 0x10;
	static final int SERVICE_STOPPED = 0x20;	
	
	BluetoothAdapter btAdapter;	
	
	Button btnStart;
	Button btnStop;
	BroadcastReceiver brStateConnection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(LOG, "MainActivity onCreate");
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if(!btAdapter.isEnabled()){
			Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOn, 0);
		}
		
		btnStart = (Button)findViewById(R.id.btnStart);
		btnStart.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startService(new Intent(MainActivity.this, BTService.class));
				Log.d(LOG, "Button start clicked");
			}
			
		});
		
		btnStop  = (Button)findViewById(R.id.btnStop);
		btnStop.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopService(new Intent(MainActivity.this, BTService.class));
			}
			
		});
		btnStop.setEnabled(false);
		
		brStateConnection = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				
				int state = intent.getIntExtra(BROADCAST_STATE, 0);
				String s = intent.getStringExtra(BROADCAST_MSG) + ", state " + state;
				Log.d(LOG, s);
				Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
				switch(state){
				case SERVICE_STARTED:
					btnStart.setEnabled(false);
					btnStop.setEnabled(true);
					break;
					
				case SERVICE_STOPPED:
					btnStop.setEnabled(false);
					btnStart.setEnabled(true);
					break;					
				}
			}			
		};
		
		IntentFilter intFilter = new IntentFilter(BROADCAST_ACTION);
		registerReceiver(brStateConnection, intFilter);

	}

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
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		unregisterReceiver(brStateConnection);
	}
}





