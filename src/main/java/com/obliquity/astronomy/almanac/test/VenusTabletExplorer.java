/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2021 David Harper at obliquity.com
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

import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

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

public class VenusTabletExplorer {
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
	
	private static final double LUNAR_MONTH = 29.53059;
	
	private static final double EPSILON_MOON = 0.5/86400.0;

	private static final double TWO_PI = 2.0 * Math.PI;
	
	private EarthCentre earth = null;
	
	private ApparentPlace apSun = null;
	
	private ApparentPlace apVenus = null;
	
	private ApparentPlace apMoon = null;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	public VenusTabletExplorer(JPLEphemeris ephemeris) {
		earth = new EarthCentre(ephemeris);
		
		PlanetCentre sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);
		
		PlanetCentre venus = new PlanetCentre(ephemeris, JPLEphemeris.VENUS);
				
		apVenus = new ApparentPlace(earth, venus, sun, erm);
		
		MovingPoint moon = new MoonCentre(ephemeris);
		
		apMoon = new ApparentPlace(earth, moon, sun, erm);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		boolean useGregorianCalendar = true;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];

			if (args[i].equalsIgnoreCase("-julian"))
				useGregorianCalendar = false;
		}

		if (filename == null || startdate == null
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

		VenusTabletExplorer explorer = new VenusTabletExplorer(ephemeris);
		
		try {
			explorer.run(jdstart, jdfinish, jdstep, useGregorianCalendar, System.out);
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
				"\t-julian\t\tConvert dates to proleptic Julian calendar [default is proleptic Gregorian calendar]"
		};
		
		for (String line : lines)
			System.err.println(line);
	}
	private void run(double jdstart, double jdfinish, double dt, boolean useGregorianCalendar, PrintStream ps) throws JPLEphemerisException {			
		double lastDX = Double.NaN;
		boolean first = true;
		char calType = useGregorianCalendar ? 'G' : 'J';
				
		for (double t = jdstart; t <= jdfinish; t += dt) {
			double dX = calculateDifferenceInLongitude(apSun, apVenus, t);
			
			if (!first) {
				if (changeOfSign(lastDX, dX)) {
					double tLast = t - dt;
					
					double tExact = findExactInstant(apSun, apVenus, tLast, t);
					
					apVenus.calculateApparentPlace(tExact);
					
					double raVenus = apVenus.getRightAscensionOfDate();
					
					double decVenus = apVenus.getDeclinationOfDate();
					
					EclipticCoordinates ecVenus = calculateEclipticCoordinates(raVenus, decVenus, tExact);
					
					double dY =  ecVenus.latitude * 180.0/Math.PI;
										
					double venusDistance = apVenus.getGeometricDistance();
					
					if (venusDistance < 1.0) {
						double tNewMoon = findPreviousNewMoon(tExact);
						
						AstronomicalDate ad = new AstronomicalDate(tExact, useGregorianCalendar);
						
						AstronomicalDate adNewMoon = new AstronomicalDate(tNewMoon, useGregorianCalendar);
						
						ps.printf("%5d %02d %02d %02d:%02d  %6.3f  %5d %02d %02d %02d:%02d %c %10.2f  %10.2f  %5.2f\n",
								ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(),
								dY,
								adNewMoon.getYear(), adNewMoon.getMonth(), adNewMoon.getDay(), adNewMoon.getHour(), adNewMoon.getMinute(),
								calType, tExact, tNewMoon, tExact-tNewMoon);
					}
				}
			}
			
			lastDX = dX;
			first = false;
		}
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
	
	private double calculateDifferenceInLongitude(ApparentPlace apSun, ApparentPlace apTarget, double t) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t);
		
		apTarget.calculateApparentPlace(t);
		
		double ra1 = apSun.getRightAscensionOfDate();
		
		double dec1 = apSun.getDeclinationOfDate();
		
		EclipticCoordinates ec1 = calculateEclipticCoordinates(ra1, dec1, t);
		
		double ra2 = apTarget.getRightAscensionOfDate();
		
		double dec2 = apTarget.getDeclinationOfDate();
		
		EclipticCoordinates ec2 = calculateEclipticCoordinates(ra2, dec2, t);
		
		return reduceAngle(ec2.longitude - ec1.longitude);	
	}
	
	// Limit of difference in RA for convergence
	private final double EPSILON = 0.1 * (Math.PI/180.0)/3600.0;
	
	private double findExactInstant(ApparentPlace apSun, ApparentPlace apTarget, double t1, double t2) throws JPLEphemerisException {
		while (true) {
			double dX1 = calculateDifferenceInLongitude(apSun, apTarget, t1);
	
			double dX2 = calculateDifferenceInLongitude(apSun, apTarget, t2);
		
			double dXchange = dX2 - dX1;
		
			double dXrate = dXchange/(t2 - t1);
		
			double tNew = t1 - dX1/dXrate;
		
			double dX3 = calculateDifferenceInLongitude(apSun, apTarget, tNew);

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
	
	private double getLunarElongation(double t) throws JPLEphemerisException {
		return calculateDifferenceInLongitude(apSun, apMoon, t);
	}
	
	public double findPreviousNewMoon(double t0) throws JPLEphemerisException {
		double theta = getLunarElongation(t0);
		
		if (theta < 0.0)
			theta += TWO_PI;
		
		double dt = theta * LUNAR_MONTH/TWO_PI;
				
		double t = t0 - dt;
		
		while (abs(dt) > EPSILON_MOON) {
			double d = getLunarElongation(t);
			
			dt =  d * LUNAR_MONTH/TWO_PI;
			
			t -= dt;
		}
		
		return t;
	}
}
