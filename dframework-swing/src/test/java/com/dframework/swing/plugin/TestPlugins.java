package com.dframework.swing.plugin;

import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

/** Plugins used by the tests, registered in META-INF/services under src/test/resources. */
final class TestPlugins {

    private TestPlugins() {
    }

    /** Works, and counts its lifecycle calls. */
    public static final class Good implements DPlugin {
        static int created;
        static int shown;
        static int disposed;

        @Override
        public String id() {
            return "test.good";
        }

        @Override
        public String name() {
            return "Good Plugin";
        }

        @Override
        public String category() {
            return "Tests";
        }

        @Override
        public JComponent createView(PluginContext context) {
            created++;
            context.audit("hello", Map.of("from", "good"));
            return new JLabel("good plugin view");
        }

        @Override
        public void onShow() {
            shown++;
        }

        @Override
        public void dispose() {
            disposed++;
        }
    }

    /** Throws while starting, as a buggy plugin might. */
    public static final class Broken implements DPlugin {
        @Override
        public String id() {
            return "test.broken";
        }

        @Override
        public String name() {
            return "Broken Plugin";
        }

        @Override
        public String category() {
            return "Tests";
        }

        @Override
        public JComponent createView(PluginContext context) {
            throw new IllegalStateException("database driver missing");
        }
    }
}
