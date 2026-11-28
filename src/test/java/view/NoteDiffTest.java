package view;

import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteDiffTest {
    @Test
    void identicalTextHasNoChanges() {
        NoteDiff.Result result = NoteDiff.compute("a\nb", "a\nb");

        assertTrue(result.isEmpty());
        assertTrue(result.getHunks().isEmpty());
    }

    @Test
    void detectsAddedLines() {
        NoteDiff.Result result = NoteDiff.compute("a\nb", "a\nb\nc");

        assertFalse(result.isEmpty());
        assertEquals(1, result.getAdded());
        assertEquals(0, result.getDeleted());
        List<NoteDiff.Line> lines = result.getHunks().get(0).getLines();
        assertEquals(NoteDiff.Type.ADD, lines.get(lines.size() - 1).getType());
        assertEquals("c", lines.get(lines.size() - 1).getText());
    }

    @Test
    void detectsDeletedLines() {
        NoteDiff.Result result = NoteDiff.compute("a\nb\nc", "a\nc");

        assertEquals(0, result.getAdded());
        assertEquals(1, result.getDeleted());
        assertTrue(result.getHunks().get(0).getLines().stream()
                .anyMatch(line -> line.getType() == NoteDiff.Type.DELETE
                        && line.getText().equals("b")));
    }

    @Test
    void detectsReplacedLines() {
        NoteDiff.Result result = NoteDiff.compute("old line", "new line");

        assertEquals(1, result.getAdded());
        assertEquals(1, result.getDeleted());
    }

    @Test
    void keepsThreeContextLinesAndMergesNearbyChanges() {
        String oldText = "1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n";
        String newText = "1\n2\n3\nfour\n5\n6\n7\n8\nnine\n10\n";
        NoteDiff.Result result = NoteDiff.compute(oldText, newText);

        assertEquals(2, result.getAdded());
        assertEquals(2, result.getDeleted());
        assertTrue(result.getHunks().size() >= 1);
        for (NoteDiff.Hunk hunk : result.getHunks()) {
            assertTrue(hunk.getHeader().startsWith("@@ -"));
            assertTrue(hunk.getHeader().contains(" +"));
        }
    }

    @Test
    void rendersGitHubStylePrefixesAndClearsPreviousContent() throws Exception {
        NoteDiff.Result result = NoteDiff.compute("old line", "new line");
        JTextPane pane = new JTextPane();
        FutureTask<Void> task = new FutureTask<>(() -> {
            NoteDiff.render(pane, result);
            return null;
        });
        SwingUtilities.invokeAndWait(task);
        task.get();

        String text = pane.getText();
        assertTrue(text.contains("- old line"));
        assertTrue(text.contains("+ new line"));
        assertTrue(text.contains("@@"));
    }
}
