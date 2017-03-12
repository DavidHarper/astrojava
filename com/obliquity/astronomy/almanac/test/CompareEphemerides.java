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

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class CompareEphemerides {
	private static final DecimalFormat dfmta = new DecimalFormat("#0.000");
	private static final DecimalFormat dfmtb = new DecimalFormat("00.00");
	
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final String TAB = "\t";

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename1 = null;
		String filename2 = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris1"))
				filename1 = args[++i];

			if (args[i].equalsIgnoreCase("-ephemeris2"))
				filename2 = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
		}

		if (filename1 == null || filename2 == null || bodyname == null || startdate == null
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

		JPLEphemeris ephemeris1 = null;
		JPLEphemeris ephemeris2 = null;

		try {
			ephemeris1 = new JPLEphemeris(filename1, jdstart - 1.0, jdfinish + 1.0);
			
			ephemeris2 = new JPLEphemeris(filename2, jdstart - 1.0, jdfinish + 1.0);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}
		
		ApparentPlace ap1 = createApparentPlace(ephemeris1, kBody);
		
		ApparentPlace ap2 = createApparentPlace(ephemeris2, kBody);

		long startTime = System.currentTimeMillis();
		int nSteps = 0;

		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ap1.calculateApparentPlace(t);
				ap2.calculateApparentPlace(t);
				if (!timingTest)
					displayApparentPlaceDifference(t, ap1, ap2, System.out);
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

	private static ApparentPlace createApparentPlace(JPLEphemeris ephemeris,
			int kBody) {

		MovingPoint planet = null;

		if (kBody == JPLEphemeris.MOON)
			planet = new MoonCentre(ephemeris);
		else
			planet = new PlanetCentre(ephemeris, kBody);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		if (kBody == JPLEphemeris.SUN)
			sun = planet;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		return new ApparentPlace(earth, planet, sun, erm);
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

	private static void displayApparentPlaceDifference(double t, ApparentPlace ap1,
			ApparentPlace ap2, PrintStream ps) {
		double ra1 = ap1.getRightAscension();
		double dec1 = ap1.getDeclination();
		
		double ra2 = ap2.getRightAscension();
		double dec2 = ap2.getDeclination();

		double diffDec = dec2 - dec1;
		double diffRA = (ra2 - ra1) * Math.cos(dec1);
		
		diffDec *= 3600.0 * 180.0/Math.PI;
		diffRA  *= 3600.0 * 180.0/Math.PI;
		
		System.out.println(t + TAB + dfmta.format(diffRA) + TAB + dfmta.format(diffDec));
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris1\tName of reference ephemeris file");
		System.err.println("\t-ephemeris2\tName of comparison ephemeris file");
		System.err.println("\t-body\t\tName of body");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-step\t\tStep size (days)");
	}
}
