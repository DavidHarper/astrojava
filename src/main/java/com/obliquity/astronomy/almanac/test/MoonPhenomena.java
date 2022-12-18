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

import static java.lang.Math.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class MoonPhenomena {
	private static final double TWO_PI = 2.0 * PI;
	
	private static final double LUNAR_MONTH = 29.53059;
	
	private static final double EPSILON = 0.5/86400.0;
	
	public static final int NEW_MOON = 0, FIRST_QUARTER = 1, FULL_MOON = 2, LAST_QUARTER = 3;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private ApparentPlace apSun, apMoon;
	
	private static final String[] dayOfWeek = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	
	public static void main(String args[]) {
		SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		final double UNIX_EPOCH_AS_JD = 2440587.5;
		 final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
		String filename = null;
		String startdate = null;
		String enddate = null;
		boolean useUT = false;
		boolean showSeconds = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];
			
			if (args[i].equalsIgnoreCase("-ut"))
				useUT = true;
			
			if (args[i].equalsIgnoreCase("-seconds"))
				showSeconds = true;
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

		MoonPhenomena mp = new MoonPhenomena(ephemeris);
		
		double t = jdstart;
		
		datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		while (t < jdfinish) {
			try {
				t = mp.getDateOfNextPhase(t, FULL_MOON, useUT);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			AstronomicalDate ad = new AstronomicalDate(t);
			
			if (!showSeconds)
				ad.roundToNearestMinute();
			
			int dow = ((int)(t + 3500000.5)) % 7;
			
			System.out.printf("%4d %2d %2d %02d:%02d", ad.getYear(), ad.getMonth(), ad.getDay(),
					ad.getHour(), ad.getMinute());
			
			if (showSeconds)
				System.out.printf(":%02d", (int)ad.getSecond());
			
			System.out.printf(" %s\n", dayOfWeek[dow]);
			
			t += 29.0;
		}
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-ut\t\tDisplay times in UT instead of TT");
		System.err.println("\t-seconds\tDisplay time to nearest second");
	}

	public MoonPhenomena(JPLEphemeris ephemeris) {
		MovingPoint moon = new MoonCentre(ephemeris);
		
		MovingPoint earth = new EarthCentre(ephemeris);
		
		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);

		apMoon = new ApparentPlace(earth, moon, sun, erm);
	}
	
	private double getLunarElongation(double t) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t);
		
		double raSun = apSun.getRightAscensionOfDate();
		
		double decSun = apSun.getDeclinationOfDate();
		
		apMoon.calculateApparentPlace(t);
		
		double raMoon = apMoon.getRightAscensionOfDate();
		
		double decMoon = apMoon.getDeclinationOfDate();
		
		double eps = erm.meanObliquity(t);
		
		NutationAngles na = erm.nutationAngles(t);
		
		eps += na.getDeps();
		
		double xMoon = cos(decMoon) * cos(raMoon);
		double yMoon = cos(decMoon) * sin(raMoon) * cos(eps) + sin(decMoon) * sin(eps);

		double xSun = cos(decSun) * cos(raSun);
		double ySun = cos(decSun) * sin(raSun) * cos(eps) + sin(decSun) * sin(eps);

		double lMoon = atan2(yMoon, xMoon);
		
		double lSun = atan2(ySun, xSun);

		double elong = (lMoon - lSun) % TWO_PI;
		
		if (elong < 0.0)
			elong += TWO_PI;
		
		return elong;
	}
	
	public double getDateOfNextPhase(double t0, int phase, boolean useUT) throws JPLEphemerisException {
		double d = getLunarElongation(t0);
		
		double dWanted = 0.5 * PI * (double)(phase % 4);
		
		double dt = dWanted - d;
		
		while (dt < 0.0)
			dt += TWO_PI;
				
		dt *= LUNAR_MONTH/TWO_PI;
				
		double t = t0 + dt;
		
		while (abs(dt) > EPSILON) {
			d = getLunarElongation(t);
			
			dt = (dWanted - d) % TWO_PI;
			dt *= LUNAR_MONTH/TWO_PI;
			
			t += dt;
		}
		
		if (useUT)
			t -= erm.deltaT(t);
		
		return t;
	}
}
