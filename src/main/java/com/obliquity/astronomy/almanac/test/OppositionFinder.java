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

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.obliquity.astronomy.almanac.AlmanacData;
import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class OppositionFinder {
	private class EclipticCoordinates {
		public double longitude, latitude;
		
		public EclipticCoordinates(double longitude, double latitude) {
			this.longitude = longitude;
			this.latitude = latitude;
		}
	};
	

	private static final double TWO_PI = 2.0 * Math.PI;
	
	private EarthCentre earth = null;
	
	private PlanetCentre sun = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private ApparentPlace apSun = null;
	
	private ApparentPlace apTarget = null;
	

	public OppositionFinder(JPLEphemeris ephemeris, int kBody) {
		earth = new EarthCentre(ephemeris);
		
		sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		MovingPoint planet = new PlanetCentre(ephemeris, kBody);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);
		
		apTarget = new ApparentPlace(earth, planet, sun, erm);
	}

	public static void main(String args[]) {
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

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

		}

		if (filename == null || startdate == null
				|| enddate == null || bodyname == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body: \"" + bodyname + "\"");
			System.exit(1);
		}


		double jdstart = parseDate(startdate);

		double jdfinish = parseDate(enddate);

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

		OppositionFinder finder = new OppositionFinder(ephemeris, kBody);
		
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
				"\t-body\t\tName of body",
				"",
				"OPTIONAL PARAMETERS",
				"\t-step\t\tStep size in days [default: 1.0]",
				"",
				"OUTPUT FORMAT",
				"\tEach line gives the date and time at opposition in this format:",
				"",
				"\tYYYY MM DD hh:mm dec beta gd mag",
				"",
				"where",
				"",
				"\tdec\tDeclination at opposition (degrees)",
				"\tbeta\tEcliptic latitude at opposition (degrees)",
				"\tgd\tGeometric distance at opposition (AU)",
				"\tmag\tApparent magnitude at opposition"
		};
		
		for (String line : lines)
			System.err.println(line);
	}

	private static int parseBody(String bodyname) {
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

	private static final Pattern jdPattern = Pattern.compile("(\\d+(\\.)?(\\d+)?)");
	private static final Pattern datePattern = Pattern.compile("(\\d{4})-(\\d{2})\\-(\\d{2})");

	private static double parseDate(String datestr) {
		Matcher matcher = jdPattern.matcher(datestr);

		if (matcher.matches())
			return Double.parseDouble(datestr);

		matcher = datePattern.matcher(datestr);

		if (!matcher.matches())
			throw new IllegalArgumentException("String \"" + datestr + "\" cannot be parsed as a date/time or a Julian Day Number");

		int year = Integer.parseInt(matcher.group(1));
		int month = Integer.parseInt(matcher.group(2));
		int day = Integer.parseInt(matcher.group(3));

		AstronomicalDate ad = new AstronomicalDate(year, month, day);

		return ad.getJulianDate();
	}
	
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWO_PI;
		
		while (x <= -Math.PI)
			x+= TWO_PI;
		
		return x;
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
	
	private double calculateOppositionAngle(double t) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t);
		
		apTarget.calculateApparentPlace(t);
		
		double raSun = apSun.getRightAscensionOfDate();
		
		double decSun = apSun.getDeclinationOfDate();
		
		EclipticCoordinates ecSun = calculateEclipticCoordinates(raSun, decSun, t);
		
		double raTarget = apTarget.getRightAscensionOfDate();
		
		double decTarget = apTarget.getDeclinationOfDate();
		
		EclipticCoordinates ecTarget = calculateEclipticCoordinates(raTarget, decTarget, t);
		
		return reduceAngle(ecTarget.longitude - ecSun.longitude + 3.0 * Math.PI);	
	}

	private void run(double jdstart, double jdfinish, double jdstep,
			PrintStream ps) throws JPLEphemerisException {
		double lastDX = Double.NaN;
		boolean first = true;
		AlmanacData data = new AlmanacData();
		
		for (double t = jdstart; t <= jdfinish; t += jdstep) {
			double dX = calculateOppositionAngle(t);
		
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - jdstep;
				
					double tExact = findExactInstant(tLast, t);
	
					apTarget.calculateApparentPlace(tExact);
				
					double raTarget = apTarget.getRightAscensionOfDate();
				
					double decTarget = apTarget.getDeclinationOfDate();
				
					EclipticCoordinates ecTarget = calculateEclipticCoordinates(raTarget, decTarget, tExact);
				
					AstronomicalDate ad = new AstronomicalDate(tExact);
					
					AlmanacData.calculateAlmanacData(apTarget, apSun, tExact, AlmanacData.TRUE_OF_DATE, data);
				
					ps.printf("%5d %02d %02d %02d:%02d  %6.2f  %6.2f  %7.4f  %6.2f\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(),
						decTarget * 180.0/Math.PI,
						ecTarget.latitude * 180.0/Math.PI,
						apTarget.getGeometricDistance(),
						data.magnitude);
				}
			}
		
			lastDX = dX;
			first = false;
		}
	}
	
	// Limit for convergence
	private final double EPSILON = 0.1 * (Math.PI/180.0)/3600.0;
	
	private double findExactInstant(double t1, double t2) throws JPLEphemerisException {
		while (true) {
			double dX1 = calculateOppositionAngle(t1);
	
			double dX2 = calculateOppositionAngle(t2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(t2 - t1);
		
			double tNew = t1 - dX1/dXrate;
		
			double dX3 = calculateOppositionAngle(tNew);

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
}
