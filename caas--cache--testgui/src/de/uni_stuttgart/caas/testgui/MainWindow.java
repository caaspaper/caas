package de.uni_stuttgart.caas.testgui;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import de.uni_stuttgart.caas.admin.AdminNode;
import de.uni_stuttgart.caas.cache.CacheNode;
import de.uni_stuttgart.caas.testgui.ImprovedFormattedTextField;
import de.uni_stuttgart.caas.testgui.RegexFormatter;

public class MainWindow {

	private JFrame frame;

	private final short DEFAULT_ADMIN_PORT = AdminNode.DEFAULT_PORT_NUMBER;
	private final String DEFAULT_ADMIN_IP = "127.0.0.1";
	private final int DEFAULT_CAPACITY = AdminNode.DEFAULT_INITIAL_CAPACITY;

	private ImprovedFormattedTextField numNodesField;
	private ImprovedFormattedTextField adminCapacityField;
	private ImprovedFormattedTextField adminPortField;
	private ImprovedFormattedTextField addressOfAdminField;
	JTextArea outputField;

	private AdminNode admin;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.redirectSystemStreams();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		JPanel applicationPanel = new JPanel();
		JLabel header = new JLabel("CaaS Gui");

		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		header.setHorizontalAlignment(SwingConstants.CENTER);
		frame.getContentPane().add(header, BorderLayout.NORTH);

		frame.getContentPane().add(applicationPanel, BorderLayout.SOUTH);
		applicationPanel.setLayout(new BorderLayout(0, 0));
		applicationPanel.add(createInputOptionsPanel(), BorderLayout.NORTH);
		applicationPanel.add(createRuntimePanel(), BorderLayout.SOUTH);

