package com.example.demo3;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Queue;

public class CNC3DViewer {
    private double anchorX, anchorY;
    private double angleX = 0, angleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate cameraTranslate = new Translate(0, 0, -800);
    private final double minZoom = 100, maxZoom = 1000;
    private double zoomDistance = 500;
    private double bedLength = 300, bedWidth = 300, bedDepth = 10;
    private double wallHeight = 200;

    private double cncZInit = -100;
    private double cncX = 0;
    private double cncY = 0;
    private double cncZ = 0;
    private double cncRotX = 0;
    private double cncRotY = 0;
    private double cncRotZ = 0;
    private Group cncTool;
    private Queue<Runnable> movementQueue = new LinkedList<>();
    private boolean isMoving = false;



    private PerspectiveCamera camera;
    private Group root;
    private SubScene subScene;

    public CNC3DViewer(double width, double height) {
        root = new Group();

        // CNC Bed
        Box cncBed = new Box(bedLength, bedWidth, bedDepth);
        cncBed.setMaterial(new PhongMaterial(Color.DARKKHAKI));
        root.getChildren().add(cncBed);

        // CNC Tool
        cncTool = createCNCTool();
        cncTool.setTranslateZ(cncZInit);
        cncZ = cncZInit;

        cncTool.setRotationAxis(Rotate.X_AXIS);
        cncTool.setRotate(90);

        Group walls = createWalls();
        root.getChildren().addAll(cncTool, walls);

        // Camera Setup
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        Group cameraGroup = new Group();
        cameraGroup.getChildren().add(camera);
        cameraGroup.getTransforms().addAll(rotateX, rotateY, cameraTranslate);
        root.getChildren().add(cameraGroup);

        // Scene Setup
        subScene = new SubScene(root, width, height, true, null);
        subScene.setFill(Color.LIGHTGRAY);
        subScene.setCamera(camera);


        // Orbit Controls
        subScene.setOnMousePressed(this::onMousePressed);
        subScene.setOnMouseDragged(this::onMouseDragged);
        subScene.setOnScroll(this::onScroll);
    }

    public SubScene getSubScene() {
        return subScene;
    }

    private void onMousePressed(MouseEvent event) {
        anchorX = event.getSceneX();
        anchorY = event.getSceneY();
    }

    private void onMouseDragged(MouseEvent event) {
        double deltaX = event.getSceneX() - anchorX;
        double deltaY = event.getSceneY() - anchorY;

        if (event.isPrimaryButtonDown()) {
            angleY += deltaX * 0.3;
            angleX -= deltaY * 0.3;
            int maxAngle = 90;
            if(Math.abs(angleY) > maxAngle) angleY = Math.signum(angleY) * maxAngle;
            if(Math.abs(angleX) > maxAngle) angleX = Math.signum(angleX) * maxAngle;

            rotateY.setAngle(angleY);
            rotateX.setAngle(angleX);
        }

        anchorX = event.getSceneX();
        anchorY = event.getSceneY();
    }

    private void onScroll(ScrollEvent event) {
        double zoomFactor = event.getDeltaY() * -5;
        zoomDistance += zoomFactor;
        zoomDistance = Math.max(minZoom, Math.min(maxZoom, zoomDistance));
        cameraTranslate.setZ(-zoomDistance);
    }

    private Group createCNCTool() {
        Group toolGroup = new Group();
        Cylinder spindle = new Cylinder(15, 50);
        spindle.setMaterial(new PhongMaterial(Color.DARKSEAGREEN));
        spindle.setTranslateY(-25);
        Cylinder toolBit = new Cylinder(5, 30);
        toolBit.setMaterial(new PhongMaterial(Color.GREEN));
        toolBit.setTranslateY(15);
        toolGroup.getChildren().addAll(spindle, toolBit);
        return toolGroup;
    }
    private Group createWalls() {
        Group wallGroup = new Group();


        // Material for walls (semi-transparent)
        PhongMaterial wallMaterial = new PhongMaterial(Color.rgb(100, 100, 255, 0.3));

        // Create 4 vertical walls
        Box wall1 = new Box(bedLength, 2, wallHeight);  // North Wall
        wall1.setMaterial(wallMaterial);
        wall1.setTranslateZ(-wallHeight/ 2);
        wall1.setTranslateY(-bedWidth / 2);



        Box wall3 = new Box(2, bedWidth, wallHeight);  // West Wall
        wall3.setMaterial(wallMaterial);
        wall3.setTranslateX(-bedLength / 2);
        wall3.setTranslateZ(-wallHeight / 2);


        // Add Grid Lines to Each Wall
        wallGroup.getChildren().addAll(wall1, wall3);
        //wallGroup.getChildren().addAll(createGridLines());

        return wallGroup;
    }

