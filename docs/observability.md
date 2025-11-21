# Observability ガイド

このドキュメントでは、quarkus-crud アプリケーションにおける可観測性（Observability）の実装と運用について説明します。

## 概要

本アプリケーションでは、以下の3つの柱（Three Pillars of Observability）を実装しています：

1. **ログ（Logs）** - JSON 構造化ログ with トレース ID 相関
2. **メトリクス（Metrics）** - Micrometer + Prometheus メトリクス収集
3. **トレース（Traces）** - OpenTelemetry 分散トレーシング

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                     Quarkus Application                      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Logging    │  │   Metrics    │  │   Tracing    │     │
│  │  (JSON Log)  │  │ (Micrometer) │  │ (OpenTelemetry)    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                  │              │
└─────────┼─────────────────┼──────────────────┼──────────────┘
          │                 │                  │
          ▼                 ▼                  ▼
     stdout/stderr    /q/metrics      OpenTelemetry
                           │           Collector (OTLP)
                           │                  │
                           ▼                  ▼
                      Prometheus         Jaeger/Tempo
                           │                  │
                           └──────────┬───────┘
                                      │
                                      ▼
                                  Grafana
```

## 1. ログ（Logs）

### 設定

`application.properties`:
```properties
# JSON 構造化ログを有効化
quarkus.log.console.json=true
quarkus.log.console.json.pretty-print=false

# 追加フィールドの設定
quarkus.log.console.json.additional-field."service.name".value=quarkus-crud
quarkus.log.console.json.additional-field."service.version".value=0.0.1

# ログレベルの設定
quarkus.log.level=INFO
quarkus.log.category."app.aoki.quarkuscrud".level=INFO
```

### JSON ログの構造

OpenTelemetry と統合することで、自動的に以下のフィールドがログに含まれます：

```json
{
  "timestamp": "2024-01-01T12:00:00.000Z",
  "sequence": 1,
  "loggerClassName": "org.jboss.logging.Logger",
  "loggerName": "app.aoki.quarkuscrud.service.UserService",
  "level": "INFO",
  "message": "User created successfully",
  "threadName": "executor-thread-1",
  "threadId": 42,
  "mdc": {},
  "trace_id": "4bf92f3577b34da6a3ce929d0e0e4736",
  "span_id": "00f067aa0ba902b7",
  "trace_flags": "01",
  "service.name": "quarkus-crud",
  "service.version": "0.0.1"
}
```

### ログとトレースの相関

OpenTelemetry を有効にすると、`trace_id` と `span_id` が自動的にログに追加されます。これにより、Grafana などで以下が可能になります：

- トレースからログへのジャンプ
- ログからトレースへのジャンプ
- 特定のリクエストに関連するすべてのログの検索

### 開発環境での確認

```bash
# Dev モードで起動
./gradlew quarkusDev

# ログを確認（JSON フォーマット）
# リクエストを送信すると、JSON 形式でログが出力される
```

## 2. メトリクス（Metrics）

### 設定

`application.properties`:
```properties
# Micrometer + Prometheus を有効化
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/q/metrics

# 各種バインダーを有効化
quarkus.micrometer.binder.http-server.enabled=true
quarkus.micrometer.binder.http-client.enabled=true
quarkus.micrometer.binder.vertx.enabled=true
```

### 公開されるメトリクス

メトリクスは `/q/metrics` エンドポイントで Prometheus 形式で公開されます。

#### HTTP メトリクス
- `http_server_requests_seconds` - HTTP リクエストの処理時間（ヒストグラム）
  - Tags: `method`, `uri`, `status`, `outcome`
- `http_server_active_requests` - アクティブなリクエスト数

#### JVM メトリクス
- `jvm_memory_used_bytes` - JVM メモリ使用量
- `jvm_gc_pause_seconds` - GC 停止時間
- `jvm_threads_live` - アクティブなスレッド数
- `jvm_classes_loaded` - ロード済みクラス数

#### システムメトリクス
- `system_cpu_usage` - システム CPU 使用率
- `process_cpu_usage` - プロセス CPU 使用率
- `process_uptime_seconds` - プロセス稼働時間

### Prometheus によるスクレイピング

Kubernetes マニフェスト（`manifests/backend.yaml`）の Service に以下の注釈が追加されています：

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/q/metrics"
    prometheus.io/scheme: "http"
```

