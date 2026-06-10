# ローカル SQS 検証環境

AWS SQS をローカル（[ElasticMQ](https://github.com/softwaremill/elasticmq)）で試すための Docker 構成と、Java（Spring Boot）製の管理 Web アプリ・受信バッチ・**負荷試験バッチ**です。

## 構成

| サービス | 説明 | ポート |
|----------|------|--------|
| `elasticmq` | SQS 互換 API | 9324（API）、9325（管理 UI） |
| `sqs-app` | キュー管理・メッセージ送信 Web（profile: `web`） | 8080 |
| `sqs-batch` | メッセージ受信・削除・ファイル保存バッチ（profile: `batch`） | — |
| `sqs-send-load` | 負荷試験用送信バッチ（profile: `send-load`、Compose profile: `load-test`） | — |
| `sqs-receive-load` | 負荷試験用受信バッチ（profile: `receive-load`、Compose profile: `load-test`） | — |

```
ブラウザ ──► sqs-app:8080 ──► ElasticMQ:9324
                │
sqs-batch ──────┘（常駐・全キューポーリング）
     │
     └──► ./data/messages/（JSON 保存）

負荷試験（docker compose --profile load-test run --rm）:
  sqs-send-load    ──► 指定キューへランダム本文を送信 ──► ElasticMQ
  sqs-receive-load ◄── 指定キューから受信・保存・削除 ◄── ElasticMQ
                          └──► ./data/messages/
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

### 負荷試験バッチ（`sqs-send-load` / `sqs-receive-load`）

既存の常駐バッチ（`sqs-batch`）とは **別コンテナ・別 Spring Profile** で動作するワンショット用バッチです。  
`docker compose up` では起動せず、`docker compose --profile load-test run --rm` で都度実行します。

#### 3 種類のバッチ比較

| | `sqs-batch` | `sqs-send-load` | `sqs-receive-load` |
|--|-------------|-----------------|---------------------|
| Profile | `batch` | `send-load` | `receive-load` |
| 起動 | `docker compose up` | `run --profile load-test` | `run --profile load-test` |
| 対象キュー | 全キュー | `--queue-name` で指定 | `--queue-name` で指定 |
| 終了条件 | 常駐（手動停止） | `--total-count` 送信完了 | 各スレッドが空受信で終了 |
| スレッド | 1（順次） | `--threads` で並列 | `--threads` で並列 |
| 主な用途 | 開発中の自動受信 | 送信性能計測 | 受信性能計測 |

#### 送信（`send-load`）

- 指定キューへ **印字可能 ASCII のランダム本文** を送信（`--message-length` バイト）
- 合計件数（`--total-count`）をスレッド数で分担（例: 1000 件 / 5 スレッド → 各 200 件）
- FIFO キューはスレッドごとに `MessageGroupId=thread-{n}` を付与
- キュー名は FIFO でも `.fifo` 省略可（例: `fifo1` → `fifo1.fifo`）
- 終了時にスループット・所要時間等の **レポートをログ出力**

#### 受信（`receive-load`）

- 指定キューから `ReceiveMessage`（`waitTimeSeconds=0`、ロングポーリングなし）で受信
- 保存成功後に削除（単体 API またはバッチ API）
- **件数指定なし**（`--total-count` は不可）
- 各スレッドは **メッセージ 0 件を受信した時点でそのスレッドのみ終了**（他スレッドの処理中メッセージは継続）
- 全スレッドが終了したらバッチ完了
- 保存先:
  - 標準キュー: `data/messages/{キュー名}/{yyyyMMdd}/{messageId}.json`
  - FIFO キュー: `data/messages/{キュー名}/{MessageGroupId}/{yyyyMMdd}/{messageId}.json`

#### CLI 引数

| 引数 | 送信 | 受信 | 説明 |
|------|:----:|:----:|------|
| `--queue-name` | 必須 | 必須 | キュー名（FIFO は `.fifo` 省略可） |
| `--threads` | 必須 | 必須 | 並列スレッド数 |
| `--total-count` | 必須 | 不可 | 合計送信件数 |
| `--message-length` | 必須 | — | 本文バイト長 |
| `--batch-size` | 省略可 | 省略可 | 1〜10。省略時は単体 API、明示時はバッチ API |

**`--batch-size` と SQS API の対応**

| 操作 | 省略時 | `--batch-size=N` 明示時 |
|------|--------|-------------------------|
| 送信 | `SendMessage` | `SendMessageBatch`（最大 N 件/回） |
| 受信 | `ReceiveMessage`（1 件/回） | `ReceiveMessage`（最大 N 件/回） |
| 削除 | `DeleteMessage` | `DeleteMessageBatch` |

#### ビルド

コード変更後はイメージを再ビルドしてから実行してください。

```bash
# 負荷試験用イメージのみ
docker compose --profile load-test build sqs-send-load sqs-receive-load

# 常駐バッチも含め同一 JAR を再ビルド
docker compose build sqs-batch
docker compose --profile load-test build
```

#### 実行例

```bash
# --- 送信: 単体 API（batch-size 省略）---
docker compose --profile load-test run --rm sqs-send-load \
  --queue-name=fifo1.fifo \
  --threads=1 \
  --total-count=1000 \
  --message-length=256

# --- 送信: バッチ API ---
docker compose --profile load-test run --rm sqs-send-load \
  --queue-name=sample-queue \
  --threads=5 \
  --total-count=1000 \
  --message-length=256 \
  --batch-size=10

# --- 受信: キューが空になるまで（単体 API）---
docker compose --profile load-test run --rm sqs-receive-load \
  --queue-name=fifo1.fifo \
  --threads=5

# --- 受信: バッチ API ---
docker compose --profile load-test run --rm sqs-receive-load \
  --queue-name=fifo1.fifo \
  --threads=5 \
  --batch-size=10
```

#### 送信 → 受信の流れ（例）

```bash
# 1. 1000 件送信
docker compose --profile load-test run --rm sqs-send-load \
  --queue-name=fifo1.fifo --threads=1 --total-count=1000 --message-length=256

# 2. 受信・保存・削除
docker compose --profile load-test run --rm sqs-receive-load \
  --queue-name=fifo1.fifo --threads=5
```

#### レポート出力（ログ）

```
========== Send Load Test Report ==========
Queue:             fifo1.fifo
Threads:           1
Requested total:   1000
Processed total:   1000
Batch size:        omitted (single API)
Message length:    256 bytes
Duration:          11.23 s
Throughput:        89.05 msg/s
Errors:            0
  thread-0: processed=1000 errors=0
==========================================
```

受信時は `Stop condition: until each thread receives empty` と各スレッドの処理件数が出力されます。

#### ローカル実行（Docker を使わない場合）

ElasticMQ を起動済みであること。

```bash
cd sqs-admin

# 送信
mvn spring-boot:run -Dspring-boot.run.profiles=send-load \
  -Dspring-boot.run.arguments="--queue-name=sample-queue --threads=5 --total-count=100 --message-length=256 --aws.sqs.endpoint=http://localhost:9324"

# 受信
mvn spring-boot:run -Dspring-boot.run.profiles=receive-load \
  -Dspring-boot.run.arguments="--queue-name=sample-queue --threads=5 --aws.sqs.endpoint=http://localhost:9324"
```

#### 実装

| Profile | クラス |
|---------|--------|
| `send-load` | `batch/load/SendLoadRunner`, `SendLoadService` |
| `receive-load` | `batch/load/ReceiveLoadRunner`, `ReceiveLoadService` |

設定ファイル: `application-send-load.yml`, `application-receive-load.yml`（いずれも Web サーバーなし）

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
├── docker-compose.yml      # ElasticMQ + Web + 常駐バッチ + 負荷試験（load-test profile）
├── elasticmq.conf          # 初期キュー定義（sample-queue）
├── data/messages/          # バッチ出力（gitignore）
└── sqs-admin/              # Spring Boot アプリ
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/sqs/
        ├── config/         # SqsClient、設定プロパティ
        ├── service/        # キュー・送信・受信・属性組み立て
        ├── web/            # Thymeleaf コントローラ
        └── batch/
            ├── MessageReceiveScheduler.java   # 常駐受信（profile: batch）
            └── load/                          # 負荷試験（send-load / receive-load）
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
- **負荷試験の本文**: ランダム本文は印字可能 ASCII のみ（ElasticMQ の XML 制約回避）。バイナリデータの送信試験には未対応です。
- **FIFO キュー名**: Web UI で `fifo1` と作成した場合、実際のキュー名は `fifo1.fifo`。負荷試験バッチは `.fifo` 省略可。
- **DLQ ARN**: ローカルでは `arn:aws:sqs:{region}:000000000000:{キュー名}` 形式で自動生成します（`AWS_REGION` を使用）。
- **保存ファイル**: `data/messages/` は `.gitignore` 対象です。

## 技術スタック

- Java 21 / Spring Boot 3.4
- AWS SDK for Java v2（`sqs`）
- Thymeleaf / Bootstrap 5
- ElasticMQ Native（Docker）
- Maven（マルチステージ Docker ビルド）
