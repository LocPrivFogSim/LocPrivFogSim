package org.fog.offloading;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;

public class BandwidthCpuResponseTimeCalculator implements IOffloadingResponseTimeCalculator {

	// The max distance in a 10x10km region is 10000m * sqrt(2)
	public static final double MAX_DISTANCE = 10000 * Math.sqrt(2);

	@Override
	public double calculateResponseTime(List<FogDevice> serverCloudlets, List<ApDevice> apDevices,
			MobileDevice source, FogDevice target, OffloadingTask task) {

		Log.printLine("===============");

		ApDevice ap = source.getSourceAp();
		if (ap == null) {
			Log.printLine("The MobileDevice's source Ap is not set. The MobileDevice might be in a handoff...");
			ap = source.getDestinationAp();
		}
		
		if (ap == null) {
			Log.printLine("The MobileDevice's destination Ap is not set. The MobileDevice might be in a handoff... Returning double max value");
			return Double.MAX_VALUE;
		}

		double distance = Coordinate.calcDistance(source.getPosition().getCoordinate(), target.getPosition().getCoordinate());
		double distanceFactor = 1 - (distance / MAX_DISTANCE);

		Log.formatLine("OffloadingTask calculating estimated response time between %s and %s; AP: %s", source.getName(), target.getName(), ap.getName());

		// Get minimum bandwidth between MobileDevice and current connected AP => direction upload
		double upBandwidthMo2AP = Math.min(source.getUplinkBandwidth(), ap.getDownlinkBandwidth());

//		// Get bandwidth between current connected AP and its associated (parent) FogDevice => direction upload
		double upBandwidthAP2FD = Math.min(ap.getUplinkBandwidth(), ap.getServerCloudlet().getDownlinkBandwidth());

		// Get bandwidth between associated (parent) FogDevice and target FogDevice => direction upload
		double upBandwidthFD2FD = Math.min(ap.getServerCloudlet().getUplinkBandwidth(), target.getDownlinkBandwidth());

		// Get bandwidth between target FogDevice and associated (parent) FogDevice => direction download
		double downBandwidthFD2FD = Math.min(target.getUplinkBandwidth(), ap.getServerCloudlet().getDownlinkBandwidth());

		// Get bandwidth between current APs associated (parent) FogDevice and the current connected AP => direction download
		double downBandwidthFD2AP = Math.min(ap.getServerCloudlet().getUplinkBandwidth(), ap.getDownlinkBandwidth());

		// Get minimum bandwidth between current connected AP and the MobileDevice => direction download
		double downBandwidthAP2Mo = Math.min(ap.getUplinkBandwidth(), source.getDownlinkBandwidth()); 

		double minUpBandwidth = 0d;
		double minDownBandwidth = 0d;
		// When the Ap's fog device is our target, there is no need for an additional network hop
		if (ap.getServerCloudlet() == target) {
			minUpBandwidth = Math.min(upBandwidthMo2AP, upBandwidthAP2FD);
			minDownBandwidth = Math.min(downBandwidthFD2AP, downBandwidthAP2Mo);
		} else {
			minUpBandwidth = Math.min(upBandwidthMo2AP, Math.min(upBandwidthAP2FD, upBandwidthFD2FD));
			minDownBandwidth = Math.min(downBandwidthFD2FD, Math.min(downBandwidthFD2AP, downBandwidthAP2Mo));
		}
			

//		// available mips are the mips the device is created with; total mips are the mips currently allocated by the device i.e.,
//		// effectively the currently number of used MIPS.
//		// So to calculate the targets remaining mips we have to subtract them from each other.
//		double targetMIPS =  getTarget().getHost().getAvailableMips() - getTarget().getHost().getTotalMips();
		double targetMIPS = target.getHost().getPeList().get(0).getPeProvisioner().getAvailableMips();

		double upBandwidth = minUpBandwidth * distanceFactor;
		double downBandwidth = minDownBandwidth * distanceFactor;

		double upTransmissionTime = task.getInputDataSize() / upBandwidth;
		double downTransmissionTime = task.getOutputDataSize() / downBandwidth;
		double executionTime = task.getMi() / targetMIPS;

		double responseTime = upTransmissionTime + executionTime + downTransmissionTime;

		Log.formatLine("MinUpBandwidth: %,.4fMB/s => min(Mo2AP: %,.4fMB/s; AP2FD: %,.4fMB/s; FD2FD: %,.4fMB/s)", minUpBandwidth, upBandwidthMo2AP, upBandwidthAP2FD, upBandwidthFD2FD);
		Log.formatLine("MinDownBandwidth: %,.4fMB/s => min(FD2FD: %,.4fMB/s; FD2AP: %,.4fMB/s; AP2Mo: %,.4fMB/s)", minDownBandwidth, downBandwidthFD2FD, downBandwidthFD2AP, downBandwidthAP2Mo);
		Log.formatLine("MinUpBandwidth: %,.4fMB/s => min(Mo2AP: %,.4fMB/s; FD2FD: %,.4fMB/s)", minUpBandwidth, upBandwidthMo2AP, upBandwidthFD2FD);
		Log.formatLine("MinDownBandwidth: %,.4fMB/s => min(FD2FD: %,.4fMB/s; AP2Mo: %,.4fMB/s)", minDownBandwidth, downBandwidthFD2FD, downBandwidthAP2Mo);
		Log.formatLine("Target MIPS: %,.4fMIPS", targetMIPS);
		Log.formatLine("UpTransmissionTime: %,.4fs for input of size %dMB", upTransmissionTime, task.getInputDataSize());
		Log.formatLine("DownTransmissionTime: %,.4fs for output of size %dMB", downTransmissionTime, task.getOutputDataSize());
		Log.formatLine("ExecutionTime: %,.4fs for num instructions %dMI", executionTime, task.getMi());
		Log.formatLine("OffloadingTask estimated response time between %s and %s: %,.4fs", source.getName(), target.getName(), responseTime);
		Log.printLine("===============");
		
		return responseTime;
	}
}
