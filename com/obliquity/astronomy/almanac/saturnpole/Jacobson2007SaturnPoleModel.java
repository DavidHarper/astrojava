/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2018 David Harper at obliquity.com
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

package com.obliquity.astronomy.almanac.saturnpole;

public class Jacobson2007SaturnPoleModel extends AbstractSaturnPoleModel {
	public SaturnPolePosition getPolePosition(double epoch) {
		double T = (epoch - 2451545.0)/36525.0;
		
		double omega1 = (Math.PI/180.0) * (24.058014 - 50.933966 * T);
		double omega2 = (Math.PI/180.0) * (325.758187 - 10.389768 * T);
		double omega3 = (Math.PI/180.0) * (234.873847 - 10.389768 * T);
		
		double ra = 40.596731 - 0.052461 * T - 0.031396 * Math.sin(omega1) - 0.001791 * Math.sin(omega2)
				+ 0.000101 * Math.sin(omega3);
		
		double dec = 83.534290 - 0.005968 * T + 0.003517 * Math.cos(omega1) + 0.000201 * Math.cos(omega2)
				+ 0.000011 * Math.cos(omega3);
		
		return new SaturnPolePosition(Math.PI * ra/180.0, Math.PI * dec/180.0);
	}
}
