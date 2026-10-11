package com.dframework.fxdemo;

import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import atlantafx.base.controls.ToggleSwitch;

import com.dframework.fx.DStage;
import com.dframework.fx.field.DTextField;
import com.dframework.fx.theme.FxTheme;
import com.dframework.fx.theme.FxThemeManager;
import com.dframework.swing.CloseAction;
import com.dframework.swing.DFramework;
import com.dframework.swing.field.InputFilter;
import com.dframework.swing.field.Validator;

/** The JavaFX spike's showcase: a stage, live themes and validated fields. */
public final class FxDemoApp extends Application {

    private static final Preferences PREFS = Preferences.userRoot().node("com/dframework/fxdemo");

    private final Label status = new Label();
    private final ToggleGroup themes = new ToggleGroup();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ignored) {
        FxTheme theme = FxTheme.PRIMER_LIGHT;
        try {
            theme = FxTheme.valueOf(PREFS.get("theme", theme.name()));
        } catch (IllegalArgumentException unknown) {
            // keep the default
        }
        FxThemeManager.apply(theme);

        BorderPane root = new BorderPane();
        root.setTop(menus());
        root.setCenter(form());
        root.setBottom(statusBar());

        DStage.builder()
                .title("DFramework for JavaFX")
                .icon(DFramework.class, "/com/dframework/swing/icons/app.gif")   // shared with the Swing library
                .content(root)
                .size(820, 560)
                .closeAction(CloseAction.CONFIRM_EXIT)
                .rememberBounds("fxdemo.main")
                .show();
    }

    private MenuBar menus() {
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> {
            Stage stage = (Stage) status.getScene().getWindow();
            stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        });
        Menu file = new Menu("File", null, exit);

        Menu theme = new Menu("Theme");
        for (FxTheme t : FxTheme.values()) {
            RadioMenuItem item = new RadioMenuItem(t.displayName());
            item.setToggleGroup(themes);
            item.setUserData(t);
            item.setSelected(FxThemeManager.current().orElse(null) == t);
            item.setOnAction(e -> applyTheme(t));
            if (t == FxTheme.MODENA) {
                theme.getItems().add(new javafx.scene.control.SeparatorMenuItem());
            }
            theme.getItems().add(item);
        }
        return new MenuBar(file, theme);
    }

    private void applyTheme(FxTheme t) {
        FxThemeManager.apply(t);
        PREFS.put("theme", t.name());
        themes.getToggles().forEach(toggle -> toggle.setSelected(toggle.getUserData() == t));
        updateStatus();
    }

    private Parent form() {
        DTextField name = new DTextField();
        name.setPromptText("Your name");
        name.setValidator(Validator.required());

        DTextField age = new DTextField();
        age.setPromptText("0 to 130");
        age.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));
        age.setValidator(Validator.required().and(Validator.integer(0, 130)));

        DTextField email = new DTextField();
        email.setPromptText("name@example.com");
        email.setValidator(Validator.email());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("Name"), name);
        grid.addRow(1, new Label("Age"), age, hint("Digits only: letters can't be typed or pasted"));
        grid.addRow(2, new Label("Email"), email, hint("Optional, but checked if given"));
        GridPane.setHgrow(name, Priority.ALWAYS);

        Label result = new Label();
        Button submit = new Button("Submit");
        submit.setDefaultButton(true);
        BooleanBinding allValid = Bindings.and(name.validProperty(), age.validProperty()).and(email.validProperty());
        submit.disableProperty().bind(allValid.not());   // a binding, where Swing needed a listener per field
        submit.setOnAction(e -> result.setText("Submitted: " + name.getText() + ", " + age.getText()));

        ToggleSwitch dark = new ToggleSwitch("Dark mode");   // an AtlantaFX control, on JavaFX 21
        dark.setSelected(FxThemeManager.current().map(FxTheme::isDark).orElse(false));
        dark.selectedProperty().addListener((o, was, on) -> {
            // Only a change by the user: when a theme chosen from the menu flips the switch, leave it be.
            if (on != FxThemeManager.current().map(FxTheme::isDark).orElse(false)) {
                applyTheme(on ? FxTheme.PRIMER_DARK : FxTheme.PRIMER_LIGHT);
            }
        });
        FxThemeManager.addThemeListener(t -> dark.setSelected(t.isDark()));

        HBox actions = new HBox(12, submit, result);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox fields = new VBox(14, grid, actions);
        TitledPane details = new TitledPane("Your details", fields);
        details.setCollapsible(true);

        Label heading = new Label("DFramework, now in JavaFX");
        heading.setStyle("-fx-font-size: 1.6em; -fx-font-weight: bold;");
        Label intro = new Label("A spike: DStage, live AtlantaFX themes, and DTextField using the same "
                + "Validator and InputFilter classes as the Swing library. Leave a field to see its check; "
                + "Submit is enabled only when every field is valid.");
        intro.setWrapText(true);

        VBox page = new VBox(16, heading, intro, dark, new Separator(), details);
        page.setPadding(new Insets(20));
        return page;
    }

    private static Label hint(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-opacity: 0.65;");
        return l;
    }

    private Parent statusBar() {
        Label version = new Label(DFramework.nameAndVersion() + "  ·  JavaFX " + System.getProperty("javafx.version"));
        version.setStyle("-fx-opacity: 0.6;");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bar = new HBox(status, gap, version);
        bar.setPadding(new Insets(4, 10, 4, 10));
        FxThemeManager.addThemeListener(t -> updateStatus());
        updateStatus();
        return bar;
    }

    private void updateStatus() {
        status.setText("Theme: " + FxThemeManager.current().map(FxTheme::displayName).orElse("?"));
    }
}
