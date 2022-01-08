package org.fog.offloading;

import org.fog.entities.MobileDevice;
import org.fog.vmmobile.constants.MaxAndMin;

public class FixedOffloadingScheduler implements IOffloadingScheduler {

	private int travelTime;

	private boolean hasScheduled = false;
	
	private int numOfOffloadingTask;
	
	private int inputDataSize;
	
	private int mi;
	
	private int outputDataSize;

	private int id;
	
	public FixedOffloadingScheduler(int travelTime, int numOfOffloadingTask, int inputDataSize, int mi, int outputDataSize) {
		this.travelTime = travelTime;
		this.numOfOffloadingTask = numOfOffloadingTask;
		this.inputDataSize = inputDataSize;
		this.mi = mi;
		this.outputDataSize = outputDataSize;
	}
	
	public void scheduleOffloadingTask(MobileDevice mobileDevice) {
		if (hasScheduled())
			return;

		int interval = travelTime % numOfOffloadingTask;
		for (int i = 0; i < MaxAndMin.MAX_SIMULATION_TIME; i += interval)
			mobileDevice.scheduleOffloadingTask(i,
					new OffloadingTask(id++, mobileDevice.getId(), inputDataSize, mi, outputDataSize));
		
		this.hasScheduled = true;
	}

	public boolean hasScheduled() {
		return hasScheduled;
	}

	public int getTravelTime() { return travelTime; }

	public void setTravelTime(int travelTime) { this.travelTime = travelTime; }

	public int getNumOfOffloadingTask() {
		return numOfOffloadingTask;
	}

	public void setNumOfOffloadingTask(int numOfOffloadingTask) {
		this.numOfOffloadingTask = numOfOffloadingTask;
	}
}
