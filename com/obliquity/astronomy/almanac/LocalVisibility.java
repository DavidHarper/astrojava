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

package com.obliquity.astronomy.almanac;

import java.util.ArrayList;
import java.util.List;

public class LocalVisibility {
	class AltitudeEvent {
		double date;
		double altitude;
		
		public AltitudeEvent(double date, double altitude) {
			this.date = date;
			this.altitude = altitude;
		}
	}
	
	public static final double TWOPI = 2.0 * Math.PI;
		
	private static final double EPSILON_ALTITUDE = (0.1/60.0) * Math.PI/180.0;
	
	private static final int MAX_ITERS = 20;
	
	private static final double EARTH_RADIUS = 6378.137;
	
	private static final double HORIZONTAL_REFRACTION = (-34.0 / 60.0) * Math.PI/180.0;
	
	private static final double SOLAR_SEMIDIAMETER = (16.0 / 60.0) * Math.PI/180.0;
	
	private double AU = Double.NaN;
	
	private final boolean verbose = !Boolean.getBoolean("quiet");
	
	public RiseSetEvent[] findRiseSetEvents(ApparentPlace ap, Place place, double jd, RiseSetType rsType) throws JPLEphemerisException {
		this.AU = ap.getTarget().getEphemeris().getAU();
		
		TransitEvent[] transitEvents = findTransitEvents(ap, place, jd);

		AltitudeEvent[] altitudeEvents = calculateAltitudeEvents(ap, place, jd, transitEvents, rsType);
		
		int nEvents = altitudeEvents.length;
		
		if (verbose) {
			System.out.println("\nThere are " + nEvents + " altitude events:");
		
			for (int i = 0; i < nEvents; i++)
				System.out.printf("\tEvent %1d : date = %.5f, altitude = %.3f\n", i, altitudeEvents[i].date , toDegrees(altitudeEvents[i].altitude));
		}
		
		List<RiseSetEvent> eventList = new ArrayList<RiseSetEvent>();
		
		for (int i = 0; i < nEvents - 1; i++) {
			double jd1 = altitudeEvents[i].date;
			double alt1 = altitudeEvents[i].altitude;
			
			double jd2 = altitudeEvents[i+1].date;
			double alt2 = altitudeEvents[i+1].altitude;
			
			if (hasOppositeSign(alt1, alt2)) {
				if (verbose)
					System.out.println("\nThere is a sign change between event " + i + " and event " + (i+1));
				
				double jdEvent = findRiseSetEventTimeByFalsePosition(ap, place, jd1, jd2, rsType);
				
				if(Double.isNaN(jdEvent))
					jdEvent = findRiseSetEventTimeByBisection(ap, place, jd1, jd2, rsType);
				
				if (!Double.isNaN(jdEvent))
					eventList.add(new RiseSetEvent(alt1 < 0.0 ? RiseSetEventType.RISE : RiseSetEventType.SET, jdEvent));
			}
		}
		
		RiseSetEvent[] events = new RiseSetEvent[eventList.size()];
		
		return eventList.toArray(events);
	}

	private boolean hasOppositeSign(double x, double y) {
		return (x < 0.0 && y > 0.0) || (x > 0.0 && y < 0.0);
	}
	
	private double findRiseSetEventTimeByBisection(ApparentPlace ap, Place place,
			double jd1, double jd2, RiseSetType rsType) throws JPLEphemerisException {
		if (verbose)
			System.out.println("\nEntered findRiseSetEvent(ApparentPlace, Place, " + jd1 + ", " + jd2 + 
					", " + rsType + ")");

		double targetAltitude = getConstantPartOfTargetAltitude(ap.getTarget().getBodyCode(), rsType);
		
		double jdLow = jd1;
		double jdHigh = jd2;
		
		// Use the method of bisection to find the root
		for (int nIters = 0; nIters < MAX_ITERS; nIters++) {
			double altLow = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdLow, rsType) - targetAltitude;
			
			double altHigh = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdHigh, rsType) - targetAltitude;
			
