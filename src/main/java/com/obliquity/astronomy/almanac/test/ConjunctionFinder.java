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
	
	private static final int WILDCARD = 999;
	
	private JPLEphemeris ephemeris = null;
	
	private EarthCentre earth = null;
	
	private PlanetCentre sun = null;
	
	private ApparentPlace apSun = null;
	
	private ApparentPlace[] apPlanets = new ApparentPlace[JPLEphemeris.LAST_COMPONENT + 1];
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private String[] bodyNames = { "Mercury", "Venus", "Earth-Moon Barycentre", "Mars", "Jupiter", "Saturn",
			"Uranus", "Neptune", "Pluto", "Moon", "Sun"
	};

	public ConjunctionFinder(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;

		earth = new EarthCentre(ephemeris);
		
		sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);
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

		if (filename == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody1 = body1name == null ? WILDCARD : parseBody(body1name);

		if (kBody1 < 0) {
			System.err.println("Unknown name for -body1: \"" + body1name + "\"");
			System.exit(1);
		}

		int kBody2 = body2name == null ? WILDCARD : parseBody(body2name);

		if (kBody2 < 0) {
			System.err.println("Unknown name for -body2: \"" + body2name + "\"");
			System.exit(1);
		}
		
		if (kBody1 == kBody2 && kBody1 != WILDCARD) {
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

		ConjunctionFinder finder = new ConjunctionFinder(ephemeris);
		
		try {
			finder.run(kBody1, kBody2, jdstart, jdfinish, jdstep, inLongitude, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"OPTIONAL PARAMETERS",
				"\t-body1\t\tName of body 1 [default: All planets, Sun, Moon]",
				"\t-body2\t\tName of body 2 [default: All planets, Sun, Moon]",
				"\t-longitude\tUse ecliptic longitude in place of Right Ascension",
				"",
				"OUTPUT FORMAT",
				"\tEach line gives the date and time at conjunction in this format:",
				"",
				"\tYYYY MM DD hh:mm dY el1 gd1 gd2",
				"",
				"where",
				"",
				"\tdY\tDifference in ecliptic latitude in the sense body2-body1 (or declination if -longitude was specified)",
				"\tel1\tEcliptic elongation of body 1 from the Sun",
				"\tgd1\tGeometric distance of body 1",
				"\tgd2\tGeometric distance of body 2"
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
	
	private ApparentPlace getApparentPlace(int kBody) {
		if (apPlanets[kBody] != null)
			return apPlanets[kBody];
		
		MovingPoint planet = null;
		
		switch (kBody) {
		case JPLEphemeris.SUN:
			planet = sun;
			break;
			
		case JPLEphemeris.MOON:
			planet = new MoonCentre(ephemeris);
			break;
			
		default:
			planet = new PlanetCentre(ephemeris, kBody);
			break;
		}
		
		ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);
		
		if (kBody >= 0 && kBody < apPlanets.length)
			apPlanets[kBody] = ap;
		
		return ap;
	}
	
	private void runAllAgainstAll(double jdstart, double jdfinish, double dt, boolean inLongitude,
			PrintStream ps) throws JPLEphemerisException {
		for (int kBody1 = JPLEphemeris.MERCURY; kBody1 < JPLEphemeris.SUN; kBody1++) {
			if (kBody1 == JPLEphemeris.EARTHMOONBARYCENTRE)
				continue;
			
			for (int kBody2 = kBody1 + 1; kBody2 <= JPLEphemeris.SUN; kBody2++) {
				if (kBody2 == JPLEphemeris.EARTHMOONBARYCENTRE)
					continue;
				
				ps.println(bodyNames[kBody1] + " -- " + bodyNames[kBody2]);
				
				run(kBody1, kBody2, jdstart, jdfinish, dt, inLongitude, ps);
				
				ps.println();
			}
		}
	}
	
	private void runOneAgainstAll(int kBody1, boolean wildcardFirst, double jdstart, double jdfinish, double dt, boolean inLongitude,
			PrintStream ps) throws JPLEphemerisException {
		for (int kBody2 = JPLEphemeris.MERCURY; kBody2 < JPLEphemeris.SUN; kBody2++) {
			if (kBody1 == kBody2 || kBody2 == JPLEphemeris.EARTHMOONBARYCENTRE)
				continue;
			
			ps.println(bodyNames[wildcardFirst ? kBody2 : kBody1] + " -- " + bodyNames[wildcardFirst ? kBody1: kBody2]);
			
			if (wildcardFirst)
				run(kBody2, kBody1, jdstart, jdfinish, dt, inLongitude, ps);
			else
				run(kBody1, kBody2, jdstart, jdfinish, dt, inLongitude, ps);
			
			ps.println();
		}
	}

	private void run(int kBody1, int kBody2, double jdstart, double jdfinish, double dt, boolean inLongitude,
			PrintStream ps) throws JPLEphemerisException {
		if (kBody1 == WILDCARD && kBody2 == WILDCARD) {
			runAllAgainstAll(jdstart, jdfinish, dt, inLongitude, ps);
			return;
		}
		
		if (kBody1 == WILDCARD) {
			runOneAgainstAll(kBody2, true, jdstart, jdfinish, dt, inLongitude, ps);
			return;
		}
		
		if (kBody2 == WILDCARD) {
			runOneAgainstAll(kBody1, false, jdstart, jdfinish, dt, inLongitude, ps);
			return;
		}
			
		double lastDX = Double.NaN;
		boolean first = true;
		
		ApparentPlace apTarget1 = getApparentPlace(kBody1);
		
		ApparentPlace apTarget2 = getApparentPlace(kBody2);
		
		for (double t = jdstart; t <= jdfinish; t += dt) {
			double dX = inLongitude ?
					calculateDifferenceInLongitude(apTarget1, apTarget2, t) :
					calculateDifferenceInRightAscension(apTarget1, apTarget2, t);
			
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - dt;
					
					debug("Sign change between " + tLast + "(" + julianDateToCalendarDate(tLast) + ") and " + t + 
							" (" + julianDateToCalendarDate(t) + ") : " + lastDX + " vs " + dX);
					
					double tExact = findExactInstant(apTarget1, apTarget2, tLast, t, inLongitude);
					
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

	private double calculateDifferenceInRightAscension(ApparentPlace apTarget1, ApparentPlace apTarget2, double t) throws JPLEphemerisException {
		apTarget1.calculateApparentPlace(t);
		
		apTarget2.calculateApparentPlace(t);
		
		double ra1 = apTarget1.getRightAscensionOfDate();
		
		double ra2 = apTarget2.getRightAscensionOfDate();
		
		return reduceAngle(ra2 - ra1);	
	}
	
	private double calculateDifferenceInLongitude(ApparentPlace apTarget1, ApparentPlace apTarget2, double t) throws JPLEphemerisException {
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
	
	private double findExactInstant(ApparentPlace apTarget1, ApparentPlace apTarget2, double t1, double t2, boolean inLongitude) throws JPLEphemerisException {
		while (true) {
			double dX1 = inLongitude ? calculateDifferenceInLongitude(apTarget1, apTarget2, t1) : calculateDifferenceInRightAscension(apTarget1, apTarget2, t1);
	
			double dX2 = inLongitude ? calculateDifferenceInLongitude(apTarget1, apTarget2, t2) : calculateDifferenceInRightAscension(apTarget1, apTarget2, t2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(t2 - t1);
		
			double tNew = t1 - dX1/dXrate;
		
			double dX3 = inLongitude ? calculateDifferenceInLongitude(apTarget1, apTarget2, tNew) : calculateDifferenceInRightAscension(apTarget1, apTarget2, tNew);
		
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