Prometheus はこの注釈を検出し、自動的にメトリクスを収集します。

### カスタムメトリクスの追加

アプリケーションコードでカスタムメトリクスを追加する場合：

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import jakarta.inject.Inject;

public class UserService {
    
    @Inject
    MeterRegistry registry;
    
    public void createUser(User user) {
        // カウンターの例
        Counter.builder("users.created")
            .description("Number of users created")
            .tag("type", user.getType())
            .register(registry)
            .increment();
        
        // タイマーの例
        registry.timer("users.creation.time")
            .record(() -> {
                // 処理
            });
    }
}
```

## 3. トレース（Traces）

### 設定

`application.properties`:
```properties
# OpenTelemetry を有効化
quarkus.otel.sdk.disabled=false
quarkus.otel.traces.enabled=true

# OTLP exporter の設定
quarkus.otel.exporter.otlp.traces.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
quarkus.otel.exporter.otlp.traces.protocol=grpc

# サンプリング設定
quarkus.otel.traces.sampler=parentbased_traceidratio
quarkus.otel.traces.sampler.arg=${OTEL_TRACES_SAMPLER_ARG:0.1}

# リソース属性
quarkus.otel.resource.attributes=service.name=quarkus-crud,service.version=0.0.1,deployment.environment=${DEPLOYMENT_ENVIRONMENT:development}

# プロパゲーション
quarkus.otel.propagators=tracecontext,baggage
```

### OpenTelemetry Collector

トレースデータは OpenTelemetry Collector（`manifests/otel-collector.yaml`）を経由して収集されます。

#### Collector の役割
- トレースデータの受信（OTLP プロトコル）
- バッチ処理によるネットワーク効率の向上
- バックエンドへのエクスポート（Jaeger、Tempo など）

#### Collector の設定

本番環境では、`manifests/otel-collector.yaml` の ConfigMap を編集して、バックエンドへのエクスポート設定を追加します：

```yaml
exporters:
  otlp:
    endpoint: "jaeger-collector:4317"
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [logging, otlp]  # otlp を追加
```

### サンプリング

本番環境では、トレースのサンプリング率を調整してオーバーヘッドを抑えます：

- **開発環境**: `OTEL_TRACES_SAMPLER_ARG=1.0` （100% サンプリング）
- **本番環境**: `OTEL_TRACES_SAMPLER_ARG=0.1` （10% サンプリング）

環境変数で動的に変更可能です。

### 手動スパンの作成

必要に応じて、カスタムスパンを作成できます：

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.inject.Inject;

public class UserService {
    
    @Inject
    Tracer tracer;
    
    public void processUser(User user) {
        Span span = tracer.spanBuilder("user.processing")
            .startSpan();
        
        try {
            span.setAttribute("user.id", user.getId());
            span.setAttribute("user.type", user.getType());
            
            // 処理
            
        } finally {
            span.end();
        }
    }
}
```

## 4. Grafana ダッシュボード

### 推奨メトリクス

以下のメトリクスを Grafana でダッシュボード化することを推奨します：

#### リクエストメトリクス
- **リクエストレート**: `rate(http_server_requests_seconds_count[5m])`
- **エラー率**: `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`
- **レイテンシ（p95）**: `histogram_quantile(0.95, http_server_requests_seconds_bucket)`
- **レイテンシ（p99）**: `histogram_quantile(0.99, http_server_requests_seconds_bucket)`

#### JVM メトリクス
- **Heap 使用量**: `jvm_memory_used_bytes{area="heap"}`
- **GC 停止時間**: `rate(jvm_gc_pause_seconds_sum[5m])`
- **スレッド数**: `jvm_threads_live`

#### システムメトリクス
- **CPU 使用率**: `process_cpu_usage`
- **稼働時間**: `process_uptime_seconds`

### アラート設定

