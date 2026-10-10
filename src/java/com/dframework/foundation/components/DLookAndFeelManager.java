/* 
 * @(#)DLookAndFeelManager.java	1.0 17/09/2012 
 * 
 * Copyright (c) 2012 Daleman Inc. All Rights Reserved.
 * 
 * This software is the proprietary information of DTML Inc.  
 * Use is subject to license terms.
 * 
 */
package com.dframework.foundation.components;

import java.awt.Component;
import java.awt.Window;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/**
 * This class encapsulates a number look and feel behaviours. It will also allow
 * additional Look & Feels to be loaded on demand.
 * 
 * It provides a method to refresh a look & feel into place.
 * 
 * @author Dale.Macdonald
 * @version 1.0
 */
public class DLookAndFeelManager {

	private KnownLookAndFeels klaf;

	private boolean hasTheme;

	private boolean hasCustomDecorations;

	private String themeName;

	/**
	 * 
	 * @param klaf
	 * @param themeName
	 * @param customDecorations
	 */
	DLookAndFeelManager(KnownLookAndFeels klaf, String themeName,
			boolean customDecorations) {
		JFrame.setDefaultLookAndFeelDecorated(customDecorations);
		this.klaf = klaf;
		if (themeName != null && themeName != "") {
			this.themeName = themeName;
			this.hasTheme = true;
		}
		this.hasCustomDecorations = customDecorations;
	}

	/**
	 * 
	 * @param klaf
	 * @param customDecorations
	 */
	DLookAndFeelManager(KnownLookAndFeels klaf, boolean customDecorations) {
		this(klaf, null, customDecorations);
	}

	/**
	 * 
	 * @param klaf
	 */
	DLookAndFeelManager(KnownLookAndFeels klaf) {
		this(klaf, null, false);
	}

	/**
	 * 
	 */
	DLookAndFeelManager() {
		this(KnownLookAndFeels.SYSTEM, null, false);
	}

	public void installLookAndFeel(Component parent, boolean pack)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		//JFrame.setDefaultLookAndFeelDecorated(true);

		if (klaf == KnownLookAndFeels.METAL) {
			this.hasTheme = true;
			MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
		} else if (klaf == KnownLookAndFeels.OCEAN) {
			this.hasTheme = true;
			MetalLookAndFeel.setCurrentTheme(new OceanTheme());
		} else if (this.hasTheme) {
			// TODO apply a custom theme
		}

		UIManager.setLookAndFeel(klaf.getLookAndFeel());
		SwingUtilities.updateComponentTreeUI(parent);
		if (pack)
			((Window) parent).pack();
	}
	
	public void installLookAndFeel(Component parent) 
		throws ClassNotFoundException, InstantiationException,
		IllegalAccessException, UnsupportedLookAndFeelException {
		installLookAndFeel(parent, false);
	}
	
}
