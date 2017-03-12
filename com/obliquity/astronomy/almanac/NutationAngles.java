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

public class NutationAngles {
	private double dpsi, deps;

	public NutationAngles() {
		dpsi = deps = 0.0;
	}

	public NutationAngles(double dpsi, double deps) {
		this.dpsi = dpsi;
		this.deps = deps;
	}

	public void setAngles(double dpsi, double deps) {
		this.dpsi = dpsi;
		this.deps = deps;
	}

	public void setDpsi(double dpsi) {
		this.dpsi = dpsi;
	}

	public double getDpsi() {
		return dpsi;
	}

	public void setDeps(double deps) {
		this.deps = deps;
	}

	public double getDeps() {
		return deps;
	}
}
