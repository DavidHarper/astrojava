import com.obliquity.astronomy.*;

import java.io.*;
import java.text.*;
import java.lang.*;

public class TestApparentPlace {
    private final static double EPSILON = 1.0e-9;

    public static void main(String args[]) {
	if (args.length < 5) {
	    System.err.println("Usage: TestApparentPlace filename kBody startdate enddate step");
	    System.exit(1);
	}

	String filename = args[0];

	int kBody = Integer.parseInt(args[1]);
	double jdstart = Double.parseDouble(args[2]);
	double jdfinish = Double.parseDouble(args[3]);
	double jdstep = Double.parseDouble(args[4]);

	JPLEphemeris ephemeris = null;

	try {
	    ephemeris = new JPLEphemeris(filename, jdstart - 1.0, jdfinish + 1.0);
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

	PrintStream ps = silent ? null : System.err;

	try {
	    for (double t = jdstart; t <= jdfinish; t += jdstep) {
		calculateApparentPlace(earth, planet, sun, erm, t, ap, ps);
		displayApparentPlace(t, ap, System.out);
	    }
	}
	catch (JPLEphemerisException jplee) {
	    jplee.printStackTrace();
	}
    }

    private static final DecimalFormat dfmt = new DecimalFormat("00.00");
    private static final DecimalFormat ifmt = new DecimalFormat("00");
    private static final DecimalFormat dfmt2 = new DecimalFormat("0.000000000");

    private static void displayApparentPlace(double t, ApparentPlace ap, PrintStream ps) {
	double ra = ap.getRightAscension() * 12.0/Math.PI;
	double dec = ap.getDeclination() * 180.0/Math.PI;
	char decsign = (dec < 0.0) ? 'S' : 'N';
	if (ra < 0.0)
	    ra += 24.0;
	if (dec < 0.0)
	    dec = -dec;

	int rah  = (int)ra;
	ra -= (double)rah;
	ra *= 60.0;
	int ram = (int)ra;
	ra -= (double)ram;
	ra *= 60.0;
	
	int decd = (int)dec;
	dec -= (double)decd;
	dec *= 60.0;
	int decm = (int)dec;
	dec -= (double)decm;
	dec *= 60.0;
	
	ps.print(dfmt.format(t));
	ps.print("  ");
	ps.print(ifmt.format(rah) + " " + ifmt.format(ram) + " " + dfmt.format(ra));
	ps.print("  ");
	ps.print(decsign + " " + ifmt.format(decd) + " " + ifmt.format(decm) + " " + dfmt.format(dec));
	ps.print("  ");
	ps.print(dfmt2.format(ap.getGeometricDistance()));
	ps.print("  ");
	ps.print(dfmt2.format(ap.getLightPathDistance()));
	ps.println();
    }

    private static void calculateApparentPlace(MovingPoint observer, MovingPoint target,
					       MovingPoint sun, EarthRotationModel erm,
					       double t, ApparentPlace ap,
					       PrintStream ps) throws JPLEphemerisException {
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

	if (ps != null) {
	    ps.println("============== APPARENT PLACE ==============");
	    ps.println("t = " + t);
	}

	svObserver = observer.getStateVector(t);
	EB = svObserver.getPosition();

	if (sun != null)
	    sun.getPosition(t, SB);

	if (ps != null) {
	    ps.println("EB = " + EB.prettyPrint(format));
	    ps.println("SB = " + SB.prettyPrint(format));
	}

	E.copy(EB);
	E.subtract(SB);

	if (ps != null)
	    ps.println("E = " + E.prettyPrint(format));

	double EE = E.magnitude();

	double dtau;
	double ctau;
	double gd = 0.0;

	double tau = 0.0;

	do {
	    if (ps != null) {
		ps.println("-------------- LIGHT TIME ITERATION BEGINS --------------");
		ps.println("tau = " + tau + ", t - tau = " + (t - tau));
	    }
	    
	    target.getPosition(t - tau, QB);

	    if (sun != null)
		sun.getPosition(t - tau, SB);

	    if (ps != null) {
		ps.println("QB = " + QB.prettyPrint(format));
		ps.println("SB = " + SB.prettyPrint(format));
	    }

	    P.copy(QB);
	    P.subtract(EB);

	    Q.copy(QB);
	    Q.subtract(SB);

	    if (ps != null) {
		ps.println("Q = " + Q.prettyPrint(format));
		ps.println("P = " + P.prettyPrint(format));
	    }
	    
	    double PP = P.magnitude();
	    double QQ = Q.magnitude();

	    if (tau == 0.0)
		gd = PP;

	    ctau = PP + factor * Math.log((EE + PP + QQ)/(EE - PP + QQ));

	    double newtau = ctau/c;

	    if (ps != null)
		ps.println("new tau = " + newtau);

	    dtau = newtau - tau;
	    
	    tau = newtau;
	} while (Math.abs(dtau) > EPSILON);

	if (ps != null) {
	    ps.println("Light path = " + ctau);
	    ps.println();
	    if (sun != null)
		ps.println("++++++++++++++ Light deflection ++++++++++++++");
	}
	
	P.normalise();
	Q.normalise();
	E.normalise();
	    
	if (ps != null) {
	    ps.println("Normalised vectors:");
	    ps.println("P = " + P.prettyPrint(format));
	    ps.println("Q = " + Q.prettyPrint(format));
	    ps.println("E = " + E.prettyPrint(format));
	}

	if (sun != null) {
	    Vector pa = new Vector(E);
	    pa.multiplyBy(P.scalarProduct(Q));
	    
	    Vector pb = new Vector(Q);
	    pb.multiplyBy(E.scalarProduct(P));
	    
	    pa.subtract(pb);
	    
	    double pfactor = (factor/EE)/(1.0 + Q.scalarProduct(E));
	    
	    pa.multiplyBy(pfactor);
	    
	    if (ps != null)
		ps.println("dP = " + pa.prettyPrint(format));
	    
	    P.add(pa);
	}
	    
	if (ps != null) {
	    if (sun != null) {
		ps.println("New P = " + P.prettyPrint(format));
		ps.println();
	    }
	    ps.println("++++++++++++++ Stellar aberration ++++++++++++++");
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
	    
	if (ps != null) {
	    ps.println("New P = " + P.prettyPrint(format));
	    ps.println();
	    ps.println("++++++++++++++ Precession and nutation ++++++++++++++");
	}

	Matrix precess = erm.precessionMatrix(target.getEpoch(), t);
	Matrix nutate = erm.nutationMatrix(t);

	if (ps != null) {
	    ps.println("Precession matrix:\n" + precess.prettyPrint(format));
	    ps.println("Nutation matrix:\n" + nutate.prettyPrint(format));
	}

	P.multiplyBy(precess);
	P.multiplyBy(nutate);

	double x = P.getX();
	double y = P.getY();
	double z = P.getZ();

	double ra = Math.atan2(y, x);
	double dec = Math.atan2(z, Math.sqrt(x * x + y * y));

	if (ps != null) {
	    ps.println("Apparent P = " + P.prettyPrint(format));
	}

	ap.setRightAscensionAndDeclination(ra, dec);
	ap.setGeometricDistance(gd);
	ap.setLightPathDistance(ctau);
    }
}
