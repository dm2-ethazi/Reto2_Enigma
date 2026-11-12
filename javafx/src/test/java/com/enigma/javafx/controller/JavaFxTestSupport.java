package com.enigma.javafx.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class JavaFxTestSupport {
    protected static final long FX_TIMEOUT_SECONDS = 15;

    @BeforeAll
    protected static void startJavaFxToolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.complete(null);
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                Platform.setImplicitExit(false);
                started.complete(null);
            });
        }
        started.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    protected static MainController loadController() throws Exception {
        return runOnFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                    JavaFxTestSupport.class.getResource("/com/enigma/javafx/main-view.fxml"));
            loader.load();
            return loader.getController();
        });
    }

    protected static void invokeWhileDismissingAlert(MainController controller, String methodName)
            throws Exception {
        runOnFxThread(() -> {
            Platform.runLater(() -> {
                for (Window window : new ArrayList<>(Window.getWindows())) {
                    if (window.isShowing() && window instanceof Stage stage
                            && stage.getScene() != null
                            && stage.getScene().getRoot() instanceof javafx.scene.control.DialogPane) {
                        window.hide();
                    }
                }
            });
            invoke(controller, methodName);
            return null;
        });
    }

    protected static Stage showControllerWindow(MainController controller) throws Exception {
        return runOnFxThread(() -> {
            TextField sourceField = field(controller, "sourcePathField", TextField.class);
            Parent root = sourceField;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();
            return stage;
        });
    }

    protected static void assertControlsDisabled(MainController controller, boolean disabled)
            throws Exception {
        runOnFxThread(() -> {
            assertEquals(disabled, field(controller, "encryptButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "sourcePathField", TextField.class).isDisable());
            assertEquals(disabled,
                    field(controller, "destinationDirectoryField", TextField.class).isDisable());
            assertEquals(disabled,
                    field(controller, "decryptSourcePathField", TextField.class).isDisable());
            assertEquals(disabled,
                    field(controller, "referencePathField", TextField.class).isDisable());
            assertEquals(disabled, field(controller, "chooseSourceButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "chooseDestinationButton", Button.class).isDisable());
            assertEquals(disabled,
                    field(controller, "chooseEncryptedFileButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "copyPasswordButton", Button.class).isDisable());
            assertEquals(disabled, field(controller, "clearSelectionButton", Button.class).isDisable());
            return null;
        });
    }

    protected static <T> T runOnFxThread(Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    protected static Object invoke(Object target, String methodName) throws Exception {
        return invoke(target, methodName, new Class<?>[0]);
    }

    protected static Object invoke(Object target, String methodName, Class<?>[] parameterTypes,
                                 Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    protected static <T> T field(Object target, String name) throws Exception {
        return field(target, name, null);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(target);
        return type == null ? (T) value : type.cast(value);
    }
}
