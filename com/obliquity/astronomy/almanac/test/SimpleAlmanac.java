package com.obliquity.astronomy.almanac.test;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class SimpleAlmanac {
	private final DecimalFormat dfmta = new DecimalFormat("00.000");
	private final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private final DecimalFormat ifmta = new DecimalFormat("00");
	private final DecimalFormat dfmtc = new DecimalFormat("0.0000000");
	private final SimpleDateFormat datefmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private ApparentPlace ap;
	private SimplePrecession sp;
	
	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;
	
	public SimpleAlmanac(ApparentPlace ap) {
		this.ap = ap;
		
		datefmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		double B1875 = SimplePrecession.yearToBesselianEpoch(1875.0);
		
		sp = new SimplePrecession(SimplePrecession.J2000, B1875);
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		boolean useJ2000 = false;

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
				useJ2000 = true;
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
			System.err.println("Failed to parse \"" + startdate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdstart = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;
		
		try {
			date = datefmtIn.parse(enddate);
		} catch (ParseException e) {
			System.err.println("Failed to parse \"" + enddate + "\" as an ISO date");
			e.printStackTrace();
			System.exit(1);
		}
		
		double jdfinish = UNIX_EPOCH_AS_JD + ((double)date.getTime())/MILLISECONDS_PER_DAY;

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

		EarthRotationModel erm = useJ2000 ? null : new IAUEarthRotationModel();

		ApparentPlace ap = new ApparentPlace(earth, planet, sun, erm);
		
		SimpleAlmanac almanac = new SimpleAlmanac(ap);
		
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
	
	private void printAngle(double x, DecimalFormat formatDegrees, DecimalFormat formatMinutes, DecimalFormat formatSeconds,
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
		
		ps.print(formatDegrees.format(xd) + " " + formatMinutes.format(xm) + " " + formatSeconds.format(x));
	}
	
	private void displayApparentPlace(double t, PrintStream ps) throws JPLEphemerisException {
		ap.calculateApparentPlace(t);

		double ra = ap.getRightAscension();
		double dec = ap.getDeclination();
		
		double posB1875[] = {ra, dec};
		
		sp.precess(posB1875);

		ra *= 12.0 / Math.PI;
		dec *= 180.0 / Math.PI;

		if (ra < 0.0)
			ra += 24.0;

		ps.print(dfmtb.format(t));
		
		ps.print("  ");
		
		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);
		
		long ticks = (long)dticks;
		
		Date date = new Date(ticks);
		
		ps.print("  " + datefmt.format(date));
		
		ps.print("  ");
		
		printAngle(ra, ifmta, ifmta, dfmta, ps, false);
		
		ps.print("  ");
		
		printAngle(dec, ifmta, ifmta, dfmtb, ps, true);
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getGeometricDistance()));
		
		ps.print("  ");
		
		ps.print(dfmtc.format(ap.getLightPathDistance()));
		
		double ra1875 = posB1875[0] * 12.0/Math.PI;
		
		while (ra1875 < 0.0)
			ra1875 += 24.0;
		
		double dec1875 = posB1875[1] * 180.0/Math.PI;
		
		String constellation = ConstellationFinder.getZone(ra1875, dec1875);
		
		ps.print("  " + constellation);
		
		ps.print("  ");
		
		printAngle(ra1875, ifmta, ifmta, dfmta, ps, false);
		
		ps.print("  ");
		
		printAngle(dec1875, ifmta, ifmta, dfmtb, ps, true);
		
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
