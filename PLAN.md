# URL Tracker Remover — Android App

## Context

使用者需要一個 Android 工具：當任何 app 把 URL 分享給它，它會移除追蹤參數（utm_*, fbclid, gclid, igshid, …），然後依使用者設定把乾淨 URL 放到剪貼簿（並 Toast 提示），或再次觸發系統 share sheet 轉發給其他 app。

另外有主畫面可手動貼 URL 清理、可選的歷史紀錄（純文字，不抓 preview）、可手動同步最新規則表。

技術選型：**原生 Android (Kotlin + Jetpack Compose)**，規則來源採用 **ClearURLs** 的 `data.min.json`（內建一份，啟動/手動可從網路更新）。這個決定是為了：
- GitHub Actions 用 ubuntu runner 即可編 APK，不需要 macOS runner。
- ClearURLs 規則表涵蓋數百個網站、數千條規則（Facebook、Instagram、Threads、Reddit、YouTube、Amazon、Twitter/X、TikTok、Google、Bing…），比自己維護覆蓋率高太多。
- 通用 `globalRules`（utm_*, fbclid, gclid, mc_cid, _ga, …）對沒列在表裡的網站也有效。

## 使用者行為 / UI 需求

1. **主畫面（MainActivity）**
   - 頂部「貼上框」：`OutlinedTextField`，`readOnly = true`，長按顯示「貼上」選單（透過 `SelectionContainer` + 自訂 context menu，或用 `LocalClipboardManager` + 長按手勢直接讀剪貼簿）。
   - 貼上後：立即顯示 before / after 兩段 URL、一個「複製」按鈕（預設已自動複製），並寫入歷史紀錄（若使用者開啟）。
   - 設定按鈕進入 Settings。
   - 歷史清單（另一個畫面，只在設定裡開啟紀錄後才能進入）：顯示原 URL → 清理後 URL + 時間戳。每筆右側有「複製」按鈕（複製乾淨 URL）；頂部工具列有「複製全部」（把所有乾淨 URL 用換行串成文字丟剪貼簿）和「全部清空」（彈確認對話框，清 Room table）。不抓 page preview，純文字。
2. **Settings 畫面**
   - 分享行為：單選
     - 直接放剪貼簿 + Toast（預設）
     - 放剪貼簿後再觸發一次分享（讓使用者選要丟到哪個 app）
     - 兩者都要：放剪貼簿 + 再觸發分享
   - 紀錄歷史：開/關（預設關，保護隱私）
   - 移除 referral marketing 參數：開/關（ClearURLs 將 `ref`、`affiliate_id` 等歸在獨立分類，預設開）
   - 「立即同步規則表」按鈕 + 顯示目前規則版本 / 最後更新時間
3. **歷史畫面（HistoryScreen）**
   - 進入方式：MainScreen 上 icon / Settings 上一個連結；若紀錄功能關閉，入口 disabled 並說明。
   - 列表項：原 URL（單行省略）、清理後 URL（單行省略）、相對時間（「剛剛 / 3 分鐘前 / 昨天」）。
   - 單筆操作：點擊展開完整 URL；右側 `IconButton` 複製該筆乾淨 URL（Toast 提示）；長按或 swipe 刪除。
   - 頂部 overflow menu：
     - 「複製全部」：把所有乾淨 URL 以 `\n` 串起來丟剪貼簿，Toast「已複製 N 筆」。
     - 「全部清空」：`AlertDialog` 二次確認，確認後清 Room 表，回空狀態。
4. **分享入口（ShareHandlerActivity）**
   - 接收 `ACTION_SEND` + `text/plain`。
   - 從分享文字中抽出第一個 URL（文字可能含前後文），清理。
   - 依設定執行：寫剪貼簿、Toast、或再 `startActivity(Intent.createChooser(...))`。
   - 透明 activity，做完就 finish，不留 UI。

## 專案結構

