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

public class ExtremumFinder {
	/*
	 * Golden Section search.
	 * 
	 * Press, W.H., Flannery, B.P., Teukolsky, F.A., Vetterling, W.T.
	 * Numerical Recipes in FORTRAN (Second Edition, 1989)
	 * Cambridge University Press
	 * 
	 * Section 10.1.
	 */
	
	private static final double R = 0.61803399, C = 1.0 - R; 
	
	public static double findMinimum(TargetFunction fn, double xa, double xb, double xc, double tol) throws JPLEphemerisException {
		double x0 = xa;
		double x3 = xc;
		
		double x1, x2;
		
		if (Math.abs(xc-xb) > Math.abs(xb-xa)) {
			x1 = xb;
			x2 = xb + C * (xc-xb);
		} else {
			x1 = xb - C * (xb-xa);
			x2= xb;
		}
		
		double f1 = fn.valueAtTime(x1);
		double f2 = fn.valueAtTime(x2);
		
		while (Math.abs(x3-x0) > tol) {
			if (f2 < f1) {
				x0 = x1;
				x1 = x2;
				x2 = R * x1 + C * x3;
				f1 = f2;
				f2 = fn.valueAtTime(x2);
			} else {
				x3 = x2;
				x2 = x1;
				x1 = R * x2 + C * x0;
				f2 = f1;
				f1 = fn.valueAtTime(x1);
			}
		}
		
		return f1 < f2 ? x1 : x2;
	}
	
	public static double findMaximum(TargetFunction fn, double xa, double xb, double xc, double tol) throws JPLEphemerisException {
		return findMinimum(new TargetFunction() {
			public double valueAtTime(double x) throws JPLEphemerisException {
				return -fn.valueAtTime(x);
			}  }, xa, xb, xc, tol);
	}

}
