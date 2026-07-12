/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2026 David Harper at obliquity.com
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

import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.obliquity.astronomy.almanac.AlmanacData;
import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.StarApparentPlace;
import com.obliquity.astronomy.almanac.TerrestrialObserver;
import com.obliquity.astronomy.almanac.Vector;

class Star {
	public double rightAscension, declination, pmRA, pmDec, parallax, radialVelocity, epoch;
		
	public Star(String starFile) throws IOException {
		File propsFile = new File(starFile);
		InputStream is = new FileInputStream(propsFile);
		Properties props = new Properties();
		props.load(is);
		
		rightAscension = parseHexagesimal(props.get("rightAscension")) * Math.PI/12.0;
		declination = parseHexagesimal(props.get("declination")) * Math.PI/180.0;
		pmRA = parseDouble(props.get("pmRA"));
		pmDec = parseDouble(props.get("pmDec"));
		parallax = parseDouble(props.get("parallax"));
		radialVelocity = parseDouble(props.get("radialVelocity"));
		epoch = parseDouble(props.get("epoch"));
	}

	private double parseDouble(Object object) {
		String value = (String)object;
		
		return value == null ? 0 : Double.parseDouble(value);
	}
	
	private double parseHexagesimal(Object object) {
		if (object == null)
			return Double.NaN;
		
		String[] words = ((String)object).split(":");
		
		boolean isNegative = words[0].startsWith("-");
		
		double value = Math.abs(Double.parseDouble(words[0]));
		
		if (words.length > 1)
			value += Double.parseDouble(words[1])/60.0;
		
		if (words.length > 2)
			value += Double.parseDouble(words[2])/3600.0;
		
		if (isNegative)
			value = -value;
		
		return value;
	}
}

public class StarAppulse {
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datetimefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd/HH:mm");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	private static final double J2000 = 2451545.0;
	private static double AU = 0.0;

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		datetimefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String ephemerisFilename = null;
		String planetName = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		String starFile = null;
		String longitude = null;
		String latitude = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				ephemerisFilename = args[++i];

