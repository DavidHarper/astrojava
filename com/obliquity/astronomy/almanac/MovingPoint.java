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

package com.obliquity.astronomy.almanac;

public interface MovingPoint {
	public StateVector getStateVector(double time) throws JPLEphemerisException;

	public void getStateVector(double time, StateVector sv)
			throws JPLEphemerisException;

	public Vector getPosition(double time) throws JPLEphemerisException;

	public void getPosition(double time, Vector v) throws JPLEphemerisException;

	public boolean isValidDate(double time);

	public double getEarliestDate();

	public double getLatestDate();

	public double getEpoch();
	
	public int getBodyCode();
	
	public JPLEphemeris getEphemeris();
}
