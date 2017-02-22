

package com.example.gesturenxt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BTCommunicator extends Thread
							implements EV3Protocol{
	public static final int ROTATE_MOTOR_A = 0;
	public static final int ROTATE_MOTOR_B = 1;
	public static final int ROTATE_MOTOR_C = 2;
	public static final int DO_BEEP = 51;
	//
	public static final int GO_FORWARD = 52;
	public static final int GO_BACKWARD = 53;
	public static final int TURN_RIGHT = 54;
	public static final int TURN_LEFT = 55;

	public static final int DISCONNECT = 99;

	public static final int DISPLAY_TOAST = 100;
	public static final int STATE_CONNECTED = 1001;
	public static final int STATE_CONNECTERROR = 1002;
	public static final int MOTOR_STATE = 1003;
	public static final int STATE_RECEIVEERROR = 1004;
	public static final int STATE_SENDERROR = 1005;
	public static final int NO_DELAY = 0;

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// this is only OUI
	public static final String OUI_LEGO = "00:16:53";

	BluetoothAdapter btAdapter;
	private BluetoothSocket nxtBTsocket = null;
	private DataOutputStream nxtDos = null;
	private boolean connected = false;

	private Handler uiHandler;
	private String mMACaddress;
	private GestureNXTActivity myGestureNxt;
	public BTCommunicator (GestureNXTActivity myGesNxt, Handler uiHandler,
			BluetoothAdapter btAdapter){
		this.myGestureNxt = myGesNxt;
		this.uiHandler = uiHandler;
		this.btAdapter = btAdapter;
	}

	public Handler getHandler() {
		return myHandler;
	}

	public boolean isBTAdapterEnabled () {
		return (btAdapter == null) ? false : btAdapter.isEnabled();
	}

	@Override
	public void run() {
		createNXTconnection();

		while (connected) {
			//
		}
	}

	private void createNXTconnection() {
		try{

			BluetoothSocket nxtBTsocketTEMPORARY ;
			BluetoothDevice nxtDevice = null;
			nxtDevice = btAdapter.getRemoteDevice(mMACaddress);

			if(nxtDevice == null) {
				sendToast (myGestureNxt.getResources().getString(R.string.no_paired_nxt));
				sendState(STATE_CONNECTERROR);
				return;
			}

			nxtBTsocketTEMPORARY = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
			nxtBTsocketTEMPORARY.connect();
			nxtBTsocket = nxtBTsocketTEMPORARY;

			nxtDos = new DataOutputStream(nxtBTsocket.getOutputStream());

			connected = true;
		} catch (IOException e) {
			Log.d("BTCommuicator","error createNXTConnection()", e);
			if (myGestureNxt.newDevice) {
				sendToast(myGestureNxt.getResources().getString(R.string.pairing_message));
				sendState (STATE_CONNECTERROR);
			} else {
				sendState(STATE_CONNECTERROR);
			}

			return;
		}

		sendState(STATE_CONNECTED);
	}

	private void destroyNXTconnection() {
		try{
			if (nxtBTsocket != null) {
				changeMotorSpeed (MOTOR_B, 0);
				changeMotorSpeed(MOTOR_C, 0);
				waitSomeTime(1000);
				connected = false;
				nxtBTsocket.close();
				nxtBTsocket = null;
			}
			nxtDos = null;

		} catch (IOException e) {
			sendToast (myGestureNxt.getResources().getString(R.string.problem_at_closing));
		}
	}

	private void doBeepBT( int frequency, int duration) {
		byte[] message = new byte[13];
		message[0] = DIRECT_COMMAND_NOREPLY;
		message[1] = 0x00;
		message[2] = 0x00;
		message[3] = OP_SOUND;
		message[4] = 0x01;
		message[5] = (byte)0x81;
		message[6] = 30;
		message[7] = (byte)0x82;
		message[8] = (byte) frequency;
		message[9] = (byte) (frequency >>8);
		message[10] = (byte)0x82;
		message[11] = (byte) duration;
		message[12] = (byte) (duration >> 8);

		sendMessage(message);
		waitSomeTime(duration);

	}

	//Motor
	private void changeMotorSpeed(int motor, int speed) {
		byte[] message = new byte[12];

		if(speed > 100)
			speed = 100;

		else if (speed < -100)
			speed = -100;

		//Direct command telegram, no response
		message[0] = (byte) 0x80;
		message[1] = (byte) 0x04;
		//Output port
		message[2] = (byte) motor;

		if(speed == 0) {
			message[3] = 0;
			message[4] = 0;
			message[5] = 0;
			message[6] = 0;
			message[7] = 0;
		} else {
			// Power set option (Range: -100 - 100)
			message[3] = (byte) speed;
			//Mode byte (Bit-field) : MOTORON + BREAK
			message[4] = 0x03;
			//Regulation mode: REGULATION_MODE_MOTOR_SPEED
			message[5] = 0x01;
			// Turn Ration (SBYTE; -100 -100)
			message[6] = 0x00;
			// RunState: MOTOR_RUN_STATE_RUNNING
			message[7] = 0x20;
		}

		//TachoLimit: run forever
		message[8] = 0;
		message[9] = 0;
		message[10] = 0;
		message[11] = 0;

		sendMessage(message);

	}

	private void goForward ( int degree) {
		//
		degree *=360;
		int degree1 = 0;
		int degree2 = 0;

		if(degree > 360)	{
			degree2 = 180;
			degree1 = degree - degree2;
		}	else {
			degree1 = degree;
		}
		byte[]  message = new byte[16];
		message[0] = DIRECT_COMMAND_NOREPLY;
		message[1] = 0x00;
		message[2] = 0x00;
		message[3] = OP_OUTPUT_STEP_SPEED;
		message[4] = LAYER_MASTER;
		message[5] = MOTOR_A+MOTOR_C;
		message[6] = (byte)0x81;
		message[7] = (byte)100;
		message[8] = 0x00;
		message[9] = (byte)0x82;
		message[10] = (byte) degree1;
		message[11] = (byte) (degree1 >>8);
		message[12] = (byte)0x82;
		message[13] = (byte) degree2;
		message[14] = (byte) (degree2 >> 8);
		message[15] = 1;
		sendMessage(message);

	}

	private void goBackward ( int degree) {
		//  motorA,C ON
		degree *=360;
		int degree1 = 0;
		int degree2 = 0;

		if(degree > 360)	{
			degree2 = 180;
			degree1 = degree - degree2;
		}	else {
			degree1 = degree;
		}
		byte[]  message = new byte[16];
		message[0] = DIRECT_COMMAND_NOREPLY;
		message[1] = 0x00;
		message[2] = 0x00;
		message[3] = OP_OUTPUT_STEP_SPEED;
		message[4] = LAYER_MASTER;
		message[5] = MOTOR_A+MOTOR_C;
		message[6] = (byte)0x81;
		message[7] = (byte)-100;
		message[8] = 0x00;
		message[9] = (byte)0x82;
		message[10] = (byte) degree1;
		message[11] = (byte) (degree1 >>8);
		message[12] = (byte)0x82;
		message[13] = (byte) degree2;
		message[14] = (byte) (degree2 >> 8);
		message[15] = 1;
		sendMessage(message);

	}

	private void turnRight( int degree) {
		// motor C ON
		degree *=360;
		int degree1 = 0;
		int degree2 = 0;

		if(degree > 360)	{
			degree2 = 180;
			degree1 = degree - degree2;
		}	else {
			degree1 = degree;
		}
		byte[]  message = new byte[16];
		message[0] = DIRECT_COMMAND_NOREPLY;
		message[1] = 0x00;
		message[2] = 0x00;
		message[3] = OP_OUTPUT_STEP_SPEED;
		message[4] = LAYER_MASTER;
		message[5] = MOTOR_A;
		message[6] = (byte)0x81;
		message[7] = (byte)100;
		message[8] = 0x00;
		message[9] = (byte)0x82;
		message[10] = (byte) degree1;
		message[11] = (byte) (degree1 >>8);
		message[12] = (byte)0x82;
		message[13] = (byte) degree2;
		message[14] = (byte) (degree2 >> 8);
		message[15] = 1;
		sendMessage(message);

	}

	private void turnLeft( int degree){
		// motor A ON
		degree *=360;
		int degree1 = 0;
		int degree2 = 0;

		if(degree > 360)	{
			degree2 = 180;
			degree1 = degree - degree2;
		}	else {
			degree1 = degree;
		}
		byte[]  message = new byte[16];
		message[0] = DIRECT_COMMAND_NOREPLY;
		message[1] = 0x00;
		message[2] = 0x00;
		message[3] = OP_OUTPUT_STEP_SPEED;
		message[4] = LAYER_MASTER;
		message[5] = MOTOR_C;
		message[6] = (byte)0x81;
		message[7] = (byte)100;
		message[8] = 0x00;
		message[9] = (byte)0x82;
		message[10] = (byte) degree1;
		message[11] = (byte) (degree1 >>8);
		message[12] = (byte)0x82;
		message[13] = (byte) degree2;
		message[14] = (byte) (degree2 >> 8);
		message[15] = 1;
		sendMessage(message);

	}

	private boolean sendMessage(byte[] message) {
		if(nxtDos == null) {
			return false;
		}

		int bodyLength = message.length + 2;
		byte[] header = {
				(byte) (bodyLength & 0xff), (byte) ((bodyLength >>> 8) & 0xff), 0x00, 0x00
		};
		try{
			Log.v("sendMessage","message="+byteToStr(message));
			int messageLength = message.length;
			nxtDos.write(header);
			nxtDos.write(message);
			nxtDos.writeByte(messageLength);
			nxtDos.writeByte(messageLength >> 8);
			nxtDos.write(message,0,message.length);
			nxtDos.flush();
		} catch ( IOException ioe) {
			sendState(STATE_SENDERROR);
			return false;
		}
		return true;
	}

		//try{
			//Log.v("sendMessage","message="+byteToStr(message));
			// send message length
			//int messageLength = message.length;
			//nxtDos.writeByte(messageLength);
			//nxtDos.writeByte(messageLength >> 8);
			//nxtDos.write(message,0,message.length);
			//nxtDos.flush();
			//return true;
		//} catch (IOException ioe) {
			//sendState(STATE_SENDERROR);
			//return false;
		//}
	//}

	private String byteToStr(byte[] mess){
		StringBuffer strbuf = new StringBuffer();
		for(int i = 0; i < mess.length; i ++) {
			strbuf.append(String.format("%02x",(mess[i])));
		}
		return strbuf.toString();
	}

	private void waitSomeTime(int millis){
		try{
			Thread.sleep(millis);

		} catch (InterruptedException e) {
		}
	}

	private void sendToast(String toastText) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", DISPLAY_TOAST);
		myBundle.putString("toastText", toastText);
		sendBundle(myBundle);
	}

	private void sendState(int message)  {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		sendBundle(myBundle);
	}

	private void sendBundle(Bundle myBundle) {
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);
		uiHandler.sendMessage(myMessage);
	}

	// receive messages from the UI
	final Handler myHandler = new Handler() {
		public void handleMessage(Message myMessage) {

			int message;

			switch ( message = myMessage.getData().getInt("message")) {
			//
			case ROTATE_MOTOR_A:
			case ROTATE_MOTOR_B:
			case ROTATE_MOTOR_C:
				changeMotorSpeed(message,myMessage.getData().getInt("value1"));
				break;
			case DO_BEEP:
			doBeepBT(myMessage.getData().getInt("value1"), myMessage.getData().getInt("value2"));
					break;
					case GO_FORWARD:
					goForward(myMessage.getData().getInt("value1"));
					break;
					case GO_BACKWARD:
						goBackward(myMessage.getData().getInt("value1"));
						break;
					case TURN_RIGHT:
						turnRight(myMessage.getData().getInt("value1"));
						break;
					case TURN_LEFT:
						turnLeft(myMessage.getData().getInt("value1"));
						break;
					case DISCONNECT:
						destroyNXTconnection();
						break;
			}

		}
	};

	public void setMACAddress(String mMACaddress){
		this.mMACaddress = mMACaddress;
	}
}
