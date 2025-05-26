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
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        graph.initfromfile("1.txt");
    }

    @Before
    public void setUp() {
        System.setOut(new TeePrintStream(outContent, originalOut));
        outContent.reset();
    }

    private void executeTest(String word1, String word2) {
        // 构造输入流，模拟用户输入：2、word1、word2、0（退出）
        String input = String.format("2\n%s\n%s\n0\n", word1, word2);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);

        // 只运行主循环，不重新建图
        try {
            graph.mainloop(new Scanner(System.in));
        } catch (NoSuchElementException e) {
            // 输入流用尽时的预期异常
        }
    }
    private void whiteTest(String string) {

        String input = String.format("3\n%s\n0\n", string);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        System.setIn(inputStream);

        // 只运行主循环，不重新建图
        try {
            graph.mainloop(new Scanner(System.in));
        } catch (NoSuchElementException e) {
            // 输入流用尽时的预期异常
        }
    }
//黑盒测试
    @Test
    public void testNormalBridgeWords() {
        executeTest("more", "so");
        String output = outContent.toString();

        assertTrue(output.contains("more") && output.contains("so"));
    }

    @Test
    public void testFirstWordNotExist() {
        executeTest("xyz", "so");
        String output = outContent.toString();
        assertTrue(output.contains("No xyz in the graph!"));
    }

    @Test
    public void testSecondWordNotExist() {
        executeTest("more", "xyz");
        String output = outContent.toString();
        assertTrue(output.contains("No xyz in the graph!"));
    }

    @Test
    public void testNoBridgeWords() {
        executeTest("with", "a");
        String output = outContent.toString();
        assertTrue(output.contains("No bridge words from with to a!"));

    }
//白盒测试
    @Test
    public void testEmptyInput() {
        String result = graph.generateNewText("");
        System.out.println("实际输出: " + result);
        assertEquals("", result);
    }

    @Test
    public void testSingleWord() {
        String result = graph.generateNewText("Hello");
        System.out.println("实际输出: " + result);
        assertEquals("Hello", result);
    }
    @Test
    public void testNobridgeWords() {
        String result = graph.generateNewText("I like you");
        List<String> bridges = graph.getBridgeWords("I", "like");
        System.out.println(bridges+"\n");
        assertTrue(bridges.isEmpty());
        bridges = graph.getBridgeWords("like", "you");
        System.out.println(bridges);
        assertTrue(bridges.isEmpty());
        System.out.println("实际输出: " + result);
        assertEquals("I like you", result);
    }

    @Test
    public void testbridgeWords() {
        String result = graph.generateNewText("I wrote a report");
        List<String> bridges = graph.getBridgeWords("I", "wrote");
        System.out.println(bridges+"\n");
        assertTrue(bridges.isEmpty());
        bridges = graph.getBridgeWords("wrote", "a");
        System.out.println(bridges+"\n");
        assertTrue(bridges.isEmpty());
        bridges = graph.getBridgeWords("a", "report");
        System.out.println(bridges);
        assertEquals("detailed", bridges.get(0)); 
        System.out.println("实际输出: " + result);
        assertEquals("I wrote a detailed report", result);
    }

    @After
    public void restoreStreams() {
        System.out.println("=== 测试结束 ===\n");
        System.setOut(originalOut);
        System.setIn(originalIn);
    }
}