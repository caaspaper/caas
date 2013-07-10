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
import java.text.NumberFormat;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
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
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
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
		frame.getContentPane().add(header, BorderLayout.CENTER);

		frame.getContentPane().add(applicationPanel, BorderLayout.SOUTH);
		applicationPanel.setLayout(new BorderLayout(0, 0));
		applicationPanel.add(createStartupPanel(), BorderLayout.NORTH);
		applicationPanel.add(createRuntimePanel(), BorderLayout.SOUTH);
		
		frame.pack();
	}

	private JPanel createStartupPanel() {

		JPanel startupPanel = new JPanel();

		startupPanel.setLayout(new BorderLayout(0, 0));

		startupPanel.add(createAdminNodePanel(), BorderLayout.NORTH);
		startupPanel.add(createCacheNodePanel(), BorderLayout.SOUTH);

		return startupPanel;
	}

	private JPanel createRuntimePanel() {

		JPanel runtimePanel = new JPanel();

		runtimePanel.setLayout(new BorderLayout(0, 0));
		runtimePanel.add(createRunTimeCommandsPanel(), BorderLayout.EAST);
		runtimePanel.add(createOutputPanel(), BorderLayout.WEST);

		return runtimePanel;
	}

	private JPanel createOutputPanel() {

		JPanel outputPanel = new JPanel();
		outputPanel.setLayout(new BorderLayout(0, 0));

		JTextArea textArea = new JTextArea();
		textArea.setRows(50);
		textArea.setColumns(60);
		outputPanel.add(textArea);

		JLabel lblOutput = new JLabel("output");
		lblOutput.setHorizontalAlignment(SwingConstants.CENTER);
		outputPanel.add(lblOutput, BorderLayout.NORTH);

		return outputPanel;

	}

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

	private JPanel createCacheNodePanel() {

		JPanel cacheNodePanel = new JPanel();
		
		JLabel lblAddressOfAdmin = new JLabel("Address of admin node");
		cacheNodePanel.add(lblAddressOfAdmin);
		addressOfAdminField = new ImprovedFormattedTextField(new RegexFormatter(
				Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])(:\\d{4,5})?$")));
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

	private JPanel createRunTimeCommandsPanel() {
		JPanel runTimeCommands = new JPanel();
		GridBagLayout gbl_runTimeCommands = new GridBagLayout();
		gbl_runTimeCommands.columnWidths = new int[] { 0, 0 };
		gbl_runTimeCommands.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_runTimeCommands.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_runTimeCommands.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		runTimeCommands.setLayout(gbl_runTimeCommands);

		JButton btnNewButton = new JButton("Start admin");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 0;
		
		btnNewButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("launchind admin node with a capacity of " + adminCapacityField.getText()
						+ "listening on port number" + adminPortField.getText());
				
				int numOfNodes = Integer.parseInt(adminCapacityField.getText());
				new AdminNode(DEFAULT_ADMIN_PORT, numOfNodes);
			}
		});
		runTimeCommands.add(btnNewButton, gbc_btnNewButton);

		JButton btnNewButton_2 = new JButton("Start cache nodes");
		GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
		gbc_btnNewButton_2.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_2.gridx = 0;
		gbc_btnNewButton_2.gridy = 1;
		btnNewButton_2.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Launching " + numNodesField.getValue()
						+ " local nodes");
				int numOfNodes = Integer.parseInt(adminCapacityField.getText());
				for (int i = 0; i < numOfNodes; i++) {
					new CacheNode("localhost", String.valueOf(DEFAULT_ADMIN_PORT));
				}
			}
		});
		runTimeCommands.add(btnNewButton_2, gbc_btnNewButton_2);

		JButton btnNewButton_3 = new JButton("New button");
		GridBagConstraints gbc_btnNewButton_3 = new GridBagConstraints();
		gbc_btnNewButton_3.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_3.gridx = 0;
		gbc_btnNewButton_3.gridy = 2;
		runTimeCommands.add(btnNewButton_3, gbc_btnNewButton_3);

		JButton btnNewButton_4 = new JButton("New button");
		GridBagConstraints gbc_btnNewButton_4 = new GridBagConstraints();
		gbc_btnNewButton_4.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_4.gridx = 0;
		gbc_btnNewButton_4.gridy = 3;
		runTimeCommands.add(btnNewButton_4, gbc_btnNewButton_4);

		JButton button = new JButton("New button");
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 4;
		runTimeCommands.add(button, gbc_button);

		JButton button_1 = new JButton("New button");
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.insets = new Insets(0, 0, 5, 0);
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 5;
		runTimeCommands.add(button_1, gbc_button_1);

		JButton button_2 = new JButton("New button");
		GridBagConstraints gbc_button_2 = new GridBagConstraints();
		gbc_button_2.insets = new Insets(0, 0, 5, 0);
		gbc_button_2.gridx = 0;
		gbc_button_2.gridy = 6;
		runTimeCommands.add(button_2, gbc_button_2);

		JButton button_3 = new JButton("New button");
		GridBagConstraints gbc_button_3 = new GridBagConstraints();
		gbc_button_3.insets = new Insets(0, 0, 5, 0);
		gbc_button_3.gridx = 0;
		gbc_button_3.gridy = 7;
		runTimeCommands.add(button_3, gbc_button_3);

		JButton exitButton = new JButton("Exit");
		GridBagConstraints gbc_button_4 = new GridBagConstraints();
		gbc_button_4.gridx = 0;
		gbc_button_4.gridy = 8;
		
		exitButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		
		runTimeCommands.add(exitButton, gbc_button_4);

		return runTimeCommands;
	}

}