			if (args[i].equalsIgnoreCase("-planet"))
				planetName = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];

			if (args[i].equalsIgnoreCase("-star"))
				starFile = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];

		}

		if (ephemerisFilename == null || planetName == null || starFile == null || startdate == null || enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(planetName);

		if (kBody < 0) {
			System.err.println("Unknown planet name: \"" + planetName + "\"");
			System.exit(1);
		}
		
		Date date = null;
		
		try {
			date = parseDate(startdate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse -startdate value \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		if (enddate != null) {
			try {
				date = parseDate(enddate);
			} catch (ParseException e) {
				System.err.println(
						"Failed to parse -enddate value \"" + enddate + "\" as an ISO date");
				e.printStackTrace();
				System.exit(1);
			}
		} 

		double jdfinish = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		double jdstep = (stepsize == null) ? 1.0 : parseStepSize(stepsize);

		JPLEphemeris ephemeris = null;

		try {
			ephemeris = new JPLEphemeris(ephemerisFilename, jdstart - 1.0,
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
		
		AU = ephemeris.getAU();
		
		Star star = null;
		
		try {
			star = new Star(starFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		MovingPoint planet = null;

		if (kBody == JPLEphemeris.MOON)
			planet = new MoonCentre(ephemeris);
		else
			planet = new PlanetCentre(ephemeris, kBody);

		EarthCentre earth;

		EarthRotationModel erm = new IAUEarthRotationModel();

		if (latitude != null && longitude != null) {
			double lat = Double.parseDouble(latitude) * Math.PI / 180.0;
			double lon = Double.parseDouble(longitude) * Math.PI / 180.0;

			earth = (EarthCentre)new TerrestrialObserver(ephemeris, erm, lat, lon, 0.0);
		} else {
			earth = new EarthCentre(ephemeris);
		}

		MovingPoint sun = null;

		if (kBody == JPLEphemeris.SUN)
			sun = planet;
		else
			sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		ApparentPlace apPlanet = new ApparentPlace(earth, planet, sun, erm);

		ApparentPlace apSun = (kBody == JPLEphemeris.SUN ? apPlanet
				: new ApparentPlace(earth, sun, sun, erm));
		
		StarApparentPlace apStar = new StarApparentPlace(earth, sun, erm);
		
		StarAppulse runner = new StarAppulse();

		try {
			runner.run(apPlanet, apSun, apStar, star, jdstart, jdfinish, jdstep);
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	private static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-planet\t\tName of body",
				"\t-star\t\tName of file containing star properties",
				"",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"Valid date formats are YYYY-MM-DD or YYYY-MM-DD/hh:mm",
				"",
				"OPTIONAL PARAMETERS",
				"\t-step\t\tStep size",
				"",
				"Valid step size formats are an integer or an integer followed by a single letter (d, h, m, s) to indicate",
				"units.  If no units are specified, days are asssumed.",
				"",
				"\t-latitude\tThe latitude of the observer, in degrees.",
				"\t-longitude\tThe longitude of the observer, in degrees.",
				"",
				"If no latitude and longitude are specified, the geocentre will be used."
		};
		
		for (String line : lines)
			System.err.println(line);		
	}

	private void run(ApparentPlace apPlanet, ApparentPlace apSun, StarApparentPlace apStar, Star star, double jdstart, double jdfinish, double jdstep)
			throws JPLEphemerisException {
		double starEpoch = J2000 + (star.epoch - 2000.0) * 365.25;
		
		for (double jd = jdstart; jd <= jdfinish; jd += jdstep) {
			Vector pStar = apStar.calculateApparentPlace(star.rightAscension, star.declination, star.parallax, star.pmRA, star.pmDec, star.radialVelocity, starEpoch, J2000, jd);
						
			double raStar = Math.atan2(pStar.getY(), pStar.getX());
			
			double decStar = Math.asin(pStar.getZ());
			
			apPlanet.calculateApparentPlace(jd);
			
			double raPlanet = apPlanet.getRightAscensionOfDate();
			
			double decPlanet = apPlanet.getDeclinationOfDate();
			
			double gd = apPlanet.getGeometricDistance();
			
			double sd = calculateSemiDiameter(apPlanet.getTarget().getBodyCode(), gd);
			
			double dx = (raPlanet - raStar) * Math.cos(decStar) * 3600.0 * 180.0/Math.PI;
			
			double dy = (decPlanet - decStar) * 3600.0 * 180/Math.PI;
			
			double dr = Math.sqrt(dx * dx + dy * dy);
			
			AstronomicalDate ad = new AstronomicalDate(jd);
			
			ad.roundToNearestMinute();
			
			System.out.printf("%04d-%02d-%02d  %02d:%02d   %8.3f   %8.3f   %8.3f   %8.3f   %8.3f\n", ad.getYear(), ad.getMonth(), ad.getDay(), ad.getHour(), ad.getMinute(),
					dx, dy, dr, sd, dr-sd);
		}
	}
	
	private static final double MOON_RADIUS = 1738.0;
	private static final double SUN_RADIUS = 696000.0;

	private double calculateSemiDiameter(int targetCode, double d) {
		switch (targetCode) {
		case JPLEphemeris.MERCURY:
			return 3.34 / d;

		case JPLEphemeris.VENUS:
			return 8.41 / d;

		case JPLEphemeris.MARS:
			return 4.68 / d;

		case JPLEphemeris.JUPITER:
			return 98.47 / d;

		case JPLEphemeris.SATURN:
			return 83.33 / d;

		case JPLEphemeris.URANUS:
			return 34.28 / d;

		case JPLEphemeris.NEPTUNE:
			return 36.56 / d;

		case JPLEphemeris.PLUTO:
			return 1.64 / d;
			
		case JPLEphemeris.MOON:
			d *= AU;
			return 3600.0 * (180.0/Math.PI) * Math.asin(MOON_RADIUS/d);
			
		case JPLEphemeris.SUN:
			d *= AU;
			return 3600.0 * (180.0/Math.PI) * Math.asin(SUN_RADIUS/d);

		default:
			throw new IllegalStateException(
					"Cannot calculate a magnitude for the target body.");
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
	
	private static double parseStepSize(String str) {
		Pattern pattern = Pattern.compile("(\\d+)([a-zA-Z]?)");
		
		Matcher matcher = pattern.matcher(str);
		
		if (matcher.matches()) {
			double step = Double.parseDouble(matcher.group(1));
			
			String units = matcher.group(2);
			
			switch (units) {
			case "s":
			case "S":
				step /= 86400.0;
				break;
				
			case "m":
			case "M":
				step /= 1440.0;
				break;
				
			case "h":
			case "H":
				step /= 24.0;
				break;
			}
			
			return step;
		} else
			return Double.NaN;
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


}
