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

public class Place {
	public static final double FLATTENING = 1.0/298.25;
	
	protected double latitude;
	protected double longitude;
	protected double height;
	protected double timezone;
	
	protected double geocentricLatitude;
	protected double geocentricDistance;
	
	public Place(double latitude, double longitude, double height, double timezone) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.height = height;
		this.timezone = timezone;
		
		double sphi = Math.sin(latitude);
		double cphi = Math.cos(latitude);
		
		double q = Math.pow(1.0 - FLATTENING, 2.0);
		
		double C = 1.0/Math.sqrt(cphi * cphi + q * sphi * sphi);
		double S = q * C;
		
		double y = S * sphi;
		double x = C * cphi;
		
		geocentricLatitude = Math.atan2(y, x);
		
		geocentricDistance = Math.sqrt(x * x + y * y);
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
	
	public double getGeocentricLatitude() {
		return geocentricLatitude;
	}
	
	public double getGeocentricDistance() {
		return geocentricDistance;
	}
}
