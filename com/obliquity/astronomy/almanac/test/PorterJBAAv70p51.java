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

import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.Vector;

/*
 * An investigation of the calculation of the dates of Earth's passage through
 * the ring plane of Saturn by J.G. Porter in the Journal of the British
 * Astronomical Association, volume 70, pages 51ff.
 */

public class PorterJBAAv70p51 {
	public static void main(String[] args) {
		double ra, dec;
		
		// Coordinates are calculates from DE406 for the mean equator and equinox of J2000.
		
		// 1965 December 13
		ra = (Math.PI/12.0) * dms2d(22, 55, 30.869);
		dec = -(Math.PI/180.0) * dms2d(9, 0, 29.34);
		
		Vector v1965dec = radec2dc(ra, dec);
		
		// 1966 January 12
		ra = (Math.PI/12.0) * dms2d(23, 3, 24.195);
		dec = -(Math.PI/180.0) * dms2d(8, 7, 31.84);
		
		Vector v1966jan = radec2dc(ra, dec);
		
		// 1966 August 22
		ra = (Math.PI/12.0) * dms2d(23, 59, 26.535);
		dec = -(Math.PI/180.0) * dms2d(2, 41, 53.70);
		
		Vector v1966aug = radec2dc(ra, dec);
		
		Vector v1x3 = v1965dec.vectorProduct(v1966aug);
		
		v1x3.normalise();
		
		double v1x3d2 = v1x3.scalarProduct(v1966jan);
		
		System.out.println("(V1 x V3) . V2 = " + v1x3d2);
		
		ra = Math.atan2(v1x3.getY(), v1x3.getX());
		
		if (ra < 0.0)
			ra += 2.0 * Math.PI;
		
		dec = Math.asin(v1x3.getZ());
		
		System.out.println("Pole of V1 x V3 is: RA = " + (ra * 12.0/Math.PI) + " h (" + (ra * 180.0/Math.PI) + " degrees), Dec = " + (dec * 180.0/Math.PI) + " degrees");
		
		Vector pole = radec2dc(ra, dec);
		
		System.out.println("Pole vector: " + pole);
		
		IAUEarthRotationModel erm = new IAUEarthRotationModel();
		
		double eps = erm.meanObliquity(2451545.0);
		
		double ce = Math.cos(eps);
		
		double se = Math.sin(eps);
		
		System.out.println("cos(eps) = " + ce + ", sin(eps) = " + se);
		
		double x = pole.getX();
		
		double y = ce * pole.getY() + se * pole.getZ();
		
		double z = -se * pole.getY() + ce * pole.getZ();
		
		System.out.println("Pole vector in ecliptic coordinates: (" + x + ", " + y + ", " + z + ")");
		
		double lambda = Math.atan2(y,x) * 180.0/Math.PI;
		
		if (lambda < 0.0)
			lambda += 360.0;
		
		double beta = Math.asin(z) * 180.0/Math.PI;
		
		System.out.println("Referred to mean ecliptic and equinox: longitude " + lambda + ", latitude " + beta);
		
		System.out.println("Pole vector dot v1965dec = " + pole.scalarProduct(v1965dec));
		System.out.println("Pole vector dot v1966jan = " + pole.scalarProduct(v1966jan));
		System.out.println("Pole vector dot v1966aug = " + pole.scalarProduct(v1966aug));
	}
	
	private static double dms2d(int d, int m, double s) {
		return (double)d + (double)m/60.0 + s/3600.0;
	}
	
	private static Vector radec2dc(double ra, double dec) {
		double x = Math.cos(ra) * Math.cos(dec);
		double y = Math.sin(ra) * Math.cos(dec);
		double z = Math.sin(dec);
		
		return new Vector(x,y,z);
	}
}
