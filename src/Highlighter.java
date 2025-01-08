/**
 * The Highlighter class can be used to highlight and/or underline text in a JavaFX TextArea or TextField.
 * The corresponding TextArea or TextField must be passed in the constructor.
 *
 * @author Tobias Martin | tomar.de
 * @version 1.0
 */
public class Highlighter {
    public enum Style {
        HIGHLIGHT, UNDERLINE, WAVY_UNDERLINE
    }
    private final double SELECTION_OPACITY = 0.4;
    private final TextInputControl textInputControl;
    private final TextInputControlSkin<?> textInputControlSkin;
    private record HighlightedString(Style style, String substring, Color color) {}
    private record HighlightedIndex(Style style, int index, Color color) {}
    private final List<HighlightedString> highlightedSubstringList = new ArrayList<>();
    private final List<HighlightedIndex> highlightedIndexList = new ArrayList<>();

    /**
     * Creates a new Highlighter object to highlight and/or underline text in a TextArea or TextField control.
     *
     * @param textInputControl TextArea or TextField control
     */
    public Highlighter(TextInputControl textInputControl) {
        switch (textInputControl) {
            case TextArea textArea -> textInputControlSkin = new HightlighterTextAreaSkin(textArea);
            case TextField textField -> textInputControlSkin = new HighlighterTextFieldSkin(textField);
            default -> throw new IllegalArgumentException("No TextArea or TextField object.");
        }
        this.textInputControl = textInputControl;
        textInputControl.setSkin(textInputControlSkin);
        textInputControl.textProperty().addListener(observable -> refreshHighlights());
        textInputControl.widthProperty().addListener(observable -> refreshHighlights());
        textInputControl.heightProperty().addListener(observable -> refreshHighlights());
        if (textInputControl instanceof TextArea) {
            Platform.runLater(() -> {
                ScrollPane scrollPane = (ScrollPane) textInputControl.lookup(".scroll-pane");
                ScrollBar verticalScrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
                verticalScrollBar.visibleProperty().addListener(observable -> refreshHighlights());
            });
        } else {
            textInputControl.caretPositionProperty().addListener(observable -> refreshHighlights());
        }
        setSelectionOpacity(SELECTION_OPACITY);
    }

    /**
     * Add a new highlightable substring
     *
     * @param style     Enumeration Style {@code Highlighter.Style.HIGHLIGHT}, {@code Highlighter.Style.UNDERLINE}, {@code Highlighter.Style.WAVY_UNDERLINE}
     * @param substring a not empty string
     * @param color     a Color object
     */
    public void addSubstring(Style style, String substring, Color color) {
        if (style == null)
            throw new IllegalArgumentException("Style object must not be null.");
        if (substring.isEmpty())
            throw new IllegalArgumentException("Substring value must not be a empty string.");
        if (color == null)
            throw new IllegalArgumentException("Color object must not be null.");
        highlightedSubstringList.add(new HighlightedString(style, substring, color));
        refreshHighlights();
    }

    /**
     * Clear all highlighted substrings from list.
     */
    public void clearSubstringList() {
        highlightedSubstringList.clear();
    }

    /**
     * Add a new highlightable index
     *
     * @param style Enumeration Style {@code Highlighter.Style.HIGHLIGHT}, {@code Highlighter.Style.UNDERLINE}, {@code Highlighter.Style.WAVY_UNDERLINE}
     * @param index 0 or a positive value
     * @param color a Color object
     */
    public void addIndex(Style style, int index, Color color) {
        if (style == null) throw new IllegalArgumentException("Style object must not be null.");
        if (index < 0) throw new IllegalArgumentException("Index must not be smaller then 0.");
        if (color == null) throw new IllegalArgumentException("Color object must not be null.");
        highlightedIndexList.add(new HighlightedIndex(style, index, color));
        refreshHighlights();
    }

    /**
     * Clear all highlighted indices from list.
     */
    public void clearIndexList() {
        highlightedIndexList.clear();
    }

