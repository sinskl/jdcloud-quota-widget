# 通用额度监控 Widget — 项目规格

## 1. 项目概述

**名称：** JDCloud Quota Widget  
**功能：** 登录通用 JoyBuilder，自动提取 Cookie，定时查询套餐额度，在桌面小组件显示  
**目标用户：** 通用 JoyBuilder 订阅用户（自用）  
**平台：** Android（minSdk 26, targetSdk 34）

---

## 2. 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin 1.9 |
| UI | Jetpack Compose + Glance（小组件） |
| DI | Hilt |
| 网络 | OkHttp + Retrofit |
| 登录 | Android WebView（过 TLS 指纹 + JS 挑战） |
| 本地存储 | DataStore（Cookie + 额度数据） |
| 后台任务 | WorkManager（1小时/次定时查询） |
| 构建 | Gradle (Kotlin DSL) |
| CI | GitHub Actions |

---

## 3. 功能列表

### 3.1 登录模块
- 通用 JoyBuilder WebView 登录页
- 自动拦截并提取 Cookie（pin, thor, qid_uid, qid_sid, jdv）
- 登录成功后显示"已登录"状态
- 退出登录（清除本地 Cookie）

### 3.2 额度查询
- 调用 `describeUserActivePlan` API
- 解析并展示：套餐名、到期时间、各周期额度已用量
- 支持手动刷新
- 1 小时自动刷新（WorkManager）

### 3.3 桌面小组件
- 单小组件（2x2 格）
- 显示：套餐名、5小时额度、月额度
- 点击跳转到 App 主界面
- 支持手动刷新

### 3.4 设置
- 刷新间隔配置（1小时 / 6小时 / 12小时 / 手动）
- 退出登录

---

## 4. UI 设计

**风格：** Material Design 3，单色+强调色  
**颜色：** JD Red (#E2231A) 主色，白/浅灰背景  
**字体：** 系统默认

**页面：**
1. **主页（LoginScreen）** — 未登录时显示 WebView 登录；已登录时显示额度卡片 + 设置入口
2. **设置页（SettingsScreen）** — 刷新间隔、退出登录

---

## 5. API 规格

**Base URL：** `https://joybuilder-console.jdcloud.com`

**查询接口：**
```
POST /openApi/modelservice/describeUserActivePlan
Headers:
  Cookie: pin=xxx; thor=xxx; qid_uid=xxx; qid_sid=xxx; jdv=xxx
  User-Agent: Mozilla/5.0 (Linux; Android 13; ...) Chrome/120
Query: _t=<timestamp_ms>
```

**Cookie 字段（必需）：**
- `pin` — 用户标识
- `thor` — 主会话 token
- `qid_uid` — 来源追踪
- `qid_sid` — 子会话 ID
- `jdv` — 来源渠道

---

## 6. GitHub Actions 构建

- 触发：push tag v* 或 push to main
- 构建： `./gradlew assembleDebug`
- 产物： `app/build/outputs/apk/debug/app-debug.apk`
- 分发： GitHub Release Asset

---

## 7. 项目结构

```
jdcloud-quota-widget/
├── SPEC.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── .github/workflows/build.yml
└── app/
    ├── build.gradle.kts
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/yi/jdcloud/
    │   │   ├── JdCloudApp.kt
    │   │   ├── MainActivity.kt
    │   │   ├── data/
    │   │   │   ├── ApiService.kt
    │   │   │   ├── CookieExtractor.kt
    │   │   │   ├── QuotaRepository.kt
    │   │   │   └── Preferences.kt
    │   │   ├── di/
    │   │   │   └── AppModule.kt
    │   │   ├── domain/
    │   │   │   └── QuotaModel.kt
    │   │   ├── ui/
    │   │   │   ├── theme/
    │   │   │   ├── login/
    │   │   │   └── settings/
    │   │   ├── worker/
    │   │   │   └── QuotaRefreshWorker.kt
    │   │   └── widget/
    │   │       └── QuotaWidget.kt
    │   └── res/
    │       └── ...
    └── src/main/res/drawable/
        └── ic_jdcloud.xml
```
