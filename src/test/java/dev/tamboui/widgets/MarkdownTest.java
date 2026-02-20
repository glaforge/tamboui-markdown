package dev.tamboui.widgets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MarkdownTest {

    static class TestBuffer {
        private final Buffer buffer;
        private final int width;
        private final int height;

        public TestBuffer(int width, int height) {
            this.width = width;
            this.height = height;
            this.buffer = Buffer.empty(new Rect(0, 0, width, height));
        }

        public Buffer getBuffer() {
            return buffer;
        }

        public String getLine(int y) {
            if (y >= 0 && y < height) {
                StringBuilder line = new StringBuilder();
                boolean hasContent = false;
                for (int x = 0; x < width; x++) {
                    Cell cell = buffer.get(x, y);
                    if (cell != null && !cell.symbol().isEmpty() && !cell.symbol().equals(" ")) {
                        line.append(cell.symbol());
                        hasContent = true;
                    } else if (hasContent) { // Add trailing spaces if content precedes
                        line.append(" ");
                    }
                }
                return line.toString().stripTrailing();
            }
            return "";
        }

        public Style getStyleAt(int x, int y) {
            Cell cell = buffer.get(x, y);
            return cell != null ? cell.style() : null;
        }
    }

    private void assertRenderedLine(String markdownText, int expectedLines, String... expectedOutput) {
        Markdown widget = new Markdown(Style.EMPTY);
        Markdown.State state = new Markdown.State(markdownText);
        TestBuffer testBuffer = new TestBuffer(80, 24);
        Rect area = new Rect(0, 0, 80, 24);

        widget.render(area, testBuffer.getBuffer(), state);

        // Assert output
        for(int i = 0; i < expectedOutput.length; i++) {
             assertEquals(expectedOutput[i], testBuffer.getLine(i), "Line " + i + " mismatching");
        }
    }

    private void assertRenderedLineAt(String markdownText, int yOffset, String expectedOutput) {
        Markdown widget = new Markdown(Style.EMPTY);
        Markdown.State state = new Markdown.State(markdownText);
        TestBuffer testBuffer = new TestBuffer(80, 24);
        Rect area = new Rect(0, 0, 80, 24);

        widget.render(area, testBuffer.getBuffer(), state);

        assertEquals(expectedOutput, testBuffer.getLine(yOffset), "Line " + yOffset + " mismatching");
    }

    @Test
    public void testParagraph() {
        assertRenderedLine("Hello World", 1, "Hello World");
    }

    @Test
    public void testParagraphWrapping() {
         String longText = "This is a very long paragraph that should wrap automatically because it exceeds the predefined width of our eighty character test buffer area bounds.";
         Markdown widget = new Markdown(Style.EMPTY);
         Markdown.State state = new Markdown.State(longText);
         TestBuffer testBuffer = new TestBuffer(40, 24); // Smaller width to force wrap
         Rect area = new Rect(0, 0, 40, 24);
         widget.render(area, testBuffer.getBuffer(), state);

         // Assert first line length and content
         String line0 = testBuffer.getLine(0);
         assertEquals("This is a very long paragraph that", line0.trim());
         String line1 = testBuffer.getLine(1);
         assertEquals("should wrap automatically because it", line1.trim());
    }

    @Test
    public void testHeadings() {
        String md = "# Heading 1\n## Heading 2";
        assertRenderedLineAt(md, 0, "# Heading 1");
        assertRenderedLineAt(md, 2, "## Heading 2");
    }

    @Test
    public void testLists() {
        String md = "- Item 1\n- Item 2";
        assertRenderedLine(md, 2, "• Item 1", "• Item 2");
    }

    @Test
    public void testOrderedLists() {
        String md = "1. Item 1\n2. Item 2";
        assertRenderedLine(md, 2, "1. Item 1", "2. Item 2");
    }

    @Test
    public void testTaskLists() {
        String md = "- [ ] Unchecked\n- [x] Checked";
        assertRenderedLine(md, 2, "• [ ] Unchecked", "• [x] Checked");
    }

    @Test
    public void testFormatting() {
         String md = "**Bold** and *Italic* and ~~Strike~~";
         Markdown widget = new Markdown(Style.EMPTY);
         Markdown.State state = new Markdown.State(md);
         TestBuffer testBuffer = new TestBuffer(80, 24);
         Rect area = new Rect(0, 0, 80, 24);
         widget.render(area, testBuffer.getBuffer(), state);

         assertEquals("Bold and Italic and Strike", testBuffer.getLine(0));

         assertNotNull(testBuffer.getStyleAt(0, 0));
    }

    @Test
    public void testBlockquote() {
        String md = "> Quote here";
        assertRenderedLine(md, 1, "> Quote here");
    }

    @Test
    public void testCodeAndFencedCode() {
        String md = "Inline `code`\n\n```\nBlock code\n```";
        assertRenderedLineAt(md, 0, "Inline code");
        assertRenderedLineAt(md, 2, "Block code");
    }

    @Test
    public void testLinksAndImages() {
         String md = "[Link](https://example.com) ![Img](img.png)";
         assertRenderedLineAt(md, 0, "Link ![Image](img.png)");
    }

    @Test
    public void testTables() {
        String md = """
                | Col1 | Col2 |
                |---|---|
                | Val1 | Val2 |""";

         Markdown widget = new Markdown(Style.EMPTY);
         Markdown.State state = new Markdown.State(md);
         TestBuffer testBuffer = new TestBuffer(40, 24);
         Rect area = new Rect(0, 0, 40, 24);
         widget.render(area, testBuffer.getBuffer(), state);

         assertEquals("| Col1 | Col2 |", testBuffer.getLine(0).replaceAll("\\s+", " ").trim());
         assertEquals("|-|-|", testBuffer.getLine(1).replaceAll("\\s+", " ").trim().replaceAll("-{2,}", "-"));
         assertEquals("| Val1 | Val2 |", testBuffer.getLine(2).replaceAll("\\s+", " ").trim());
    }

     @Test
    public void testThematicBreak() {
        String md = "---";
        assertRenderedLineAt(md, 0, "---");
    }
}

