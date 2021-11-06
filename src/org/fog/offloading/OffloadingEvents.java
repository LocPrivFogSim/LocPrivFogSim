package org.fog.offloading;

public class OffloadingEvents {
	public static final int BASE = 6000;
	public static final int MAKE_OFFLOADING_SCHEDULING_DECISION = BASE + 0;
	public static final int START_OFFLOADING = BASE + 1;
	public static final int BEGIN_OFFLOAD_TASK_EXECUTION = BASE + 2;
	public static final int END_OFFLOAD_TASK_EXECUTION = BASE + 3;
	public static final int FINISHED_OFFLOADING = BASE + 4;
}
