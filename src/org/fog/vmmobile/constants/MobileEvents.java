package org.fog.vmmobile.constants;

public class MobileEvents {
	public static final int BASE = 5000;
	public static final int TO_MIGRATION = BASE + 0;
	public static final int NO_MIGRATION = BASE + 1;
	public static final int START_MIGRATION = BASE + 2;
	public static final int ABORT_MIGRATION = BASE + 3;
	public static final int MAKE_DECISION_MIGRATION = BASE + 4;
	public static final int REMOVE_VM_OLD_CLOUDLET = BASE + 5;
	public static final int ADD_VM_NEW_CLOUDLET = BASE + 6;
	public static final int STOP_SIMULATION = BASE + 7;
	public static final int START_HANDOFF = BASE + 8;
	public static final int DELIVERY_VM = BASE + 9;
	public static final int NEXT_STEP = BASE + 10;
	public static final int CHECK_NEW_STEP = BASE + 11;
	public static final int CREATE_NEW_SMARTTHING = BASE + 12;
	public static final int UNLOCKED_HANDOFF = BASE + 13;
	public static final int CONNECT_ST_TO_SC = BASE + 14;
	public static final int DESCONNECT_ST_TO_SC = BASE + 15;
	public static final int UNLOCKED_MIGRATION = BASE + 16;
	public static final int VM_MIGRATE = BASE + 17;
	public static final int APP_SUBMIT_MIGRATE = BASE + 18;
	public static final int SET_MIG_STATUS_TRUE = BASE + 19;

}
