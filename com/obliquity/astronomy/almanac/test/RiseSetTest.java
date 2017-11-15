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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.*;

public class RiseSetTest {
	public static final double TWOPI = 2.0 * Math.PI;
	
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	private static final double EPSILON = 0.1/86400.0;
	
	private static final double HORIZONTAL_REFRACTION = (-34.0 / 60.0) * Math.PI/180.0;
	
	private static final double SOLAR_SEMIDIAMETER = (16.0 / 60.0) * Math.PI/180.0;

	public static void main(String[] args) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String longitude = null;
		String latitude = null;

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
		}

		if (filename == null || bodyname == null || startdate == null || enddate == null) {
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
			startDate = datefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse start date \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)startDate.getTime())/MILLISECONDS_PER_DAY;

		Date endDate = null;
		
		try {
			endDate = datefmt.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse end date \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)endDate.getTime())/MILLISECONDS_PER_DAY;

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
		
		RiseSetTest rst = new RiseSetTest();
		
		try {
			rst.run(ap, place, jdstart, jdfinish);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}

	public void run(ApparentPlace ap, Place place, double jdstart, double jdfinish) throws JPLEphemerisException {
		RiseSetEvent rse[] = calculateRiseSetTime(ap, place, jdstart, jdfinish,
					RiseSetEvent.RISE_SET, RiseSetEvent.UPPER_LIMB);
		
		if (rse != null && rse.length > 0) {
			for (RiseSetEvent e : rse) {
				AstronomicalDate ad = new AstronomicalDate(e.getTime());
				
				System.out.printf("%04d-%02d-%02d %02d:%02d %s\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(), e.getEventAsString());
			}
		}
	}

	private RiseSetEvent[] calculateRiseSetTime(ApparentPlace ap, Place place,
			double jdstart, double jdfinish, int type, int limb) throws JPLEphemerisException {
		MovingPoint observer = ap.getObserver();

		if (!(observer instanceof EarthCentre)) 
			throw new IllegalArgumentException("Observer is not EarthCentre");
		
		// Calculate altitude and hour angle at the start of the interval
		
		ap.calculateApparentPlace(jdstart);
		
		double ra = ap.getRightAscensionOfDate();
		double dec = ap.getDeclinationOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jdstart);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());

		double phi = place.getLatitude();
		
		double q = Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(ha);
		
		double a = Math.asin(q);
		
		debug("At initial time " + jdstart + ", hour angle = " + (180.0 * ha/Math.PI) + ", altitude = " + (180.0 * a/Math.PI));
		
		java.util.Vector<RiseSetEvent> events = new java.util.Vector<RiseSetEvent>();
		
		boolean startAtPreviousTransit = a > 0.0 && ha > 0.0;
		
		// Calculate approximate time of first transit
		double transitTime = 0.0;
		
		if (startAtPreviousTransit) {
			// ha > 0 by definition
			transitTime = jdstart - ha / TWOPI;
		} else {
			if (ha > 0.0) {
				transitTime = jdstart + (TWOPI - ha) / TWOPI;
			} else {
				transitTime = jdstart - ha / TWOPI;
			}
		}
		
		debug("Approximate time of first transit is " + transitTime);
		
		transitTime = findNearestTransit(ap, place, transitTime);
		
		debug("Improved time of first transit is " + transitTime);
		
		double riseTime = 0.0, setTime;
		
		do {
			riseTime = findRiseSetTime(ap, place, transitTime, RiseSetEvent.RISING);
			
			if (riseTime >= jdstart && riseTime < jdfinish)
				events.add(new RiseSetEvent(RiseSetEvent.RISING, riseTime));
			
			setTime = findRiseSetTime(ap, place, transitTime, RiseSetEvent.SETTING);
			
			if (setTime >= jdstart && setTime <= jdfinish)
				events.add(new RiseSetEvent(RiseSetEvent.SETTING, setTime));
			
			transitTime = findNearestTransit(ap, place, transitTime + 1.0);
		} while (riseTime < jdfinish);
		
		RiseSetEvent[] rse = new RiseSetEvent[events.size()];
		
		return events.toArray(rse);
	}
	
	private double findNearestTransit(ApparentPlace ap, Place place, double t0) throws JPLEphemerisException {
		double dt = 0.0;
		double t = t0;
		
		do {
			ap.calculateApparentPlace(t);
			
			double ra = ap.getRightAscensionOfDate();
			
			double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(t);
			
			double ha = reduceAngle(gmst - ra + place.getLongitude());
			
			dt = -ha / TWOPI;
			
			t += dt;
		} while (Math.abs(dt) > EPSILON);
		
		return t;
	}
	
	private double findRiseSetTime(ApparentPlace ap, Place place, double transitTime, int type) throws JPLEphemerisException {
		ap.calculateApparentPlace(transitTime);
		
		double dec = ap.getDeclinationOfDate();
		
		int body = ap.getTarget().getBodyCode();
		
		double targetAltitude = HORIZONTAL_REFRACTION;
		
		if (body == JPLEphemeris.SUN)
			targetAltitude -= SOLAR_SEMIDIAMETER;
		
		double phi = place.getLatitude();
		
		double q = (Math.sin(targetAltitude) - Math.sin(dec) * Math.sin(phi)) / (Math.cos(dec) * Math.cos(phi));
		
		double sda = Math.acos(q);
		
		double dt = sda/TWOPI;
		
		return transitTime + (type == RiseSetEvent.RISING ? -dt : dt);
	}
		
	// Reduce an angle to the range (-PI, PI]
	private double reduceAngle(double x) {
		while (x > Math.PI)
			x -= TWOPI;
		
		while (x <= -Math.PI)
			x+= TWOPI;
		
		return x;
	}
	
	private void debug(String str) {
		if (Boolean.getBoolean("debug")) {
			System.err.print("DEBUG: ");
			System.err.println(str);
		}
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
