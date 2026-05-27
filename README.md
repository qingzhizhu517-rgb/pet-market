# 🐾 宠物集市

一款以猫狗为主的宠物集市与品种百科 Android 应用，支持浏览宠物详情、品种百科、AI 智能顾问以及收藏功能。

## 功能特性

- **宝贝集市** — 浏览待领养/出售的宠物列表，支持按分类（狗狗/猫咪/小宠）筛选和关键词搜索，可发布自己的送养帖子
- **品种百科** — 查看详细的品种信息，包括品性雷达图、性格标签、科学喂养指南等
- **AI 顾问** — 基于 Gemini API 的智能问答，提供品种推荐、喂养建议、养宠新手指南等专业回答（离线时自动切换本地知识库）
- **我的收藏** — 收藏感兴趣的宠物帖子和品种百科，方便随时查看

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM (ViewModel + StateFlow) |
| 本地存储 | Room Database |
| 网络 | Retrofit + OkHttp |
| 图片加载 | Coil |
| AI 能力 | Google Gemini API |
| 密钥管理 | Secrets Gradle Plugin (.env) |

## 本地运行

**前置条件：** [Android Studio](https://developer.android.com/studio)

1. 克隆本仓库
2. 使用 Android Studio 打开项目目录
3. 在项目根目录创建 `.env` 文件，设置你的 Gemini API Key：
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
4. 等待 Gradle 同步完成
5. 在模拟器或真机上运行应用

> 如果没有 Gemini API Key，应用会自动使用内置的本地知识库回答问题。

## 项目结构

```
app/src/main/java/com/example/
├── MainActivity.kt          # 主界面（集市、百科、AI聊天、收藏）
├── data/
│   ├── BreedData.kt         # 品种百科数据
│   ├── Database.kt          # Room 数据库定义
│   ├── GeminiService.kt     # Gemini API 服务
│   └── Models.kt            # 数据模型
├── ui/theme/                 # Material 3 主题配置
└── viewmodel/
    └── MainViewModel.kt     # 业务逻辑 ViewModel
```

## 截图

| 宝贝集市 | 品种百科 | AI 顾问 |
|:---------:|:---------:|:---------:|
| <img width="250" alt="宝贝集市" src="https://github.com/user-attachments/assets/6ff59d72-0777-49c1-b936-965e4fdc4a5e" /> | <img width="250" alt="品种百科" src="https://github.com/user-attachments/assets/fe41497e-73fd-4977-8e09-5290029fb78c" /> | <img width="250" alt="AI顾问" src="https://github.com/user-attachments/assets/d1e8b4bf-bb35-42fc-995c-d0f0017a2381" /> |
| 浏览宠物列表，筛选搜索 | 品种详情与品性雷达图 | 智能问答，快速提问 |

| 我的收藏 | 发布帖子 |
|:---------:|:---------:|
| <img width="250" alt="我的收藏" src="https://github.com/user-attachments/assets/4bb8dfdb-b24d-4f08-90d0-eb5be1dfee0b" /> | <img width="250" alt="发布帖子" src="https://github.com/user-attachments/assets/43e10198-16da-406f-aee7-51770baec57a" /> |
| 收藏品种与宠物帖子 | 发布送养/出售信息 |

## 许可证

本项目仅供学习交流使用。
