package de.uni_stuttgart.caas.testgui;

import javax.swing.*;

public class TestGuiForm extends JFrame {

	public TestGuiForm() {
		setTitle("CaaS Admin GUI");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JButton btn = new JButton("Launch Admin");
        getContentPane().add(btn);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	TestGuiForm form = new TestGuiForm();
        		form.setVisible(true);
            }
        });
	}

}
