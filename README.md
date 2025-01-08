# JavaFX - TextAreaHighlighter

## Summary of the Function of the Java Class "Highlighter"

The Highlighter class allows to highlighting and/or underlining text in a JavaFX TextArea or TextField. It is initialized through the constructor with a corresponding TextInputControl.

        textAreaHighlighter = new Highlighter(textArea);
        textFieldHighlighter = new Highlighter(textField);

The class provides various styles for highlighting, including `HIGHLIGHT`, `UNDERLINE`, and `WAVY_UNDERLINE`. Users can add text substrings or specific indices to be highlighted, and the class automatically updates the display when the text or size of the input field changes. 

        textAreaHighlighter.addSubstring(Highlighter.Style.HIGHLIGHT, "Lorem", Color.YELLOW);
        textAreaHighlighter.addSubstring(Highlighter.Style.HIGHLIGHT,"diam nonumy eirmod", Color.LIGHTGREEN);
        textAreaHighlighter.addSubstring(Highlighter.Style.WAVY_UNDERLINE, "consetetur sadipscing", Color.RED);
        textAreaHighlighter.addSubstring(Highlighter.Style.UNDERLINE, "eirmod tempor", Color.BLUE);

        textAreaHighlighter.addIndex(Highlighter.Style.HIGHLIGHT,12,Color.ORANGE);
        textAreaHighlighter.addIndex(Highlighter.Style.HIGHLIGHT,13,Color.ORANGE);
        textAreaHighlighter.addIndex(Highlighter.Style.HIGHLIGHT,14,Color.ORANGE);

Additionally, highlights can be cleared, and the selection opacity can be adjusted. The class uses internal data structures to manage the highlighted texts. In the Highlighter class, a background image is created to visually represent the highlighted text.


![example](/screenshot.png)

