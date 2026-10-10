package com.dframework.swing.close;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;

import org.junit.jupiter.api.Test;

import com.dframework.swing.util.DResources;

class ConfirmCloserTest {

    private final AtomicInteger closes = new AtomicInteger();
    private final ConfirmCloser closer = new ConfirmCloser(null, "Title", "Sure?", closes::incrementAndGet);

    @Test
    void runsCloseActionWhenConfirmed() {
        closer.setPrompt((parent, title, message, icon) -> true);

        assertThat(closer.requestClose()).isTrue();
        assertThat(closes).hasValue(1);
    }

    @Test
    void doesNothingWhenDeclined() {
        closer.setPrompt((parent, title, message, icon) -> false);

        assertThat(closer.requestClose()).isFalse();
        assertThat(closes).hasValue(0);
    }

    @Test
    void windowClosingAndActionEventsBothPrompt() {
        AtomicInteger prompts = new AtomicInteger();
        closer.setPrompt((parent, title, message, icon) -> prompts.incrementAndGet() > 0);

        closer.windowClosing(null);
        closer.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "exit"));

        assertThat(prompts).hasValue(2);
        assertThat(closes).hasValue(2);
    }

    @Test
    void promptReceivesConfiguredTextAndIcon() {
        ImageIcon icon = DResources.icon("question.gif").orElseThrow();
        closer.setTitle("Quit");
        closer.setMessage("Really quit?");
        closer.setIcon(icon);
        closer.setPrompt((parent, title, message, shown) -> {
            assertThat(title).isEqualTo("Quit");
            assertThat(message).isEqualTo("Really quit?");
            assertThat(shown).isSameAs(icon);
            return false;
        });

        closer.requestClose();
    }

    @Test
    void exitCloserUsesLocalisedDefaults() {
        ConfirmCloser exit = ConfirmCloser.exitApplication(null);

        assertThat(exit.getTitle()).isEqualTo("Shutdown Confirm");
        assertThat(exit.getMessage()).isEqualTo("Are you sure you want to shut down?");
        assertThat(exit.getIcon()).isNull();
    }
}
