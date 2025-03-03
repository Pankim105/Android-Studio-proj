
一个集成终端模拟器、文件浏览器和Python执行环境的Android应用，专为移动端开发者和技术爱好者设计。

## 主要功能

### 🖥️ 终端模拟器
- 完整的Shell命令支持（cd、ls、cat等）
- 智能命令历史记录（⬆️⬇️浏览历史）
- 目录切换自动同步文件浏览器
- 支持多行Python代码输入
- ANSI颜色代码解析
- 一键清屏功能（clear命令）

### 📁 文件浏览器
- 实时目录结构展示
- 与终端路径同步
- 文件/目录基础操作（浏览、打开）
- 上下文菜单支持（未来扩展）

### 🐍 Python环境
- 基于Chaquopy的Python 3.x执行环境
- 交互式REPL模式
- 多行代码输入支持
- 标准输出/错误流捕获
- 第三方库支持（通过pip）

### 🖼️ 界面特性
- 响应式双窗格布局
- 可调节分隔栏（支持拖拽）
- 智能窗格管理（自动隐藏/显示）
- 自适应屏幕方向
- 夜间模式支持

## 快速开始

### 环境要求
- Android SDK 28+
- Java 11+
- Chaquopy Python插件（已集成）

### 安装步骤
```bash
git clone https://github.com/yourusername/tnote.git
cd tnote
# 使用Android Studio打开项目
# 连接Android设备或启动模拟器
# 点击运行按钮
使用指南
终端操作
输入Shell命令后按回车执行
使用python命令进入Python模式
输入exit()返回Shell
长按输入框滑动查看历史命令
双指捏合调整字体大小
文件管理
终端输入cd [路径]同步目录
点击文件浏览器中的目录进行导航
长按文件/目录弹出操作菜单
使用右上角按钮创建新文件
界面操作
拖动分隔线调整窗格比例
双击分隔线切换单/双窗模式
横屏时自动进入分屏模式
滑动边缘呼出快速命令面板
技术架构
核心组件
MainActivity：管理窗口布局和导航
TerminalFragment：终端界面与逻辑控制
ShellSession：本地Shell进程管理
PythonSession：Python解释器交互
FileBrowserFragment：文件系统可视化
关键技术
AndroidX & Material Design组件
Chaquopy Python运行时
ANSI转Spanned文本渲染
多线程进程管理
实时目录监听
手势驱动界面
