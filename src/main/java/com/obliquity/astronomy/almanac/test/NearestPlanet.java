/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2020 David Harper at obliquity.com
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
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Vector;

class InterPlanetDistance implements Comparable<InterPlanetDistance> {
	public int body;
	public double distance;

	public int compareTo(InterPlanetDistance that) {
		return (this.distance < that.distance) ? -1 : 1;
	}
}

public class NearestPlanet {
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private JPLEphemeris ephemeris = null;
	
	private String[] planetNames = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto" };

	public NearestPlanet(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;
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

		if (filename == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown name for -body: \"" + bodyname + "\"");
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

		NearestPlanet finder = new NearestPlanet(ephemeris);
		
		try {
			finder.run(kBody, jdstart, jdfinish, jdstep, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	private void run(int kBody, double jdstart, double jdfinish, double jdstep,
			PrintStream out) throws JPLEphemerisException {
		InterPlanetDistance[] distances = new InterPlanetDistance[8];
		
		for (int i = 0; i < distances.length; i++)
			distances[i] = new InterPlanetDistance();

		for (int i = 0, j=0; i <= JPLEphemeris.PLUTO; i++) {
			if (i != kBody)
				distances[j++].body = i;
		}
		
		for (double jd = jdstart; jd <= jdfinish; jd += jdstep) {
			calculateDistances(jd, kBody, distances);
		
			Arrays.sort(distances);
			
			AstronomicalDate ad = new AstronomicalDate(jd);
			
			out.printf("%5d %02d %02d %02d:%02d",
					ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
		
			out.printf("  %7s  %9.2f\n", planetNames[distances[0].body], distances[0].distance/1000000.0);
		}
	}
	
	Vector position1 = new Vector(), position2 = new Vector();
	
	private void calculateDistances(double jd, int kBody, InterPlanetDistance[] distances) throws JPLEphemerisException {
		for (int i = 0; i < distances.length; i++) {
			ephemeris.calculatePositionAndVelocity(jd, kBody, position1, null);
			
			ephemeris.calculatePositionAndVelocity(jd, distances[i].body, position2, null);
			
			position1.subtract(position2);
			
			distances[i].distance = position1.magnitude();
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"\t-body\t\tName of body",
		};
		
		for (String line : lines)
			System.err.println(line);
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

	private static int parseBody(String bodyname) {
		if (bodyname.equalsIgnoreCase("mercury"))
			return JPLEphemeris.MERCURY;

		if (bodyname.equalsIgnoreCase("venus"))
			return JPLEphemeris.VENUS;

		if (bodyname.equalsIgnoreCase("earth"))
			return JPLEphemeris.EMB;

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
