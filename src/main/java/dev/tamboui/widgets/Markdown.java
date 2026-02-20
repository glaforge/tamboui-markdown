package dev.tamboui.widgets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.StatefulWidget;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.ext.gfm.tables.*;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.ext.task.list.items.TaskListItemMarker;

import java.util.Arrays;

public class Markdown implements StatefulWidget<Markdown.State> {

    public static class State {
        private String text;

        public State(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private final Style baseStyle;
    private final Parser parser;

    public Markdown(Style baseStyle) {
        this.baseStyle = baseStyle;
        this.parser = Parser.builder()
                .extensions(Arrays.asList(
                        TablesExtension.create(),
                        StrikethroughExtension.create(),
                        AutolinkExtension.create(),
                        TaskListItemsExtension.create()
                ))
                .build();
    }

    @Override
    public void render(Rect area, Buffer buffer, State state) {
        if (state.text() == null || state.text().isEmpty()) {
            return;
        }

        Node document = parser.parse(state.text());
        RenderVisitor visitor = new RenderVisitor(area, buffer, baseStyle);
        document.accept(visitor);
    }

    private static class RenderVisitor extends AbstractVisitor {
        private Rect area;
        private final Buffer buffer;

        private int currentX;
        private int currentY;

        private Style currentStyle;
        private TableContext currentTableContext = null;

        private class TableContext {
            int[] colWidths;
            int[] colXs;
            int rowStartY;
            int currentCol;
            int maxRowHeight;

            TableContext(int[] colWidths, int startX) {
                this.colWidths = colWidths;
                this.colXs = new int[colWidths.length];
                int x = startX;
                for (int i = 0; i < colWidths.length; i++) {
                    this.colXs[i] = x + 2;
                    x += colWidths[i] + 3;
                }
            }

            void startRow(int y) {
                this.rowStartY = y;
                this.currentCol = 0;
                this.maxRowHeight = 1;
            }

            void updateMaxHeight(int h) {
                if (h > maxRowHeight) {
                    maxRowHeight = h;
                }
            }
        }

        private int countColumns(TableBlock table) {
            Node head = table.getFirstChild();
            if (head instanceof TableHead) {
                Node row = head.getFirstChild();
                if (row instanceof TableRow) {
                    int count = 0;
                    Node cell = row.getFirstChild();
                    while (cell != null) {
                        count++;
                        cell = cell.getNext();
                    }
                    return count;
                }
            }
            return 0;
        }

        public RenderVisitor(Rect area, Buffer buffer, Style baseStyle) {
            this.area = area;
            this.buffer = buffer;
            this.currentX = area.x();
            this.currentY = area.y();
            this.currentStyle = baseStyle;
        }

        @Override
        public void visit(Document document) {
            visitChildren(document);
        }

        @Override
        public void visit(Heading heading) {
            Style prevStyle = currentStyle;
            currentStyle = currentStyle.bold().fg(Color.CYAN); // Style headings

            // Advance Y if not at the beginning
            if (currentX > area.x()) {
                newLine();
            }

            // Add '#' prefix depending on level
            String prefix = "#".repeat(heading.getLevel()) + " ";
            printText(prefix);

            visitChildren(heading);

            currentStyle = prevStyle;
            newLine();
            newLine();
        }

        @Override
        public void visit(Paragraph paragraph) {
            boolean inListItem = paragraph.getParent() instanceof ListItem;
            boolean inBlockQuote = paragraph.getParent() instanceof BlockQuote;
            if (currentX > area.x() && !inListItem && !inBlockQuote) {
                newLine();
            }
            visitChildren(paragraph);
            if (!inListItem) {
                newLine();
                newLine();
            } else {
                newLine();
            }
        }

        @Override
        public void visit(Text text) {
            printText(text.getLiteral());
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            Style prev = currentStyle;
            currentStyle = currentStyle.bold();
            visitChildren(strongEmphasis);
            currentStyle = prev;
        }

        @Override
        public void visit(Emphasis emphasis) {
            Style prev = currentStyle;
            currentStyle = currentStyle.italic();
            visitChildren(emphasis);
            currentStyle = prev;
        }

        @Override
        public void visit(Code code) {
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.YELLOW).bg(Color.DARK_GRAY);
            printText(code.getLiteral());
            currentStyle = prev;
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            printText(" ");
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            newLine();
        }

        @Override
        public void visit(BulletList bulletList) {
            visitChildren(bulletList);
            newLine();
        }

        private int currentListNumber = 1;

        @Override
        public void visit(ListItem listItem) {
            if (currentX > area.x()) {
                newLine();
            }

            boolean isTaskListItem = false;
            Node child = listItem.getFirstChild();
            if (child instanceof Paragraph && child.getFirstChild() instanceof TaskListItemMarker) {
                isTaskListItem = true;
            }

            if (!isTaskListItem) {
                if (listItem.getParent() instanceof OrderedList) {
                    printText(currentListNumber + ". ");
                    currentListNumber++;
                } else {
                    printText("• ");
                }
            }
            visitChildren(listItem);
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            Style prev = currentStyle;
            currentStyle = currentStyle.italic().fg(Color.GRAY);
            if (currentX > area.x()) {
                newLine();
            }
            printText("> ");
            visitChildren(blockQuote);
            currentStyle = prev;
        }

        private String extractText(Node node) {
            StringBuilder sb = new StringBuilder();
            node.accept(new AbstractVisitor() {
                @Override public void visit(Text text) { sb.append(text.getLiteral()); }
                @Override public void visit(Code code) { sb.append(code.getLiteral()); }
                @Override public void visit(SoftLineBreak softLineBreak) { sb.append(" "); }
                @Override public void visit(HtmlInline htmlInline) { sb.append(htmlInline.getLiteral()); }
                @Override public void visit(Image image) {
                    String altText = image.getTitle() != null ? image.getTitle() : "Image";
                    sb.append("![").append(altText).append("](").append(image.getDestination()).append(")");
                }
            });
            return sb.toString();
        }

        private int[] calculateColumnWidths(TableBlock table, int availableWidth) {
            int numCols = countColumns(table);
            if (numCols == 0) return new int[0];

            int[] maxContentWidths = new int[numCols];
            table.accept(new AbstractVisitor() {
                int colIdx = 0;
                @Override
                public void visit(CustomNode customNode) {
                    if (customNode instanceof TableRow) {
                        colIdx = 0;
                        visitChildren(customNode);
                    } else if (customNode instanceof TableCell) {
                        if (colIdx < numCols) {
                            String text = extractText(customNode);
                            int w = CharWidth.of(text);
                            if (w > maxContentWidths[colIdx]) {
                                maxContentWidths[colIdx] = w;
                            }
                        }
                        colIdx++;
                    } else {
                        visitChildren(customNode);
                    }
                }
                @Override
                public void visit(CustomBlock customBlock) {
                    visitChildren(customBlock); // Process children like TableHead/TableBody
                }
            });

            for (int i=0; i<numCols; i++) {
                if (maxContentWidths[i] == 0) maxContentWidths[i] = 1;
            }

            int totalBorders = 3 * numCols + 1;
            int maxAvailableForContents = Math.max(numCols, availableWidth - totalBorders);
            int[] actualWidths = new int[numCols];
            int totalContents = 0;
            for (int w : maxContentWidths) totalContents += w;

            if (totalContents <= maxAvailableForContents) {
                System.arraycopy(maxContentWidths, 0, actualWidths, 0, numCols);
            } else {
                // Fair share allocation
                int[] sortedIndices = new int[numCols];
                for (int i = 0; i < numCols; i++) sortedIndices[i] = i;
                // Sort by maxContentWidths ascending
                for (int i = 0; i < numCols - 1; i++) {
                    for (int j = i + 1; j < numCols; j++) {
                        if (maxContentWidths[sortedIndices[i]] > maxContentWidths[sortedIndices[j]]) {
                            int temp = sortedIndices[i];
                            sortedIndices[i] = sortedIndices[j];
                            sortedIndices[j] = temp;
                        }
                    }
                }

                int remainingToDistribute = maxAvailableForContents;
                int remainingCols = numCols;

                for (int i = 0; i < numCols; i++) {
                    int originalIdx = sortedIndices[i];
                    int fairShare = remainingToDistribute / remainingCols;
                    if (maxContentWidths[originalIdx] <= fairShare) {
                        actualWidths[originalIdx] = maxContentWidths[originalIdx];
                    } else {
                        actualWidths[originalIdx] = fairShare;
                    }
                    remainingToDistribute -= actualWidths[originalIdx];
                    remainingCols--;
                }
            }
            return actualWidths;
        }

        @Override
        public void visit(CustomBlock customBlock) {
            if (customBlock instanceof TableBlock) {
                TableBlock table = (TableBlock) customBlock;
                int numCols = countColumns(table);
                if (numCols > 0) {
                    int[] colWidths = calculateColumnWidths(table, area.width());
                    TableContext prevTableContext = currentTableContext;
                    currentTableContext = new TableContext(colWidths, area.x());

                    if (currentX > area.x()) {
                        newLine();
                        newLine(); // extra space before table
                    }
                    visitChildren(table);

                    currentTableContext = prevTableContext;
                    newLine();
                } else {
                    visitChildren(customBlock);
                }
            } else {
                visitChildren(customBlock);
            }
        }

        @Override
        public void visit(CustomNode customNode) {
            if (customNode instanceof TableHead) {
                visitChildren(customNode);
                if (currentTableContext != null && currentY < area.bottom()) {
                    int x = area.x();
                    for (int col = 0; col < currentTableContext.colWidths.length; col++) {
                        buffer.set(x++, currentY, new Cell("|", currentStyle));
                        buffer.set(x++, currentY, new Cell("-", currentStyle));
                        for (int w = 0; w < currentTableContext.colWidths[col]; w++) {
                            buffer.set(x++, currentY, new Cell("-", currentStyle));
                        }
                        buffer.set(x++, currentY, new Cell("-", currentStyle));
                    }
                    buffer.set(x, currentY, new Cell("|", currentStyle));
                    currentY++;
                    currentX = area.x();
                }
            } else if (customNode instanceof TableBody) {
                visitChildren(customNode);
            } else if (customNode instanceof TableRow) {
                if (currentTableContext != null) {
                    currentTableContext.startRow(currentY);

                    visitChildren(customNode);

                    int maxH = currentTableContext.maxRowHeight;
                    for (int i = 0; i < maxH; i++) {
                        int y = currentTableContext.rowStartY + i;
                        if (y >= area.bottom()) break;
                        int x = area.x();
                        for (int col = 0; col < currentTableContext.colWidths.length; col++) {
                            buffer.set(x, y, new Cell("|", currentStyle));
                            x += currentTableContext.colWidths[col] + 3;
                        }
                        buffer.set(x, y, new Cell("|", currentStyle));
                    }
                    currentY = currentTableContext.rowStartY + maxH;
                    currentX = area.x();
                } else {
                    visitChildren(customNode);
                }
            } else if (customNode instanceof TableCell) {
                if (currentTableContext != null) {
                    TableCell cell = (TableCell) customNode;
                    int colIdx = currentTableContext.currentCol++;
                    if (colIdx >= currentTableContext.colWidths.length) return;

                    int cellX = currentTableContext.colXs[colIdx];
                    int cellWidth = currentTableContext.colWidths[colIdx];

                    Rect prevArea = this.area;
                    this.area = new Rect(cellX, currentTableContext.rowStartY, cellWidth, prevArea.bottom() - currentTableContext.rowStartY);
                    this.currentX = cellX;
                    this.currentY = currentTableContext.rowStartY;

                    Style prev = currentStyle;
                    if (cell.isHeader()) {
                        currentStyle = currentStyle.bold();
                    }

                    visitChildren(cell);

                    currentStyle = prev;

                    int h = this.currentY - currentTableContext.rowStartY + (this.currentX > this.area.x() ? 1 : 0);
                    currentTableContext.updateMaxHeight(h);

                    this.area = prevArea;
                } else {
                    visitChildren(customNode);
                }
            } else if (customNode instanceof Strikethrough) {
                Style prev = currentStyle;
                // Dim the text to represent strikethrough if no native style modifier exists
                currentStyle = currentStyle.fg(Color.DARK_GRAY).italic();
                visitChildren(customNode);
                currentStyle = prev;
            } else if (customNode instanceof TaskListItemMarker) {
                TaskListItemMarker marker = (TaskListItemMarker) customNode;
                if (marker.isChecked()) {
                    printText("[x] ");
                } else {
                    printText("[ ] ");
                }
            } else {
                visitChildren(customNode);
            }
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            if (currentX > area.x()) {
                newLine();
            }
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.YELLOW).bg(Color.DARK_GRAY);
            printText(fencedCodeBlock.getLiteral());
            currentStyle = prev;
            newLine();
            newLine();
        }

