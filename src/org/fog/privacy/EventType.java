package org.fog.privacy;

public class EventType {
	public static final int HANDOFF = 0;
	public static final int MIGRATION = 1;
	public static final int OFFLOADING = 2;
	
	public static String eventTypeToString(int eventType) {
		switch(eventType) {
			case EventType.HANDOFF:
				return "handoff";
			case EventType.MIGRATION:
				return "migration";
			case EventType.OFFLOADING:
				return "offloading";
			default:
				return "unknown";
		}
	}
}
