package de.uni_stuttgart.caas.testgui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.*;

import de.uni_stuttgart.caas.admin.AdminNode;
import de.uni_stuttgart.caas.cache.CacheNode;

public class TestGuiForm extends JFrame {

	private final short DEFAULT_ADMIN_PORT = AdminNode.DEFAULT_PORT_NUMBER;
	private final String DEFAULT_ADMIN_IP = "127.0.0.1";

	private final int DEFAULT_CAPACITY = AdminNode.DEFAULT_INITIAL_CAPACITY;

	private final int WIDTH = 1000;
	private final int HEIGHT = 700;

	private final JTextArea textArea;
	private final ImprovedFormattedTextField numNodesField;
	private final ImprovedFormattedTextField ipAdminField;

	private final ImprovedFormattedTextField adminPortField;
	private final ImprovedFormattedTextField adminCapacityField;
	
	private final JCheckBox adminDebuggerCheckBox;
	

	public TestGuiForm() {
		setTitle("CaaS Admin GUI");

		final Insets insets = getInsets();
		setSize(WIDTH + insets.left + insets.right, HEIGHT + insets.top
				+ insets.bottom);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);

		// null layout to keep SWING from screwing it all up
		setLayout(null);

		// console/stdout GUI
		textArea = new JTextArea();

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBounds(0, HEIGHT - 250, WIDTH, 250);
		scrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getContentPane().add(scrollPane);

		//
		JButton btn = new JButton("Launch Admin Node");
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Launching Admin Node");
				
				Runnable r = new Runnable() {
					
					@Override
					public void run() {
						int numOfNodes = Integer.parseInt(adminCapacityField.getText());
						new AdminNode(Integer.parseInt(adminPortField.getText()), numOfNodes);
					}
				};
				Thread t = new Thread(r);
				t.start();
			}
		});
		btn.setBounds(WIDTH - 250, 100, 200, 50);
		getContentPane().add(btn);

		JLabel label = new JLabel("Port to listen to");
		label.setBounds(30, 100, 200, 30);
		getContentPane().add(label);

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		intFormat.setGroupingUsed(false);

		adminPortField = new ImprovedFormattedTextField(intFormat,
				DEFAULT_ADMIN_PORT);
		adminPortField.setColumns(20);
		adminPortField.setBounds(240, 100, 200, 30);
		getContentPane().add(adminPortField);

		label = new JLabel("Grid node capacity");
		label.setBounds(30, 160, 200, 30);
		getContentPane().add(label);

		adminCapacityField = new ImprovedFormattedTextField(intFormat,
				DEFAULT_CAPACITY);
		adminCapacityField.setColumns(20);
		adminCapacityField.setBounds(240, 160, 200, 30);
		getContentPane().add(adminCapacityField);
		
		adminDebuggerCheckBox = new JCheckBox("Attach as debugger");
		adminDebuggerCheckBox.setBounds(490, 160, 200, 30);
		adminDebuggerCheckBox.setSelected(true);
		getContentPane().add(adminDebuggerCheckBox);
		
		// node configuration

		btn = new JButton("Launch Nodes Locally");
		btn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Launching " + numNodesField.getValue()
						+ " local nodes");
				
				// CacheNode constructors wait for the admin to be available, so we have
				// to start them on a separate thread that only serves to construct them.
				// Once the CacheNode's are all constructed, it dies.
				Runnable r = new Runnable() {
					
					@Override
					public void run() {
						int numOfNodes = Integer.parseInt(numNodesField.getText());
						for (int i = 0; i < numOfNodes; i++) {
							try {
								final String[] cache_args = ipAdminField.getText().split(":"); 
								//ipAdminField validation regex allows port to be optional
								final String port = cache_args[0].length()==2 ? cache_args[0] : adminPortField.getText();
								new CacheNode(cache_args[0], Integer.parseInt(port));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				};
				Thread t = new Thread(r);
				t.start();
			}
		});
		btn.setBounds(WIDTH - 250, 300, 200, 50);
		getContentPane().add(btn);

		label = new JLabel("Number of nodes to run");
		label.setBounds(30, 300, 200, 30);
		getContentPane().add(label);

		numNodesField = new ImprovedFormattedTextField(intFormat, 64);
		numNodesField.setColumns(20);
		numNodesField.setBounds(240, 300, 200, 30);
		getContentPane().add(numNodesField);

		label = new JLabel("IP:Port of Admin Node");
		label.setBounds(30, 360, 200, 30);
		getContentPane().add(label);

		NumberFormat ipFormat = NumberFormat.getIntegerInstance();
		ipAdminField = new ImprovedFormattedTextField(new RegexFormatter(
				Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(:\\d{4,5})?$")));

		ipAdminField.setValue(DEFAULT_ADMIN_IP + ":" + DEFAULT_ADMIN_PORT);

		ipAdminField.setColumns(20);
		ipAdminField.setBounds(240, 360, 200, 30);
		getContentPane().add(ipAdminField);

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
