import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

public class Main {
    private Map<String, Map<String, Integer>> graph = new HashMap<>(); //主图结构，存储节点及其相邻节点（带权重）。
    private List<Edge> shortestPathEdges = null; //存储最短路径的边（用于可视化）。
    private Map<String, Double> pageRankMap = new HashMap<>(); //存储每个节点的PageRank值。

    private Map<String, Integer> outDegreeMap = new HashMap<>();//记录每个节点的出度。
    private Map<String, List<String>> inEdgesMap = new HashMap<>();//记录每个节点的入边来源节点。
    private static final double DAMPING_FACTOR = 0.85;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.0001;

    private static class NodeDistance implements Comparable<NodeDistance> {
        String node;
        int distance;

        public NodeDistance(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    private static class Edge {
        String from;
        String to;
        int weight;

        Edge(String from, String to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    public static void main(String[] args) {
        Main Main = new Main();
        Scanner scanner = new Scanner(System.in);

        // 获取文件路径
        String filePath;
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.print("请输入文本文件路径: ");
            filePath = scanner.nextLine();
        }

        // 读取文件并构建图
        try {
            String content = readFile(filePath);
            Main.buildGraph(content);
            System.out.println("图构建完成！");
        } catch (IOException e) {
            System.out.println("文件读取错误: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.println("\n=== 文本图分析系统 ===");
            System.out.println("1. 显示有向图");
            System.out.println("2. 查询桥接词");
            System.out.println("3. 生成新文本");
            System.out.println("4. 计算最短路径");
            System.out.println("5. 计算PageRank");
            System.out.println("6. 随机游走");
            System.out.println("0. 退出程序");
            System.out.print("请选择功能 (0-6): ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("无效输入，请输入数字0-6");
                continue;
            }

            switch (choice) {
                case 0:
                    System.out.println("程序已退出");
                    scanner.close();
                    return;

                case 1:
                    try {
                        File image = Main.showDirectedGraph();
                        System.out.println("有向图已生成: " + image.getAbsolutePath());
                    } catch (Exception e) {
                        System.out.println("图形生成失败: " + e.getMessage());
                    }
                    break;

                case 2:
                    System.out.print("输入第一个单词: ");
                    String word1 = scanner.nextLine();
                    System.out.print("输入第二个单词: ");
                    String word2 = scanner.nextLine();
                    System.out.println(Main.queryBridgeWords(word1, word2));
                    break;

                case 3:
                    System.out.print("输入新文本: ");
                    String inputText = scanner.nextLine();
                    String newText = Main.generateNewText(inputText);
                    System.out.println("生成的新文本: " + newText);
                    break;

                case 4:
                    System.out.print("输入起始单词: ");
                    String startWord = scanner.nextLine().trim();
                    if (!startWord.isEmpty()) {
                        System.out.print("输入结束单词（留空则显示所有路径）: ");
                        String endWord = scanner.nextLine().trim();
                        
                        String pathResult;
                        if (!endWord.isEmpty()) {
                            pathResult = Main.calcShortestPath(startWord, endWord);
                        } else {
                            pathResult = Main.calcAllShortestPaths(startWord);
                        }
                        System.out.println(pathResult);

                        try {
                            File image = Main.showDirectedGraph();
                            System.out.println("带有最短路径标注的图已生成: " + image.getAbsolutePath());
                        } catch (Exception e) {
                            System.out.println("图形生成失败: " + e.getMessage());
                        }
                    }
                    break;

                case 5:
                    System.out.print("输入要查询的单词: ");
                    String prWord = scanner.nextLine().trim();
                    if (!prWord.isEmpty()) {
                        Double prValue = Main.calPageRank(prWord);
                        if (prValue == 0.0) {
                            System.out.println("单词 '" + prWord + "' 不在图中！");
                        } else {
                            System.out.printf("单词 '%s' 的PageRank值: %.4f\n", prWord, prValue);
                        }
                    }

                    // 显示前5重要节点
                    System.out.println("\n--- 重要节点排名（前5）---");
                    List<Map.Entry<String, Double>> ranked = new ArrayList<>(Main.pageRankMap.entrySet());
                    ranked.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
                    for (int i = 0; i < Math.min(5, ranked.size()); i++) {
                        Map.Entry<String, Double> entry = ranked.get(i);
                        System.out.printf("%d. %s (PR=%.4f)\n", i+1, entry.getKey(), entry.getValue());
                    }
                    break;

                case 6:
                    System.out.print("输入要生成的游走记录文件名（留空使用默认文件名）: ");
                    String fileName = scanner.nextLine().trim();
                    if (fileName.isEmpty()) {
                        fileName = "random_walk_result.txt";
                    }
                    System.out.println(Main.randomWalk(fileName));
                    System.out.printf("游走记录已保存到文件: %s\n", fileName);
                    break;

                default:
                    System.out.println("无效选择，请重新输入");
                    break;
            }
        }
    }

    // [其余方法保持不变...]
public void buildGraph(String text) {
        // 清空旧数据
        graph.clear();
        outDegreeMap.clear();
        inEdgesMap.clear();

        String cleaned = text.replaceAll("[^a-zA-Z]", " ").toLowerCase();
        String[] words = cleaned.split("\\s+");

        // 收集所有节点并初始化数据结构
        Set<String> nodes = new HashSet<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                nodes.add(word);
                outDegreeMap.put(word, 0);
                inEdgesMap.putIfAbsent(word, new ArrayList<>());
            }
        }

        // 初始化图结构
        for (String node : nodes) {
            graph.put(node, new HashMap<>());
        }

        // 构建边关系并统计出度
        for (int i = 0; i < words.length - 1; i++) {
            String current = words[i];
            String next = words[i + 1];
            if (!current.isEmpty() && !next.isEmpty()) {
                // 更新边权重
                graph.get(current).put(next, 
                    graph.get(current).getOrDefault(next, 0) + 1);
                
                // 更新出度
                outDegreeMap.put(current, outDegreeMap.get(current) + 1);
                
                // 更新入边
                if (!inEdgesMap.get(next).contains(current)) {
                    inEdgesMap.get(next).add(current);
                }
            }
        }
    }

    public String queryBridgeWords(String word1, String word2) {
        String w1 = word1.toLowerCase();
        String w2 = word2.toLowerCase();

        // 检查节点存在性
        boolean hasW1 = graph.containsKey(w1);
        boolean hasW2 = graph.containsKey(w2);
        if (!hasW1 || !hasW2) {
            if (!hasW1 && !hasW2) {
                return String.format("No %s or %s in the graph!", word1, word2);
            }
            return String.format("No %s in the graph!", hasW1 ? word2 : word1);
        }

        List<String> bridges = getBridgeWords(w1, w2);
        return formatBridgeOutput(bridges, word1, word2);
    }

    private List<String> getBridgeWords(String w1, String w2) {
        List<String> bridges = new ArrayList<>();
        if (!graph.containsKey(w1) || !graph.containsKey(w2)) {
            return bridges;
        }
        Map<String, Integer> w1Edges = graph.get(w1);
        for (String candidate : w1Edges.keySet()) {
            if (graph.get(candidate).containsKey(w2)) {
                bridges.add(candidate);
            }
        }
        return bridges;
    }

    private String formatBridgeOutput(List<String> bridges, String w1, String w2) {
        if (bridges.isEmpty()) {
            return String.format("No bridge words from %s to %s!", w1, w2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("The bridge words from ").append(w1).append(" to ")
                .append(w2).append(" are: ");
        if (bridges.size() == 1) {
            sb.append(bridges.get(0));
        } else {
            String last = bridges.remove(bridges.size() - 1);
            sb.append(String.join(", ", bridges))
                    .append(" and ").append(last);
        }
        return sb.append(".").toString();
    }

    public String generateNewText(String inputText) {
        List<String> originalWords = new ArrayList<>();
        List<String> processedWords = new ArrayList<>();

        // 提取原始单词（保留大小写）和处理后的小写单词
        Matcher matcher = Pattern.compile("[a-zA-Z]+").matcher(inputText);
        while (matcher.find()) {
            String originalWord = matcher.group();
            originalWords.add(originalWord);
            processedWords.add(originalWord.toLowerCase());
        }

        if (processedWords.isEmpty()) {
            return "";
        }

        List<String> newWords = new ArrayList<>();
        newWords.add(originalWords.get(0));

        for (int i = 0; i < processedWords.size() - 1; i++) {
            String current = processedWords.get(i);
            String next = processedWords.get(i + 1);

            List<String> bridges = getBridgeWords(current, next);

            if (!bridges.isEmpty()) {
                // 随机选择一个桥接词
                Random random = new Random();
                String bridge = bridges.get(random.nextInt(bridges.size()));
                newWords.add(bridge);
            }

            newWords.add(originalWords.get(i + 1));
        }

        return String.join(" ", newWords);
    }

    public File showDirectedGraph() throws Exception {
        // 生成DOT语言描述
        StringBuilder dot = new StringBuilder();
        dot.append("digraph G {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=circle];\n");

        // 添加所有节点
        Set<String> nodes = new HashSet<>(graph.keySet());
        graph.values().forEach(m -> nodes.addAll(m.keySet()));
        nodes.forEach(n -> dot.append("  \"").append(n).append("\";\n"));

        // 添加边
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                String to = edge.getKey();
                int weight = edge.getValue();
                boolean isHighlighted = false;
                if (shortestPathEdges != null) {
                    for (Edge e : shortestPathEdges) {
                        if (e.from.equals(from) && e.to.equals(to)) {
                            isHighlighted = true;
                            break;
                        }
                    }
                }
                if (isHighlighted) {
                    dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\" color=\"red\" penwidth=2.0];\n", from, to, weight));
                } else {
                    dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\"];\n", from, to, weight));
                }
            }
        }
        dot.append("}");

        // 创建临时文件
        File dotFile = File.createTempFile("graph", ".dot");
        File imageFile = new File("graph.png");

        // 写入DOT文件
        Files.write(dotFile.toPath(), dot.toString().getBytes());

        // 调用Graphviz生成图片
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-o", imageFile.getAbsolutePath(), dotFile.getAbsolutePath());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Graphviz执行失败，请确认已安装并添加到PATH");
        }

        return imageFile;
    }

    public String calcShortestPath(String word1, String word2) {
        return calculatePaths(word1, word2, true);
    }

    public String calcAllShortestPaths(String word) {
        return calculatePaths(word, null, false);
    }

    private String calculatePaths(String word1, String word2, boolean singlePath) {
        shortestPathEdges = null;
        String w1 = word1.toLowerCase();
        String w2 = (word2 != null) ? word2.toLowerCase() : null;

        // 验证输入有效性
        if (!graph.containsKey(w1)) {
            return String.format("图中不存在单词 '%s'!", word1);
        }
        if (singlePath && !graph.containsKey(w2)) {
            return String.format("图中不存在单词 '%s'!", word2);
        }

        // 执行Dijkstra算法
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();

        // 初始化距离
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(w1, 0);
        queue.add(new NodeDistance(w1, 0));

        // 主算法循环
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            String currentNode = current.node;
            int currentDist = current.distance;

            if (currentDist > distances.get(currentNode)) continue;

            Map<String, Integer> neighbors = graph.get(currentNode);
            if (neighbors == null) continue;

            for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();
                int newDist = currentDist + weight;

                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, currentNode);
                    queue.add(new NodeDistance(neighbor, newDist));
                }
            }
        }

        // 处理不同输出模式
        if (singlePath) {
            return handleSinglePath(w1, w2, distances, predecessors);
        } else {
            return handleAllPaths(w1, distances, predecessors);
        }
    }

    private String handleSinglePath(String source, String target,
                                   Map<String, Integer> distances,
                                   Map<String, String> predecessors) {
        if (distances.get(target) == Integer.MAX_VALUE) {
            return String.format("从 %s 到 %s 没有可达路径!", source, target);
        }

        List<String> path = buildPath(target, predecessors);
        storePathEdges(path);

        return formatPathOutput(source, target, path, distances.get(target));
    }

    private String handleAllPaths(String source,
                                 Map<String, Integer> distances,
                                 Map<String, String> predecessors) {
        StringBuilder result = new StringBuilder();
        result.append("从 ").append(source).append(" 出发的最短路径：\n");

        List<String> sortedNodes = new ArrayList<>(graph.keySet());
        Collections.sort(sortedNodes);

        for (String node : sortedNodes) {
            if (node.equals(source)) continue;

            if (distances.get(node) == Integer.MAX_VALUE) {
                result.append(String.format("→ %s: 不可达\n", node));
                continue;
            }

            List<String> path = buildPath(node, predecessors);
            result.append(formatPathOutput(source, node, path, distances.get(node)))
                  .append("\n");
        }

        return result.toString();
    }

    private List<String> buildPath(String target, Map<String, String> predecessors) {
        LinkedList<String> path = new LinkedList<>();
        String current = target;
        while (current != null) {
            path.addFirst(current);
            current = predecessors.get(current);
        }
        return path;
    }

    private void storePathEdges(List<String> path) {
        shortestPathEdges = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Integer weight = graph.get(from).get(to);
            if (weight != null) {
                shortestPathEdges.add(new Edge(from, to, weight));
            }
        }
    }

    private String formatPathOutput(String source, String target,
                                   List<String> path, int distance) {
        StringBuilder sb = new StringBuilder();
        sb.append("从 ").append(source).append(" 到 ").append(target).append(":\n");
        sb.append("路径: ").append(String.join(" → ", path));
        sb.append("\n长度: ").append(distance);
        return sb.toString();
    }

    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(" ");
            }
        }
        return content.toString();
    }

    public Double calPageRank(String word) {
        computePageRanks();
        return pageRankMap.getOrDefault(word.toLowerCase(), 0.0);
    }

    private void computePageRanks() {
        if (!pageRankMap.isEmpty()) return; // 避免重复计算

        int N = graph.size();
        if (N == 0) return;

        // 初始化PR值（基于词频的可选功能）
        double initialValue = 1.0 / N;
        Map<String, Double> pr = new HashMap<>();
        for (String k : graph.keySet()) {
             pr.put(k, initialValue);
        }   

        // 迭代计算
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<String, Double> newPr = new HashMap<>();
            double danglingSum = 0.0;

            // 计算悬挂节点的总PR值
            for (String node : graph.keySet()) {
                if (outDegreeMap.get(node) == 0) {
                    danglingSum += pr.get(node);
                }
            }
            double danglingContribution = DAMPING_FACTOR * danglingSum / N;

            // 计算每个节点的PR值
            for (String u : graph.keySet()) {
                double sum = 0.0;
                for (String v : inEdgesMap.get(u)) {
                    int Lv = outDegreeMap.get(v);
                    if (Lv > 0) {
                        sum += pr.get(v) / Lv;
                    }
                }
                newPr.put(u, (1 - DAMPING_FACTOR)/N + DAMPING_FACTOR * sum + danglingContribution);
            }

            // 检查收敛
            boolean converged = true;
            for (String node : graph.keySet()) {
                if (Math.abs(newPr.get(node) - pr.get(node)) > CONVERGENCE_THRESHOLD) {
                    converged = false;
                    break;
                }
            }
            
            pr = newPr;
            if (converged) break;
        }

        pageRankMap = pr;
    }

    public String randomWalk(String outputFileName) {
        if (graph.isEmpty()) {
            return "图为空，无法进行随机游走！";
        }
    
        // 随机选择起点
        String currentNode = new ArrayList<>(graph.keySet()).get(new Random().nextInt(graph.size()));
    
        StringBuilder result = new StringBuilder();
        result.append(currentNode);
    
        Set<String> visitedEdges = new HashSet<>();
    
        // 随机游走
        while (true) {
            Map<String, Integer> neighbors = graph.get(currentNode);
            if (neighbors == null || neighbors.isEmpty()) {
                // 当前节点没有出边，停止游走
                break;
            }
    
            // 随机选择一个邻接节点
            List<String> nextNodes = new ArrayList<>(neighbors.keySet());
            String nextNode = nextNodes.get(new Random().nextInt(nextNodes.size()));
            String edgeKey = currentNode + "->" + nextNode;
    
            // 检查是否已经访问过该边
            if (visitedEdges.contains(edgeKey)) {
                // 遇到重复的边，停止游走
                break;
            }
    
            result.append(" -> ").append(nextNode);
            visitedEdges.add(edgeKey);
            currentNode = nextNode;
        }
    
        // 将结果写入文件
        try {
            Files.write(Paths.get(outputFileName), result.toString().getBytes());
        } catch (IOException e) {
            return "无法写入文件: " + e.getMessage();
        }
    
        return result.toString();
    }
}