package org.fog.placement;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ModulePlacement {
	
	
	public static int ONLY_CLOUD = 1;
	public static int EDGEWARDS = 2;
	public static int USER_MAPPING = 3;
	
	private List<FogDevice> fogDevices;
	private List<MobileDevice> mobileDevices;
	private Application application;
	private Map<String, List<Integer>> moduleToDeviceMap;
	private Map<Integer, List<AppModule>> deviceToModuleMap;
	private Map<Integer, Map<String, Integer>> moduleInstanceCountMap;
	
	protected abstract void mapModules();
	
	protected boolean canBeCreated(FogDevice fogDevice, AppModule module){		
		return fogDevice.getVmAllocationPolicy().allocateHostForVm(module);
	}
	
	protected int getParentDevice(int fogDeviceId){
		return ((FogDevice)CloudSim.getEntity(fogDeviceId)).getParentId();
	}
	
	protected FogDevice getFogDeviceById(int fogDeviceId){
		return (FogDevice)CloudSim.getEntity(fogDeviceId);
	}
	
	protected boolean createModuleInstanceOnDevice(AppModule _module, final FogDevice device, int instanceCount){
		return false;
	}
	
	protected boolean createModuleInstanceOnDevice(AppModule _module, final FogDevice device){
		AppModule module = null;
		if(getModuleToDeviceMap().containsKey(_module.getName()))
			module = new AppModule(_module);
		else
			module = _module;
		try(FileWriter fw1 = new FileWriter("creating_modules.txt", true);
			    BufferedWriter bw1 = new BufferedWriter(fw1);
			    PrintWriter out1 = new PrintWriter(bw1))
		{
			out1.println (CloudSim.clock()+ " Creating "+module.getName()+" on device "+device.getName());
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(canBeCreated(device, module)){
			Log.printLine("Creating "+module.getName()+" on device "+device.getName());
			if(!getDeviceToModuleMap().containsKey(device.getId()))
				getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
			getDeviceToModuleMap().get(device.getId()).add(module);

			if(!getModuleToDeviceMap().containsKey(module.getName()))
				getModuleToDeviceMap().put(module.getName(), new ArrayList<Integer>());
			getModuleToDeviceMap().get(module.getName()).add(device.getId());
			return true;
		} else {
			Log.printLine("Creating "+module.getName()+" on device "+device.getName()+ " was not possible");
			System.err.println("Module "+module.getName()+" cannot be created on device "+device.getName());
			System.err.println("Terminating");
			return false;
		}
	}
	
	protected FogDevice getDeviceByName(String deviceName) {
		for(FogDevice dev : getFogDevices()){
			if(dev.getName().equals(deviceName))
				return dev;
		}
		return null;
	}
	
	protected FogDevice getDeviceById(int id){
		for(FogDevice dev : getFogDevices()){
			if(dev.getId() == id)
				return dev;
		}
		return null;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public Map<String, List<Integer>> getModuleToDeviceMap() {
		return moduleToDeviceMap;
	}

	public void setModuleToDeviceMap(Map<String, List<Integer>> moduleToDeviceMap) {
		this.moduleToDeviceMap = moduleToDeviceMap;
	}

	public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
		return deviceToModuleMap;
	}

	public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
		this.deviceToModuleMap = deviceToModuleMap;
	}

	public Map<Integer, Map<String, Integer>> getModuleInstanceCountMap() {
		return moduleInstanceCountMap;
	}

	public void setModuleInstanceCountMap(Map<Integer, Map<String, Integer>> moduleInstanceCountMap) {
		this.moduleInstanceCountMap = moduleInstanceCountMap;
	}

	public List<MobileDevice> getMobileDevices() {
		return mobileDevices;
	}

	public void setMobileDevices(List<MobileDevice> mobileDevices) {
		this.mobileDevices = mobileDevices;
	}

}
