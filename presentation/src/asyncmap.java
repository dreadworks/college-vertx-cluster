final SharedData sd = this.vertx.SharedData();
sd.<String, String>getClusterWideMap("name", ar -> {

    final AsyncMap<String, String> map = ar.result();
    map.put("foo", "bar", putResult -> {
        map.get("foo", getResult -> {
            String val = getResult.result(); // "bar" ?
        });
    });

});
