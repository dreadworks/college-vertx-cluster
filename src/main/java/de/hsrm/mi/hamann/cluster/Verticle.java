package de.hsrm.mi.hamann.cluster;

import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
	
	final private static String LOCALMAP  = "localmap";
	final private static String KEY_LOCAL = "local";
	final private static String KEY_BUS   = "bus";
	final private static String BUS_KEY   = "counter"; 
	
	private SharedData sd;
	private EventBus bus;
	
	final private Statistics stats = new Statistics();
	final int port;
	
	public Verticle (int port) {
		this.port = port;
	}
	
	
	// UTILITY
	
	/**
	 * Send a json encoded response to the client
	 * 
	 * @param req
	 * @param code
	 * @param json
	 */
	private void write (HttpServerRequest req, int code, JsonObject json) {
		this.write(req, code, json.toString());
	}

	/**
	 * Send a json encoded response to the client
	 * 
	 * @param req
	 * @param code
	 * @param msg
	 */
	private void write (HttpServerRequest req, int code, String msg) {
		final String clen = Integer.toString(msg.getBytes().length);
		req.response()
			.setStatusCode(code)
			.putHeader(HttpHeaders.CONTENT_LENGTH, clen)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.write(msg)
			.end();
	}
	
	
	/**
	 * Handle asynchronous failure generically
	 * 
	 * @param req
	 * @param handler
	 * @return
	 */
	private <T> Handler<AsyncResult<T>> proxy (HttpServerRequest req, Consumer<T> handler) {
		return res -> {
			if (res.failed()) {
				JsonObject msg = new JsonObject();
				msg.put("error", res.cause().getMessage());
				this.write(req, 500, msg);
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
	private Long getLocalCounter (String key) {
		LocalMap<String, Long> local = this.sd.getLocalMap(LOCALMAP);
		
		if (local == null) {
			throw new RuntimeException("could not retrieve the local map");
		}
		
		if (!local.keySet().contains(key)) {
			local.put(key, 0L);
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
	 * Handle all http stuff
	 * 
	 * @param req
	 */
	private void handle (HttpServerRequest req) {
		this.stats.setLocal(this.getLocalCounter(KEY_LOCAL));
		this.stats.setBus(this.sd.<String, Long>getLocalMap(LOCALMAP).get(KEY_BUS));
		
		this.getClusterCounter(req, clusterCounter -> {
			this.stats.setCluster(clusterCounter);
			
			this.getCounter(req, counter -> {
				this.stats.setCounter(counter);
				
				this.bus.publish(BUS_KEY, "");
				String msg = Json.encodePrettily(this.stats);
				this.write(req, 200, msg);
				
			});
		});
	}
	
	
	@Override public void start (Future<Void> fut) {
		this.sd = this.vertx.sharedData();
		this.bus = this.vertx.eventBus();
		
		
		// handle event bus messages
		
		this.sd.getLocalMap(LOCALMAP).put(KEY_BUS, 0L);
		MessageConsumer<String> consumer = this.bus.consumer(BUS_KEY);
		consumer.handler(msg -> {
			
			LocalMap<String, Long> local = this.sd.getLocalMap(LOCALMAP);
			Long count = local.get(KEY_BUS);
			if (count == null) { count = 0L; };
			local.put(KEY_BUS, count + 1L);
			
		});
		
		// create http server
		
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
