package com.obliquity.astronomy;

public class PrecessionAngles {
	private double zeta, z, theta;

	public PrecessionAngles() {
		zeta = z = theta = 0.0;
	}

	public PrecessionAngles(double zeta, double z, double theta) {
		this.zeta = zeta;
		this.z = z;
		this.theta = theta;
	}

	public void setAngles(double zeta, double z, double theta) {
		this.zeta = zeta;
		this.z = z;
		this.theta = theta;
	}

	public void setZeta(double zeta) {
		this.zeta = zeta;
	}

	public double getZeta() {
		return zeta;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public double getZ() {
		return z;
	}

	public void setTheta(double theta) {
		this.theta = theta;
	}

	public double getTheta() {
		return theta;
	}
}
