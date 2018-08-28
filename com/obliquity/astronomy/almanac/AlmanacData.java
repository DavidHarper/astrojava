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

package com.obliquity.astronomy.almanac;

import static java.lang.Math.*;

public class AlmanacData {
	public double julianDate = Double.NaN;
	public double rightAscension= Double.NaN, declination = Double.NaN;
	public double geometricDistance = Double.NaN, lightPathDistance = Double.NaN, heliocentricDistance = Double.NaN;
	public double elongation = Double.NaN, eclipticElongation = Double.NaN;
	public double phaseAngle = Double.NaN, illuminatedFraction = Double.NaN;
	public double magnitude = Double.NaN, semiDiameter = Double.NaN;
	public double positionAngleOfBrightLimb = Double.NaN;
	public double eclipticLongitude = Double.NaN, eclipticLatitude = Double.NaN;
	public SaturnRingAngles saturnRingAnglesForEarth = null;
	public SaturnRingAngles saturnRingAnglesForSun = null;
	
	public static final int OF_DATE = 0, J2000 = 1, B1875 = 2;
	
	private static final double MOON_RADIUS = 1738.0;
	private static final double SUN_RADIUS = 696000.0;
	
	private static double AU = Double.NaN;
	
	static Matrix precessJ2000toB1875;
	
	static {
		IAUEarthRotationModel erm = new IAUEarthRotationModel();
		
		double epochJ2000 = erm.JulianEpoch(2000.0);
		double epochB1875 = erm.BesselianEpoch(1875.0);
		
		precessJ2000toB1875 = new Matrix();
		
		erm.precessionMatrix(epochJ2000, epochB1875, precessJ2000toB1875);
	}

	public static AlmanacData calculateAlmanacData(ApparentPlace apTarget, ApparentPlace apSun, double t, int targetEpoch, AlmanacData data)
			throws JPLEphemerisException {
		apTarget.calculateApparentPlace(t);

		double ra = 0.0, dec = 0.0;

		switch (targetEpoch) {
		case OF_DATE:
			ra = apTarget.getRightAscensionOfDate();
			dec = apTarget.getDeclinationOfDate();
			break;

		case J2000:
			ra = apTarget.getRightAscensionJ2000();
			dec = apTarget.getDeclinationJ2000();
			break;

		case B1875:		
			Vector dc = (Vector) apTarget.getDirectionCosinesJ2000().clone();

			dc.multiplyBy(precessJ2000toB1875);

			ra = Math.atan2(dc.getY(), dc.getX());

			while (ra < 0.0)
				ra += 2.0 * PI;

			double aux = Math.sqrt(dc.getX() * dc.getX() + dc.getY() * dc.getY());

			dec = Math.atan2(dc.getZ(), aux);
			break;
		}
		
		data.julianDate = t;
		
		// Required later for conversion to ecliptic longitude and latitude
		double xa = cos(ra) * cos(dec);
		double ya = sin(ra) * cos(dec);
		double za = sin(dec);

		ra *= 12.0 / Math.PI;
		dec *= 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;
		
		data.rightAscension = ra;
		data.declination = dec;
		
		data.geometricDistance = apTarget.getGeometricDistance();
		data.lightPathDistance = apTarget.getLightPathDistance();
		data.heliocentricDistance = apTarget.getHeliocentricDistance();
		
		int iBody = apTarget.getTarget().getBodyCode();
			
		double dEarthPlanet = apTarget.getGeometricDistance();
		
		AU = apTarget.getTarget().getEphemeris().getAU();

		data.semiDiameter = calculateSemiDiameter(iBody, dEarthPlanet);
		
		IAUEarthRotationModel erm = (IAUEarthRotationModel)apTarget.getEarthRotationModel();
		
		double epochAsJD = Double.NaN;
		
		switch (targetEpoch) {
		case J2000:
			epochAsJD = erm.JulianEpoch(2000.0);
			break;
			
		case B1875:
			epochAsJD = erm.BesselianEpoch(1875.0);
			break;
			
		default:
			epochAsJD = t;
			break;
		}

		double obliquity = erm.meanObliquity(epochAsJD);
		
		double ce = cos(obliquity);		
		double se = sin(obliquity);
		
		double xe = xa;
		double ye = ce * ya + se * za;
		double ze = -se * ya + ce * za;
		
		double lambda = atan2(ye, xe);
		double beta = asin(ze);
		
		lambda *= 180.0/PI;
		
		if (lambda < 0.0)
			lambda += 360.0;
		
		data.eclipticLongitude = lambda;
		
		data.eclipticLatitude = beta * 180.0/PI;

		if (iBody != JPLEphemeris.SUN) {
			apSun.calculateApparentPlace(t);

			ra = apTarget.getRightAscensionOfDate();
			dec = apTarget.getDeclinationOfDate();

			double raSun = apSun.getRightAscensionOfDate();
			double decSun = apSun.getDeclinationOfDate();

			data.elongation = 180.0/PI * calculateElongation(ra, dec, raSun, decSun);
			
			double eclipticElongation = calculateEclipticElongation(ra, dec, raSun, decSun, obliquity);
			
			data.eclipticElongation = 180.0/PI * eclipticElongation;

			double dEarthSun = apSun.getGeometricDistance();

			double dPlanetSun = calculatePlanetSunDistance(dEarthSun,
					dEarthPlanet, eclipticElongation);

			double phaseAngle = calculatePhaseAngle(dEarthSun, dEarthPlanet,
					dPlanetSun);

			data.phaseAngle = phaseAngle * 180.0 / PI;

			data.illuminatedFraction = 0.5 * (1.0 + cos(phaseAngle));
			
			double y = cos(decSun) * sin(raSun - ra);
			double x = sin(decSun) * cos(dec) - cos(decSun) * sin(dec) * cos(raSun - ra);
			
			data.positionAngleOfBrightLimb = atan2(y, x);

			data.magnitude = iBody != JPLEphemeris.MOON ? calculateMagnitude(iBody, dEarthPlanet, dPlanetSun,
					phaseAngle, t) : 0.0;
			
			if (iBody == JPLEphemeris.SATURN)
				data.magnitude += saturnRingCorrection(apTarget, apSun, t);
			
			if (iBody == JPLEphemeris.SATURN)
				data.saturnRingAnglesForEarth = calculateSaturnRingAnglesForEarth(apTarget, t);
			
			if (iBody == JPLEphemeris.SATURN)
				data.saturnRingAnglesForSun = calculateSaturnRingAnglesForSun(apTarget, apSun, t);
		}

		return data;
	}
	
