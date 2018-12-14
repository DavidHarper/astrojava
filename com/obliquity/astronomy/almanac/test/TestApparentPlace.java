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

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class TestApparentPlace {
	private static final DecimalFormat dfmta = new DecimalFormat("00.000");
	private static final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private static final DecimalFormat ifmta = new DecimalFormat("00");
	private static final DecimalFormat ifmtb = new DecimalFormat("000");
	private static final DecimalFormat dfmtc = new DecimalFormat("0.0000000");
	
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static EarthRotationModel erm = new IAUEarthRotationModel();
	private static NutationAngles na = new NutationAngles();
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final int EQUATORIAL = 0;
	private static final int ECLIPTIC = 1;

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		int referenceSystem = EQUATORIAL;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
			
			if (args[i].equalsIgnoreCase("-ecliptic"))
				referenceSystem = ECLIPTIC;
		}

		if (filename == null || bodyname == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body name: \"" + bodyname + "\"");
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
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;

		double jdstep = 0.0;

		jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

		boolean timingTest = Boolean.getBoolean("timingtest");

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

		MovingPoint planet = null;

		if (kBody == JPLEphemeris.MOON)
			planet = new MoonCentre(ephemeris);
		else if (kBody == Nereid.BODY_CODE)
			planet = new Nereid(ephemeris);
		else
			planet = new PlanetCentre(ephemeris, kBody);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		if (kBody == JPLEphemeris.SUN)
			sun = planet;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		long startTime = System.currentTimeMillis();
		int nSteps = 0;

		ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);

		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ap.calculateApparentPlace(t);
				if (!timingTest)
					displayApparentPlace(t, ap, referenceSystem, System.out);
				nSteps++;
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

		long duration = System.currentTimeMillis() - startTime;
		
		double speed = (double)duration/(double)nSteps;
	
		int rate = (int)(1000.0/speed);
		
		speed *= 1000.0;

		System.err.println("Executed " + nSteps + " steps in " + duration
				+ " ms --> " + dfmtb.format(speed) + " \u03bcs/step or " + rate + " steps per second");
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
		
		if (bodyname.equalsIgnoreCase("nereid"))
			return Nereid.BODY_CODE;

		return -1;
	}

	private static void displayApparentPlace(double t, ApparentPlace ap, int referenceSystem,
			PrintStream ps) {
		switch (referenceSystem) {
		case EQUATORIAL:
			displayApparentPlaceEquatorial(t, ap, ps);
			break;
			
		case ECLIPTIC:
			displayApparentPlaceEcliptic(t, ap, ps);
			break;
		}
	}
	
	private static void printAngle(double x, DecimalFormat formatDegrees, DecimalFormat formatMinutes, DecimalFormat formatSeconds,
			PrintStream ps, boolean hasSign) {
		char signum = (x < 0.0) ? '-' : '+';
		
		if (x < 0.0)
			x = -x;
		
		int xd = (int) x;
		
		x -= (double) xd;
		x *= 60.0;
		
		int xm = (int) x;
		
		x -= (double) xm;
		x *= 60.0;
		
		if (hasSign)
			ps.print(signum + " ");
		
		ps.print(formatDegrees.format(xd) + " " + formatMinutes.format(xm) + " " + formatSeconds.format(x));
	}
	
	private static void displayApparentPlaceEquatorial(double t, ApparentPlace ap, PrintStream ps) {
		double ra = ap.getRightAscensionOfDate() * 12.0 / Math.PI;
		double dec = ap.getDeclinationOfDate() * 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;

		ps.println(dfmtb.format(t));
		
		ps.print("   True RA: ");
		
		printAngle(ra, ifmta, ifmta, dfmta, ps, false);
		
		ps.println();
		
		ps.print("   Mean RA: ");

		ra = ap.getMeanRightAscension() * 12.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;
		
		printAngle(ra, ifmta, ifmta, dfmta, ps, false);
		
		ps.println();
		
		ps.print("  True Dec: ");
		
		printAngle(dec, ifmta, ifmta, dfmtb, ps, true);
		
		ps.println();
		
		ps.print("  Mean Dec: ");
		
		dec = ap.getMeanDeclination() * 180.0 / Math.PI;
		
		printAngle(dec, ifmta, ifmta, dfmtb, ps, true);
		
		ps.println();
		
		ps.print("   Geometric distance: ");
		
		ps.println(dfmtc.format(ap.getGeometricDistance()));
		
		ps.print("  Light-path distance: ");
		
		ps.println(dfmtc.format(ap.getLightPathDistance()));
		
		ps.println();
	}
	
	private static void displayApparentPlaceEcliptic(double t, ApparentPlace ap, PrintStream ps) {
		double obliquity = erm.meanObliquity(t);
		
		erm.nutationAngles(t, na);
		
		obliquity += na.getDeps();
		
		double ra = ap.getRightAscensionOfDate();
		double dec = ap.getDeclinationOfDate();
		
		double x = Math.cos(dec) * Math.cos(ra);
		double y = Math.sin(obliquity) * Math.sin(dec) + Math.cos(obliquity) * Math.cos(dec) * Math.sin(ra);
		double z = Math.cos(obliquity) * Math.sin(dec) - Math.sin(obliquity) * Math.cos(dec) * Math.sin(ra);
		
		double lambda = Math.atan2(y,  x) * 180.0 / Math.PI;
		double beta = Math.atan2(z,  Math.sqrt(x * x + y * y)) * 180.0 / Math.PI;
		
		if (lambda < 0.0)
			lambda += 360.0;
		
		ps.print(dfmtb.format(t));
		
		ps.print(" ");
		
		printAngle(lambda, ifmtb, ifmta, dfmta, ps, false);
		
		ps.print("  ");
		
		printAngle(beta, ifmta, ifmta, dfmta, ps, true);
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getGeometricDistance()));
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getLightPathDistance()));
		
		ps.println();
		
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-body\t\tName of body");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-step\t\tStep size (days)");
		System.err.println("\t-ecliptic\tDisplay longitude and latitude on the mean ecliptic of date");
	}
}
