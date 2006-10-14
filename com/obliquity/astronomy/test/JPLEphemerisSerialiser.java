package com.obliquity.astronomy.test;

import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

import com.obliquity.astronomy.*;

public class JPLEphemerisSerialiser {

    public static void main(String[] args) {
	if (args.length < 4) {
	    System.err.println("Usage: JPLEphemerisSerialiser filename start-date end-date outputfile");
	    System.exit(1);
	}

	String filename = args[0];

	double jdstart = Double.parseDouble(args[1]);
	double jdfinis = Double.parseDouble(args[2]);

	String outputfilename = args[3];

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

	try {
	    FileOutputStream ostream = new FileOutputStream(outputfilename);
	    ObjectOutputStream oos = new ObjectOutputStream(ostream);	
	    oos.writeObject(ephemeris);
	    oos.close();
	}
        catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("IOException when serialising ephemeris ... " + ioe);
 	    System.exit(1);
	}
    }
}