    /**
     * Refreshes und updates the highlights (the background image), if changes (e.g. text size) are not detected and refreshed automatically.
     */
    public void refreshHighlights() {
        Platform.runLater(() -> {
            if ((highlightedSubstringList.isEmpty()) && (highlightedIndexList.isEmpty())) return;
            Region content = getContent();
            if (content == null) return;
            if ((int) content.getWidth() == 0 || (int) content.getHeight() == 0) return;
            WritableImage writableImage = new WritableImage((int) (content.getWidth() + 100), (int) (content.getHeight()) + 100);
            List<HighlightedIndex> combinedHighlightedIndexList = new ArrayList<>(highlightedIndexList);
            for (HighlightedString highlightedString : highlightedSubstringList) {
                for (int index : substringToIndices(highlightedString.substring))
                    combinedHighlightedIndexList.add(new HighlightedIndex(highlightedString.style, index, highlightedString.color));
            }
            for (HighlightedIndex highlightedIndex : combinedHighlightedIndexList) {
                if (highlightedIndex.style == Style.HIGHLIGHT) {
                    highlightIndex(writableImage, highlightedIndex.index, highlightedIndex.color);
                }
                if (highlightedIndex.style == Style.UNDERLINE) {
                    underlineIndex(writableImage, highlightedIndex.index, highlightedIndex.color);
                }
                if (highlightedIndex.style == Style.WAVY_UNDERLINE) {
                    wavyUnderlineIndex(writableImage, highlightedIndex.index, highlightedIndex.color);
                }
            }
            BackgroundImage backgroundImage = new BackgroundImage(
                    writableImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
            getContent().setBackground(new Background(backgroundImage));
        });
    }

    /**
     * Set selection opacity (Default value is 0.4).
     *
     * @param opacity from 0.0 to 1.0
     */
    public void setSelectionOpacity(double opacity) {
        if (opacity < 0.0 || opacity > 1.0)
            throw new IllegalArgumentException("Opacity value must be between 0.0 and 1.0");
        if (textInputControl instanceof TextArea) {
            ((HightlighterTextAreaSkin) textInputControlSkin).setSelectionOpacity(opacity);
        } else {
            ((HighlighterTextFieldSkin) textInputControlSkin).setSelectionOpacity(opacity);
        }
    }

    /**
     * Get selection opacity
     *
     * @return opacity from 0.0 to 1.0
     */
    public double getSelectionOpacity() {
        if (textInputControl instanceof TextArea) {
            return ((HightlighterTextAreaSkin) textInputControlSkin).getSelectionOpacity();
        } else {
            return ((HighlighterTextFieldSkin) textInputControlSkin).getSelectionOpacity();
        }
    }

    private Region getContent() {
        if (textInputControl instanceof TextArea) {
            return (Region) textInputControl.lookup(".content");
        } else {
            return (Region) textInputControl.getChildrenUnmodifiable().getFirst();
        }
    }

    private List<Integer> substringToIndices(String substring) {
        List<Integer> substringIndices = new ArrayList<>();
        String text = textInputControl.getText();
        for (int i = 0; i < text.length() - substring.length(); i++) {
            if (text.startsWith(substring, i)) {
                for (int j = i; j < i + substring.length(); j++) substringIndices.add(j);
            }
        }
        return substringIndices;
    }

    private int getUnderlineOffset() {
        if (textInputControl.getText().isEmpty()) return 0;
        return (int) Math.round(textInputControlSkin.getCharacterBounds(0).getHeight() / 6 * -1);
    }

    private double getDrawOffsetLeft() {
        if (textInputControl instanceof TextArea textArea) {
            return textArea.getScrollLeft();
        } else {
            return -getContent().getLayoutX();
        }
    }

    private double getDrawOffsetTop() {
        if (textInputControl instanceof TextArea textArea) {
            return textArea.getScrollTop();
        } else {
            return -getContent().getLayoutY();
        }
    }

    private void highlightIndex(WritableImage writableImage, int index, Color color) {
        int highlightOffsetTop = 1;
        int highlightOffsetHeight = -2;
        drawRectangle(
                writableImage,
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMinX() + getDrawOffsetLeft()),
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMinY() + getDrawOffsetTop() + highlightOffsetTop),
                (int) textInputControlSkin.getCharacterBounds(index).getWidth(),
                (int) (textInputControlSkin.getCharacterBounds(index).getHeight() + highlightOffsetHeight),
                color);
    }

    private void underlineIndex(WritableImage writableImage, int index, Color color) {
        drawHorizontalLine(
                writableImage,
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMinX() + getDrawOffsetLeft()),
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMaxY() + getDrawOffsetTop() + getUnderlineOffset()),
                (int) textInputControlSkin.getCharacterBounds(index).getWidth(),
                color);
    }

    private void wavyUnderlineIndex(WritableImage writableImage, int index, Color color) {
        drawWavyHorizontalLine(
                writableImage,
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMinX() + getDrawOffsetLeft()),
                (int) Math.round(textInputControlSkin.getCharacterBounds(index).getMaxY() + getDrawOffsetTop() + getUnderlineOffset()),
                (int) textInputControlSkin.getCharacterBounds(index).getWidth(),
                color);
    }

    private void drawRectangle(WritableImage writableImage, int x, int y, int width, int height, Color color) {
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int deltaY = 0; deltaY < height; deltaY++) {
            for (int deltaX = 0; deltaX < width; deltaX++) {
                int drawX = x + deltaX;
                int drawY = y + deltaY;
                if ((drawX < 0) || (drawY < 0) || (drawX >= writableImage.getWidth()) || (drawY >= writableImage.getHeight()))
                    continue;
                pixelWriter.setColor(drawX, drawY, color);
            }
        }
    }

    private void drawHorizontalLine(WritableImage writableImage, int x, int y, int length, Color color) {
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int deltaX = 0; deltaX < length; deltaX++) {
            int drawX = x + deltaX;
            int drawY = y;
            if ((drawX < 0) || (drawY < 0) || (drawX >= writableImage.getWidth()) || (drawY >= writableImage.getHeight()))
                continue;
            pixelWriter.setColor(drawX, drawY, color);
        }
    }

    private void drawWavyHorizontalLine(WritableImage writableImage, int x, int y, int length, Color color) {
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        final double[][] OPACITY_MATRIX = {
                {0.3, 1.0, 0.3, 0.0},
                {1.0, 0.3, 1.0, 0.3},
                {0.3, 0.0, 0.3, 1.0}};
        for (int deltaX = 0; deltaX < length; deltaX++) {
            int drawX = x + deltaX;
            int drawY = y;
            if ((drawX < 0) || (drawY < 0) || ((drawY + 1) < 0) || ((drawY + 2) < 0) || (drawX >= writableImage.getWidth()) || ((drawY + 2) >= writableImage.getHeight()))
                continue;
            Color color0 = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * OPACITY_MATRIX[0][(x + deltaX) % 4]);
            Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * OPACITY_MATRIX[1][(x + deltaX) % 4]);
            Color color2 = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getOpacity() * OPACITY_MATRIX[2][(x + deltaX) % 4]);
            if (color0.getOpacity() != 0) pixelWriter.setColor(drawX, drawY, color0);
            if (color1.getOpacity() != 0) pixelWriter.setColor(drawX, drawY + 1, color1);
            if (color2.getOpacity() != 0) pixelWriter.setColor(drawX, drawY + 2, color2);
        }
    }

    private Color blendColors(Color color1, Color color2) {
        double weightingColor1 = color1.getOpacity() / (color1.getOpacity() + color2.getOpacity());
        double weightingColor2 = color2.getOpacity() / (color1.getOpacity() + color2.getOpacity());
        return new Color(
                weightingColor1 * color1.getRed() + weightingColor2 * color2.getRed(),
                weightingColor1 * color1.getGreen() + weightingColor2 * color2.getGreen(),
                weightingColor1 * color1.getBlue() + weightingColor2 * color2.getBlue(),
                Math.max(color1.getOpacity(), color2.getOpacity()));
    }

    // Extend TextAreaSkin and TextFieldSkin class to make protected methods accessible and
    // get and set selection opacity (HighlightFill).
    private static class HightlighterTextAreaSkin extends TextAreaSkin {
        public HightlighterTextAreaSkin(TextArea textArea) {
            super(textArea);
        }

        public void setSelectionOpacity(double opacity) {
            Color color = (Color) getHighlightFill();
            Color colorOpacity = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
            setHighlightFill(colorOpacity);
        }

        public double getSelectionOpacity() {
            Color color = (Color) getHighlightFill();
            return color.getOpacity();
        }

        public Color getSelectionColor() {
            return (Color) getHighlightFill();
        }

        public void setSelectionColor(Color color) {
            setHighlightFill(color);
        }
    }

    private static class HighlighterTextFieldSkin extends TextFieldSkin {
        public HighlighterTextFieldSkin(TextField textField) {
            super(textField);
        }

        public void setSelectionOpacity(double opacity) {
            Color color = (Color) getHighlightFill();
            Color colorOpacity = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
            setHighlightFill(colorOpacity);
        }

        public double getSelectionOpacity() {
            Color color = (Color) getHighlightFill();
            return color.getOpacity();
        }

        public Color getSelectionColor() {
            return (Color) getHighlightFill();
        }

        public void setSelectionColor(Color color) {
            setHighlightFill(color);
        }
    }
}
