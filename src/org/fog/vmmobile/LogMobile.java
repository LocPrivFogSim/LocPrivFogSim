package org.fog.vmmobile;

import org.cloudbus.cloudsim.core.CloudSim;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;

public class LogMobile {
	public static final int ERROR = 1;
	public static final int DEBUG = 0;
	
	public static int LOG_LEVEL = LogMobile.DEBUG;
	private static DecimalFormat df = new DecimalFormat("#.00"); 

	public static boolean ENABLED = false;;
	
	/** The Constant LINE_SEPARATOR. */
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static OutputStream output;
	
	public static void setLogLevel(int level){
		LogMobile.LOG_LEVEL = level;
	}
	
	public static void debug(String classJava, String message){
		if(!ENABLED)
			return;
		if(LogMobile.LOG_LEVEL <= LogMobile.DEBUG)
			log("Clock: " + CloudSim.clock() + " - " + classJava + ": " + message + LINE_SEPARATOR);
	}
	
	private static void log(String message)
	{
		try {
			getOutput().write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public static void error(String name, String message){
//		if(!ENABLED)
//			return;
//		if(LogMobile.LOG_LEVEL <= LogMobile.ERROR)
//			System.out.println(df.format(CloudSim.clock())+" : "+name+" : "+message);
//	}
	
	/**
	 * Sets the output.
	 * 
	 * @param _output the new output
	 */
	public static void setOutput(OutputStream _output) {
		output = _output;
	}

	/**
	 * Gets the output.
	 * 
	 * @return the output
	 */
	public static OutputStream getOutput() {
		if (output == null) {
			setOutput(System.out);
		}
		return output;
	}
	
}
