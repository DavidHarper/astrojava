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

import static java.lang.Math.*;

import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class LunarEclipses {
	private static final double LUNAR_MONTH = 29.53059;
	
	private static final double EARTH_RADIUS = 6378.0, MOON_RADIUS = 1738.0, SUN_RADIUS = 696000.0;
	
	private static final double MEAN_EARTH_RADIUS = 0.998340 * EARTH_RADIUS;
	
	private static final double SEMI_INTERVAL = 120.0;
	
	private static final double RADIANS_TO_ARCSEC = 3600.0 * 180.0/Math.PI;

	private final DecimalFormat dfmta = new DecimalFormat("#0.000");
	private final DecimalFormat dfmtb = new DecimalFormat("##0.0");

	private final DecimalFormat fmtSeconds = new DecimalFormat("00.00");
	private final DecimalFormat fmtYear = new DecimalFormat(" 0000;-0000");
	private final DecimalFormat fmtTwoDigits = new DecimalFormat("00");

	private final SimpleDateFormat datefmt = new SimpleDateFormat("G yyyy-MM-dd HH:mm:ss");
	private final SimpleDateFormat prefixfmt = new SimpleDateFormat("yyyyMMdd: ");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private final double AU;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private ApparentPlace apSun, apMoon;
	
	public static void main(String args[]) {
		SimpleDateFormat parsefmt = new SimpleDateFormat("yyyy-MM-dd");
		parsefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	
		String filename = null;
		String startdate = null;
		String enddate = null;
		boolean onlyTotal = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];
			
			if (args[i].equalsIgnoreCase("-only-total"))
				onlyTotal = true;
		}

		if (filename == null || startdate == null || enddate == null) {
			showUsage();
			System.exit(1);
		}
		
		Date date = null;
		
		try {
			date = parsefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = parsefmt.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY + LUNAR_MONTH;
		
		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(filename, jdstart - 1.0,
					jdfinish + 1.0);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}

		MoonPhases mp = new MoonPhases(ephemeris);
		
		LunarEclipses le = new LunarEclipses(ephemeris);
		
		double t = jdstart;
		
		while (t < jdfinish) {
			try {
				t = mp.getDateOfNextPhase(t, MoonPhases.FULL_MOON);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			try {
				le.testForLunarEclipse(t, onlyTotal);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			t += 29.0;
		}
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-only-total\tOnly list total eclipses");
	}

	public LunarEclipses(JPLEphemeris ephemeris) {
		MovingPoint moon = new MoonCentre(ephemeris);
		
		MovingPoint earth = new EarthCentre(ephemeris);
		
		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);

		apMoon = new ApparentPlace(earth, moon, sun, erm);
		
		AU = ephemeris.getAU();
		
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
				
		DateFormatSymbols dfs = datefmt.getDateFormatSymbols();
		
		String[] eras = {"BCE", " CE"};
		dfs.setEras(eras);
		
		datefmt.setDateFormatSymbols(dfs);
		
		prefixfmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	// Reference: Explanatory Supplement to the Astronomical Almanac, first edition (1961),
	// fourth impression (1977).  Section 9E, "Lunar Eclipses"
	public void testForLunarEclipse(double t0, boolean onlyTotal) throws JPLEphemerisException {
		boolean debug = Boolean.getBoolean("debug");
		
		if (debug)
			System.out.println("\n\nDEBUG: Testing for lunar eclipse with t0 = " + t0);
		
		double xx[] = new double[3], yy[] = new double[3], tt[] = { -SEMI_INTERVAL, 0.0, SEMI_INTERVAL };
		
		for (int j = 0; j < tt.length; j++) {
			double dm = tt[j];
			
			double t = t0 + dm/1440.0;
			
			apSun.calculateApparentPlace(t);
			apMoon.calculateApparentPlace(t);

			double raVertex = apSun.getRightAscensionOfDate() + PI;
			
			double decVertex = -apSun.getDeclinationOfDate();
			
			double raMoon = apMoon.getRightAscensionOfDate();
			
			double decMoon = apMoon.getDeclinationOfDate();
			
			xx[j] = cos(decMoon) * sin(raMoon - raVertex);
			
			yy[j] = cos(decVertex) * sin(decMoon) - sin(decVertex) * cos(decMoon) * cos(raMoon - raVertex);
			
			if (debug)
				System.out.println("DEBUG: Interpolation point t = " + t + " : x = " + xx[j] + ", y = " + yy[j]);
		}
		
		double xDot = (xx[2] - xx[0])/(2.0 * SEMI_INTERVAL);
		double yDot = (yy[2] - yy[0])/(2.0 * SEMI_INTERVAL);
		
		if (debug)
			System.out.println("DEBUG: xDot = " + xDot + ", yDot = " + yDot);
		
		double x0 = xx[1];
		double y0 = yy[1];
		
		double tMin = - (x0 * xDot + y0 * yDot)/(xDot * xDot + yDot * yDot);
		
		if (debug)
			System.out.println("DEBUG: Correction to time for closest approach is " + tMin);
		
		double xMin = x0 + xDot * tMin;
		double yMin = y0 + yDot * tMin;
		
		double n = sqrt(xDot * xDot + yDot * yDot);
		
		double delta = abs(x0 * yDot - y0 * xDot)/n;
		
		tMin = t0 + tMin/1440.0;

		apSun.calculateApparentPlace(tMin);
		apMoon.calculateApparentPlace(tMin);
		
		double rSun = AU * apSun.getGeometricDistance();
		
		double parallaxSun = asin(EARTH_RADIUS/rSun);
		double semiDiameterSun = asin(SUN_RADIUS/rSun);
		
		double rMoon = AU * apMoon.getGeometricDistance();
		
		double parallaxMoon = asin(MEAN_EARTH_RADIUS/rMoon);
		double semiDiameterMoon = asin(MOON_RADIUS/rMoon);
		
		if (debug)
			System.out.println("DEBUG: parallax of Sun = " + (parallaxSun * RADIANS_TO_ARCSEC)  +
					", semi-diameter of Sun = " + (semiDiameterSun * RADIANS_TO_ARCSEC) +
					", parallax of Moon = " + (parallaxMoon * RADIANS_TO_ARCSEC) +
					", semi-diameter of Moon = " + (semiDiameterMoon * RADIANS_TO_ARCSEC));
		
		// Radius of the penumbra at the distance of the Moon
		double f1 = 1.02 * (parallaxMoon + semiDiameterSun + parallaxSun);
		
		// Radius of the umbra at the distance of the Moon
		double f2 = 1.02 * (parallaxMoon - semiDiameterSun + parallaxSun);
		
		if (debug)
			System.out.println("DEBUG: f1 = " + f1 + ", f2 = " + f2 );
		
		// Start/end of penumbral phase
		double L1 = f1 + semiDiameterMoon;
		
		// Start/end of partial phase
		double L2 = f2 + semiDiameterMoon;
		
		// Start/end of total phase
		double L3 = f2 - semiDiameterMoon;
		
		if (debug)
			System.out.println("DEBUG: L1 = " + L1 + ", L2 = " + L2 + ", L3 = " + L3);
		
		double q1 = L1 * L1 - delta * delta;
		
		// Test for no eclipse
		if (q1 < 0.0)
			return;
		
		double q2 = L2 * L2 - delta * delta;
		
		// Test for penumbral eclipse
		if (q2 < 0.0)
			return;
		
		double q3 = L3 * L3 - delta * delta;
		
		// Test for total eclipse
		if (q3 < 0.0 && onlyTotal)
			return;
		
		double mMin = sqrt(xMin * xMin + yMin * yMin);
		
		double maxMag = (L2 - mMin)/(2.0 * semiDiameterMoon);
		
		if (debug)
			System.out.println("DEBUG: q2 = " + q2 + ", q3 = " + q3 + ", mMin = " + mMin + ", max magnitude = " + dfmta.format(maxMag));
		
		String eclipseType = q3 < 0.0 ? "PARTIAL" : "TOTAL";
		
		double partialDuration = 2.0 * sqrt(q2)/n;
		
		double totalDuration = q3 < 0.0 ? 0.0 : 2.0 * sqrt(q3)/n;
		
		if (debug)
			System.out.println("DEBUG: partial duration = " + partialDuration + ", total duration = " + totalDuration);
		
		AstronomicalDate date = new AstronomicalDate(tMin);
		
		String dateString = fmtYear.format(date.getYear()) + "-" + fmtTwoDigits.format(date.getMonth()) +
				"-" + fmtTwoDigits.format(date.getDay()) + " " + fmtTwoDigits.format(date.getHour()) +
				":" + fmtTwoDigits.format(date.getMinute()) + ":" + fmtSeconds.format(date.getSecond());
		
		System.out.println(dateString + " " + eclipseType + " " + dfmta.format(maxMag) +
				" " + dfmtb.format(partialDuration) +
				" " + dfmtb.format(totalDuration));
	}
}
