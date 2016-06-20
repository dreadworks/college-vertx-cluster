package de.hsrm.mi.hamann.cluster;

import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

/**
 * Implements counters using different types of shared data
 *
 * @author Felix Hamann
 *
 */
public class Verticle extends AbstractVerticle {
	final private Logger log = LoggerFactory.getLogger(Verticle.class);
	
	private SharedData sd;
	
	final private Statistics stats = new Statistics();
	final int port;
	
	public Verticle (int port) {
		this.port = port;
	}
	
	
	// UTILITY
	
	private void write (HttpServerRequest req, int code, String msg) {
		final String clen = Integer.toString(msg.getBytes().length);
		
		req.response()
			.setStatusCode(code)
			.putHeader(HttpHeaders.CONTENT_LENGTH, clen)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.write(msg)
			.end();
	}
	
	
	private <T> Handler<AsyncResult<T>> proxy (HttpServerRequest req, Consumer<T> handler) {
		return res -> {
			if (res.failed()) {
				JsonObject msg = new JsonObject();
				msg.put("error", res.cause().getMessage());
				this.write(req, 500, msg.toString());
			}
			
			handler.accept(res.result());
		};
	}
	
	
	// SHARED DATA 
	
	/**
	 * Return the counter saved in the vertx instances' local map
	 * 
	 * @return
	 */
	private Long getLocalCounter () {
		final String name = "localcounter";
		final String key = "counter";
		
		LocalMap<String, Long> local = this.sd.getLocalMap(name);
		
		if (local == null) {
			throw new RuntimeException("could not retrieve the local map");
		}
		
		if (!local.keySet().contains(key)) {
			local.put(key, (long) 0);
		}
		
		// no local race?
		Long counter = local.get(key);
		local.put(key, counter + 1);
		
		return local.get(key);
	}
	
	
	
	/**
	 * Retrieve and return the current value of the cluster wide map
	 * 
	 * There is a race condition!
	 * 
	 * @param req
	 * @param handler
	 */
	private void getClusterCounter (HttpServerRequest req, Consumer<Long> handler) {
		final String name = "clustercounter";
		final String key = "counter";
		
		// take cover - heavy elbow incoming!
		
		this.sd.<String, Long>getClusterWideMap(name, this.proxy(req, cluster -> {
			cluster.get(key, this.proxy(req, count -> {
				
				if (count == null) { count = 0L; }
				final Long val = count + 1L;
				cluster.put(key, val, this.proxy(req, res -> handler.accept(val)));

			}));
		}));
	}
	
	
	/**
	 * Increment and get a cluster wide counter
	 * 
	 * @param handler
	 */
	private void getCounter (HttpServerRequest req, Consumer<Long> handler) {
		final String name = "counter";
		
		this.sd.getCounter(name, this.proxy(req, counter -> {
			counter.incrementAndGet(this.proxy(req, inc -> {
				handler.accept(inc);
			}));
		}));
	}
	
	
	/**
	 * Handle everything (!)
	 * 
	 * @param req
	 */
	private void handle (HttpServerRequest req) {
		this.stats.setLocal(this.getLocalCounter());
		
		this.getClusterCounter(req, clusterCounter -> {
			this.stats.setCluster(clusterCounter);
			
			this.getCounter(req, counter -> {
				this.stats.setCounter(counter);
				
				String msg = Json.encodePrettily(this.stats).toString();
				this.write(req, 200, msg);
				
			});
		});
	}
	
	
	@Override public void start (Future<Void> fut) {
		this.sd = this.vertx.sharedData();
		
		this.vertx.createHttpServer()
			.requestHandler(this::handle)
			.listen(this.port, res -> {
				
				if (res.failed()) {
					fut.fail(res.cause());
				}
				
				
				final String fmt = "deployed verticle, listening on :'%d'";
				this.log.info(String.format(fmt, this.port));
				fut.complete();
			});
	}
	
}
