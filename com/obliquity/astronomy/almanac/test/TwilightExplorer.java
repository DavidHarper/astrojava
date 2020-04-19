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
import com.obliquity.astronomy.almanac.RiseSetEventType;
import com.obliquity.astronomy.almanac.RiseSetType;
import com.obliquity.astronomy.almanac.TransitEvent;
import com.obliquity.astronomy.almanac.TransitType;

public class TwilightExplorer {
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
		String startdate = null;
		String enddate = null;
		String latitude = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];
		}

		if (filename == null || startdate == null) {
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

		double lat = Double.parseDouble(latitude) * Math.PI / 180.0;

		Place place = new Place(lat, 0.0, 0.0, 0.0);
		
		TwilightExplorer te = new TwilightExplorer();
		
		try {
			te.run(ephemeris, place, jdstart, jdfinish);
		}
		catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	public void run(JPLEphemeris ephemeris, Place place, double jdstart,
			double jdfinish) throws JPLEphemerisException {
		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace ap = new ApparentPlace(earth, sun, sun, erm);
				
		LocalVisibility lv = new LocalVisibility();
		
		for (double jd = jdstart; jd < jdfinish; jd += 1.0) {
			RiseSetEvent sunset = findSettingEvent(lv, ap, place, jd, RiseSetType.UPPER_LIMB);
			
			if (sunset != null)
				System.out.println("# SUNSET: " + dateToString(sunset.date));
			
			double latest = sunset.date;
			
			RiseSetEvent civilTwilight = findSettingEvent(lv, ap, place, jd, RiseSetType.CIVIL_TWILIGHT);
			
			if (civilTwilight != null) {
				System.out.println("# CIVIL: " + dateToString(civilTwilight.date));
				latest = civilTwilight.date;
			}
			
			RiseSetEvent nauticalTwilight = findSettingEvent(lv, ap, place, jd, RiseSetType.NAUTICAL_TWILIGHT);
			
			if (nauticalTwilight != null) {
				System.out.println("# NAUTICAL: " + dateToString(nauticalTwilight.date));
				latest = nauticalTwilight.date;
			}
			
			RiseSetEvent astronomicalTwilight = findSettingEvent(lv, ap, place, jd, RiseSetType.ASTRONOMICAL_TWILIGHT);
			
			if (astronomicalTwilight != null) {
				System.out.println("# ASTRONOMICAL: " + dateToString(astronomicalTwilight.date));
				latest = astronomicalTwilight.date;
			}
			
			TransitEvent[] transits = lv.findTransitEvents(ap, place, sunset.date);
			
			TransitEvent lowerTransit = null;
			
			for (TransitEvent te : transits) {
				if (te.type == TransitType.LOWER) {
					lowerTransit = te;
					break;
				}
			}
			
			if (lowerTransit != null) {
				System.out.println("# LOWER TRANSIT: " + dateToString(lowerTransit.date));
				latest = lowerTransit.date;
			}
			
			for (double t = sunset.date - 30.0/1440.0; t < latest; t += 10.0/1440.0) {
				HorizontalCoordinates hc = (t <= sunset.date) ? lv.calculateApparentAltitudeAndAzimuth(ap, place, t) :
					lv.calculateGeometricAltitudeAndAzimuth(ap, place, t);
				
				System.out.printf("%s  %7.2f  %7.2f\n", dateToString(t), hc.azimuth * 180.0/Math.PI, hc.altitude * 180.0/Math.PI);
			}
		}		
	}
	
	private RiseSetEvent findSettingEvent(LocalVisibility lv, ApparentPlace ap, Place place,
			double jd, RiseSetType rsType) throws JPLEphemerisException {
		RiseSetEvent[] events = lv.findRiseSetEvents(ap, place, jd, rsType);
		
		for (RiseSetEvent rse : events) {
			if (rse.type == RiseSetEventType.SET)
				return rse;
		}
		
		return null;
	}
	
	private static final DecimalFormat dfmt1 = new DecimalFormat("0000");
	private static final DecimalFormat dfmt2 = new DecimalFormat("00");
	
	private static String dateToString(double t) {
		AstronomicalDate ad = new AstronomicalDate(t);
		ad.roundToNearestSecond();
		return dfmt1.format(ad.getYear()) + "-" + dfmt2.format(ad.getMonth()) + "-" + dfmt2.format(ad.getDay()) 
				+ " " + dfmt2.format(ad.getHour()) + ":" + dfmt2.format(ad.getMinute());
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
		System.err.println("\t-latitude\tLatitude, in degrees");
		
		System.err.println();
		
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-enddate\tEnd date [DEFAULT: startdate + 1.0]");
	}

}
