package org.fog.offloading;

import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public class BelowThresholdRandomDeviceOffloadingStrategy extends AbstractBelowThresholdFilter {

	private Random random;
	
	public BelowThresholdRandomDeviceOffloadingStrategy(int seed, double threshold) {
		super(threshold);
		
		this.random = new Random(seed * Integer.MAX_VALUE);
	}
		
	@Override
	public FogDevice selectOffloadingTarget(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, OffloadingTask task) { 
		
		List<FogDevice> suitableDevices = getDevicesBelowThreshold(serverCloudlets, apDevices, source, task);
				
		Log.formatLine("BelowThresholdRandomDeviceOffloadingStrategy found %d devices below the threshold of %,.4fs",
				suitableDevices.size(), getThreshold());
		
		if (suitableDevices.size() == 0)
			return null;
		
		int i = this.random.nextInt(suitableDevices.size());
		FogDevice device = suitableDevices.get(i);
		
		Log.formatLine("Selected a random fog device: %s", device.getName());
		
		return device;
	}
}
