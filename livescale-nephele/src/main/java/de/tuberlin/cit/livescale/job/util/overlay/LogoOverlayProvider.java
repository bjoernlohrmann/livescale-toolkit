/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package de.tuberlin.cit.livescale.job.util.overlay;

import java.io.IOException;

import eu.stratosphere.nephele.configuration.Configuration;

/**
 * @author bjoern
 *
 */
public class LogoOverlayProvider implements OverlayProvider {

	public static final String LOGO_OVERLAY_IMAGE = "overlay.logo.image";
	
	private final LogoOverlay overlay;
	
	public LogoOverlayProvider(Configuration conf) throws IOException {
		this.overlay = new LogoOverlay(conf.getString(LOGO_OVERLAY_IMAGE, "logo.png"));
	}
	
	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#getOverlay()
	 */
	@Override
	public VideoOverlay getOverlay() {
		return this.overlay;
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#start()
	 */
	@Override
	public void start() {
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#stop()
	 */
	@Override
	public void stop() {
	}

}
