# TamboUI Markdown Widget: Implementation Walkthrough

This document outlines the journey of building a fully-featured, dynamically wrapping Markdown widget natively rendered for the terminal using TamboUI and CommonMark.

## The Objective
Our goal was to create `Markdown.java`, a custom `StatefulWidget<Markdown.State>` in the TamboUI framework. It needed to take raw Markdown text, parse it into an Abstract Syntax Tree (AST), and then systematically apply TamboUI styling formats (`Style.bold()`, `.italic()`, `.fg()`, `.bg()`, etc.) directly to the `Buffer` coordinates.

## Core Architecture
We utilized `org.commonmark:commonmark:0.27.1` as the backbone parser.
1. **The Widget (`Markdown.java`)**: We designed a stateful component that receives `Markdown.State` (holding the raw string alongside advanced viewport scroll `scrollY` state tracking) and the parent terminal `Rect` constraints.
2. **The AST Visitor (`RenderVisitor`)**: We extended CommonMark's `AbstractVisitor` to traverse the resulting Node tree element by element. At each node (e.g. `Heading`, `List`, `Text`), it delegates TamboUI `Cell` printing while persisting and modifying the current active `Style`. We compute a global `getTotalHeight()` out-of-bounds to calibrate accurate document scroll limits `state.setMaxScrollY()`.

### Standard CommonMark Support
We started off by satisfying the core `visit()` definitions:
- Text styling (`StrongEmphasis`, `Emphasis`, `Code`) cleanly updates the `currentStyle` in memory, visits text leaf nodes, and reverts the style when exiting the parent node boundaries.
- Block elements (`Paragraph`, `BlockQuote`) intelligently handle vertical progression (modifying `currentY`) while appending stylistic prefixes like `> ` for blockquotes.
- Lists track their depth and type. `BulletList` correctly appends inline `• ` representations. Crucially, we fixed `OrderedList` nodes by injecting a memory counter so `ListItem` elements iterate correctly (e.g., `1. `, `2. `) using CommonMark's `.getMarkerStartNumber()` context.

## Solving The Layout Constraints
Text UI interfaces only have fixed coordinate rectangles. Simply printing strings sequentially would crash when hitting terminal margins.

### Smart Text Wrapping
We engineered a context-aware algorithm inside `printText(String)`:
1. Strings are intelligently tokenized by both whitespace and natural linguistic punctuation marks (`(?<=[ \t\n\r,.;:/?!-])|(?=[ \t\n\r,.;:/?!-])`).
2. We continuously measure the graphical size of upcoming tokens utilizing TamboUI's native `CharWidth.of()`.
3. If a word threatens to exceed the right bounds of the parent `Rect` area `area.right()`, we seamlessly execute a `newLine()`, placing the whole punctuation-bracketed token cleanly onto the row below.
4. **Failsafe character-wrapping**: In the anomalous event where an unbroken, unpunctuated block is wider than the *entire terminal size*, the system individually evaluates character widths, forcing a hard carriage return at the exact pixel constraints before continuing.
5. **Scroll & Height Context**: We intentionally allow coordinates to build iteratively off the screen's bottom bounding box limit while truncating actual character drawing `buffer.set()`. This permits CommonMark to correctly calculate full conceptual document height for UI scrolling via `visit(Document)` callbacks without crashing the application memory frame.

## Mathematical Notations
A recent addition elevates basic monospace `<Code>` visualization into parsed mathematical domains.

### Inline and Fenced Logic
We introduced `printMath()` and pattern marching `\\\\(?:begin|end)\\{[^}]*\\}|\\\\\\\\|\\\\[a-zA-Z]+|\\\\.|&` to extract rich LaTeX commands and distinguish code structures logically:
- **Math Environments**: By detecting fenced backticks parameterized with ```math , we strip raw CommonMark layout artifacts and structurally format equations, suppressing functional boilerplate tags (`\begin`, `\end`, `&`) logically from view while highlighting active variables and commands.
- **Inline Variables**: `$math$`: For basic equations wrapped in individual dollar-signs within raw running text, the abstract inline `<Code>` logic checks for prefix/postfix conditions and forwards rendering directly to `printMath()`, retaining paragraph flow natively rather than splitting lines.

## Advanced Features & Extensions
To make the widget exceptionally robust, we embraced GitHub Flavored Markdown (GFM) extensions alongside the `RenderVisitor`.

### Responsive Tables
We imported `org.commonmark:commonmark-ext-gfm-tables`. Tabular matrices inside absolute terminal matrices required an entirely separate contextual algorithm:
- When a `TableBlock` is reached, `calculateColumnWidths` analyzes the optimal "intrinsic width" of all text contents across every `TableCell` descending from the table.
- **Fair-Share Priority Expansion**: Based on the parent `area` width, it perfectly matches the intrinsic dimensions for small columns (like headers or status codes), and then dynamically distributes the remainder to data-dense columns.
- The RenderVisitor isolates the active global `area` constraints into fractional bounding boxes representing individual table cells. This means that if you inject a paragraph into a tiny column, it leverages the native *Smart Text Wrapping* (described above) identically but wraps densely within that restricted horizontal coordinate zone, rather than breaking the UI. Table rows measure the max required height recursively.

### Final GFM Add-ons
We added three final modular extensions to complete the markdown experience:
- **Autolinks (`commonmark-ext-autolink`)**: Native recognition of loose URLs and emails directly into our existing `Link` visitor logic.
- **Strikethrough (`commonmark-ext-gfm-strikethrough`)**: Trapping `Strikethrough` nodes (`~~text~~`) dynamically modifies the `Style` pointer to dim the color space `fg(Color.DARK_GRAY)` while activating italics to approximate markdown deletion.
- **Task Lists (`commonmark-ext-task-list-items`)**: Interacting with `TaskListItemMarker` contexts, we effectively skip rendering normative default list bullets (`• `) and instead conditionally draw interactive terminal brackets `[ ]` or `[x]` correlating to boolean attributes.

## Conclusion
The outcome is a highly responsive `MarkdownDemo.java` executable JBang sandbox that elegantly translates complex documents natively onto JLine-supported TamboUI terminal applications, gracefully expanding and reflowing content universally.
