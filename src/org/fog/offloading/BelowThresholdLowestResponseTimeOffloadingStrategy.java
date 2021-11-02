package org.fog.offloading;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public class BelowThresholdLowestResponseTimeOffloadingStrategy extends AbstractBelowThresholdFilter {
			
	public BelowThresholdLowestResponseTimeOffloadingStrategy(double threshold) {
		super(threshold);
	}
		
	@Override
	public FogDevice selectOffloadingTarget(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, OffloadingTask task) {
		
		List<FogDevice> suitableDevices = getDevicesBelowThreshold(serverCloudlets, apDevices, source, task);
		
		if (suitableDevices.size() == 0)
			return null;
		
		double minResponseTime = Double.MAX_VALUE;
		FogDevice device = null;
				
		// Search the device with the lowest response time
		for (FogDevice target : suitableDevices) {
			double responseTime = source.getOffloadingResponseTimeCalculator()
					.calculateResponseTime(serverCloudlets, apDevices, source, target, task);
				
			if (responseTime < minResponseTime) {
				minResponseTime = responseTime;
				device = target;
			}
		}
		
		Log.formatLine("BelowThresholdLowestResponseTimeeOffloadingStrategy: Selected %s device below the threshold of"
				+ " %,.4fs with response time %,.4fs", device.getName(), getThreshold(), minResponseTime);
		
		return device;
	}
}
