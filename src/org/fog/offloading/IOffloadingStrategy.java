package org.fog.offloading;

import java.util.List;

import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public interface IOffloadingStrategy {

	FogDevice selectOffloadingTarget(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, MobileDevice source, OffloadingTask task);
	
}
