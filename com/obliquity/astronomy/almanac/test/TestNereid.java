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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Vector;

public class TestNereid {
	public static void main(String[] args) {
		TestNereid tester = new TestNereid();
		
		try {
			tester.run();
		} catch (JPLEphemerisException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This test program expects an input file generated using the JPL Horizons
	 * web site.  Appropriate settings:
	 * 
	 * Target body: Nereid (802)
	 * Center body: Neptune (899)
	 * Output units    : KM-D                                                         
	 * Output type     : GEOMETRIC cartesian states
	 * Output format   : 2 (position and velocity)
	 * Reference frame : ICRF/J2000.0                                                 
	 * Coordinate systm: Earth Mean Equator and Equinox of Reference Epoch            
	 */
	
	public void run() throws JPLEphemerisException, IOException {
		Nereid nereid = new Nereid();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		String format1 = "%10.2f %13.2f %13.2f %13.2f\n";
		String format2 = "%10s %13.2f %13.2f %13.2f\n";
		
		while (true) {
			String line1 = br.readLine();
			
			if (line1 == null)
				break;
			
			String line2 = br.readLine();
			
			if (line2 == null)
				break;
			
			String line3 = br.readLine();
			
			if (line3 == null)
				break;
			
			double t = Double.parseDouble(line1.substring(0, 18));
			
			Vector p0 = parseVector(line2);
			Vector v0 = parseVector(line3);
			
			Vector z0 = p0.vectorProduct(v0);
			
			Vector t0 = z0.vectorProduct(p0);
			
			t0.normalise();
			
			Vector p = nereid.getPosition(t);
			
			System.out.printf(format1, t, p.getX(), p.getY(), p.getZ());
			System.out.printf(format2, "", p0.getX(), p0.getY(), p0.getZ());
			
			p.subtract(p0);
			
			System.out.printf(format2, "", p.getX(), p.getY(), p.getZ());
			
			p0.normalise();
			z0.normalise();
			
			System.out.printf(format2, "", p.scalarProduct(p0), p.scalarProduct(t0), p.scalarProduct(z0));
			
			System.out.println();
		}
	}
	
	private Vector parseVector(String line) {
		double x = Double.parseDouble(line.substring(4, 26));
		double y = Double.parseDouble(line.substring(30, 52));
		double z = Double.parseDouble(line.substring(56, 78));
		
		return new Vector(x, y, z);
	}
}
