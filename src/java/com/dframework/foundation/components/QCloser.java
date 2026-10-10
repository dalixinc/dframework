/*
 * @(#)QCloser.java	1.3 12/09/12
 *
 * Copyright 2001-2012 Daleman Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of DTML Inc.  
 * Use is subject to license terms.
 * 
 */
package com.dframework.foundation.components;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

/**
 * This class provides a popup to confirm whether or not you wish to <b>EXIT</b> an
 * application
 * It may be registered as an <i>ActionListener</i> or a <i>WindowListener</i>
 * (ie, it may be triggered in numerous ways)
 * @author Dale 
 * @version 1.2
 *
 */
public class QCloser extends WindowAdapter implements ActionListener {

	/** An optional parent component to centre on */
	private Component c;
	//TODO Locate icons better
	private ImageIcon questionIcon = new ImageIcon("./src/java/com/dframework/foundation/icons/question.gif");
	//TODO Internationalise Text
	private String closeMessage = "Are you sure you want to shut down?";
	private String closeHeading = "Shutdown Confirm";
	/** Whether or not to use the default L&F icon */
	private boolean bUseDefaultIcon = false;



	/**
	 * No-argument constructor
	 * @param none 
	 */	
	public QCloser()
	{
		this.c = null;
	}

	/**
	 * Pass in a parent Component object constructor. Component will use the 
	 * DFramework Default question icon.
	 * @param Component c = parent for the popup window to be based on 
	 */
	public QCloser(Component c)
	{
		this.c = c;
	}

	/**
	 * Pass in a boolean to determine whether DFramework icon (false) or default L&F icon
	 * (true) to be displayed on the QCloser dialog
	 * @param boolean bDefault
	 */	
	public QCloser(boolean bDefault)
	{
		this();
		bUseDefaultIcon = bDefault;
	}

	/**
	 * Pass in a parent Component object plus a boolean to determine whether DFramework 
	 * icon (false) or default L&F icon (true) to be used constructor
	 * @param Component c = parent for the popup window to be based on 
	 * @param boolean bDefault
	 */
	public QCloser(Component c, boolean bDefault)
	{
		this(c);
		bUseDefaultIcon = bDefault;
	}
	/* Overidden "Listener" Methods */
	// TODO Not sure why these listeners are implemented - may stip them out
	
	@Override
	public void windowClosing(WindowEvent we)
	{
		close();
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		close();
	}

	/* Public Utility Methods */
	
	/**
	 * A method to enable any class to launch a QCloser
	 */
	public void show()
	{
		close();
	}

	/* Accessor and Mutator Methods */

	/**
	 * Accessor to retrieve the current message which appears on the close dialogue
	 * @param none 
	 */
	public String getCloseMessage() 
	{
		return closeMessage; 
	}

	/**
	 * Accessor to retrieve the current heading which appears on the close dialogue
	 * @param none 
	 */
	public String getCloseHeading() 
	{
		return closeHeading;
	}

	/**
	 * Mutator to set the  message which appears on the close dialogue
	 * @param cm = new close message
	 */
	public void setCloseMessage(String cm) 
	{
		closeMessage = cm;
	}

	/**
	 * Mutator to set the heading which appears on the close dialogue
	 * @param ch = new close message
	 */	
	public void setCloseHeading(String ch) 
	{
		closeHeading = ch;
	}

	/**
	 * Accessor to retrieve a String representing the path to an image
	 * to become the icon on the Dialogue Itself
	 * @param none 
	 */
	public String getImageIconString()
	{
		return questionIcon.getDescription();
	}

	/**
	 * Accessor to retrieve the image on the Dialogue itself, as an
	 * actual ImageIcon object
	 * @param none 
	 */
	public ImageIcon getImageIcon()
	{
		return questionIcon;
	}

	/**
	 * Mutator to change the image on the Dialogue itself, as an
	 * String pointing out the path to a GIF etc
	 * @param iconString = a String representing the path to the image  
	 */
	public void setImageIconString(String iconString)
	{
		questionIcon = new ImageIcon(iconString);
	}

	/**
	 * Mutator to change the image on the Dialogue itself, sending an
	 * actual ImageIcon object
	 * @param newIcon = an ImageIcon object 
	 */
	public void setImageIcon(ImageIcon newIcon)
	{
		questionIcon = newIcon;  //Can pass an ImageIcon Object
	}

	   /**
	    * Mutator to set whether the L&F default icon, or DFramework icon should be
	    * shown on the QCloser OptionPane
	    * 
	    * Default is false, which means DFramework Icon is used
	    *
	    * @param boolean bUseDefaultIcon = which to use 
		*/	
		public void setBUseDefaultIcon(boolean bUseDefaultIcon) {
			this.bUseDefaultIcon = bUseDefaultIcon; 
		}

	   /**
	    * Accessor to discover whether DFramework or default L&F icon is used
	    * @param none 
		*/
		public boolean isUseDefaultIcon() {
			return (this.bUseDefaultIcon); 
		}

						/* Internal (Private) Functions */	
		
		public void close()
		{
			int ans = 0;
			
			if (bUseDefaultIcon)
				ans =JOptionPane.showConfirmDialog(c, closeMessage, closeHeading,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			else
				ans =JOptionPane.showConfirmDialog(c, closeMessage, closeHeading,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,questionIcon);
			
			if (ans == JOptionPane.YES_OPTION) 		
				System.exit(0);
			else return;
		}
		
		public static void main(String [] args)
		{
			QCloser qc = new QCloser(null, true);
			//qc.bUseDefaultIcon = true;
			qc.show();	
		}


}
