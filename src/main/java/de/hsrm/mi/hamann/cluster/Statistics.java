package de.hsrm.mi.hamann.cluster;

public class Statistics {

	Long local;
	Long cluster;
	Long counter;

	public Statistics () {
		this.local   = (long) 0;
		this.cluster = (long) 0;
		this.counter = (long) 0;
	}

	
	// auto-generated
	
	public Long getLocal() {
		return local;
	}

	public void setLocal(Long local) {
		this.local = local;
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
