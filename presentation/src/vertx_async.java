VertxOptions opts = new VertxOptions();
opts.setClusterPort(CLUSTER_PORT);
opts.setClusterHost(CLUSTER_HOST);

Vertx.clusteredVertx(opts, ar ->
    final Vertx vertx = ar.result()
);
