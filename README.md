# ローカル SQS 検証環境

AWS SQS をローカル（[ElasticMQ](https://github.com/softwaremill/elasticmq)）で試すための Docker 構成と、Java（Spring Boot）製の管理 Web アプリ・受信バッチです。

## 構成

| サービス | 説明 | ポート |
|----------|------|--------|
| `elasticmq` | SQS 互換 API | 9324（API）、9325（管理 UI） |
| `sqs-app` | キュー管理・メッセージ送信 Web（profile: `web`） | 8080 |
| `sqs-batch` | メッセージ受信・削除・ファイル保存バッチ（profile: `batch`） | — |

```
ブラウザ ──► sqs-app:8080 ──► ElasticMQ:9324
                │
sqs-batch ──────┘（ポーリング受信）
     │
     └──► ./data/messages/（JSON 保存）
```

## 機能

### Web（`sqs-app`）

- **キュー管理**（`/queues`）
  - 一覧・作成・詳細・編集（属性）・削除
  - 作成時: **標準 / FIFO** を選択
  - 作成時: AWS `CreateQueue` で指定可能な属性を画面から設定
- **メッセージ送信**（`/messages/send`）
  - キュー選択、本文送信（FIFO 時は MessageGroupId 等）

### バッチ（`sqs-batch`）

- **実行時に存在する全キュー**を `ListQueues` で取得し、順に `ReceiveMessage`
- 本文を JSON で `data/messages/{キュー名}/{yyyyMMdd}/{messageId}.json` に保存
- 保存成功後に `DeleteMessage`（失敗時はキューに残す）
- Web で新規作成したキューも、再起動なしで自動的に対象になる

## 前提

- Docker / Docker Compose
- （任意）Maven 3.9+、Java 21 — コンテナ外でビルドする場合

## 起動

```bash
cd /path/to/sqs_test
docker compose up -d --build
```

| URL | 内容 |
|-----|------|
| http://localhost:8080 | 管理 Web |
| http://localhost:8080/queues | キュー一覧 |
| http://localhost:8080/queues/new | キュー作成 |
| http://localhost:8080/messages/send | メッセージ送信 |
| http://localhost:9324 | ElasticMQ SQS API |
| http://localhost:9325 | ElasticMQ 管理 UI |

停止:

```bash
docker compose down
```

ログ:

```bash
docker compose logs -f sqs-app
docker compose logs -f sqs-batch
```

## 動作確認の例

1. ブラウザで http://localhost:8080/queues/new を開き、キュー `test-queue`（標準）を作成
2. http://localhost:8080/messages/send からメッセージを送信
3. `./data/messages/{キュー名}/` に JSON が保存されることを確認（数秒以内）

初期状態では `elasticmq.conf` により `sample-queue` が定義されています。バッチは **すべてのキュー** をポーリングします。

## 環境変数

### 共通（Web / バッチ）

| 変数 | デフォルト | 説明 |
|------|------------|------|
| `AWS_SQS_ENDPOINT` | `http://localhost:9324` | SQS エンドポイント（Compose 内は `http://elasticmq:9324`） |
| `AWS_REGION` | `us-east-1` | リージョン（ローカル用ダミー） |
| `AWS_ACCESS_KEY_ID` | `x` | 認証（ElasticMQ は任意） |
| `AWS_SECRET_ACCESS_KEY` | `x` | 認証 |
| `MESSAGE_STORAGE_DIR` | `./data/messages` | バッチ保存先 |

### Web（`sqs-app`）

| 変数 | デフォルト | 説明 |
|------|------------|------|
| `SPRING_PROFILES_ACTIVE` | `web` | Web モード |

### バッチ（`sqs-batch`）

| 変数 | デフォルト | 説明 |
|------|------------|------|
| `BATCH_POLL_INTERVAL_MS` | `5000` | 全キュー1周後の待機時間（ミリ秒） |
| `BATCH_WAIT_TIME_SECONDS` | `1` | 各キューの `ReceiveMessage` 待機（秒） |

`sqs-admin/src/main/resources/application.yml` の `batch.max-messages`（10）はプロパティファイルで変更できます。

## プロジェクト構成

```
sqs_test/
├── README.md
├── docker-compose.yml      # ElasticMQ + Web + バッチ
├── elasticmq.conf          # 初期キュー定義（sample-queue）
├── data/messages/          # バッチ出力（gitignore）
└── sqs-admin/              # Spring Boot アプリ
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/sqs/
        ├── config/         # SqsClient、設定プロパティ
        ├── service/        # キュー・送信・受信・属性組み立て
        ├── web/            # Thymeleaf コントローラ
        └── batch/          # 受信スケジューラ
```

## キュー作成で設定できる属性

作成画面（`/queues/new`）では AWS [CreateQueue](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_CreateQueue.html) の属性に対応する項目を設定できます。

| 分類 | 属性 |
|------|------|
| 基本 | キュー名、**標準 / FIFO** |
| メッセージ | `DelaySeconds`, `MaximumMessageSize`, `MessageRetentionPeriod`, `ReceiveMessageWaitTimeSeconds`, `VisibilityTimeout` |
| FIFO | `ContentBasedDeduplication`, `DeduplicationScope`, `FifoThroughputLimit` |
| DLQ | `RedrivePolicy`（DLQ ARN / 既存キュー名、`maxReceiveCount`） |
| DLQ 許可 | `RedriveAllowPolicy` |
| 暗号化 | SQS マネージド SSE / KMS（`KmsMasterKeyId`, `KmsDataKeyReusePeriodSeconds`） |
| その他 | `Policy`, `tags` |

編集画面では作成後に変更可能な属性のみ更新します。キュー名や FIFO/標準の種別は、SQS の仕様上作成後に変更できません。

## ローカル開発（Docker を使わない場合）

### IDE（Cursor / VS Code）

Java 拡張用の設定は `.vscode/settings.json`（共通）と `.vscode/settings.local.json`（個人の JDK / Maven パス）に分けています。詳細は [.vscode/README.md](.vscode/README.md) を参照してください。

### アプリ起動

ElasticMQ を別途起動したうえで:

```bash
cd sqs-admin
mvn spring-boot:run -Dspring-boot.run.profiles=web \
  -Dspring-boot.run.arguments=--aws.sqs.endpoint=http://localhost:9324
```

バッチのみ:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=batch \
  -Dspring-boot.run.arguments=--aws.sqs.endpoint=http://localhost:9324
```

## AWS CLI での確認（任意）

```bash
aws --endpoint-url http://localhost:9324 sqs list-queues

aws --endpoint-url http://localhost:9324 sqs send-message \
  --queue-url http://localhost:9324/000000000000/sample-queue \
  --message-body "hello"
```

## 注意事項

- **ElasticMQ の制限**: 本番 AWS SQS と完全同一ではありません。KMS、一部 DLQ/タグ操作などはローカルで失敗することがあります。
- **DLQ ARN**: ローカルでは `arn:aws:sqs:{region}:000000000000:{キュー名}` 形式で自動生成します（`AWS_REGION` を使用）。
- **保存ファイル**: `data/messages/` は `.gitignore` 対象です。

## 技術スタック

- Java 21 / Spring Boot 3.4
- AWS SDK for Java v2（`sqs`）
- Thymeleaf / Bootstrap 5
- ElasticMQ Native（Docker）
- Maven（マルチステージ Docker ビルド）
