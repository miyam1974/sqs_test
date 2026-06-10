# VS Code / Cursor ワークスペース設定

## ファイル構成

| ファイル | Git | 用途 |
|----------|-----|------|
| `settings.json` | コミット対象 | チーム共通の設定 |
| `settings.local.json.example` | コミット対象 | ローカル設定のテンプレート |
| `settings.local.json` | **除外** | 各開発者の JDK / Maven パス |

## 初回セットアップ

1. テンプレートをコピーする

   ```bash
   cp .vscode/settings.local.json.example .vscode/settings.local.json
   ```

2. `settings.local.json` のパスを自分の環境に合わせて編集する

3. **Cursor / VS Code のユーザー設定に反映する**

   ワークスペースは `settings.local.json` を自動では読み込みません。
   `settings.local.json` の内容を **ユーザー設定**（`Preferences: Open User Settings (JSON)`）にコピーしてください。

4. ウィンドウを再読み込みする（`Developer: Reload Window`）

## 前提

- Java 21（プロジェクトの `pom.xml` に準拠）
- Maven 3.9+（Docker ビルドのみの場合は不要）