	private static double calculateElongation(double ra1, double dec1, double ra2,
			double dec2) {
		double x = sin(dec1) * sin(dec2)
				+ cos(dec1) * cos(dec2) * cos(ra1 - ra2);

		return acos(x);
	}
	
	private static double calculateEclipticElongation(double ra1, double dec1, double ra2,
			double dec2, double obliquity) {
		double longitude1 = calculateEclipticLongitude(ra1, dec1, obliquity);
		double longitude2 = calculateEclipticLongitude(ra2, dec2, obliquity);
		
		double elongation = longitude1 - longitude2;
		
		while (elongation < -PI)
			elongation += 2.0 * PI;
		
		while (elongation > PI)
			elongation -= 2.0 * PI;

		return elongation;
	}
	
	private static double calculateEclipticLongitude(double ra, double dec, double obliquity) {
		double xa = cos(ra) * cos(dec);
		double ya = sin(ra) * cos(dec);
		double za = sin(dec);
		
		double xe = xa;
		double ye = ya * cos(obliquity) + za * sin(obliquity);

		return atan2(ye, xe);
	}

	private static double calculatePlanetSunDistance(double dEarthSun,
			double dEarthPlanet, double elongation) {
		double dSquared = dEarthSun * dEarthSun + dEarthPlanet * dEarthPlanet
				- 2.0 * dEarthSun * dEarthPlanet * cos(elongation);

		return sqrt(dSquared);
	}

	private static double calculatePhaseAngle(double dEarthSun, double dEarthPlanet,
			double dPlanetSun) {
		double x = (dEarthPlanet * dEarthPlanet + dPlanetSun * dPlanetSun
				- dEarthSun * dEarthSun) / (2.0 * dEarthPlanet * dPlanetSun);

		return acos(x);
	}

