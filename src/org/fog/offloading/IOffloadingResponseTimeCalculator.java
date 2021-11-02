package org.fog.offloading;

import java.util.List;

import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public interface IOffloadingResponseTimeCalculator {

	public double calculateResponseTime(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, FogDevice target, OffloadingTask task);
	
}
