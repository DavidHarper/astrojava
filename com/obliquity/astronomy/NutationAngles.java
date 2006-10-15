package com.obliquity.astronomy;

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
