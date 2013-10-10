package com.obliquity.astronomy.test;

import com.obliquity.astronomy.*;

import java.io.*;
import java.text.*;
import java.util.Date;
import java.util.TimeZone;

public class TestApparentPlace {
	private static final DecimalFormat dfmta = new DecimalFormat("00.000");
	private static final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private static final DecimalFormat ifmt = new DecimalFormat("00");
	private static final DecimalFormat dfmtc = new DecimalFormat("0.0000000");
	
	private static final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	public static void main(String args[]) {
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;

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
			date = datefmt.parse(startdate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = datefmt.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;

		double jdstep = 0.0;

		jdstep = (stepsize == null) ? 1.0 : Double.parseDouble(stepsize);

		boolean timingTest = Boolean.getBoolean("timingtest");

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

		long startTime = System.currentTimeMillis();
		int nSteps = 0;

		ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);

		try {
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ap.calculateApparentPlace(t);
				if (!timingTest)
					displayApparentPlace(t, ap, System.out);
				nSteps++;
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}

		long duration = System.currentTimeMillis() - startTime;
		
		double speed = (double)duration/(double)nSteps;
	
		int rate = (int)(1000.0/speed);
		
		speed *= 1000.0;

		System.err.println("Executed " + nSteps + " steps in " + duration
				+ " ms --> " + dfmtb.format(speed) + " \u03bcs/step or " + rate + " steps per second");
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

	private static void displayApparentPlace(double t, ApparentPlace ap,
			PrintStream ps) {
		double ra = ap.getRightAscension() * 12.0 / Math.PI;
		double dec = ap.getDeclination() * 180.0 / Math.PI;

		char decsign = (dec < 0.0) ? 'S' : 'N';

		if (ra < 0.0)
			ra += 24.0;

		if (dec < 0.0)
			dec = -dec;

		int rah = (int) ra;
		ra -= (double) rah;
		ra *= 60.0;
		int ram = (int) ra;
		ra -= (double) ram;
		ra *= 60.0;

		int decd = (int) dec;
		dec -= (double) decd;
		dec *= 60.0;
		int decm = (int) dec;
		dec -= (double) decm;
		dec *= 60.0;

		ps.print(dfmtb.format(t));
		ps.print("  ");
		ps.print(ifmt.format(rah) + " " + ifmt.format(ram) + " "
				+ dfmta.format(ra));
		ps.print("  ");
		ps.print(decsign + " " + ifmt.format(decd) + " " + ifmt.format(decm)
				+ " " + dfmtb.format(dec));
		ps.print("  ");
		ps.print(dfmtc.format(ap.getGeometricDistance()));
		ps.print("  ");
		ps.print(dfmtc.format(ap.getLightPathDistance()));
		ps.println();
	}

	public static void showUsage() {
		System.err.println("MANDATORY PARAMETERS");
		System.err.println("\t-ephemeris\tName of ephemeris file");
		System.err.println("\t-body\t\tName of body");
		System.err.println("\t-startdate\tStart date");
		System.err.println("\t-enddate\tEnd date");
		System.err.println();
		System.err.println("OPTIONAL PARAMETERS");
		System.err.println("\t-step\t\tStep size (days)");
	}
}
