import com.obliquity.astronomy.*;

import java.io.*;
import java.text.*;
import java.lang.*;

public class TestApparentPlace {
    private final static double EPSILON = 1.0e-9;

    public static void main(String args[]) {
	if (args.length < 3) {
	    System.err.println("Usage: TestApparentPlace filename kBody date");
	    System.exit(1);
	}

	DecimalFormat format = new DecimalFormat("0.000000000");
	format.setPositivePrefix(" ");

	String filename = args[0];

	int kBody = Integer.parseInt(args[1]);
	double t = Double.parseDouble(args[2]);

	double jdstart = t - 10.0;
	double jdfinis = t + 10.0;

	JPLEphemeris ephemeris = null;

	try {
	    ephemeris = new JPLEphemeris(filename, jdstart, jdfinis);
	}
	catch (JPLEphemerisException jee) {
	    jee.printStackTrace();
            System.err.println("JPLEphemerisException ... " + jee);
	    System.exit(1);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("IOException ... " + ioe);
 	    System.exit(1);
	}

	PlanetCentre planet = new PlanetCentre(ephemeris, kBody);
	EarthCentre earth = new EarthCentre(ephemeris);
	PlanetCentre sun = (kBody == JPLEphemeris.SUN) ? 
	    planet : new PlanetCentre(ephemeris, JPLEphemeris.SUN);

	double tEarliest = ephemeris.getEarliestDate() + 1.0;
	double tLatest   = ephemeris.getLatestDate() - 1.0;
	double tSpan = tLatest - tEarliest;

	boolean silent = Boolean.getBoolean("silent");

	StateVector svEarth = null;
	Vector EB = null;
	Vector SB = new Vector();
	Vector QB = new Vector();

	Vector P = new Vector();
	Vector Q = new Vector();
	Vector E = new Vector();

	double c = 173.1446 * ephemeris.getAU();
	double factor = 2.0 * 9.87e-9;


	if (!silent) {
	    System.err.println("============== NEXT DATE ==============");
	    System.err.println("t = " + t);
	}

	try {
	    double tau = 0.0;
	    
	    svEarth = earth.getStateVector(t);
	    EB = svEarth.getPosition();
		
	    sun.getPosition(t, SB);

	    if (!silent) {
		System.err.println("EB = " + EB.prettyPrint(format));
		System.err.println("SB = " + SB.prettyPrint(format));
	    }

	    E.copy(EB);
	    E.subtract(SB);

	    if (!silent)
		System.err.println("E = " + E.prettyPrint(format));

	    double EE = E.magnitude();

	    double dtau;
	    double ctau;

	    do {
		if (!silent) {
		    System.err.println("-------------- LIGHT TIME ITERATION BEGINS --------------");
		    System.err.println("tau = " + tau + ", t - tau = " + (t - tau));
		}
		
		planet.getPosition(t - tau, QB);
		sun.getPosition(t - tau, SB);

		if (!silent) {
		    System.err.println("QB = " + QB.prettyPrint(format));
		    System.err.println("SB = " + SB.prettyPrint(format));
		}

		P.copy(QB);
		P.subtract(EB);

		Q.copy(QB);
		Q.subtract(SB);

		if (!silent) {
		    System.err.println("Q = " + Q.prettyPrint(format));
		    System.err.println("P = " + P.prettyPrint(format));
		}

		double PP = P.magnitude();
		double QQ = Q.magnitude();

		ctau = PP + factor * Math.log((EE + PP + QQ)/(EE - PP + QQ));

		double newtau = ctau/c;

		if (!silent)
		    System.err.println("new tau = " + newtau);

		dtau = newtau - tau;

		tau = newtau;
	    } while (Math.abs(dtau) > EPSILON);

	    if (!silent) {
		System.err.println("Light path = " + ctau);
		System.err.println();
		System.err.println("++++++++++++++ Light deflection ++++++++++++++");
	    }
	    
	    P.normalise();
	    Q.normalise();
	    E.normalise();
	    
	    if (!silent) {
		System.err.println("Normalised vectors:");
		System.err.println("P = " + P.prettyPrint(format));
		System.err.println("Q = " + Q.prettyPrint(format));
		System.err.println("E = " + E.prettyPrint(format));
	    }
	    
	    Vector pa = new Vector(E);
	    pa.multiplyBy(P.scalarProduct(Q));
	    
	    Vector pb = new Vector(Q);
	    pb.multiplyBy(E.scalarProduct(Q));
	    
	    pa.subtract(pb);
	    
	    double pfactor = (factor/EE)/(1.0 + Q.scalarProduct(E));
	    
	    pa.multiplyBy(pfactor);
	    
	    if (!silent)
		System.err.println("dP = " + pa.prettyPrint(format));
	    
	    P.add(pa);
	    
	    if (!silent) {
		System.err.println("New P = " + P.prettyPrint(format));
		System.err.println();
		System.err.println("++++++++++++++ Stellar aberration ++++++++++++++");
	    }
	    
	    Vector V = svEarth.getVelocity();
	    V.multiplyBy(1.0/c);
	    
	    double VV = V.magnitude();
	    
	    double beta = Math.sqrt(1.0 - VV * VV);
	    
	    double denominator = 1.0 + P.scalarProduct(V);
	    
	    double factora = beta/denominator;
	    
	    double factorb = (1.0 + P.scalarProduct(V)/(1.0 + beta))/denominator;
	    
	    P.multiplyBy(factora);
	    V.multiplyBy(factorb);
	    
	    P.add(V);
	    
	    if (!silent) {
		System.err.println("New P = " + P.prettyPrint(format));
	    }
	}
	catch (JPLEphemerisException jee) {
	    jee.printStackTrace();
	}
    }
}
