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

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class RiseSetTest {
	enum TransitType { UPPER, LOWER }
	
	enum RiseSetType {
		UPPER_LIMB,
		LOWER_LIMB,
		CENTRE_OF_DISK,
		CIVIL_TWILIGHT,
		NAUTICAL_TWILIGHT,
		ASTRONOMICAL_TWILIGHT
	}
	
	class TransitEvent {
		TransitType type;
		double date;
		
		public TransitEvent(TransitType type, double date) {
			this.type = type;
			this.date = date;
		}
	}
	
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
	
	private static final double EPSILON = 0.1/86400.0;
	
	private static final double HORIZONTAL_REFRACTION = (-34.0 / 60.0) * Math.PI/180.0;
	
	private static final double SOLAR_SEMIDIAMETER = (16.0 / 60.0) * Math.PI/180.0;
	
	public static void main(String[] args) {
		String filename = null;
		String bodyname = null;
		String date = null;
		String longitude = null;
		String latitude = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				date = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];
		}

		if (filename == null || bodyname == null || date == null ) {
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
			startDate = parseDate(date);
		} catch (ParseException e1) {
			e1.printStackTrace();
			System.exit(1);;
		}
		
		double jd = UNIX_EPOCH_AS_JD + ((double)startDate.getTime())/MILLISECONDS_PER_DAY;

		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(filename, jd - 1.0,
					jd + 2.0);
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
		
		RiseSetTest rst = new RiseSetTest();
		
		try {
			rst.run(ap, place, jd, RiseSetType.UPPER_LIMB);
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

	public void run(ApparentPlace ap, Place place, double jd, RiseSetType type) throws JPLEphemerisException {
		TransitEvent[] transitEvents = calculateTransitTimes(ap, place, jd);
		
		AltitudeEvent[] altitudeEvents = calculateAltitudeEvents(ap, place, jd, transitEvents, type);
	}
	
	private double getConstantPartOfTargetAltitude(int body, RiseSetType type) {
		switch (body) {
			case JPLEphemeris.SUN:
				switch (type) {
					case UPPER_LIMB:
						return HORIZONTAL_REFRACTION - SOLAR_SEMIDIAMETER;
						
					case CENTRE_OF_DISK:
						return HORIZONTAL_REFRACTION;
						
					case LOWER_LIMB:
						return HORIZONTAL_REFRACTION + SOLAR_SEMIDIAMETER;
						
					case CIVIL_TWILIGHT:
						return -6.0 * Math.PI/180.0;
						
					case NAUTICAL_TWILIGHT:
						return -12.0 * Math.PI/180.0;
						
					case ASTRONOMICAL_TWILIGHT:
						return -18.0 * Math.PI/180.0;
				}
				
			default:
				return HORIZONTAL_REFRACTION;
		}
	}
	
	private TransitEvent[] calculateTransitTimes(ApparentPlace ap, Place place, double jdstart) throws JPLEphemerisException {
		System.out.println("Entered calculateTransitTimes(ApparentPlace, Place, " + jdstart + ")");
		
		double deltaT = ap.getEarthRotationModel().deltaT(jdstart);
		
		System.out.println("\tDelta T = " + deltaT);
		
		ap.calculateApparentPlace(jdstart + deltaT);
		
		double ra = ap.getRightAscensionOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jdstart);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());
		
		System.out.println("\n\tHour angle at start of interval = " + toDegrees(ha));
		
		TransitEvent[] transits = new TransitEvent[3];
		
		double targetHA = ha < 0.0 ? 0.0 : Math.PI;
		
		TransitType targetType = ha < 0.0 ? TransitType.UPPER : TransitType.LOWER;
		
		double meanSiderealRate = getMeanSiderealRate(ap.getTarget().getBodyCode());
		
		double jd = jdstart;
		
		double dt = Double.NaN;
		
		int iTransits = 0;
		
		for (int i = 0; i < 3; i++) {
			System.out.println("\n\tLooking for transit " + i + " with target HA = " + toDegrees(targetHA));
			
			do {
				ap.calculateApparentPlace(jd + deltaT);
				
				ra = ap.getRightAscensionOfDate();
				
				gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jd);
				
				ha = gmst - ra + place.getLongitude();
				
				double dha = reduceAngle(ha - targetHA);
				
				dt = -dha/meanSiderealRate;
				
				jd += dt;
				
				System.out.println("\t\tdha = " + dha + ", dt = " + dt);
			} while (Math.abs(dt) > 0.00001);
			
			if (jd < jdstart + 1.0) {
				transits[i] = new TransitEvent(targetType, jd);
			
				System.out.println("\tTransit " + i + " is " + targetType + " at " + jd);
			
				iTransits++;
			}
			
			targetHA += Math.PI;
			
			targetType = (targetType == TransitType.LOWER) ? TransitType.UPPER : TransitType.LOWER;
			
			jd += Math.PI/meanSiderealRate;
		}
		
		System.out.println("\n\tFound " + iTransits + " transits");
		
		TransitEvent[] data = new TransitEvent[iTransits];
		
		for (int i = 0; i < iTransits; i++)
			data[i] = transits[i];
		
		return data;
	}
	
	private AltitudeEvent[] calculateAltitudeEvents(ApparentPlace ap, Place place, double jdstart, TransitEvent[] transitEvents, RiseSetType rsType)
			throws JPLEphemerisException {
		System.out.println("\nEntered calculateAltitudeEvents(ApparentPlace, Place, " + jdstart + ", TransitEvent[], " + rsType + ")");
		
		double targetAltitude = getConstantPartOfTargetAltitude(ap.getTarget().getBodyCode(), rsType);
		
		System.out.println("\tConstant part of target altitude is " + toDegrees(targetAltitude));
		
		double jdfinish = jdstart + 1.0;
		
		AltitudeEvent[] events = new AltitudeEvent[transitEvents.length + 2];
		
		events[0] = new AltitudeEvent(jdstart, calculateGeometricAltitude(ap, place, jdstart) - targetAltitude);
		
		System.out.println("\n\tAt start of interval, altitude function is " + toDegrees(events[0].altitude));
		
		final double h = 30.0/1440.0;
		
		for (int i = 0; i < transitEvents.length; i++) {
			System.out.println("\n\tFinding altitude extremum at transit " + i);
			
			double jd2 = transitEvents[i].date;
			
			double alt2 = calculateGeometricAltitude(ap, place, jd2) - targetAltitude;
			
			double jd1 = jd2 - h;
			
			double alt1 = calculateGeometricAltitude(ap, place, jd1) - targetAltitude;
			
			double jd3 = jd2 + h;
			
			double alt3 = calculateGeometricAltitude(ap, place, jd3) - targetAltitude;
			
			System.out.printf("\t\tInterpolation points: (%.5f, %.3f), (%.5f, %.3f), (%.5f, %.3f)\n", jd1, toDegrees(alt1), jd2, toDegrees(alt2), jd3, toDegrees(alt3));
			
			double a = alt3 - alt1;
			
			double b = alt1 - 2.0 * alt2 + alt3;
			
			double dt = -0.5 * h * a/b;
			
			System.out.printf("\t\ta = %.6f, b = %.6f, dt = %.5f\n", a, b, dt);
			
			double jd2new = jd2 + dt;
			
			if (jd2new < jdstart)
				jd2new = jdstart;
			
			if (jd2new > jdfinish)
				jd2new = jdfinish;
			
			double alt2new = calculateGeometricAltitude(ap, place, jd2new) - targetAltitude;
			
			System.out.println("\tAt transit " + i + ", altitude function is " + toDegrees(alt2new));
			
			events[i + 1] = new AltitudeEvent(jd2new, alt2new);
		}
		
		events[events.length - 1] = new AltitudeEvent(jdfinish, calculateGeometricAltitude(ap, place, jdfinish) - targetAltitude);
		
		System.out.println("\n\tAt end of interval, altitude function is " + toDegrees(events[events.length - 1].altitude));
		
		return events;
	}
	
	private double calculateGeometricAltitude(ApparentPlace ap, Place place, double jd) throws JPLEphemerisException {
		double deltaT = ap.getEarthRotationModel().deltaT(jd);
		
		ap.calculateApparentPlace(jd + deltaT);
		
		double ra = ap.getRightAscensionOfDate();
		
		double dec = ap.getDeclinationOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jd);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());
		
		double latitude = place.getLatitude();
		
		double q = Math.sin(latitude) * Math.sin(dec) + Math.cos(latitude) * Math.cos(dec) * Math.cos(ha);
		
		return Math.asin(q);
	}
	
	private double getMeanSiderealRate(int iBody) {
		switch (iBody) {
			case JPLEphemeris.SUN:
				return TWOPI;
				
			case JPLEphemeris.MOON:
				return TWOPI * (1.0 - 1.0/27.322);
				
			default:
				return TWOPI * 366.0/365.0;
		}
	}
	
	private double toDegrees(double x) {
		return 180.0 * x/Math.PI;
	}
		
	// Reduce an angle to the range (-PI, PI]
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWOPI;
		
		while (x <= -Math.PI)
			x+= TWOPI;
		
		return x;
	}
		
	private final DecimalFormat dfmt1 = new DecimalFormat("0000");
	private final DecimalFormat dfmt2 = new DecimalFormat("00");
	
	private String dateToString(double t) {
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
	}

}
