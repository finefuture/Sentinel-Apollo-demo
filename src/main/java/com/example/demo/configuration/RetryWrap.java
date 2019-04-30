package com.example.demo.configuration;

/**
 * Retry implementation
 *
 * @author longqiang
 */
public abstract class RetryWrap {

    private final int retryCount;

    public RetryWrap(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void run() {
        runWithRetry(0);
    }

    private void runWithRetry(int retriedCount) {
        boolean result = doing();
        if (!result && retriedCount < retryCount) {
            afterFailure();
            runWithRetry(++retriedCount);
        }
    }

    /**
     * something need to be retry
     * @return boolean doing result, false will retry
     */
    protected abstract boolean doing();

    /**
     * if doing() result is false, something to do before the next retry start
     */
    protected abstract void afterFailure();

}
