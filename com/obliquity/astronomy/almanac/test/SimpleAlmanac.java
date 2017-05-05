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
import java.text.DecimalFormat;
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
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.Vector;

public class SimpleAlmanac {
	private final DecimalFormat dfmta = new DecimalFormat("00.000");
	private final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private final DecimalFormat ifmta = new DecimalFormat("00");
	private final DecimalFormat dfmtc = new DecimalFormat("0.0000000");
	private final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private ApparentPlace ap;
	private IAUEarthRotationModel erm = new IAUEarthRotationModel();
	private Matrix precessJ2000toB1875; 
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	public SimpleAlmanac(ApparentPlace ap) {
		this.ap = ap;
		
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));	
		
		// Calculate the precession matrix from J2000 to B1875
		double epochJ2000 = erm.JulianEpoch(2000.0);
		double epochB1875 = erm.BesselianEpoch(1875.0);
		precessJ2000toB1875 = new Matrix();
		erm.precessionMatrix(epochJ2000, epochB1875, precessJ2000toB1875);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		
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
			date = datefmtIn.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = datefmtIn.parse(enddate);
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

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		if (kBody == JPLEphemeris.SUN)
			sun = planet;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);
		
		SimpleAlmanac almanac = new SimpleAlmanac(ap);
		
		almanac.run(jdstart, jdfinish, jdstep);
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
	
	public void run(double jdstart, double jdfinish, double jdstep) {
		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				displayApparentPlace(t, System.out);
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

	}
	
	private void printAngle(double x, DecimalFormat formatDegrees, DecimalFormat formatMinutes, DecimalFormat formatSeconds,
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
	
	private void displayApparentPlace(double t, PrintStream ps) throws JPLEphemerisException {
		ap.calculateApparentPlace(t);

		double ra = ap.getRightAscensionOfDate();
		double dec = ap.getDeclinationOfDate();

		ra *= 12.0 / Math.PI;
		dec *= 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;

		ps.print(dfmtb.format(t));
		
		ps.print("  ");
		
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);
		
		long ticks = (long)dticks;
		
		Date date = new Date(ticks);
		
		ps.print("  " + datefmt.format(date));
		
		ps.print("  ");
		
		printAngle(ra, ifmta, ifmta, dfmta, ps, false);
		
		ps.print("  ");
		
		printAngle(dec, ifmta, ifmta, dfmtb, ps, true);
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getGeometricDistance()));
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getLightPathDistance()));
		
		// Calculate the RA and Dec referred to B1875 for constellation identification
		Vector dc = (Vector)ap.getDirectionCosinesJ2000().clone();
		
		dc.multiplyBy(precessJ2000toB1875);	
		
		double ra1875 = Math.atan2(dc.getY(), dc.getX()) * 12.0/Math.PI;
		
		while (ra1875 < 0.0)
			ra1875 += 24.0;
		
		double aux = Math.sqrt(dc.getX() * dc.getX() + dc.getY() * dc.getY());
		
		double dec1875 = Math.atan2(dc.getZ(), aux) * 180.0/Math.PI;
		
		String constellation = ConstellationFinder.getZone(ra1875, dec1875);
		
		ps.print("  " + constellation);
		
		ps.print("  ");
		
		printAngle(ra1875, ifmta, ifmta, dfmta, ps, false);
		
		ps.print("  ");
		
		printAngle(dec1875, ifmta, ifmta, dfmtb, ps, true);
		
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
	}

}
