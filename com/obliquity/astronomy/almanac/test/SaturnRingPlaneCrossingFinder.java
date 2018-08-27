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

import com.obliquity.astronomy.almanac.AlmanacData;
import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class SaturnRingPlaneCrossingFinder {
	private static final int EARTH = 0, SUN = 1;
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");
	
	private final SimpleDateFormat datefmtOut = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");
	
	private boolean debug = Boolean.getBoolean("debug");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	// Convergence criterion in radians.
	private static final double EPSILON = 1.0e-6;
	
	private ApparentPlace apSaturn = null, apSun = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();

	public SaturnRingPlaneCrossingFinder(JPLEphemeris ephemeris) {
		MovingPoint saturn = new PlanetCentre(ephemeris, JPLEphemeris.SATURN);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		apSaturn = new ApparentPlace(earth, saturn, sun, erm);

		apSun = new ApparentPlace(earth, sun, sun, erm);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];
		}

		if (filename == null  || startdate == null
				|| enddate == null) {
			showUsage();
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

		SaturnRingPlaneCrossingFinder finder = new SaturnRingPlaneCrossingFinder(ephemeris);
		
		try {
			finder.run(jdstart, jdfinish, jdstep, System.out);
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
				"\t-step\t\tStep size in days [default: 1.0]"
		};
		
		for (String line : lines)
			System.err.println(line);
	}

	public void run(double jdstart, double jdfinish, double jdstep, PrintStream ps)
			throws JPLEphemerisException {
		AlmanacData lastData = null;
		double tlast = 0.0;
		
		for (double t = jdstart; t < jdfinish; t += jdstep) {
			AlmanacData data = new AlmanacData();
			
			AlmanacData.calculateAlmanacData(apSaturn, apSun, t, AlmanacData.OF_DATE, data);
			
			if (lastData != null) {
				double earthLatitudeBefore = lastData.saturnRingAnglesForEarth.B;
				double earthLatitudeNow = data.saturnRingAnglesForEarth.B;
				
				if (signHasChanged(earthLatitudeBefore, earthLatitudeNow)) {					
					findRingPlaneCrossing(tlast, earthLatitudeBefore, t, earthLatitudeNow, EARTH, ps);
				}
				
				double sunLatitudeBefore = lastData.saturnRingAnglesForSun.B;
				double sunLatitudeNow = data.saturnRingAnglesForSun.B;
				
				if (signHasChanged(sunLatitudeBefore, sunLatitudeNow)) {					
					findRingPlaneCrossing(tlast, sunLatitudeBefore, t, sunLatitudeNow, SUN, ps);
				}
			}
				
			tlast = t;
			lastData = data;
		}
	}
	
	private boolean signHasChanged(double a, double b) {
		return (a < 0.0 && b > 0.0) || (a > 0.0 && b < 0.0);
	}
	
	private void findRingPlaneCrossing(double tstart, double bstart, double tend, double bend, int target, PrintStream ps)
		throws JPLEphemerisException {
		if (debug)
			ps.println("# Starting search for " + (target == EARTH ? "Earth" : "Sun") + " crossing between " +
				tstart + " (B = " + bstart + ") and " + tend + " (B = " + bend +")");
		
		double dbdt = (bend - bstart)/(tend - tstart);
		
		double dt = -bstart/dbdt;
		
		double t = tstart + dt;
		
		AlmanacData data = new AlmanacData();
		
		double B = 0.0;
		
		int i = 0;
		
		do {
			i++;
			
			AlmanacData.calculateAlmanacData(apSaturn, apSun, t, AlmanacData.OF_DATE, data);
		
			B = (target == EARTH) ? data.saturnRingAnglesForEarth.B : data.saturnRingAnglesForSun.B;
		
			if (debug)
				ps.println("# Refinement " + i + " : t = " + t + " ==> B = " + B);
		
			dt = -B/dbdt;
		
			t += dt;
		} while (Math.abs(B) > EPSILON);
		
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

		long ticks = (long) dticks;

		Date date = new Date(ticks);
		
		ps.println(datefmtOut.format(date) + " " + ((dbdt > 0.0) ? '+' : '-') + " " +
				(target == EARTH ? "Earth" : "Sun"));
	}
}
