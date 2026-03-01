package com.example;

import java.util.HashMap;
import java.util.Map;
import javafx.animation.FillTransition;
import javafx.application.Platform;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

public class MapController {
    private final MapView mapView;
    private final Group mapGroup;
    private final Scale zoomScale;
    private final Map<SVGPath, FillTransition> activeFills = new HashMap<>();

    private Timeline zoomTimeline;
    private boolean markersAdded = false;
    private boolean dragging = false;
    private boolean zooming = false;

    private double dragAnchorX;
    private double dragAnchorY;
    private double dragStartTranslateX;
    private double dragStartTranslateY;

    private static final double MIN_SCALE_FACTOR = 0.6;
    private static final double MAX_SCALE_FACTOR = 6.0;

    public MapController(MapView mapView) {
        this.mapView = mapView;
        this.mapGroup = mapView.getMapGroup();
        this.zoomScale = mapView.getZoomScale();

        attachRegionHandlers();
        attachPanAndZoom();
        mapView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !markersAdded) {
                Platform.runLater(this::addDemoMarkers);
            }
        });
    }

    private void attachRegionHandlers() {
        for (MapView.MapRegion region : mapView.getRegions()) {
            SVGPath path = region.getPath();
            Tooltip tooltip = new Tooltip(region.getName());
            tooltip.getStyleClass().add("map-tooltip");
            Tooltip.install(path, tooltip);

            path.setOnMouseEntered(event -> {
                animateFill(path, region.getBaseColor().brighter());
                path.setEffect(hoverEffect());
            });

            path.setOnMouseExited(event -> {
                animateFill(path, region.getBaseColor());
                path.setEffect(null);
            });

            path.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }
                mapView.showInfo(region, new Point2D(event.getSceneX(), event.getSceneY()));
                event.consume();
            });
        }

        mapView.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() instanceof SVGPath) {
                return;
            }
            mapView.hideInfo();
        });
    }

    private void attachPanAndZoom() {
        mapView.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragAnchorX = event.getSceneX();
            dragAnchorY = event.getSceneY();
            dragStartTranslateX = mapGroup.getTranslateX();
            dragStartTranslateY = mapGroup.getTranslateY();
            dragging = true;
            updateCacheHint();
        });

        mapView.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            double deltaX = event.getSceneX() - dragAnchorX;
            double deltaY = event.getSceneY() - dragAnchorY;
            mapGroup.setTranslateX(dragStartTranslateX + deltaX);
            mapGroup.setTranslateY(dragStartTranslateY + deltaY);
        });

        mapView.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragging = false;
            updateCacheHint();
        });

        mapView.setOnMouseExited(event -> {
            if (!event.isPrimaryButtonDown()) {
                dragging = false;
                updateCacheHint();
            }
        });

        mapView.addEventFilter(ScrollEvent.SCROLL, this::handleScroll);
    }

    private void handleScroll(ScrollEvent event) {
        double delta = event.getDeltaY();
        if (delta == 0) {
            return;
        }
        double factor = delta > 0 ? 1.12 : 0.88;
        double baseScale = mapView.getBaseScale();
        double target = clamp(zoomScale.getX() * factor,
                baseScale * MIN_SCALE_FACTOR,
                baseScale * MAX_SCALE_FACTOR);

        Point2D pivot = mapGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        animateZoom(target, pivot);
        event.consume();
    }

    private void animateZoom(double targetScale, Point2D pivot) {
        zoomScale.setPivotX(pivot.getX());
        zoomScale.setPivotY(pivot.getY());

        if (zoomTimeline != null) {
            zoomTimeline.stop();
        }

        zooming = true;
        updateCacheHint();

        zoomTimeline = new Timeline(
                new KeyFrame(Duration.millis(160),
                        new KeyValue(zoomScale.xProperty(), targetScale, Interpolator.EASE_BOTH),
                        new KeyValue(zoomScale.yProperty(), targetScale, Interpolator.EASE_BOTH))
        );
        zoomTimeline.setOnFinished(event -> {
            zooming = false;
            updateCacheHint();
        });
        zoomTimeline.play();
    }

    private void animateFill(SVGPath path, Color target) {
        FillTransition existing = activeFills.get(path);
        if (existing != null) {
            existing.stop();
        }
        Color from = path.getFill() instanceof Color ? (Color) path.getFill() : Color.web("#7da6d9");
        FillTransition transition = new FillTransition(Duration.millis(180), path, from, target);
        activeFills.put(path, transition);
        transition.setOnFinished(event -> activeFills.remove(path));
        transition.play();
    }

    private static DropShadow hoverEffect() {
        Glow glow = new Glow(0.45);
        DropShadow shadow = new DropShadow(18, Color.rgb(255, 156, 92, 0.65));
        shadow.setInput(glow);
        return shadow;
    }

    private void updateCacheHint() {
        if (dragging || zooming) {
            mapGroup.setCache(true);
            mapGroup.setCacheHint(CacheHint.SCALE);
        } else {
            mapGroup.setCacheHint(CacheHint.QUALITY);
        }
    }

    private void addDemoMarkers() {
        if (markersAdded) {
            return;
        }
        Bounds bounds = mapView.getMapBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }
        double left = bounds.getMinX();
        double top = bounds.getMinY();
        double width = bounds.getWidth();
        double height = bounds.getHeight();

        mapView.addMarker("Dhaka", left + width * 0.58, top + height * 0.45);
        mapView.addMarker("Chattogram", left + width * 0.74, top + height * 0.65);
        mapView.addMarker("Khulna", left + width * 0.44, top + height * 0.75);
        markersAdded = true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
