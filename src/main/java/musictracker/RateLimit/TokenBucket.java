package musictracker.RateLimit;

import java.util.function.LongSupplier;

public class TokenBucket {

    public record Result(boolean allowed, long remaining, long retryAfterSeconds) {
    }

    private final long capacity;
    private final double refillPerNano;
    private final LongSupplier clock;

    private double tokens;
    private long lastRefill;

    public TokenBucket(long capacity, double refillPerSecond, LongSupplier clock) {
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000d;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.getAsLong();
    }

    public synchronized Result tryConsume() {
        refill();

        if (tokens >= 1) {
            tokens -= 1;
            return new Result(true, (long) tokens, 0);
        }

        double missingTokens = 1 - tokens;
        long waitSeconds = (long) Math.ceil(missingTokens / (refillPerNano * 1_000_000_000d));
        return new Result(false, 0, Math.max(1, waitSeconds));
    }

    public synchronized boolean isFull() {
        refill();
        return tokens >= capacity;
    }

    private void refill() {
        long now = clock.getAsLong();
        double newTokens = (now - lastRefill) * refillPerNano;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefill = now;
    }
}