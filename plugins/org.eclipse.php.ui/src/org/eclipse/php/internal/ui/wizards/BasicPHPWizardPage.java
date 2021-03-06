/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.ui.wizards;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.php.internal.ui.util.StatusInfo;
import org.eclipse.php.internal.ui.util.StatusUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public abstract class BasicPHPWizardPage extends WizardPage {

	private IStatus fCurrStatus;

	private boolean fPageVisible;

	protected BasicPHPWizardPage(String pageName) {
		super(pageName);
	}

	protected Button createButton(Composite composite, int style, String message, GridData gd) {
		Button button = new Button(composite, style);
		button.setText(message);
		button.setLayoutData(gd);
		return button;
	}

	protected GridLayout createGridLayout(int columns) {
		return new GridLayout(columns, false);

	}

	protected GridData createGridData(int flag, int hspan, int vspan, int indent) {
		GridData gd = new GridData(flag);
		gd.horizontalIndent = indent;
		gd.horizontalSpan = hspan;
		gd.verticalSpan = vspan;
		return gd;
	}

	protected GridData createGridData(int flag, int hspan, int indent) {
		GridData gd = new GridData(flag);
		gd.horizontalIndent = indent;
		gd.horizontalSpan = hspan;
		return gd;
	}

	protected GridData createGridData(int hspan) {
		GridData gd = new GridData();
		gd.horizontalSpan = hspan;
		return gd;
	}

	protected void createSeparator(Composite composite, int horizontalSpan) {
		Label line = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (horizontalSpan > 0) {
			gridData.horizontalSpan = horizontalSpan;
		}

		line.setLayoutData(gridData);
	}

	protected Label createLabel(Composite composite, int style, String message, GridData gd) {
		Label label = new Label(composite, style);
		label.setText(message);
		label.setLayoutData(gd);
		return label;
	}

	protected Text createText(Composite composite, int style, String message, GridData gd) {
		Text text = new Text(composite, style);
		if (message != null) {
			text.setText(message);
		}
		text.setLayoutData(gd);
		return text;
	}

	protected Combo createCombo(Composite composite, int style, String message, GridData gd) {
		Combo combo = new Combo(composite, style);
		if (message != null) {
			combo.setText(message);
		}
		combo.setLayoutData(gd);
		return combo;
	}

	protected void handleFileBrowseButtonPressed(Text text, String[] extensions, String title) {
		FileDialog dialog = new FileDialog(text.getShell());
		dialog.setText(title);
		dialog.setFilterExtensions(extensions);
		String dirName = text.getText();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path = new File(dirName);
			if (path.exists()) {
				dialog.setFilterPath(dirName);
			}

		}
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			text.setText(selectedDirectory);
		}
	}

	protected void handleFileBrowseButtonPressed(Combo text, String[] extensions, String title) {
		FileDialog dialog = new FileDialog(text.getShell());
		dialog.setText(title);
		dialog.setFilterExtensions(extensions);
		String dirName = text.getText();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path = new File(dirName);
			if (path.exists()) {
				dialog.setFilterPath(dirName);
			}

		}
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			text.add(selectedDirectory);
			text.select(text.indexOf(selectedDirectory));
			text.notifyListeners(SWT.Modify, new Event());
		}
	}

	protected void handleFileBrowseButtonPressed(Combo text, String[] extensions, String title, String fileName) {
		FileDialog dialog = new FileDialog(text.getShell(), SWT.SAVE);
		dialog.setText(title);
		dialog.setFilterExtensions(extensions);
		dialog.setFileName(fileName);
		String dirName = text.getText();
		if (!dirName.equals("")) { //$NON-NLS-1$
			File path = new File(dirName);
			if (path.exists()) {
				dialog.setFilterPath(dirName);
			}

		}
		String selectedDirectory = dialog.open();
		if (selectedDirectory != null) {
			text.add(selectedDirectory);
			text.select(text.indexOf(selectedDirectory));
			text.notifyListeners(SWT.Modify, new Event());
		}
	}

	protected String handleFolderBrowseButtonPressed(String dir, String title, String message) {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(dir);
		dialog.setText(title);
		dialog.setMessage(message);
		String res = dialog.open();
		if (res != null) {
			File file = new File(res);
			if (file.isDirectory()) {
				return res;
			}
		}
		return dir;
	}

	/**
	 * Updates the status line and the OK button according to the given status
	 * 
	 * @param status
	 *            status to apply
	 */
	protected void updateStatus(IStatus status) {
		fCurrStatus = status;
		setPageComplete(!status.matches(IStatus.ERROR));
		if (fPageVisible) {
			StatusUtil.applyToStatusLine(this, status);
		}
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		fPageVisible = visible;
		// policy: wizards are not allowed to come up with an error message
		if (visible && fCurrStatus.matches(IStatus.ERROR)) {
			StatusInfo status = new StatusInfo();
			status.setError(""); //$NON-NLS-1$
			fCurrStatus = status;
		}
		updateStatus(fCurrStatus);
	}

	public List<String> setComboItems(Combo combo, List<String> arrItems) {
		arrItems.clear();
		arrItems.add(combo.getText()); // must be first
		String[] items = combo.getItems();
		for (int i = 0; i < items.length; i++) {
			String curr = items[i];
			if (!arrItems.contains(curr)) {
				arrItems.add(curr);
			}
		}
		return arrItems;

	}

}