以下のアラートルールを設定することを推奨します：

```yaml
groups:
  - name: quarkus-crud-alerts
    interval: 30s
    rules:
      # エラー率が高い
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors/sec"
      
      # レイテンシが高い
      - alert: HighLatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P95 latency is {{ $value }} seconds"
      
      # Heap 使用率が高い
      - alert: HighHeapUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High heap usage detected"
          description: "Heap usage is {{ $value | humanizePercentage }}"
```

## 5. 開発環境と本番環境の違い

### JVM モード vs ネイティブモード

| 機能 | JVM モード | ネイティブモード |
|------|-----------|----------------|
| JSON ログ | ✅ 完全サポート | ✅ 完全サポート |
| Micrometer メトリクス | ✅ 完全サポート | ✅ 完全サポート |
| OpenTelemetry | ✅ 完全サポート | ✅ 完全サポート |
| JFR プロファイリング | ✅ 利用可能 | ❌ 利用不可 |
| async-profiler | ✅ 利用可能 | ⚠️ 制限あり |

### プロファイリング

#### JVM モード
- **JFR（Java Flight Recorder）** が利用可能
- **async-profiler** が利用可能
- オンデマンドでプロファイリングデータを取得できる

```bash
# JFR の使用例
jcmd <pid> JFR.start name=profile settings=profile duration=60s filename=profile.jfr
jcmd <pid> JFR.stop name=profile
```

#### ネイティブモード
- JFR は利用できない
- プロファイリングには以下のオプションがある：
  - **Parca** - 継続的プロファイリング（eBPF ベース）
  - **eBPF ツール** - システムレベルのプロファイリング
  - **サイドカープロファイラー** - 専用のプロファイリングコンテナ

## 6. トラブルシューティング

### ログが JSON 形式で出力されない

**原因**: `quarkus-logging-json` 依存関係が追加されていない

**解決方法**:
```gradle
implementation 'io.quarkus:quarkus-logging-json'
```

### メトリクスが `/q/metrics` で取得できない

**原因**: Micrometer Prometheus の依存関係が追加されていない

**解決方法**:
```gradle
implementation 'io.quarkus:quarkus-micrometer-registry-prometheus'
```

### トレースが Collector に送信されない

**原因**: 
1. Collector が起動していない
2. エンドポイント設定が間違っている

**解決方法**:
```bash
# Collector のステータス確認
kubectl get pods -n quarkus-crud | grep otel-collector

# ログで接続エラーを確認
kubectl logs -n quarkus-crud deployment/backend | grep "Failed to export"

# 環境変数の確認
kubectl describe pod -n quarkus-crud <pod-name> | grep OTEL_EXPORTER
```

### ログに trace_id が含まれない

**原因**: OpenTelemetry が有効になっていない、またはサンプリングされていない

**解決方法**:
1. `quarkus.otel.traces.enabled=true` が設定されているか確認
2. サンプリング率を上げる: `OTEL_TRACES_SAMPLER_ARG=1.0`

## 7. 参考資料

- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [Quarkus Micrometer Guide](https://quarkus.io/guides/micrometer)
- [Quarkus Logging JSON Guide](https://quarkus.io/guides/logging#json-logging)
- [OpenTelemetry Specification](https://opentelemetry.io/docs/specs/otel/)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)

## 8. 今後の拡張

### イベントストリーミング
将来的に Kafka などのメッセージングシステムを導入する場合、以下の対応が必要：

1. CloudEvents 形式でイベントを出力
2. Kafka トピックのメトリクスを収集
3. メッセージトレーシングの実装

### カスタムメトリクス
ビジネスロジックに応じて、以下のカスタムメトリクスを追加：

- CRUD 操作のカウント（create, read, update, delete）
- データベースクエリのレイテンシ
- ユーザー認証の成功/失敗率
- イベント参加者数の推移

### サービスレベル目標（SLO）
可観測性データを基に SLO を定義：

- **可用性**: 99.9% （月間ダウンタイム < 43.2 分）
- **レイテンシ**: P95 < 500ms
- **エラー率**: < 0.1%
