> 面试功能已移除，见 [面试移除说明](docs/interview-retirement.md)。历史 Version 1 说明和 PDF 不代表当前功能。

> 当前角色显示名称为 **Applicant、Teacher、admin**；Teacher 保留内部 `organiser` 键和路由。Reviewer、Decision-maker、Viewer 明确属于高级审核角色，不迁移或合并已有账号。完整 CSV 页面、模板、预览、导入导出及共享 API 仅限有效的 **admin** 账号；Teacher 无 CSV 入口。

> 当前版本基准：**Version 1（v1.0）**。主要改动、测试结果和已知限制见 [Version 1 版本说明](VERSION_1_zh.md)。

> Sponsor 新版字段、技能多选与 CSV 已实现，使用步骤见 [Sponsor 表单与 CSV 说明](SPONSOR_CSV_GUIDE_zh.md)。

> 当前 Proposal 流程与角色权限，以 [功能交接与验收](PROPOSAL_HANDOFF_zh.md) 为准（CSV 见上方新说明）。

> 新增：管理员可在 **Application Periods** 管理申请周期。操作步骤和规则见 [申请周期管理说明](APPLICATION_PERIODS_zh.md)。

[English](README.md) | [简体中文](README_zh.md)

# TA&MO — 助教招聘与管理系统

<div align="center">
  <a href="http://47.93.90.208:8080/tamo" target="_blank">
    <img src="backend/src/main/webapp/static/images/image.png" alt="TA&MO Logo" width="150">
  </a>
  <br>
  <strong>🌍 <a href="http://47.93.90.208:8080/tamo" target="_blank">官方网站: http://47.93.90.208:8080/tamo</a></strong>
</div>
<br>

> 基于 **Java Servlet/JSP** + **Embedded Tomcat** 构建的高校助教招聘与管理平台。

> [!NOTE]
> **📖 正在寻找用户手册？**
> 本代码库提供了一份详尽的、带图文注释的用户操作指南（OpenAI 文档风格），覆盖了所有 25 个核心界面功能。
> - 📄 [在线阅读 Markdown 版本](User_Manual_zh.md)
> - 📥 [下载高清 PDF 版本](User_Manual_zh.pdf)

---

## 目录

