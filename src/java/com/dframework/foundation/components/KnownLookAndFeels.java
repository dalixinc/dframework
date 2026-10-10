/* 
 * @(#)KnownLookAndFeels.java	1.0 12/09/2012
 * 
 * Copyright (c) 2012 Daleman Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of DTML Inc.  
 * Use is subject to license terms.
 * 
 */
package com.dframework.foundation.components;

import javax.swing.UIManager;

/**
 * This represents various directly look & feel implementations supported by 
 * the DFramework.
 * 
 * Other look and feels may be added in time, and additionally there will be
 * a mechanism for dynamically adding L&F's via a configuration entry.
 * 
 * The implementation makes use of the power of the typesafe enum in Java 5.
 * 
 * @author  dale.macdonald
 * @version 1.0
 */
public enum KnownLookAndFeels {
	
	/* Items */
	SYSTEM(UIManager.getSystemLookAndFeelClassName()),
	CROSS_PLATFORM(UIManager.getCrossPlatformLookAndFeelClassName()), 
	MAC("com.sun.java.swing.plaf.mac.MacLookAndFeel"),	//Only on apple
	WINDOWS("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"),
	METAL("javax.swing.plaf.metal.MetalLookAndFeel"),
	OCEAN("javax.swing.plaf.metal.MetalLookAndFeel"),
	MOTIF("com.sun.java.swing.plaf.motif.MotifLookAndFeel"),
	GTK("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"),
	QUAQUA("ch.randelshofer.quaqua.QuaquaLookAndFeel"), //Is better done with a method call as name has changed per version
	NAPKIN("napkin.NapkinLookAndFeel"),
	LIQUID("com.birosoft.liquid.LiquidLookAndFeel"),
	SMOOTH("smoothmetal.SmoothLookAndFeel"),
	LIPSTIK("com.lipstikLF.LipstikLookAndFeel"),
	FH("com.shfarr.ui.plaf.fh.FhLookAndFeel");
	
	/**
	 * The String to be used by JFrames in setting L&F
	 */
	private final String lookAndFeel;
	
	/**
	 * Constructor 
	 * @param lAndF represents the class name to pass to UIManager
	 */
	KnownLookAndFeels(String lAndF) {
		this.lookAndFeel = lAndF;
	}

	/**
	 * Class string accessor
	 */
	public String getLookAndFeel() {
		return lookAndFeel;
	}
	
	
	
}
