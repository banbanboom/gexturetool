package com.example.gesturenxt;

import java.util.Set;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity {

	static final String PAIRING = "pairing";

	// Debugging
	private static final String TAG = "DeviceListActivity";
	private static final boolean DEBUG = true;
	// this is the only OUI registered by LEGO, see http://standards.ieee.org/regauth/oui/indext.shtml
	public static final String OUI_LEGO = "00:16:53";
	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstancesState)	{
		super.onCreate(savedInstancesState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		setResult(Activity.RESULT_CANCELED);

		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener()		{
			public void onClick(View v)	{
				doDiscovery();
				v.setVisibility(View.GONE);
			}

		});

		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		ListView newListView = (ListView) findViewById(R.id.new_devices);
		newListView.setAdapter(mNewDevicesArrayAdapter);
		newListView.setOnItemClickListener(mDeviceClickListener);

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver,filter);

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		boolean legoDevicesFound = false;

		if (pairedDevices.size() > 0)	{
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices)	{
				if (device.getAddress().startsWith(OUI_LEGO))	{
					legoDevicesFound = true;
					mPairedDevicesArrayAdapter.add(device.getName() +  "\n" + device.getAddress());

				}
			}
		}

		if (legoDevicesFound == false)	{
			String noDevices = getResources().getText(R.string.none_paired).toString();
			mPairedDevicesArrayAdapter.add(noDevices);
		}

	}
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener()	{
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3 )	{
			mBtAdapter.cancelDiscovery();

			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length()  -17);

			Intent intent = new Intent();
			Bundle data = new Bundle();
			data.putString(EXTRA_DEVICE_ADDRESS, address);
			data.putBoolean(PAIRING, av.getId() == R.id.new_devices);
			intent.putExtras(data);

			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};

	@Override
	public void onDestroy()	{
			super.onDestroy();
			unregisterReceiver(mReceiver);
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver()	{
		public void onReceive(Context context, Intent intent)	{
			String action = intent.getAction();
			String dName = null;

			if (BluetoothDevice.ACTION_FOUND.equals(action))	{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if((dName = device.getName()) != null) {
					if (device.getBondState() != BluetoothDevice.BOND_BONDED)	{
						mNewDevicesArrayAdapter.add(device.getName() + "￥n"	+ device.getAddress());
					}
				}
			}

			if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action))	{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() != BluetoothDevice.BOND_BONDED)	{
					mNewDevicesArrayAdapter.add(device.getName() + "￥n"	+ device.getAddress() );
				}
			}
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))	{
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if (mNewDevicesArrayAdapter.getCount() == 0)	{
					String noDevices = getResources().getText(R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};

	private void doDiscovery() {
		if (DEBUG) Log.d(TAG, "doDiscovery()");
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		if (mBtAdapter.isDiscovering())	{
			mBtAdapter.cancelDiscovery();
		}
		mBtAdapter.startDiscovery();
		}
	}