```
/
├── .github/workflows/android.yml   # CI: assembleDebug + 上傳 APK artifact
├── .gitignore                      # Android/Gradle/IDE 排除
├── README.md                       # 使用說明、下載、授權
├── PLAN.md                         # 本計畫檔的副本（使用者要求附在 repo）
├── LICENSE                         # MIT（與 ClearURLs rules 的 LGPL 規則資料分離，於 NOTICE 說明）
├── NOTICE                          # ClearURLs 歸屬聲明
├── build.gradle.kts                # root
├── settings.gradle.kts
├── gradle/wrapper/...              # gradle wrapper 8.x
├── gradlew, gradlew.bat
└── app/
    ├── build.gradle.kts            # Kotlin 2.0, Compose BOM, minSdk 24, targetSdk 34
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/
        │   └── clearurls-rules.json        # 內建快照，啟動/手動可更新
        ├── kotlin/app/urlcleaner/
        │   ├── MainActivity.kt             # Compose 主畫面
        │   ├── ShareHandlerActivity.kt     # 透明 activity，處理 ACTION_SEND
        │   ├── SettingsActivity.kt         # 或在 MainActivity 中用 Nav
        │   ├── cleaner/
        │   │   ├── UrlCleaner.kt           # 核心邏輯（純 Kotlin，無 Android 依賴）
        │   │   ├── Rules.kt                # 資料類：Provider, Rule, …
        │   │   └── RulesParser.kt          # 解析 ClearURLs data.min.json
        │   ├── data/
        │   │   ├── RulesRepository.kt      # 內建 + 網路更新 + 本地快取（files dir）
        │   │   ├── HistoryRepository.kt    # 單純 filesDir/history.tsv 讀寫
        │   │   └── SettingsRepository.kt   # DataStore<Preferences>
        │   ├── ui/
        │   │   ├── MainScreen.kt
        │   │   ├── SettingsScreen.kt
        │   │   ├── HistoryScreen.kt
        │   │   └── theme/                  # Material 3
        │   └── util/
        │       └── UrlExtractor.kt         # 從任意文字抓第一個 URL
        └── src/test/kotlin/app/urlcleaner/cleaner/
            ├── UrlCleanerTest.kt           # 針對 FB/IG/Threads/Reddit/YouTube/Amazon/Google/X 的案例
            └── UrlExtractorTest.kt
```

## 核心邏輯 — UrlCleaner

依 ClearURLs 規則格式實作（`providers` map，每個 provider 有 `urlPattern`、`rules`、`referralMarketing`、`rawRules`、`exceptions`、`redirections`、`completeProvider`）。

清理流程（對每個 provider 只要 `urlPattern` match 就套用）：
1. **Redirections**：若 provider 有 `redirections` 且 match，抽出第一個 capture group 當新 URL，解 URL-decode 後遞迴重跑整個清理流程（例如 `facebook.com/l.php?u=...`、`google.com/url?q=...`、`l.instagram.com`）。限制最多 5 次避免 loop。
2. **Exceptions**：若任一 exception regex match，該 provider 整組規則跳過。
3. **Complete Provider**：若 `completeProvider = true`，回傳空字串或提示「這個連結整段都是追蹤」（UI 顯示警告、不寫剪貼簿）。
4. **Raw Rules**：對完整 URL 字串套用 regex replace。
5. **Query params**：逐個 `rules`（+ 若設定開啟，合併 `referralMarketing`）當作 query key 的 regex，刪除符合的 key（要處理 key 大小寫不敏感、`?a=1&b=2&c=3` 的 `&`/`?` 邊界）。
6. **Fragment**：ClearURLs 規則也會清 fragment 內的 query-like 參數，照樣處理。
7. 清理後若 query 為空，移除尾端 `?`。

**globalRules** 對所有 URL 都跑一次（在 provider 規則後或前都可，ClearURLs 實作是先跑 globalRules）。

## 規則表來源與更新

- 內建 `app/src/main/assets/clearurls-rules.json`：建置時從 <https://rules2.clearurls.xyz/data.min.json> 下載最新版塞進去（或第一次手動下載提交）。
- 啟動時：若本地有更新版快取（`filesDir/rules.json`）就用，否則用 assets 版。**不**自動在背景更新（尊重使用者要求「手動同步」）。
- 設定畫面「立即同步規則表」按鈕：用 `HttpURLConnection` 抓 `https://rules2.clearurls.xyz/data.min.json`（connect/read timeout 各 10s），比對 `hash` 欄位（ClearURLs 附 sha256），寫入 `filesDir/rules.json`，更新設定裡的「版本/時間」。失敗時 Snackbar 顯示原因。檔案本身已 minified（這就是 `data.min.json` 的意義），不需再壓縮。
- 授權：ClearURLs 規則資料是 LGPL-3.0，在 NOTICE / README 附上出處與授權連結。

