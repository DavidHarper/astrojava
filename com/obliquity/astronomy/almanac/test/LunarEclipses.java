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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class LunarEclipses {
	private static final char TAB = '\t';
	
	private static final double LUNAR_MONTH = 29.53059;
	
	private static final double EARTH_RADIUS = 6378.0, MOON_RADIUS = 1737.0, SUN_RADIUS = 695700.0;
	
	private static final double ADJUSTED_EARTH_RADIUS = EARTH_RADIUS * 1.02;

	private final DecimalFormat dfmta = new DecimalFormat("#0.000");
	
	private final double AU;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private ApparentPlace apSun, apMoon;
	
	public static void main(String args[]) {
		SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		final double UNIX_EPOCH_AS_JD = 2440587.5;
		final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
		String filename = null;
		String startdate = null;
		String enddate = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];
		}

		if (filename == null || startdate == null || enddate == null) {
			showUsage();
			System.exit(1);
		}
		
		Date date = null;
		
		try {
			date = datefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = datefmt.parse(enddate);
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
		
		datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		while (t < jdfinish) {
			try {
				t = mp.getDateOfNextPhase(t, MoonPhases.FULL_MOON);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);
			
			long ticks = (long)dticks;
			
			date = new Date(ticks);
			
			System.out.println(datefmt.format(date));
			
			try {
				le.testForLunarEclipse(t);
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
	}

	public LunarEclipses(JPLEphemeris ephemeris) {
		MovingPoint moon = new MoonCentre(ephemeris);
		
		MovingPoint earth = new EarthCentre(ephemeris);
		
		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);

		apMoon = new ApparentPlace(earth, moon, sun, erm);
		
		AU = ephemeris.getAU();
	}

	public void testForLunarEclipse(double t0) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t0);
		apMoon.calculateApparentPlace(t0);
		
		double rSun = AU * apSun.getGeometricDistance();
		double rMoon = AU * apMoon.getGeometricDistance();
		
		// Half-angle at vertex of umbral cone
		double theta = asin((SUN_RADIUS - ADJUSTED_EARTH_RADIUS)/rSun);
		
		// Complement of theta
		double beta = 0.5 * PI - theta;
		
		double alpha1 = acos((ADJUSTED_EARTH_RADIUS - MOON_RADIUS)/rMoon);
		
		double gamma1 = beta - alpha1;
		gamma1 *= 180.0/PI;
		
		double alpha2 = acos((ADJUSTED_EARTH_RADIUS + MOON_RADIUS)/rMoon);
		
		double gamma2 = beta - alpha2;
		gamma2 *= 180.0/PI;
		
		double raVertex = apSun.getRightAscensionOfDate();
		
		double decVertex = apSun.getDeclinationOfDate();
		
		double raMoon = apMoon.getRightAscensionOfDate();
		
		double decMoon = apMoon.getDeclinationOfDate();

		double q = sin(decMoon) * sin(decVertex) + cos(decMoon) * cos(decVertex) * cos(raMoon-raVertex);
		
		q = 180.0 - acos(q) * 180.0/PI;

		if (q < gamma1) {
			System.out.println(TAB + "TOTAL ECLIPSE");
		} else if (q < gamma2) {
			System.out.println(TAB + "PARTIAL ECLIPSE");
		} else
			return;
		
		System.out.println(TAB + "gamma1 = " + dfmta.format(gamma1));
		System.out.println(TAB + "gamma2 = " + dfmta.format(gamma2));
		
		double xStart = 0.0, yStart = 0.0, xEnd = 0.0, yEnd = 0.0, x0 = 0.0, y0 = 0.0;
		
		for (int j = -90; j < 100; j += 10) {
			double dm = (double)j;
			
			double t = t0 + dm/1440.0;
			
			apSun.calculateApparentPlace(t);
			apMoon.calculateApparentPlace(t);

			raVertex = apSun.getRightAscensionOfDate() + PI;
			
			decVertex = -apSun.getDeclinationOfDate();
			
			raMoon = apMoon.getRightAscensionOfDate();
			
			decMoon = apMoon.getDeclinationOfDate();
			
			double x = ((raMoon - raVertex) * 180.0/PI) % 360.0;
			
			while (x < -180.0)
				x += 360.0;
			
			x *= cos(decVertex);

			double y = (decMoon - decVertex) * 180.0/PI;
			
			switch (j) {
			case -90:
				xStart = x;
				yStart = y;
				break;
			
			case 0:
				x0 = x;
				y0 = y;
				break;
				
			case 90:
				xEnd = x;
				yEnd = y;
				break;
			}
			
			q = sin(decMoon) * sin(decVertex) + cos(decMoon) * cos(decVertex) * cos(raMoon-raVertex);
			
			q = acos(q) * 180.0/PI;
			
			System.out.println(TAB + dfmta.format(dm) + "    " + dfmta.format(x) + "    " + dfmta.format(y) + "    " + dfmta.format(q));
		}
		
		double xDot = (xEnd - xStart)/180.0;
		double yDot = (yEnd - yStart)/180.0;
		
		double tMin = - (x0 * xDot + y0 * yDot)/(xDot * xDot + yDot * yDot);
		
		double x = x0 + xDot * tMin;
		double y = y0 + yDot * tMin;
		
		q = sqrt(x * x + y * y);
		
		System.out.println(TAB + "Minimum distance is at " + dfmta.format(tMin) + " : x = " + dfmta.format(x) + ", y = " + dfmta.format(y) +
				", q = " + dfmta.format(q));
	} 
}
