package de.hsrm.mi.hamann.cluster;

import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

public class Verticle extends AbstractVerticle {
	final private Logger log = LoggerFactory.getLogger(Verticle.class);
	
	private SharedData sd;
	
	final private Statistics stats = new Statistics();
	final int port;
	
	public Verticle (int port) {
		this.port = port;
	}
	
	
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
		
		// local race
		Long counter = local.get(key);
		local.put(key, counter + 1);
		
		return local.get(key);
	}
	
	
	
	private void putClusterCounter (
			AsyncMap<String, Long> cluster,
			String key,
			Long val,
			Consumer<Long> handler
	){
		cluster.put(key, val, putRes -> {
			if (putRes.failed()) {
				throw new RuntimeException(putRes.cause());
			}
			handler.accept(val);
		});
	}
	
	private void getClusterCounter (Consumer<Long> handler) {
		final String name = "clustercounter";
		final String key = "counter";
		
		// take cover - heavy elbow incoming!
		
		this.sd.<String, Long>getClusterWideMap(name, mapRes -> {
			
			if (mapRes.failed()) {
				throw new RuntimeException(mapRes.cause());
			}
			
			AsyncMap<String, Long> cluster = mapRes.result();
			cluster.get(key, getRes -> {
				
				if (getRes.failed()) {
					throw new RuntimeException(getRes.cause());
				}
				
				// initialize
				if (getRes.result() == null) {
					this.putClusterCounter(cluster, key, (long) 1, handler);
					
				// increment
				} else {
					
					// cluster-wide race
					Long count = getRes.result().longValue() + 1;
					this.putClusterCounter(cluster, key, count, handler);
					
				}
			});
			
		});
	}
	
	
	private void getCounter (Consumer<Long> handler) {
		final String name = "counter";
		
		this.sd.getCounter(name, counterRes -> {
			if (counterRes.failed()) {
				throw new RuntimeException(counterRes.cause());
			}
			
			counterRes.result().incrementAndGet(incRes -> {
				if (incRes.failed()) {
					throw new RuntimeException(incRes.cause());
				}
				
				handler.accept(incRes.result());
			});
		});
	}
	
	
	private void response (Consumer<String> handler) {
		this.stats.setLocal(this.getLocalCounter());
		this.getClusterCounter(clusterCounter -> {
			this.stats.setCluster(clusterCounter);
			this.getCounter(counter -> {
				this.stats.setCounter(counter);
				handler.accept(Json.encodePrettily(this.stats).toString());
			});
		});
	}
	
	
	private void handle (HttpServerRequest req) {
		this.response(msg -> {
			final String clen = Integer.toString(msg.getBytes().length);
		
			req.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_LENGTH, clen)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.write(msg)
				.end();
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

	// TODO this.proxy for generic error handling
	// TODO int -> Integer -> Long 
	
}
