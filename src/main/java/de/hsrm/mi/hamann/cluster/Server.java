package de.hsrm.mi.hamann.cluster;

import java.util.Arrays;
import java.util.Iterator;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Server {
	private final Logger log = LoggerFactory.getLogger(Server.class);
	
	private int port = -1;
	private int cport = 15781;
	
	
	private void parseArgs (String ...args) {
		
		Iterator<String> it = Arrays.asList(args).iterator();
		while (it.hasNext()) {
			
			// fall through... the root of all evil.
			// well, should work in this case
			
			switch (it.next()) {
			
			case "-p":
			case "--port":
				this.port = Integer.valueOf(it.next());
				break;
			
			case "-c":
			case "--cluster-port":
				this.cport = Integer.valueOf(it.next());
				break;
			}
		}
		
		if (this.port == -1) {
			throw new RuntimeException("no port set");
		}
	}
	
	
	public void start () {
		VertxOptions opts = new VertxOptions();
		opts.setClusterPort(this.cport);
		
		Vertx.clusteredVertx(opts, vertxRes -> {
			if (vertxRes.failed()) {
				throw new RuntimeException(vertxRes.cause());
			}
			
			log.info("created clustered vertx instance");
			final Vertx vertx = vertxRes.result();
			vertx.deployVerticle(new Verticle(this.port), deploymentRes -> {
				
				if (deploymentRes.failed()) {
					throw new RuntimeException(deploymentRes.cause());
				}
				
				final String id = deploymentRes.result(); 
				log.info(String.format("successfully deployed '%s'", id));
				
			});
		});
	}
	
	
	public Server (String ...args) {
		try {
			this.parseArgs(args);
		} catch (Exception exc) {
			System.out.println("Usage: Clustering-Demo*.jar (-p|--port) PORT");
			throw exc;
		}
	}
	

	public static void main (String ...args) {
		(new Server(args)).start();
	}
	
}
