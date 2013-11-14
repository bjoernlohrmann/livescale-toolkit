package de.tuberlin.cit.livescale.dispatcher.db;

import java.util.List;

import de.tuberlin.cit.livescale.dispatcher.db.model.Follower;
import de.tuberlin.cit.livescale.dispatcher.db.model.Stream;
import de.tuberlin.cit.livescale.dispatcher.db.model.User;

public interface DispatcherDbInterface {

	public Object persistObject(Object obj);
	
	public Stream findStreamByRcvToken(String rcvToken);
	
	public User findUserByUsername(String username);
	
	public User findUserbyC2DMḰey(String c2dmKey);
	
	public List<Follower> findFollowersByUser(User user);
	
	public List<Follower> findFollowingByUser(User user);
	
	public Follower findFollowerByUserAndFavorite(User user, User favorite);
	
	public Stream findStreamByUser(User user);
	
	public List<Follower> getAllFollowers();
	
	public List<Stream> getAllStreams();
	
	public List<User> getAllUsers();
	
	public void removeObject(Object obj);
	
	public Object updateObject(Object obj);
}