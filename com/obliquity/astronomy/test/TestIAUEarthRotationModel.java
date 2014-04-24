/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2014 David Harper at obliquity.com
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
