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

import static java.lang.Math.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class LunarEclipses {
	private static final double LUNAR_MONTH = 29.53059;
	
	private static final double EARTH_RADIUS = 6378.0, MOON_RADIUS = 1738.0, SUN_RADIUS = 696000.0;
	
	private static final double ADJUSTED_EARTH_RADIUS = EARTH_RADIUS * 1.02;
	
	private static final double SEMI_INTERVAL = 90.0;

	private final DecimalFormat dfmta = new DecimalFormat("#0.000");
	private final DecimalFormat dfmtb = new DecimalFormat("0.0");
	
	private final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private final SimpleDateFormat prefixfmt = new SimpleDateFormat("yyyyMMdd: ");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private final double AU;
	
	private EarthRotationModel erm = new IAUEarthRotationModel();
	
	private ApparentPlace apSun, apMoon;
	
	public static void main(String args[]) {
		SimpleDateFormat parsefmt = new SimpleDateFormat("yyyy-MM-dd");
		parsefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	
		String filename = null;
		String startdate = null;
		String enddate = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];
		}

		if (filename == null || startdate == null || enddate == null) {
			showUsage();
			System.exit(1);
		}
		
		Date date = null;
		
		try {
			date = parsefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = parsefmt.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY + LUNAR_MONTH;
		
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

		MoonPhases mp = new MoonPhases(ephemeris);
		
		LunarEclipses le = new LunarEclipses(ephemeris);
		
		double t = jdstart;
		
		while (t < jdfinish) {
			try {
				t = mp.getDateOfNextPhase(t, MoonPhases.FULL_MOON);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			try {
				le.testForLunarEclipse(t);
			} catch (JPLEphemerisException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			t += 29.0;
		}
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
	}

	public LunarEclipses(JPLEphemeris ephemeris) {
		MovingPoint moon = new MoonCentre(ephemeris);
		
		MovingPoint earth = new EarthCentre(ephemeris);
		
		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apSun = new ApparentPlace(earth, sun, sun, erm);

		apMoon = new ApparentPlace(earth, moon, sun, erm);
		
		AU = ephemeris.getAU();
		
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		prefixfmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public void testForLunarEclipse(double t0) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t0);
		apMoon.calculateApparentPlace(t0);
		
		double rSun = AU * apSun.getGeometricDistance();
		double rMoon = AU * apMoon.getGeometricDistance();
		
		// Half-angle at vertex of umbral cone
		double theta = asin((SUN_RADIUS - ADJUSTED_EARTH_RADIUS)/rSun);
		
		// Complement of theta
		double beta = 0.5 * PI - theta;
		
		double alpha1 = acos((ADJUSTED_EARTH_RADIUS - MOON_RADIUS)/rMoon);
		
		double gamma1 = beta - alpha1;
		gamma1 *= 180.0/PI;
		
		double alpha2 = acos((ADJUSTED_EARTH_RADIUS + MOON_RADIUS)/rMoon);
		
		double gamma2 = beta - alpha2;
		gamma2 *= 180.0/PI;
		
		double raVertex = apSun.getRightAscensionOfDate();
		
		double decVertex = apSun.getDeclinationOfDate();
		
		double raMoon = apMoon.getRightAscensionOfDate();
		
		double decMoon = apMoon.getDeclinationOfDate();

		double q = sin(decMoon) * sin(decVertex) + cos(decMoon) * cos(decVertex) * cos(raMoon-raVertex);
		
		q = 180.0 - acos(q) * 180.0/PI;
		
		double xx[] = new double[3], yy[] = new double[3], tt[] = { -SEMI_INTERVAL, 0.0, SEMI_INTERVAL };
		
		for (int j = 0; j < tt.length; j++) {
			double dm = tt[j];
			
			double t = t0 + dm/1440.0;
			
			apSun.calculateApparentPlace(t);
			apMoon.calculateApparentPlace(t);

			raVertex = apSun.getRightAscensionOfDate() + PI;
			
			decVertex = -apSun.getDeclinationOfDate();
			
			raMoon = apMoon.getRightAscensionOfDate();
			
			decMoon = apMoon.getDeclinationOfDate();
			
			double x = ((raMoon - raVertex) * 180.0/PI) % 360.0;
			
			while (x < -180.0)
				x += 360.0;
			
			x *= cos(decVertex);

			double y = (decMoon - decVertex) * 180.0/PI;
			
			xx[j] = x;
			yy[j] = y;
			
			q = sin(decMoon) * sin(decVertex) + cos(decMoon) * cos(decVertex) * cos(raMoon-raVertex);
			
			q = acos(q) * 180.0/PI;
		}
		
		double xDot = (xx[2] - xx[0])/(2.0 * SEMI_INTERVAL);
		double yDot = (yy[2] - yy[0])/SEMI_INTERVAL;
		
		double x0 = xx[1];
		double y0 = yy[1];
		
		double tMin = - (x0 * xDot + y0 * yDot)/(xDot * xDot + yDot * yDot);
		
		double x = x0 + xDot * tMin;
		double y = y0 + yDot * tMin;
		
		double qMin = sqrt(x * x + y * y);
		
		if (qMin > gamma2)
			return;
				
		String PREFIX = timeToDateString(t0, prefixfmt);

		System.out.println();
		System.out.println(PREFIX + "FULL MOON: " + timeToDateString(t0, datefmt));
		
		System.out.println(PREFIX + "gamma1 = " + dfmta.format(gamma1));
		System.out.println(PREFIX + "gamma2 = " + dfmta.format(gamma2));
		
		double tMinimumGamma = t0 + tMin/1440.0;
		
		System.out.println(PREFIX + "Minimum gamma: " + timeToDateString(tMinimumGamma, datefmt) + " : gamma = " + dfmta.format(qMin));
		
		System.out.println(PREFIX + (qMin < gamma1 ? "TOTAL" : "PARTIAL") + " ECLIPSE");
		
		double a = xDot * xDot + yDot * yDot;
		double b = 2.0 * (x0 * xDot + y0 * yDot);
		double q0squared = x0 * x0 + y0 * y0;
		
		// Exterior contacts with umbra
		double c = q0squared - gamma2 * gamma2;
		
		//double f1 =  -b/(2.0 * a);
		double f2 = sqrt(b * b - 4.0 * a * c)/(2.0 * a);
		
		System.out.println(PREFIX + "Partial duration: " + dfmtb.format(2.0 * f2) + " minutes");
		
		if (qMin < gamma1) {
			c = q0squared - gamma1 * gamma1;
			
			f2 = sqrt(b * b - 4.0 * a * c)/(2.0 * a);
			
			System.out.println(PREFIX + "Total duration: " + dfmtb.format(2.0 * f2) + " minutes");
		}
	}
	
	private String timeToDateString(double t, SimpleDateFormat fmt) {
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);
		
		long ticks = (long)dticks;
		
		Date date = new Date(ticks);

		return fmt.format(date);
	}
}
