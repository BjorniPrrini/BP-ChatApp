package com.chatappfrontend.frontend.controller;

import com.chatappfrontend.frontend.util.AppConfig;
import com.chatappfrontend.frontend.util.ThemeStorage;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Properties;

public class ThemeManagerController {
    @FXML
    private ColorPicker accentColorPicker;
    @FXML
    private ColorPicker panelColorPicker;
    @FXML
    private ColorPicker textColorPicker;
    @FXML
    private Slider opacitySlider;
    @FXML
    private Label opacityValueLabel;

    @FXML
    public void initialize() {
        Properties properties = ThemeStorage.load();

        String accent = properties.getProperty("glow.color", AppConfig.get("glow.color"));
        String text = properties.getProperty("text.color", AppConfig.get("text.color"));
        String panel = properties.getProperty("panel.color", AppConfig.get("panel.color"));
        double opacity = Double.parseDouble(properties.getProperty("panel.opacity", AppConfig.get("panel.opacity")));

        accentColorPicker.setValue(Color.web(accent));
        panelColorPicker.setValue(Color.web(panel));
        textColorPicker.setValue(Color.web(text));
        opacitySlider.setValue(opacity);
        opacityValueLabel.setText(Math.round(opacity * 100) + "%");
    }

    @FXML
    public void handlePreviewChange(){
        Color accent = accentColorPicker.getValue();
        Color panel = panelColorPicker.getValue();
        Color text = textColorPicker.getValue();
        double opacity = opacitySlider.getValue();

        opacityValueLabel.setText(Math.round(opacity * 100) + "%");

        applyTheme(accent, panel, text, opacity);
    }

    @FXML
    public void handleReset(){
        String glowColor = AppConfig.get("glow.color");
        String textColor = AppConfig.get("text.color");
        String panelColor = AppConfig.get("panel.color");

        double opacity = Double.parseDouble(AppConfig.get("panel.opacity"));

        accentColorPicker.setValue(Color.web(glowColor));
        panelColorPicker.setValue(Color.web(panelColor));
        textColorPicker.setValue(Color.web(textColor));
        opacitySlider.setValue(opacity);
        opacityValueLabel.setText(Math.round(opacity * 100) + "%");

        applyTheme(Color.web(glowColor), Color.web(panelColor), Color.web(textColor), opacity);
    }

    @FXML
    public void handleSave() {
        Color accent = accentColorPicker.getValue();
        Color panel = panelColorPicker.getValue();
        Color text = textColorPicker.getValue();
        double opacity = opacitySlider.getValue();

        applyTheme(accent, panel, text, opacity);

        ThemeStorage.save(accent.toString(), text.toString(), panel.toString(), opacity);
    }

    private void applyTheme(Color accent, Color panel, Color text, double opacity) {
        Scene scene = accentColorPicker.getScene();

        if(scene == null) {
            return;
        }

        ThemeStorage.applyTheme(scene, accent, panel, text, opacity);
    }
}