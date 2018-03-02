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

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class ConjunctionFinder {
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	private static final double TWO_PI = 2.0 * Math.PI;
	
	private ApparentPlace apTarget1 = null, apTarget2 = null;

	public ConjunctionFinder(ApparentPlace apTarget1, ApparentPlace apTarget2) {
		this.apTarget1 = apTarget1;
		this.apTarget2 = apTarget2;
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String body1name = null;
		String body2name = null;
		String startdate = null;
		String enddate = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body1"))
				body1name = args[++i];

			if (args[i].equalsIgnoreCase("-body2"))
				body2name = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];
		}

		if (filename == null || body1name == null || body2name == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody1 = parseBody(body1name);

		if (kBody1 < 0) {
			System.err.println("Unknown name for -body1: \"" + body1name + "\"");
			System.exit(1);
		}

		int kBody2 = parseBody(body2name);

		if (kBody2 < 0) {
			System.err.println("Unknown name for -body2: \"" + body2name + "\"");
			System.exit(1);
		}
		
		if (kBody1 == kBody2) {
			System.err.println("Target bodies are the same.");
			System.exit(1);
		}

		Date date = null;

		try {
			date = datefmtIn.parse(startdate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		try {
			date = datefmtIn.parse(enddate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdfinish = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

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

		MovingPoint planet1 = null;

		if (kBody1 == JPLEphemeris.MOON)
			planet1 = new MoonCentre(ephemeris);
		else
			planet1 = new PlanetCentre(ephemeris, kBody1);

		MovingPoint planet2 = null;

		if (kBody2 == JPLEphemeris.MOON)
			planet2 = new MoonCentre(ephemeris);
		else
			planet2 = new PlanetCentre(ephemeris, kBody2);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		if (kBody1 == JPLEphemeris.SUN)
			sun = planet1;
		else if (kBody2 == JPLEphemeris.SUN)
			sun = planet2;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace apTarget1 = new ApparentPlace(earth, planet1, sun, erm);

		ApparentPlace apTarget2 = new ApparentPlace(earth, planet2, sun, erm);

		ConjunctionFinder finder = new ConjunctionFinder(apTarget1, apTarget2);
		
		try {
			finder.run(jdstart, jdfinish, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-body1\t\tName of body 1",
				"\t-body2\t\tName of body 2",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
		};
		
		for (String line : lines)
			System.err.println(line);
	}

	private static int parseBody(String bodyname) {
		if (bodyname.equalsIgnoreCase("sun"))
			return JPLEphemeris.SUN;

		if (bodyname.equalsIgnoreCase("moon"))
			return JPLEphemeris.MOON;

		if (bodyname.equalsIgnoreCase("mercury"))
			return JPLEphemeris.MERCURY;

		if (bodyname.equalsIgnoreCase("venus"))
			return JPLEphemeris.VENUS;

		if (bodyname.equalsIgnoreCase("mars"))
			return JPLEphemeris.MARS;

		if (bodyname.equalsIgnoreCase("jupiter"))
			return JPLEphemeris.JUPITER;

		if (bodyname.equalsIgnoreCase("saturn"))
			return JPLEphemeris.SATURN;

		if (bodyname.equalsIgnoreCase("uranus"))
			return JPLEphemeris.URANUS;

		if (bodyname.equalsIgnoreCase("neptune"))
			return JPLEphemeris.NEPTUNE;

		if (bodyname.equalsIgnoreCase("pluto"))
			return JPLEphemeris.PLUTO;

		return -1;
	}
	
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWO_PI;
		
		while (x <= -Math.PI)
			x+= TWO_PI;
		
		return x;
	}

	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private String julianDateToCalendarDate(double t) {
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

		long ticks = (long) dticks;

		Date date = new Date(ticks);

		return datefmt.format(date);
	}

	private void run(double jdstart, double jdfinish, PrintStream ps) throws JPLEphemerisException {
		double lastDRA = Double.NaN;
		boolean first = true;
		
		double dt = 1.0;
		
		for (double t = jdstart; t <= jdfinish; t += dt) {
			double dRA = calculateDifferenceInRightAscension(t);
			
			if (!first) {
				if (changeOfSign(lastDRA, dRA)) {
					double tLast = t - dt;
					
					debug("Sign change between " + tLast + "(" + julianDateToCalendarDate(tLast) + ") and " + t + 
							" (" + julianDateToCalendarDate(t) + ") : " + lastDRA + " vs " + dRA);
					
					double tExact = findExactInstant(tLast, t);
				}
			}
			
			lastDRA = dRA;
			first = false;
		}
	}
	
	private double calculateDifferenceInRightAscension(double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		return reduceAngle(ra2 - ra1);	
	}
	
	// Limit of difference in RA for convergence
	private final double EPSILON = 0.1 * (Math.PI/180.0)/3600.0;
	
	private double findExactInstant(double t1, double t2) throws JPLEphemerisException {
		while (true) {
			double dRA1 = calculateDifferenceInRightAscension(t1);
	
			double dRA2 = calculateDifferenceInRightAscension(t2);
		
			double dRAchange = dRA2 - dRA1;
		
			double dRArate = dRAchange/(t2 - t1);
		
			double tNew = t1 - dRA1/dRArate;
		
			double dRA3 = calculateDifferenceInRightAscension(tNew);
		
			debug("\tImproved t = " + tNew + " (" + julianDateToCalendarDate(tNew) + ") => " + dRA3);

			if (Math.abs(dRA3) < EPSILON)
				return tNew;
			
			if (changeOfSign(dRA1, dRA3))
				t2 = tNew;
			else
				t1 = tNew;
		}
	}
	
	private boolean changeOfSign(double x1, double x2) {
		if (x1 > 0.0 && x2 > 0.0)
			return false;
		
		if (x1 < 0.0 && x2 < 0.0)
			return false;
	
		// Exclude signs change between -PI and +PI
		if (Math.abs(x1) > 2.0 || Math.abs(x2) > 2.0)
			return false;
		
		return true;
	}
	
	private void debug(String message) {
		System.err.println(message);
	}
}
