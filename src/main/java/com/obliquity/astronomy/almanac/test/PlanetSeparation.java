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

import static java.lang.Math.*;


import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class PlanetSeparation {
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private JPLEphemeris ephemeris = null;
	
	private EarthCentre earth = null;
	
	private PlanetCentre sun = null;
		
	private ApparentPlace[] apPlanets = new ApparentPlace[JPLEphemeris.LAST_COMPONENT + 1];
	
	private EarthRotationModel erm = new IAUEarthRotationModel();

	public PlanetSeparation(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;

		earth = new EarthCentre(ephemeris);
		
		sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String body1name = null;
		String body2name = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

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
		}

		if (filename == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody1 = parseBody(body1name);

		if (kBody1 < 0) {
			System.err.println("Unknown name for -body1: \"" + body1name + "\"");
			System.exit(1);
		}

		int kBody2 = parseBody(body2name);

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

		double jdstep = (stepsize == null) ? 1.0 : parseStepSize(stepsize);

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

		PlanetSeparation runner = new PlanetSeparation(ephemeris);
		
		try {
			runner.run(kBody1, kBody2, jdstart, jdfinish, jdstep, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	public static void showUsage() {
		String[] lines = {
				"MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"\t-body1\t\tName of body 1",
				"\t-body2\t\tName of body 2",
				"",
				"OPTIONAL PAREMETERS",
				"\t-step\t\tStep size",
				"",
				"Valid step size formats are an integer or an integer followed by a single letter (d, h, m, s) to indicate",
				"units.  If no units are specified, days are asssumed.",
				"",
				"OUTPUT FORMAT",
				"",
				"\tYYYY MM DD hh:mm dX dY d",
				"",
				"where",
				"",
				"\tdX\t(RA of body 2 - RA of body 1) * cos(Dec of body 1) in arc-seconds",
				"\tdY\tDec of body 2 - Dec of body 1 in arc-seconds",
				"\td\tAngular separation in arc-seconds"
		};
		
		for (String line : lines)
			System.err.println(line);
	}

	private static int parseBody(String bodyname) {
		if (bodyname == null)
			return -1;
		
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
	
	private static double parseStepSize(String str) {
		Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z]?)");
		
		Matcher matcher = pattern.matcher(str);
		
		if (matcher.matches()) {
			double step = Double.parseDouble(matcher.group(1));
			
			String units = matcher.group(2);
			
			switch (units) {
			case "s":
			case "S":
				step /= 86400.0;
				break;
				
			case "m":
			case "M":
				step /= 1440.0;
				break;
				
			case "h":
			case "H":
				step /= 24.0;
				break;
			}
			
			return step;
		} else
			return Double.NaN;
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

	private void run(int kBody1, int kBody2, double jdstart, double jdfinish, double dt,
			PrintStream ps) throws JPLEphemerisException {		
		ApparentPlace apTarget1 = getApparentPlace(kBody1);
		
		ApparentPlace apTarget2 = getApparentPlace(kBody2);
		
		for (double t = jdstart; t <= jdfinish; t += dt) {
			apTarget1.calculateApparentPlace(t);
					
			apTarget2.calculateApparentPlace(t);
					
			double ra1 = apTarget1.getRightAscensionOfDate();
					
			double dec1 = apTarget1.getDeclinationOfDate();
										
			double ra2 = apTarget2.getRightAscensionOfDate();
					
			double dec2 = apTarget2.getDeclinationOfDate();
										
			double dX = (ra2 - ra1) * Math.acos(dec1) * 3600.0 * 180.0/Math.PI;
			
			double dY = (dec2 - dec1) * 3600.0 * 180.0/Math.PI;
										
			double s = sin(dec1) * sin(dec2)
					+ cos(dec1) * cos(dec2) * cos(ra1 - ra2);
			
			double d = acos(s) * 3600.0 * 180.0/Math.PI;
			
			AstronomicalDate ad = new AstronomicalDate(t);

					
			ps.printf("%5d %02d %02d %02d:%02d  %9.3f  %9.3f  %9.3f\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(),
							dX, dY, d);
		}
	}
}
