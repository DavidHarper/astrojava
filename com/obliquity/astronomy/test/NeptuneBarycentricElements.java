package com.obliquity.astronomy.test;

import java.io.*;
import java.text.DecimalFormat;

import com.obliquity.astronomy.*;

public class NeptuneBarycentricElements {
	private final DecimalFormat dfmt1 = new DecimalFormat("####000.00");
	private final DecimalFormat dfmt2 = new DecimalFormat("##0.000000000");
	
	private final JPLEphemeris ephemeris;
	private final double step;
	private final double tStart;
	private final double tFinish;
	
	private final double GMB;
	private final double AU;
	private final double K;
	
	private final double OBLIQUITY = Math.PI/180.0 * (23.0 + 26.0/60.0 + 21.448/3600.0);
	
	private final double MEAN_MOTION = 360.0/60189.53;
	
	private final double TWO_PI = 2.0 * Math.PI;
	
	private final double J2000 = 2451545.0;

	private Vector position = new Vector();
	private Vector velocity = new Vector();

	public NeptuneBarycentricElements(JPLEphemeris ephemeris, double step) {
		this.ephemeris = ephemeris;
		this.step = step;
		
		GMB = calculateMassOfSunAndPlanets();
		K = 1.0/Math.sqrt(GMB);
		AU = 1.0/ephemeris.getAU();
		
		tStart = ephemeris.getEarliestDate();
		tFinish = ephemeris.getLatestDate();
	}
	
	public void run() throws JPLEphemerisException {
		calculateElementsForDateRange();
	}
	
	private static final String[] DE406_FILES = {
		"unxm3000.406", "unxm2700.406", "unxm2400.406", "unxm2100.406",
		"unxm1800.406", "unxm1500.406", "unxm1200.406", "unxm0900.406",
		"unxm0600.406", "unxm0300.406", "unxp0000.406", "unxp0300.406",
		"unxp0600.406", "unxp0900.406", "unxp1200.406", "unxp1500.406",
		"unxp1800.406", "unxp2100.406", "unxp2400.406", "unxp2700.406"
	};
	
	public static void main(String[] args) {
		String[] filenames = args.length > 0 ? args : DE406_FILES;

		String strStep = System.getProperty("step", "10.0");
		double dt = Double.parseDouble(strStep);
		
		for (int i = 0; i < filenames.length; i++)
			processFile(filenames[i], dt);
		
		System.exit(0);
	}
	
	private static void processFile(String filename, double dt) {
		try {
			System.err.println("Loading file " + filename + " ...");
			JPLEphemeris ephemeris = new JPLEphemeris(filename);
			NeptuneBarycentricElements runner = new NeptuneBarycentricElements(ephemeris, dt);
			runner.run();
		} catch (JPLEphemerisException jee) {
			jee.printStackTrace();
			System.err.println("JPLEphemerisException ... " + jee);
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IOException ... " + ioe);
			System.exit(1);
		}
	}
	
	private double calculateMassOfSunAndPlanets() {
		String[] names = {"GMS", "GM1", "GM2", "GMB", "GM4", "GM5", "GM6", "GM7" };
		
		double sum = 0.0;
		
		for (int i = 0; i < names.length; i++) {
			Double mass = ephemeris.getConstant(names[i]);
			sum += mass.doubleValue();
		}
		
		return sum;
	}

	private void calculateElementsForDateRange() throws JPLEphemerisException {
		for (double t = tStart; t < tFinish; t += step)
			calculateElements(t);
	}
	
	private Vector U0 = new Vector();
	private Vector V0 = new Vector();
	private Vector P = new Vector();
	private Vector Q = new Vector();

	private void calculateElements(double t)
			throws JPLEphemerisException {
		ephemeris.calculatePositionAndVelocity(t, JPLEphemeris.NEPTUNE,
				position, velocity);
		
		position.rotate(OBLIQUITY, Vector.X_AXIS);
		velocity.rotate(OBLIQUITY, Vector.X_AXIS);
		
		position.multiplyBy(AU);
		velocity.multiplyBy(AU*K);
		
		double r = position.magnitude();
		double v = velocity.magnitude();
		
		double alpha = v * v - 2.0/r;
		double a = -1.0/alpha;
		
		double d0 = position.scalarProduct(velocity);
		
		double c0 = 1.0 + alpha * r;
		
		double ecc = Math.sqrt(c0 * c0 - alpha * d0 * d0);
		
		V0.linearCombination(velocity, r, position, -d0/r);
		V0.normalise();
		
		double kt0 = c0/ecc;
		double st0 = d0/ecc;
		double xw0 = r * kt0 - d0 * st0;
		
		U0.copy(position);
		U0.normalise();
		
		P.linearCombination(U0, kt0, velocity, -st0);
		P.normalise();
		
		Q.linearCombination(U0, st0, velocity, xw0);
		Q.normalise();
		
		Vector W = P.vectorProduct(Q);
		
		double xi = Math.sqrt(W.getX() * W.getX() + W.getY() * W.getY());
		double yi = W.getZ();
		
		double incl = Math.atan2(xi, yi);
		
		incl *= 180.0/Math.PI;
		
		double node = Math.atan2(W.getX(), -W.getY());
		
		while (node < 0.0)
			node += TWO_PI;
		
		node %= TWO_PI;
		
		double apse = node + Math.atan2(P.getZ(), Q.getZ());
		
		while (apse < 0.0)
			apse += TWO_PI;
		
		apse %= TWO_PI;
		
		double eAnomaly = Math.atan2(st0*Math.sqrt(-alpha), kt0);
		
		double mAnomaly = eAnomaly - ecc * Math.sin(eAnomaly);
		
		double lambda = apse + mAnomaly;
		
		while (lambda < 0.0)
			lambda += TWO_PI;
		
		node *= 180.0/Math.PI;
		apse *= 180.0/Math.PI;
		lambda *= 180.0/Math.PI;
		
		lambda -= MEAN_MOTION * (t - J2000);
		while (lambda < 0.0)
			lambda += 360.0;
		
		lambda %= 360.0;

		System.out.print(dfmt1.format(t));
		System.out.print(' ');
		
		System.out.print(dfmt2.format(a));
		System.out.print(' ');
		System.out.print(dfmt2.format(ecc));
		System.out.print(' ');
		System.out.print(dfmt2.format(incl));
		System.out.print(' ');
		System.out.print(dfmt2.format(node));
		System.out.print(' ');
		System.out.print(dfmt2.format(apse));
		System.out.print(' ');
		System.out.print(dfmt2.format(lambda));

		System.out.println();
	}

}