			if (verbose)
				System.out.printf("\n\tIteration %2d\n\t\tLow:  t = %.5f, altitude = %.5f\n\t\tHigh: t = %.5f, altitude = %.5f\n",
					nIters, jdLow, toDegrees(altLow), jdHigh, toDegrees(altHigh));

			double jdNew = 0.5 * (jdLow + jdHigh);
			
			double altNew = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdNew, rsType) - targetAltitude;
			
			boolean replaceHigh = hasOppositeSign(altLow, altNew);
			
			if (verbose)
				System.out.printf("\t\tNew:  t = %.5f, altitude = %.5f [replaces %s]\n", jdNew, toDegrees(altNew), replaceHigh ? "HIGH" : "LOW");
		
			if (Math.abs(altNew) < EPSILON_ALTITUDE)
				return jdNew;
			
			if (replaceHigh)
				jdHigh = jdNew;
			else
				jdLow = jdNew;
		}
		
		return Double.NaN;
	}
	
	private double findRiseSetEventTimeByFalsePosition(ApparentPlace ap, Place place,
			double jd1, double jd2, RiseSetType rsType) throws JPLEphemerisException {
		if (verbose)
			System.out.println("\nEntered findRiseSetEvent(ApparentPlace, Place, " + jd1 + ", " + jd2 + 
					", " + rsType + ")");

		double targetAltitude = getConstantPartOfTargetAltitude(ap.getTarget().getBodyCode(), rsType);
		
		double jdLow = jd1;
		double jdHigh = jd2;
		
		// Use the method of false position (Regula Falsi) to find the root
		for (int nIters = 0; nIters < MAX_ITERS; nIters++) {
			double altLow = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdLow, rsType) - targetAltitude;
			
			double altHigh = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdHigh, rsType) - targetAltitude;
			
			if (verbose)
				System.out.printf("\n\tIteration %2d\n\t\tLow:  t = %.5f, altitude = %.5f\n\t\tHigh: t = %.5f, altitude = %.5f\n",
					nIters, jdLow, toDegrees(altLow), jdHigh, toDegrees(altHigh));

			double jdNew = (jdLow * altHigh - jdHigh * altLow)/(altHigh - altLow);
			
			double altNew = calculateGeometricAltitudeForRisingOrSetting(ap, place, jdNew, rsType) - targetAltitude;
			
			boolean replaceHigh = hasOppositeSign(altLow, altNew);
			
			if (verbose)
				System.out.printf("\t\tNew:  t = %.5f, altitude = %.5f [replaces %s]\n", jdNew, toDegrees(altNew), replaceHigh ? "HIGH" : "LOW");
		
			if (Math.abs(altNew) < EPSILON_ALTITUDE)
				return jdNew;
			
			if (replaceHigh)
				jdHigh = jdNew;
			else
				jdLow = jdNew;
		}
		
		return Double.NaN;
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
	
	public TransitEvent[] findTransitEvents(ApparentPlace ap, Place place, double jdstart) throws JPLEphemerisException {
		if (verbose)
			System.out.println("Entered calculateTransitTimes(ApparentPlace, Place, " + jdstart + ")");
		
		double deltaT = ap.getEarthRotationModel().deltaT(jdstart);
		
		if (verbose)
			System.out.println("\tDelta T = " + deltaT);
		
		ap.calculateApparentPlace(jdstart + deltaT);
		
		double ra = ap.getRightAscensionOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jdstart);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());
		
		if (verbose)
			System.out.println("\n\tHour angle at start of interval = " + toDegrees(ha));
		
		TransitEvent[] transits = new TransitEvent[3];
		
		double targetHA = ha < 0.0 ? 0.0 : Math.PI;
		
		TransitType targetType = ha < 0.0 ? TransitType.UPPER : TransitType.LOWER;
		
		double meanSiderealRate = getMeanSiderealRate(ap.getTarget().getBodyCode());
		
		double jd = jdstart;
		
		double dt = Double.NaN;
		
		int iTransits = 0;
		
		for (int i = 0; i < 3; i++) {
			if (verbose)	
			System.out.println("\n\tLooking for transit " + i + " with target HA = " + toDegrees(targetHA));
			
			do {
				ap.calculateApparentPlace(jd + deltaT);
				
				ra = ap.getRightAscensionOfDate();
				
				gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jd);
				
				ha = gmst - ra + place.getLongitude();
				
				double dha = reduceAngle(ha - targetHA);
				
				dt = -dha/meanSiderealRate;
				
				jd += dt;
				
				if (verbose)
					System.out.println("\t\tdha = " + dha + ", dt = " + dt);
			} while (Math.abs(dt) > 0.00001);
			
			if (jd < jdstart + 1.0) {
				transits[i] = new TransitEvent(targetType, jd);
			
				if (verbose)
					System.out.println("\tTransit " + i + " is " + targetType + " at " + jd);
			
				iTransits++;
			}
			
			targetHA += Math.PI;
			
			targetType = (targetType == TransitType.LOWER) ? TransitType.UPPER : TransitType.LOWER;
			
			jd += Math.PI/meanSiderealRate;
		}
		
		if (verbose)
			System.out.println("\n\tFound " + iTransits + " transits");
		
		TransitEvent[] data = new TransitEvent[iTransits];
		
		for (int i = 0; i < iTransits; i++)
			data[i] = transits[i];
		
		return data;
	}
	
	private AltitudeEvent[] calculateAltitudeEvents(ApparentPlace ap, Place place, double jdstart, TransitEvent[] transitEvents,  RiseSetType rsType)
			throws JPLEphemerisException {
		if (verbose)
			System.out.println("\nEntered calculateAltitudeEvents(ApparentPlace, Place, " + jdstart + ", TransitEvent[], "
					+  rsType + ")");

		double targetAltitude = getConstantPartOfTargetAltitude(ap.getTarget().getBodyCode(), rsType);
			
		if (verbose)
			System.out.println("\tConstant part of target altitude is " + toDegrees(targetAltitude));
		
		double jdfinish = jdstart + 1.0;
		
		AltitudeEvent[] events = new AltitudeEvent[transitEvents.length + 2];
		
		events[0] = new AltitudeEvent(jdstart, calculateGeometricAltitudeForRisingOrSetting(ap, place, jdstart, rsType) - targetAltitude);
		
		if (verbose)
			System.out.println("\n\tAt start of interval, altitude function is " + toDegrees(events[0].altitude));
		
		final double h = 30.0/1440.0;
		
		for (int i = 0; i < transitEvents.length; i++) {
			if (verbose)
				System.out.println("\n\tFinding altitude extremum at transit " + i);
			
			double jd2 = transitEvents[i].date;
			
			double alt2 = calculateGeometricAltitudeForRisingOrSetting(ap, place, jd2, rsType) - targetAltitude;
			
			double jd1 = jd2 - h;
			
			double alt1 = calculateGeometricAltitudeForRisingOrSetting(ap, place, jd1, rsType) - targetAltitude;
			
			double jd3 = jd2 + h;
			
			double alt3 = calculateGeometricAltitudeForRisingOrSetting(ap, place, jd3, rsType) - targetAltitude;
			
			if (verbose)	
				System.out.printf("\t\tInterpolation points: (%.5f, %.3f), (%.5f, %.3f), (%.5f, %.3f)\n", jd1, toDegrees(alt1), jd2, toDegrees(alt2), jd3, toDegrees(alt3));
			
			double a = alt3 - alt1;
			
			double b = alt1 - 2.0 * alt2 + alt3;
			
			double dt = -0.5 * h * a/b;
			
			if (verbose)
				System.out.printf("\t\ta = %.6f, b = %.6f, dt = %.5f\n", a, b, dt);
			
			double jd2new = jd2 + dt;
			
			if (jd2new < jdstart)
				jd2new = jdstart;
			
			if (jd2new > jdfinish)
				jd2new = jdfinish;
			
			double alt2new = calculateGeometricAltitudeForRisingOrSetting(ap, place, jd2new, rsType) - targetAltitude;
			
			if (verbose)
				System.out.println("\tAt transit " + i + ", altitude function is " + toDegrees(alt2new));
			
			events[i + 1] = new AltitudeEvent(jd2new, alt2new);
		}
		
		events[events.length - 1] = new AltitudeEvent(jdfinish, calculateGeometricAltitudeForRisingOrSetting(ap, place, jdfinish, rsType) - targetAltitude);
		
		if (verbose)
			System.out.println("\n\tAt end of interval, altitude function is " + toDegrees(events[events.length - 1].altitude));
		
		return events;
	}
	
	public double calculateGeometricAltitude(ApparentPlace ap, Place place, double jd) 
			 throws JPLEphemerisException {
		double deltaT = ap.getEarthRotationModel().deltaT(jd);
		
		ap.calculateApparentPlace(jd + deltaT);
		
		double ra = ap.getRightAscensionOfDate();
		
		double dec = ap.getDeclinationOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jd);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());
		
		double latitude = place.getLatitude();
		
		return Math.asin(Math.sin(latitude) * Math.sin(dec) + Math.cos(latitude) * Math.cos(dec) * Math.cos(ha));
	}
	
	public double calculateRefraction(double altitude, double temperature, double pressure, RefractionType type) {
		double correctionFactor = 0.28 * pressure/(temperature + 273.15);
		
		double a = 180.0 * altitude/Math.PI;
		
		double refraction = (type == RefractionType.APPARENT_TO_GEOMETRIC) ?
				1.0 / Math.tan((a + 7.31/(a + 4.4)) * Math.PI/180.0) :
				1.02 / Math.tan((a + 10.3/(a + 5.11)) * Math.PI/180.0);
		
		return refraction * correctionFactor;
	}
	
	public static final double STANDARD_TEMPERATURE = 10.0;
	public static final double STANDARD_PRESSURE = 1010.0;
	
	public double calculateRefraction(double altitude, RefractionType type) {
		return calculateRefraction(altitude, STANDARD_TEMPERATURE, STANDARD_PRESSURE, type);
	}
	
	public double calculateApparentAltitude(ApparentPlace ap, Place place, double jd, double temperature, double pressure) 
			 throws JPLEphemerisException {
		double geometricAltitude = calculateGeometricAltitude(ap, place, jd);
		
		double refraction = calculateRefraction(geometricAltitude, temperature, pressure, RefractionType.GEOMETRIC_TO_APPARENT);
		
		return geometricAltitude + refraction;
	}
	
	public double calculateApparentAltitude(ApparentPlace ap, Place place, double jd) throws JPLEphemerisException {
		return calculateApparentAltitude(ap, place, jd, STANDARD_TEMPERATURE, STANDARD_PRESSURE);
	}
	
	private double calculateGeometricAltitudeForRisingOrSetting(ApparentPlace ap, Place place, double jd, RiseSetType rsType) throws JPLEphemerisException {
		// NOTE
		//
		// Duplication of code from calculateGeometricAltitude is deliberate
		// because we need the geometric distance from the apparent place in
		// order to calculate the parallax if this is the Moon.
		
		double deltaT = ap.getEarthRotationModel().deltaT(jd);
		
		ap.calculateApparentPlace(jd + deltaT);
		
		double ra = ap.getRightAscensionOfDate();
		
		double dec = ap.getDeclinationOfDate();
		
		double gmst = ap.getEarthRotationModel().greenwichApparentSiderealTime(jd);
		
		double ha = reduceAngle(gmst - ra + place.getLongitude());
		
		double latitude = place.getLatitude();
		
		double alt = Math.asin(Math.sin(latitude) * Math.sin(dec) + Math.cos(latitude) * Math.cos(dec) * Math.cos(ha));
		
		// Adjust altitude for Moon's parallax and semi-diameter
		if (isMoon(ap)) {
			double hp= Math.asin(EARTH_RADIUS/(AU * ap.getGeometricDistance()));
			
			switch (rsType) {
				case UPPER_LIMB:
					alt -= 0.7276 * hp;
					break;
					
				case CENTRE_OF_DISK:
					alt -= hp;
					break;
					
				case LOWER_LIMB:
					alt -= 1.2724 * hp;
					
				default:
					// Do nothing
			}
		}
		
		return alt;
	}
	
	private boolean isMoon(ApparentPlace ap) {
		return ap.getTarget().getBodyCode() == JPLEphemeris.MOON;
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
}
