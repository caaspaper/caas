package de.uni_stuttgart.caas.testgui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.regex.Pattern;

import javax.swing.*;

import de.uni_stuttgart.caas.admin.AdminNode;

public class TestGuiForm extends JFrame {
	
	private final short DEFAULT_ADMIN_PORT = AdminNode.PORT_NUMBER;
	private final String DEFAULT_ADMIN_IP = "127.0.0.1";
	
	private final int WIDTH = 1000;
	private final int HEIGHT = 700;
	
	private JTextArea textArea;
	private ImprovedFormattedTextField numNodesField;
	private ImprovedFormattedTextField ipAdminField;

	public TestGuiForm() {
		setTitle("CaaS Admin GUI");
		
		final Insets insets = getInsets();
		setSize(WIDTH + insets.left + insets.right, HEIGHT + insets.top + insets.bottom);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		

		// null layout to keep SWING from screwing it all up
		setLayout(null);

		// console/stdout GUI
		textArea = new JTextArea();
		
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBounds(0, HEIGHT-250, WIDTH, 250);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getContentPane().add(scrollPane);
		
		//
		JButton btn = new JButton("Launch Admin Node");
		btn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Launching Admin Node");
			}
		});
		btn.setBounds(WIDTH-250, 100, 200, 50);
		getContentPane().add(btn);
	
		
		//
		btn = new JButton("Launch Nodes Locally");
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Launching " + numNodesField.getValue()  + " local nodes");
				System.out.println("Assuming admin is running at " + ipAdminField.getValue());
			}
		});
		btn.setBounds(WIDTH-250, 300, 200, 50);
		getContentPane().add(btn);
		
		JLabel label = new JLabel("Number of nodes to run");
		label.setBounds(10, 300, 200, 30);
		getContentPane().add(label);
		
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
        numNodesField = new ImprovedFormattedTextField( intFormat, 64 );
        numNodesField.setColumns( 20 );
        numNodesField.setBounds(220, 300, 200, 30);
        getContentPane().add(numNodesField);
        
        label = new JLabel("IP:Port of Admin Node");
		label.setBounds(10, 360, 200, 30);
		getContentPane().add(label);
		
		NumberFormat ipFormat = NumberFormat.getIntegerInstance();
		ipAdminField = new ImprovedFormattedTextField( new RegexFormatter(Pattern.compile(
        				"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."	+
        				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."	+
        				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."	+
        				"([01]?\\d\\d?|2[0-4]\\d|25[0-5])(:\\d{4,5})?$")) );
        
		ipAdminField.setValue(DEFAULT_ADMIN_IP + ":" + DEFAULT_ADMIN_PORT);
		
        ipAdminField.setColumns( 20 );
        ipAdminField.setBounds(220, 360, 200, 30);
        getContentPane().add(ipAdminField );
        
	}

	// this taken from
	// http://unserializableone.blogspot.de/2009/01/redirecting-systemout-and-systemerr-to.html
	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.append(text);
			}
		});
	}

	// this taken from
	// http://unserializableone.blogspot.de/2009/01/redirecting-systemout-and-systemerr-to.html
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				TestGuiForm form = new TestGuiForm();
				form.setVisible(true);
				
				form.redirectSystemStreams();
			}
		});
	}

}
