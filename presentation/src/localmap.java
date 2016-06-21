final SharedData sd = this.vertx.SharedData();
LocalMap<String, String> local = sd.getLocalMap("mapname");

local.put("foo", "bar");
local.get("foo"); // "bar"
