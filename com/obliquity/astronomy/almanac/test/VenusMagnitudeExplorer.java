package com.obliquity.astronomy.almanac.test;

public class VenusMagnitudeExplorer {
	public static void main(String[] args) {
		VenusMagnitudeExplorer vme = new VenusMagnitudeExplorer();		
		vme.run();
	}
	
	public void run() {
		double rEarth = 1.0, rVenus = 0.723332;
		
		for (int i = 1; i < 293; i++) {
			double x = Math.PI * ((double)i)/292.0;
			
			double delta = Math.sqrt(rEarth * rEarth + rVenus * rVenus - 2.0 * rEarth * rVenus * Math.cos(x));
			
			double theta = Math.acos((delta * delta + rVenus * rVenus - rEarth * rEarth)/(2.0 * delta * rVenus));
			
			theta *= 180.0/Math.PI;
			
			x *= 180.0/Math.PI;
			
			double m1 = -4.40 + 5.0 * Math.log10(rVenus);
			
			double m2 = 5.0 * Math.log10(delta);
			
			double q = theta/100.0;
			
			double m3 = 0.09 * q + 2.39 * q * q -0.65 * q * q * q;
			
			double m = m1 + m2 + m3;
			
			System.out.printf("%3d  %4.1f  %7.5f %6.2f %6.2f  %6.2f\n", i, theta, delta, m2, m3, m);
		}
	}
}
