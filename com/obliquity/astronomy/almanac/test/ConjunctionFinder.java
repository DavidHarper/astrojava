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

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.asin;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class ConjunctionFinder {
	private class EclipticCoordinates {
		public double longitude, latitude;
		
		public EclipticCoordinates(double longitude, double latitude) {
			this.longitude = longitude;
			this.latitude = latitude;
		}
	};
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	private static final double TWO_PI = 2.0 * Math.PI;
	
	private ApparentPlace apTarget1 = null, apTarget2 = null, apSun = null;
	
	private EarthRotationModel erm = null;

	public ConjunctionFinder(ApparentPlace apTarget1, ApparentPlace apTarget2, ApparentPlace apSun) {
		this.apTarget1 = apTarget1;
		this.apTarget2 = apTarget2;
		this.apSun = apSun;
		this.erm = apTarget1.getEarthRotationModel();
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String body1name = null;
		String body2name = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		boolean inLongitude = false;

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

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
			
			if (args[i].equalsIgnoreCase("-longitude"))
				inLongitude = true;
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

		double jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

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
		
		ApparentPlace apSun = null;
		
		if (kBody1 == JPLEphemeris.SUN)
			apSun = apTarget1;
		else if (kBody2 == JPLEphemeris.SUN)
			apSun = apTarget2;
		else
			apSun = new ApparentPlace(earth, sun, sun, erm);

		ConjunctionFinder finder = new ConjunctionFinder(apTarget1, apTarget2, apSun);
		
		try {
			finder.run(jdstart, jdfinish, jdstep, inLongitude, System.out);
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
				"",
				"OPTIONAL PARAMETERS",
				"\t-longitude\tUse ecliptic longitude in place of Right Ascension"
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

	private void run(double jdstart, double jdfinish, double dt, boolean inLongitude,
			PrintStream ps) throws JPLEphemerisException {
		double lastDX = Double.NaN;
		boolean first = true;
		
		for (double t = jdstart; t <= jdfinish; t += dt) {
			double dX = inLongitude ? calculateDifferenceInLongitude(t) : calculateDifferenceInRightAscension(t);
			
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - dt;
					
					debug("Sign change between " + tLast + "(" + julianDateToCalendarDate(tLast) + ") and " + t + 
							" (" + julianDateToCalendarDate(t) + ") : " + lastDX + " vs " + dX);
					
					double tExact = findExactInstant(tLast, t, inLongitude);
					
					apTarget1.calculateApparentPlace(tExact);
					
					apTarget2.calculateApparentPlace(tExact);
					
					double ra1 = apTarget1.getRightAscensionOfDate();
					
					double dec1 = apTarget1.getDeclinationOfDate();
					
					EclipticCoordinates ec1 = calculateEclipticCoordinates(ra1, dec1, tExact);
					
					double ra2 = apTarget2.getRightAscensionOfDate();
					
					double dec2 = apTarget2.getDeclinationOfDate();
					
					EclipticCoordinates ec2 = calculateEclipticCoordinates(ra2, dec2, tExact);
					
					double dY = (inLongitude ? ec2.latitude - ec1.latitude : dec2 - dec1) * 180.0/Math.PI;

					apSun.calculateApparentPlace(tExact);
					
					double raSun = apSun.getRightAscensionOfDate();
					
					double decSun = apSun.getDeclinationOfDate();
					
					EclipticCoordinates ecSun = calculateEclipticCoordinates(raSun, decSun, tExact);
					
					double dLambda = reduceAngle(ec1.longitude - ecSun.longitude) * 180.0/Math.PI;
					
					AstronomicalDate ad = new AstronomicalDate(tExact);
					
					ps.printf("%5d %02d %02d %02d:%02d  %6.3f  %6.1f  %7.4f  %7.4f\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(),
							dY, dLambda, apTarget1.getGeometricDistance(), apTarget2.getGeometricDistance());
				}
			}
			
			lastDX = dX;
			first = false;
		}
	}
	
	private EclipticCoordinates calculateEclipticCoordinates(double ra, double dec, double t) {
		double xa = cos(ra) * cos(dec);
		double ya = sin(ra) * cos(dec);
		double za = sin(dec);
		
		double obliquity = erm.meanObliquity(t);
		
		double xe = xa;
		double ye = ya * cos(obliquity) + za * sin(obliquity);
		double ze = -ya * sin(obliquity) + za * cos(obliquity);
		
		return new EclipticCoordinates(atan2(ye, xe), asin(ze));
	}

	private double calculateDifferenceInRightAscension(double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		return reduceAngle(ra2 - ra1);	
	}
	
	private double calculateDifferenceInLongitude(double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double dec1 = apTarget1.getDeclinationOfDate();
		
		EclipticCoordinates ec1 = calculateEclipticCoordinates(ra1, dec1, t);
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		double dec2 = apTarget2.getDeclinationOfDate();
		
		EclipticCoordinates ec2 = calculateEclipticCoordinates(ra2, dec2, t);
		
		return reduceAngle(ec2.longitude - ec1.longitude);	
	}
	
	// Limit of difference in RA for convergence
	private final double EPSILON = 0.1 * (Math.PI/180.0)/3600.0;
	
	private double findExactInstant(double t1, double t2, boolean inLongitude) throws JPLEphemerisException {
		while (true) {
			double dX1 = inLongitude ? calculateDifferenceInLongitude(t1) : calculateDifferenceInRightAscension(t1);
	
			double dX2 = inLongitude ? calculateDifferenceInLongitude(t2) : calculateDifferenceInRightAscension(t2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(t2 - t1);
		
			double tNew = t1 - dX1/dXrate;
		
			double dX3 = inLongitude ? calculateDifferenceInLongitude(tNew) : calculateDifferenceInRightAscension(tNew);
		
			debug("\tImproved t = " + tNew + " (" + julianDateToCalendarDate(tNew) + ") => " + dX3);

			if (Math.abs(dX3) < EPSILON)
				return tNew;
			
			if (changeOfSign(dX1, dX3))
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
		if (Boolean.getBoolean("debug"))
			System.err.println(message);
	}
}
