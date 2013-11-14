package de.tuberlin.cit.livescale.job.util.overlay;

public class TimeOverlayProvider implements OverlayProvider {

	private final TimeVideoOverlay overlay;
	
	public TimeOverlayProvider() {
		
		this.overlay = new TimeVideoOverlay();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public VideoOverlay getOverlay() {		
		return this.overlay;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		
		
	}

}
