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

package com.obliquity.astronomy.almanac;

public class StateVector {
	private Vector position = null;
	private Vector velocity = null;

	public StateVector(Vector position, Vector velocity) {
		this.position = position;
		this.velocity = velocity;
	}

	public StateVector(StateVector that) {
		this.position = new Vector(that.getPosition());
		this.velocity = new Vector(that.getVelocity());
	}

	public void setPosition(Vector position) {
		this.position = position;
	}

	public Vector getPosition() {
		return position;
	}

	public void setPositionComponents(double[] components) {
		if (position == null)
			position = new Vector(components);
		else
			position.setComponents(components);
	}

	public double[] getPositionComponents() {
		if (position == null)
			return null;
		else
			return position.getComponents();
	}

	public void setVelocity(Vector velocity) {
		this.velocity = velocity;
	}

	public Vector getVelocity() {
		return velocity;
	}

	public void setVelocityComponents(double[] components) {
		if (velocity == null)
			velocity = new Vector(components);
		else
			velocity.setComponents(components);
	}

	public double[] getVelocityComponents() {
		if (velocity == null)
			return null;
		else
			return velocity.getComponents();
	}

	public void subtract(StateVector that) {
		position.subtract(that.getPosition());
		velocity.subtract(that.getVelocity());
	}
	
	public void add(StateVector that) {
		position.add(that.getPosition());
		velocity.add(that.getVelocity());
	}
}
