package com.dframework.foundation.components;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import junit.framework.TestCase;

/**
 * JUnit 3 tests of the DCloser
 * 
 * This test doesn't fully exercise the DCloser, but gives it a reasonable workout
 * and shows a few different forms of it visually to the tester.
 * 
 * @author dale.macdonald
 *
 */
public class DCloserTest extends TestCase{
	java.awt.Event evvy;
	JFrame frame;
	DCloser closer;
	Robot ronnie;
	boolean success;
	
	@Override
	protected void setUp() throws Exception {
		frame = new JFrame("Test DCloser");
		closer = new DCloser(frame);
		JLabel label = new JLabel("A Label");
		JTextField field = new JTextField(20);
		
		JButton button = new JButton("Button");
		button.setMnemonic(KeyEvent.VK_B);
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				closer.show();
			}
		});
		
		JPanel panel = new JPanel(); 
		
		panel.add(label);
		panel.add(field);
		panel.add(button);
		
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
		super.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}
	
	public void testDCloserOnButton() throws InterruptedException {
		success = false;
		Thread t = new Thread() {
			public void run() {
				try {
					ronnie = new Robot();
					ronnie.delay(2000);
					ronnie.keyPress(KeyEvent.VK_SHIFT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_SHIFT);
					ronnie.keyRelease(KeyEvent.VK_B);
			
					ronnie.delay(1000);
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_B);
					
					ronnie.delay(1000);
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_N);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_N);
					
					//closeWindow(frame);
					ronnie.delay(1000);
					
					closer.setImageIcon(new ImageIcon("./icons/food9.gif"));
					ronnie.delay(1000);
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_B);
					
					ronnie.delay(1000);
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_Y);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_Y);
					
					success = true;
					ronnie.delay(1000);
					
				} catch (AWTException awe) {
					System.out.println("This platform appears to have issues with Robots");
				}
			}
		};
		t.start();
		t.join();
		assertTrue(success);
	}
	
	//Simulating clicking on the "close" widget
	//Posting events programmatically
	private void closeWindow(JFrame frame){
		 Toolkit tk = Toolkit.getDefaultToolkit();
	        EventQueue evtQ = tk.getSystemEventQueue();
	        evtQ.postEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
	        success = true;
	}
	
	public void testLAndFDCloser() throws InterruptedException{
		closer = new DCloser(frame, true);
		testDCloserOnButton();
	}
}
