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

package com.obliquity.astronomy.almanac.test;

import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.Vector;

public class SaturnPoleComparison {
	private IAUEarthRotationModel erm = new IAUEarthRotationModel();
	
	private class PolePosition {
		public double rightAscension = 0.0;
		public double declination = 0.0;
		
		public PolePosition(double rightAscension, double declination) {
			this.rightAscension = rightAscension;
			this.declination = declination;
		}
	}
	
	public static void main(String[] args) {
		SaturnPoleComparison runner = new SaturnPoleComparison();
		
		runner.run();
	}
	
	public void run() {
		double J2000 = erm.JulianEpoch(2000.0);
		
		double B1875 = erm.BesselianEpoch(1889.25);
		
		PolePosition struvePole = nodeAndInclinationToPolePosition(167.9680, 28.0758, B1875, J2000);
		
		double B1950 = erm.BesselianEpoch(1950.0);
		
		PolePosition dourneauPole = nodeAndInclinationToPolePosition(168.8112, 28.0817, B1950, J2000);
		
		PolePosition harperTaylorPole = nodeAndInclinationToPolePosition(168.8387, 28.0653, B1950, J2000);
		
		PolePosition jacobsonPole = jacobson2007PolePosition(J2000);
				
		PolePosition iauPole = iauPolePosition(J2000);
		
		showPole("IAU", iauPole, iauPole);
		showPole("Struve", struvePole, iauPole);
		showPole("Dourneau", dourneauPole, iauPole);
		showPole("Harper/Taylor", harperTaylorPole, iauPole);
		showPole("Jacobson", jacobsonPole, iauPole);
	}
	
	private void showPole(String name, PolePosition pole, PolePosition refPole) {
		double dy = pole.declination - refPole.declination;
		double dx = (pole.rightAscension - refPole.rightAscension) * Math.cos(refPole.declination * Math.PI/180.0);
		
		String format = "   %3s: %8.4f  %8.4f\n";
		
		System.out.println(name);
		System.out.printf(format, "RA", pole.rightAscension, dx);
		System.out.printf(format, "Dec", pole.declination, dy);
		System.out.println();
	}
	
	private PolePosition iauPolePosition(double epoch) {
		double T = (epoch - 2451545.0)/36525.0;
		
		double ra = 40.58 - 0.036 * T;
		
		double dec = 83.54 - 0.004 * T;
		
		return new PolePosition(ra, dec);
	}

	private PolePosition jacobson2007PolePosition(double epoch) {
		double T = (epoch - 2451545.0)/36525.0;
		
		double omega1 = (Math.PI/180.0) * (24.058014 - 50.933966 * T);
		double omega2 = (Math.PI/180.0) * (325.758187 - 10.389768 * T);
		double omega3 = (Math.PI/180.0) * (234.873847 - 10.389768 * T);
		
		double ra = 40.596731 - 0.052461 * T - 0.031396 * Math.sin(omega1) - 0.001791 * Math.sin(omega2)
				+ 0.000101 * Math.sin(omega3);
		
		double dec = 83.534290 - 0.005968 * T + 0.003517 * Math.cos(omega1) + 0.000201 * Math.cos(omega2)
				+ 0.000011 * Math.cos(omega3);
		
		return new PolePosition(ra, dec);
	}

	private PolePosition nodeAndInclinationToPolePosition(double node, double inclination,
			double sourceEpoch, double targetEpoch) {
		Matrix precession = erm.precessionMatrix(sourceEpoch, targetEpoch);
		
		double obliquity = erm.meanObliquity(sourceEpoch);
		
		double lngPole = node * Math.PI/180.0 - 0.5 * Math.PI;
		
		double latPole = 0.5 * Math.PI - inclination * Math.PI/180.0;
		
		double xe = Math.cos(lngPole) * Math.cos(latPole);
		double ye = Math.sin(lngPole) * Math.cos(latPole);
		double ze = Math.sin(latPole);
		
		double ce = Math.cos(obliquity);
		double se = Math.sin(obliquity);
		
		double xa0 = xe;
		double ya0 = ce * ye - se * ze;
		double za0 = se * ye + ce * ze;
		
		Vector v = new Vector(xa0, ya0, za0);
		
		v.multiplyBy(precession);
		
		double ra = Math.atan2(v.getY(), v.getX()) * 180.0/Math.PI;
		
		if (ra < 0.0)
			ra += 360.0;
		
		double dec = Math.asin(v.getZ()) * 180.0/Math.PI;
		
		return new PolePosition(ra, dec);
	}
}
