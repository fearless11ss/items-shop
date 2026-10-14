package ru.stas.itemsshop.client;

public final class ClientState {
    private static long balance;

    private ClientState() {}

    public static long balance() {
        return balance;
    }

    static void setBalance(long value) {
        balance = Math.max(0L, value);
    }
}
