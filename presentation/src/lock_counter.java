final SharedData sd = this.vertx.sharedData();

sd.getCounter("name", ar -> {
    Counter counter = ar.result();
    long val = 1L;
    long exp = 4L;

    counter.get(cr -> {});
    counter.addAndGet(val, cr -> {});
    counter.getAndAdd(val, cr -> {});
    counter.incrementAndGet(cr -> {});
    counter.getAndIncrement(cr -> {});
    counter.decrementAndGet(cr -> {});
    counter.compareAndSet(exp, val, cr -> {});
});

// sd.getLockWithTimeout
sd.getLock("name", ar -> {
    Lock lock = ar.result();
    // do stuff
    lock.release();
});
