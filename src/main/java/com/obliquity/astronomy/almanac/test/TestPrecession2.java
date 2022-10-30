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
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.Vector;

public class TestPrecession2 {
	private IAUEarthRotationModel erm = new IAUEarthRotationModel();

	public static void main(String[] args) {
		TestPrecession2 tp2 = new TestPrecession2();
		tp2.run(args);
	}

	public void run(String[] args) {
		double epoch = Double.NaN, ra = Double.NaN, dec = Double.NaN,
				pmra = 0.0, pmdec = 0.0, startYear = Double.NaN,
				endYear = Double.NaN, stepYear = 1.0;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-epoch":
				epoch = Double.parseDouble(args[++i]);
				break;

			case "-ra":
				ra = parseRA(args[++i]);
				break;

			case "-dec":
				dec = parseDec(args[++i]);
				break;

			case "-pmra":
				pmra = Double.parseDouble(args[++i]);
				break;

			case "-pmdec":
				pmdec = Double.parseDouble(args[++i]);
				break;

			case "-start":
				startYear = Double.parseDouble(args[++i]);
				break;

			case "-end":
				endYear = Double.parseDouble(args[++i]);
				break;

			case "-step":
				stepYear = Double.parseDouble(args[++i]);
				break;

			default:
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
		}

		run(epoch, ra, dec, pmra, pmdec, startYear, endYear, stepYear);
	}

	private void run(double epoch, double ra, double dec, double pmra,
			double pmdec, double startYear, double endYear, double stepYear) {
		ra *= Math.PI / 12.0;
		dec *= Math.PI / 180.0;
		pmra *= Math.PI / (180.0 * 3600.0 * 100.0);
		pmdec *= Math.PI / (180.0 * 3600.0 * 100.0);

		pmra /= Math.cos(dec);

		for (double year = startYear; year <= endYear; year += stepYear)
			precess(epoch, year, ra, dec, pmra, pmdec);
	}

	private void precess(double epoch, double year, double ra, double dec,
			double pmra, double pmdec) {
		ra += (year - epoch) * pmra;
		dec += (year - epoch) * pmdec;
		
		Vector v = new Vector(Math.cos(dec) * Math.cos(ra),
				Math.cos(dec) * Math.sin(ra), Math.sin(dec));

		Matrix pm = erm.precessionMatrix(erm.JulianEpoch(2000.0), erm.JulianEpoch(year));

		v.multiplyBy(pm);

		double x = v.getX();
		double y = v.getY();
		double z = v.getZ();

		double ra2 = Math.atan2(y, x) * 12.0 / Math.PI;
		if (ra2 < 0.0)
			ra2 += 24.0;

		double cd = Math.sqrt(x * x + y * y);

		double dec2 = Math.atan2(z, cd) * 180.0 / Math.PI;

		boolean isSouth = dec2 < 0.0;
		
		if (isSouth)
			dec2 = -dec2;
		
		int rah = (int)ra2;
		ra2 -= (double)rah;
		ra2 *= 60.0;
		int ram = (int)ra2;
		ra2 -= (double)ram;
		double ras = ra2 * 60.0;
		
		int decd = (int)dec2;
		dec2 -= (double)decd;
		dec2 *= 60.0;
		int decm = (int)dec2;
		dec2 -= (double)decm;
		double decs = dec2 * 60.0;
		
		char signum = isSouth ? '-' : '+';
		
		System.out.printf("%7.3f  %2d %02d %6.3f  %s %2d %02d %6.2f\n", year, rah, ram, ras, signum, decd, decm, decs);
	}

	private double parseRA(String str) {
		String[] words = str.split(":");

		double ra = 0.0, factor = 1.0;

		for (String word : words) {
			ra += Double.parseDouble(word) * factor;
			factor /= 60.0;
		}

		return ra;
	}

	private double parseDec(String str) {
		boolean isSouth = str.startsWith("-");

		if (isSouth)
			str = str.substring(1);

		String[] words = str.split(":");

		double dec = 0.0, factor = 1.0;

		for (String word : words) {
			dec += Double.parseDouble(word) * factor;
			factor /= 60.0;
		}

		if (isSouth)
			dec = -dec;

		return dec;
	}
}
