package com.obliquity.astronomy.almanac;

public class AstronomicalDate {
	private int year, month, day, hour, minute;
	double second;

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

	public AstronomicalDate(double djd) {
		int K, L, N, I, J, D, M, Y;
		int JD;
		double t;

		JD = (int) (djd + 0.5);

		if (JD > GREGORIAN_TRANSITION_JD) {
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
			J = JD + 1402;
			K = (J - 1) / 1461;
			L = J - 1461 * K;
			N = (L - 1) / 365 - L / 1461;
			I = L - 365 * N + 30;
			J = (80 * I) / 2447;
			D = I - (2447 * J) / 80;
			I = J / 11;
			M = J + 2 - 12 * I;
			Y = 4 * K + N + I - 4716;
		}

		this.year = Y;

		this.month = M;

		this.day = D;

		t = 24.0 * ((djd + 0.5) % 1.0);

		this.hour = (int) t;

		t -= Math.floor(t);
		t *= 60.0;
		this.minute = (int) t;

		t -= Math.floor(t);
		this.second = 60.0 * t;
	}

	public double getJulianDate() {
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

		return (double) JD - 0.5 + ((double) this.hour) / 24.0
				+ ((double) this.minute) / 1440.0 + (this.second) / 86400.0;

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
}
