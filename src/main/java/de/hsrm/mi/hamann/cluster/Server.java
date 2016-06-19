package de.hsrm.mi.hamann.cluster;

import java.util.Arrays;
import java.util.Iterator;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Server {
	private final Logger log = LoggerFactory.getLogger(Server.class);
	
	private Vertx vertx;
	private int port;
	
	
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
			
			}
		}
	}
	
	
	public void start () {
		VertxOptions opts = new VertxOptions();
		
		opts.setClustered(true);
		
		
		Vertx.clusteredVertx(opts, res -> {
			if (res.failed()) {
				final String fmt = "creating vertx failed: '%s'"; 
				throw new RuntimeException(String.format(fmt, res.cause()));
			}
			
			log.info("created clustered vertx instance");
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
