package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScreenOffReturnHomeTest {
    private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    private static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";

    @Test
    public void screenOffWhileAppOpenReturnsHome() {
        assertTrue(MainActivity.shouldReturnHomeOnScreenChange(true, ACTION_SCREEN_OFF));
    }

    @Test
    public void screenOnWhileAppOpenReturnsHome() {
        assertTrue(MainActivity.shouldReturnHomeOnScreenChange(true, ACTION_SCREEN_ON));
    }

    @Test
    public void screenOffWhileAlreadyHomeDoesNotReturn() {
        assertFalse(MainActivity.shouldReturnHomeOnScreenChange(false, ACTION_SCREEN_OFF));
    }

    @Test
    public void otherActionDoesNotReturnHome() {
        assertFalse(MainActivity.shouldReturnHomeOnScreenChange(true, "android.intent.action.TIME_TICK"));
        assertFalse(MainActivity.shouldReturnHomeOnScreenChange(false, null));
    }
}

