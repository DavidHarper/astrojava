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

public class TestTopocentricApparentPlace {
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static EarthRotationModel erm = new IAUEarthRotationModel();
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		double latitude = Double.NaN;
		double longitude = Double.NaN;

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

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = Double.parseDouble(args[++i]) * Math.PI/180.0;

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = Double.parseDouble(args[++i]) * Math.PI/180.0;
		}

		if (filename == null || bodyname == null || startdate == null
				|| enddate == null || Double.isNaN(latitude) || Double.isNaN(longitude)) {
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
		else
			planet = new PlanetCentre(ephemeris, kBody);

		EarthCentre geocentre = new EarthCentre(ephemeris);
		
		EarthCentre topo = (EarthCentre)new TerrestrialObserver(ephemeris, erm, latitude, longitude, 0.0);

		MovingPoint sun = null;

		if (kBody == JPLEphemeris.SUN)
			sun = planet;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		long startTime = System.currentTimeMillis();
		int nSteps = 0;

		ApparentPlace apGeocentre = new ApparentPlace(geocentre, planet, sun, erm);
		
		ApparentPlace apTopo = new ApparentPlace(topo, planet, sun, erm);

		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				apGeocentre.calculateApparentPlace(t);
				apTopo.calculateApparentPlace(t);
				displayApparentPlace(t, apGeocentre, apTopo, System.out);
				nSteps++;
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

		long duration = System.currentTimeMillis() - startTime;
		
		double speed = (double)duration/(double)nSteps;
	
		int rate = (int)(1000.0/speed);
		
		speed *= 1000.0;
		
		System.err.printf("Executed %d steps in %d ms --> %.2f \u03bcs/step or %d steps per second\n", nSteps, duration, speed, rate);
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
	
	private static void printAngle(double x, PrintStream ps, boolean hasSign) {
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
		
		ps.printf("%02d %02d %06.3f", xd, xm, x);
	}
	
	private static final SimpleDateFormat datefmtOut = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	
	private static void printDate(double t, PrintStream ps) {
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

		long ticks = (long) dticks;

		Date date = new Date(ticks);

		ps.print(datefmtOut.format(date));
	}
	
	private static void displayApparentPlace(double t, ApparentPlace apGeocentre, ApparentPlace apTopo, PrintStream ps) throws JPLEphemerisException {
		ps.printf("%13.5f", t);
		
		ps.print(" ");
		
		printDate(t, ps);
		
		ps.print(" ");
		
		displayRightAscensionAndDeclination(apGeocentre.getRightAscensionOfDate(), apGeocentre.getDeclinationOfDate(), ps);
		
		ps.print(" ");
		
		displayRightAscensionAndDeclination(apTopo.getRightAscensionOfDate(), apTopo.getDeclinationOfDate(), ps);
		
		ps.print(" ");
		
		calculateAndDisplayApproximateTopocentricPosition(t, apGeocentre, apTopo, ps);
		
		ps.println();
	}
	
	private static void calculateAndDisplayApproximateTopocentricPosition(
			double t, ApparentPlace apGeocentre, ApparentPlace apTopo,
			PrintStream ps) throws JPLEphemerisException {
		Vector dcGeo = apGeocentre.getDirectionCosinesOfDate();
		double rGeo = apGeocentre.getGeometricDistance();
		dcGeo.multiplyBy(rGeo);
		Vector pGeo = apGeocentre.getObserver().getPosition(t);
		
		Vector dcTopo = apTopo.getDirectionCosinesOfDate();
		double rTopo = apTopo.getGeometricDistance();
		dcTopo.multiplyBy(rTopo);
		Vector pTopo = apTopo.getObserver().getPosition(t);
		
		pTopo.subtract(pGeo);
		dcGeo.subtract(pTopo);
		
		double x = dcGeo.getX();
		double y = dcGeo.getY();
		double z = dcGeo.getZ();
		
		double ra = Math.atan2(y, x);
		double w = Math.sqrt(x * x + y * y);
		double dec = Math.atan2(z, w);
		
		displayRightAscensionAndDeclination(ra, dec, ps);
	}

	private static void displayRightAscensionAndDeclination(double ra, double dec, PrintStream ps) {
		ra *= 12.0 / Math.PI;
		dec *= 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;
		printAngle(ra, ps, false);
		
		ps.print(" ");
		
		printAngle(dec,  ps, true);
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-body\t\tName of body");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println("\t-latitude\tLatitude in degrees");
		System.err.println("\t-longitude\tLongitude in degrees");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-step\t\tStep size (days)");

	}
}

