package org.cloudbus.cloudsim.util;

import org.cloudbus.cloudsim.Log;

public class Check {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		Test2 t2 = new Test2(10);
//		Test2 t1 = new Test2(10);
//		Test2 t3 = new Test2(40);
//
//		if(t1.equals(t2)){
//			Log.printLine("Success1");
//		}if(t3==t2){
//			Log.printLine("Success2");
//		}if(t1==t3){
//			Log.printLine("Success3");
//		}
//		Log.printLine("DONE");
		Test t2 = new Test(10);
		Test t1 = new Test(10);
		Test t3 = new Test(40);

		if(t1.equals(t2)){
			Log.printLine("Success1");
		}if(t3==t2){
			Log.printLine("Success2");
		}if(t1==t3){
			Log.printLine("Success3");
		}
		Log.printLine("DONE");
	}

}
