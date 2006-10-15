package com.obliquity.astronomy.test;

import com.obliquity.astronomy.*;

import java.text.*;

public class TestIAUEarthRotationModel {
	public static void main(String args[]) {
		double JD, jdFixed;

		double jdstart, jdfinish, jdstep;

		if (args.length < 3) {
			System.err
					.println("Usage: TestIAUEarthRotationModel start-date end-date step [fixed-epoch]");
			return;
		}

		jdstart = Double.parseDouble(args[0]);
		jdfinish = Double.parseDouble(args[1]);
		jdstep = Double.parseDouble(args[2]);

		DecimalFormat format = new DecimalFormat("0.0000000000");
		// format.setPositivePrefix(" ");

		IAUEarthRotationModel erm = new IAUEarthRotationModel();

		if (args.length > 3)
			jdFixed = Double.parseDouble(args[3]);
		else
			jdFixed = 2415020.0 + 50.0 * 365.25;

		for (JD = jdstart; JD <= jdfinish; JD += jdstep) {
			System.out.println("JD = " + JD);

			double dt = erm.deltaT(JD);
			System.out.println("  DeltaT = " + format.format(dt));

			double gmst = erm.greenwichMeanSiderealTime(JD);
			System.out.println("  GMST = " + format.format(gmst));

			NutationAngles na = erm.nutationAngles(JD);
			double dpsi = na.getDpsi();
			double deps = na.getDeps();
			System.out.println("  Nutation angles = " + format.format(dpsi)
					+ ", " + format.format(deps));

			PrecessionAngles pa = erm.precessionAngles(jdFixed, JD);
			double zeta = pa.getZeta();
			double z = pa.getZ();
			double theta = pa.getTheta();
			System.out.println("  Precession angles = " + format.format(zeta)
					+ ", " + format.format(z) + ", " + format.format(theta));
		}
	}
}
