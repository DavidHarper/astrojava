package com.obliquity.astronomy.almanac;

public class AstronomicalDate implements Comparable<AstronomicalDate> {
	private int year, month, day, hour, minute;
	double second;
	
	private double julianDate = Double.NaN;

	private static final int GREGORIAN_TRANSITION_JD = 2299160;
	private static final int GREGORIAN_TRANSITION_DATE = 15821004;

	public AstronomicalDate(int year, int month, int day, int hour, int minute,
			double second) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}

	public AstronomicalDate(int year, int month, int day) {
		this(year, month, day, 0, 0, 0.0);
	}
	
	// This is a nasty hack to enable the routine to handle
	// negative Julian Day numbers.
	private static final int JULIAN_CYCLES_OFFSET = 10000;
	
	public AstronomicalDate(double djd) {
		this(djd, false);
	}

	public AstronomicalDate(double djd, boolean useProlepticGregorianCalendar) {
		julianDate = djd;
		
		int K, L, N, I, J, D, M, Y;
		int JD;
		double t;

		JD = (int) Math.floor(djd + 0.5);

		if (JD > GREGORIAN_TRANSITION_JD || useProlepticGregorianCalendar) {
			// Gregorian calendar
			L = JD + 68569;
			N = (4 * L) / 146097;
			L -= (146097 * N + 3) / 4;
			I = (4000 * (L + 1)) / 1461001;
			L += -(1461 * I) / 4 + 31;
			J = (80 * L) / 2447;
			D = L - (2447 * J) / 80;
			L = J / 11;
			M = J + 2 - 12 * L;
			Y = 100 * (N - 49) + I + L;
		} else {
			// Julian calendar
			J = JD + 1402 + 1461 * JULIAN_CYCLES_OFFSET;
			K = (J - 1) / 1461;
			L = J - 1461 * K;
			N = (L - 1) / 365 - L / 1461;
			I = L - 365 * N + 30;
			J = (80 * I) / 2447;
			D = I - (2447 * J) / 80;
			I = J / 11;
			M = J + 2 - 12 * I;
			Y = 4 * K + N + I - 4716 - 4 * JULIAN_CYCLES_OFFSET;
		}

		this.year = Y;

		this.month = M;

		this.day = D;

		t = (24.0 * (0.5 + djd - Math.floor(djd))) % 24.0;

		this.hour = (int) t;

		t -= Math.floor(t);
		t *= 60.0;
		this.minute = (int) t;

		t -= Math.floor(t);
		this.second = 60.0 * t;
	}

	public double getJulianDate() {
		if (!Double.isNaN(julianDate))
			return julianDate;
		
		int D, M, Y, mu, JD;
		int ThisDate;

		D = this.day;
		M = this.month;
		Y = this.year;

		ThisDate = 10000 * Y + 100 * M + D;

		if (ThisDate > GREGORIAN_TRANSITION_DATE) {
			// Gregorian algorithm
			mu = (M - 14) / 12;

			JD = (1461 * (Y + 4800 + mu)) / 4;

			JD += (367 * (M - 2 - 12 * mu)) / 12;

			mu = (Y + 4900 + mu) / 100;

			JD -= (3 * mu) / 4;

			JD += D - 32075;
		} else {
			// Julian algorithm
			mu = (M - 9) / 7;
			mu = (7 * (Y + 5001 + mu)) / 4;

			JD = 367 * Y - mu + (275 * M) / 9 + D + 1729777;
		}

		julianDate = (double) JD - 0.5 + ((double) this.hour) / 24.0
				+ ((double) this.minute) / 1440.0 + (this.second) / 86400.0;
		
		return julianDate;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public int getHour() {
		return hour;
	}

	public int getMinute() {
		return minute;
	}

	public double getSecond() {
		return second;
	}
	
	public boolean roundToNearestMinute() {
		if (second < 30.0)
			return false;
		
		second = 0.0;
		
		// Increment the minute figure and cascade any overflows to higher components if necessary.
		
		minute++;
		
		if (minute > 59) {
			minute -= 60;
			
			hour++;
			
			if (hour > 23) {
				hour -= 24;
				
				day++;
				
				int maxday = getLengthOfMonth();
				
				if (day > maxday) {
					day -= maxday;
					
					month++;
					
					if (month > 12) {
						month -= 12;
						
						year++;
					}
				}
			}
		}
		
		// Invalidate any pre-calculated Julian date.
		julianDate = Double.NaN;
		
		return true;
	}
	
	private int getLengthOfMonth() {
		  final int ML[] = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		  
		  int ml = ML[month];
		  
		  if (isLeapYear() && (month == 2))
			  ml++;
		  
		  return ml;
	}
	
	private boolean isLeapYear() {
	  /* is it divisible by 4? */
	  if ((year % 4) != 0)
		  return false;

	  /* Is it a century year? */
	  if ((year % 100) != 0)
		  return true;

	  /* Is it before 1752? */
	  if (year < 1752)
		  return true;

	  /* Is it divisible by 400? */
	  if ((year % 400) == 0)
		  return true;

	  /* We're left with centurial years after 1752 that don't divide by 400. */
	  return false;
	}

	public boolean equals(AstronomicalDate that) {
		return this.year == that.year && this.month == that.month && this.day == that.day &&
				this.hour == that.hour && this.minute == that.minute && Math.abs(this.second - that.second) < 0.01;
	}

	public int compareTo(AstronomicalDate that) {
		int d = this.year - that.year;
		
		if (d != 0)
			return d;
		
		d = this.month - that.month;
		
		if (d != 0)
			return d;
		
		d = this.day - that.day;
		
		if (d != 0)
			return d;
		
		d = this.hour - that.hour;
		
		if (d != 0)
			return d;
		
		d = this.minute - that.minute;
		
		if (d != 0)
			return d;
		
		if (this.second < that.second)
			return -1;
		
		if (this.second > that.second)
			return 1;
		
		return 0;
	}
	
	public String toString() {
		return String.format("%04d-%02d-%02d %02d:%02d:%05.2f", year, month, day, hour, minute, second);
	}
	
	public String toISO8601String() {
		return String.format("%04d-%02d-%02dT%02d:%02d:%05.2f", year, month, day, hour, minute, second);
	}
}
