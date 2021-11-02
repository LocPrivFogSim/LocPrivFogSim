package org.fog.scheduler;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;

import java.util.List;

public class StreamOperatorScheduler extends VmSchedulerTimeSharedOverbookingEnergy{

	public StreamOperatorScheduler(List<? extends Pe> pelist) {
		super(pelist);
	}
}
