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

package com.obliquity.astronomy.almanac;

/**
 * Refraction formula taken from the Explanatory Supplement
 */

public class Refraction {
	private static final int APPARENT = 0;
	private static final int GEOMETRIC = 1;

	private static double generalRefraction(double H, double T, double P,
			int flag) {
		final double R = Math.PI / 180.0;

		double a, f, ref0, ref;

		f = 0.28 * P / (T + 273.0);

		a = H / R;

		switch (flag) {
		case APPARENT:
			ref0 = 1.0 / Math.tan((a + 7.31 / (a + 4.4)) * R);
			break;

		case GEOMETRIC:
			ref0 = 1.02 / Math.tan((a + 10.3 / (a + 5.11)) * R);
			break;

		default:
			ref0 = 0.0;
			break;
		}

		ref = ref0 * f;

		ref = ref * R / 60.0;

		return ref;
	}

	public static double geometricRefraction(double H, double T, double P) {
		return generalRefraction(H, T, P, GEOMETRIC);
	}

	public static double apparentRefraction(double H, double T, double P) {
		return generalRefraction(H, T, P, APPARENT);
	}

	public static double refraction(double H, double T, double P) {
		return generalRefraction(H, T, P, GEOMETRIC);
	}
}
