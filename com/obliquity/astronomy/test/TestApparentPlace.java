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

	boolean nosun = Boolean.getBoolean("nosun");

	PlanetCentre planet = new PlanetCentre(ephemeris, kBody);
	EarthCentre earth = new EarthCentre(ephemeris);

	PlanetCentre sun = null;

	if (!nosun) 
	    sun = (kBody == JPLEphemeris.SUN) ? 
		planet : new PlanetCentre(ephemeris, JPLEphemeris.SUN);

	double tEarliest = ephemeris.getEarliestDate() + 1.0;
	double tLatest   = ephemeris.getLatestDate() - 1.0;
	double tSpan = tLatest - tEarliest;

	boolean silent = Boolean.getBoolean("silent");

	ApparentPlace ap = new ApparentPlace();

	EarthRotationModel erm = new IAUEarthRotationModel();

	try {
	    calculateApparentPlace(earth, planet, sun, erm, t, ap, silent);
	}
	catch (JPLEphemerisException jplee) {
	    jplee.printStackTrace();
	}
    }

    private static void calculateApparentPlace(MovingPoint observer, MovingPoint target,
					       MovingPoint sun, EarthRotationModel erm,
					       double t, ApparentPlace ap,
					       boolean silent) throws JPLEphemerisException {
	DecimalFormat format = new DecimalFormat("0.000000000");
	format.setPositivePrefix(" ");

	StateVector svObserver = null;
	Vector EB = null;
	Vector SB = new Vector();
	Vector QB = new Vector();

	Vector P = new Vector();
	Vector Q = new Vector();
	Vector E = new Vector();

	double c = 173.1446;
	double factor = 2.0 * 9.87e-9;

	if (!silent) {
	    System.err.println("============== APPARENT PLACE ==============");
	    System.err.println("t = " + t);
	}

	svObserver = observer.getStateVector(t);
	EB = svObserver.getPosition();

	if (sun != null)
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
	double gd = 0.0;

	double tau = 0.0;

	do {
	    if (!silent) {
		System.err.println("-------------- LIGHT TIME ITERATION BEGINS --------------");
		System.err.println("tau = " + tau + ", t - tau = " + (t - tau));
	    }
	    
	    target.getPosition(t - tau, QB);

	    if (sun != null)
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

	    if (tau == 0.0)
		gd = PP;

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
	    if (sun != null)
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

	if (sun != null) {
	    Vector pa = new Vector(E);
	    pa.multiplyBy(P.scalarProduct(Q));
	    
	    Vector pb = new Vector(Q);
	    pb.multiplyBy(E.scalarProduct(P));
	    
	    pa.subtract(pb);
	    
	    double pfactor = (factor/EE)/(1.0 + Q.scalarProduct(E));
	    
	    pa.multiplyBy(pfactor);
	    
	    if (!silent)
		System.err.println("dP = " + pa.prettyPrint(format));
	    
	    P.add(pa);
	}
	    
	if (!silent) {
	    if (sun != null) {
		System.err.println("New P = " + P.prettyPrint(format));
		System.err.println();
	    }
	    System.err.println("++++++++++++++ Stellar aberration ++++++++++++++");
	}
	    
	Vector V = svObserver.getVelocity();
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
	    System.err.println();
	    System.err.println("++++++++++++++ Precession and nutation ++++++++++++++");
	}

	Matrix precess = erm.precessionMatrix(target.getEpoch(), t);
	Matrix nutate = erm.nutationMatrix(t);

	if (!silent) {
	    System.err.println("Precession matrix:\n" + precess.prettyPrint(format));
	    System.err.println("Nutation matrix:\n" + nutate.prettyPrint(format));
	}

	P.multiplyBy(precess);
	P.multiplyBy(nutate);

	double x = P.getX();
	double y = P.getY();
	double z = P.getZ();

	double ra = Math.atan2(y, x);
	double dec = Math.atan2(x, Math.sqrt(x * x + y * y));

	if (!silent) {
	    System.err.println("Apparent P = " + P.prettyPrint(format));
	}

	ap.setRightAscensionAndDeclination(ra, dec);
	ap.setGeometricDistance(gd);
	ap.setLightPathDistance(ctau);
    }
}
