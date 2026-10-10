/* 
 * @(#)DFrame.java	2.0.04 11/09/2012 (now renamed DFrameOrig)
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * This class extends JFrame to provide additional functionality, such as
 * different look& feels, Passing in Icons, and provides window closer facility
 * 
 * @author Dale Macdonald
 * @see <a href = "C:\jdk1.3.0_02\Docs\jdk1.3\docs\api\JFrame.html">JFrame
 *      Spec</a> //adjust!!
 * @version 2.0.04
 * 
 */
public class DFrameOrig extends JFrame {

	/* Internal Constants */

	// Possible Look & Feels

	/** Set the Macintosh Look & Feel (Only avail on a Mac) */
	public static final String MAC = "com.sun.java.swing.plaf.mac.MacLookAndFeel";

	/** Set the Metal (Java) Look & Feel */
	public static final String METAL = "javax.swing.plaf.metal.MetalLookAndFeel";

	/** Set the Motif (Unix) Look & Feel */
	public static final String MOTIF = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";

	/**
	 * Set the GTK (Unix) Look & Feel - will only run on Unix systems with GTK+
	 * 2.2 or later
	 */
	public static final String GTK = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
	/** Set the Windows Look & Feel (Only avail on a Windows PC) */
	public static final String WINDOWS = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"; // For
																								// Classic
																								// Windoze:
	//TODO Need to separate the Look & Feel Engine out into a package																						// -Dswing.noxp=true
	/** Set Quaqua Look & Feel (Only legal on Apple commercially) */
	public static final String QUAQUA = "ch.randelshofer.quaqua.QuaquaLookAndFeel";
	/** Set Napkin Look & Feel (Currently still in Alpha, and buggy) */
	public static final String NAPKIN = "napkin.NapkinLookAndFeel";
	/** Set Liquid Look & Feel */
	public static final String LIQUID = "com.birosoft.liquid.LiquidLookAndFeel";
	/** Set Smooth Metal Look & Feel */
	public static final String SMOOTH = "smoothmetal.SmoothLookAndFeel";
	/** Set Lipstik Look & Feel */
	public static final String LIPSTIK = "com.lipstikLF.LipstikLookAndFeel";
	/** Set FH Look & Feel */
	public static final String FH = "com.shfarr.ui.plaf.fh.FhLookAndFeel";
	
	/**
	 * Set the Kunststoff Look & Feel (A freely Downloadable really cool L&F)
	 */
	public static final String KUNS = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";

	// Possible Themes

	// Possible Close Behaviours

	/** Throw up a QCloser Option (ie prompt before total program termination) */
	public static final int QCLOSE = 1;

	/** Throw up a DCloser Option (ie prompt before disposing of window) */
	public static final int DCLOSE = 2;

	/** Dispose of window without prompt */
	public static final int DISPOSE = 3;

	/** Close window without prompt */
	public static final int CLOSE = 4;

	// Possible Window States

	/** Throw window up Maximised */
	public static final int MAXIMISED = 1;

	/** Throw window up Minimised */
	public static final int MINIMISED = 2;

	/** Throw window up Normal */
	public static final int NORMAL = 3;

	/* Internal State (Fields) */

	private String lookNFeel; // a String representing the current look n feel
	private int closeBehaviour; // determines what is to be done on close
	private int windowState; // Max/Min/Normal

	private static String strDefaultIcon = "./icons/food9.gif"; // The default
																// icon if none
																// is supplied
	private static boolean bUseDefaultCloseIcon = false;
	private Icon messageIcon = new ImageIcon("./icons/dangerCLR.gif");

	/* Constructors */

	public DFrameOrig() {
		super();
	}

