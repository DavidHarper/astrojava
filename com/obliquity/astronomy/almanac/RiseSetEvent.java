/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2014 David Harper at obliquity.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 */

package com.obliquity.astronomy.almanac;

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
	
	public String getEventAsString() {
		return eventTypeToString(event);
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public double getTime() {
		return time;
	}
	
	public String toString() {
		return "RiseSetEvent[" + eventTypeToString(event) + ", time = " + time + "]";
	}
	
	public static String eventTypeToString(int type) {
		switch (type) {
		case RISING: return "RISING";
		
		case SETTING: return "SETTING";
		
		default: return "UNKNOWN";
		}
	}
}
