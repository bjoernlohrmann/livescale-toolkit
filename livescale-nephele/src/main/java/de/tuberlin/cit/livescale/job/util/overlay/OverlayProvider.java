package de.tuberlin.cit.livescale.job.util.overlay;

public interface OverlayProvider {

	public VideoOverlay getOverlay();
	
	public void start();
	
	public void stop();
}
