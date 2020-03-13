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

package com.obliquity.astronomy.almanac.marspole;

/*
 * Source: Kuchynka, P. et al. (2014)
 * New constraints on Mars rotation determined from radiometric tracking of the Opportunity Mars Exploration Rover
 * Icarus 229, 340–347
 * http://dx.doi.org/10.1016/j.icarus.2013.11.015
 */

public class Kuchynka2014PoleModel implements MarsPoleModel {
	private final double[][] periodicTermsInRA = {
			{ +0.000068, 198.991226, 19139.4819985 },
			{ +0.000238, 226.292679, 38280.8511281 },
			{ +0.000052, 249.663391, 57420.7251593 },
			{ +0.000009, 266.183510, 76560.6367950 },
			{ +0.419057,  79.398797,     0.5042615 }
	};
	
	private final double[][] periodicTermsInDec = {
			{ +0.000051, 122.433576, 19139.9407476 },
			{ +0.000141,  43.058401, 38280.8753272 },
			{ +0.000031,  57.663379, 57420.7517205 },
			{ +0.000005,  79.476401, 76560.6495004 },
			{ +1.591274, 166.325722,     0.5042615 }
	};
	
	public MarsPolePosition getPolePosition(double epoch) {
		double T = (epoch - 2451545.0)/36525.0;
		
		double ra = 317.269202 - 0.10927547 * T;
		
		for (int i = 0; i < periodicTermsInRA.length; i++) {
			double arg = Math.PI/180.0 * ((periodicTermsInRA[i][1] + periodicTermsInRA[i][2] * T) % 360.0);
			
			ra += periodicTermsInRA[i][0] * Math.sin(arg);
		}
		
		double dec = 54.432516 - 0.05827105 * T;
		
		for (int i = 0; i < periodicTermsInDec.length; i++) {
			double arg = Math.PI/180.0 * ((periodicTermsInDec[i][1] + periodicTermsInDec[i][2] * T) % 360.0);
			
			dec += periodicTermsInDec[i][0] * Math.cos(arg);
		}
		
		return new MarsPolePosition(Math.PI * ra / 180.0, Math.PI * dec / 180.0);
	}

}