- [项目概述](#项目概述)
- [架构与技术栈](#架构与技术栈)
- [快速开始（安装与配置）](#快速开始安装与配置)
- [运行系统](#运行系统)
- [运行测试](#运行测试)
- [生成 Javadoc](#生成-javadoc)
- [项目结构](#项目结构)
- [用户角色与测试账号](#用户角色与测试账号)
- [API 参考](#api-参考)
- [常见问题排查](#常见问题排查)
- [开源许可](#开源许可)

---

## 项目概述

TA&MO 是一个专为大学设计的全栈 Web 应用程序，旨在管理助教（Teaching Assistant）招聘的全生命周期。它提供了一个强大的基于角色的平台，极大简化了从发布职位到录用的整个招聘流程。

| 角色 | 核心功能 |
|------|-------------|
| **申请人 (Applicant)** | 浏览与申请 TA 职位、追踪申请状态、管理个人资料与简历、在线沟通 |
| **模块组织者 (Organiser)** | 管理 TA 职位发布、通过看板（Kanban）视图审核候选人、记录已批准的 Section 分配 |
| **系统管理员 (Admin)** | 数据分析仪表盘、用户与模块管理、工作负载智能监控（AI 分析）、系统日志、底层数据编辑 |

---

## 架构与技术栈

```
┌───────────────────────────────────────────────────┐
│              Embedded Tomcat (端口 8080)          │
│                                                   │
│  ┌───────────────┐     ┌───────────────────────┐  │
│  │  JSP 前端     │     │  Servlet 后端 API     │  │
│  │  (18+ 个页面) │────▶│  (40+ 个 REST 接口)   │  │ 
│  │  Tailwind CSS │     │  Session 鉴权 + RBAC  │  │
│  │  Vanilla JS   │     │  PostgreSQL 持久化存储  │  │
│  └───────────────┘     └───────────────────────┘  │
│                              │                    │
│                        ┌─────▼─────┐              │
│                        │ PostgreSQL│              │
│                        └───────────┘              │
└───────────────────────────────────────────────────┘
```

| 层级 | 技术 | 版本 |
|-------|-----------|---------|
| 编程语言 | Java | 17 |
| 构建工具 | Apache Maven | 3.6+ |
| Web 服务器 | Embedded Apache Tomcat | 10.1.19 |
| Servlet API | Jakarta Servlet | 6.0.0 |
| JSP API | Jakarta JSP | 3.1.0 |
| JSON 解析 | Gson | 2.10.1 |
| 单元测试 | JUnit | 5.x |
| 样式与 UI | Tailwind CSS, Lucide Icons, Chart.js, Rive | 最新版 (通过 CDN) |

---

## 快速开始（安装与配置）

### 环境要求

在配置项目之前，请确保你的系统已安装以下软件：

1. **Java Development Kit (JDK) 17+**
   - 验证安装: `java -version` 和 `javac -version`
2. **Apache Maven 3.6+**
   - 验证安装: `mvn -version`
3. **Docker Desktop / Docker Engine + Compose**（用于本地 PostgreSQL）
4. **可用的网络连接**
   - 前端样式库依赖 CDN 下载（Tailwind CSS、Icons 等）。

### 1. 克隆代码库

```bash
git clone https://github.com/Elrond-JJ/Elrond-JJ-EBU6304-TA-Recruitment-System-Group100.git
cd Elrond-JJ-EBU6304-TA-Recruitment-System-Group100
git checkout dev
```

### 2. 配置说明

系统开箱即用，默认配置在本地运行，但生产环境可以部署到云服务器。如需修改默认行为，可参考以下信息：

- **服务器端口**: 嵌入式 Tomcat 默认运行在 **8080 端口**。如果此端口被占用，你可以在 `backend/src/main/java/com/tamo/Main.java` 文件中修改 `tomcat.setPort(8080);`。
- **数据库**: 应用使用本地 PostgreSQL 17；首次启动自动导入当前 `backend/src/main/resources/data.json` 并备份，之后仅写数据库。详见 [数据库使用说明](DATABASE_zh.md)。
- **上下文路径**: 应用服务的 Context Path 为 `/tamo`。

---

## 运行系统

我们使用 Maven 插件通过单条命令即可编译并启动嵌入式的 Tomcat 服务器。

### 启动应用

先启动 Docker Desktop，在项目根目录运行 `docker compose up -d --wait db`，再执行下面的后端命令。macOS/Linux 也可以直接在项目根目录运行 `./run.sh` 启动数据库和应用。

1. 进入 `backend` 目录:
   ```bash
   cd backend
   ```
2. 运行系统:
   ```bash
   mvn compile exec:java
   ```
   *此命令将自动下载 Maven 依赖、编译 Java 源码，并在 8080 端口启动 Tomcat 服务器。*

### 访问系统

服务器成功启动后（终端将输出 "TA&MO Backend Started"），打开浏览器并访问：

- **系统主页**: [http://localhost:8080/tamo](http://localhost:8080/tamo)
- **登录页面**: [http://localhost:8080/tamo/auth/login.jsp](http://localhost:8080/tamo/auth/login.jsp)
- **注册页面**: [http://localhost:8080/tamo/auth/signup.jsp](http://localhost:8080/tamo/auth/signup.jsp)

*如需停止服务器，请在终端中按下 `Ctrl + C`。*

---

## 运行测试

本项目包含详尽的自动化测试用例（超过 200 个 JUnit 测试），用于验证核心服务层、数据模型、安全过滤器及工具类的可靠性。

### 如何执行测试

进入 `backend` 目录，运行标准的 Maven 测试命令：

```bash
cd backend
mvn test
```

Maven 会通过 Surefire 插件自动执行所有测试。你将会看到 `BUILD SUCCESS` 的提示，以及已运行、失败和跳过的测试总结报告。

---

## 生成 Javadoc

整个 Java 代码库都具备专业的英文 Javadoc 注释，详尽说明了类、属性、方法、参数及返回值。

### 如何生成并查看 API 文档

1. 进入 `backend` 目录:
   ```bash
   cd backend
   ```
2. 使用 Maven 插件生成文档:
   ```bash
   mvn javadoc:javadoc
   ```
3. 在浏览器中打开生成的 API 文档。如果是 macOS，可使用以下命令直接打开：
   ```bash
   open target/site/apidocs/index.html
   ```

---

## 项目结构

```
TA&MO_v1.0/
├── backend/                           # 后端主程序
│   ├── pom.xml                        # Maven 配置文件
│   ├── TEST_DOCUMENT.md               # API 测试说明
│   │
│   ├── src/main/java/com/tamo/        # Java 源码
│   │   ├── Main.java                  # 入口程序 — 启动嵌入式 Tomcat
│   │   ├── filter/                    # 鉴权与 CORS 跨域过滤器
│   │   ├── listener/                  # Servlet 上下文监听器
│   │   ├── model/                     # 数据模型实体类 (POJOs)
│   │   ├── service/                   # 核心业务逻辑服务
│   │   ├── servlet/                   # RESTful API 控制器
│   │   └── util/                      # 工具辅助类
│   │
│   ├── src/main/resources/
│   │   └── data.json                  # 首次导入数据（运行后以 PostgreSQL 为准）
│   │
│   ├── src/main/webapp/               # 前端文件
│   │   ├── WEB-INF/web.xml            # Servlet 路由配置
│   │   ├── index.jsp                  # 落地页
│   │   ├── auth/, applicant/, admin/, organiser/ # 基于角色的 JSP 页面
│   │   ├── common/                    # 共享 UI 组件 (侧边栏、头部等)
│   │   └── static/                    # 静态资源 (CSS, JS, 图片)
│   │
│   └── src/test/java/com/tamo/        # JUnit 单元测试代码
│
├── screenshot/                        # 系统界面截图
└── README.md                          # 英文项目文档
```

---

## 用户角色与测试账号

首次导入随项目提供的测试数据后，可以使用以下账号；如已修改账号，以数据库中的记录为准。

| 角色 | 姓名 | 邮箱 | 密码 |
|------|------|-------|----------|
| Applicant | Alex Johnson | `alex.j@university.edu` | `demo123` |
| Applicant | Bob Smith | `bob.s@university.edu` | `demo123` |
| Organiser | Alan Turing | `alan@university.edu` | `demo123` |
| Organiser | Grace Hopper | `grace@university.edu` | `demo123` |
| Admin | System Admin | `admin@university.edu` | `admin123` |

---

## API 参考

后端通过 `/tamo/api/` 前缀暴露了 **42 个 RESTful API 接口**。所有受保护的接口均通过 `AuthFilter` 强制执行基于 HTTP Session 的身份验证。

- `/api/auth/*` : 注册、登录、会话验证。
- `/api/applicant/*` : 浏览职位、管理个人资料、提交申请。
- `/api/organiser/*` : 发布职位、管理看板流水线状态、记录 Section 分配。
- `/api/admin/*` : 系统数据分析、用户角色管理、系统日志、AI 工作负载分配。

*有关详细的请求/响应参数规范，请查阅 `backend/TEST_DOCUMENT.md`。*

---

## 常见问题排查

### 端口 8080 已被占用
如果有其他程序正在使用 8080 端口，请在启动前结束该进程：
```bash
lsof -ti :8080 | xargs kill -9
```

### 页面加载时没有样式或图标
前端严重依赖 CDN 链接来加载 Tailwind CSS 和 Lucide 图标。请检查你的网络连接。

### 数据意外丢失或需要重置
数据保存在 PostgreSQL 的 Docker 数据卷中。修改或恢复 `data.json` 不会重置已初始化的数据库。备份和恢复方法见 [数据库使用说明](DATABASE_zh.md)。

---

## 开源许可

本项目由 **Group 100** 为 **EBU6304 软件工程** 课程作业开发。有关分发条款，请参阅 `LICENSE` 文件。
