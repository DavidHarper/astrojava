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

package com.obliquity.astronomy;

public class Place {
	protected double latitude;
	protected double longitude;
	protected double height;
	protected double timezone;
	
	public Place(double latitude, double longitude, double height, double timezone) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.height = height;
		this.timezone = timezone;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public double getHeight() {
		return height;
	}
	
	public double getTimeZone() {
		return timezone;
	}
}
