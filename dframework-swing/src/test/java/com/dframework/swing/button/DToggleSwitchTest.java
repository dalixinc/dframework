package com.dframework.swing.button;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DToggleSwitchTest {

    @Test
    void togglesLikeAToggleButton() {
        DToggleSwitch toggle = new DToggleSwitch("Wi-Fi");
        List<Integer> changes = new ArrayList<>();
        toggle.addItemListener(e -> changes.add(e.getStateChange()));

        toggle.doClick();
        assertThat(toggle.isSelected()).isTrue();
        toggle.doClick();
        assertThat(toggle.isSelected()).isFalse();

        assertThat(changes).containsExactly(ItemEvent.SELECTED, ItemEvent.DESELECTED);
    }

    @Test
    void knobFollowsStateWhenNotOnScreen() {
        DToggleSwitch toggle = new DToggleSwitch("Dark mode", true);
        assertThat(toggle.getKnobPosition()).isEqualTo(1f);

        toggle.setSelected(false);
        assertThat(toggle.getKnobPosition()).isEqualTo(0f);
    }

    @Test
    void preferredSizeMakesRoomForLabel() {
        DToggleSwitch bare = new DToggleSwitch();
        DToggleSwitch labelled = new DToggleSwitch("Bluetooth");

        assertThat(bare.getPreferredSize().width).isPositive();
        assertThat(labelled.getPreferredSize().width).isGreaterThan(bare.getPreferredSize().width);
        assertThat(bare.getPreferredSize().width).isGreaterThan(bare.getPreferredSize().height);
    }

    @Test
    void onColourIsConfigurable() {
        DToggleSwitch toggle = new DToggleSwitch();
        toggle.setOnColor(Color.GREEN);

        assertThat(toggle.getOnColor()).isEqualTo(Color.GREEN);
    }
}
