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

package com.obliquity.astronomy.almanac.phenomena;

import com.obliquity.astronomy.almanac.AstronomicalDate;

public class Phenomenon implements Comparable<Phenomenon> {
	public enum Type {
		CONJUNCTION,
		OPPOSITION,
		QUADRATURE_EAST,
		QUADRATURE_WEST, 
		GREATEST_ELONGATION_EAST,
		GREATEST_ELONGATION_WEST,
		STATIONARY_EAST,
		STATIONARY_WEST
	}
	
	private Type type;
	private AstronomicalDate date;
	private int bodyCode;
	
	public Phenomenon(Type type, AstronomicalDate date, int bodyCode) {
		this.type = type;
		this.date = date;
		this.bodyCode = bodyCode;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getTypeAsString() {
		switch (type) {
		case CONJUNCTION:
			return "conjunction";
			
		case OPPOSITION:
			return "opposition";
			
		case QUADRATURE_EAST:
			return "quadrature east";
			
		case QUADRATURE_WEST:
			return "quadrature_west";
			
		case GREATEST_ELONGATION_EAST:
			return "greatest elongation east";
			
		case GREATEST_ELONGATION_WEST:
			return "greatest elongation west";
			
		case STATIONARY_EAST:
			return "stationary east";
			
		case STATIONARY_WEST:
			return "stationary west";
		}
		
		return null;
	}
	
	public AstronomicalDate getDate() {
		return date;
	}
	
	public int getBodyCode() {
		return bodyCode;
	}
	
	public int compareTo(Phenomenon that) {
		return date.compareTo(that.date);
	}
	
}
