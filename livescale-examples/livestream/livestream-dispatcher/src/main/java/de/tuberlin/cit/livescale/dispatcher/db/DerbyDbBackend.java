package de.tuberlin.cit.livescale.dispatcher.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import de.tuberlin.cit.livescale.dispatcher.db.model.Follower;
import de.tuberlin.cit.livescale.dispatcher.db.model.Stream;
import de.tuberlin.cit.livescale.dispatcher.db.model.User;

public class DerbyDbBackend implements DispatcherDbInterface {
	
	private EntityManagerFactory eMFactory;

	public DerbyDbBackend() {
		Properties props = new Properties();
		props.setProperty("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
		props.setProperty("javax.persistence.jdbc.url", "jdbc:derby:citstreamerdb;create=true");
		props.setProperty("javax.persistence.jdbc.user", "");
		props.setProperty("javax.persistence.jdbc.password", "");
		props.setProperty("eclipselink.ddl-generation", "create-tables");
		props.setProperty("eclipselink.ddl-generation.output-mode", "database");
		eMFactory = Persistence.createEntityManagerFactory("DispatcherDb",props);
	}

	@Override
	public Object persistObject(Object obj) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		eM.persist(obj);
		eM.getTransaction().commit();
		eM.close();
		return obj;
	}

	@Override
	public Stream findStreamByRcvToken(String rcvToken) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		Stream stream = null;
		try {
			stream = (Stream) eM.createQuery("select x from Stream x where x.rcvToken = ?1").setParameter(1, rcvToken).getResultList().get(0);
		} catch (Exception e) {
		}
		eM.close();
		return stream;
	}
	
	@Override
	public User findUserByUsername(String username) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		User user = null;
		try {
			user = (User) eM.createQuery("select x from User x where x.username = ?1").setParameter(1, username).getResultList().get(0);
		} catch (Exception e) {
		}
		eM.close();
		return user;
	}
	
	public User findUserbyC2DMḰey(String c2dmKey) {
		EntityManager em = eMFactory.createEntityManager();
		em.getTransaction().begin();
		User user = (User) em.createQuery("select x from User x where x.c2dmKey = ?1").setParameter(1, c2dmKey).getSingleResult();
		return user;
	}
	
	@Override
	public List<Stream> getAllStreams() {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		List<Stream> streams = new ArrayList<Stream>();
		try {
			for (Object obj : eM.createQuery("select x from Stream x").getResultList()) {
				streams.add((Stream) obj);
			}
		} catch (Exception e) {
		}
		eM.close();
		return streams;
	}

	@Override
	public List<User> getAllUsers() {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		List<User> users = new ArrayList<User>();
		try {
			for (Object obj : eM.createQuery("select x from User x").getResultList()) {
				users.add((User) obj);
			}
		} catch (Exception e) {
		}
		eM.close();
		return users;
	}

	@Override
	public void removeObject(Object obj) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		obj = eM.merge(obj);
		eM.remove(obj);
		eM.getTransaction().commit();
		eM.close();
	}

	@Override
	public Object updateObject(Object obj) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		Object newObj = eM.merge(obj);
		eM.getTransaction().commit();
		eM.close();
		return newObj;
	}

	@Override
	public List<Follower> findFollowersByUser(User user) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		List<Follower> followers = new ArrayList<Follower>();
		try {
			for (Object obj : eM.createQuery("select x from Follower x where x.user = ?1").setParameter(1, user).getResultList()) {
				followers.add((Follower) obj);
			}
		} catch (Exception e) {
		}
		return followers;
	}

	@Override
	public List<Follower> findFollowingByUser(User user) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		List<Follower> followers = new ArrayList<Follower>();
		try {
			for (Object obj : eM.createQuery("select x from Follower x where x.follower = ?1").setParameter(1, user).getResultList()) {
				followers.add((Follower) obj);
			}
		} catch (Exception e) {
		}
		return followers;
	}

	@Override
	public List<Follower> getAllFollowers() {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		List<Follower> followers = new ArrayList<Follower>();
		try {
			for (Object obj : eM.createQuery("select x from Follower x").getResultList()) {
				followers.add((Follower) obj);
			}
		} catch (Exception e) {
		}
		eM.close();
		return followers;
	}

	@Override
	public Stream findStreamByUser(User user) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		Stream stream = null;
		try {
			stream = (Stream) eM.createQuery("select x from Stream x where x.user = ?1").setParameter(1, user).getResultList().get(0);
		} catch (Exception e) {
		}
		eM.close();
		return stream;
	}

	@Override
	public Follower findFollowerByUserAndFavorite(User user, User favorite) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		Follower follower = null;
		try {
			follower = (Follower) eM.createQuery("select x from Follower x where x.user = ?1 and x.follower = ?2").setParameter(1, favorite).setParameter(2, user).getResultList().get(0);
		} catch (Exception e) {
		}
		return follower;
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.dispatcher.db.DispatcherDbInterface#findStreamBySendToken(java.lang.String)
	 */
	@Override
	public Stream findStreamBySendToken(String sendEndpointToken) {
		EntityManager eM = eMFactory.createEntityManager();
		eM.getTransaction().begin();
		Stream stream = null;
		try {
			stream = (Stream) eM.createQuery("select x from Stream x where x.sendToken = ?1").setParameter(1, sendEndpointToken).getResultList().get(0);
		} catch (Exception e) {
		}
		eM.close();
		return stream;
	}
}