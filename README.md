# unity_ads_4_12_5

Unity広告SDK（バージョン4.12.5）をAndroid（Kotlin）に実装するためのサンプルプログラムです。

## 📌 主な機能
* バナー広告の表示

## 🛠 使い方
このプロジェクトに含まれる `MainActivity.kt` の内容を、ご自身のプロジェクトに必要に応じてコピーしてご使用ください。

## 🛠 開発環境
* **開発ツール:** Android Studio
* **言語:** Kotlin
* **Unity Ads SDK Version:** 4.12.5

## 📂 主要なファイル
* `MainActivity.kt` : 広告の読み込みと表示をコントロールするメイン処理
* `capture/` : テストモードでの実行画面キャプチャを格納

---

## 🔍 実装のポイントと設定方法

### 1. テストモードの自動判定 (`testMethod`)
`MainActivity.kt` 内にある `testMethod` メソッドは、`BuildConfig.DEBUG` の実行結果と同等の処理を行います。
* **注意点:** コード内の `Class.forName("ご自身のパッケージ名.BuildConfig")` の部分を、必ずご自身のパッケージ名に置き換えてください。
* **戻り値:** `0`（本番環境） / `1`（テスト環境）
* 正しい戻り値が取得できると、`adTestMethod` および `placementId` から分岐した設定値を自動取得できるようになります。

### 2. Unity Game IDの設定 (`adTestMethod`)
`MainActivity.kt` 内の `adTestMethod` メソッドにある `gameId` を書き換えます。
* サンプルコードでは `123456789` になっています。ご自身の **Unity Game ID** に差し替えてください。

### 3. Placement IDの設定 (`placementId`)
`MainActivity.kt` 内の `placementId` メソッドにある ID を書き換えます。
* サンプルコードでは `xxx` になっています。Unityダッシュボードで取得した **Placement ID** に差し替えてください。

### 📊 広告に使用するパラメータ一覧

| パラメータの種類 | 本番モード | テストモード |
| :--- | :--- | :--- |
| **Game ID** | あなたのUnity Game ID | `4550882` |
| **Test Mode** | `false` | `true` |
| **Placement ID** | Unity登録画面で取得したID | `Banner_Android` |

---

## ⚙️ 各種ファイルの配置・修正例

### 📄 AndroidManifest.xml
Android 12以降のシミュレーターや実機で広告IDを正常に読み取るため、以下のパーミッションを `<manifest>` タグ内に追加してください。

```xml
<!-- Android 12以降で広告IDを読み取るために必須 -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

### 📄 libs.version.toml
新規プロジェクトに導入する場合、`coreKtx` のバージョン競合が原因でエラーが発生することがあります。本サンプル（unity-ads:4.12.5）は、**`1.10.1`** で動作確認をしています。

**[場所]** `Gradle Scripts` ＞ `libs.version.toml`

```toml
[versions]
coreKtx = "1.10.1"
# ... その他のバージョン

[libraries]
# ... 既存のライブラリ指定
androidx-ui-text = { group = "androidx.compose.ui", name = "ui-text", version.ref = "androidxComposeUi" }

### 📄 build.gradle.kts (:app)
Unity広告SDKを利用するために、依存関係を追加します。

**[場所]** `Gradle Scripts` ＞ `build.gradle.kts (:app)`

```kotlin
dependencies {
    // ... 他の依存関係
    
    // Unity Ads SDK
    implementation("com.unity3d.ads:unity-ads:4.12.5")
}
```

### 📄 activity_main.xml
バナー広告を表示するためのレイアウト例です。
💡 **Tips:** Unity Adsの広告がうまく表示されない場合、
`layout_height` を通常の `50dp` から **`55dp`** に少し広げることで、
表示の誤差を吸収できる場合があります。
ただし、画面最下部に配置する場合は5dp分の広がりを考慮して位置を調整してください。

```xml
<FrameLayout
    android:id="@+id/adView"
    android:layout_width="320dp"
    android:layout_height="55dp"
    android:layout_gravity="center_horizontal" />
```

---

## 🔗 関連リンク
* **開発者サイト:** (https://nekofes.github.io/)
