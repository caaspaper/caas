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

import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import de.uni_stuttgart.caas.admin.AdminNode;
import de.uni_stuttgart.caas.testgui.ImprovedFormattedTextField;

public class MainWindow {

	private final short DEFAULT_ADMIN_PORT = AdminNode.DEFAULT_PORT_NUMBER;
	private final String DEFAULT_ADMIN_IP = "127.0.0.1";

	private final int DEFAULT_CAPACITY = AdminNode.DEFAULT_INITIAL_CAPACITY;
	
	
	private JFrame frame;
	private JTextField addressOfAdmin;
	private JTextField numOfNodesConnecting;
	private JTextField textField_2;
	private JTextField textField_3;
	
	ImprovedFormattedTextField numNodesField;
	

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

		JLabel lblIpAndPort = new JLabel("ip and port of admin");
		adminPanel.add(lblIpAndPort);

		addressOfAdmin = new JTextField();
		adminPanel.add(addressOfAdmin);
		addressOfAdmin.setColumns(10);

		JLabel lblNumberOfNodes = new JLabel("Number of nodes Connecting");
		adminPanel.add(lblNumberOfNodes);
		
		NumberFormat intFormat = NumberFormat.getIntegerInstance();
		intFormat.setGroupingUsed(false);

		numNodesField = new ImprovedFormattedTextField(intFormat, DEFAULT_ADMIN_PORT);
		adminPanel.add(numNodesField);
		numNodesField.setColumns(6);

		JButton adminButton = new JButton("startAdmin");
		
		adminButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		adminPanel.add(adminButton);

		return adminPanel;
	}

	private JPanel createCacheNodePanel() {

		JPanel cacheNodePanel = new JPanel();
		textField_2 = new JTextField();
		cacheNodePanel.add(textField_2);
		textField_2.setColumns(10);

		JLabel lblNumberOfCache = new JLabel("Number of Cache nodes to start");
		cacheNodePanel.add(lblNumberOfCache);

		textField_3 = new JTextField();
		cacheNodePanel.add(textField_3);
		textField_3.setColumns(10);

		JButton cacheNodeButton = new JButton("start cacheNodes");
		
		cacheNodeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		cacheNodePanel.add(cacheNodeButton);

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

		JButton btnNewButton = new JButton("New button");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 0;
		runTimeCommands.add(btnNewButton, gbc_btnNewButton);

		JButton btnNewButton_2 = new JButton("New button");
		GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
		gbc_btnNewButton_2.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_2.gridx = 0;
		gbc_btnNewButton_2.gridy = 1;
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
