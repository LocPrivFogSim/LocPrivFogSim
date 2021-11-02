package org.fog.offloading;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public class OffloadingTask {

	private int inputDataSize;

	private int mi;

	private int outputDataSize;

	private String uid;

	private MobileDevice source;

	private FogDevice target;
	
	public OffloadingTask(int id, int userId, int inputDataSize, int mi, int outputDataSize) {
		setUid(getUid(userId, id));
		setInputDataSize(inputDataSize);
		setMi(mi);
		setOutputDataSize(outputDataSize);
	}

	public int getInputDataSize() {
		return inputDataSize;
	}

	public void setInputDataSize(int inputDataSize) {
		this.inputDataSize = inputDataSize;
	}

	public int getMi() {
		return mi;
	}

	public void setMi(int mi) {
		this.mi = mi;
	}

	public int getOutputDataSize() {
		return outputDataSize;
	}

	public void setOutputDataSize(int outputDataSize) {
		this.outputDataSize = outputDataSize;
	}

	public MobileDevice getSource() {
		return this.source;
	}
	
	public void setSource(MobileDevice source) {
		this.source = source;
	}

	public FogDevice getTarget() {
		return this.target;
	}
	
	public void setTarget(FogDevice target) {
		this.target = target;
	}

	/**
	 * Get unique string identificator of the VM.
	 *
	 * @return string uid
	 */
	public String getUid() {
		return uid;
	}
	
	/**
	 * Sets the uid.
	 *
	 * @param uid the new uid
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * Generate unique string identificator of the offloading task.
	 *
	 * @param userId the user id
	 * @param taskId the task id
	 * @return string uid
	 */
	public static String getUid(int userId, int taskId) {
		return userId + "-Task" + taskId;
	}
}