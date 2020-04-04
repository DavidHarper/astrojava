package com.obliquity.astronomy.almanac.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
import com.obliquity.astronomy.almanac.RiseSetEvent;
import com.obliquity.astronomy.almanac.RiseSetEventType;
import com.obliquity.astronomy.almanac.RiseSetType;

public class Comet2019Y4Apparition {
	public static void main(String[] args) {
		double latitude = Double.NaN;
		String filename = null;
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
			case "-latitude":
				latitude = Double.parseDouble(args[++i]) * Math.PI/180.0;
				break;
				
			case "-ephemeris":
				filename = args[++i];
				break;
				
			default:
				System.err.println("Unrecognised argument: " + args[i]);
				System.exit(1);
			}
		}
		
		if (filename == null) {
			System.err.println("You must supply an ephemeris filename with the -ephemeris argument");
			System.exit(1);
		}
		
		if (Double.isNaN(latitude)) {
			System.err.println("You must supply a latitude with the -latitude argument");
			System.exit(1);
		}
	
		Comet2019Y4Apparition runner = new Comet2019Y4Apparition();
		
		try {
			runner.run(latitude, filename);
		} catch (IOException | JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	public void run(double latitude, String filename) throws IOException, JPLEphemerisException {
		AstronomicalDate startDate = new AstronomicalDate(2020, 1, 1);
		AstronomicalDate endDate = new AstronomicalDate(2020, 12, 31);
		
		JPLEphemeris ephemeris = new JPLEphemeris(filename, startDate.getJulianDate(), endDate.getJulianDate());
		
		EarthCentre earth = new EarthCentre(ephemeris);

		MovingPoint sun = null;

		sun = new PlanetCentre(ephemeris, JPLEphemeris.SUN);

		EarthRotationModel erm = new IAUEarthRotationModel();

		ApparentPlace apSun = new ApparentPlace(earth, sun, sun, erm);
		
		LocalVisibility lv = new LocalVisibility();
		
		Place place = new Place(latitude, 0.0, 0.0, 0.0);
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// Skip first two lines
		br.readLine();
		br.readLine();
		
		while (true) {
			String line = br.readLine();
			
			if (line == null)
				break;
			
			String[] words = line.split("\\s+");
			
			int year = Integer.parseInt(words[0]);
			int month = Integer.parseInt(words[1]);
			int day = Integer.parseInt(words[2]);
			
			double ra = Double.parseDouble(words[4]) * Math.PI/180.0;
			double dec = Double.parseDouble(words[5]) * Math.PI/180.0;
			
			AstronomicalDate date = new AstronomicalDate(year, month, day);
			
			double jd = date.getJulianDate();
			
			RiseSetEvent[] events = lv.findRiseSetEvents(apSun,
					place, jd, RiseSetType.UPPER_LIMB);
						
			for (RiseSetEvent rse : events) {
				HorizontalCoordinates hc = lv.calculateApparentAltitudeAndAzimuth(ra, dec, place, rse.date);
			
				HorizontalCoordinates hcSun = lv.calculateApparentAltitudeAndAzimuth(apSun, place, rse.date);
			
				date = new AstronomicalDate(rse.date);
			
				System.out.printf("%s %04d %02d %02d  %02d:%02d  %6.1f  %6.1f  %6.1f\n",
						(rse.type == RiseSetEventType.RISE ? "R" : "S"),
						date.getYear(), date.getMonth(), date.getDay(), date.getHour(), date.getMinute(), 
						hcSun.azimuth * 180.0/Math.PI, hc.azimuth * 180.0/Math.PI, hc.altitude * 180.0/Math.PI);
			}
		}
	}
}
