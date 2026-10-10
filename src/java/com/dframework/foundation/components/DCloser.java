/*
 * @(#)DCloser.java	1.3 12/09/2012
 *
 * Copyright 2001-2012 DTML, Inc. All Rights Reserved.
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
 * A class which can generate a dialogue box, bound to a Window object
 * Various parameters of this "dispose" dialogue are easily manipulated
 * It may be registered as an <i>ActionListener</i> or a <i>WindowListener</i>
 * (ie, it may be triggered in numerous ways)
 * @author Dale
 * @version 1.3
 */ 

public class DCloser extends WindowAdapter implements ActionListener
{

	/* Internal State (Fields) */

	private Window ww;	//Must use Window, not Component, as that is where dispose() method is defined
	private ImageIcon questionIcon = new ImageIcon("./icons/question.gif");
	private String closeMessage = "Are you sure you want to close this window?";
	private String closeHeading = "Close Confirm";
	private boolean bUseDefaultIcon = false; //Will pop up the funky animated icon by default

	/* Constructors */

	/**
	 * Pass in a parent Window Object constructor
	 * @param Window ww = parent for the popup window to be based on 
	 */
	public DCloser(Window w)
	{
		this.ww = w;
	}

	/**
	 * Pass in a parent Window Object plus boolean whether DFramework icon (false)
	 * or default L&F icon (true) should be displayed on DCloser dialog constructor
	 * @param Window ww = parent for the popup window to be based on 
	 * @param boolean bDefault
	 */
	public DCloser(Window w, boolean bDefault)
	{
		this(w);
		bUseDefaultIcon = bDefault;	
	}

	/* Overridden "Listener" Methods */

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
	 * A method to enable any class to launch a DCloser
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
	 * shown on the DCloser OptionPane
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
	public boolean getBUseDefaultIcon() {
		return (this.bUseDefaultIcon); 
	}	

	/* Internal (Private) Functions */

	private void close()
	{
		int ans = 0;

		if(bUseDefaultIcon)
			ans =JOptionPane.showConfirmDialog(ww, closeMessage, closeHeading,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		else
			ans =JOptionPane.showConfirmDialog(ww, closeMessage, closeHeading,JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,questionIcon);

		if (ans == JOptionPane.YES_OPTION) 		
			ww.dispose ();
		else return;
	}

	public static void main(String [] args)
	{
		DCloser dc = new DCloser(null, true);
		//dc.bUseDefaultIcon = true;
		dc.show();	
	}

}



