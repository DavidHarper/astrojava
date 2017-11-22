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

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static java.lang.Math.*;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.Vector;

public class SimpleAlmanac {
	public static final int OF_DATE = 1;
	public static final int J2000 = 2;
	public static final int B1875 = 3;

	private int targetEpoch = OF_DATE;

	private final DecimalFormat dfmta = new DecimalFormat("00.000");
	private final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private final DecimalFormat ifmta = new DecimalFormat("00");
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private ApparentPlace apTarget, apSun;
	private IAUEarthRotationModel erm = null;
	private Matrix precessJ2000toB1875 = null;

	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	private static final double TWO_PI = 2.0 * Math.PI;

	public SimpleAlmanac(ApparentPlace apTarget, ApparentPlace apSun,
			int targetEpoch) {
		this.apTarget = apTarget;
		this.apSun = apSun;
		this.targetEpoch = targetEpoch;
		
		erm = (IAUEarthRotationModel)apTarget.getEarthRotationModel();

		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		// Calculate the precession matrix from J2000 to B1875
		double epochJ2000 = erm.JulianEpoch(2000.0);
		double epochB1875 = erm.BesselianEpoch(1875.0);
		precessJ2000toB1875 = new Matrix();
		erm.precessionMatrix(epochJ2000, epochB1875, precessJ2000toB1875);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		int targetEpoch = OF_DATE;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];

			if (args[i].equalsIgnoreCase("-j2000"))
				targetEpoch = J2000;

			if (args[i].equalsIgnoreCase("-b1875"))
				targetEpoch = B1875;

