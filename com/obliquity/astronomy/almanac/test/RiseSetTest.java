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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class RiseSetTest {	
	class AltitudeEvent {
		double date;
		double altitude;
		
		public AltitudeEvent(double date, double altitude) {
			this.date = date;
			this.altitude = altitude;
		}
	}
	
	public static final double TWOPI = 2.0 * Math.PI;
	
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");

	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datetimefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd/HH:mm");

	static {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		datetimefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	public static void main(String[] args) {
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String longitude = null;
		String latitude = null;
		String typename = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];
			
			if (args[i].equalsIgnoreCase("-type"))
				typename = args[++i];
		}

		if (filename == null || bodyname == null || startdate == null ) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body name: \"" + bodyname + "\"");
			System.exit(1);
		}

		Date startDate = null;
		
		try {
			startDate = parseDate(startdate);
		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(1);;
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)startDate.getTime())/MILLISECONDS_PER_DAY;
		
		double jdfinish = 0.0;
		
		if (enddate != null) {
			Date endDate = null;
			
			try {
				endDate = parseDate(enddate);
			} catch (ParseException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			jdfinish = UNIX_EPOCH_AS_JD + ((double)endDate.getTime())/MILLISECONDS_PER_DAY + 1.0;
		} else
			jdfinish = jdstart + 1.0;

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

		double lat = Double.parseDouble(latitude) * Math.PI / 180.0;
		double lon = Double.parseDouble(longitude) * Math.PI / 180.0;

		Place place = new Place(lat, lon, 0.0, 0.0);
		
		RiseSetType rsType = parseRiseSetType(typename);
				
		LocalVisibility lv = new LocalVisibility();
		
		try {
			for (double jd = jdstart; jd < jdfinish; jd += 1.0) {
				RiseSetEvent[] events = lv.findRiseSetEvents(ap, place, jd, rsType);
				
				for (int i = 0; i < events.length; i++)
					System.out.println((events[i].type == RiseSetEventType.RISE ? "RISE " : "SET  ") + dateToString(events[i].date));
			}
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	private static RiseSetType parseRiseSetType(String typename) {
		if (typename == null)
			return RiseSetType.UPPER_LIMB;
		
		switch (typename.toLowerCase()) {
			case "upper":
			case "upperlimb":
				return RiseSetType.UPPER_LIMB;
				
			case "lower":
			case "lowerlimb":
				return RiseSetType.LOWER_LIMB;
				
			case "centre":
				return RiseSetType.CENTRE_OF_DISK;
				
			case "civil":
			case "ct":
				return RiseSetType.CIVIL_TWILIGHT;
				
			case "nautical":
			case "nt":
				return RiseSetType.NAUTICAL_TWILIGHT;
				
			case "astronomical":
			case "astro":
			case "at":
				return RiseSetType.ASTRONOMICAL_TWILIGHT;
				
			default:
				return RiseSetType.UPPER_LIMB;
		}
	}
	
	private static Date parseDate(String str) throws ParseException {
		if (str != null) {
			try {
				return datetimefmtIn.parse(str);
			} catch (ParseException e) {
				return datefmtIn.parse(str);
			}
		} else
			return new Date();		
	}
	
		
	private static final DecimalFormat dfmt1 = new DecimalFormat("0000");
	private static final DecimalFormat dfmt2 = new DecimalFormat("00");
	
	private static String dateToString(double t) {
		AstronomicalDate ad = new AstronomicalDate(t);
		ad.roundToNearestSecond();
		return dfmt1.format(ad.getYear()) + "-" + dfmt2.format(ad.getMonth()) + "-" + dfmt2.format(ad.getDay()) 
				+ " " + dfmt2.format(ad.getHour()) + ":" + dfmt2.format(ad.getMinute());
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

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-body\t\tName of body");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-longitude\tLongitude, in degrees");
		System.err.println("\t-latitude\tLatitude, in degrees");
		
		System.err.println();
		
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-enddate\tEnd date [DEFAULT: startdate + 1.0]");
		System.err.println("\t-type\t\tType code (upper|lower|centre|civil|nautical|astronomical) [DEFAULT: upper]");
	}

}
