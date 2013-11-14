package de.tuberlin.cit.livescale.dispatcher.db.model;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name="Followers")
public class Follower {

	@Id
    @TableGenerator(name = "follower_gen", table="primary_keys")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "follower_gen")
	private Integer id;
	private Date created;
	private Date updated;
	
	private User user;
	private User follower;
	
	@PrePersist
	public void createTimestamp() {
		Date date = new Date(System.currentTimeMillis()); 
		created = date;
		updated = date;
	}
	
	@PreUpdate
	public void updateTimestamp() {
		updated = new Date(System.currentTimeMillis());
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public User getFollower() {
		return follower;
	}
	
	public void setFollower(User follower) {
		this.follower = follower;
	}
	
	public Integer getId() {
		return id;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}
}
