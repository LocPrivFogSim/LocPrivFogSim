package org.fog.vmmobile.constants;

public final class MaxAndMin {

	public static final int AP_COVERAGE = 2000; //Max Ap coverage distance - It should modify
	public static final int CLOUDLET_COVERAGE = 2000; //Max Ap coverage distance - It should modify
	public static final int MAX_DISTANCE_TO_HANDOFF = 1000; //It cannot be less than Max_SPEED
	public static final int MIG_POINT = (int) (MAX_DISTANCE_TO_HANDOFF*1.3);//		0; //Distance from boundary - it should modify
	public static final int LIVE_MIG_POINT = 200;//(int) (MAX_DISTANCE_TO_HANDOFF*20.0);//It can be based on the Network's Bandwidth
	public static final int MAX_HANDOFF_TIME = 10;	//1200
	public static final int MIN_HANDOFF_TIME = 0;	//700
	public static final int MAX_AP_DEVICE = 20;
	public static final int MAX_SMART_THING = 7;
	public static final int MAX_SERVER_CLOUDLET = 50;
	public static final int MAX_X = 50;
	public static final int MAX_Y = 50;
	public static final int MAX_SPEED = 20;
	public static final int MAX_DIRECTION = 9;
	public static final int MAX_SERVICES = 3;
	public static final float MAX_VALUE_SERVICE = 1.1f;
	public static final float MAX_VALUE_AGREE = 70f;
	public static final int MAX_ST_IN_AP = 500;
	public static final int MAX_SIMULATION_TIME = 1000 * 60 * 10; // 1000 Ticks * 60 Seconds * 10 Minutes
	public static final int MAX_VM_SIZE = 201; //200MB Random.nextInt(int bound) returns a value between 0 (inclusive) and the specified value (exclusive)
	public static final int MIN_VM_SIZE = 100; //100MB
	public static final int MAX_BANDWIDTH = 15 * 1024 * 1024;
	public static final int MIN_BANDWIDTH = 5 * 1024 * 1024;
	public static final int DELAY_PROCESS = 500;
	public static final double SIZE_CONTAINER = 0.6;
	public static final double PROCESS_CONTAINER = 1.3;

}
