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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;  // Needed if you don't extend TestCase


/**
 * JUnit 4 tests of the QCloser
 * 
 * This test doesn't fully exercise the QCloser, but gives it a reasonable workout
 * and shows a few different forms of it visually to the tester.
 * 
 * This test is no use as part of a suite as clicking yes on a closer terminates the jvm!
 * It can be "Suited" until you  force a "yes" on the closer
 * @author dale.macdonald
 *
 */
public class QCloserTest {
	
	JFrame frame;
	QCloser closer;
	Robot ronnie;
	boolean success;
	
	@Before
	public void setUp() {
		frame = new JFrame("Test QCloser");
		closer = new QCloser();
		JLabel label = new JLabel("A Label");
		JTextField field = new JTextField(20);
		
		JButton button = new JButton("Button");
		button.setMnemonic(KeyEvent.VK_B);
		button.addActionListener(closer);
//		button.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				closer.show();
//			}
//		});
		
		JPanel panel = new JPanel(); 
		
		panel.add(label);
		panel.add(field);
		panel.add(button);
		
		//frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
		//super.setUp();  - only valid if we are extending TestCase
	}
	
	@After
	public void tearDown() {
		//super.tearDown(); - only valid if we are extending TestCase
		// Perhaps need to strip down - not sure
	}
	
	@Test
	public void testCloserAsAnActionListener() throws InterruptedException {
		success = false;
		Thread t = new Thread() {
			public void run() {
				try {
					ronnie = new Robot();
					ronnie.delay(2000);
					
					//Type a capital B
					ronnie.keyPress(KeyEvent.VK_SHIFT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_SHIFT);
					ronnie.keyRelease(KeyEvent.VK_B);
					ronnie.delay(1000);
					
					//Invoke the mnemonic key, B
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_B);
					ronnie.delay(1000);
					
					//Decline the closedown
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_N);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_N);
					ronnie.delay(1000);
					
					//Change the Image for question Icon, then retrigger
					closer.setImageIcon(new ImageIcon("./icons/food9.gif"));
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_B);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_B);
					ronnie.delay(1000);
					
					//Again, don't close the Frame
					ronnie.keyPress(KeyEvent.VK_ALT);
					ronnie.keyPress(KeyEvent.VK_N);
					ronnie.keyRelease(KeyEvent.VK_ALT);
					ronnie.keyRelease(KeyEvent.VK_N);
					
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
	
	@Test
	public void testLAndFDCloser() throws InterruptedException{
		closer = new QCloser(frame, true);
		testCloserAsAnActionListener();
	}	
	
	@Test
	public void testFrameCloseTrigger() {
		closer = new QCloser(frame, true); //Centre the closer on the frame
		frame.addWindowListener(closer); //This should make it respond with a closer
		
		// Programmatically force a closing event
		Toolkit tk = Toolkit.getDefaultToolkit();
        EventQueue evtQ = tk.getSystemEventQueue();
        evtQ.postEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        
        //Make a robot to decline the closedown
        try {
			ronnie = new Robot();
			ronnie.delay(2000);
			
			//Decline the closedown
			ronnie.keyPress(KeyEvent.VK_ALT);
			ronnie.keyPress(KeyEvent.VK_Y);
			ronnie.keyRelease(KeyEvent.VK_ALT);
			ronnie.keyRelease(KeyEvent.VK_Y);
			ronnie.delay(2000);
			
        } catch (AWTException awe) {
			System.out.println("This platform appears to have issues with Robots");
		}
        
        success = true;
		
	}
}






