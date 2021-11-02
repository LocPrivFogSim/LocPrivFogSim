package org.fog.offloading;

import org.fog.entities.MobileDevice;
import org.fog.vmmobile.constants.MaxAndMin;

public class FixedOffloadingScheduler implements IOffloadingScheduler {

	private boolean hasScheduled = false;
	
	private int intervall;
	
	private int inputDataSize;
	
	private int mi;
	
	private int outputDataSize;

	private int id;
	
	public FixedOffloadingScheduler(int intervall, int inputDataSize, int mi, int outputDataSize) {
		this.intervall = intervall;
		this.inputDataSize = inputDataSize;
		this.mi = mi;
		this.outputDataSize = outputDataSize;
	}
	
	public void scheduleOffloadingTask(MobileDevice mobileDevice) {
		if (hasScheduled())
			return;
			
		for (int i = 0; i < MaxAndMin.MAX_SIMULATION_TIME; i += getIntervall())
			mobileDevice.scheduleOffloadingTask(i,
					new OffloadingTask(id++, mobileDevice.getId(), inputDataSize, mi, outputDataSize));
		
		this.hasScheduled = true;
	}

	public boolean hasScheduled() {
		return hasScheduled;
	}

	public int getIntervall() {
		return intervall;
	}

	public void setIntervall(int intervall) {
		this.intervall = intervall;
	}
}
