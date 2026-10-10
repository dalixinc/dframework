/*
 * @(#)DFrame.java	3.0 17/09/2012
 *
 * Copyright (c) 2001-2012 Daleman Inc. All Rights Reserved.
 *
 * This software is the proprietary information of DTML Inc.
 * Use is subject to license terms.
 *
 */
package com.dframework.foundation.components;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;


/**
 * This class extends JFrame to provide additional functionality, such as
 * different look& feels, Passing in Icons, and provides window closer facility
 *
 * @author Dale Macdonald
 * @see <a href = "C:\jdk1.3.0_02\Docs\jdk1.3\docs\api\JFrame.html">JFrame
 *      Spec</a> //adjust!!
 * @version 3.0
 *
 */
public class DFrame extends JFrame {

	/**
	 * Computer generated Serial ID
	 */
	private static final long serialVersionUID = 2762830259776517036L;

	private static final String STR_DEFAULT_ICON = "./icons/food9.gif";

	private static final Icon DANGER_ICON = new ImageIcon("./icons/dangerCLR.gif");

	private DLookAndFeelManager lookManager;

	private CloseBehaviour closeBehaviour;

	private WindowState windowState;

	boolean bUseDefaultCloseIcon = false;

	public DFrame(String title, ImageIcon ii, CloseBehaviour cb, WindowState ws, KnownLookAndFeels laf, boolean decorations, Rectangle rect) {
		super(title); // Call JFrame Constructor
		Image im;
		if (ii != null) {
			im = ii.getImage(); // Get the Image
		} else {
			im = new ImageIcon(STR_DEFAULT_ICON).getImage();
		}
		this.setIconImage(im); // Make it the Icon -- I show "this" explicitly
		this.setCloseBehaviour(cb);
		this.setLookAndFeel(laf, decorations);
		if (rect != null && rect.height != 0 && rect.width !=0)
			setBounds(rect);
		else
			this.pack();
		this.setWindowState(ws); // Call MY method which sets Window State
	}

	public DFrame(String title, ImageIcon ii, CloseBehaviour cb, WindowState ws, KnownLookAndFeels laf, boolean decorations, Dimension dim) {
		this(title, ii, cb, ws, laf, decorations, new Rectangle(dim));
	}

	public DFrame(String title, ImageIcon ii, CloseBehaviour cb, WindowState ws, KnownLookAndFeels laf, boolean decorations) {
		this(title, ii, cb, ws, laf, decorations, new Rectangle());
	}

	public DFrame(String title, ImageIcon ii, CloseBehaviour cb, WindowState ws, KnownLookAndFeels laf) {
		this(title, ii, cb, ws, laf, false, new Rectangle());
	}

	public DFrame(String title, ImageIcon ii, CloseBehaviour cb, KnownLookAndFeels laf) {
		this(title, ii, cb, WindowState.NORMAL, laf, false, new Rectangle());
	}

	public DFrame(String title, ImageIcon ii, KnownLookAndFeels laf) {
		this(title, ii, CloseBehaviour.CLOSE, WindowState.NORMAL, laf, false, new Rectangle());
	}

	public DFrame(String title, ImageIcon ii) {
		this(title, ii, CloseBehaviour.CLOSE, WindowState.NORMAL, KnownLookAndFeels.SYSTEM, false, new Rectangle());
	}

	public DFrame(String title, KnownLookAndFeels laf) {
		this(title, new ImageIcon(STR_DEFAULT_ICON), CloseBehaviour.CLOSE, WindowState.NORMAL, laf, false, new Rectangle());
	}

	public DFrame(String title) {
		super(title);
	}

	public DFrame() {
		super();
	}

	/**
	 * Method to Set the icon after the DaleFrame is already Constructed
	 *
	 * It slightly simplifies life, as it accepts String, instead of an image
	 *
	 * @param pic
	 *            The Icon, supplied as a String Object
	 *
	 */

	public void setIcon(String pic) {
		ImageIcon i = new ImageIcon(pic);
		Image im = i.getImage();
		this.setIconImage(im); // I use the "this" to signify that the method is
								// inherited
	}

	/**
	 * Method to set the Closing behaviour of the window when the close widget is
	 * clicked
	 *
	 * @param cB
	 *             constants: QCLOSE,
	 *            DCLOSE, DISPOSE, CLOSE
	 *
	 */

