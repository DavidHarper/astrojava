import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

import com.obliquity.astronomy.*;

public class JPLReader {
    private static String []planetNames = {"Mercury", "Venus", "EMB", "Mars", "Jupiter", "Saturn",
					   "Uranus", "Neptune", "Pluto", "Moon", "Sun", "Nutations",
					   "Librations"};

    public static void main(String[] args) {
	if (args.length < 3) {
	    System.err.println("Usage: JPLReader filename start-date end-date");
	    System.exit(1);
	}

	String filename = args[0];

	double jdstart = Double.parseDouble(args[1]);
	double jdfinis = Double.parseDouble(args[2]);

	int nTests = 10;

	if (args.length > 3)
	    nTests = Integer.parseInt(args[3]);

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

	testEphemeris(ephemeris);

	System.out.println();
	System.out.println("### SERIALISING EPHEMERIS ###");

	try {
	    FileOutputStream ostream = new FileOutputStream("ephemeris.ser");
	    ObjectOutputStream oos = new ObjectOutputStream(ostream);	
	    oos.writeObject(ephemeris);
	    oos.close();
	}
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("IOException when serialising ephemeris ... " + ioe);
 	    System.exit(1);
	}

	System.out.println();
	System.out.println("### DESERIALISING EPHEMERIS ###");

	JPLEphemeris ephemeris2 = null;

	try {
	    FileInputStream istream = new FileInputStream("ephemeris.ser");
	    ObjectInputStream ois = new ObjectInputStream(istream);
	    ephemeris2 = (JPLEphemeris)ois.readObject();
	    ois.close();
	}
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("IOException when de serialising ephemeris ... " + ioe);
 	    System.exit(1);
	}
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            System.err.println("ClassNotFoundException when de serialising ephemeris ... " + cnfe);
 	    System.exit(1);
	}

	System.out.println();
	System.out.println("### TESTING NEW EPHEMERIS ###");

	testEphemeris(ephemeris2);

	compareEphemerides(ephemeris, ephemeris2, nTests);

	System.exit(0);
    }

    public static void testEphemeris(JPLEphemeris ephemeris) {
	if (ephemeris == null) {
	    System.out.println("EPHEMERIS OBJECT IS NULL");
	    return;
	}

	double tEarliest = ephemeris.getEarliestDate();
	double tLatest =   ephemeris.getLatestDate();
	System.out.println("Ephemeris has number " + ephemeris.getEphemerisNumber());
	System.out.println("Date range is " + tEarliest + " to " + tLatest);

	System.out.println("The ephemeris has " + ephemeris.getNumberOfDataRecords() +
			   " records, each of length " + ephemeris.getLengthOfDataRecord());

	System.out.println();

	System.out.println("The ephemeris has the following components:");
	for (int i = 0; i < planetNames.length; i++)
	    if (ephemeris.hasComponent(i))
		System.out.println("[" + i + "] " + planetNames[i]);

	System.out.println();

	System.out.println("AU    = " + ephemeris.getAU());
	System.out.println("EMRAT = " + ephemeris.getEMRAT());
    }

    public static void compareEphemerides(JPLEphemeris eph1, JPLEphemeris eph2, int nTests) {
	if (eph1 == null) {
	    System.err.println("First ephemeris object is null");
	    return;
	}

	if (eph2 == null) {
	    System.err.println("Second ephemeris object is null");
	    return;
	}

	if (eph1 == eph2)  {
	    System.err.println("Ephemeris objects are IDENTICAL");
	    return;
	}

	Random random = new Random();
	
	double []pos1 = new double[3];
	double []vel1 = new double[3];
	double []pos2 = new double[3];
	double []vel2 = new double[3];

	double tEarliest = Math.max(eph1.getEarliestDate(), eph2.getEarliestDate());
	double tLatest   = Math.min(eph1.getLatestDate(), eph2.getLatestDate());

	if (tEarliest >= tLatest) {
	    System.err.println("Date ranges of ephemerides do not overlap");
	    return;
	}

	double tSpan = tLatest - tEarliest;

	File outfile = new File("testeval.in");
	FileOutputStream fos;
	PrintWriter pw = null;
;
	try {
	    fos = new FileOutputStream(outfile);
	    pw = new PrintWriter(fos);
	}
	catch (FileNotFoundException fnfe) {
	    fnfe.printStackTrace();
	    System.err.println("FileNotFoundException ... " + fnfe);
	    System.exit(1);
	}


	for (int j = 0; j < nTests; j++) {
	    int nBody = random.nextInt(JPLEphemeris.SUN);
	    double t = tEarliest + tSpan * random.nextDouble();

	    try {
		eph1.calculatePositionAndVelocity(t, nBody, pos1, vel1);
	    }
	    catch (JPLEphemerisException jee) {
		jee.printStackTrace();
		System.err.println("JPLEphemerisException from first ephemeris object ... " + jee);
		System.exit(1);
	    }

	    try {
		eph2.calculatePositionAndVelocity(t, nBody, pos2, vel2);
	    }
	    catch (JPLEphemerisException jee) {
		jee.printStackTrace();
		System.err.println("JPLEphemerisException from second ephemeris object ... " + jee);
		System.exit(1);
	    }

	    pw.println(nBody + " " + t + " " + pos1[0] + " " + pos1[1] + " " + pos1[2] + " " +
		       vel1[0] + " " + vel1[1] + " " + vel1[2]);

	    double dp = 0.0;
	    double dv = 0.0;
	    for (int k = 0; k < 3; k++) {
		double dx = pos2[k] - pos1[k];
		double dvx = vel2[k] - vel1[k];
		dp += dx * dx;
		dv += dvx * dvx;
	    }
	    dp = Math.sqrt(dp);
	    dv = Math.sqrt(dv);
	    System.out.println("Test #" + j + " : " + planetNames[nBody] + " DP = " + dp +
			       ", DV = " + dv);
        }

	pw.close();
    }
}
