package org.fog.offloading;

import org.fog.entities.MobileDevice;

public interface IOffloadingScheduler {
	void scheduleOffloadingTask(MobileDevice mobileDevice);
}
