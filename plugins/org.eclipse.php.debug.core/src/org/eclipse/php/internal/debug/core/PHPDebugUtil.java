/*******************************************************************************
 * Copyright (c) 2015 Zend Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.debug.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.php.internal.core.util.SyncObject;
import org.eclipse.php.internal.debug.core.debugger.DebuggerSettingsManager;
import org.eclipse.php.internal.debug.core.debugger.IDebuggerSettings;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.XDebugDebuggerConfiguration;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.XDebugDebuggerSettingsConstants;
import org.eclipse.php.internal.debug.core.zend.debugger.ZendDebuggerConfiguration;
import org.eclipse.php.internal.debug.core.zend.debugger.ZendDebuggerSettingsConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.icu.text.MessageFormat;

/**
 * Class with common utility methods for PHP debuggers.
 * 
 * @author Bartlomiej Laczkowski
 */
public final class PHPDebugUtil {

	private PHPDebugUtil() {
	}

	/**
	 * Opens URL from debug/run launch configuration. Users of this method
	 * should handle exceptions that might be thrown but without need to log the
	 * exception info (it is already handled by this implementation).
	 * 
	 * @param launchURL
	 * @throws DebugException
	 * @throws MalformedURLException
	 */
	public static void openLaunchURL(final String launchURL)
			throws DebugException {
		final SyncObject<DebugException> e = new SyncObject<DebugException>();
		// Run synchronously to pass exception if any
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				try {
					final URL urlToOpen = new URL(launchURL);
					StringBuilder browserTitle = new StringBuilder(urlToOpen
							.getProtocol()).append("://").append( //$NON-NLS-1$
							urlToOpen.getHost());
					if (urlToOpen.getPort() != -1)
						browserTitle.append(':').append(urlToOpen.getPort());
					browserTitle.append(urlToOpen.getPath());
					IWorkbenchBrowserSupport browserSupport = PlatformUI
							.getWorkbench().getBrowserSupport();
					IWebBrowser browser = browserSupport.createBrowser(
							IWorkbenchBrowserSupport.LOCATION_BAR
									| IWorkbenchBrowserSupport.NAVIGATION_BAR
									| IWorkbenchBrowserSupport.STATUS,
							"PHP Debugger Browser", //$NON-NLS-1$
							browserTitle.toString(), browserTitle.toString());
					if (PHPDebugPlugin.DEBUG)
						System.out
								.println("Opening debug/launch URL in a Web Browser: " //$NON-NLS-1$
										+ urlToOpen.toString());
					browser.openURL(urlToOpen);
				} catch (Throwable t) {
					Logger.logException(
							MessageFormat
									.format("Error initializing the Web Browser for debug/launch URL: {0}", launchURL), //$NON-NLS-1$
							t);
					String errorMessage = PHPDebugCoreMessages.Debugger_Unexpected_Error_1;
					e.set(new DebugException(new Status(IStatus.ERROR,
							PHPDebugPlugin.getID(),
							IPHPDebugConstants.INTERNAL_ERROR, errorMessage, t)));
				}
			}
		});
		DebugException ex = e.get();
		if (ex != null) {
			throw ex;
		}
	}

	/**
	 * Computes and returns a set of non-duplicated port numbers that are common
	 * for all of the possible settings for particular debugger type.
	 * 
	 * @param debuggerId
	 * @return set of unique port numbers for given debugger type
	 */
	public static Set<Integer> getDebugPorts(String debuggerId) {
		Set<Integer> ports = new HashSet<Integer>();
		if (debuggerId.equals(ZendDebuggerConfiguration.ID)) {
			// Get default port from preferences first
			Integer defaultPort = PHPDebugPlugin.getDebugPort(debuggerId);
			ports.add(defaultPort);
			// Get ports from dedicated settings
			List<IDebuggerSettings> allSettings = DebuggerSettingsManager.INSTANCE
					.findSettings(debuggerId);
			for (IDebuggerSettings settings : allSettings) {
				String clientPort = settings
						.getAttribute(ZendDebuggerSettingsConstants.PROP_CLIENT_PORT);
				try {
					Integer dedicatedPort = Integer.valueOf(clientPort);
					ports.add(dedicatedPort);
				} catch (Exception e) {
					// ignore
				}
			}
		} else if (debuggerId.equals(XDebugDebuggerConfiguration.ID)) {
			// Get default port from preferences first
			Integer defaultPort = PHPDebugPlugin.getDebugPort(debuggerId);
			ports.add(defaultPort);
			// Get ports from all of debugger dedicated settings
			for (IDebuggerSettings settings : DebuggerSettingsManager.INSTANCE
					.findSettings(debuggerId)) {
				String clientPort = settings
						.getAttribute(XDebugDebuggerSettingsConstants.PROP_CLIENT_PORT);
				try {
					Integer dedicatedPort = Integer.valueOf(clientPort);
					ports.add(dedicatedPort);
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return ports;
	}

}