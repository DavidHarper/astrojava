package com.obliquity.astronomy;

public class Place {
	protected double latitude;
	protected double longitude;
	protected double height;
	protected double timezone;
	
	public Place(double latitude, double longitude, double height, double timezone) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.height = height;
		this.timezone = timezone;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public double getHeight() {
		return height;
	}
	
	public double getTimeZone() {
		return timezone;
	}
}
