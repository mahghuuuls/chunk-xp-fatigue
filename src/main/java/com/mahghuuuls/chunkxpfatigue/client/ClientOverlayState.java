package com.mahghuuuls.chunkxpfatigue.client;

import com.mahghuuuls.chunkxpfatigue.network.OverlaySnapshot;

public final class ClientOverlayState {
    private static volatile OverlaySnapshot snapshot;
    private static long generation;
    private ClientOverlayState() { }
    public static OverlaySnapshot get() { return snapshot; }
    public static synchronized long beginSession() { snapshot = null; return ++generation; }
    public static synchronized void endSession() { snapshot = null; generation++; }
    public static synchronized long currentGeneration() { return generation; }
    public static synchronized boolean setIfCurrent(OverlaySnapshot value, long expectedGeneration) {
        if (generation != expectedGeneration) return false;
        snapshot = value;
        return true;
    }
}
