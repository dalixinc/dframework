package com.dframework.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Frame;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WindowStateTest {

    @ParameterizedTest
    @EnumSource(WindowState.class)
    void roundTripsThroughFrameState(WindowState state) {
        assertThat(WindowState.fromFrameState(state.frameState())).isEqualTo(state);
    }

    @ParameterizedTest
    @EnumSource(WindowState.class)
    void minimisedWinsOverOtherBits(WindowState state) {
        assertThat(WindowState.fromFrameState(state.frameState() | Frame.ICONIFIED)).isEqualTo(WindowState.MINIMIZED);
    }
}