		frame.pack();
	}

	/**
	 * Creates a panel with options for starting the simulation
	 * 
	 */
	private JPanel createInputOptionsPanel() {

		JPanel startupPanel = new JPanel();

		startupPanel.setLayout(new BorderLayout(0, 0));

		startupPanel.add(createAdminNodePanel(), BorderLayout.NORTH);
		startupPanel.add(createCacheNodePanel(), BorderLayout.SOUTH);

		return startupPanel;
	}

	/**
	 * Creates a panel with runtime relevant Components
	 * 
	 */
	private JPanel createRuntimePanel() {

		JPanel runtimePanel = new JPanel();

		runtimePanel.setLayout(new BorderLayout(0, 0));
		runtimePanel.add(createRunTimeCommandsPanel(), BorderLayout.EAST);
		runtimePanel.add(createOutputPanel(), BorderLayout.WEST);

		return runtimePanel;
	}

	/**
	 * Creates a Panel for the output
	 */
	private JPanel createOutputPanel() {

		JPanel outputPanel = new JPanel();
		outputPanel.setLayout(new BorderLayout(0, 0));

		outputField = new JTextArea();
		outputField.setRows(50);
		outputField.setColumns(60);
		outputPanel.add(new JScrollPane(outputField));

		JLabel lblOutput = new JLabel("output");
		lblOutput.setHorizontalAlignment(SwingConstants.CENTER);
		outputPanel.add(lblOutput, BorderLayout.NORTH);

		return outputPanel;

	}

	/**
	 * Creates a panel with options for the admin
	 */
	private JPanel createAdminNodePanel() {
		JPanel adminPanel = new JPanel();
		adminPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JLabel lblIpAndPort = new JLabel("port of admin");
		adminPanel.add(lblIpAndPort);

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		intFormat.setGroupingUsed(false);

		adminPortField = new ImprovedFormattedTextField(intFormat, DEFAULT_ADMIN_PORT);
		adminPortField.setColumns(6);
		adminPanel.add(adminPortField);

		JLabel lblNumberOfNodes = new JLabel("Number of nodes Connecting");
		adminPanel.add(lblNumberOfNodes);

		adminCapacityField = new ImprovedFormattedTextField(intFormat, DEFAULT_CAPACITY);
		adminCapacityField.setColumns(10);
		adminPanel.add(adminCapacityField);

		return adminPanel;
	}

	/**
	 * Create a panel with options for the cache node
	 */
	private JPanel createCacheNodePanel() {

		JPanel cacheNodePanel = new JPanel();

		JLabel lblAddressOfAdmin = new JLabel("Address of admin node");
		cacheNodePanel.add(lblAddressOfAdmin);
		addressOfAdminField = new ImprovedFormattedTextField(new RegexFormatter(Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(:\\d{4,5})?$")));
		addressOfAdminField.setValue(DEFAULT_ADMIN_IP + ":" + DEFAULT_ADMIN_PORT);
		addressOfAdminField.setColumns(20);
		cacheNodePanel.add(addressOfAdminField);

		JLabel lblNumberOfCache = new JLabel("Number of Cache nodes to start");
		cacheNodePanel.add(lblNumberOfCache);

		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		intFormat.setGroupingUsed(false);

		numNodesField = new ImprovedFormattedTextField(intFormat, DEFAULT_CAPACITY);
		numNodesField.setColumns(10);

		cacheNodePanel.add(numNodesField);

		return cacheNodePanel;
	}

	/**
	 * Creates a panel with buttons to control the simulation
	 */
	private JPanel createRunTimeCommandsPanel() {
		JPanel runTimeCommands = new JPanel();
		GridBagLayout gbl_runTimeCommands = new GridBagLayout();
		gbl_runTimeCommands.columnWidths = new int[] { 0, 0 };
		gbl_runTimeCommands.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_runTimeCommands.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_runTimeCommands.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		runTimeCommands.setLayout(gbl_runTimeCommands);

		JButton btnNewButton_2 = new JButton("Start nodes");
		GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
		gbc_btnNewButton_2.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_2.gridx = 0;
		gbc_btnNewButton_2.gridy = 3;
		btnNewButton_2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Launching " + numNodesField.getValue() + " local nodes");
				int numOfNodes = Integer.parseInt(numNodesField.getText());
				for (int i = 0; i < numOfNodes; i++) {
					try {
						final String[] cache_args = addressOfAdminField.getText().split(":"); 
						final String port = cache_args.length==2 ? cache_args[0] : adminPortField.getText();
						new CacheNode(cache_args[0], Integer.parseInt(port));
					} catch (IOException e) {
						JOptionPane.showMessageDialog(frame, "One of the nodes could not connect to the admin", "Critical Error", JOptionPane.ERROR_MESSAGE);
						break;
					}
				}
			}
		});

		JButton btnNewButton = new JButton("Start admin");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 2;

		btnNewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (admin != null) {
					JOptionPane.showMessageDialog(frame, "Admin already started", "error", JOptionPane.ERROR_MESSAGE);
				} else {
					System.out.println("launchind admin node with a capacity of " + adminCapacityField.getText() + ", listening on port number "
							+ adminPortField.getText());

					Runnable r = new Runnable() {

						@Override
						public void run() {
							int numOfNodes = Integer.parseInt(adminCapacityField.getText());
							admin = new AdminNode(Integer.parseInt(adminPortField.getText()), numOfNodes);
						}
					};
					new Thread(r).start();
				}
			}
		});
		runTimeCommands.add(btnNewButton, gbc_btnNewButton);
		runTimeCommands.add(btnNewButton_2, gbc_btnNewButton_2);

		JButton btnNewButton_3 = new JButton("New button");
		GridBagConstraints gbc_btnNewButton_3 = new GridBagConstraints();
		gbc_btnNewButton_3.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_3.gridx = 0;
		gbc_btnNewButton_3.gridy = 4;
		runTimeCommands.add(btnNewButton_3, gbc_btnNewButton_3);

		JButton btnNewButton_4 = new JButton("New button");
		GridBagConstraints gbc_btnNewButton_4 = new GridBagConstraints();
		gbc_btnNewButton_4.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_4.gridx = 0;
		gbc_btnNewButton_4.gridy = 5;
		runTimeCommands.add(btnNewButton_4, gbc_btnNewButton_4);

		JButton button = new JButton("New button");
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 6;
		runTimeCommands.add(button, gbc_button);

		JButton button_1 = new JButton("New button");
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.insets = new Insets(0, 0, 5, 0);
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 7;
		runTimeCommands.add(button_1, gbc_button_1);

		JButton btnStopProcesses = new JButton("stop admin");
		GridBagConstraints gbc_btnStopProcesses = new GridBagConstraints();
		gbc_btnStopProcesses.insets = new Insets(0, 0, 5, 0);
		gbc_btnStopProcesses.gridx = 0;
		gbc_btnStopProcesses.gridy = 8;
		btnStopProcesses.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (admin == null) {
					JOptionPane.showMessageDialog(frame, "admin has already been shut down", "error", JOptionPane.ERROR_MESSAGE);
				} else {
					System.out.println("admin is shutting down");
					admin.shutDownSystem();
					admin = null;
				}
			}
		});
		runTimeCommands.add(btnStopProcesses, gbc_btnStopProcesses);

		JButton btnShutdownSimu = new JButton("New Button");
		GridBagConstraints gbc_btnShutdownSimu = new GridBagConstraints();
		gbc_btnShutdownSimu.insets = new Insets(0, 0, 5, 0);
		gbc_btnShutdownSimu.gridx = 0;
		gbc_btnShutdownSimu.gridy = 9;
		btnShutdownSimu.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

			}
		});
		runTimeCommands.add(btnShutdownSimu, gbc_btnShutdownSimu);

		JButton exitButton = new JButton("Exit");
		GridBagConstraints gbc_button_4 = new GridBagConstraints();
		gbc_button_4.insets = new Insets(0, 0, 5, 0);
		gbc_button_4.gridx = 0;
		gbc_button_4.gridy = 10;

		exitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (admin != null) {
					JOptionPane.showMessageDialog(frame, "The admin has to be shut down first", "Error", JOptionPane.ERROR_MESSAGE);
				} else {
					System.exit(0);
				}
			}
		});

		runTimeCommands.add(exitButton, gbc_button_4);

		return runTimeCommands;
	}

	// this taken from
	// http://unserializableone.blogspot.de/2009/01/redirecting-systemout-and-systemerr-to.html
	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				outputField.append(text);
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

}
