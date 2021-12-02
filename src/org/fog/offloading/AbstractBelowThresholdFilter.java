package org.fog.offloading;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.vmmobile.TestExample4;

public abstract class AbstractBelowThresholdFilter implements IOffloadingStrategy {

	private double threshold;

	public AbstractBelowThresholdFilter(double threshold) {
		setThreshold(threshold);
	}

	@Override
	public abstract FogDevice selectOffloadingTarget(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, OffloadingTask task);

	protected List<FogDevice> getDevicesBelowThreshold(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, OffloadingTask task) {
		List<FogDevice> result = new ArrayList<FogDevice>();

		// Find region of source mobile device
		List<Coordinate> key = null;
		for (List<Coordinate> keys : TestExample4.fogDevicesInField.keySet()) {
			if (Coordinate.coordIsInField(keys, source.getPosition().getCoordinate()))
			{
				key = keys;
				break;
			}
		}

		if (key == null)
			throw new IllegalStateException();

		// Fetch all fog devices in region
		List<FogDevice> devices = TestExample4.fogDevicesInField.get(key);

		// Filter for devices below the threshold
		for (FogDevice target : devices) {
			double responseTime = source.getOffloadingResponseTimeCalculator()
					.calculateResponseTime(serverCloudlets, apDevices, source, target, task);

			if (responseTime <= getThreshold())
				result.add(target);
		}

		Log.printLine(result.size() + " num devices below threshold " + getThreshold());

		return result;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
}