	private static double calculateMagnitude(int targetCode, double dEarthPlanet, double dPlanetSun,
			double phaseAngle, double t) throws JPLEphemerisException {
		double phaseDegrees = phaseAngle * 180.0 / PI;

		double distanceModulus = 5.0 * log10(dEarthPlanet * dPlanetSun);

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
			return -8.88 + distanceModulus;

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
	
	// Saturn pole coordinates from Davies, M.E. et al. (1989) Celes. Mech. 46, 187
	
	private static double saturnPoleRightAscension(double tau) {
		return (40.58 - 0.036 * tau) * PI/180.0;
	}
	
	private static double saturnPoleDeclination(double tau) {
		return (83.54 - 0.004 * tau) * PI/180.0;
	}
			
	private static SaturnRingAngles calculateSaturnRingAnglesForEarth(ApparentPlace apTarget, double t) {
		double tau = (t - 2451545.0)/36525.0;
		
		double raSaturnPole = saturnPoleRightAscension(tau);
		double decSaturnPole = saturnPoleDeclination(tau);
		
		double J = 0.5 * PI - decSaturnPole;
		double N = raSaturnPole + 0.5 * PI;
		
		double alpha = apTarget.getRightAscensionJ2000();
		double delta = apTarget.getDeclinationJ2000();
		
		double phi = alpha - N;
		
		double p1 = cos(J) * cos(delta) * sin(phi) + sin(J) * sin(delta);
		double p2 = cos(delta) * cos(phi);
		double p3 = sin(J) * cos(delta) * sin(phi) - cos(J) * sin(delta);
		double p4 = -sin(J) * cos(phi);
		double p5 = sin(J) * sin(delta) * sin(phi) + cos(J) * cos(delta);
		
		SaturnRingAngles sra = new SaturnRingAngles();
		
		sra.B = asin(p3) * 180.0/PI;
		
		sra.U = atan2(p1, p2) * 180.0/PI;
		
		sra.P = atan2(p4, p5) * 180.0/PI;
		
		return sra;
	}
	
	private static SaturnRingAngles calculateSaturnRingAnglesForSun(ApparentPlace apTarget, ApparentPlace apSun, double t)
			throws JPLEphemerisException {
		double lightTime = apTarget.getHeliocentricDistance()/ApparentPlace.SPEED_OF_LIGHT;

		double[] eclipticCoordinates = getHeliocentricEclipticCoordinates(apTarget, apSun, t - lightTime);
		double hlong = eclipticCoordinates[0];
		double hlat = eclipticCoordinates[1];
		
		// Julian centuries since 2000
		double tau = (t - 2451545.0) / 36525.0;
		
		double raSaturnPole = saturnPoleRightAscension(tau);
		double decSaturnPole = saturnPoleDeclination(tau);

		double xp = cos(decSaturnPole) * cos(raSaturnPole);
		double yp = cos(decSaturnPole) * sin(raSaturnPole);
		double zp = sin(decSaturnPole);
		
		double eps = apTarget.getEarthRotationModel().meanObliquity(t);
		
		double xpe = xp;
		double ype = yp * cos(eps) + zp * sin(eps);
		double zpe = -yp * sin(eps) + zp * cos(eps);
		
		double lngSaturnPole = atan2(ype, xpe);
		double latSaturnPole = asin(zpe);

		// Ring orientation angles
		double node = lngSaturnPole + 0.5 * PI;
		double incl = 0.5 * PI - latSaturnPole;
		
		// Apply approximate correction for precession in longitude
		node += (5029.0966 * tau/3600.0) * PI/180.0;

		double p1 = -sin(incl) * cos(hlong - node);
		double p2 = cos(incl) * cos(hlat) + sin(incl) * sin(hlat) * sin(hlong - node);
		double p3 = -cos(incl) * sin(hlat) + sin(incl) * cos(hlat) * sin(hlong - node);
		double p4 = sin(incl) * sin(hlat) + cos(incl) * cos(hlat) * sin(hlong - node);
		double p5 = cos(hlat) * cos(hlong - node);
		
		SaturnRingAngles sra = new SaturnRingAngles();
		
		sra.B = asin(p3) * 180.0/PI;
		
		sra.U = atan2(p4, p5) * 180.0/PI;
		
		sra.P = atan2(p1, p2) * 180.0/PI;
		
		return sra;
	}
	
	// Ring node and inclination from Meeus, Astronomical Algorithms, 1991, p302
	
	private static double saturnRingNodeLongitude(double tau) {
		return (169.508470 + tau * (1.394681 + tau * 0.000412)) * PI/180.0;
	}
	
	private static double saturnRingInclination(double tau) {
		return (28.075216 - tau * (0.012998 - tau * 0.000004)) * PI/180.0;
	}

	private static double saturnRingCorrection(ApparentPlace apTarget, ApparentPlace apSun, double t) throws JPLEphemerisException {
		final double R = PI / 180.0;

		double ra = apTarget.getRightAscensionOfDate();
		double dec = apTarget.getDeclinationOfDate();

		double[] eclipticCoordinates = getHeliocentricEclipticCoordinates(apTarget, apSun, t);

		double hlong = eclipticCoordinates[0];
		double hlat = eclipticCoordinates[1];

		// Julian centuries since 1900
		double tau = (t - 2451545.0) / 36525.0;

		// Ring orientation angles
		double node = saturnRingNodeLongitude(tau);
		double incl = saturnRingInclination(tau);

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

	private static double[] getHeliocentricEclipticCoordinates(ApparentPlace apTarget, ApparentPlace apSun, double t)
			throws JPLEphemerisException {
		MovingPoint mp = apTarget.getTarget();

		Vector pos = mp.getPosition(t);

		MovingPoint mps = apSun.getTarget();

		Vector posSun = mps.getPosition(t);

		pos.subtract(posSun);
		
		EarthRotationModel erm = apTarget.getEarthRotationModel();

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

	private static double calculateSemiDiameter(int targetCode, double d) {
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

}
