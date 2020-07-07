/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2018 David Harper at obliquity.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 */

package com.obliquity.astronomy.almanac.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class InferiorPlanetApparitionPanel extends JPanel {	
	private double maxAltitude = 0.0, minAzimuth = Double.MAX_VALUE, maxAzimuth = -Double.MAX_VALUE;
	
	public InferiorPlanetApparitionPanel(InferiorPlanetApparitionData[] data) {
		for (InferiorPlanetApparitionData ipaData : data) {
			double altitude = ipaData.horizontalCoordinates.altitude;
			double azimuth = ipaData.horizontalCoordinates.azimuth;
			
			if (altitude > maxAltitude)
				maxAltitude = altitude;
			
			if (azimuth > maxAzimuth)
				maxAzimuth = azimuth;
			
			if (azimuth < minAzimuth)
				minAzimuth = azimuth;
		}
		
		System.err.println("maxAltitude = " + maxAltitude + " ; minAzimuth = " + minAzimuth + " ; maxAzimuth = " + maxAzimuth);
		
		setBackground(Color.WHITE);
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(1000, 800);
	}
	
	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		
		Graphics2D g = (Graphics2D)(gr.create());

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		
		g.setColor(Color.BLACK);
	
		double leftMargin = 20.0, rightMargin = 20.0, topMargin = 20.0, bottomMargin = 20.0;
		
		double boxWidth = 800.0, boxHeight = 700.0;
		
		double x = leftMargin, y = topMargin;
		
		Shape box = new Rectangle2D.Double(x, y, boxWidth, boxHeight);
		
		g.draw(box);
		
		g.dispose();
	}
}
