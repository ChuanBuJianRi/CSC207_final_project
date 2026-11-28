package view;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * A small line-based unified diff used to show what the agent changed.
 */
public final class NoteDiff {
    public enum Type {
        CONTEXT,
        ADD,
        DELETE
    }

    public static final class Line {
        private final Type type;
        private final String text;

        public Line(Type type, String text) {
            this.type = type;
            this.text = text;
        }

        public Type getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }

    public static final class Hunk {
        private final String header;
        private final List<Line> lines;

        public Hunk(String header, List<Line> lines) {
            this.header = header;
            this.lines = List.copyOf(lines);
        }

        public String getHeader() {
            return header;
        }

        public List<Line> getLines() {
            return lines;
        }
    }

    public static final class Result {
        private final List<Hunk> hunks;
        private final int added;
        private final int deleted;

        public Result(List<Hunk> hunks, int added, int deleted) {
            this.hunks = List.copyOf(hunks);
            this.added = added;
            this.deleted = deleted;
        }

        public List<Hunk> getHunks() {
            return hunks;
        }

        public int getAdded() {
            return added;
        }

        public int getDeleted() {
            return deleted;
        }

        public boolean isEmpty() {
            return added == 0 && deleted == 0;
        }
    }

    private static final int CONTEXT_LINES = 3;
    private static final long MAX_LCS_CELLS = 1_000_000L;

    private NoteDiff() {
    }

    public static Result compute(String oldText, String newText) {
        String[] oldLines = splitLines(oldText);
        String[] newLines = splitLines(newText);
        int prefix = 0;
        while (prefix < oldLines.length && prefix < newLines.length
                && oldLines[prefix].equals(newLines[prefix])) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < oldLines.length - prefix && suffix < newLines.length - prefix
                && oldLines[oldLines.length - 1 - suffix]
                .equals(newLines[newLines.length - 1 - suffix])) {
            suffix++;
        }

        List<Line> lines = new ArrayList<>();
        for (int index = 0; index < prefix; index++) {
            lines.add(new Line(Type.CONTEXT, oldLines[index]));
        }
        int oldMiddleEnd = oldLines.length - suffix;
        int newMiddleEnd = newLines.length - suffix;
        diffMiddle(oldLines, prefix, oldMiddleEnd, newLines, prefix, newMiddleEnd, lines);
        for (int index = suffix; index > 0; index--) {
            lines.add(new Line(Type.CONTEXT, oldLines[oldLines.length - index]));
        }

