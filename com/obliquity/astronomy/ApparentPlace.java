package com.obliquity.astronomy;

public class ApparentPlace {
    protected double RA = 0.0;
    protected double dec = 0.0;
    protected double pl = 0.0;
    protected double gd = 0.0;

    public ApparentPlace(double RA, double dec, double pl, double gd) {
	setData(RA, dec, pl, gd);
    }

    public ApparentPlace() {
	setData(0.0, 0.0, 0.0, 0.0);
    }

    public void setData(double RA, double dec, double pl, double gd) {
	this.RA = RA;
	this.dec = dec;
	this.pl = pl;
	this.gd = gd;
    }

    public void setRightAscension(double RA) { this.RA = RA; }

    public double getRightAscension() { return  RA; }

    public void setDeclination(double dec) { this.dec = dec; }

    public double getDeclination() { return dec; }

    public void setRightAscensionAndDeclination(double RA, double dec) {
	this.RA = RA;
	this.dec = dec;
    }

    public void setLightPathDistance(double pl) { this.pl = pl; }

    public double getLightPathDistance() { return pl; }

    public void setGeometricDistance(double gd) { this.gd = gd; }

    public double getGeometricPathDistance() { return gd; }
}
