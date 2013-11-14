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
@Table(name="Users")
public class User {
	
	@Id
    @TableGenerator(name = "user_gen", table="primary_keys")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "user_gen")
	private int id;
	private Date created;
	private Date updated;
	
	private String username;
	private String password;
	private String c2dmKey;
	private String latestMessageQueue;
	
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

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public String getC2dmKey() {
		return c2dmKey;
	}

	public void setC2dmKey(String c2dmKey) {
		this.c2dmKey = c2dmKey;
	}

	public String getLatestMessageQueue() {
		return latestMessageQueue;
	}

	public void setLatestMessageQueue(String latestMessageQueue) {
		this.latestMessageQueue = latestMessageQueue;
	}
}
