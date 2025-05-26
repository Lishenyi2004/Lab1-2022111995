import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class TextGraphTest {
    private static TextGraph graph;
    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static final PrintStream originalOut = System.out;
    private static final InputStream originalIn = System.in;

    // 创建一个同时输出到控制台和ByteArrayOutputStream的PrintStream
    private static class TeePrintStream extends PrintStream {
        private final PrintStream second;

        public TeePrintStream(OutputStream main, PrintStream second) {
            super(main);
            this.second = second;
        }

        @Override
        public void write(int b) {
            super.write(b);
            second.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            second.write(buf, off, len);
        }
    }

    @BeforeClass
    public static void globalSetUp() {
        // 只初始化一次图
        graph = new TextGraph();
        graph.initfromfile("1.txt", graph);
    }

    @Before
    public void setUp() {
        // 每次测试前重定向输出流并清空内容
        System.setOut(new TeePrintStream(outContent, originalOut));
        outContent.reset();
    }

    private void executeTest(String word1, String word2) {
        // 构造输入流，模拟用户输入：2、word1、word2、0（退出）
        String input = String.format("2\n%s\n%s\n", word1, word2);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);

        // 只运行主循环，不重新建图
        try {
            graph.mainloop(graph, new Scanner(System.in));
        } catch (NoSuchElementException e) {
            // 输入流用尽时的预期异常
        }
    }

    @Test
    public void testNormalBridgeWords() {
        executeTest("more", "so");
        String output = outContent.toString();
        System.out.println("实际输出: " + output);
        assertTrue(output.contains("more") && output.contains("so"));
    }

    @Test
    public void testFirstWordNotExist() {
        executeTest("xyz", "so");
        String output = outContent.toString();
        System.out.println("实际输出: " + output);
        assertTrue(output.contains("No xyz in the graph!"));
    }

    @Test
    public void testSecondWordNotExist() {
        executeTest("more", "xyz");
        String output = outContent.toString();
        System.out.println("实际输出: " + output);
        assertTrue(output.contains("No xyz in the graph!"));
    }

    @Test
    public void testNoBridgeWords() {
        executeTest("with", "a");
        String output = outContent.toString();
        System.out.println("实际输出: " + output);
        assertTrue(output.contains("No bridge words from with to a!"));

    }

    @After
    public void restoreStreams() {
        System.out.println("=== 测试结束 ===\n");
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
}