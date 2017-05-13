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
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.NutationAngles;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class LongitudeTable {
	private ApparentPlace[] apparentPlaces;
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	private static final double J2000 = 2451545.0;
	
	private static final char TAB = '\t';

	private final DecimalFormat dfmt = new DecimalFormat("0");
	
	private final String[] monthNames = { "",
			"Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	public LongitudeTable(String filename, int year) throws IOException, JPLEphemerisException {
		double jdLow = J2000 + 365.25 * (double)(year - 2000) - 10.0;
		double jdHigh = J2000 + 365.25 * (double)(year + 1 - 2000) + 10.0;
		
		JPLEphemeris ephemeris = new JPLEphemeris(filename, jdLow, jdHigh);
		
		int[] bodies = {
				JPLEphemeris.SUN, JPLEphemeris.MOON, JPLEphemeris.VENUS,
				JPLEphemeris.MARS, JPLEphemeris.JUPITER, JPLEphemeris.SATURN,
				JPLEphemeris.URANUS, JPLEphemeris.NEPTUNE
		};
		
		EarthRotationModel erm = new IAUEarthRotationModel();

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		apparentPlaces = new ApparentPlace[bodies.length];
		
		for (int i = 0; i < bodies.length; i++) {
			int body = bodies[i];
			
			MovingPoint planet;
			
			switch (body) {
			case JPLEphemeris.SUN:
				planet = sun;
				break;
				
			case JPLEphemeris.MOON:
				planet = new MoonCentre(ephemeris);
				break;
				
			default:
				planet = new PlanetCentre(ephemeris, body);
				break;
			}
			
			apparentPlaces[i] = new ApparentPlace(earth, planet, sun, erm);
		}
	}
	
	public static void main(String[] args) {
		String filename = null;
		int year = -1;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-ephemeris":
				filename = args[++i];
				break;
				
			case "-year":
				year = Integer.parseInt(args[++i]);
				break;
				
			default:
				System.err.println("Unknown option: " + args[i]);
				System.exit(1);
			}
		}
		
		try {
			LongitudeTable tabulator = new LongitudeTable(filename, year);
		
			tabulator.run(year, System.out);
		} catch (JPLEphemerisException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run(int year, PrintStream ps) throws JPLEphemerisException {
		String headers[] = {
				"Month", "Day",
				"Sun", "Moon", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
		};
		
		for (int i = 0; i < headers.length; i++) {
			if (i > 0)
				ps.print(TAB);
			
			ps.print(headers[i]);
		}
		
		ps.println();
		
		for (int month = 1; month < 13; month++) {
			for (int day = 6; day < 30; day += 15) {
				ps.print(day < 15 ? monthNames[month] : "");
				ps.print(TAB);
				ps.print(day);
				
				calculateLongitudes(year, month, day, ps);
			}
		}
	}
	
	private void calculateLongitudes(int year, int month, int day, PrintStream ps) throws JPLEphemerisException {
		GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day);
		
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		long ticks = calendar.getTimeInMillis();
		
		double jd = UNIX_EPOCH_AS_JD + ((double)ticks)/MILLISECONDS_PER_DAY;
		
		NutationAngles na = new NutationAngles();
		
		EarthRotationModel erm = apparentPlaces[0].getEarthRotationModel();
		
		double obliquity = erm.meanObliquity(jd);
		
		erm.nutationAngles(jd, na);
		
		obliquity += na.getDeps();
		
		double cosEps = Math.cos(obliquity);
		double sinEps = Math.sin(obliquity);
		
		for (int i = 0; i < apparentPlaces.length; i++) {
			ApparentPlace ap = apparentPlaces[i];
			
			ap.calculateApparentPlace(jd);
			
			double ra = ap.getRightAscensionOfDate();
			double dec = ap.getDeclinationOfDate();
			
			double x = Math.cos(dec) * Math.cos(ra);
			double y = sinEps * Math.sin(dec) + cosEps * Math.cos(dec) * Math.sin(ra);
			
			double lambda = Math.atan2(y,  x) * 180.0 / Math.PI;

			if (lambda < 0.0)
				lambda += 360.0;
			
			ps.print(TAB + dfmt.format(lambda));
		}
		
		ps.println();
	}
}
