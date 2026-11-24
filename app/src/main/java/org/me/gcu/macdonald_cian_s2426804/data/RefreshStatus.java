package org.me.gcu.macdonald_cian_s2426804.data;

/** State of the most recent refresh, shown in the toolbar / status banner. */
public class RefreshStatus {

    public enum State { LOADING, SUCCESS, FAILED }

    private final State state;
    private final String message;
    private final long lastSuccessMillis;
    private final long nextRefreshMillis;

    public RefreshStatus(State state, String message, long lastSuccessMillis, long nextRefreshMillis) {
        this.state = state;
        this.message = message;
        this.lastSuccessMillis = lastSuccessMillis;
        this.nextRefreshMillis = nextRefreshMillis;
    }

    public State getState() { return state; }
    public String getMessage() { return message; }
    public long getLastSuccessMillis() { return lastSuccessMillis; }
    public long getNextRefreshMillis() { return nextRefreshMillis; }
    public boolean isLoading() { return state == State.LOADING; }
}