	/**
	 * All argument plus Rectangle
	 * 
	 * @param all
	 *            plus rect : (x , y, w, h)
	 */
	public DFrameOrig(String s, ImageIcon ii, String laf, int iCO, int iWS,
			Rectangle rect) {
		super(s); // Call JFrame Constructor
		Image im;
		if (ii != null) {
			im = ii.getImage(); // Get the Image
		} else {
			im = new ImageIcon(strDefaultIcon).getImage();
		}

		this.setIconImage(im); // Make it the Icon -- I show "this" explicitly
		this.setLAF(laf); // Call MY method which sets Look and Feel
		this.setCloseBehaviour(iCO); // Call MY method which sets what happens
										// on close
		setBounds(rect);
		this.setWindowState(iWS); // Call MY method which sets Window State
	}

	/**
	 * All argument plus Dimension
	 * 
	 * @param all
	 *            plus dim (w, h)
	 */
	public DFrameOrig(String s, ImageIcon ii, String laf, int iCO, int iWS,
			Dimension dim) {
		this(s, ii, laf, iCO, iWS, new Rectangle(dim));
	}

	/**
	 * All argument plus Rectangle but default ImageIcon
	 * 
	 * @param all
	 *            plus rect : (x , y, w, h)
	 */
	public DFrameOrig(String s, String laf, int iCO, int iWS, Rectangle rect) {
		this(s, null, laf, iCO, iWS, rect);
	}

//TODO - Other Constructors
	
	/* Accessor and Mutator Methods */

	/**
	 * Method to change the Look & Feel of the DaleFrame
	 * 
	 * @param laf
	 *            - A <i>String</i> reprsenting the look and feel
	 * 
	 */
	public void setLAF(String laf) {
		lookNFeel = laf;
		if (laf.equalsIgnoreCase("mac"))
			setLAFi(MAC);
		else if (laf.equalsIgnoreCase("motif"))
			setLAFi(MOTIF);
		else if (laf.equalsIgnoreCase("metal"))
			setLAFi(METAL);
		else if (laf.equalsIgnoreCase("windows"))
			setLAFi(WINDOWS);
		else if (laf.equalsIgnoreCase("quaqua"))
			setLAFi(QUAQUA);
		else if (laf.equalsIgnoreCase("napkin"))
			setLAFi(NAPKIN);
		else if (laf.equalsIgnoreCase("liquid"))
			setLAFi(LIQUID);
		else if (laf.equalsIgnoreCase("smooth"))
			setLAFi(SMOOTH);
		else if (laf.equalsIgnoreCase("lipstik"))
			setLAFi(LIPSTIK);
		else if (laf.equalsIgnoreCase("fh"))
			setLAFi(FH);
		else if (laf.equalsIgnoreCase("kuns"))
			setLAFi(KUNS);
		else
			setLAFi(METAL); // defaults to metal
	}

	/**
	 * Returns the Current Look & Feel
	 * 
	 * @return <i>String</i> Returns the look and feel (the short String
	 *         representing it)
	 */

	public String getLAF() {
		return lookNFeel;
	}

	/**
	 * Method to set the Closing behaviour of the window when the close widget is
	 * clicked
	 * 
	 * @param iCO
	 *            - <i>int</i> - should use one of the Class constants: QCLOSE,
	 *            DCLOSE, DISPOSE, CLOSE
	 * 
	 */

	public void setCloseBehaviour(int iCO) {
		closeBehaviour = iCO;
		// System.out.println("Dispose");

		// WindowListener[] wlArr = this.getWindowListeners();
		// for (int i=0; i<wlArr.length; i++)
		// this.removeWindowListener(wlArr[i]);

		WindowListener[] wlArr = this.getWindowListeners();
		for (WindowListener wl : wlArr)
			this.removeWindowListener(wl);

		switch (iCO) {
		case DFrameOrig.QCLOSE:
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new QCloser(this, bUseDefaultCloseIcon));
			break;

		case DFrameOrig.DCLOSE:
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new DCloser(this, bUseDefaultCloseIcon));
			break;

		case DFrameOrig.DISPOSE:
			setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			this.addWindowListener(new Disposer());
			break;

		case DFrameOrig.CLOSE:
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			break;

