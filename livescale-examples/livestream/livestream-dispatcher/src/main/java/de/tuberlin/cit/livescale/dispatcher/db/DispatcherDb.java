package de.tuberlin.cit.livescale.dispatcher.db;

public class DispatcherDb {
	
	private static DispatcherDbInterface db = null;
	
	public static DispatcherDbInterface getInterface() {
		if (db == null) {
			try {
				db = new DerbyDbBackend();
			} catch (Exception e) {
				e.printStackTrace();
				db = null;
			}
		}
		return db;
	}
}