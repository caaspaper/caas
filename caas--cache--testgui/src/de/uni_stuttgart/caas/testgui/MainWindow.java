package de.uni_stuttgart.caas.testgui;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.regex.Pattern;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
	private JTextArea outputField;
	private NetworkGraph window;
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

		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));

		frame.getContentPane().add(createInputOptionsPanel(), BorderLayout.NORTH);
		frame.getContentPane().add(createRuntimePanel(), BorderLayout.CENTER);

		createMenuBar();
		
		frame.pack();
	}

	/**
	 * Creates a MenuBar and adds it to the frame
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu actions = new JMenu("Actions");
		JMenu views = new JMenu("Views");
		JMenu remoteActions = new JMenu("Remote Actions");
		
		menuBar.add(actions);
		menuBar.add(views);
		menuBar.add(remoteActions);
		
		JMenuItem startAdmin = new JMenuItem("Start admin");
		JMenuItem startCacheNodes = new JMenuItem("Start cache nodes");
		JMenuItem exit = new JMenuItem("Exit");
		
		actions.add(startAdmin);
		actions.add(startCacheNodes);
		actions.add(exit);
		
		JMenuItem networkGraph = new JMenuItem("Network Graph");
		views.add(networkGraph);
		
		JMenuItem startRemoteSimulation = new JMenuItem("Start remote simulation");
		remoteActions.add(startRemoteSimulation);
		
		startAdmin.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (admin != null) {
					JOptionPane.showMessageDialog(frame, "Admin already started", "error", JOptionPane.ERROR_MESSAGE);
				} else {
					System.out.println("launchind admin node with a capacity of " + adminCapacityField.getText() + ", listening on port number "
							+ adminPortField.getText());

					int numOfNodes = Integer.parseInt(adminCapacityField.getText());
					int port = Integer.parseInt(adminPortField.getText());
					try {
						admin = new AdminNode(port, numOfNodes);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(frame, "Admin could not start", "error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		
		startCacheNodes.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Launching " + numNodesField.getValue() + " local nodes");
				int numOfNodes = Integer.parseInt(numNodesField.getText());
				for (int i = 0; i < numOfNodes; i++) {
					try {
						final String[] cache_args = addressOfAdminField.getText().split(":"); 
						final String port = cache_args.length==2 ? cache_args[1] : adminPortField.getText();
						new CacheNode(addressOfAdminField.getText(), Integer.parseInt(adminPortField.getText()));
					} catch (IOException e) {
						JOptionPane.showMessageDialog(frame, "One of the nodes could not connect to the admin", "Critical Error", JOptionPane.ERROR_MESSAGE);
						break;
					}
				}
			}
		});
		
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (admin != null) {
					admin.close();
					admin = null;
				}
				System.exit(0);
			}
		});
		
		networkGraph.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (admin == null) {
					JOptionPane.showMessageDialog(frame, "The admin has to be running", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (window != null) {
					window.setVisible(true);
				} else {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								window = new NetworkGraph(admin.getTriangles());
								window.setVisible(true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		});
		
		startRemoteSimulation.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int numOfNodes = getNumberOfNodes();
				
				// start admin
				try {
					admin = new AdminNode(DEFAULT_ADMIN_PORT, numOfNodes);
				} catch (IOException e) {
					e.printStackTrace();
				}
				numOfNodes /= 4;
				
				try {
					Runtime.getRuntime().exec("sh /export/elb/startSimulation.sh " + numOfNodes);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
			private int getNumberOfNodes() {
				
				String input = null;
				int number = 0;
				while (input == null || input.isEmpty()) {
					input = JOptionPane.showInputDialog(frame, "the number of nodes to start (has to be divisible by four)");
					try {
						number = Integer.parseInt(input);
						if (number < 0 || number > 64 || number % 4 != 0) {
							input = null;
						}
					} catch (NumberFormatException e) {
						input = null;
					}
				}
				return number;
			}
		});
		
		frame.setJMenuBar(menuBar);
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

		JLabel lblAddressOfAdmin = new JLabel("Host of admin node");
		cacheNodePanel.add(lblAddressOfAdmin);
		addressOfAdminField = new ImprovedFormattedTextField(new RegexFormatter(Pattern.compile("^\\w[\\w\\.-]*?\\w$")));
		addressOfAdminField.setValue(DEFAULT_ADMIN_IP); // TODO replace with "elb"
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
