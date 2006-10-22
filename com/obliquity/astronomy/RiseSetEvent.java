package com.obliquity.astronomy;

public class RiseSetEvent {
	public static final int UNKNOWN = 0;
	public static final int RISING = 1;
	public static final int SETTING = 2;
	
	public static final int RISE_SET = 3;
	public static final int CIVIL_TWILIGHT = 4;
	public static final int NAUTICAL_TWILIGHT = 5;
	public static final int ASTRONOMICAL_TWILIGHT = 6;
	
	public static final int UPPER_LIMB = 7;
	public static final int LOWER_LIMB = 8;
	public static final int CENTRE = 9;
	public static final int CENTER = 9;
	
	public static final int ALWAYS_ABOVE = -8888;
	public static final int ALWAYS_BELOW = -9999;
	
	public static final int INVALID_ARGUMENT = -7777;
	public static final int ARRAY_TOO_SMALL = -6666;
	
	public static final int OK = 0;
	
	protected int event = UNKNOWN;
	protected double time = 0.0;
	
	public RiseSetEvent(int event, double time) {
		this.event = event;
		this.time = time;
	}
	
	public void setEvent(int event) {
		this.event = event;
	}
	
	public int getEvent() {
		return event;
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public double getTime() {
		return time;
	}
}
