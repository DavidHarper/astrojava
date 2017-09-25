package com.obliquity.astronomy.almanac.test;

import java.text.DecimalFormat;
import java.util.Random;

import com.obliquity.astronomy.almanac.AstronomicalDate;

public class TestAstronomicalDate {
	private static final double J2000 = 2451545.5;
	private static final double JULIAN_MILLENNIUM = 365.25 * 1000.0;
	
	public static void main(String[] args) {
		TestAstronomicalDate tad = new TestAstronomicalDate();
		
		tad.run();
		
		System.exit(0);;
	}
	
	public void run() {
		Random random = new Random();
		
		DecimalFormat dfmta = new DecimalFormat("0.00000");
		DecimalFormat dfmtb = new DecimalFormat("00.00");
		DecimalFormat ifmta = new DecimalFormat(" 0000;-0000");
		DecimalFormat ifmtb = new DecimalFormat("00");

		System.out.println("TEST 1: Julian Day Number to date to Julian Day Number");
		
		for (int i = 0; i < 100; i++) {
			double jd = J2000 + 6.0 * (random.nextDouble() - 0.5) * JULIAN_MILLENNIUM;
			
			AstronomicalDate date = new AstronomicalDate(jd);
			
			double jd2 = date.getJulianDate();
			
			String status = Math.abs(jd - jd2) < 0.00001 ? "OK" : "ERROR";
			
			System.out.println(dfmta.format(jd) + " ==> " +
					ifmta.format(date.getYear()) + " " +
					ifmtb.format(date.getMonth()) + " " +
					ifmtb.format(date.getDay()) + "  " +
					ifmtb.format(date.getHour()) + ":" +
					ifmtb.format(date.getMinute()) + ":" +
					dfmtb.format(date.getSecond()) + "  " +
					dfmta.format(jd2) +
					"  [" + status + "]"
					);
		}
		
		System.out.println();
		
		System.out.println("TEST 2: Date to Julian Day Number to date");
		
		for (int i = 0; i < 100; i++) {
			int year = -1000 + random.nextInt(4000);
			int month = 1 + random.nextInt(12);
			int day = getRandomDay(random, year, month);
			
			int hour = random.nextInt(24);
			int minute = random.nextInt(60);
			double second = 60.0 * random.nextDouble();
			
			AstronomicalDate date = new AstronomicalDate(year, month, day, hour, minute, second);
			
			AstronomicalDate date2 = new AstronomicalDate(date.getJulianDate());

			String status = date.equals(date2) ? "OK" : "ERROR";
		
			System.out.println(
					ifmta.format(year) + " " +
					ifmtb.format(month) + " " +
					ifmtb.format(day) + "  " +
					ifmtb.format(hour) + ":" +
					ifmtb.format(minute) + ":" +
					dfmtb.format(second) + " ==> " +
					dfmta.format(date.getJulianDate()) + " ==> " +
					ifmta.format(date2.getYear()) + " " +
					ifmtb.format(date2.getMonth()) + " " +
					ifmtb.format(date2.getDay()) + "  " +
					ifmtb.format(date2.getHour()) + ":" +
					ifmtb.format(date2.getMinute()) + ":" +
					dfmtb.format(date2.getSecond()) +
					"  [" + status + "]"
					);
			
		}
	}
	
	private int getRandomDay(Random random, int year, int month) {
		boolean isLeapYear = year > 1582 ? (year % 4) == 0 || (year % 400) == 0 : (year % 4) == 0;
		
		switch (month) {
		case 1:
		case 3:
		case 5:
		case 7:
		case 8:
		case 10:
		case 12:
			return 1 + random.nextInt(31);
			
		case 2:
			return 1 + random.nextInt(isLeapYear ? 29 : 28);
			
		default:
			return 1 + random.nextInt(30);
		}
	}
}