        int added = 0;
        int deleted = 0;
        for (Line line : lines) {
            if (line.getType() == Type.ADD) {
                added++;
            } else if (line.getType() == Type.DELETE) {
                deleted++;
            }
        }
        return new Result(buildHunks(lines), added, deleted);
    }

    private static void diffMiddle(String[] oldLines, int oldStart, int oldEnd,
                                   String[] newLines, int newStart, int newEnd,
                                   List<Line> result) {
        int oldCount = oldEnd - oldStart;
        int newCount = newEnd - newStart;
        if (oldCount == 0) {
            for (int index = newStart; index < newEnd; index++) {
                result.add(new Line(Type.ADD, newLines[index]));
            }
            return;
        }
        if (newCount == 0) {
            for (int index = oldStart; index < oldEnd; index++) {
                result.add(new Line(Type.DELETE, oldLines[index]));
            }
            return;
        }
        if ((long) oldCount * newCount > MAX_LCS_CELLS) {
            for (int index = oldStart; index < oldEnd; index++) {
                result.add(new Line(Type.DELETE, oldLines[index]));
            }
            for (int index = newStart; index < newEnd; index++) {
                result.add(new Line(Type.ADD, newLines[index]));
            }
            return;
        }

        int[][] lcs = new int[oldCount + 1][newCount + 1];
        for (int i = oldCount - 1; i >= 0; i--) {
            for (int j = newCount - 1; j >= 0; j--) {
                if (oldLines[oldStart + i].equals(newLines[newStart + j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        int i = 0;
        int j = 0;
        while (i < oldCount && j < newCount) {
            if (oldLines[oldStart + i].equals(newLines[newStart + j])) {
                result.add(new Line(Type.CONTEXT, oldLines[oldStart + i]));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                result.add(new Line(Type.DELETE, oldLines[oldStart + i]));
                i++;
            } else {
                result.add(new Line(Type.ADD, newLines[newStart + j]));
                j++;
            }
        }
        while (i < oldCount) {
            result.add(new Line(Type.DELETE, oldLines[oldStart + i]));
            i++;
        }
        while (j < newCount) {
            result.add(new Line(Type.ADD, newLines[newStart + j]));
            j++;
        }
    }

    private static List<Hunk> buildHunks(List<Line> lines) {
        List<Integer> changes = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).getType() != Type.CONTEXT) {
                changes.add(index);
            }
        }
        if (changes.isEmpty()) {
            return List.of();
        }
        int[] oldBefore = new int[lines.size() + 1];
        int[] newBefore = new int[lines.size() + 1];
        for (int index = 0; index < lines.size(); index++) {
            oldBefore[index + 1] = oldBefore[index]
                    + (lines.get(index).getType() == Type.ADD ? 0 : 1);
            newBefore[index + 1] = newBefore[index]
                    + (lines.get(index).getType() == Type.DELETE ? 0 : 1);
        }

        List<Hunk> hunks = new ArrayList<>();
        int start = -1;
        int end = -1;
        for (int change : changes) {
            int rangeStart = Math.max(0, change - CONTEXT_LINES);
            int rangeEnd = Math.min(lines.size(), change + CONTEXT_LINES + 1);
            if (start < 0) {
                start = rangeStart;
                end = rangeEnd;
            } else if (rangeStart <= end) {
                end = Math.max(end, rangeEnd);
            } else {
                hunks.add(hunk(lines, oldBefore, newBefore, start, end));
                start = rangeStart;
                end = rangeEnd;
            }
        }
        hunks.add(hunk(lines, oldBefore, newBefore, start, end));
        return hunks;
    }

    private static Hunk hunk(List<Line> lines, int[] oldBefore, int[] newBefore,
                             int start, int end) {
        int oldCount = oldBefore[end] - oldBefore[start];
        int newCount = newBefore[end] - newBefore[start];
        int oldStart = oldCount == 0 ? oldBefore[start] : oldBefore[start] + 1;
        int newStart = newCount == 0 ? newBefore[start] : newBefore[start] + 1;
        String header = "@@ -" + oldStart + "," + oldCount
                + " +" + newStart + "," + newCount + " @@";
        return new Hunk(header, lines.subList(start, end));
    }

    public static void render(JTextPane pane, Result result) {
        StyledDocument document = pane.getStyledDocument();
        try {
            document.remove(0, document.getLength());
            Style base = pane.getStyle("base");
            if (base == null) {
                base = pane.addStyle("base", null);
                StyleConstants.setFontFamily(base, Font.MONOSPACED);
                StyleConstants.setFontSize(base, 12);
            }
            Style context = style(pane, "context", base, null, new Color(36, 41, 47), false);
            Style add = style(pane, "add", base, new Color(230, 255, 236),
                    new Color(17, 99, 41), false);
            Style delete = style(pane, "delete", base, new Color(255, 235, 233),
                    new Color(130, 7, 30), false);
            Style hunk = style(pane, "hunk", base, new Color(246, 248, 250),
                    new Color(87, 96, 106), true);

            for (Hunk hunkLine : result.getHunks()) {
                document.insertString(document.getLength(), hunkLine.getHeader() + "\n", hunk);
                for (Line line : hunkLine.getLines()) {
                    String prefix;
                    Style style;
                    switch (line.getType()) {
                        case ADD:
                            prefix = "+ ";
                            style = add;
                            break;
                        case DELETE:
                            prefix = "- ";
                            style = delete;
                            break;
                        default:
                            prefix = "  ";
                            style = context;
                            break;
                    }
                    document.insertString(document.getLength(), prefix + line.getText() + "\n",
                            style);
                }
            }
        } catch (BadLocationException exception) {
            throw new IllegalStateException("Could not render the note diff.", exception);
        }
        pane.setCaretPosition(0);
    }

    private static Style style(JTextPane pane, String name, Style parent, Color background,
                               Color foreground, boolean bold) {
        Style style = pane.getStyle(name);
        if (style == null) {
            style = pane.addStyle(name, parent);
            if (background != null) {
                StyleConstants.setBackground(style, background);
            }
            StyleConstants.setForeground(style, foreground);
            StyleConstants.setBold(style, bold);
        }
        return style;
    }

    private static String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split("\n", -1);
    }
}
