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

package com.obliquity.astronomy.almanac.test;

public class VenusMagnitudeExplorer {
	public static void main(String[] args) {
		VenusMagnitudeExplorer vme = new VenusMagnitudeExplorer();		
		vme.run();
	}
	
	public void run() {
		double rEarth = 1.0, rVenus = 0.723332;
		
		for (int i = 1; i < 293; i++) {
			double x = Math.PI * ((double)i)/292.0;
			
			double delta = Math.sqrt(rEarth * rEarth + rVenus * rVenus - 2.0 * rEarth * rVenus * Math.cos(x));
			
			double z = (delta * delta + rVenus * rVenus - rEarth * rEarth)/(2.0 * delta * rVenus);
			
			double theta = Math.acos(z);
			
			theta *= 180.0/Math.PI;
			
			x *= 180.0/Math.PI;
			
			double m1 = -4.40 + 5.0 * Math.log10(rVenus);
			
			double m2 = 5.0 * Math.log10(delta);
			
			double q = theta/100.0;
			
			double m3 = 0.09 * q + 2.39 * q * q -0.65 * q * q * q;
			
			double m = m1 + m2 + m3;
			
			double semiDiameter = 8.41/delta;
			
			double illuminatedFraction = 0.5 * (1.0 + z);
			
			double illuminatedArea = Math.PI * semiDiameter * semiDiameter * illuminatedFraction;
			
			System.out.printf("%3d  %4.1f  %7.5f  %5.2f  %7.2f  %6.2f %6.2f  %6.2f\n", i, theta, delta, semiDiameter, illuminatedArea, m2, m3, m);
		}
	}
}