	public void setCloseBehaviour(CloseBehaviour cB) {

		closeBehaviour = cB;

		WindowListener[] wlArr = this.getWindowListeners();
		for (WindowListener wl : wlArr)
			this.removeWindowListener(wl);

		switch (closeBehaviour) {
		case QCLOSE:
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new QCloser(this, bUseDefaultCloseIcon));
			break;

		case DCLOSE:
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new DCloser(this, bUseDefaultCloseIcon));
			break;

		case HIDE:
			setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			break;

		case DISPOSE:
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			break;

		case CLOSE:
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			break;

		case NOTHING:
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			break;

		default: // TODO - Resource Bundle this
			JOptionPane
					.showMessageDialog(
							this,
							"You entered a value which is invalid - QCLOSE behaviour will prevail",
							"Invalid Closer Option", JOptionPane.OK_OPTION,
							DANGER_ICON);
			this.addWindowListener(new QCloser(this, bUseDefaultCloseIcon));
			break;
		}
	}

	/**
	 * Method to set the Window's State (ie maximised, iconised etc)
	 *
	 * @param <i>int</i> - iWS - representing the state
	 *
	 */

	public void setWindowState(WindowState ws) {
		this.windowState = ws;

		switch (windowState) {
		case MAXIMISED:
			maximise();
			break;

		case MINIMISED:
			minimise();
			break;

		case NORMAL:
			break;

		case MAXIMIZED_HORIZ:
			maximizeHorizontal();
			break;

		case MAXIMIZED_VERT:
			maximizeVertical();
			break;

		default:
			JOptionPane
					.showMessageDialog(
							this,
							"You entered a value which is invalid - NORMAL behaviour will prevail",
							"Invalid Closer Option", JOptionPane.OK_OPTION,
							DANGER_ICON);
			break;

		}
	}

	/**
	 * Method which returns the current Close Behaviour
	 *
	 * @return <i>int</i> - representing the current behaviour
	 */

	public CloseBehaviour getCloseBehaviour() {
		return closeBehaviour;
	}

	/**
	 * Method to setup a look and feel for the GUI
	 *
	 * @param lafs One of the many available constants in the KnownLookAndFeels enum
	 */
	public void setLookAndFeel(KnownLookAndFeels lafs, boolean decorations) {
		lookManager = new DLookAndFeelManager(lafs, decorations);
		try {
			lookManager.installLookAndFeel(this);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Get a reference to the associated Look and Feel Manager
	 * @return
	 */
	public DLookAndFeelManager getLookManager() {
		return lookManager;
	}

	/**
	 * Set a new Look and Feel Manager
	 * @param lookManager
	 */
	public void setLookManager(DLookAndFeelManager lookManager) {
		this.lookManager = lookManager;
	}

	/**
	 * Mutator to change the default icon on the Closer Dialog of the
	 * DFrame Default is false (DFramework icon), true brings the L&F default
	 * icon
	 *
	 * @param boolean bUseDefaultCloseIcon
	 */
	public void setBUseDefaultCloseIcon(boolean bUseDefaultCloseIcon) {
		this.bUseDefaultCloseIcon = bUseDefaultCloseIcon;
	}

	/* Public Action Methods */

	/**
	 * A method to maximise the screen, based on current resolution Note, it
	 * doesn't by default reposition the screen
	 *
	 */
	public void maximise() {
		Toolkit tools = Toolkit.getDefaultToolkit();
		setSize(tools.getScreenSize());
		windowState = WindowState.MAXIMISED;
	}

	/**
	 * A method to maximise the screen, based on current resolution
	 *
	 * @param reposition
	 *            = a boolean which if true, repositions to 0,0
	 *
	 */
	public void maximise(boolean reposition) {
		if (reposition)
			this.setLocation(0, 0);
		maximise();
		this.repaint();
	}

	/**
	 * A method to maximise the screen Horizontally, based on current resolution
	 * Does not work in all environments
	 * @since Works with Java 1.4 upwards
	 *
	 */
	public void maximizeHorizontal() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.MAXIMIZED_HORIZ);
		System.out.println("Maximised Horiz " + (isSupported?"is":"is not")+ " supported!");
		if (isSupported) {
			this.setExtendedState (Frame.MAXIMIZED_HORIZ);
			windowState = WindowState.MAXIMIZED_HORIZ;
		}
	}

	/**
	 * A method to maximise the screen Vertically, based on current resolution
	 *
	 * @since Works with Java 1.4 upwards
	 *
	 */
	public void maximizeVertical() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.MAXIMIZED_VERT);
		System.out.println("Maximised Vert " + (isSupported?"is":"is not")+ " supported!");
		if (isSupported){
			this.setExtendedState (Frame.MAXIMIZED_VERT);
			windowState = WindowState.MAXIMIZED_VERT;
		}
	}

	/**
	 * A method to minimise the screen
	 *
	 *
	 */
	public void minimise() {
		// /this.setExtendedState (Frame.ICONIFIED);
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.ICONIFIED);
		System.out.println("Iconified " + (isSupported?"is":"is not")+ " supported!");
		this.setState(Frame.ICONIFIED); // Pre 1.4
		windowState = WindowState.MINIMISED;
	}

	/**
	 * A method to maximize the screen
	 *
	 *
	 */
	public void maximize() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.MAXIMIZED_BOTH);
		System.out.println("Maximised (Both) " + (isSupported?"is":"is not")+ " supported!");
		this.setExtendedState (Frame.MAXIMIZED_BOTH);
		windowState = WindowState.MAXIMISED;
	}

	/**
	 * Check out the window state
	 * @return
	 */
	public WindowState getWindowState() {
		return this.windowState;
	}

	/**
	 * An enum to represent all the possible close behaviours on offer.
	 */
	private static enum CloseBehaviour {
		QCLOSE, DCLOSE, HIDE, DISPOSE, CLOSE, NOTHING
	}

	/**
	 * An enum to represent each possible window state
	 */
	private static enum WindowState {
		MAXIMISED, MINIMISED, NORMAL, MAXIMIZED_HORIZ, MAXIMIZED_VERT
	}

	/**
	 * A main method for testing the class
	 *
	 */

	public static void main(String[] args) {
		JFrame.setDefaultLookAndFeelDecorated(true);
		////DFrameOrig.setBUseDefaultCloseIcon(false);
		// DFrame frame = new DFrame("Jerkoff");
		ImageIcon icon = new ImageIcon("./icons/alien10.gif");
		// /-DFrame frame = new DFrame("Testing DFrame", icon,"", CLOSE,
		// NORMAL,new Rectangle(100, 100, 300, 200));
		// DFrame frame = new DFrame("Testing DFrame", icon, "", QCLOSE, NORMAL,
		// new Rectangle(100, 100, 300, 500));

		// DFrame frame = new DFrame("Testing DFrame", icon, "", QCLOSE, NORMAL,
		// new Dimension(600, 300));

		// DFrame frame = new DFrame("Testing DFrame", "", QCLOSE, NORMAL,
		// new Rectangle(100, 100, 300, 500));

		// Make sure we have nice window decorations.
		// JFrame.setDefaultLookAndFeelDecorated(true);

//		DFrameOrig frame = new DFrameOrig("Testing DFrame", "liquid", QCLOSE,
//				NORMAL, new Rectangle(100, 100, 300, 500));

		DFrame frame = new DFrame("Testing DFrame", icon, CloseBehaviour.QCLOSE, WindowState.NORMAL, KnownLookAndFeels.QUAQUA, true, new Rectangle(200, 200, 600, 400));
		frame.setBUseDefaultCloseIcon(false);
		//-frame.setBUseDefaultCloseIcon(false);

		JPanel panel = new JPanel();
		JLabel label = new JLabel(
				"<html>The <b>DFramework</b> <i>Test</i></html>");
		JTextField field = new JTextField(25);
		JButton button = new JButton("Testing");
		panel.add(label);
		panel.add(field);
		panel.add(button);
		panel.setBorder(BorderFactory.createTitledBorder("The Border"));

		frame.setContentPane(panel);

		//frame.setBounds (100,250,200,150);


		// frame.maximiseVertical();
		// frame.maximiseHorizontal();
		//frame.maximize();
		// frame.maximise(true);
		// frame.maximise();
		// frame.minimise();
		// /frame.setBounds (100,0,200,150);
		// /frame.setCloseBehaviour (12);
		// frame.setCloseBehaviour (DFrame.DCLOSE);
		frame.show();
	}
}
