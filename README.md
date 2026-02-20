# TamboUI Markdown Widget

This project provides a Markdown widget for **TamboUI**, allowing you to parse and render styled Markdown directly in your terminal user interfaces!

Powered by [CommonMark Java](https://github.com/commonmark/commonmark-java), it supports a wide array of generic Markdown syntax blocks and inline formatting options.

## Features

- **Standard Blocks**: Headings (`#`), Paragraphs (automatic soft word-wrapping), Blockquotes (`>`), Thematic Breaks (`---`).
- **Inline Formatting**: **Bold**, *Italic*, ~~Strikethrough~~, `Inline Code`.
- **Code & Elements**:
  - Fenced and indented code blocks (rendered with distinct background and foreground colors).
  - Images and Links (rendered using colored anchor text and alt representations).
- **Lists**: Ordered strings (`1.`), Bullet lists (`•`), and nested task lists (`[ ]`, `[x]`).
- **Tables**: GFM Tables built perfectly with column-width bounding rules to scale gracefully inside your requested `Rect` area bounds!

## Usage

Using the `Markdown` widget in your application requires creating a state holding your textual data and passing it to the widget during TamboUI's rendering phase.

```java
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.Markdown;

// ... Inside your UI setup ...

String mdText = """
        # Hello TamboUI

        This is a **bold** and *italic* text.
        Here is a list:
        - Item 1
        - [x] Task complete
        """;

// 1. Initialize State
Markdown.State state = new Markdown.State(mdText);

// 2. Initialize Widget with a base style (optional)
Markdown widget = new Markdown(Style.EMPTY);

// 3. Render onto an area block and buffer within a draw cycle
terminal.draw(frame -> {
    Buffer buffer = frame.buffer();
    Rect area = new Rect(2, 2, frame.width() - 4, frame.height() - 4);

    widget.render(area, buffer, state);
});
```

### Trying out the Demo

A JBang script is included at the root of the project to instantly visualize the widget's capabilities.

Make sure you have [JBang](https://jbang.dev/) installed, then run:
```bash
jbang MarkdownDemo.java
```

## Running Tests

The logic behind line breaking, symbol extraction, and style mapping leverages a suite of assertions verifying behavior inside a custom `TestBuffer`.

To execute the test suite (using Gradle):
```bash
./gradlew test
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).
