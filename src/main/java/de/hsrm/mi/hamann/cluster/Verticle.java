package de.hsrm.mi.hamann.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Verticle extends AbstractVerticle {
	final private Logger log = LoggerFactory.getLogger(Verticle.class);
	
	
	final int port;
	
	
	public Verticle (int port) {
		this.port = port;
	}
	
	
	private String response () {
		return "{ \"what\": \"up\" }";
	}
	
	
	private void handle (HttpServerRequest req) {
		final String msg  = this.response(); 
		final String clen = Integer.toString(msg.getBytes().length);
		
		req.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_LENGTH, clen)
			.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.write(msg)
			.end();
	}
	
	
	@Override public void start (Future<Void> fut) {
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