## 分享行為實作細節

- `AndroidManifest.xml` `ShareHandlerActivity` 聲明：
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.SEND" />
      <category android:name="android.intent.category.DEFAULT" />
      <data android:mimeType="text/plain" />
  </intent-filter>
  ```
- 主題 `Theme.Translucent.NoTitleBar`，不啟動 Compose UI。
- 行為分支（從 SettingsRepository 讀設定）：
  - **clipboard-only**：`ClipboardManager.setPrimaryClip(...)` + `Toast.makeText(...).show()`。注意 Android 13+ 寫剪貼簿會自動顯示系統通知框，額外 Toast 仍需要（顯示「已清理 N 個參數」）。
  - **re-share**：寫剪貼簿後 `startActivity(Intent.createChooser(Intent(ACTION_SEND).putExtra(EXTRA_TEXT, cleaned), null))`。
  - 若 `completeProvider` 或完全沒變動：Toast 告知「此連結無追蹤碼」或「整段都是追蹤連結，未處理」，不再觸發分享。

## 依賴（app/build.gradle.kts）

**最小依賴原則**——只加無法合理避免的：

- `androidx.activity:activity-compose`
- `androidx.compose.bom`（material3、ui、ui-tooling-preview、ui-tooling debug-only）
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `androidx.navigation:navigation-compose`
- `androidx.datastore:datastore-preferences`（輕量，只存幾個 flag/字串）
- test: `junit`、`kotlin-test`

**刻意不加**：
- ~~Room~~ → 歷史用 `filesDir/history.tsv`，每行 `timestamp<TAB>original<TAB>cleaned`。append 用 `FileWriter(..., true)`；clear all 用 `file.writeText("")` 或 `file.delete()`；單筆刪除用讀進記憶體、過濾、整檔覆寫（個人量級幾百筆內可接受）。
- ~~OkHttp~~ → 規則下載用 `HttpURLConnection`（JDK 內建，約 30 行足夠：set timeout、讀 body、比對既有 `hash` 欄位、寫檔）。
- ~~kotlinx-serialization~~ → 規則解析用 `org.json.JSONObject`（Android 內建，零依賴）。`RulesParser.kt` 把 `data.min.json` 轉成 `List<Provider>` 資料類。

## GitHub Actions — `.github/workflows/android.yml`

- trigger：push 到任何分支 + PR
- runner：`ubuntu-latest`
- 步驟：
  1. checkout
  2. setup JDK 17 (temurin)
  3. `gradle/actions/setup-gradle@v3`（含 cache）
  4. `./gradlew lint testDebugUnitTest assembleDebug`
  5. `./gradlew assembleRelease`（已簽名）
  6. upload `app/build/outputs/apk/release/app-release.apk` + debug apk 為 artifact
  7. 若 push 了 `v*` tag：`softprops/action-gh-release` 自動建 Release 並附上 release APK

### 簽名策略

使用者明示「隨便弄一個新簽章就好，需要 fork 的人自己改」——走**最簡路徑**：

- 用 `keytool` 產一把 25 年期 RSA 2048 keystore，commit 到 `app/signing/release.jks`（self-signed，只影響更新者必須用同 key 簽名才能覆蓋安裝，對私人工具可接受）。
- 密碼/alias/key 密碼都放進 `app/build.gradle.kts` 的 `signingConfigs.release` **明文**（配 CI secrets 的 override：若環境變數 `URLCLEANER_KEYSTORE_PASSWORD` 存在就蓋掉，讓 fork 者可改用自己的）。
- README 明確說明此 keystore 是公開 self-signed、不代表身分驗證、fork 者應替換成自己的。
- `.gitignore` 保留對「其他」keystore（`*.keystore`、`*.jks`）的排除，例外放行 `app/signing/release.jks`。

執行細節（實作階段會跑）：
```
keytool -genkey -v -keystore app/signing/release.jks -alias urlcleaner \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -storepass urlcleaner -keypass urlcleaner \
  -dname "CN=URL Cleaner, OU=Personal, O=Personal, L=NA, ST=NA, C=NA"
