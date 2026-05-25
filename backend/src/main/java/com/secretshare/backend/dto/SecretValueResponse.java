package com.secretshare.backend.dto;

public class SecretValueResponse {

    private String value;
    private int usesLeft;

    public SecretValueResponse() {}

    public SecretValueResponse(String value, int usesLeft) {
        this.value = value;
        this.usesLeft = usesLeft;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int getUsesLeft() { return usesLeft; }
    public void setUsesLeft(int usesLeft) { this.usesLeft = usesLeft; }
}
