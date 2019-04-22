package com.onek.consts;

public enum MessageEvent {

    PAY_CALLBACK(1, "付款回调");

    private int state;
    private String stateInfo;

    MessageEvent(int state, String stateInfo) {
        this.state = state;
        this.stateInfo = stateInfo;
    }

    public int getState() {
        return state;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public static MessageEvent stateOf(int index) {
        for (MessageEvent state : values()) {
            if (state.getState() == index) {
                return state;
            }
        }
        return null;
    }
}
