package com.example.demo3;

import com.example.demo3.CNC3DViewer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CNCApp extends Application {
    // Constants for layout sizes
    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 600;
    private static final double LEFT_PANEL_WIDTH = 300;
    private static final double EDITOR_HEIGHT = 400;
    private static final double SPACING = 10;
    private static final double PADDING = 10;
    private CNC3DViewer visualizer;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Left Panel - G-code Editor
        TextArea gcodeEditor = new TextArea();
        gcodeEditor.setPromptText("Enter G-code here...");
        gcodeEditor.setPrefWidth(LEFT_PANEL_WIDTH);
        gcodeEditor.setPrefHeight(EDITOR_HEIGHT);

        // Button Panel - Row Layout
        HBox buttonBox = new HBox(SPACING); // Buttons in a row with spacing
        buttonBox.setPadding(new Insets(PADDING, 0, 0, 0)); // Padding at the top
        Button runAllButton = new Button("Run All");
        Button stepButton = new Button("Step");
        Button resetButton = new Button("Reset");
        buttonBox.getChildren().addAll(runAllButton, stepButton, resetButton);

        // Left Pane - Contains Editor + Buttons
        VBox leftPane = new VBox(SPACING); // Vertical layout with spacing
        leftPane.setPadding(new Insets(PADDING)); // Padding around the left pane
        leftPane.getChildren().addAll(gcodeEditor, buttonBox);
        leftPane.setPrefWidth(LEFT_PANEL_WIDTH);

        // Instructional text
        Text instructionText = new Text("Drag for better view");
        instructionText.setFill(Color.SLATEGREY);
        instructionText.setFont(Font.font(16));
        // Wrap text in a VBox and position slightly lower
        VBox textContainer = new VBox(instructionText);
        textContainer.setAlignment(Pos.BOTTOM_CENTER); // Align text lower
        textContainer.setPadding(new Insets(0, 0, 20, 0)); // Push it down by 20px
        textContainer.setMouseTransparent(true);

        // Right Panel - 3D Visualizer (fills remaining space and keeps square shape)
        double availableSize = Math.min(WINDOW_WIDTH - LEFT_PANEL_WIDTH - 2 * PADDING, WINDOW_HEIGHT - 2 * PADDING);
        visualizer = new CNC3DViewer(availableSize, availableSize);
        StackPane rightPane = new StackPane(visualizer.getSubScene(), textContainer);
        rightPane.setPadding(new Insets(PADDING));
        rightPane.setMinSize(availableSize, availableSize);

        //moving test
       //visualizer.moveZ(50, false, 20, false);
//        visualizer.moveY(50, false, 20, false);
//        visualizer.moveX(50, false, 20, false);
//        visualizer.moveY(-50, false, 20, false);
//        visualizer.moveX(0, false, 20, false);
        //parseGCode("G91\nF20\nZ-50");


        // Set layout
        root.setLeft(leftPane);
        root.setCenter(rightPane);

        // Adjust sizes dynamically on resize
        root.widthProperty().addListener((obs, oldVal, newVal) -> adjustLayout(root, gcodeEditor, rightPane, newVal.doubleValue(), root.getHeight()));
        root.heightProperty().addListener((obs, oldVal, newVal) -> adjustLayout(root, gcodeEditor, rightPane, root.getWidth(), newVal.doubleValue()));


        //add button handlers
        runAllButton.setOnAction(e -> {
            reset();
            String gcode = gcodeEditor.getText();
            parseGCode(gcode); // Start execution
        });

        resetButton.setOnAction(e -> {
            reset();
        });



        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("CNC Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    private void adjustLayout(BorderPane root, TextArea gcodeEditor, StackPane rightPane, double width, double height) {
        double leftPanelWidth = width * (LEFT_PANEL_WIDTH / WINDOW_WIDTH);
        double editorHeight = height * (EDITOR_HEIGHT / WINDOW_HEIGHT);

        gcodeEditor.setPrefWidth(leftPanelWidth);
        gcodeEditor.setPrefHeight(editorHeight);
        ((VBox) root.getLeft()).setPrefWidth(leftPanelWidth);

        double availableSize = Math.min(width - leftPanelWidth - 2 * PADDING, height - 2 * PADDING);
        rightPane.setMinSize(availableSize, availableSize);
        visualizer.setSize(availableSize,availableSize);
    }

    public void parseGCode(String gcode) {
        String[] lines = gcode.split("\n");
        Pattern pattern = Pattern.compile("([GMXYZF])([-\\d\\.]+)");

        boolean isRelative = false;
        double feedRate = 10; // Default speed

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            Double targetX = null, targetY = null, targetZ = null;

            while (matcher.find()) {
                String command = matcher.group(1);
                double value = Double.parseDouble(matcher.group(2));

                switch (command) {
                    case "G":
                        if (value == 0 || value == 1) isRelative = false;
                        if (value == 90) isRelative = false;
                        if (value == 91) isRelative = true;
                        break;
                    case "X": targetX = value; break;
                    case "Y": targetY = -1*value; break;
                    case "Z": targetZ = -1*value; break;
                    case "F": feedRate = value; break; // Convert mm/min to mm/sec
                }
            }

            if (targetX != null) visualizer.moveX(targetX, isRelative, feedRate, false);
            if (targetY != null) visualizer.moveY(targetY, isRelative, feedRate, false);
            if (targetZ != null) visualizer.moveZ(targetZ, isRelative, feedRate, false);

        }
    }
    public void reset(){
        visualizer.resetCNC();
    }




    public static void main(String[] args) {
        launch(args);
    }
}