			if (args[i].equalsIgnoreCase("-ofdate"))
				targetEpoch = OF_DATE;
		}

		if (filename == null || bodyname == null || startdate == null
				|| enddate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body name: \"" + bodyname + "\"");
			System.exit(1);
		}

		Date date = null;

		try {
			date = datefmtIn.parse(startdate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdstart = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		try {
			date = datefmtIn.parse(enddate);
		} catch (ParseException e) {
			System.err.println(
					"Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}

		double jdfinish = UNIX_EPOCH_AS_JD
				+ ((double) date.getTime()) / MILLISECONDS_PER_DAY;

		double jdstep = 0.0;

		jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

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

		ApparentPlace apTarget = new ApparentPlace(earth, planet, sun, erm);

		ApparentPlace apSun = (kBody == JPLEphemeris.SUN ? apTarget
				: new ApparentPlace(earth, sun, sun, erm));

		SimpleAlmanac almanac = new SimpleAlmanac(apTarget, apSun, targetEpoch);

		almanac.run(jdstart, jdfinish, jdstep);
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

	public void run(double jdstart, double jdfinish, double jdstep) {
		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				displayApparentPlace(t, System.out);
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

	}

	private void printAngle(double x, DecimalFormat formatDegrees,
			DecimalFormat formatMinutes, DecimalFormat formatSeconds,
			PrintStream ps, boolean hasSign) {
		char signum = (x < 0.0) ? '-' : '+';

		if (x < 0.0)
			x = -x;

		int xd = (int) x;

		x -= (double) xd;
		x *= 60.0;

		int xm = (int) x;

		x -= (double) xm;
		x *= 60.0;

		if (hasSign)
			ps.print(signum + " ");

		ps.print(formatDegrees.format(xd) + " " + formatMinutes.format(xm) + " "
				+ formatSeconds.format(x));
	}

	private void displayApparentPlace(double t, PrintStream ps)
			throws JPLEphemerisException {
		apTarget.calculateApparentPlace(t);

		double ra = 0.0, dec = 0.0, ra1875, dec1875;
		String epochName = null;

		Vector dc = (Vector) apTarget.getDirectionCosinesJ2000().clone();

		dc.multiplyBy(precessJ2000toB1875);

		ra1875 = Math.atan2(dc.getY(), dc.getX());

		while (ra1875 < 0.0)
			ra1875 += TWO_PI;

		double aux = Math.sqrt(dc.getX() * dc.getX() + dc.getY() * dc.getY());

		dec1875 = Math.atan2(dc.getZ(), aux);

		switch (targetEpoch) {
		case OF_DATE:
			ra = apTarget.getRightAscensionOfDate();
			dec = apTarget.getDeclinationOfDate();
			epochName = "OF_DATE";
			break;

		case J2000:
			ra = apTarget.getRightAscensionJ2000();
			dec = apTarget.getDeclinationJ2000();
			epochName = "J2000";
			break;

		case B1875:
			ra = ra1875;
			dec = dec1875;
			epochName = "B1875";
			break;
		}

		ra *= 12.0 / Math.PI;
		dec *= 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;

		ps.print(dfmtb.format(t));

		ps.print("  ");

		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

		long ticks = (long) dticks;

		Date date = new Date(ticks);

		ps.print("  " + datefmt.format(date));

		ps.print("  ");

		printAngle(ra, ifmta, ifmta, dfmta, ps, false);

		ps.print("  ");

		printAngle(dec, ifmta, ifmta, dfmtb, ps, true);

		ps.print("  ");

		ps.format("%10.7f", apTarget.getGeometricDistance());

		ps.print("  ");

		ps.format("%10.7f", apTarget.getLightPathDistance());

		String constellation = ConstellationFinder.getZone(ra1875, dec1875);

		ps.print("  " + constellation);

		ps.print("  " + epochName);

		if (targetIsPlanet()) {
			apSun.calculateApparentPlace(t);

			ra = apTarget.getRightAscensionJ2000();
			dec = apTarget.getDeclinationJ2000();

			double raSun = apSun.getRightAscensionJ2000();
			double decSun = apSun.getDeclinationJ2000();

			double elongation = calculateElongation(ra, dec, raSun, decSun);

			ps.printf("  %7.2f", elongation * 180.0 / PI);
			
			elongation = calculateEclipticElongation(ra, dec, raSun, decSun, t);

			ps.printf("  %7.2f", elongation * 180.0 / PI);

			double dEarthSun = apSun.getGeometricDistance();

			double dEarthPlanet = apTarget.getGeometricDistance();

			double dPlanetSun = calculatePlanetSunDistance(dEarthSun,
					dEarthPlanet, elongation);

			double phaseAngle = calculatePhaseAngle(dEarthSun, dEarthPlanet,
					dPlanetSun);

			ps.printf("  %6.2f", phaseAngle * 180.0 / PI);

			double phase = 0.5 * (1.0 + cos(phaseAngle));

			ps.printf("  %5.3f", phase);

			double magnitude = calculateMagnitude(dEarthPlanet, dPlanetSun,
					phaseAngle, t);

			ps.printf("  %5.2f", magnitude);

			double semiDiameter = calculateSemiDiameter(dEarthPlanet);

			ps.printf("  %5.2f", 2.0 * semiDiameter);
		}

		ps.println();
	}

	private double calculateElongation(double ra1, double dec1, double ra2,
			double dec2) {
		double x = sin(dec1) * sin(dec2)
				+ cos(dec1) * cos(dec2) * cos(ra1 - ra2);

		return acos(x);
	}
	
	private double calculateEclipticElongation(double ra1, double dec1, double ra2,
			double dec2, double t) {
		double longitude1 = calculateEclipticLongitude(ra1, dec1, t);
		double longitude2 = calculateEclipticLongitude(ra2, dec2, t);
		
		double elongation = longitude1 - longitude2;
		
		while (elongation < -PI)
			elongation += 2.0 * PI;
		
		while (elongation > PI)
			elongation -= 2.0 * PI;

		return elongation;
	}
	
	private double calculateEclipticLongitude(double ra, double dec, double t) {
		double xa = cos(ra) * cos(dec);
		double ya = sin(ra) * cos(dec);
		double za = sin(dec);
		
		double obliquity = erm.meanObliquity(t);
		
		double xe = xa;
		double ye = ya * cos(obliquity) + za * sin(obliquity);

		return atan2(ye, xe);
	}

	private double calculatePlanetSunDistance(double dEarthSun,
			double dEarthPlanet, double elongation) {
		double dSquared = dEarthSun * dEarthSun + dEarthPlanet * dEarthPlanet
				- 2.0 * dEarthSun * dEarthPlanet * cos(elongation);

		return sqrt(dSquared);
	}

	private double calculatePhaseAngle(double dEarthSun, double dEarthPlanet,
			double dPlanetSun) {
		double x = (dEarthPlanet * dEarthPlanet + dPlanetSun * dPlanetSun
				- dEarthSun * dEarthSun) / (2.0 * dEarthPlanet * dPlanetSun);

		return acos(x);
	}

	private double calculateMagnitude(double dEarthPlanet, double dPlanetSun,
			double phaseAngle, double t) throws JPLEphemerisException {
		double phaseDegrees = phaseAngle * 180.0 / PI;

		double distanceModulus = 5.0 * log10(dEarthPlanet * dPlanetSun);

		int targetCode = apTarget.getTarget().getBodyCode();

		double a = 0.0;

		switch (targetCode) {
		case JPLEphemeris.MERCURY:
			a = phaseDegrees / 100.0;
			return -0.42 + distanceModulus + 3.8 * a - 2.73 * a * a
					+ 2.0 * a * a * a;

		case JPLEphemeris.VENUS:
			a = phaseDegrees / 100.0;
			return -4.40 + distanceModulus + 0.09 * a + 2.39 * a * a
					- 0.65 * a * a * a;

		case JPLEphemeris.MARS:
			return -1.52 + distanceModulus + 0.016 * phaseDegrees;

		case JPLEphemeris.JUPITER:
			return -9.40 + distanceModulus + 0.005 * phaseDegrees;

		case JPLEphemeris.SATURN:
			return -8.88 + distanceModulus + saturnRingCorrection(t);

		case JPLEphemeris.URANUS:
			return -7.19 + distanceModulus + 0.002 * phaseDegrees;

		case JPLEphemeris.NEPTUNE:
			return -6.87 + distanceModulus;

		case JPLEphemeris.PLUTO:
			return -1.0 + distanceModulus;

		default:
			throw new IllegalStateException(
					"Cannot calculate a magnitude for the target body.");
		}
	}

	private double saturnRingCorrection(double t) throws JPLEphemerisException {
		final double R = PI / 180.0;

		double ra = apTarget.getRightAscensionOfDate();
		double dec = apTarget.getDeclinationOfDate();

		double[] eclipticCoordinates = getHeliocentricEclipticCoordinates(t);

		double hlong = eclipticCoordinates[0];
		double hlat = eclipticCoordinates[1];

		// Julian centuries since 1900
		double tau = (t - 2415020.0) / 36525.0;

		// Ring orientation angles
		double node = (168.11780 + 1.39352 * tau + 0.00041 * tau * tau) * R;
		double incl = (28.07443 - 0.01302 * tau) * R;
		double nn = (126.35863 + 3.99712 * tau + 0.23542 * tau * tau) * R;
		double jj = (6.91086 - 0.44892 * tau + 0.01291 * tau * tau) * R;
		double omega = (42.92442 - 2.73981 * tau - 0.23517 * tau * tau) * R;
		double dln = (hlong - node);

		double udash = atan2(
				sin(incl) * sin(hlat) + cos(incl) * cos(hlat) * sin(dln),
				cos(hlat) * cos(dln));

		if (udash < -PI)
			udash += 2.0 * PI;

		double dran = ra - nn;

		double u = atan2(sin(jj) * sin(dec) + cos(jj) * cos(dec) * sin(dran),
				cos(dec) * cos(dran));

		if (u < -PI)
			u += 2.0 * PI;

		double sinb = -cos(jj) * sin(dec) + sin(jj) * cos(dec) * sin(dran);

		double udwu = udash + omega - u;

		while (udwu > PI)
			udwu = udwu - 2.0 * PI;

		while (udwu < -PI)
			udwu = udwu + 2.0 * PI;

		return 0.044 * abs(udwu / R) - 2.60 * abs(sinb) + 1.25 * sinb * sinb;
	}

	private double[] getHeliocentricEclipticCoordinates(double t)
			throws JPLEphemerisException {
		MovingPoint mp = apTarget.getTarget();

		Vector pos = mp.getPosition(t);

		MovingPoint mps = apSun.getTarget();

		Vector posSun = mps.getPosition(t);

		pos.subtract(posSun);

		Matrix precess = erm.precessionMatrix(mp.getEpoch(), t);

		pos.multiplyBy(precess);

		double obliquity = erm.meanObliquity(t);

		double xa = pos.getX();
		double ya = pos.getY();
		double za = pos.getZ();

		double ce = cos(obliquity);
		double se = sin(obliquity);

		double xe = xa;
		double ye = ya * ce + za * se;
		double ze = -ya * se + za * ce;

		double[] coords = new double[2];

		coords[0] = atan2(ye, xe);
		coords[1] = atan2(ze, sqrt(xe * xe + ye * ye));

		return coords;
	}

	private double calculateSemiDiameter(double d) {
		int targetCode = apTarget.getTarget().getBodyCode();

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

		default:
			throw new IllegalStateException(
					"Cannot calculate a magnitude for the target body.");
		}

	}

	private boolean targetIsPlanet() {
		int targetCode = apTarget.getTarget().getBodyCode();

		switch (targetCode) {
		case JPLEphemeris.MERCURY:
		case JPLEphemeris.VENUS:
		case JPLEphemeris.MARS:
		case JPLEphemeris.JUPITER:
		case JPLEphemeris.SATURN:
		case JPLEphemeris.URANUS:
		case JPLEphemeris.NEPTUNE:
		case JPLEphemeris.PLUTO:
			return true;

		default:
			return false;
		}
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-body\t\tName of body",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"OPTIONAL PARAMETERS",
				"\t-step\t\tStep size (days)",
				"\t-j2000\tCalculate position for epoch J2000",
				"\t-b1875\tCalculate position for epoch B1875",
				"\t-ofdate\tCalculate position for epoch of date (this is the default",
				"",
				"OUTPUT COLUMNS",
				"\tJulian Day Number",
				"\tDate and time",
				"\tRight Ascension",
				"\tDeclination",
				"\tGeometric distance",
				"\tLight-path distance",
				"\tConstellation",
				"\tEpoch of Right Ascension and Declination",
				"[For planets only]",
				"\tElongation",
				"\tElongation in ecliptic longitude",
				"\tPhase angle",
				"\tIlluminated fraction",
				"\tApparent magnitude",
				"\tApparent diameter of disk"
		};
		
		for (String line : lines)
			System.err.println(line);
	}

}