		default:
			JOptionPane
					.showMessageDialog(
							this,
							"You entered a value which is invalid - QCLOSE behaviour will prevail",
							"Invalid Closer Option", JOptionPane.OK_OPTION,
							messageIcon);
			this.addWindowListener(new QCloser(this, bUseDefaultCloseIcon));
			break;
		}
	}

	/**
	 * Method which returns the current Close Behaviour
	 * 
	 * @return <i>int</i> - representing the current behaviour
	 */

	public int getCloseBehaviour() {
		return closeBehaviour;
	}

	/**
	 * Method to set the Window's State (ie maximised, iconised etc)
	 * 
	 * @param <i>int</i> - iWS - representing the state
	 * 
	 */

	public void setWindowState(int iWS) {
		windowState = iWS;

		switch (iWS) {
		case DFrameOrig.MAXIMISED:

			maximise();
			break;

		case DFrameOrig.MINIMISED:
			minimise();
			break;

		case DFrameOrig.NORMAL:

			break;
		default:
			JOptionPane
					.showMessageDialog(
							this,
							"You entered a value which is invalid - NORMAL behaviour will prevail",
							"Invalid Closer Option", JOptionPane.OK_OPTION,
							messageIcon);
			break;

		}
	}

	/**
	 * Method which returns the current Window State
	 * 
	 * @return <i>int</i> - representing the current state
	 */

	public int getWindowState() {
		return windowState;
	}

	/**
	 * Static Accessor to retrieve a String representing the path to an image
	 * which is the icon atop the DFrame Itself
	 * 
	 * @param none
	 */
	public static String getDefaultIconString() {
		return strDefaultIcon;
	}

	/**
	 * Static Accessor to retrieve the image from the DFrame itself, as an
	 * actual ImageIcon object
	 * 
	 * @param none
	 */
	public static ImageIcon getDefaultIcon() {
		return new ImageIcon(strDefaultIcon);
	}

	/**
	 * Static Accessor to retrieve current Screen Resolution in dots-per-inch.
	 * 
	 * @param none
	 */
	public static int getScreenResolution() {
		Toolkit tools = Toolkit.getDefaultToolkit();
		return tools.getScreenResolution();
	}

	/**
	 * Static Accessor to retrieve current Screen Size in pixels as a Dimesion
	 * Object
	 * 
	 * @param none
	 */
	public static Dimension getScreenSize() {
		Toolkit tools = Toolkit.getDefaultToolkit();
		return tools.getScreenSize();
	}

	/**
	 * Static Mutator to change the default icon on the DFrame itself, as a
	 * String pointing out the path to a GIF etc
	 * 
	 * @param iconString
	 *            = a String representing the path to the image
	 */
	public static void setDefaultIconString(String iconString) {
		strDefaultIcon = iconString;
	}

	/**
	 * Static Mutator to change the default icon on the Closer Dialog of the
	 * DFrame Default is false (DFramework icon), true brings the L&F default
	 * icon
	 * 
	 * @param boolean bUseDefaultCloseIcon
	 */
	public static void setBUseDefaultCloseIcon(boolean bUseDefaultCloseIcon) {
		DFrameOrig.bUseDefaultCloseIcon = bUseDefaultCloseIcon;
	}

	/**
	 * Static Accessor to retrieve current default icon status on the Closer
	 * Dialog of the DFrame. Default is false (DFramework icon), true brings the
	 * L&F default icon
	 * 
	 * @param none
	 */
	public static boolean getBUseDefaultCloseIcon() {
		return (DFrameOrig.bUseDefaultCloseIcon);
	}

	/**
	 * Method to refresh the default icon on the DFrame itself, as a result of
	 * changing the default
	 */
	public void refreshDefaultIconDisplay() {
		setIcon(strDefaultIcon);
	}

	/**
	 * Mutator to change the default icon on the DFrame itself, sending an
	 * actual ImageIcon object
	 * 
	 * @param newIcon
	 *            = an ImageIcon object
	 */
	public void setDefaultImageIcon(ImageIcon newIcon) {
		strDefaultIcon = newIcon.getDescription(); // Can pass an ImageIcon
													// Object
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
	 * 
	 * @since Works with Java 1.4 upwards
	 * 
	 */
	public void maximiseHorizontal() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.MAXIMIZED_HORIZ);
		System.out.println("Maximised Horiz " + (isSupported?"is":"is not")+ " supported!");
		this.setExtendedState (Frame.MAXIMIZED_HORIZ);
	}

	/**
	 * A method to maximise the screen Vertically, based on current resolution
	 * 
	 * @since Works with Java 1.4 upwards
	 * 
	 */
	public void maximiseVertical() {
		Toolkit kit = Toolkit.getDefaultToolkit();
		boolean isSupported = kit.isFrameStateSupported(Frame.MAXIMIZED_VERT);
		System.out.println("Maximised Vert " + (isSupported?"is":"is not")+ " supported!");
		this.setExtendedState (Frame.MAXIMIZED_VERT);
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

	/* Internal (Private) Functions */

	// Internal Function which actually pulls off the Look & Feel change
	private void setLAFi(String laf) {
		try {
			UIManager.setLookAndFeel(laf);
			//UIManager.setLookAndFeel(ch.randelshofer.quaqua.QuaquaManager.getLookAndFeel());
			SwingUtilities.updateComponentTreeUI(this);
			this.pack();
		} catch (Exception e) {
			System.out.println("Problem!!" + e.getMessage());
		}
	}

	/* Inner Classes */

	// Class which calls superclass dispose method to release any resources

	class Disposer extends WindowAdapter {
		public void windowClosing(WindowEvent we) {
			Object source = we.getSource();
			JFrame jf = (JFrame) source;
			jf.dispose(); // Releases all resources
			System.out.println("Disposed");
		}
	}

	/**
	 * A main method for testing the class
	 * 
	 */

	public static void main(String[] args) {
		DFrameOrig.setBUseDefaultCloseIcon(false);
		// DFrame frame = new DFrame("Jerkoff");
		ImageIcon icon = new ImageIcon("./icons/alien10.gif");
		// /-DFrame frame = new DFrame("Testing DFrame", icon,"", CLOSE,
		// NORMAL,new Rectangle(100, 100, 300, 200));
		//DFrame frame = new DFrame("Testing DFrame", icon, "", QCLOSE, NORMAL,
		//		new Rectangle(100, 100, 300, 500));
		
		//DFrame frame = new DFrame("Testing DFrame", icon, "", QCLOSE, NORMAL,
		//		new Dimension(600, 300));
		
//		DFrame frame = new DFrame("Testing DFrame", "", QCLOSE, NORMAL,
//				new Rectangle(100, 100, 300, 500));
		
		//Make sure we have nice window decorations.
        //JFrame.setDefaultLookAndFeelDecorated(true);
		
		DFrameOrig frame = new DFrameOrig("Testing DFrame", "liquid", QCLOSE, NORMAL,
				new Rectangle(100, 100, 300, 500));
		
		JPanel panel = new JPanel();
		JLabel label = new JLabel("<html>The <b>DFramework</b> <i>Test</i></html>");
		JTextField field =new JTextField(25);
		JButton button = new JButton("Testing");
		panel.add(label);
		panel.add(field);
		panel.add(button);
		panel.setBorder(BorderFactory.createTitledBorder("The Border"));
		
		frame.setContentPane(panel);
		
		//frame.maximiseVertical();
		//frame.maximiseHorizontal();
		frame.maximize();
		//frame.maximise(true);
		//frame.maximise();
		//frame.minimise();
		// /frame.setBounds (100,0,200,150);
		// /frame.setCloseBehaviour (12);
		// frame.setCloseBehaviour (DFrame.DCLOSE);
		frame.show();
	}

}
