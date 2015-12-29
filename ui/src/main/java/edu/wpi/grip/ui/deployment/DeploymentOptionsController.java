package edu.wpi.grip.ui.deployment;

import edu.wpi.grip.ui.util.SupplierWithIO;
import edu.wpi.grip.ui.util.deployment.DeployedInstanceManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import org.jdeferred.DeferredCallable;
import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import org.jdeferred.impl.DefaultDeferredManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.Consumer;

public abstract class DeploymentOptionsController {

    @FXML
    private TitledPane root;

    @FXML
    private GridPane optionsGrid;

    @FXML
    private Label deployErrorText;

    @FXML
    private ProgressIndicator deploySpinner;

    @FXML
    private Button deployButton;

    private final String title;
    private final Consumer<DeployedInstanceManager> onDeployCallback;

    protected DeploymentOptionsController(String title, Consumer<DeployedInstanceManager> onDeployCallback) {
        this.title = title;
        this.onDeployCallback = onDeployCallback;
        try {
            FXMLLoader.load(DeploymentOptionsController.class.getResource("DeploymentOptions.fxml"), null, null, c -> this);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load FXML", e);
        }
    }

    @FXML
    protected final void initialize() {
        root.setText(title);
        postInit();
    }

    /**
     * Called after the initialize method
     */
    protected abstract void postInit();

    protected abstract Promise<DeployedInstanceManager, String, String> onDeploy();

    @FXML
    private void deploy() {
        deploySpinner.setVisible(true);
        deployButton.setDisable(true);
        onDeploy()
                .progress(this::setErrorText)
                .fail((text) -> {
                    setErrorText(text);
                    Platform.runLater(() -> {
                        deploySpinner.setVisible(false);
                        deployButton.setDisable(false);
                    });
                })
                .done((t) -> {
                    onDeployCallback.accept(t);
                    Platform.runLater(() -> {
                        deploySpinner.setVisible(false);
                        deployButton.setDisable(false);
                    });
                });
    }

    private void setErrorText(String text) {
        Platform.runLater(() -> {
            deployErrorText.setText(text);
            root.requestLayout();
        });
    }

    protected GridPane getOptionsGrid() {
        return optionsGrid;
    }

    protected Button getDeployButton() {
        return deployButton;
    }

    public TitledPane getRoot() {
        return root;
    }

    protected static Promise<InetAddress, Throwable, String> checkInetAddressReachable(SupplierWithIO<InetAddress> address){
        final DeferredManager checkAddressDeferred = new DefaultDeferredManager();
        return checkAddressDeferred.when(new DeferredCallable<InetAddress, String>() {
            @Override
            public InetAddress call() throws Exception {
                final InetAddress inetAddress = address.getWithIO();
                final int attemptCount = 5;
                for (int i = 0; i < attemptCount; i++) {
                    if(inetAddress.isReachable(1000)) {
                        return inetAddress;
                    } else {
                        notify("Attempt " + i + "/" + attemptCount + " failed");
                    }
                }
                throw new IOException("Failed to connect");
            }
        });

    }
}
