import com.obliquity.astronomy.*;

import java.io.*;
import java.text.*;
import java.lang.*;
import java.util.Random;

public class TestApparentPlace {
    private final static double EPSILON = 1.0e-9;

    public static void main(String args[]) {
	if (args.length < 5) {
	    System.err.println("Usage: TestApparentPlace filename kBody start-date end-date nTests");
	    System.exit(1);
	}

	DecimalFormat format = new DecimalFormat("0.000000");
	format.setPositivePrefix(" ");

	String filename = args[0];

	int kBody = Integer.parseInt(args[1]);
	double jdstart = Double.parseDouble(args[2]);
	double jdfinis = Double.parseDouble(args[3]);
	int nTests = Integer.parseInt(args[4]);

	String outputfilename = args[3];

	JPLEphemeris ephemeris = null;

	Random random = new Random();

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

	Vector EB = new Vector();
	Vector SB = new Vector();
	Vector QB = new Vector();

	Vector P = new Vector();
	Vector Q = new Vector();
	Vector E = new Vector();

	double c = 173.1446 * ephemeris.getAU();
	double factor = 2.0 * 9.87e-9;

	for (int j = 0; j < nTests; j++) {
	    double t = tEarliest + tSpan * random.nextDouble();

	    if (!silent) {
		System.err.println("============== NEXT DATE ==============");
		System.err.println("t = " + t);
	    }

	    try {
		double tau = 0.0;

		earth.getPosition(t, EB);
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

		    double ctau = PP + factor * Math.log((EE + PP + QQ)/(EE - PP + QQ));

		    double newtau = ctau/c;

		    if (!silent)
			System.err.println("new tau = " + newtau);

		    dtau = newtau - tau;

		    tau = newtau;
		} while (Math.abs(dtau) > EPSILON);

	    }
	    catch (JPLEphemerisException jee) {
		jee.printStackTrace();
	    }
	}
    }
}
