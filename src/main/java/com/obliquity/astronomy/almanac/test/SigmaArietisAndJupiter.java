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

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.StarApparentPlace;
import com.obliquity.astronomy.almanac.Vector;

public class SigmaArietisAndJupiter {
	// Hipparcos 2 catalogue
	private double raHip2 = 0.7482784923, decHip2 = 0.2632327741, plxHip2 = 6.60, pmRAHip2 = 30.13, pmDecHip2 = -23.68;
	private double J2000 = 2451545.0;
	private double epochHip2 = J2000 + (1991.25 - 2000.0) * 365.25;
	
	// FK6
	private double rvFK6 = 17.0;
	
	private JPLEphemeris ephemeris = null;
	private StarApparentPlace sap = null;
	private ApparentPlace apJupiter = null;
	private boolean useJ2000 = Boolean.getBoolean("sigmaarietisandjupiter.usej2000");
		
	public SigmaArietisAndJupiter(JPLEphemeris ephemeris) {
		this.ephemeris = ephemeris;
		
		MovingPoint earth = new EarthCentre(ephemeris);
		
		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);
		
		MovingPoint jupiter = new PlanetCentre(ephemeris, JPLEphemeris.JUPITER);
		
		EarthRotationModel erm = useJ2000 ? null : new IAUEarthRotationModel();
		
		apJupiter = new ApparentPlace(earth, jupiter, sun, erm);
		
		sap = new StarApparentPlace(earth, sun, erm);
	}

	public static void main(String[] args) {
		String filename = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];
		}
		
		if (filename == null) {
			System.err.println("You MUST specify an ephemeris file with the -ephemeris option.");
			System.exit(1);
		}
		
		String[] dates = new String[args.length - 2];
		
		for (int i = 2; i < args.length; i++)
			dates[i-2] = args[i];
		
		try {
			JPLEphemeris ephemeris = new JPLEphemeris(filename);
			
			SigmaArietisAndJupiter runner = new SigmaArietisAndJupiter(ephemeris);
			
			runner.run(dates);
		} catch (IOException | JPLEphemerisException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void run(String[] args) throws JPLEphemerisException {
		if (args.length == 0) {
			runForDate(new AstronomicalDate(1952, 11, 20));
		
			runForDate(new AstronomicalDate(2023, 8, 21));
		} else {
			for (String datestr: args) {
				AstronomicalDate ad = parseDate(datestr);
				
				runForDate(ad);
			}
		}
	}
	
	private AstronomicalDate parseDate(String datestr) {
		String[] words = datestr.split("-");
		
		if (words.length != 3)
			throw new IllegalArgumentException("Date string \"" + datestr + "\" is invalid");
		
		int year = Integer.parseInt(words[0]);
		int month = Integer.parseInt(words[1]);
		int day = Integer.parseInt(words[2]);
		
		return new AstronomicalDate(year, month, day);
	}
	
	private void runForDate(AstronomicalDate ad) throws JPLEphemerisException {
		System.out.println("================================================================================");
		System.out.printf("RUNNING FOR %04d-%02d-%02d\n", ad.getYear(), ad.getMonth(), ad.getDay());
		
		double jd = ad.getJulianDate();
		
		Vector pStar = sap.calculateApparentPlace(raHip2, decHip2, plxHip2, pmRAHip2, pmDecHip2, rvFK6, epochHip2, J2000, jd);
		
		displayEquatorialCoordinates(pStar);
		
		double raStar = Math.atan2(pStar.getY(), pStar.getX());
		
		double decStar = Math.asin(pStar.getZ());
		
		for (int i = 0; i < 144; i++) {
			apJupiter.calculateApparentPlace(jd);
			
			double raJupiter = useJ2000 ? apJupiter.getRightAscensionJ2000() : apJupiter.getRightAscensionOfDate();
			
			double decJupiter = useJ2000 ? apJupiter.getDeclinationJ2000() : apJupiter.getDeclinationOfDate();
			
			double gd = apJupiter.getGeometricDistance();
			
			double sd = 98.47 / gd;
			
			double dx = (raJupiter - raStar) * Math.cos(decStar) * 3600.0 * 180.0/Math.PI;
			
			double dy = (decJupiter - decStar) * 3600.0 * 180/Math.PI;
			
			double dr = Math.sqrt(dx * dx + dy * dy);
			
			int mins = 10 * i;
			
			int hours = mins/60;
			
			mins -= hours * 60;
			
			System.out.printf("  %02d:%02d   %8.3f   %8.3f   %8.3f   %8.3f   %8.3f\n", hours, mins, dx, dy, dr, sd, dr-sd);
			
			jd += 10.0/1440.0;
		}
	}
	
	private void displayEquatorialCoordinates(Vector p) {
		p.normalise();
		
		double ra = Math.atan2(p.getY(), p.getX());
		
		double dec = Math.asin(p.getZ());
		
		ra *= 12.0/Math.PI;
		
		if (ra < 0.0)
			ra += 24.0;
		
		int rah = (int)ra;
		
		ra -= (double)rah;
		
		ra *= 60.0;
		
		int ram = (int)ra;
		
		ra -= (double)ram;
		
		double ras = 60.0 * ra;
		
		dec *= 180.0/Math.PI;
		
		char decp = dec > 0.0 ? 'N' : 'S';
		
		if (dec < 0.0)
			dec = -dec;
		
		int decd = (int)dec;
		
		dec -= (double) decd;
		
		dec *= 60.0;
		
		int decm = (int)dec;
		
		dec -= (double)decm;
		
		double decs = 60.0 * dec;
		
		System.out.printf("  RA  %2d %02d %6.3f\n", rah, ram, ras);
		System.out.printf("  Dec %2d %02d %5.2f %s\n", decd, decm, decs, decp);
		
		System.out.println();
	}

}
