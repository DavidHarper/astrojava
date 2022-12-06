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
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.obliquity.astronomy.almanac.AlmanacData;
import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Matrix;
import com.obliquity.astronomy.almanac.MoonCentre;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.Place;
import com.obliquity.astronomy.almanac.PlanetCentre;
import com.obliquity.astronomy.almanac.TerrestrialObserver;
import com.obliquity.astronomy.almanac.Vector;
import com.obliquity.astronomy.almanac.saturnpole.DourneauSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.HarperTaylorSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.IAU1989SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.Jacobson2007SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.StruveSaturnPoleModel;

public class SimpleAlmanac {	
	private class ExtendedAlmanacData extends AlmanacData {
		public String constellation;
		public Date date;
		public int epoch;
	}

	private final DecimalFormat dfmta = new DecimalFormat("00.000");
	private final DecimalFormat dfmtb = new DecimalFormat("00.00");
	private final DecimalFormat ifmta = new DecimalFormat("00");
	private final SimpleDateFormat datefmt = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private ApparentPlace apTarget, apSun;
	private IAUEarthRotationModel erm = null;
	private Matrix precessJ2000toB1875 = null;
	private int targetEpoch;

	private static final SimpleDateFormat datefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd");

	private static final SimpleDateFormat datetimefmtIn = new SimpleDateFormat(
			"yyyy-MM-dd/HH:mm");

	private static final double UNIX_EPOCH_AS_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 1000.0 * 86400.0;

	private static final double TWO_PI = 2.0 * Math.PI;
	
	private boolean elongationDeltas = false;

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
	
	public void setElongationDeltas(boolean elongationDeltas) {
		this.elongationDeltas = elongationDeltas;
	}

