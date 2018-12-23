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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.obliquity.astronomy.almanac.ApparentPlace;
import com.obliquity.astronomy.almanac.AstronomicalDate;
import com.obliquity.astronomy.almanac.EarthCentre;
import com.obliquity.astronomy.almanac.EarthRotationModel;
import com.obliquity.astronomy.almanac.IAUEarthRotationModel;
import com.obliquity.astronomy.almanac.JPLEphemeris;
import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.MovingPoint;
import com.obliquity.astronomy.almanac.PlanetCentre;

public class NereidDataAnalyser {
	private class NereidObservation implements Comparable<NereidObservation> {
		public int observationNumber;
		public double julianDate;
		public double rightAscension;
		public double declination;
		public int observatoryCode;
		
		public NereidObservation(int observationNumber, double julianDate, double rightAscension, double declination, int observatoryCode) {
			this.observationNumber = observationNumber;
			this.julianDate = julianDate;
			this.rightAscension = rightAscension;
			this.declination = declination;
			this.observatoryCode = observatoryCode;
		}

		public int compareTo(NereidObservation that) {
			if (this.julianDate < that.julianDate)
				return -1;
			
			if (this.julianDate > that.julianDate)
				return 1;
			
			return 0;
		}
	}
	
	public static void main(String[] args) {
		String ephemerisFilename = System.getProperty("ephemeris");
		
		if (ephemerisFilename == null) {
			System.err.println("You must specify the ephemeris file using -Dephemeris=<filename>");
			System.exit(1);
		}
			
		NereidDataAnalyser analyser = new NereidDataAnalyser();
		
		try {
			analyser.run();
		} catch (IOException | JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	public void run() throws IOException, JPLEphemerisException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		List<NereidObservation> data = new ArrayList<NereidObservation>();
		
		while (true) {
			String line = br.readLine();
			
			if (line == null)
				break;
			
			String[] words = line.trim().split("\\s+");
			
			int type = Integer.parseInt(words[words.length-2]);
			
			if (type == 22)
				continue;
			
			int obsnum = Integer.parseInt(words[0]);
			
			int year = Integer.parseInt(words[1]);
			int month = Integer.parseInt(words[2]);
			double day = Double.parseDouble(words[3]);
			int iDay = (int)day;
			
			AstronomicalDate ad = new AstronomicalDate(year, month, iDay);
			
			double jd = ad.getJulianDate() + day - (double)iDay;
			
			int raHour = Integer.parseInt(words[4]);
			int raMin = Integer.parseInt(words[5]);
			double raSec = Double.parseDouble(words[6]);
			
			double raObserved = (double)raHour + ((double)raMin)/60.0 + raSec/3600.0;
			
			boolean dash = words[7].equalsIgnoreCase("-");
			
			int decDeg = Integer.parseInt(words[dash ? 8 : 7]);
			int decMin = Integer.parseInt(words[dash ? 9 : 8]);
			double decSec = Double.parseDouble(words[dash ? 10 : 9]);
			
			boolean decSouth = dash || decDeg < 0;
			
			if (decDeg < 0)
				decDeg = -decDeg;
			
			double decObserved = (double)decDeg + ((double)decMin)/60.0 + decSec/3600.0;
			
			if (decSouth)
				decObserved = -decObserved;
			
			int obsCode = Integer.parseInt(words[dash ? 12 : 11]);
			
			data.add(new NereidObservation(obsnum, jd, raObserved, decObserved, obsCode));
		}
		
		System.err.println("Found " + data.size() + " observations in the file.");
		
		NereidObservation[] observations = new NereidObservation[data.size()];
		
		data.toArray(observations);
		
		Arrays.sort(observations);
		
		double jdstart = observations[0].julianDate;
		double jdfinish = observations[observations.length - 1].julianDate;
	
		System.err.println("Date range is " + jdstart + " to " + jdfinish);
		
		String ephemerisFilename = System.getProperty("ephemeris");
		
		JPLEphemeris ephemeris =  new JPLEphemeris(ephemerisFilename, jdstart - 1.0,
					jdfinish + 1.0);

		MovingPoint nereid = new Nereid(ephemeris);
		
		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace ap = new ApparentPlace(earth, nereid, sun, erm);

		for (NereidObservation obs : observations) {
			double ut = obs.julianDate;
			
			double dt = erm.deltaT(ut);
			
			double tt = ut + dt;
			
			ap.calculateApparentPlace(tt);
			
			double ra = ap.getRightAscensionJ2000() * 12.0/Math.PI;
			
			if (ra < 0.0)
				ra += 24.0;
			
			double dec = ap.getDeclinationJ2000();
			
			double cosDec = Math.cos(dec);
			
			dec *= 180.0/Math.PI;
			
			double dx = 15.0 * 3600.0 * cosDec * (obs.rightAscension - ra);
			
			double dy = 3600.0 * (obs.declination - dec);
			
			System.out.printf("%4d %13.5f %8.3f %8.3f\n", obs.observationNumber, ut, dx, dy);
		}
	}
}
