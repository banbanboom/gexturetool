package com.example.gesturenxt;


import java.util.ArrayList;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class GestureNXTActivity extends Activity implements OnGesturePerformedListener {
	private static final int REQUEST_CONNECT_DEVICE = 1000;
	private static final int REQUEST_ENABLE_BT =2000;
	private static final int MENU_TOGGLE_CONNECT = Menu.FIRST;
	private static final int MENU_QUIT = Menu.FIRST + 1;
	private static final String FORWARD = "forward";
	private static final String BACKWARD = "backward";
	private static final String RIGHT = "right";
	private static final String LEFT = "left";

	private Activity thisActivity;
	private Handler btcHandler;
	private BTCommunicator myBTCommunicator = null;
	private Toast mLongToast;
	private Toast mShortToast;
	boolean newDevice;
	private ProgressDialog connectingProgressDialog;
	private boolean connected = false;
	private boolean bt_error_pending = false;
	int motorLeft;
	int motorRight;

	private GestureLibrary mLibrary;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gesture_nxt);
		thisActivity = this;

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mLongToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
		mShortToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

		motorLeft = BTCommunicator.MOTOR_C;
		motorRight = BTCommunicator.MOTOR_B;
		mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
		if(!mLibrary.load()){
			finish();
		}
		GestureOverlayView gestures = (GestureOverlayView)findViewById(R.id.gestures);
		gestures.addOnGesturePerformedListener(this);
	}
	protected void onStart(){
		super.onStart();

		if(!BluetoothAdapter.getDefaultAdapter().equals(null)){
			Log.v("Bluetooth", "Bluetooth is supported");
		}else{
			Log.v("Bluetooth", "Bluetooth is supported");
			finish();
		}
		if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
			showToastShort(getResources().getString(R.string.wait_till_bt_on));
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}else{
			Log.v("Bluetooth", "Bluetooth is on");
			selectNXT();
		}
	}
	public void onResume(){
		super.onResume();
	}
	public void onPause(){
		// sensor
		destroyBTCommunicator();
		super.onStop();
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		switch (requestCode){
		case REQUEST_CONNECT_DEVICE:

			if(resultCode == Activity.RESULT_OK){
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				newDevice = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
				if(newDevice == true) {
					enDiscoverable();
				}
				startBTCommunicator(address);
			}
			break;
		case REQUEST_ENABLE_BT:
			switch (resultCode){
			case Activity.RESULT_OK:
				selectNXT();
				break;
			case Activity.RESULT_CANCELED:
				showToastShort(getResources().getString(R.string.bt_needs_to_be_enabled));
				finish();
				break;
				default:
					showToastShort(getResources().getString(R.string.problem_at_connecting));
					finish();
					break;

			}
		}
	}
	private void enDiscoverable(){
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
		startActivity(discoverableIntent);
	}
	void selectNXT(){
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}
	public void startBTCommunicator(String mac_address){

		connectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

		if(myBTCommunicator == null){
			createBTCommunicator();
		}
		switch(((Thread) myBTCommunicator).getState()){
		case NEW:
			myBTCommunicator.setMACAddress(mac_address);
			myBTCommunicator.start();
			break;
		default:
			connected = false;
			myBTCommunicator = null;
			createBTCommunicator();
			myBTCommunicator.setMACAddress(mac_address);
			myBTCommunicator.start();
			break;
		}
		updateButtonsAndMenu();
	}
	public void createBTCommunicator(){
		myBTCommunicator = new BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter());
		btcHandler = myBTCommunicator.getHandler();
	}
	//receive messages from the BTCommunicator
	final Handler myHandler = new Handler() {

	@Override
		public void handleMessage(Message myMessage){
			switch (myMessage.getData().getInt("message")){
			case BTCommunicator.STATE_CONNECTED:
				connected = true;
				connectingProgressDialog.dismiss();
				updateButtonsAndMenu();
				doBeep(440, 500);
				showToastLong(getResources().getString(R.string.connected));
				break;
			case BTCommunicator.STATE_CONNECTERROR:
				connectingProgressDialog.dismiss();
			case BTCommunicator.STATE_RECEIVEERROR:
			case BTCommunicator.STATE_SENDERROR:
				destroyBTCommunicator();

				if (bt_error_pending == false){
					bt_error_pending = true;
					//inform the user of the error witch an AlertDailog
					DialogFragment newFragment = MyAlertDialogFragment.newInstance(R.string.bt_error_dialog_title, R.string.bt_error_dialog_message);
					newFragment.show(getFragmentManager(), "dialog");
				}
				break;
			}
		}
	};
	public void doPositiveClick(){
		bt_error_pending = false;
		selectNXT();
	}
	private void doBeep(int frequency, int duration){
		sendBTCmessage(BTCommunicator.NO_DELAY,  BTCommunicator.DO_BEEP, frequency, duration);

	}
	//motor
	public void updateMotorControl(int left, int right){
		if(myBTCommunicator != null){
			//send message via the handler
			sendBTCmessage(BTCommunicator.NO_DELAY,motorLeft, left, 0);
			sendBTCmessage(BTCommunicator.NO_DELAY,motorRight, right, 0);
		}
	}
	void sendBTCmessage(int delay, int message, int value1, int value2){
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putInt("value1", value1);
		myBundle.putInt("value2", value2);
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);

		if(delay == 0)
			btcHandler.sendMessage(myMessage);
		else
			btcHandler.sendMessageDelayed(myMessage, delay);
	}
	public void destroyBTCommunicator(){
		if(myBTCommunicator != null){
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0);
			myBTCommunicator = null;
		}
		connected = false;
		updateButtonsAndMenu();
	}
	private void showToastShort(String textToShow){
		mShortToast.setText(textToShow);
		mShortToast.show();
	}
	private void showToastLong(String textToShow){
		mLongToast.setText(textToShow);
		mLongToast.show();
	}
	//Menu
	private Menu myMenu;
	/**
	 * Create the menu items
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		myMenu = menu;
		myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect));
		myMenu.add(0, MENU_QUIT, 2,getResources().getString(R.string.quit));
		updateButtonsAndMenu();
		return true;
	}
	/**
	 * Handles item selections
	 */


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())	{
		case MENU_TOGGLE_CONNECT:

			if(myBTCommunicator==null||connected==false){
				selectNXT();
			}else{
				destroyBTCommunicator();
				updateButtonsAndMenu();
			}
			return true;
		case MENU_QUIT:
			destroyBTCommunicator();
			finish();
			return true;
		}
		return false;
	}
	private void updateButtonsAndMenu(){
		if(myMenu == null)return;
		myMenu.removeItem(MENU_TOGGLE_CONNECT);
		if(connected){
			myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.disconnect));
		}else{
			myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect));
		}
	}
	public boolean isConnected(){
		return connected;
	}
	public void onGesturePerformed(GestureOverlayView oView, Gesture gesture){
		// TODO 自動生成された
		TextView txtView01 = (TextView)findViewById(R.id.txtView1);
		ArrayList<Prediction>predictions = mLibrary.recognize(gesture);
		if(predictions.size()>0){
			Prediction prediction = predictions.get(0);
			// 2.0 より低い
			if(prediction.score > 1.0){
				txtView01.setText(prediction.name);
				moveRover(prediction.name);
			}
		}
	}
	private void moveRover(String action){
		int drvMode = 0;
		int degree = 0;
		if(action.equals(FORWARD)){
			drvMode = BTCommunicator.GO_FORWARD;
			degree = 3;
		}else if(action.equals(BACKWARD)){
			drvMode = BTCommunicator.GO_BACKWARD;
			degree = 3;
		}else if(action.equals(RIGHT)){
			drvMode = BTCommunicator.TURN_RIGHT;
			degree = 1;
		}else if(action.equals(LEFT)){
			drvMode = BTCommunicator.TURN_LEFT;
			degree = 1;
		}
		// message, value1, value2
		sendBTCmessage(BTCommunicator.NO_DELAY, drvMode, degree, 0);
	}
}