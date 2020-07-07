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

import com.obliquity.astronomy.almanac.saturnpole.DourneauSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.HarperTaylorSaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.IAU1989SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.Jacobson2007SaturnPoleModel;
import com.obliquity.astronomy.almanac.saturnpole.SaturnPolePosition;
import com.obliquity.astronomy.almanac.saturnpole.StruveSaturnPoleModel;

public class SaturnPoleModelTester {
	public static void main(String[] args) {
		SaturnPoleModelTester tester = new SaturnPoleModelTester();
		
		double epoch = args.length > 0 ? Double.parseDouble(args[0]) : 2451545.0;
		
		tester.run(epoch);
	}
	
	public void run(double epoch) {
		IAU1989SaturnPoleModel iau1989 = new IAU1989SaturnPoleModel();
		
		Jacobson2007SaturnPoleModel jacobson2007 = new Jacobson2007SaturnPoleModel();
		
		StruveSaturnPoleModel struve = new StruveSaturnPoleModel();
		
		DourneauSaturnPoleModel dourneau = new DourneauSaturnPoleModel();
		
		HarperTaylorSaturnPoleModel harperTaylor = new HarperTaylorSaturnPoleModel();
		
		SaturnPolePosition iau1989Pole = iau1989.getPolePosition(epoch);
		
		SaturnPolePosition jacobson2007Pole = jacobson2007.getPolePosition(epoch);
		
		SaturnPolePosition struvePole = struve.getPolePosition(epoch);
		
		SaturnPolePosition dourneauPole = dourneau.getPolePosition(epoch);
		
		SaturnPolePosition harperTaylorPole = harperTaylor.getPolePosition(epoch);
		
		showPole("IAU", iau1989Pole, iau1989Pole);
		showPole("Struve", struvePole, iau1989Pole);
		showPole("Dourneau", dourneauPole, iau1989Pole);
		showPole("Harper/Taylor", harperTaylorPole, iau1989Pole);
		showPole("Jacobson", jacobson2007Pole, iau1989Pole);
	}
	
	private final double R2D = 180.0/Math.PI;
	
	private void showPole(String name, SaturnPolePosition pole, SaturnPolePosition refPole) {
		double dy = pole.declination - refPole.declination;
		double dx = (pole.rightAscension - refPole.rightAscension) * Math.cos(refPole.declination);
		
		String format = "   %3s: %8.4f  %8.4f\n";
		
		System.out.println(name);
		System.out.printf(format, "RA", R2D * pole.rightAscension, R2D * dx);
		System.out.printf(format, "Dec", R2D * pole.declination, R2D * dy);
		System.out.println();
	}
}
