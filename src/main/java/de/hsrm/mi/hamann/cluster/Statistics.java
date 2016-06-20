package de.hsrm.mi.hamann.cluster;

public class Statistics {

	Long local;
	Long bus;
	Long cluster;
	Long counter;

	public Statistics () {
		this.local   = 0L;
		this.bus     = 0L;
		this.cluster = 0L;
		this.counter = 0L;
	}

	
	// auto-generated
	
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