        @Override
        public void visit(HtmlBlock htmlBlock) {
            if (currentX > area.x()) {
                newLine();
            }
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.GRAY);
            printText(htmlBlock.getLiteral());
            currentStyle = prev;
            newLine();
            newLine();
        }

        @Override
        public void visit(HtmlInline htmlInline) {
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.GRAY);
            printText(htmlInline.getLiteral());
            currentStyle = prev;
        }

        @Override
        public void visit(Image image) {
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.BLUE).underlined();
            String altText = image.getTitle() != null ? image.getTitle() : "Image";
            printText("![" + altText + "](" + image.getDestination() + ")");
            currentStyle = prev;
        }

        @Override
        public void visit(IndentedCodeBlock indentedCodeBlock) {
            if (currentX > area.x()) {
                newLine();
            }
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.YELLOW).bg(Color.DARK_GRAY);
            printText(indentedCodeBlock.getLiteral());
            currentStyle = prev;
            newLine();
            newLine();
        }

        @Override
        public void visit(Link link) {
            Style prev = currentStyle;
            currentStyle = currentStyle.fg(Color.BLUE).underlined();
            visitChildren(link);
            currentStyle = prev;
        }

        @Override
        public void visit(LinkReferenceDefinition linkReferenceDefinition) {
            // usually not rendered directly inline
        }

        @Override
        public void visit(OrderedList orderedList) {
            int previousListNumber = currentListNumber;
            Integer startNumber = orderedList.getMarkerStartNumber();
            currentListNumber = startNumber != null ? startNumber : 1;
            visitChildren(orderedList);
            currentListNumber = previousListNumber;
            newLine();
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {
            if (currentX > area.x()) {
                newLine();
            }
            printText("---");
            newLine();
            newLine();
        }

        private void printText(String text) {
            if (currentY >= area.bottom()) {
                return; // OOB
            }

            // Split by whitespace and common punctuation, keeping the delimiters intact
            String[] words = text.split("(?<=[ \\t\\n\\r,.;:/?!-])|(?=[ \\t\\n\\r,.;:/?!-])");
            for (String word : words) {
                if (word.isEmpty()) continue;

                int width = CharWidth.of(word);

                // If it doesn't fit on the current line
                if (currentX + width > area.right()) {
                    // Try to wrap. If the word is huge, at least start it on a fresh line
                    // *unless* we are already at the beginning of a line.
                    if (currentX > area.x()) {
                        newLine();
                    }
                }

                if (currentY >= area.bottom()) {
                    return; // OOB
                }

                int col = currentX;
                for (char c : word.toCharArray()) {
                    String s = String.valueOf(c);
                    int charW = CharWidth.of(s);

                    if (currentX + charW > area.right()) {
                        newLine();
                        col = currentX;
                        if (currentY >= area.bottom()) return;
                    }

                    buffer.set(col, currentY, new Cell(s, currentStyle));
                    col += charW;
                    currentX += charW;
                }
            }
        }

        private void newLine() {
            currentX = area.x();
            currentY++;
        }
    }
}
