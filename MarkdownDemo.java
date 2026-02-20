///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-core:0.2.0-SNAPSHOT
//DEPS dev.tamboui:tamboui-widgets:0.2.0-SNAPSHOT
//DEPS dev.tamboui:tamboui-toolkit:0.2.0-SNAPSHOT
//DEPS dev.tamboui:tamboui-jline3-backend:0.2.0-SNAPSHOT
//DEPS org.commonmark:commonmark:0.27.1
//DEPS org.commonmark:commonmark-ext-gfm-tables:0.27.1
//DEPS org.commonmark:commonmark-ext-gfm-strikethrough:0.27.1
//DEPS org.commonmark:commonmark-ext-autolink:0.27.1
//DEPS org.commonmark:commonmark-ext-task-list-items:0.27.1
//SOURCES src/main/java/dev/tamboui/widgets/Markdown.java

import dev.tamboui.buffer.Buffer;
import dev.tamboui.widgets.Markdown;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Terminal;

import java.io.IOException;

public class MarkdownDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        String mdText = """
                # Hello TamboUI

                This is a **bold** and *italic* text.
                A long sentence: Zero-Width Joiner (ZWJ) sequences combine multiple code points into a single visible glyph. `CharWidth.substringByWidth()` automatically preserves these sequences. Always use `CharWidth` for text width calculations (see [Unicode and Display Width Handling](#unicode-and-display-width-handling)) For widgets that need to track selection, scroll position, or other state. Here is some `code` and an image: ![Logo](logo.png)

                > This is a blockquote

                ## Features
                1. Numbered lists
                2. Links to [TamboUI](https://tamboui.dev)
                ---
                | Syntax | Description |
                | --- | --- |
                | Header | Title |
                | Paragraph | Text that is incredibly long and should absolutely wrap multiple times so we can test the new soft wrap formatting implementation inside the table cell constraints! |
                - Paragraphs
                - Bold and Italic
                - [ ] Fix bugs
                - [x] Write code
                - ~~Mistakes~~ Corrections
                - Check out https://github.com/glaforge/tamboui
                ```
                System.out.println("Fenced code!");
                ```""";

        Markdown.State state = new Markdown.State(mdText);
        Markdown widget = new Markdown(Style.EMPTY);

        try (Backend backend = BackendFactory.create();
             Terminal<Backend> terminal = new Terminal<>(backend)) {

            backend.enterAlternateScreen();
            backend.enableRawMode();
            terminal.hideCursor();

            terminal.draw(frame -> {
                Buffer buffer = frame.buffer();
                Rect area = new Rect(2, 2, frame.width() - 4, frame.height() - 4);
                widget.render(area, buffer, state);
            });

            // Wait 10 seconds before exiting
            Thread.sleep(10000);
        }
    }
}
