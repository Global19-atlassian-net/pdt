/*******************************************************************************
 * Copyright (c) 2017 Rogue Wave Software Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Rogue Wave Software Inc. - initial implementation
 *******************************************************************************/
package org.eclipse.php.profile.core.engine;

import org.eclipse.php.profile.core.data.ProfilerCallTrace;
import org.eclipse.php.profile.core.data.ProfilerData;
import org.eclipse.php.profile.core.data.ProfilerFileData;
import org.eclipse.php.profile.core.data.ProfilerGlobalData;

/**
 * Common interface for Zend profiler implementation.
 */
public interface IProfiler {

	public ProfilerGlobalData getProfilerGlobalData();

	public ProfilerFileData getProfilerFileData(int fileNumber);

	public ProfilerCallTrace getProfilerCallTrace();

	public ProfilerData getProfilerData();

}