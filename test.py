import tkinter as tk
from tkinter import ttk, scrolledtext, filedialog, messagebox
import networkx as nx
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import re
import random
import numpy as np
from PIL import Image, ImageTk


class TextGraphGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Text Graph Analysis")
        self.root.geometry("1200x800")
        
        # 设置主题样式
        style = ttk.Style()
        style.theme_use('clam')
        
        self.graph = TextGraph()
        self.setup_gui()
        
    def setup_gui(self):
        # 创建主框架
        self.main_frame = ttk.Frame(self.root)
        self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # 左侧控制面板 垂直方向填满
        self.control_frame = ttk.LabelFrame(self.main_frame, text="Controls")
        self.control_frame.pack(side=tk.LEFT, fill=tk.Y, padx=5, pady=5)
        
        # 文件选择按钮
        self.file_btn = ttk.Button(self.control_frame, text="Select File", 
                                 command=self.load_file)
        self.file_btn.pack(fill=tk.X, padx=5, pady=5)
        
        # 功能按钮
        functions = [
            ("Show Graph", self.show_graph),
            ("Query Bridge Words", self.show_bridge_query),
            ("Generate New Text", self.show_text_generation),
            ("Shortest Path", self.show_shortest_path),
            ("PageRank", self.show_pagerank),
            ("Random Walk", self.perform_random_walk)
        ]
        
        for text, command in functions:
            btn = ttk.Button(self.control_frame, text=text, command=command)
            btn.pack(fill=tk.X, padx=5, pady=2)
            
        # 右侧主要显示区域
        self.display_frame = ttk.Frame(self.main_frame)
        self.display_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)
        
        # 创建绘图区域
        self.fig, self.ax = plt.subplots(figsize=(8, 6))
        self.canvas = FigureCanvasTkAgg(self.fig, master=self.display_frame)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        # 创建文本显示区域 按照单词分行
        self.text_area = scrolledtext.ScrolledText(self.display_frame, 
                                                 height=10, wrap=tk.WORD)
        self.text_area.pack(fill=tk.X, padx=5, pady=5)
        
        # 创建输入框框架
        self.input_frame = ttk.Frame(self.display_frame)
        self.input_frame.pack(fill=tk.X, padx=5, pady=5)
        # 保存按钮
        self.save_btn = ttk.Button(self.control_frame, 
                              text="Save Graph as PNG", 
                              command=self.save_graph)
        self.save_btn.pack(fill=tk.X, padx=5, pady=2)
        
        # 初始隐藏输入框
        self.hide_input_widgets()
        
    def load_file(self):
        filename = filedialog.askopenfilename(
            title="Select text file",
            filetypes=(("Text files", "*.txt"), ("All files", "*.*"))
        )
        if filename:
            if self.graph.process_text(filename):
                # 插入到文本区域末尾
                self.text_area.insert(tk.END, f"File loaded: {filename}\n")
                # self.show_graph()
            else:
                messagebox.showerror("Error", "Failed to process file")

    def save_graph(self):
        file_path = filedialog.asksaveasfilename(
            defaultextension=".png",
            filetypes=[("PNG files", "*.png"), ("All files", "*.*")]
        )
        if file_path:
            try:
                self.fig.savefig(file_path, 
                                bbox_inches='tight', 
                                dpi=300, 
                                format='png')
                messagebox.showinfo("Success", 
                                f"Graph saved successfully to {file_path}")
            except Exception as e:
                messagebox.showerror("Error", 
                                f"Failed to save graph: {str(e)}")

    def show_graph(self):
        self.hide_input_widgets()
        # 清空绘图区域
        self.ax.clear()

        pos = nx.spring_layout(self.graph.G)
        nx.draw_networkx(self.graph.G, pos, ax=self.ax, 
                        node_color='lightblue', 
                        node_size=1000, 
                        alpha=0.6,
                        with_labels=True)
        edge_weights = nx.get_edge_attributes(self.graph.G, 'weight')
        nx.draw_networkx_edge_labels(self.graph.G, pos, 
                                   edge_labels=edge_weights, ax=self.ax)
        self.canvas.draw()
        
    def hide_input_widgets(self):
        """隐藏所有输入组件"""
        for widget in self.input_frame.winfo_children():
            widget.destroy()
            
    def show_bridge_query(self):
        self.hide_input_widgets()
        
        # 创建输入框
        ttk.Label(self.input_frame, text="Word 1:").pack(side=tk.LEFT)
        word1_entry = ttk.Entry(self.input_frame)
        word1_entry.pack(side=tk.LEFT, padx=5)
        
        ttk.Label(self.input_frame, text="Word 2:").pack(side=tk.LEFT)
        word2_entry = ttk.Entry(self.input_frame)
        word2_entry.pack(side=tk.LEFT, padx=5)
        # 清空原有区域
        def query():
            result = self.graph.query_bridge_words(
                word1_entry.get(), word2_entry.get())
            self.text_area.delete(1.0, tk.END)
            self.text_area.insert(tk.END, result + "\n")
            
        ttk.Button(self.input_frame, text="Query", 
                  command=query).pack(side=tk.LEFT, padx=5)
        
    def show_text_generation(self):
        self.hide_input_widgets()
        
        ttk.Label(self.input_frame, text="Input Text:").pack(side=tk.LEFT)
        text_entry = ttk.Entry(self.input_frame, width=50)
        text_entry.pack(side=tk.LEFT, padx=5)
        
        def generate():
            result = self.graph.generate_new_text(text_entry.get())
            self.text_area.delete(1.0, tk.END)
            self.text_area.insert(tk.END, "Generated text:\n" + result + "\n")
            
        ttk.Button(self.input_frame, text="Generate", 
                  command=generate).pack(side=tk.LEFT, padx=5)
        
    def show_pagerank(self):
        self.hide_input_widgets()
        
        # 创建一个框架来容纳个性化PR设置
        pr_frame = ttk.LabelFrame(self.input_frame, text="Personalized PageRank")
        pr_frame.pack(side=tk.LEFT, padx=5)
        
        # 添加单词和PR值的输入框
        ttk.Label(pr_frame, text="Word:").pack(side=tk.LEFT)
        word_entry = ttk.Entry(pr_frame, width=15)
        word_entry.pack(side=tk.LEFT, padx=2)
        
        ttk.Label(pr_frame, text="Initial PR:").pack(side=tk.LEFT)
        pr_entry = ttk.Entry(pr_frame, width=8)
        pr_entry.pack(side=tk.LEFT, padx=2)
        
        # 存储个性化PR值的字典
        personalization = {}
        
        def add_personalized_pr():
            word = word_entry.get().strip().lower()
            try:
                pr_value = float(pr_entry.get().strip())
                if word and pr_value > 0:
                    personalization[word] = pr_value
                    self.text_area.insert(tk.END, 
                                        f"Added personalized PR: {word} = {pr_value}\n")
                    word_entry.delete(0, tk.END)
                    pr_entry.delete(0, tk.END)
            except ValueError:
                messagebox.showerror("Error", "Please enter a valid PR value")
        
        ttk.Button(pr_frame, text="Add PR", 
                command=add_personalized_pr).pack(side=tk.LEFT, padx=2)
        
        # 查询框架
        query_frame = ttk.Frame(self.input_frame)
        query_frame.pack(side=tk.LEFT, padx=5)
        
        ttk.Label(query_frame, text="Query word (optional):").pack(side=tk.LEFT)
        query_word_entry = ttk.Entry(query_frame)
        query_word_entry.pack(side=tk.LEFT, padx=5)
        
        def calculate():
            word = query_word_entry.get().strip().lower()
            # 如果没有设置个性化PR，使用None
            pers = personalization if personalization else None
            
            if word:
                result = self.graph.calc_page_rank(word, personalization=pers)
                self.text_area.delete(1.0, tk.END)
                self.text_area.insert(tk.END, 
                                    f"PageRank for {word}: {result}\n")
            else:
                pr = self.graph.calc_page_rank(personalization=pers)
                self.text_area.delete(1.0, tk.END)
                for word, rank in sorted(pr.items(), 
                                    key=lambda x: x[1], 
                                    reverse=True):
                    self.text_area.insert(tk.END, 
                                        f"{word}: {rank:.4f}\n")
        
        ttk.Button(query_frame, text="Calculate", 
                command=calculate).pack(side=tk.LEFT, padx=5)
        
    def perform_random_walk(self):
        self.hide_input_widgets()
        result = self.graph.random_walk()
        self.text_area.delete(1.0, tk.END)
        self.text_area.insert(tk.END, "Random walk result:\n" + result + "\n")
        
        # 保存到文件
        with open("random_walk.txt", "w") as f:
            f.write(result)
        self.text_area.insert(tk.END, 
                            "Result saved to random_walk.txt\n")
        

    def show_shortest_path(self):
        self.hide_input_widgets()
        
        ttk.Label(self.input_frame, text="From:").pack(side=tk.LEFT)
        word1_entry = ttk.Entry(self.input_frame)
        word1_entry.pack(side=tk.LEFT, padx=5)
        
        ttk.Label(self.input_frame, text="To (optional):").pack(side=tk.LEFT)
        word2_entry = ttk.Entry(self.input_frame)
        word2_entry.pack(side=tk.LEFT, padx=5)
        
        def find_path():
            word1 = word1_entry.get().strip()
            word2 = word2_entry.get().strip()
            
            if not word1:
                messagebox.showerror("Error", "Please enter at least one word!")
                return
                
            result, paths_data = self.graph.calc_shortest_path(word1, word2)
            
            # 显示文本结果
            self.text_area.delete(1.0, tk.END)
            self.text_area.insert(tk.END, result)
            
            # 更新图形显示
            if paths_data:
                self.ax.clear()
                self.graph.draw_path_in_graph(self.ax, paths_data)
                self.canvas.draw()
            
        ttk.Button(self.input_frame, text="Find Path", 
                  command=find_path).pack(side=tk.LEFT, padx=5)
        
