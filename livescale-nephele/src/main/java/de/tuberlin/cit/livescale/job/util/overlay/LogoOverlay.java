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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.tuberlin.cit.livescale.job.record.VideoFrame;

/**
 * @author bjoern
 * 
 */
public class LogoOverlay implements VideoOverlay {

	private final BufferedImage originalLogoImage;

	private Image scaledLogo;

	private int scaledWidth;

	private int scaledHeight;

	/**
	 * Initializes LogoOverlay.
	 * 
	 * @param string
	 */
	public LogoOverlay(String logoFile) throws IOException {
		this.originalLogoImage = ImageIO.read(new File(logoFile));
		this.scaledLogo = originalLogoImage;
		this.scaledWidth = originalLogoImage.getWidth();
		this.scaledHeight = originalLogoImage.getHeight();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.job.util.overlay.VideoOverlay#draw(de.tuberlin
	 * .cit.livescale.job.record.VideoFrame)
	 */
	@Override
	public void draw(VideoFrame frame) {

		BufferedImage frameImage = frame.frameImage;

		int maxWidth = (int) (0.25 * frameImage.getWidth());
		int maxHeight = (int) (0.25 * frameImage.getHeight());

		ensureScaledLogoDimensions(maxWidth, maxHeight);
		drawScaledLogo(frameImage);
	}

	/**
	 * @param maxWidth
	 * @param maxHeight
	 */
	private void ensureScaledLogoDimensions(int maxWidth, int maxHeight) {
		if (scaledWidth > maxWidth || scaledHeight > maxHeight) {

			double scaleFactor = Math.min(
					maxWidth / ((double) originalLogoImage.getWidth()), maxHeight
							/ ((double) originalLogoImage.getHeight()));

			scaledWidth = (int) (scaleFactor * originalLogoImage.getWidth());
			scaledHeight = (int) (scaleFactor * originalLogoImage.getHeight());
			scaledLogo = originalLogoImage.getScaledInstance(scaledWidth,
					scaledHeight, Image.SCALE_SMOOTH);
		}
	}

	/**
	 * @param frameImage
	 */
	private void drawScaledLogo(BufferedImage frameImage) {
		int x = 15;
		int y = frameImage.getHeight() - scaledHeight - 15;
		Graphics2D g = (Graphics2D) frameImage.getGraphics();
		g.drawImage(scaledLogo, x, y, null);
	}
}