    public void moveX(double value, boolean isRelative, double speed, boolean applyErrors) {
        movementQueue.add(() -> animateX(value, isRelative, speed, applyErrors));
        executeNextMovement();
    }

    public void moveY(double value, boolean isRelative, double speed, boolean applyErrors) {
        movementQueue.add(() -> animateY(value, isRelative, speed, applyErrors));
        executeNextMovement();
    }

    private void executeNextMovement() {
        if (!isMoving && !movementQueue.isEmpty()) {
            isMoving = true;
            Runnable nextMove = movementQueue.poll();
            nextMove.run();
        }
    }

    private void animateX(double value, boolean isRelative, double speed, boolean applyErrors) {
        double startX = cncX;
        double targetX = isRelative ? cncX + value : value;

        if (applyErrors) {
            targetX += introduceError(targetX);
        }

        double distance = Math.abs(targetX - startX);
        double durationMillis = (distance / speed) * 1000;
        if (durationMillis < 50) durationMillis = 50;

        Timeline timeline = new Timeline();
        int steps = 60;
        double stepDuration = durationMillis / steps;
        double stepSize = (targetX - startX) / steps;

        for (int i = 0; i <= steps; i++) {
            double newX = startX + (stepSize * i);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(stepDuration * i),
                    event -> cncTool.setTranslateX(newX)
            ));
        }

        timeline.setCycleCount(1);
        double finalTargetX = targetX;
        timeline.setOnFinished(event -> {
            cncX = finalTargetX;
            isMoving = false;
            executeNextMovement(); // Start next movement after finishing
        });

        timeline.play();
    }

    private void animateY(double value, boolean isRelative, double speed, boolean applyErrors) {
        double startY = cncY;
        double targetY = isRelative ? cncY + value : value;

        if (applyErrors) {
            targetY += introduceError(targetY);
        }

        double distance = Math.abs(targetY - startY);
        double durationMillis = (distance / speed) * 1000;
        if (durationMillis < 50) durationMillis = 50;

        Timeline timeline = new Timeline();
        int steps = 60;
        double stepDuration = durationMillis / steps;
        double stepSize = (targetY - startY) / steps;

        for (int i = 0; i <= steps; i++) {
            double newY = startY + (stepSize * i);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(stepDuration * i),
                    event -> cncTool.setTranslateY(newY)
            ));
        }

        timeline.setCycleCount(1);
        double finalTargetY = targetY;
        timeline.setOnFinished(event -> {
            cncY = finalTargetY;
            isMoving = false;
            executeNextMovement();
        });

        timeline.play();
    }

    public void moveZ(double value, boolean isRelative, double speed, boolean applyErrors) {
        movementQueue.add(() -> animateZ(value, isRelative, speed, applyErrors));
        executeNextMovement();
    }

    private void animateZ(double value, boolean isRelative, double speed, boolean applyErrors) {
        double startZ = cncZ;
        double targetZ = isRelative ? cncZ + value : value;

        if (applyErrors) {
            targetZ += introduceError(targetZ);
        }

        double distance = Math.abs(targetZ - startZ);
        double durationMillis = (distance / speed) * 1000;
        if (durationMillis < 50) durationMillis = 50;

        Timeline timeline = new Timeline();
        int steps = 60;
        double stepDuration = durationMillis / steps;
        double stepSize = (targetZ - startZ) / steps;

        for (int i = 0; i <= steps; i++) {
            double newZ = startZ + (stepSize * i);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(stepDuration * i),
                    event -> cncTool.setTranslateZ(newZ)
            ));
        }

        timeline.setCycleCount(1);
        double finalTargetZ = targetZ;
        timeline.setOnFinished(event -> {
            cncZ = finalTargetZ;
            isMoving = false;
            executeNextMovement(); // Continue with next movement
        });

        timeline.play();
    }

    public void resetCNC(){
        cncTool.setTranslateX(0);
        cncX = 0;
        cncTool.setTranslateY(0);
        cncY = 0;
        cncTool.setTranslateZ(cncZInit);
        cncZ = cncZInit;
    }


    private double introduceError(double target) {
        double errorFactor = Math.random() * 0.5;  // Example: Random error
        return target * errorFactor;
    }
    public void setSize(double width, double height){
        subScene.setWidth(width);
        subScene.setHeight(height);
    }


}
