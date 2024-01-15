/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2024 David Harper at obliquity.com
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

import static java.lang.Math.acos;
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
import com.obliquity.astronomy.almanac.HorizontalCoordinates;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.LocalVisibility;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.Place;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.RiseSetEvent;
import com.obliquity.astronomy.almanac.RiseSetEventType;
import com.obliquity.astronomy.almanac.RiseSetType;
import com.obliquity.astronomy.almanac.TerrestrialObserver;

public class MoonVisibility {
	public static final double TWOPI = 2.0 * Math.PI;
	
	private static final double EARTH_RADIUS = 6378.137;
	
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

	private static final String[] yallopCode = {
			"A (Easily visible)",
			"B (Visible under perfect conditions)",
			"C (May need optical aid to find crescent)",
			"D (Will need optical aid to find crescent)",
			"E (Not visible with a telescope)",
			"F (Not visible, below Danjon limit)"
	};

	public static void main(String[] args) {
		String filename = null;
		String startdate = null;
		String enddate = null;
		String longitude = null;
		String latitude = null;
		String timezone = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];

			if (args[i].equalsIgnoreCase("-timezone"))
				timezone = args[++i];
		}

		if (filename == null || startdate == null ) {
			showUsage();
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
					jdfinish + 30.0);
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}

		MovingPoint moon = new MoonCentre(ephemeris);

		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace apMoonGeocentric = new ApparentPlace(earth, moon, sun, erm);
		
		ApparentPlace apSunGeocentric = new ApparentPlace(earth, sun, sun, erm);

		double lat = Double.parseDouble(latitude) * Math.PI / 180.0;
		double lon = Double.parseDouble(longitude) * Math.PI / 180.0;
		
		double tz = (timezone != null) ? Double.parseDouble(timezone)/24.0 : 0.0;

		Place place = new Place(lat, lon, 0.0, tz);
		
		TerrestrialObserver observer = new TerrestrialObserver(ephemeris, erm, place);
		
		ApparentPlace apMoonTopocentric = new ApparentPlace(observer, moon, sun, erm);
		
		MoonVisibility runner = new MoonVisibility();
		
		try {
			runner.run(apMoonGeocentric, apSunGeocentric, apMoonTopocentric, place, jdstart, jdfinish, System.out);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
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

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-longitude\tLongitude, in degrees");
		System.err.println("\t-latitude\tLatitude, in degrees");
		
		System.err.println();
		
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-enddate\tEnd date [DEFAULT: startdate + 1.0]");
		System.err.println("\t-timezone\tTimezone offset from UTC, in hours [DEFAULT: 0]");
	}
	
	private static final String SEPARATOR1 = "================================================================================";
	private static final String SEPARATOR2 = "----------------------------------------";

	private void run(ApparentPlace apMoonGeocentric, ApparentPlace apSunGeocentric, ApparentPlace apMoonTopocentric, Place place,
			double jdstart, double jdfinish, PrintStream ps) throws JPLEphemerisException {
		MoonPhenomena mp = new MoonPhenomena(apMoonGeocentric, apSunGeocentric);
		
		double tNewMoon = mp.getDateOfNextPhase(jdstart, MoonPhenomena.NEW_MOON, true);
				
		while (tNewMoon < jdfinish) {
			ps.println(SEPARATOR1);
			ps.println("NEW MOON: " + dateToString(tNewMoon, place.getTimeZone()));
			
			calculateMoonVisibility(apMoonGeocentric, apSunGeocentric, apMoonTopocentric, place, tNewMoon, ps);
			
			tNewMoon = mp.getDateOfNextPhase(tNewMoon + 1.0, MoonPhenomena.NEW_MOON, true);
		}
	}
	
	private void calculateMoonVisibility(ApparentPlace apMoonGeocentric, ApparentPlace apSunGeocentric, ApparentPlace apMoonTopocentric, Place place,
			double tNewMoon, PrintStream ps) throws JPLEphemerisException {
		boolean moonIsVisible = false;
		
		LocalVisibility lv = new LocalVisibility();
		
		double jd = tNewMoon;
		
		int evening = 1;

		while (! moonIsVisible) {
			RiseSetEvent[] events = lv.findRiseSetEvents(apSunGeocentric, place, jd, RiseSetType.UPPER_LIMB);
			
			double tSunset = findSettingTime(events);
			
			if (Double.isNaN(tSunset)) {
				ps.println("***** UNABLE TO FIND SUNSET TIME");
				return;
			}

			if (evening > 1)
				ps.println(SEPARATOR2);

			ps.println("\nEVENING " + evening + " AFTER NEW MOON");

			ps.println("  SUNSET: " + dateToString(tSunset, place.getTimeZone()));

			HorizontalCoordinates hcMoon = lv.calculateApparentAltitudeAndAzimuth(apMoonTopocentric, place, tSunset);
			
			double elong = getLunarElongation(apMoonGeocentric, apSunGeocentric, tSunset);
			
			ps.printf("    Moon's elongation = %4.1f degrees\n    Moon's altitude = %4.1f degrees\n    Moon's age = %4.1f hours\n", toDegrees(elong),
					toDegrees(hcMoon.altitude), 24.0*(tSunset-tNewMoon));
			
			if (hcMoon.altitude > 0.0) {
				events = lv.findRiseSetEvents(apMoonGeocentric, place, tSunset, RiseSetType.UPPER_LIMB);
				
				double tMoonset = findSettingTime(events);
				
				if (Double.isNaN(tMoonset)) {
					ps.println("***** UNABLE TO FIND MOONSET TIME");
					return;
				}
				
				ps.println("  MOONSET: " + dateToString(tMoonset, place.getTimeZone()));
				
				double tBest = (5.0 * tSunset + 4.0 * tMoonset)/9.0;
				
				hcMoon = lv.calculateGeometricAltitudeAndAzimuth(apMoonGeocentric, place, tBest);
				
				HorizontalCoordinates hcSun = lv.calculateGeometricAltitudeAndAzimuth(apSunGeocentric, place, tBest);

				elong = getLunarElongation(apMoonGeocentric, apSunGeocentric, tBest);
				
				double hpMoon = getLunarHorizontalParallax(apMoonGeocentric, tBest);

				double q = calculateYallopCriterion(elong, hpMoon, hcMoon, hcSun);

				int code = getYallopCode(q);
				
				ps.println("  BEST TIME: " + dateToString(tBest, place.getTimeZone()));
				
				ps.printf("    Yallop's q = %6.3f\n    Visibility code = %s\n", q, yallopCode[code]);
				
				moonIsVisible = code < 2 || elong > 1.0;
			} else {
				ps.println("    MOON IS BELOW THE HORIZON AT SUNSET");
			}
			
			ps.println();
			
			jd = jd + 1.0;

			evening++;
		}
	}

	private double getLunarHorizontalParallax(ApparentPlace apMoon,
			double tBest) throws JPLEphemerisException {	
		apMoon.calculateApparentPlace(tBest);
		
		double gd = apMoon.getGeometricDistance() * apMoon.getTarget().getEphemeris().getAU();
		
		double hpMoon = Math.asin(EARTH_RADIUS/gd);

		return hpMoon;
	}

	private double calculateYallopCriterion(double elong, double hpMoon,
			HorizontalCoordinates hcMoon, HorizontalCoordinates hcSun) {
		// Arc of vision i.e. geocentric difference in altitudes
		double arcv = toDegrees(hcMoon.altitude - hcSun.altitude); 
		
		// Geocentric semi-diameter of Moon
		double sdMoon = 0.27245 * hpMoon;
		
		// Topocentric semi-diameter of Moon
		double w = sdMoon * (1.0 + Math.sin(hcMoon.altitude) * Math.sin(hpMoon));
		
		// Topocentric width of crescent
		w *= (1.0 - Math.cos(elong));
		
		// Convert to arc-minutes for Yallop's Formulae
		w *= 60.0 * 180.0/Math.PI;
		
		// Yallop's q Criterion
		double q = 0.1 * (arcv - (11.8371 - 6.3226 * w + 0.7319 * w * w
		        - 0.1018 * w * w * w));
		
		return q;
	}
	
	private int getYallopCode(double q) {
		  double qlimits[] = {+0.216, -0.014, -0.160, -0.232, -0.293};

		  for (int j = 0; j < 5; j++)
		    if (q > qlimits[j]) return j;

		  return 5;
	}

	private double findSettingTime(RiseSetEvent[] events) {
		if (events == null || events.length == 0)
			return Double.NaN;
		
		for (int i = 0; i < events.length; i++)
			if (events[i].type == RiseSetEventType.SET)
				return events[i].date;
		
		return Double.NaN;
	}
	
	private String dateToString(double t, double tz) {
		AstronomicalDate ad = new AstronomicalDate(t - tz);
		ad.roundToNearestMinute();
		return String.format("%04d-%02d-%02d %02d:%02d", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute());
	}
	
	private double toDegrees(double x) {
		return x * 180.0/Math.PI;
	}
	
	private double getLunarElongation(ApparentPlace apMoon, ApparentPlace apSun, double t) throws JPLEphemerisException {
		apSun.calculateApparentPlace(t);
		
		double raSun = apSun.getRightAscensionOfDate();
		
		double decSun = apSun.getDeclinationOfDate();
		
		apMoon.calculateApparentPlace(t);
		
		double raMoon = apMoon.getRightAscensionOfDate();
		
		double decMoon = apMoon.getDeclinationOfDate();
		
		double x = sin(decMoon) * sin(decSun)
				+ cos(decMoon) * cos(decSun) * cos(raMoon - raSun);

		return acos(x);
	}

}
