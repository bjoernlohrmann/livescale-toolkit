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
@Table(name="Streams")
public class Stream {

	@Id
    @TableGenerator(name = "stream_gen", table="primary_keys")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "stream_gen")
	private int id;
	
	private User user;
	private String rcvAddress;
	private int rcvPort;
	private String rcvToken;
	private String sendAddress;
	private int sendPort;
	private String sendToken;
	private Date created;
	private Date updated;
	
	public Stream() {
		super();
	}

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
	
	public String getRcvAddress() {
		return rcvAddress;
	}
	
	public void setRcvAddress(String rcvAddress) {
		this.rcvAddress = rcvAddress;
	}
	
	public int getRcvPort() {
		return rcvPort;
	}
	
	public void setRcvPort(int rcvPort) {
		this.rcvPort = rcvPort;
	}
	
	public String getRcvToken() {
		return rcvToken;
	}
	
	public void setRcvToken(String rcvToken) {
		this.rcvToken = rcvToken;
	}
	
	public String getSendAddress() {
		return sendAddress;
	}
	
	public void setSendAddress(String sendAddress) {
		this.sendAddress = sendAddress;
	}
	
	public int getSendPort() {
		return sendPort;
	}
	
	public void setSendPort(int sendPort) {
		this.sendPort = sendPort;
	}
	
	public String getSendToken() {
		return sendToken;
	}
	
	public void setSendToken(String sendToken) {
		this.sendToken = sendToken;
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

	public int getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
