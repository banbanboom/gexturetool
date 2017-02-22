package com.example.gesturenxt;

public interface EV3Protocol {

	public static byte DIRECT_COMMAND_REPLY = (byte) 0x00;
	public static byte DIRECT_COMMAND_NOREPLY = (byte) 0x80;

	public static byte DIRECT_COMMAND_SUCCESS = 0x02;

	public static byte OP_UI_DRAW = (byte) 0x84;

	public static byte OP_INPUT_DEVICE_LIST = (byte) 0x98;
	public static byte OP_INPUT_DEVICE = (byte) 0x99;
	public static byte OP_INPUT_READ = (byte) 0x9A;
	public static byte OP_INPUT_TEST = (byte) 0x9B;
	public static byte OP_INPUT_READY = (byte) 0x9C;
	public static byte OP_INPUT_READSI = (byte) 0x9D;
	public static byte OP_INPUT_READEXT = (byte) 0x9E;
	public static byte OP_INPUT_WRITE = (byte) 0x9F;

	public static byte OP_SOUND = (byte) 0x94;

	public static byte OP_OUTPUT_SET_TYPE = (byte) 0xA1;
	public static byte OP_OUTPUT_RESET = (byte) 0xA2;
	public static byte OP_OUTPUT_STOP = (byte) 0xA3;
	public static byte OP_OUTPUT_POWER = (byte) 0xA4;
	public static byte OP_OUTPUT_SPEED = (byte) 0xA5;
	public static byte OP_OUTPUT_START = (byte) 0xA6;
	public static byte OP_OUTPUT_POLARITY = (byte) 0xA7;
	public static byte OP_OUTPUT_READ = (byte) 0xA8;
	public static byte OP_OUTPUT_TEST = (byte) 0xA9;
	public static byte OP_OUTPUT_READY = (byte) 0xAA;
	public static byte OP_OUTPUT_POSITION = (byte) 0xAB;
	public static byte OP_OUTPUT_STEP_POWER = (byte) 0xAC;
	public static byte OP_OUTPUT_TIME_POWER = (byte) 0xAD;
	public static byte OP_OUTPUT_STEP_SPEED = (byte) 0xAE;
	public static byte OP_OUTPUT_TIME_SPEED = (byte) 0xAF;

	public static byte MOTOR_A = (byte) 0x01;
	public static byte MOTOR_B = (byte) 0x02;
	public static byte MOTOR_C = (byte) 0x04;
	public static byte MOTOR_D = (byte) 0x08;
	public static byte LAYER_MASTER = (byte) 0x00;
	public static byte LAYER_SLAVE = (byte) 0x01;

	public static byte TOUCH = (byte) 0x10;
	public static byte COLOR = (byte) 0x1D;
	public static byte ULTRASONIC = (byte) 0x1E;

	public static byte TOUCH_TOUCH = (byte) 0x00;
	public static byte TOUCH_BUMPS = (byte) 0x01;

	public static byte COL_REFLECT = (byte) 0x00;
	public static byte COL_AMBIENT = (byte) 0x01;
	public static byte COL_COLOR = (byte) 0x02;

	public static byte US_CM = (byte) 0x00;
	public static byte US_INCH = (byte) 0x01;
	public static byte US_LISTEN = (byte) 0x02;



}
