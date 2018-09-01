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

package com.obliquity.astronomy.almanac.saturnpole;

/*
 * Source: Davies, M.E. et al. (1989) Report of the IAU/IAG/COSPAR working group on
 * cartographic coordinates and rotational elements of the planets and satellites - 1988
 * Celestial Mechanics and Dynamical Astronomy 46, 187.
 */

public class IAU1989SaturnPoleModel extends RightAscensionAndDeclinationSaturnPoleModel {
	public SaturnPolePosition getPolePositionInSourceFrame(double epoch) {
		double T = (epoch - 2451545.0)/36525.0;
		
		double ra = (40.58 - 0.036 * T) * Math.PI/180.0;
		
		double dec = (83.54 - 0.004 * T) * Math.PI/180.0;
		
		return new SaturnPolePosition(ra, dec);
	}

	double getSourceEpoch() {
		return 2451545.0;
	}
}
