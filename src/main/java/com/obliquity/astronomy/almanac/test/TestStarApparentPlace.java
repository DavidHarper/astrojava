/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2020 David Harper at obliquity.com
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

import java.io.IOException;

import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.Star;
import com.obliquity.astronomy.almanac.StarApparentPlace;
import com.obliquity.astronomy.almanac.Vector;

public class TestStarApparentPlace {
	private StarApparentPlace sap = null;
	private static final double J2000 = 2451545.0;
	
	public TestStarApparentPlace(StarApparentPlace sap) {
		this.sap = sap;
	}

	public static void main(String[] args) {
		String filename = null;
		String strJD = null;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-ephemeris":
				filename = args[++i];
				break;
				
			case "-jd":
				strJD = args[++i];
				break;
			}

		}
		
		if (filename == null) {
			System.err.println("You MUST specify an ephemeris file with the -ephemeris option.");
			System.exit(1);
		}
		
		if (strJD == null) {
			System.err.println("you MUST specify a target date with the -jd option.");
			System.exit(1);
		}
		
		double jd = Double.parseDouble(strJD);
		
		try {
			JPLEphemeris ephemeris = new JPLEphemeris(filename);
			
			MovingPoint earth = new EarthCentre(ephemeris);
			
			MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
			
			StarApparentPlace sap = new StarApparentPlace(earth, sun, new IAUEarthRotationModel());
			
			TestStarApparentPlace runner = new TestStarApparentPlace(sap);
			
			runner.run(jd);

		} catch (IOException | JPLEphemerisException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run(double jd) throws JPLEphemerisException {
		// The Astronomical Almanac, page B40
		double ra = 14.0 + 39.0/60.0 + 36.087/3600.0;
		ra *= Math.PI/12.0;
		
		double dec = 60.0 + 50.0/60.0 + 7.14/3600.0;
		dec = -dec * Math.PI/180.0;
		
		double parallax = 752.0;
		
		double pmRA = -494.86 * 15.0;
		
		double pmDec = 696.0;
		
		double rv = -22.2;
		
		Star star = new Star(0, 0, ra, dec, pmRA, pmDec,parallax, 0.0, rv);
		
		double fixedEpoch = J2000;
		double positionEpoch = J2000;
		
		Vector p = sap.calculateApparentPlace(star, positionEpoch, fixedEpoch, jd);
		
		displayEquatorialCoordinates(p);
	}
	
	private void displayEquatorialCoordinates(Vector p) {
		p.normalise();
		
		double ra = Math.atan2(p.getY(), p.getX());
		
		double dec = Math.asin(p.getZ());
		
		ra *= 12.0/Math.PI;
		
		if (ra < 0.0)
			ra += 24.0;
		
		int rah = (int)ra;
		
		ra -= (double)rah;
		
		ra *= 60.0;
		
		int ram = (int)ra;
		
		ra -= (double)ram;
		
		double ras = 60.0 * ra;
		
		dec *= 180.0/Math.PI;
		
		char decp = dec > 0.0 ? 'N' : 'S';
		
		if (dec < 0.0)
			dec = -dec;
		
		int decd = (int)dec;
		
		dec -= (double) decd;
		
		dec *= 60.0;
		
		int decm = (int)dec;
		
		dec -= (double)decm;
		
		double decs = 60.0 * dec;
		
		System.out.printf("    RA  %2d %02d %6.3f\n", rah, ram, ras);
		System.out.printf("    Dec %2d %02d %5.2f %s\n", decd, decm, decs, decp);
		
		System.out.println();
	}

}
