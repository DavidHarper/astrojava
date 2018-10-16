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

	private final boolean debug = Boolean.getBoolean("nightlyplanetnotes.debug");
	
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
	
	private static final double
			CP_NNE = 22.5, CP_ENE=67.5,
			CP_ESE = 112.5, CP_SSE = 157.5,
			CP_SSW = 202.5, CP_WSW = 247.5,
			CP_WNW = 292.5, CP_NNW = 337.5;
	
	private String compassPoint(double az) {
		az = normaliseAzimuth(180.0 * az/Math.PI);
		
		if (az > CP_NNW || az < CP_NNE)
			return "north";
		else if (az < CP_ENE)
			return "north-east";
		else if (az < CP_ESE)
			return "east";
		else if (az < CP_SSE)
			return "south-east";
		else if (az < CP_SSW)
			return "south";
		else if (az < CP_WSW)
			return "south-west";
		else if (az < CP_WNW)
			return "west";
		else
			return "north-west";
	}
	
	private String altitudeDescription(double alt) {
		alt *= 180.0/Math.PI;
		
		if (alt < 0.0)
			return "below the horizon";
		else if (alt < 20.0)
			return "low";
		else if (alt < 45.0)
			return "at moderate altitude";
		else
			return "high";
	}
	
	private final DecimalFormat dfmt = new DecimalFormat("0.0");
	
	public void run(double jd, Place place, ApparentPlace[] apTargets, String[] targetNames, boolean civil, PrintStream ps) throws JPLEphemerisException {
		LocalVisibility lv = new LocalVisibility();
		
		String sunsetName = civil ? "Evening Civil Twilight" : "Sunset";
		String sunriseName = civil ? "Morning Civil Twilight" : "Sunrise";
		
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
			ps.println("ERROR: failed to find " + sunsetName);
			return;
		}
		
		if (sunrise == null) {
			ps.println("ERROR: failed to find " + sunriseName);
			return;
		}
		
		if (midnight == null) {
			ps.println("ERROR: failed to find midnight");
		}
		
		ps.println(sunsetName + " is at " + dateToString(sunset.date));
		
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
		
		ps.println("\nMidnight is at " + dateToString(midnight.date));
		
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
		
		ps.println("\n" + sunriseName + " is at " + dateToString(sunrise.date));
		
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
		
		ps.println();
		
		for (int i = 1; i < apTargets.length; i++) {
			if (apTargets[i] == null)
				continue;
			
			if (debug)
				ps.println("SUMMARY FOR " + targetNames[i]);
			
			rsEvents = lv.findRiseSetEvents(apTargets[i], place, sunset.date, RiseSetType.UPPER_LIMB);
			
			RiseSetEvent rising = null, setting = null;
			
			for (RiseSetEvent rse : rsEvents) {
				switch (rse.type) {
				case RISE:
					rising = rse;
					
					if (debug)
						ps.println("RISES:   " + dateToString(rse.date));
					break;
					
				case SET:
					setting = rse;
					
					if (debug)
						ps.println("SETS:    " + dateToString(rse.date));
					break;
				}
			}	
			
			trEvents = lv.findTransitEvents(apTargets[i], place, sunset.date);
			
			TransitEvent transit = null;
			
			for (TransitEvent te : trEvents) {
				if (te.type == TransitType.UPPER) {
					transit = te;
					
					if (debug)
						ps.println("TRANSIT: " + dateToString(te.date));
				}
			}
			
			HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
					place, sunset.date);
			
			if (hc.altitude > 0.0) {
				// Target is already visible at sunset.
				ps.println(targetNames[i] + " is " + altitudeDescription(hc.altitude) + " (" +
				dfmt.format(180.0*hc.altitude/Math.PI) +" degrees) in the " + compassPoint(hc.azimuth) + " at the start of the night.");
			} else {
				// Target is not yet visible at sunset.
				reportEvent(ps, targetNames[i], "rises", rising.date, sunset.date, midnight.date, sunrise.date, true);
			}
			
			if (transit.date > sunset.date && transit.date < sunrise.date) {
				reportEvent(ps, targetNames[i], "transits", transit.date, sunset.date, midnight.date, sunrise.date, false);
				
				hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
						place, transit.date);
				
				ps.println(", when it is " + altitudeDescription(hc.altitude) +
						" (" + dfmt.format(180.0*hc.altitude/Math.PI) +
						" degrees) in the " + compassPoint(hc.azimuth) + ".");
			}
			
			hc = lv.calculateApparentAltitudeAndAzimuth(apTargets[i],
					place, sunrise.date);
			
			if (hc.altitude > 0.0) {
				// Target is already visible at sunset.
				ps.println(targetNames[i] + " is " + altitudeDescription(hc.altitude) + " (" +
				dfmt.format(180.0*hc.altitude/Math.PI) + " degrees) in the " + compassPoint(hc.azimuth) + " at the end of the night.");
			} else {
				// Target is not yet visible at sunset.
				reportEvent(ps, targetNames[i], "sets", setting.date, sunset.date, midnight.date, sunrise.date, true);
			}
		}		
	}
	
	private void reportEvent(PrintStream ps, String targetName, String eventVerb, double date, double sunset, double midnight, double sunrise, boolean newline) {
		if (date < sunset || date > sunrise)
			return;
		
		double t1 = 24.0 * (date - sunset);
		double t2 = 24.0 * (date - midnight);
		double t3 = 24.0 * (date - sunrise);
		
		ps.print(targetName + " " + eventVerb);
		
		if (Math.abs(t2) < Math.abs(t1) && Math.abs(t2) < Math.abs(t3))
			ps.printf(" %.1f hours %s midnight", Math.abs(t2), (t2 < 0.0 ? "before" : "after"));
		else if (Math.abs(t1) < Math.abs(t3))
			ps.printf(" %.1f hours after the start of the night", t1);
		else
			ps.printf(" %.1f hours before the end of the night", Math.abs(t3));
		
		if (newline)
			ps.println(".");
	}
}
