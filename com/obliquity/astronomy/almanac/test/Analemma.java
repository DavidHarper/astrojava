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
import java.util.GregorianCalendar;

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


public class Analemma {
	public static void main(String[] args) {
		String filename = null;
		int year = Integer.MIN_VALUE;
		int time = 720;
		double latitude = Double.NaN;
		int step = 1;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-ephemeris":
				filename = args[++i];
				break;
				
			case "-year":
				year = Integer.parseInt(args[++i]);
				break;
				
			case "-latitude":
				latitude = Double.parseDouble(args[++i]) * Math.PI/180.0;
				break;
				
			case "-step":
				step = Integer.parseInt(args[++i]);
				break;
				
			case "-time":
				String[] words = args[++i].split(":");
				time = 60 * Integer.parseInt(words[0]) + Integer.parseInt(words[1]);
				break;
				
			default:	
				showUsage("Unknown option: " + args[i]);
				break;
			}
		}

		if (filename == null) {
			showUsage("No ephemeris filename specified");
			System.exit(1);
		}
		
		if (year == Integer.MIN_VALUE) {
			GregorianCalendar gcal = new GregorianCalendar();
			
			year = gcal.get(GregorianCalendar.YEAR);
		}
		
		AstronomicalDate adStart = new AstronomicalDate(year, 1, 1);
		
		AstronomicalDate adFinish = new AstronomicalDate(year+1, 1, 1);
		
		double jdstart = adStart.getJulianDate();
		
		double jdfinish = adFinish.getJulianDate();

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
		
		Analemma analemma = new Analemma(ephemeris);
		
		try {
			analemma.run(latitude, year, time, step);
		} catch (JPLEphemerisException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private EarthRotationModel erm = null;
	
	private ApparentPlace apSun = null;
	
	private LocalVisibility lv = new LocalVisibility();
	
	public Analemma(JPLEphemeris ephemeris) {	
		erm = new IAUEarthRotationModel();
		
		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		apSun = new ApparentPlace(earth, sun, sun, erm);
	}
	
	public void run(double latitude, int year, int time, int step) throws JPLEphemerisException {
		Place place = new Place(latitude, 0.0, 0.0, 0.0);
		
		AstronomicalDate adStart = new AstronomicalDate(year, 1, 1, time/60, time%60, 0.0);
		
		double jdStart = adStart.getJulianDate();
		
		double jdFinish = jdStart + 366.0;
		
		for (double jd = jdStart; jd <= jdFinish; jd += (double)step) {
			HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(apSun, place, jd);
			
			double azimuth = hc.azimuth * 180.0/Math.PI;
			
			if (azimuth < 0.0)
				azimuth += 360.0;
			
			AstronomicalDate ad = new AstronomicalDate(jd);
			
			System.out.printf("%04d %02d %02d %02d %02d  %6.2f  %6.2f\n", ad.getYear(), ad.getMonth(), ad.getDay(),
						ad.getHour(), ad.getMinute(), azimuth, hc.altitude * 180.0/Math.PI);
		}
	}

	public static void showUsage(String message) {
		if (message != null)	
			System.err.println("ERROR: " + message + "\n");
		
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-latitude\tLatitude, in degrees");
		
		System.err.println();
		
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-year\t\tStart date [DEFAULT: current year]");
		System.err.println("\t-step\t\tStep size in days [DEFAULT: 1]");
		System.err.println("\t-time\t\tLocal mean time [DEFAULT: 12:00]");
	}

}