	public static void main(String args[]) {
		datefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));
		datetimefmtIn.setTimeZone(TimeZone.getTimeZone("GMT"));

		String filename = null;
		String bodyname = null;
		String startdate = null;
		String enddate = null;
		String stepsize = null;
		String poleModelName = null;
		String longitude = null;
		String latitude = null;

		int targetEpoch = AlmanacData.TRUE_OF_DATE;
		boolean elongationDeltas = false;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-ephemeris"))
				filename = args[++i];

			if (args[i].equalsIgnoreCase("-body"))
				bodyname = args[++i];

			if (args[i].equalsIgnoreCase("-startdate"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-date"))
				startdate = args[++i];

			if (args[i].equalsIgnoreCase("-enddate"))
				enddate = args[++i];

			if (args[i].equalsIgnoreCase("-step"))
				stepsize = args[++i];

			if (args[i].equalsIgnoreCase("-j2000"))
				targetEpoch = AlmanacData.J2000;

			if (args[i].equalsIgnoreCase("-b1875"))
				targetEpoch = AlmanacData.B1875;

			if (args[i].equalsIgnoreCase("-true"))
				targetEpoch = AlmanacData.TRUE_OF_DATE;
			
			if (args[i].equalsIgnoreCase("-mean"))
				targetEpoch = AlmanacData.MEAN_OF_DATE;

			if (args[i].equalsIgnoreCase("-elongationdeltas"))
				elongationDeltas = true;
			
			if (args[i].equalsIgnoreCase("-saturnpolemodel"))
				poleModelName = args[++i];

			if (args[i].equalsIgnoreCase("-latitude"))
				latitude = args[++i];

			if (args[i].equalsIgnoreCase("-longitude"))
				longitude = args[++i];

		}

		if (filename == null || bodyname == null) {
			showUsage();
			System.exit(1);
		}

		int kBody = parseBody(bodyname);

		if (kBody < 0) {
			System.err.println("Unknown body name: \"" + bodyname + "\"");
			System.exit(1);
		}
		
		if (poleModelName != null) {
			SaturnPoleModel poleModel = parsePoleModel(poleModelName);
			
			AlmanacData.setSaturnPoleModel(poleModel);
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

		double jdstep = 0.0;

		jdstep = (stepsize == null) ? 1.0 : parseStepSize(stepsize);

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

		ApparentPlace apTarget = new ApparentPlace(earth, planet, sun, erm);

		ApparentPlace apSun = (kBody == JPLEphemeris.SUN ? apTarget
				: new ApparentPlace(earth, sun, sun, erm));

		SimpleAlmanac almanac = new SimpleAlmanac(apTarget, apSun, targetEpoch);
		
		almanac.setElongationDeltas(elongationDeltas);
		
		PrintStream ps = Boolean.getBoolean("silent") ? null : System.out;

		almanac.run(jdstart, jdfinish, jdstep, ps);
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
	
	private static SaturnPoleModel parsePoleModel(String poleModelName) {
		if (poleModelName == null)
			return null;
		
		switch (poleModelName.toLowerCase()) {
		case "dourneau":
			return new DourneauSaturnPoleModel();
			
		case "harpertaylor":
			return new HarperTaylorSaturnPoleModel();
			
		case "iau1989":
			return new IAU1989SaturnPoleModel();
			
		case "jacobson2007":
			return new Jacobson2007SaturnPoleModel();
			
		case "struve":
			return new StruveSaturnPoleModel();
			
		default:
			return null;
		}
	}

	public void run(double jdstart, double jdfinish, double jdstep, PrintStream ps) {
		try {
			ExtendedAlmanacData lastData = null;
			
			for (double t = jdstart; t <= jdfinish; t += jdstep) {
				ExtendedAlmanacData data = calculateAlmanacData(t);
				
				if (elongationDeltas && lastData != null)
					displayElongationDelta(lastData, data, ps);
				
				lastData = data;
				
				displayApparentPlace(data, ps);
			}
		} catch (JPLEphemerisException jplee) {
			jplee.printStackTrace();
		}
	}
	
	private void displayElongationDelta(AlmanacData lastData, AlmanacData thisData, PrintStream ps) {
		if (ps == null)
			return;
		
		double delta = thisData.elongation - lastData.elongation;
		
		ps.printf("#%99s   %11.7f\n", " ", delta);
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

	private ExtendedAlmanacData calculateAlmanacData(double t)
			throws JPLEphemerisException {
		ExtendedAlmanacData data = new ExtendedAlmanacData();
		
		data.epoch = targetEpoch;
		
		AlmanacData.calculateAlmanacData(apTarget, apSun, t, targetEpoch, data);
		
		apTarget.calculateApparentPlace(t);

		double ra1875, dec1875;

		Vector dc = (Vector) apTarget.getDirectionCosinesJ2000().clone();

		dc.multiplyBy(precessJ2000toB1875);

		ra1875 = Math.atan2(dc.getY(), dc.getX());

		while (ra1875 < 0.0)
			ra1875 += TWO_PI;

		double aux = Math.sqrt(dc.getX() * dc.getX() + dc.getY() * dc.getY());

		dec1875 = Math.atan2(dc.getZ(), aux);

		double dticks = MILLISECONDS_PER_DAY * (t - UNIX_EPOCH_AS_JD);

		long ticks = (long) dticks;

		data.date = new Date(ticks);
			
		data.constellation = ConstellationFinder.getZone(ra1875, dec1875);
		
		return data;
	}

	private void displayApparentPlace(ExtendedAlmanacData data, PrintStream ps) {
		if (ps == null)
			return;
		
		ps.format("%13.5f", data.julianDate);

		ps.print("  ");

		ps.print("  " + datefmt.format(data.date));

		ps.print("  ");

		printAngle(data.rightAscension, ifmta, ifmta, dfmta, ps, false);

		ps.print("  ");

		printAngle(data.declination, ifmta, ifmta, dfmtb, ps, true);

		ps.print("  ");

		ps.format("%10.7f", data.geometricDistance);

		ps.print("  ");

		ps.format("%10.7f", data.lightPathDistance);

		ps.print("  ");

		ps.format("%10.7f", data.heliocentricDistance);

		ps.print("  " + data.constellation);

		String epochName = null;
		
		switch (data.epoch) {
		case AlmanacData.TRUE_OF_DATE:
			epochName = " TRUE";
			break;
			
		case AlmanacData.MEAN_OF_DATE:
			epochName = " MEAN";
			break;

		case AlmanacData.J2000:
			epochName = "J2000";
			break;

		case AlmanacData.B1875:
			epochName = "B1875";
			break;
		}
		
		ps.print("  " + epochName);

		ps.printf("  %5.2f", 2.0 * data.semiDiameter);

		if (targetIsPlanet()) {
			ps.printf("  %7.2f", data.elongation);

			ps.printf("  %7.2f", data.eclipticElongation);

			ps.printf("  %6.2f", data.phaseAngle);

			ps.printf("  %5.3f", data.illuminatedFraction);

			ps.printf("  %5.2f", data.magnitude);
			
			ps.printf("  %8.4f  %8.4f", data.eclipticLongitude, data.eclipticLatitude);
			
			if (data.saturnRingAnglesForEarth != null)
				ps.printf("  %8.5f", data.saturnRingAnglesForEarth.B);
			
			if (data.saturnRingAnglesForSun != null)
				ps.printf("  %8.5f", data.saturnRingAnglesForSun.B);
		} else if (targetIsSun()) {
			ps.printf("  %8.4f  %8.4f", data.eclipticLongitude, data.eclipticLatitude);
		}

		ps.println();
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
		case JPLEphemeris.MOON:
			return true;

		default:
			return false;
		}
	}
	
	private boolean targetIsSun() {
		return apTarget.getTarget().getBodyCode() == JPLEphemeris.SUN;
	}

	public static void showUsage() {
		String[] lines = { "MANDATORY PARAMETERS",
				"\t-ephemeris\tName of ephemeris file",
				"\t-body\t\tName of body",
				"",
				"OPTIONAL PARAMETERS",
				"\t-startdate\tStart date",
				"\t-enddate\tEnd date",
				"",
				"If no start date is specified, the current date and time are used.",
				"If no end date is specified, it is set to equal the start date, and only one line of output is produced.",
				"",
				"Valid date formats are YYYY-MM-DD or YYYY-MM-DD/hh:mm",
				"",
				"\t-step\t\tStep size",
				"",
				"Valid step size formats are an integer or an integer followed by a single letter (d, h, m, s) to indicate",
				"units.  If no units are specified, days are asssumed.",
				"",
				"\t-elongationdeltas\tDisplay change in elongation between output lines (prefixed with #)",
				"",
				"\t-latitude\tThe latitude of the observer, in degrees.",
				"\t-longitude\tThe longitude of the observer, in degrees.",
				"",
				"COORDINATE SYSTEM FOR RIGHT ASCENSION AND DECLINATION",
				"\t-true\t\tCalculate true position for epoch of date [this is the default]",
				"\t-mean\t\tCalculate mean position for epoch of date (i.e. apply precession but not nutation)",
				"\t-j2000\t\tCalculate position for epoch J2000",
				"\t-b1875\t\tCalculate position for epoch B1875 (the reference frame of the Delporte constellation boundaries)",
				"",
				"SATURN POLE MODEL",
				"\t-saturnpolemodel MODELNAME\tUse this model for the pole of Saturn [default: iau1989]",
				"",
				"\tAvailable models: dourneau, harpertaylor, iau1989, jacobson2007, struve",
				"",
				"OUTPUT COLUMNS (prefixed by column number)",
				"1\tJulian Day Number",
				"2,3\tDate and time",
				"4-6\tRight Ascension",
				"7-10\tDeclination",
				"11\tGeometric distance",
				"12\tLight-path distance",
				"13\tHeliocentric distance",
				"14\tConstellation",
				"15\tEpoch of Right Ascension and Declination",
				"16\tApparent diameter of disk",
				"",
				"[For Moon and planets]",
				"17\tElongation",
				"18\tElongation in ecliptic longitude",
				"19\tPhase angle",
				"20\tIlluminated fraction",
				"21\tApparent magnitude (zero for Moon)",
				"22\tEcliptic longitude (in same reference frame as RA and Dec)",
				"23\tEcliptic latitude (in same reference frame as RA and Dec)",
				"24\t[Saturn only] Saturnicentric latitude of Earth referred to ring plane",
				"25\t[Saturn only] Saturnicentric latitude of Sun referred to ring plane",
				"",
				"[For Sun]",
				"17\tEcliptic longitude (in same reference frame as RA and Dec)",
				"18\tEcliptic latitude (in same reference frame as RA and Dec)",
			
		};
		
		for (String line : lines)
			System.err.println(line);
	}

}
