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

package com.obliquity.astronomy.almanac.phenomena;

import com.obliquity.astronomy.almanac.JPLEphemerisException;

public class ZeroFinder {
	public static double findZero(TargetFunction fn, double x1, double x2, double tol) throws JPLEphemerisException {
		while (true) {
			double dX1 = fn.valueAtTime(x1);
	
			double dX2 = fn.valueAtTime(x2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(x2 - x1);
		
			double tNew = x1 - dX1/dXrate;
		
			double dX3 = fn.valueAtTime(tNew);

			if (Math.abs(dX3) < tol)
				return tNew;
			
			if (changeOfSign(dX1, dX3))
				x2 = tNew;
			else
				x1 = tNew;
		}
	}
	
	private static boolean changeOfSign(double x1, double x2) {
		if (x1 > 0.0 && x2 > 0.0)
			return false;
		
		if (x1 < 0.0 && x2 < 0.0)
			return false;
		
		return true;
	}
}
