package org.fog.offloading;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;

import java.util.List;

public class ClosestFogDeviceOffloadingStrategy implements IOffloadingStrategy {

	public ClosestFogDeviceOffloadingStrategy() {
	}
		
	@Override
	public FogDevice selectOffloadingTarget(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, OffloadingTask task) {

		FogDevice device = serverCloudlets.get(0);
		double distance = Double.MAX_VALUE;

		// Search the device closest to the source
		for (FogDevice current : serverCloudlets) {
			double value = Coordinate.calcDistance(source.getPosition().getCoordinate(), current.getPosition().getCoordinate());
			if (value < distance) {
				device = current;
				distance = value;
			}
		}
		
		if (device == null)
			return null;

		Log.formatLine("ClosestFogDeviceOffloadingStrategy: Selected %s device as closest device with an distance of %,.4fm"
			, device.getName(), distance);
		
		return device;
	}
}
