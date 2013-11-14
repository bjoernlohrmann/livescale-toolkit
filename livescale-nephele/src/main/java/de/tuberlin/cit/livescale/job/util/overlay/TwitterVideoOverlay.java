package de.tuberlin.cit.livescale.job.util.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import twitter4j.Status;
import de.tuberlin.cit.livescale.job.record.VideoFrame;

public class TwitterVideoOverlay implements VideoOverlay {

	private static final int OUTER_MARGIN = 10;

	private static final int INNER_MARGIN = 5;

	private static final int BOX_HEIGHT = 60;

	private final String user;

	private final String text;

	private List<CharSequence> formattedText = null;

	private Font userFont = null;

	private Font textFont = null;

	private BufferedImage whiteBox = null;

	private final RescaleOp whiteBoxRescaleOp;

	private final BufferedImage profileImage;

	public TwitterVideoOverlay(final Status status) throws IOException {

		this.user = status.getUser().getScreenName();
		this.text = status.getText();

		final URL profileImageURL = status.getUser().getProfileImageURL();
		if (profileImageURL != null) {

			final HttpURLConnection connection = (HttpURLConnection) profileImageURL.openConnection();
			connection.setRequestMethod("GET");
			connection.addRequestProperty("Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.connect();
			final InputStream is = connection.getInputStream();
			BufferedImage bi = ImageIO.read(is);
			if (bi != null) {
				if (bi.getHeight() >= BOX_HEIGHT) {
					bi = null;
				}
			}

			this.profileImage = bi;
		} else {
			this.profileImage = null;
		}

		// System.out.println(status.getUser().getProfileImageURL());
		float[] scales = { 1f, 1f, 1f, 0.5f };
		float[] offsets = new float[4];
		this.whiteBoxRescaleOp = new RescaleOp(scales, offsets, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void draw(final VideoFrame frame) {

		final BufferedImage image = frame.frameImage;

		if (this.whiteBox == null) {
			final int width = image.getWidth();
			this.whiteBox = new BufferedImage(width - (2 * OUTER_MARGIN), BOX_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			final Graphics g = this.whiteBox.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.whiteBox.getWidth(), this.whiteBox.getHeight());
		}

		final Graphics2D g = (Graphics2D) image.getGraphics();

		final int height = image.getHeight();
		g.drawImage(this.whiteBox, this.whiteBoxRescaleOp, OUTER_MARGIN, height - OUTER_MARGIN - BOX_HEIGHT);

		int textXOffset = OUTER_MARGIN + INNER_MARGIN;
		int textYOffset;

		if (this.profileImage != null) {

			final int offsetWithinBox = (BOX_HEIGHT - this.profileImage.getHeight()) / 2;
			textYOffset = height - OUTER_MARGIN - BOX_HEIGHT + offsetWithinBox;
			g.drawImage(this.profileImage, OUTER_MARGIN + offsetWithinBox, textYOffset, this.profileImage.getWidth(),
				this.profileImage.getHeight(), null);

			textXOffset += offsetWithinBox + this.profileImage.getWidth();
		} else {
			textYOffset = height - OUTER_MARGIN - BOX_HEIGHT + INNER_MARGIN;
		}

		if (this.userFont == null) {
			this.userFont = new Font("Helvetica", Font.BOLD, 12);
		}

		g.setFont(this.userFont);
		g.setColor(Color.BLUE);
		FontMetrics fm = g.getFontMetrics();
		int textHeight = fm.getHeight();
		textYOffset += textHeight;
		g.drawString(this.user + ":", textXOffset, textYOffset);

		if (this.textFont == null) {
			this.textFont = new Font("Helvetica", Font.PLAIN, 10);
		}

		g.setFont(this.textFont);
		g.setColor(Color.BLACK);
		fm = g.getFontMetrics();
		textHeight = fm.getHeight();
		textYOffset += textHeight;

		if (this.formattedText == null) {
			this.formattedText = cropText(this.text, fm, image.getWidth() - textXOffset - OUTER_MARGIN
				- INNER_MARGIN);
		}

		for (final CharSequence cs : this.formattedText) {
			g.drawString(cs.toString(), textXOffset, textYOffset);
			textYOffset += textHeight;
		}

	}

	private static List<CharSequence> cropText(final String text, final FontMetrics fm, final int maxTextWidth) {

		String tmp = text;
		final ArrayList<CharSequence> lines = new ArrayList<CharSequence>();

		while (tmp.length() > 0) {

			final int cropPos = findCropPosition(tmp, fm, maxTextWidth);
			if (cropPos < 0) {
				break;
			}

			lines.add(tmp.subSequence(0, cropPos));

			if ((cropPos + 1) < tmp.length()) {
				tmp = tmp.substring(cropPos + 1);
			} else {
				tmp = tmp.substring(cropPos);
			}
		}

		if (tmp.length() > 0) {
			lines.add(tmp);
		}

		if (lines.size() > 2) {

			while (lines.size() != 2) {
				lines.remove(lines.size() - 1);
			}

			tmp = lines.get(1).toString();
			while (fm.stringWidth(tmp + "...") > maxTextWidth) {
				tmp = tmp.substring(0, tmp.length() - 1);
			}
			tmp += "...";
			lines.set(1, tmp);

		}

		return lines;
	}

	private static int findCropPosition(String str, final FontMetrics fm, final int maxTextWidth) {

		int pos = str.length();
		while (fm.stringWidth(str) > maxTextWidth) {

			pos = str.lastIndexOf(' ');
			if (pos < 0) {
				break;
			}

			str = str.substring(0, pos);
		}

		return pos;
	}
}
