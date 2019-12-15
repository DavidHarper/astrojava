/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2019 David Harper at obliquity.com
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Place;

public class GreatCircleFlight {
	final double EARTH_RADIUS = 6378.14;
	
	private static final boolean debug = Boolean.getBoolean("debug");
	
	public static void main(String[] args) {
		String startPosition = null, endPosition = null;
		String ephemerisFile = null, startDateTime = null, endDateTime = null;
		String targetBody = null;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-startpos":
				startPosition = args[++i];
				break;
				
			case "-startdate":
				startDateTime = args[++i];
				break;
				
			case "-endpos":
				endPosition = args[++i];
				break;
				
			case "-enddate":
				endDateTime = args[++i];
				break;
				
			case "-ephemeris":
				ephemerisFile = args[++i];
				break;
				
			case "-target":
				targetBody = args[++i];
				break;
				
			default:
				showUsage("Unknown option: " + args[i]);
				System.exit(1);
			}
		}
		
		if (ephemerisFile == null || startPosition == null || endPosition == null
				|| startDateTime == null || endDateTime == null || targetBody == null) {
			showUsage("One or more mandatory options are missing");
			System.exit(1);
		}
		
		try {
			Place startPlace = parsePlace(startPosition);
		
			AstronomicalDate startDate = parseDate(startDateTime);
		
			Place endPlace = parsePlace(endPosition);
		
			AstronomicalDate endDate = parseDate(endDateTime);
		
			JPLEphemeris ephemeris = getEphemeris(ephemerisFile, startDate, endDate);
		
			GreatCircleFlight gcf = new GreatCircleFlight();
		
			int targetID = parseTarget(targetBody);
		
			gcf.run(ephemeris, startPlace, startDate, endPlace, endDate, targetID);
		}
		catch (JPLEphemerisException | IOException e) {
			e.printStackTrace();
		}
	}

	private static int parseTarget(String targetBody) {
		switch (targetBody.toLowerCase()) {
		case "sun":
			return JPLEphemeris.SUN;
			
		case "moon":
			return JPLEphemeris.MOON;
			
		case "mercury":
			return JPLEphemeris.MERCURY;
			
		case "venus":
			return JPLEphemeris.VENUS;
			
		case "mars":
			return JPLEphemeris.MARS;
			
		case "jupiter":
			return JPLEphemeris.JUPITER;
			
		case "saturn":
			return JPLEphemeris.SATURN;
			
		default:
			return -1;
		}
	}
	
	private static Pattern dateTimePattern =
			Pattern.compile("^([\\d]{4})-([\\d]{2})-([\\d]{2})\\s+([\\d]{2}):([\\d]{2})$");

	private static AstronomicalDate parseDate(String strDateTime) {
		Matcher matcher = dateTimePattern.matcher(strDateTime);
		
		if (!matcher.matches())
			return null;
		
		int year = Integer.parseInt(matcher.group(1));
		int month = Integer.parseInt(matcher.group(2));
		int day = Integer.parseInt(matcher.group(3));
		int hour = Integer.parseInt(matcher.group(4));
		int minute = Integer.parseInt(matcher.group(5));
		
		if (debug)
			System.err.println("parseDate(" + strDateTime + ") => [" + year + ", " + month +
					", " + day + ", " + hour + ", " + minute + "]");
		
		return new AstronomicalDate(year, month, day, hour, minute, 0.0);
	}

	private static Place parsePlace(String strPosition) {
		String[] words = strPosition.split(",");
		
		if (words.length != 2)
			return null;
		
		double longitude = -Double.parseDouble(words[0]) * Math.PI/180.0;
		double latitude = Double.parseDouble(words[1]) * Math.PI/180.0;
		
		return new Place(latitude, longitude, 0.0, 0.0);
	}

	private static JPLEphemeris getEphemeris(String filename, AstronomicalDate startDate, AstronomicalDate endDate) throws IOException, JPLEphemerisException {
		return new JPLEphemeris(filename, startDate.getJulianDate() - 1.0, endDate.getJulianDate() + 1.0);
	}
	
	private void run(JPLEphemeris ephemeris, Place startPlace,
			AstronomicalDate startDate, Place endPlace,
			AstronomicalDate endDate, int targetID) {
		double latitude1 = startPlace.getLatitude();
		double longitude1 = startPlace.getLongitude();
		
		double latitude2 = endPlace.getLatitude();
		double longitude2 = endPlace.getLongitude();
	}

	public static void showUsage(String message) {
		if (message != null)
			System.err.println(message);
		
		String[] lines = {
				"MANDATORY PARAMETERS",
				"",
				"\t-ephemeris\tName of the ephemeris file",
				"\t-startdate\tStart date and time in format YYYY-MM-DD hh:mm",
				"\t-startpos\tStart position in format longitude,latitude",
				"\t-enddate\tEnd date and time in format YYYY-MM-DD hh:mm",
				"\t-endpos\t\tEnd position in format longitude,latitude",
				"\t-target\t\tName of the target body",
				"",
				"NOTE",
				"\tAngles should be given in decimal degrees.",
				"\tEast longitudes are positive, west longitudes are negative."
		};
		
		for (String line : lines) {
			System.err.println(line);
		}
	}
}