```

## .gitignore

標準 Android/Gradle/IDE 模板：`.gradle/`, `build/`, `local.properties`, `.idea/`（除了 codeStyles）, `*.iml`, `.DS_Store`, `captures/`, `*.keystore`, `/app/release/` 等。

## README.md 內容

- 專案目的與功能摘要
- 螢幕截圖（暫用佔位，實作完補）
- 安裝：從 Releases 下 APK，或 GitHub Actions artifact
- 使用方式：從其他 app 分享 URL 過來，或打開 app 手動貼
- 規則來源：ClearURLs（含連結與授權聲明）
- 編譯：`./gradlew assembleDebug`
- 授權：MIT（程式碼）+ LGPL-3.0（rules 資料）

## 驗證 / 測試

**單元測試**（跑 `./gradlew testDebugUnitTest`）：
- `UrlCleanerTest` 涵蓋案例（至少 20 個）：
  - Facebook: `?fbclid=...` 移除
  - Facebook `l.php?u=...` redirection 解開
  - Instagram: `?igshid=...&utm_source=ig_web`
  - Threads: `?xmt=...`
  - Reddit: `?share_id=...&utm_medium=...`
  - YouTube: `?si=...&pp=...`（保留 `v=` 與 `t=`）
  - Amazon: `/ref=...`、`/?th=1&psc=1&tag=...`
  - X/Twitter: `?s=20&t=...`
  - Google search `/url?q=<真正的 URL>`
  - TikTok: `?_r=1&_t=...`
  - LinkedIn: `?trk=...`
  - Spotify: `?si=...`
  - globalRules: `utm_source`, `utm_medium`, `gclid`, `mc_cid`, `_hsenc`
  - Exceptions：某些合法參數不被誤刪
  - Complete provider：已知短網址追蹤器直接回傳空
  - 無追蹤碼時原樣返回
- `UrlExtractorTest`：「看這個 https://… 很棒」→ 抽出 URL。

**手動驗證**（實作完成後）：
- 用 Android Studio emulator 或實機，從 Chrome、Instagram app、Threads app 分享連結 → 確認 Toast 出現、剪貼簿內容、重新分享流程。
- 設定畫面按「立即同步規則表」→ 確認版本字串更新、離線時 Snackbar 顯示錯誤。
- 歷史紀錄開/關切換 → 確認開關後的行為符合預期。

## 實作順序（approx commits）

1. 建立 Gradle 專案骨架、`.gitignore`、README、LICENSE、NOTICE、PLAN.md 副本
2. `UrlCleaner` + rules 解析 + 單元測試（先離線跑起來，TDD）
3. 下載 ClearURLs `data.min.json` 放 assets，跑完整測試
4. `MainActivity` + Compose 貼上框 + 清理顯示
5. `ShareHandlerActivity` + intent filter
6. `SettingsScreen` + DataStore
7. `HistoryScreen` + Room
8. 規則更新（OkHttp）
9. GitHub Actions workflow
10. 手動測試 + 截圖 + README 補完

## 需要確認 / 風險

- **剪貼簿寫入提示**：Android 13+ 每次寫剪貼簿系統都會顯示小卡「Copied to clipboard」＋內容預覽，是系統行為、無 permission 可關、每次都會出現。URL 不視為敏感資料，不設 `EXTRA_IS_SENSITIVE`。
- **Toast 策略**：
  - Android 12 以下（無系統小卡）：所有路徑都 Toast（「已清理 N 個參數」/「無追蹤碼」/「整段是追蹤跳轉，未複製」）。
  - Android 13+（系統小卡已告知「已複製」）：預設不 Toast，**僅以下兩種邊界情況 Toast**——
    1. URL 本來就乾淨、無改動（讓使用者知道 app 有運作、不是系統自動複製）。
    2. completeProvider：整段是追蹤跳轉，**我們不寫剪貼簿**，系統小卡不會出現，必須 Toast 告知。
- ClearURLs `data.min.json` 本身已是 minified 格式（官方命名就是 `.min.json`），壓縮後約 200–400 KB，加進 assets 可接受；不重新壓縮也不自行瘦身，保留完整規則涵蓋率。
- 無 Room / 無 KSP，編譯鏈更簡潔。
- 「再觸發一次分享」若使用者從 app A 分享來，選擇再分享可能又出現本 app——要在 Intent.createChooser 時用 `EXTRA_EXCLUDE_COMPONENTS` 把自己排除（API 24+ 支援）。
