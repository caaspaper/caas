package de.uni_stuttgart.caas.testgui;

import gui.TriangulationDrawer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import delaunay_triangulation.Triangle_dt;

public class NetworkGraph extends JFrame {

	private JPanel contentPane;
	private TriangulationDrawer drawer;
	private Vector<Triangle_dt> triangles;

	/**
	 * Create the frame.
	 */
	public NetworkGraph(Vector<Triangle_dt> triangles) {

		this.triangles = triangles;
		final Network n = new Network();
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		drawer = new TriangulationDrawer(800, 800);
		JMenu networkMenu = new JMenu("NetworkGraph");
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		});

		JMenuBar menuBar = new JMenuBar();
		networkMenu.add(exit);
		menuBar.add(networkMenu);
		contentPane.add(n, BorderLayout.CENTER);
		this.setJMenuBar(menuBar);
	}

	private class Network extends JPanel {

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			drawer.drawTriangulation(g, triangles);
		}

		private void drawCircle(Graphics g, int x, int y, int r, Color c) {
			Color previous = g.getColor();
			g.setColor(c);
			g.fillOval(x - r, y - r, 2 * r, 2 * r);
			g.setColor(previous);
		}

	}
}
