package com.obliquity.astronomy.almanac.test;

/*
 * astrojava - a package for reading JPL ephemeris files
 *
 * Copyright (C) 2006-2023 David Harper at obliquity.com
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.*;

import javax.swing.*;
import javax.swing.border.*;

import com.obliquity.astronomy.almanac.*;

@SuppressWarnings("serial")
public class TestPrecessionAndNutation extends JPanel {
	public TestPrecessionAndNutation() {
		super(null);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		PositionInputPanel ppa = new PositionInputPanel("Epoch A");
		PositionOutputPanel ppb = new PositionOutputPanel("Epoch B");

		JButton btnAtoB = new JButton("Precess A to B");
		
		btnAtoB.addActionListener(new Precessor(ppa, ppb));

		add(ppa);
		add(btnAtoB);
		add(ppb);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Test Precession");

		frame.getContentPane().add(new TestPrecessionAndNutation());
		frame.pack();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(true);
	}

	class PositionInputPanel extends JPanel {
		protected String[] strEpoch = { "J", "B" };
		protected JComboBox<String> cbxEpoch = new JComboBox<String>(strEpoch);
		protected JTextField txtEpoch;

		protected JTextField rah, ram, ras, decd, decm, decs;

		protected String[] strDecNS = { "N", "S" };
		protected JComboBox<String> cbxDecNS = new JComboBox<String>(strDecNS);

		protected NumberFormat f5_2 = NumberFormat.getInstance();
		protected DecimalFormat i2 = new DecimalFormat("00");

		public PositionInputPanel(String caption) {
			super(null);

			f5_2.setMaximumFractionDigits(2);
			f5_2.setMinimumFractionDigits(2);

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();

			setLayout(gbl);

			gbc.ipadx = 5;
			gbc.insets = new Insets(0, 0, 0, 0);

			JLabel label = new JLabel("Epoch");
			gbc.gridwidth = 1;
			add(label, gbc);

			add(cbxEpoch, gbc);
			txtEpoch = new JTextField(6);

			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(txtEpoch, gbc);

			label = new JLabel("RA");
			gbc.gridwidth = 1;
			add(label, gbc);

			rah = new JTextField(2);
			add(rah, gbc);

			add(new JLabel("h"), gbc);

			ram = new JTextField(2);
			add(ram, gbc);

			add(new JLabel("m"), gbc);

			ras = new JTextField(5);
			add(ras, gbc);

			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(new JLabel("s"), gbc);

			label = new JLabel("Dec");
			gbc.gridwidth = 1;
			gbc.insets = new Insets(0, 0, 0, 0);
			add(label, gbc);

			decd = new JTextField(2);
			add(decd, gbc);

			add(new JLabel("\u00b0"), gbc);

			decm = new JTextField(2);
			add(decm, gbc);

			add(new JLabel("'"), gbc);

			decs = new JTextField(5);
			add(decs, gbc);

			add(new JLabel("\""), gbc);

			gbc.gridwidth = GridBagConstraints.REMAINDER;

			add(cbxDecNS, gbc);

			Border loweredetched1 = BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED);
			Border title1 = BorderFactory.createTitledBorder(loweredetched1,
					caption);
			setBorder(title1);
		}

		public double getRA() {
			return Double.parseDouble(rah.getText())
					+ Double.parseDouble(ram.getText()) / 60.0
					+ Double.parseDouble(ras.getText()) / 3600.0;
		}

		public void setRA(double ra) {
			int i = (int) ra;
			rah.setText(i2.format(i));

			ra = 60.0 * (ra - i);
			i = (int) ra;
			ram.setText(i2.format(i));

			ra = 60.0 * (ra - i);
			ras.setText(f5_2.format(ra));
		}

		public double getDec() {
			double dec = Double.parseDouble(decd.getText())
					+ Double.parseDouble(decm.getText()) / 60.0
					+ Double.parseDouble(decs.getText()) / 3600.0;

			String str = (String) cbxDecNS.getSelectedItem();

			if (str.equalsIgnoreCase("S"))
				dec = -dec;

			return dec;
		}

		public void setDec(double dec) {
			if (dec < 0.0) {
				cbxDecNS.setSelectedIndex(1);
				dec = -dec;
			} else
				cbxDecNS.setSelectedIndex(0);

			int i = (int) dec;
			decd.setText(i2.format(i));

			dec = 60.0 * (dec - i);
			i = (int) dec;
			decm.setText(i2.format(i));

			dec = 60.0 * (dec - i);
			decs.setText(f5_2.format(dec));
		}

		public double getEpoch() {
			return Double.parseDouble(txtEpoch.getText());
		}

		public char getEpochType() {
			switch (cbxEpoch.getSelectedIndex()) {
			case 0:
				return 'J';

			case 1:
				return 'B';

			default:
				return '?';
			}
		}
	}

	class PositionOutputPanel extends JPanel {
		protected String[] strEpoch = { "J", "B" };
		protected JComboBox<String> cbxEpoch = new JComboBox<String>(strEpoch);
		protected JTextField txtEpoch;

		protected JLabel raMean, decMean, raTrue, decTrue;

		protected NumberFormat f5_2 = NumberFormat.getInstance();
		protected DecimalFormat i2 = new DecimalFormat("00");

		public PositionOutputPanel(String caption) {
			super(null);

			f5_2.setMaximumFractionDigits(2);
			f5_2.setMinimumFractionDigits(2);

			GridBagLayout gbl = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();

			setLayout(gbl);

			gbc.ipadx = 5;
			gbc.insets = new Insets(0, 0, 0, 0);

			JLabel label = new JLabel("Epoch");
			gbc.gridwidth = 1;
			add(label, gbc);

			add(cbxEpoch, gbc);
			txtEpoch = new JTextField(6);

			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(txtEpoch, gbc);

			label = new JLabel("Mean RA");
			gbc.gridwidth = 1;
			add(label, gbc);

			raMean = new JLabel("  ");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(raMean, gbc);


			label = new JLabel("Mean Dec");
			gbc.gridwidth = 1;
			gbc.insets = new Insets(0, 0, 0, 0);
			add(label, gbc);

			decMean = new JLabel("  ");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(decMean, gbc);

			label = new JLabel("True RA");
			gbc.gridwidth = 1;
			add(label, gbc);

			raTrue = new JLabel("  ");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(raTrue, gbc);

			label = new JLabel("True Dec");
			gbc.gridwidth = 1;
			gbc.insets = new Insets(0, 0, 0, 0);
			add(label, gbc);

			decTrue = new JLabel("  ");
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			add(decTrue, gbc);

			Border loweredetched1 = BorderFactory
					.createEtchedBorder(EtchedBorder.LOWERED);
			Border title1 = BorderFactory.createTitledBorder(loweredetched1,
					caption);
			setBorder(title1);
		}


		private void setRA(JLabel label, double ra) {
			int rah = (int) ra;

			ra = 60.0 * (ra - rah);
			int ram = (int) ra;

			ra = 60.0 * (ra - ram);
			
			label.setText(String.format("%02d %02d %5.2f", rah, ram, ra));
		}


		private void setDec(JLabel label, double dec) {
			String decSign = dec < 0.0 ? "S" : "N";
			
			dec = Math.abs(dec);

			int decd = (int) dec;

			dec = 60.0 * (dec - decd);
			int decm = (int) dec;

			dec = 60.0 * (dec - decm);
			
			label.setText(String.format("%02d %02d %5.2f %s", decd, decm, dec, decSign));
		}
		
		public void setMeanRA(double ra) {
			setRA(raMean, ra);
		}
		
		public void setMeanDec(double dec) {
			setDec(decMean, dec);
		}
		
		public void setTrueRA(double ra) {
			setRA(raTrue, ra);
		}
		
		public void setTrueDec(double dec) {
			setDec(decTrue, dec);
		}

		public double getEpoch() {
			return Double.parseDouble(txtEpoch.getText());
		}

		public char getEpochType() {
			switch (cbxEpoch.getSelectedIndex()) {
			case 0:
				return 'J';

			case 1:
				return 'B';

			default:
				return '?';
			}
		}
	}

	class Precessor implements ActionListener {
		PositionInputPanel pa;
		PositionOutputPanel pb;
		IAUEarthRotationModel erm = new IAUEarthRotationModel();

		public Precessor(PositionInputPanel pa, PositionOutputPanel pb) {
			this.pa = pa;
			this.pb = pb;
		}

		public void actionPerformed(ActionEvent e) {
			precess();
		}

		protected void precess() {
			double ra1 = pa.getRA() * Math.PI / 12.0;
			double dec1 = pa.getDec() * Math.PI / 180.0;
			
			Vector v = new Vector(Math.cos(dec1)*Math.cos(ra1),
						Math.cos(dec1)*Math.sin(ra1), Math.sin(dec1));

			double epoch1 = pa.getEpoch();
			char etype1 = pa.getEpochType();

			switch (etype1) {
			case 'J':
				epoch1 = erm.JulianEpoch(epoch1);
				break;

			case 'B':
				epoch1 = erm.BesselianEpoch(epoch1);
			}

			double epoch2 = pb.getEpoch();
			char etype2 = pb.getEpochType();

			switch (etype2) {
			case 'J':
				epoch2 = erm.JulianEpoch(epoch2);
				break;

			case 'B':
				epoch2 = erm.BesselianEpoch(epoch2);
			}
			
			Matrix pm = erm.precessionMatrix(epoch1, epoch2);
			
			v.multiplyBy(pm);
			
			double x = v.getX();
			double y = v.getY();
			double z = v.getZ();
			
			double ra2 = Math.atan2(y, x) * 12.0/Math.PI;
			if (ra2 < 0.0)
				ra2 += 24.0;
			
			double cd = Math.sqrt(x*x + y*y);
			
			double dec2 = Math.atan2(z, cd) * 180.0/Math.PI;
			
			pb.setMeanRA(ra2);
			pb.setMeanDec(dec2);
			
			Matrix nm = erm.nutationMatrix(epoch2);
			
			v.multiplyBy(nm);
			
			x = v.getX();
			y = v.getY();
			z = v.getZ();
			
			double ra3 = Math.atan2(y, x) * 12.0/Math.PI;
			if (ra3 < 0.0)
				ra3 += 24.0;
			
			cd = Math.sqrt(x*x + y*y);
			
			double dec3 = Math.atan2(z, cd) * 180.0/Math.PI;
			
			pb.setTrueRA(ra3);
			pb.setTrueDec(dec3);
		}
	}
}