class TextGraph:
    def __init__(self):
        self.G = nx.DiGraph()
        self.words = []
        
    def process_text(self, filename):
        """处理文本文件并构建有向图"""
        try:
            with open(filename, 'r', encoding='utf-8') as file:
                text = file.read().lower()
                
            # 将标点符号和换行符替换为空格
            text = re.sub(r'[^a-z\s]', ' ', text)
            # 分割成单词列表
            self.words = [word for word in text.split() if word.isalpha()]
            
            # 构建有向图
            for i in range(len(self.words) - 1):
                word1, word2 = self.words[i], self.words[i + 1]
                if self.G.has_edge(word1, word2):
                    self.G[word1][word2]['weight'] += 1
                else:
                    self.G.add_edge(word1, word2, weight=1)
                    
            return True
        except Exception as e:
            print(f"Error processing file: {e}")
            return False

    def show_directed_graph(self):
        """展示有向图"""
        plt.figure(figsize=(12, 8))
        # 返回节点和二维坐标
        pos = nx.spring_layout(self.G)
        
        # 绘制节点
        nx.draw_networkx_nodes(self.G, pos, node_color='lightblue', 
                             node_size=1000, alpha=0.6)
        
        # 绘制边和权重
        edge_weights = nx.get_edge_attributes(self.G, 'weight')
        nx.draw_networkx_edge_labels(self.G, pos, edge_labels=edge_weights)
        nx.draw_networkx_edges(self.G, pos, edge_color='gray', arrows=True)
        
        # 添加标签
        nx.draw_networkx_labels(self.G, pos)
        
        plt.title("Directed Graph of Text")
        #隐藏坐标
        plt.axis('off')
        plt.show()
        
    def query_bridge_words(self, word1, word2):
        """查询桥接词"""
        word1, word2 = word1.lower(), word2.lower()
        
        if not (word1 in self.G and word2 in self.G):
            return f"No {word1} or {word2} in the graph!"
            
        bridge_words = []
        for node in self.G.nodes():
            if self.G.has_edge(word1, node) and self.G.has_edge(node, word2):
                bridge_words.append(node)
                
        if not bridge_words:
            return f"No bridge words from {word1} to {word2}!"
        elif len(bridge_words) == 1:
            return f"The bridge word from {word1} to {word2} is: {bridge_words[0]}."
        else:
            return f"The bridge words from {word1} to {word2} are: {', '.join(bridge_words[:-1])} and {bridge_words[-1]}."

    def generate_new_text(self, input_text):
        """根据bridge word生成新文本"""
        words = input_text.lower().split()
        result = [words[0]]
        
        for i in range(len(words)-1):
            word1, word2 = words[i], words[i+1]
            bridge_words = []
            
            if word1 in self.G and word2 in self.G:
                for node in self.G.nodes():
                    if self.G.has_edge(word1, node) and self.G.has_edge(node, word2):
                        bridge_words.append(node)
            
            result.append(random.choice(bridge_words) if bridge_words else "")
            # 测试下一个
            result.append(word2)
            
        return " ".join(filter(None, result))

    def calc_shortest_path(self, word1, word2=None):
        """计算最短路径，支持单个词和两个词的情况"""
        word1 = word1.lower()
        if word2:
            word2 = word2.lower()
            
        if word1 not in self.G:
            return "Start word not in graph!", None
            
        if word2 and word2 not in self.G:
            return "End word not in graph!", None
            
        paths_data = []
        
        if word2:  # 计算两个词之间的所有最短路径
            try:
                # 获取所有最短路径
                all_shortest_paths = list(nx.all_shortest_paths(
                    self.G, word1, word2, weight='weight'))
                
                if not all_shortest_paths:
                    return f"No path exists between {word1} and {word2}", None
                    
                # 计算每条路径的长度
                for path in all_shortest_paths:
                    length = sum(self.G[path[i]][path[i+1]]['weight'] 
                               for i in range(len(path)-1))
                    paths_data.append({
                        'path': path,
                        'length': length,
                        'path_str': " → ".join(path)
                    })
                    
                result = "Found following shortest paths:\n"
                for data in paths_data:
                    result += f"Path: {data['path_str']}\n"
                    result += f"Length: {data['length']}\n\n"
                    
            except nx.NetworkXNoPath:
                return f"No path exists between {word1} and {word2}", None
                
        else:  # 计算从一个词到所有其他词的最短路径
            result = f"Shortest paths from {word1} to all other words:\n\n"
            for target in self.G.nodes():
                if target != word1:
                    try:
                        path = nx.shortest_path(
                            self.G, word1, target, weight='weight')
                        length = sum(self.G[path[i]][path[i+1]]['weight'] 
                                   for i in range(len(path)-1))
                        paths_data.append({
                            'path': path,
                            'length': length,
                            'path_str': " → ".join(path)
                        })
                    except nx.NetworkXNoPath:
                        continue
                        
            # 按路径长度升序排序
            paths_data.sort(key=lambda x: x['length'])
            
            for data in paths_data:
                result += f"To {data['path'][-1]}:\n"
                result += f"Path: {data['path_str']}\n"
                result += f"Length: {data['length']}\n\n"
                
        return result, paths_data

    def draw_path_in_graph(self, ax, paths_data):
        """在图中突出显示路径"""
        if not paths_data:
            return
            
        # 清除当前图形
        ax.clear()
        
        # 获取布局
        pos = nx.spring_layout(self.G)
        
        # 绘制所有边（灰色，作为背景）
        nx.draw_networkx_edges(self.G, pos, ax=ax, 
                             edge_color='gray', alpha=0.2)
        
        # 绘制所有节点（浅色）
        nx.draw_networkx_nodes(self.G, pos, ax=ax,
                             node_color='lightgray',
                             node_size=1000, alpha=0.6)
        
        # 绘制所有节点标签
        nx.draw_networkx_labels(self.G, pos, ax=ax)
        
        # 为每条路径使用不同的颜色
        colors = ['r', 'b', 'g', 'c', 'm', 'y']
        
        for idx, path_data in enumerate(paths_data):
            path = path_data['path']
            color = colors[idx % len(colors)]
            
            # 突出显示路径上的节点
            path_nodes = path
            nx.draw_networkx_nodes(self.G, pos, ax=ax,
                                 nodelist=path_nodes,
                                 node_color=color,
                                 node_size=1200)
            
            # 突出显示路径上的边
            path_edges = list(zip(path[:-1], path[1:]))
            nx.draw_networkx_edges(self.G, pos, ax=ax,
                                 edgelist=path_edges,
                                 edge_color=color,
                                 width=2)
            
            # 添加边的权重标签
            edge_labels = {(path[i], path[i+1]): 
                         self.G[path[i]][path[i+1]]['weight']
                         for i in range(len(path)-1)}
            nx.draw_networkx_edge_labels(self.G, pos,
                                       edge_labels=edge_labels)
        
        ax.set_title("Shortest Path(s) Visualization")
        ax.axis('off')


    def calc_page_rank(self, word=None, personalization=None):

        # 使用个性化向量计算PageRank
        page_ranks = nx.pagerank(self.G, alpha=0.85, personalization=personalization)
        if word:
            return page_ranks.get(word.lower(), 0)
        return page_ranks

    def random_walk(self):
        """随机游走"""
        if not self.G.nodes():
            return "Graph is empty!"
            
        current = random.choice(list(self.G.nodes()))
        path = [current]
        # 避免重复 
        edges_used = set()
        
        while True:
            neighbors = list(self.G.neighbors(current))
            if not neighbors:
                break
                
            next_node = random.choice(neighbors)
            edge = (current, next_node)
            
            if edge in edges_used:
                break
                
            edges_used.add(edge)
            path.append(next_node)
            current = next_node
            
        return " ".join(path)

def main():
    #创建主窗口
    root = tk.Tk()

    app = TextGraphGUI(root)
    root.mainloop()

if __name__ == "__main__":
    print("测试1")
    main()
