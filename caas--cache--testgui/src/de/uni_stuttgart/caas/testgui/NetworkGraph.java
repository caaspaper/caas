package de.uni_stuttgart.caas.testgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import de.uni_stuttgart.caas.base.NodeInfo;
import delaunay.Point;
import delaunay.Segment;

public class NetworkGraph extends JFrame {
	
	private double scale = 0.0000005;

	private JPanel contentPane;
	private List<NodeInfo> nodes;
	private List<Segment> segments;
	/**
	 * Create the frame.
	 */
	public NetworkGraph(List<NodeInfo> nodes, List<Segment> segments) {
		
		final Network n = new Network();
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		this.nodes = nodes;
		this.segments = segments;
		JPanel buttonPannel = new JPanel(new FlowLayout());
		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		});
		buttonPannel.add(exitButton);
		
		buttonPannel.add(new JLabel("scale:"));
		final JSlider slider = new JSlider(JSlider.HORIZONTAL);
		slider.setMaximum(10);
		slider.setMinimum(1);
		slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent arg0) {
				scale = (double) slider.getValue() / 20000000;
				n.repaint();
			}
		});
		buttonPannel.add(slider);
		
		contentPane.add(buttonPannel, BorderLayout.NORTH);
		contentPane.add(n, BorderLayout.CENTER);
	}
	
	private class Network extends JPanel {
		
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			paintNetwork(g);
		}
	
		private void paintNetwork(Graphics g) {
			for (NodeInfo n : nodes) {
				drawCircle(g, (int)(n.getLocationOfNode().getIX() * scale), (int)(n.getLocationOfNode().getIY() * scale), 5, Color.BLUE);
			}
			for (Segment s : segments) {
				List<Point> points = s.getPoints();
				g.drawLine((int)(points.get(0).getIX() * scale), (int)(points.get(0).getIY() * scale), (int)(points.get(1).getIX() * scale), (int)(points.get(1).getIY() * scale));
			}
		}
		
		private void drawCircle(Graphics g, int x, int y, int r, Color c) {
			Color previous = g.getColor();
			g.setColor(c);
			g.fillOval(x - r, y - r, 2 * r, 2 * r);
			g.setColor(previous);
		}
		
	}
}
