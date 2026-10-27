package com.chatappfrontend.frontend.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class AlertUtils {
    private static final String TIMER_KEY = "alert-timer";

    public static void showError(Label label, String message){
        show(label, message);
    }

    public static void showSuccess(Label label, String message){
        show(label, message);
    }

    private static void show(Label label, String message){
        stopTimer(label);

        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));

        delay.setOnFinished(_ -> hide(label));

        label.getProperties().put(TIMER_KEY, delay);

        delay.play();
    }

    public static void hide(Label label){
        stopTimer(label);

        label.setVisible(false);
        label.setManaged(false);
    }

    private static void stopTimer(Label label){
        Object timer = label.getProperties().remove(TIMER_KEY);

        if(timer instanceof PauseTransition pause){
            pause.stop();
        }
    }
}