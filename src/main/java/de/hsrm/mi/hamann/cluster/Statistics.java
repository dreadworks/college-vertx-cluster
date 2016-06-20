package de.hsrm.mi.hamann.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Statistics {

	private String hostname;
	
	// metrics
	
	private Long local;
	private Long bus;
	private Long cluster;
	private Long counter;

	public Statistics () {
		try {
			this.hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException exc) {
			this.hostname = "unknown";
		}
		
		this.local   = 0L;
		this.bus     = 0L;
		this.cluster = 0L;
		this.counter = 0L;
	}

	
	// auto-generated
	
	public String getHostname() {
		return hostname;
	}


	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public Long getLocal() {
		return local;
	}

	public void setLocal(Long local) {
		this.local = local;
	}
	

	public Long getBus() {
		return bus;
	}

	public void setBus(Long bus) {
		this.bus = bus;
	}


	public Long getCluster() {
		return cluster;
	}

	public void setCluster(Long cluster) {
		this.cluster = cluster;
	}

	public Long getCounter() {
		return counter;
	}

	public void setCounter(Long counter) {
		this.counter = counter;
	}
	
}
