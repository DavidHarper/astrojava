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

package com.obliquity.astronomy.almanac.phenomena;

import java.io.IOException;
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

public class PhenomenaFinder {
	private enum Mode {
		CONJUNCTION, OPPOSITION, QUADRATURE
	}

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");
	
	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String body1name = null;
		String body2name = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		boolean inRA = false;
		
		Mode mode = Mode.CONJUNCTION;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-ephemeris":
				filename = args[++i];
				break;
				
			case "-body1":
				body1name = args[++i];
				break;
				
			case "-body2":
				body2name = args[++i];
				break;
				
			case "-startdate":
				startdate = args[++i];
				break;

			case "-enddate":
				enddate = args[++i];
				break;
				
			case "-step":
				stepsize = args[++i];
				break;
				
			case "-ra":
				inRA = true;
				break;
				
			case "-conjunction":
				mode = Mode.CONJUNCTION;
				break;
				
			case "-opposition":
				mode = Mode.OPPOSITION;
				break;
				
			case "-quadrature":
				mode = Mode.QUADRATURE;
				break;
				
			default:
				System.err.println("Unrecognised keyword \"" + args[i] + "\"");
				showUsage();
				System.exit(1);
			}
		}

		if (filename == null || body1name == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody1 = parseBody(body1name);

		if (kBody1 < 0) {
			System.err.println("Unknown name for -body1: \"" + body1name + "\"");
			System.exit(1);
		}

		int kBody2 = body2name != null ? parseBody(body2name) : JPLEphemeris.SUN;

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

		LongitudeDifference ldiff = null;
		
		try {
			ldiff = new LongitudeDifference(apTarget1, apTarget2);
			
			if (inRA)
				ldiff.setMode(LongitudeDifference.IN_RIGHT_ASCENSION);
		} catch (PhenomenaException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		PhenomenaFinder finder = new PhenomenaFinder();
		
		try {
			finder.findPhenomena(ldiff, jdstart, jdfinish, jdstep, mode);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
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

	public void findPhenomena(LongitudeDifference ldiff, double jdstart, double jdfinish,
			double jdstep, Mode mode) throws JPLEphemerisException {
		switch (mode) {
		case CONJUNCTION:
			ldiff.setTargetDifference(0.0);
			findPhenomena(ldiff, jdstart, jdfinish, jdstep);
			break;
			
		case OPPOSITION:
			ldiff.setTargetDifference(Math.PI);
			findPhenomena(ldiff, jdstart, jdfinish, jdstep);
			break;
			
		case QUADRATURE:
			ldiff.setTargetDifference(0.5 * Math.PI);
			findPhenomena(ldiff, jdstart, jdfinish, jdstep);
			ldiff.setTargetDifference(-0.5 * Math.PI);
			findPhenomena(ldiff, jdstart, jdfinish, jdstep);
			break;
		}
	}
	
	public void findPhenomena(LongitudeDifference ldiff, double jdstart, double jdfinish,
			double jdstep) throws JPLEphemerisException {
		double lastDX = Double.NaN;
		boolean first = true;
		
		for (double t = jdstart; t <= jdfinish; t += jdstep) {
			double dX = ldiff.valueAtTime(t);
			
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - jdstep;
					
					double tExact = findZero(ldiff, tLast, t);
					
					AstronomicalDate ad = new AstronomicalDate(tExact);
					
					System.out.printf("%5d %02d %02d %02d:%02d\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
				}
			}
			
			lastDX = dX;
			first = false;
		}		
	}
	
	// Limit of difference in RA for convergence
	private final double EPSILON = 0.1 * (Math.PI/180.0)/3600.0;
	
	private double findZero(LongitudeDifference ldiff, double t1, double t2) throws JPLEphemerisException {
		while (true) {
			double dX1 = ldiff.valueAtTime(t1);
	
			double dX2 = ldiff.valueAtTime(t2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(t2 - t1);
		
			double tNew = t1 - dX1/dXrate;
		
			double dX3 = ldiff.valueAtTime(tNew);

			if (Math.abs(dX3) < EPSILON)
				return tNew;
			
			if (changeOfSign(dX1, dX3))
				t2 = tNew;
			else
				t1 = tNew;
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-body1\t\tName of body 1",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"OPTIONAL PARAMETERS",
				"\t-body2\t\tName of body 2 (Sun will be assumed if absent)",
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

}
