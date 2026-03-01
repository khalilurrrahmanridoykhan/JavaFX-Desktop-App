package com.example;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MapView extends StackPane {
    public static class MapRegion {
        private final String name;
        private final SVGPath path;
        private final Color baseColor;
        private final double value;

        MapRegion(String name, SVGPath path, Color baseColor, double value) {
            this.name = name;
            this.path = path;
            this.baseColor = baseColor;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public SVGPath getPath() {
            return path;
        }

        public Color getBaseColor() {
            return baseColor;
        }

        public double getValue() {
            return value;
        }
    }

    private final Group mapGroup = new Group();
    private final Scale zoomScale = new Scale(1, 1, 0, 0);
    private final Pane overlay = new Pane();
    private final List<MapRegion> regions = new ArrayList<>();

    private final VBox infoCard = new VBox(6);
    private final Label infoTitle = new Label();
    private final Label infoBody = new Label();

    private boolean fitted = false;
    private double baseScale = 1.0;

    public MapView() {
        getStyleClass().add("map-view");
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_LEFT);

        mapGroup.getTransforms().add(zoomScale);
        mapGroup.setManaged(false);
        mapGroup.setOpacity(0);
        mapGroup.setEffect(new DropShadow(24, Color.rgb(29, 44, 68, 0.28)));
        mapGroup.setCache(true);
        mapGroup.setCacheHint(CacheHint.QUALITY);

        overlay.setPickOnBounds(false);

        loadSvg("/assets/map.svg");
        getChildren().addAll(mapGroup, overlay);

        configureInfoCard();
        playIntro();

        widthProperty().addListener((obs, oldValue, newValue) -> fitToView());
        heightProperty().addListener((obs, oldValue, newValue) -> fitToView());
    }

    public List<MapRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    public Group getMapGroup() {
        return mapGroup;
    }

    public Scale getZoomScale() {
        return zoomScale;
    }

    public Bounds getMapBounds() {
        return mapGroup.getLayoutBounds();
    }

    public double getBaseScale() {
        return baseScale;
    }

    public void showInfo(MapRegion region, Point2D scenePoint) {
        Point2D localPoint = sceneToLocal(scenePoint);

        infoTitle.setText(region.getName());
        infoBody.setText(String.format(Locale.ENGLISH,
                "Heat index: %.0f\nScroll to zoom, drag to pan.",
                region.getValue() * 100));

        infoCard.applyCss();
        infoCard.layout();

        double cardWidth = infoCard.prefWidth(-1);
        double cardHeight = infoCard.prefHeight(-1);
        double x = clamp(localPoint.getX() + 18, 18, getWidth() - cardWidth - 18);
        double y = clamp(localPoint.getY() + 18, 18, getHeight() - cardHeight - 18);

        infoCard.relocate(x, y);
        if (!infoCard.isVisible()) {
            infoCard.setVisible(true);
            infoCard.setOpacity(0);
            infoCard.setScaleX(0.96);
            infoCard.setScaleY(0.96);
        }

        FadeTransition fade = new FadeTransition(Duration.millis(220), infoCard);
        fade.setFromValue(infoCard.getOpacity());
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(220), infoCard);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    public void hideInfo() {
        if (!infoCard.isVisible()) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(160), infoCard);
        fade.setToValue(0);
        fade.setOnFinished(event -> infoCard.setVisible(false));
        fade.play();
    }

    public void addMarker(String label, double x, double y) {
        Circle pulse = new Circle(10, Color.web("#ff7a6b"));
        pulse.setOpacity(0.5);

        Circle pin = new Circle(5.5, Color.web("#ff5f5f"));
        Circle core = new Circle(2.2, Color.WHITE);

        Group marker = new Group(pulse, pin, core);
        marker.setTranslateX(x);
        marker.setTranslateY(y);

        Tooltip tooltip = new Tooltip(label);
        tooltip.getStyleClass().add("map-tooltip");
        Tooltip.install(marker, tooltip);

        ScaleTransition pulseScale = new ScaleTransition(Duration.seconds(1.6), pulse);
        pulseScale.setFromX(0.6);
        pulseScale.setFromY(0.6);
        pulseScale.setToX(1.8);
        pulseScale.setToY(1.8);
        pulseScale.setCycleCount(ScaleTransition.INDEFINITE);

        FadeTransition pulseFade = new FadeTransition(Duration.seconds(1.6), pulse);
        pulseFade.setFromValue(0.55);
        pulseFade.setToValue(0);
        pulseFade.setCycleCount(FadeTransition.INDEFINITE);

        new ParallelTransition(pulseScale, pulseFade).play();

        mapGroup.getChildren().add(marker);
    }

    private void configureInfoCard() {
        infoCard.getStyleClass().add("info-card");
        infoTitle.getStyleClass().add("info-title");
        infoBody.getStyleClass().add("info-body");
        infoBody.setWrapText(true);
        infoBody.setMaxWidth(220);
        infoCard.getChildren().addAll(infoTitle, infoBody);
        infoCard.setVisible(false);
        infoCard.setManaged(false);
        overlay.getChildren().add(infoCard);
    }

    private void playIntro() {
        FadeTransition fade = new FadeTransition(Duration.millis(900), mapGroup);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void loadSvg(String resourcePath) {
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Map SVG not found: " + resourcePath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(input);
            document.getDocumentElement().normalize();

            NodeList paths = document.getElementsByTagName("path");
            int index = 1;
            for (int i = 0; i < paths.getLength(); i++) {
                Element element = (Element) paths.item(i);
                String d = element.getAttribute("d");
                if (d == null || d.isBlank()) {
                    continue;
                }

                String name = element.getAttribute("name");
                if (name == null || name.isBlank()) {
                    name = element.getAttribute("id");
                }
                if (name == null || name.isBlank()) {
                    name = "Region " + index;
                }

                double value = valueForName(name);
                Color base = colorForValue(value);

                SVGPath path = new SVGPath();
                path.setContent(d.trim());
                path.getStyleClass().add("map-path");
                path.setFill(base);
                path.setStroke(Color.web("#f5f7ff"));
                path.setStrokeWidth(0.7);

                MapRegion region = new MapRegion(name, path, base, value);
                regions.add(region);
                mapGroup.getChildren().add(path);
                index++;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load map SVG", ex);
        }
    }

    private void fitToView() {
        if (fitted || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Bounds bounds = mapGroup.getLayoutBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }

        double padding = 40;
        double scale = Math.min(
                (getWidth() - padding * 2) / bounds.getWidth(),
                (getHeight() - padding * 2) / bounds.getHeight());

        zoomScale.setX(scale);
        zoomScale.setY(scale);
        zoomScale.setPivotX(bounds.getMinX());
        zoomScale.setPivotY(bounds.getMinY());
        baseScale = scale;

        double scaledWidth = bounds.getWidth() * scale;
        double scaledHeight = bounds.getHeight() * scale;
        double offsetX = (getWidth() - scaledWidth) / 2 - bounds.getMinX() * scale;
        double offsetY = (getHeight() - scaledHeight) / 2 - bounds.getMinY() * scale;

        mapGroup.setTranslateX(offsetX);
        mapGroup.setTranslateY(offsetY);
        fitted = true;
    }

    private static double valueForName(String name) {
        int hash = Math.abs(Objects.hash(name));
        return (hash % 100) / 100.0;
    }

    private static Color colorForValue(double value) {
        double hue = 210 - (value * 110);
        double saturation = 0.45 + (value * 0.25);
        double brightness = 0.92;
        return Color.hsb(hue, saturation, brightness);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
