package com.obliquity.astronomy.almanac.test;

import com.obliquity.astronomy.almanac.JPLEphemerisException;
import com.obliquity.astronomy.almanac.Vector;

public class TestNereid {
	public static void main(String[] args) {
		TestNereid tester = new TestNereid();
		
		try {
			tester.run();
		} catch (JPLEphemerisException e) {
			e.printStackTrace();
		}
	}
	
	public void run() throws JPLEphemerisException {
		Nereid nereid = new Nereid();
		
		for (double t = 2458464.5; t <= 2458844.5; t += 10.0) {
			Vector p = nereid.getPosition(t);
			
			System.out.printf("%10.2f %13.2f %13.2f %13.2f\n", t, p.getX(), p.getY(), p.getZ());
		}
	}
}
