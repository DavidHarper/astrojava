package com.obliquity.astronomy.test;

import com.obliquity.astronomy.*;
import java.io.*;

public class RiseSetTest {
	public static final double TWOPI = 2.0 * Math.PI;
	
	public static void main(String[] args) {
		final int NEVENTS = 6;

		String filename = null;
		String bodyname = null;
		String startdate = null;
		String longitude = null;
		String latitude = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];
		}

		if (filename == null || bodyname == null || startdate == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body name: \"" + bodyname + "\"");
			System.exit(1);
		}

		double jdstart = Double.parseDouble(startdate);
		double jdfinish = jdstart + 1.0;

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

		RiseSetEvent rse[] = new RiseSetEvent[NEVENTS];

		int rc = calculateRiseSetTime(ap, place, jdstart,
				RiseSetEvent.RISE_SET, RiseSetEvent.UPPER_LIMB, rse);

		System.out.println(rc);
	}

	private static int calculateRiseSetTime(ApparentPlace ap, Place place,
			double jdstart, int type, int limb, RiseSetEvent[] rse) {
		MovingPoint observer = ap.getObserver();
		MovingPoint target = ap.getTarget();

		if (!(observer instanceof EarthCentre)) {
			return RiseSetEvent.INVALID_ARGUMENT;
		}

		int body = target.getBodyCode();

		int nEvents = 0;
		
		double ghapoly[] = new double[3];
		double decpoly[] = new double[3];
		double hppoly[] = new double[3];
		
		calculatePolynomialApproximation(ap, jdstart, ghapoly, decpoly, hppoly);
		
		return nEvents;
	}

	private static void calculatePolynomialApproximation(ApparentPlace ap,
			double t0, double ghapoly[], double decpoly[], double hppoly[]) {
		double gha[] = new double[3];
		double dec[] = new double[3];
		double hp[] = new double[3];
		double earthRadius = 6378137.0/ap.getObserver().getEphemeris().getAU();
		boolean isMoon = ap.getTarget().getBodyCode() == JPLEphemeris.MOON;
		
		for (int i = 0; i < 3; i++) {
			double t = t0 + 0.5 * (double)i;
			
			try {
				ap.calculateApparentPlace(t);
			}
			catch (JPLEphemerisException jee) {
			}
			
			double gast = ap.getEarthRotationModel().greenwichApparentSiderealTime(t);
			
			gha[i] = (gast - ap.getRightAscension()) % TWOPI;
			
			if (gha[i] < 0.0)
				gha[i] += TWOPI;
			
			dec[i] = ap.getDeclination();
			
			if (isMoon) {
				hp[i] = Math.asin(earthRadius/ap.getGeometricDistance());
			}
		}
		
		while (gha[1] < gha[0])
			gha[1] += TWOPI;
		
		while (gha[2] < gha[1])
			gha[2] += TWOPI;
		
		calculatePolynomialCoefficients(gha, ghapoly);
		calculatePolynomialCoefficients(dec, decpoly);
		
		if (isMoon)
			calculatePolynomialCoefficients(hp, hppoly);
	}
	
	private static void calculatePolynomialCoefficients(double[] values, double[] coeffs) {
		coeffs[0] = values[0];
		coeffs[1] = -values[2] + 4.0 * values[1] - 3.0 * values[0];
		coeffs[2] = 2.0 * values[2] - 4.0 * values[1] + 2.0 * values[0];
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
