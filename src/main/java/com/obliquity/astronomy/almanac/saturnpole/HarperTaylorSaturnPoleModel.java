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
 * Source: Harper, D. and Taylor, D.B. (1993) The orbits of the major satellites of Saturn.
 * Astron. Astrophys. 268, 326.
 * http://adsabs.harvard.edu/abs/1993A%26A...268..326H
 */

public class HarperTaylorSaturnPoleModel extends EclipticNodeAndInclinationSaturnPoleModel {
	private final double sourceEpoch;
	
	public HarperTaylorSaturnPoleModel() {
		super(168.8387  * Math.PI/180.0, 28.0653 * Math.PI/180.0);
		this.sourceEpoch = erm.BesselianEpoch(1950.0);
	}
	
	double getSourceEpoch() {
		return sourceEpoch;
	}

	public SaturnPolePosition getPolePosition(double epoch) {
		return nodeAndInclinationToPolePosition(epoch);
	}
}
