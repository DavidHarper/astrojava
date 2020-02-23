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
