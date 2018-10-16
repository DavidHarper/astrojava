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

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.HorizontalCoordinates;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.LocalVisibility;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.Place;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.RiseSetEvent;
import com.obliquity.astronomy.almanac.RiseSetType;
import com.obliquity.astronomy.almanac.TransitEvent;
import com.obliquity.astronomy.almanac.TransitType;

public class NightlyPlanetNotes {
	public static final double TWOPI = 2.0 * Math.PI;

	private static final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");


	static {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	public static void main(String[] args) {
		String filename = null;
		String datestr = null;
		String latitude = null;
		boolean civil = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-civil"))
				civil = true;

			if (args[i].equalsIgnoreCase("-date"))
				datestr = args[++i];


			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];
		}

		if (filename == null ||  datestr == null || latitude == null) {
			showUsage();
			System.exit(1);
		}

		Date date = null;

		try {
			date = parseDate(datestr);
		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY + 0.5;


		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(filename, jdstart - 1.0,
					jdstart + 2.0);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}
		
		int[] targetCodes = {
				JPLEphemeris.SUN, JPLEphemeris.MERCURY, JPLEphemeris.VENUS,
				JPLEphemeris.EMB, JPLEphemeris.MARS, JPLEphemeris.JUPITER,
				JPLEphemeris.SATURN, JPLEphemeris.URANUS, JPLEphemeris.NEPTUNE
		};
		
		String[] targetNames = {
				"Sun", "Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"
		};
		
		ApparentPlace[] apTargets = new ApparentPlace[targetCodes.length];

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun =  new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();
	
		for (int i = 0; i < targetCodes.length; i++) {
			switch (targetCodes[i]) {
			case JPLEphemeris.SUN:
				apTargets[i] = new ApparentPlace(earth, sun, sun, erm);
				break;
				
			case JPLEphemeris.EMB:
				apTargets[i] = null;
				break;
				
			default:
				MovingPoint planet = new PlanetCentre(ephemeris, targetCodes[i]);
				apTargets[i] = new ApparentPlace(earth, planet, sun, erm);
				break;
			}
		}

		double lat = Double.parseDouble(latitude) * Math.PI / 180.0;

		Place place = new Place(lat, 0.0, 0.0, 0.0);
		
		NightlyPlanetNotes npn = new NightlyPlanetNotes();
		
		try {
			npn.run(jdstart, place, apTargets, targetNames, civil, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	private static Date parseDate(String str) throws ParseException {
		return datefmtIn.parse(str);
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-date\t\tDate for which notes are required");
		System.err.println("\t-latitude\tLatitude, in degrees");

		System.err.println();

		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-civil\t\tDefine night by start/end of civil twilight instead of sunset/sunrise");
	}

	private final DecimalFormat dfmt1 = new DecimalFormat("0000");
	private final DecimalFormat dfmt2 = new DecimalFormat("00");

	private String dateToString(double t) {
		AstronomicalDate ad = new AstronomicalDate(t);
		ad.roundToNearestSecond();
		return dfmt1.format(ad.getYear()) + "-" + dfmt2.format(ad.getMonth())
				+ "-" + dfmt2.format(ad.getDay()) + " "
				+ dfmt2.format(ad.getHour()) + ":"
				+ dfmt2.format(ad.getMinute());
	}
	
	private double normaliseAzimuth(double az) {
		return az < 0.0 ? 360.0 + az : az;
	}
	
	public void run(double jd, Place place, ApparentPlace[] apTargets, String[] targetNames, boolean civil, PrintStream ps) throws JPLEphemerisException {
		LocalVisibility lv = new LocalVisibility();
		
		RiseSetType riseSetType = civil ? RiseSetType.CIVIL_TWILIGHT : RiseSetType.UPPER_LIMB;
		
		RiseSetEvent[] rsEvents = lv.findRiseSetEvents(apTargets[0], place, jd, riseSetType);
		
		TransitEvent[] trEvents = lv.findTransitEvents(apTargets[0], place, jd);
		
		RiseSetEvent sunset = null, sunrise = null;
		TransitEvent midnight = null;
		
		for (RiseSetEvent rse : rsEvents) {
			switch (rse.type) {
			case SET:
				sunset = rse;
				break;
				
			case RISE:
				sunrise = rse;
				break;
			}
		}
		
		for (TransitEvent te : trEvents) {
			if (te.type == TransitType.LOWER)
				midnight = te;
		}
		
		if (sunset == null) {
			ps.println("ERROR: failed to find sunset");
			return;
		}
		
		if (sunrise == null) {
			ps.println("ERROR: failed to find sunrise");
			return;
		}
		
		if (midnight == null) {
			ps.println("ERROR: failed to find midnight");
		}
		
		ps.println("Sunset is at " + dateToString(sunset.date));
		
		String format = "\t%-8s is at altitude %4.1f and azimuth %5.1f (%s)\n";
		
		for (int i = 1; i < apTargets.length; i++) {
			if (apTargets[i] == null)
				continue;
			
			HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
					place, sunset.date);
			
			if (hc.altitude > 0.0) {
				ps.printf(format, targetNames[i], 180.0 * hc.altitude/Math.PI,
						normaliseAzimuth(180.0 * hc.azimuth/Math.PI),
						hc.azimuth < 0.0 ? "setting" : "rising");
			}
		}
		
		ps.println("Midnight is at " + dateToString(midnight.date));
		
		for (int i = 1; i < apTargets.length; i++) {
			if (apTargets[i] == null)
				continue;
			
			HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
					place, midnight.date);
			
			if (hc.altitude > 0.0) {
				ps.printf(format, targetNames[i], 180.0 * hc.altitude/Math.PI,
						normaliseAzimuth(180.0 * hc.azimuth/Math.PI),
						hc.azimuth < 0.0 ? "setting" : "rising");
			}
		}
		
		ps.println("Sunrise is at " + dateToString(sunrise.date));
		
		for (int i = 1; i < apTargets.length; i++) {
			if (apTargets[i] == null)
				continue;
			
			HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
					place, sunrise.date);
			
			if (hc.altitude > 0.0) {
				ps.printf(format, targetNames[i], 180.0 * hc.altitude/Math.PI,
						normaliseAzimuth(180.0 * hc.azimuth/Math.PI),
						hc.azimuth < 0.0 ? "setting" : "rising");
			}
		}
	}
}
