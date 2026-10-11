package com.dframework.fx;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.dframework.swing.CloseAction;

/**
 * The JavaFX counterpart of {@code DFrame}: a {@link Stage} with a fluent
 * builder, a choice of what closing does (with an optional "are you sure?"),
 * window icons, and a memory of where the user left it.
 *
 * <pre>{@code
 * DStage.builder()
 *         .title("My App")
 *         .content(root)
 *         .size(900, 600)
 *         .closeAction(CloseAction.CONFIRM_EXIT)
 *         .rememberBounds("myapp.main")
 *         .show();
 * }</pre>
 */
public class DStage extends Stage {

    private static final Preferences ROOT = Preferences.userRoot().node("com/dframework/fx/windows");

    private CloseAction closeAction = CloseAction.EXIT;
    private String confirmMessage;
    private Preferences bounds;

    public DStage() {
        setOnCloseRequest(this::closeRequested);
    }

    public static Builder builder() {
        return new Builder();
    }

    public CloseAction getCloseAction() {
        return closeAction;
    }

    /** What the window's close button does. */
    public void setCloseAction(CloseAction closeAction) {
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
    }

    /** Sets the window icon from a classpath resource (PNG, GIF or JPEG). */
    public void setIcon(Class<?> owner, String resource) {
        try (InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("No such icon: " + resource);
            }
            getIcons().setAll(new Image(in));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Can't read icon " + resource, e);
        }
    }

    /**
     * Restores the position, size and maximised state saved under {@code key},
     * and saves them whenever the window closes. A saved position that is no
     * longer on any screen is ignored, so the window doesn't open out of reach.
     */
    public void rememberBounds(String key) {
        bounds = ROOT.node(Objects.requireNonNull(key, "key"));
        double w = bounds.getDouble("width", -1);
        double h = bounds.getDouble("height", -1);
        if (w > 0 && h > 0) {
            double x = bounds.getDouble("x", Double.NaN);
            double y = bounds.getDouble("y", Double.NaN);
            setWidth(w);
            setHeight(h);
            if (!Double.isNaN(x) && !Screen.getScreensForRectangle(new Rectangle2D(x, y, w, h)).isEmpty()) {
                setX(x);
                setY(y);
            } else {
                centerOnScreen();
            }
            setMaximized(bounds.getBoolean("maximized", false));
        }
        addEventHandler(WindowEvent.WINDOW_HIDING, e -> saveBounds());
    }

    /** The saved bounds node, if {@link #rememberBounds} was called. */
    Optional<Preferences> boundsNode() {
        return Optional.ofNullable(bounds);
    }

    void saveBounds() {
        if (bounds == null) {
            return;
        }
        bounds.putBoolean("maximized", isMaximized());
        if (!isMaximized() && !isIconified()) {   // keep the normal size while maximised
            bounds.putDouble("x", getX());
            bounds.putDouble("y", getY());
            bounds.putDouble("width", getWidth());
            bounds.putDouble("height", getHeight());
        }
    }

    private void closeRequested(WindowEvent event) {
        switch (closeAction) {
            case NOTHING -> event.consume();
            case CONFIRM_EXIT, CONFIRM_DISPOSE -> {
                if (!confirmed()) {
                    event.consume();
                } else if (closeAction == CloseAction.CONFIRM_EXIT) {
                    saveBounds();
                    Platform.exit();
                }
            }
            case EXIT -> {
                saveBounds();
                Platform.exit();
            }
            default -> {
                // DISPOSE and HIDE: JavaFX hides a closed stage; it can be shown again.
            }
        }
    }

    private boolean confirmed() {
        boolean exit = closeAction == CloseAction.CONFIRM_EXIT;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                confirmMessage != null ? confirmMessage : exit ? "Are you sure you want to exit?" : "Close this window?",
                ButtonType.YES, ButtonType.NO);
        alert.initOwner(this);
        alert.setTitle(exit ? "Exit" : "Close");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    /** Builds and shows a {@link DStage}. */
    public static final class Builder {

        private String title = "";
        private Parent content;
        private double width = -1;
        private double height = -1;
        private CloseAction closeAction = CloseAction.EXIT;
        private String confirmMessage;
        private String rememberKey;
        private Class<?> iconOwner;
        private String iconResource;
        private final java.util.List<String> stylesheets = new java.util.ArrayList<>();

        private Builder() {
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public Builder content(Parent content) {
            this.content = Objects.requireNonNull(content, "content");
            return this;
        }

        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder closeAction(CloseAction closeAction) {
            this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
            return this;
        }

        /** The question asked by the CONFIRM_ close actions. */
        public Builder confirmMessage(String message) {
            this.confirmMessage = message;
            return this;
        }

        public Builder icon(Class<?> owner, String resource) {
            this.iconOwner = owner;
            this.iconResource = resource;
            return this;
        }

        public Builder rememberBounds(String key) {
            this.rememberKey = key;
            return this;
        }

        public Builder stylesheet(String url) {
            stylesheets.add(url);
            return this;
        }

        public DStage build() {
            DStage stage = new DStage();
            stage.setTitle(title);
            stage.setCloseAction(closeAction);
            stage.confirmMessage = confirmMessage;
            if (iconResource != null) {
                stage.setIcon(iconOwner, iconResource);
            }
            Scene scene = width > 0 ? new Scene(content, width, height) : new Scene(content);
            scene.getStylesheets().addAll(stylesheets);
            stage.setScene(scene);
            if (rememberKey != null) {
                stage.rememberBounds(rememberKey);
            }
            return stage;
        }

        public DStage show() {
            DStage stage = build();
            stage.show();
            return stage;
        }
    }
}
