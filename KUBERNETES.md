# Kubernetes デプロイメントガイド

このドキュメントでは、Quarkus CRUD アプリケーションの Kubernetes デプロイメントについて説明します。

## 概要

このプロジェクトには、以下の Kubernetes リソースが含まれています：

### アプリケーションコンポーネント

1. **Backend (Quarkus アプリケーション)**
   - Deployment + Service
   - ポート: 8080 (内部)、80 (Service)
   - イメージ: `ghcr.io/yuki-js/quarkus-crud:latest-jvm`

2. **PostgreSQL (データベース)**
   - CloudNativePG Cluster (3インスタンス)
   - 自動フェイルオーバーと高可用性
   - ストレージ: 10Gi

3. **Ingress (外部アクセス)**
   - ホスト: `quarkus-crud.ouchiserver.aokiapp.com`
   - OAuth2 Proxy による認証
   - TLS/HTTPS 対応

4. **Secret (データベース認証情報)**
   - データベースのユーザー名とパスワード

## ワークロード定義規約への準拠

このプロジェクトは、組織のワークロード定義規約に従って構成されています：

### マニフェスト構成

✅ **配置場所**: `manifests/` ディレクトリに配置

✅ **per-file マニフェスト**:
- `backend.yaml` - バックエンドの Deployment と Service
- `postgres-cluster.yaml` - PostgreSQL の CloudNativePG Cluster

✅ **Application-wide マニフェスト**:
- `common-secret.yaml` - データベース認証情報（複数コンポーネントで共有）
- `ingress.yaml` - Ingress 設定

✅ **Kustomize 統合**:
- `kustomization.yaml` で全マニフェストを統合
- `namespace` を利用して Namespace を指定
- `images` を利用してイメージタグを管理
- `labels` を利用して共通ラベルを適用

### Deployment 規約準拠

✅ 必須項目をすべて含む：
- `name`: namespace・kind・name の組み合わせで一意
- `namespace`: `quarkus-crud` を明示的に指定
- `labels.app`: `quarkus-crud`
- `labels.component`: `backend` / `postgres`
- `labels.name`: ワークロード名
- `labels.environment`: `production`
- `replicas`: 1（推奨値）
- `spec.imagePullSecrets`: `github-registry-secret`
- `containers[].name`: ワークロード名と同じ
- `containers[].image`: `ghcr.io` を使用
- `containers[].resources`: 適切なリソース制限を設定
- `containers[].ports[].containerPort`: 8080

### Service 規約準拠

✅ 必須項目をすべて含む：
- `name`: 対応する Deployment と同じ名前
- `namespace`: `quarkus-crud`
- `labels`: Deployment と同じラベル構成
- `spec.type`: `ClusterIP`（推奨値）
- `spec.selector`: Deployment の labels.app, labels.component と一致
- `spec.ports`: targetPort は containerPort と一致

### Ingress 規約準拠

✅ 必須項目をすべて含む：
- `name`: `quarkus-crud`
- `namespace`: `quarkus-crud`
- `labels.environment`: `production`
- `annotations`: nginx-ingress アノテーションを設定
  - `nginx.ingress.kubernetes.io/rewrite-target`: `/`
  - `cert-manager.io/cluster-issuer`: `letsencrypt-cloudflare`
  - `nginx.ingress.kubernetes.io/auth-url`: OAuth2 Proxy 認証 URL
  - `nginx.ingress.kubernetes.io/auth-signin`: OAuth2 Proxy サインイン URL
- `spec.tls`: TLS 設定
  - `hosts`: `*.ouchiserver.aokiapp.com`
  - `secretName`: `service-tls`
- `spec.rules`: ホスト名とパスのルーティング設定

## GitHub Actions ワークフロー

### Jib を使用した Docker イメージビルドとプッシュ

`.github/workflows/publish-jib.yml` では、Jib プラグインを使用して以下を実行します：

✅ ワークフロー機能：
- **JVM ビルド**: Distroless Java 21 ベースイメージを使用
- **ネイティブビルド**: GraalVM を使用したネイティブイメージ
- **デバッグイメージ**: デバッグ用の Eclipse Temurin ベースイメージ
- **マルチタグ**: コミット SHA、latest、セマンティックバージョン対応

### 生成されるイメージタグ

ワークフローは以下のイメージタグを生成します：

1. **すべての push/PR**:
   - `{short-sha}-jvm` - JVM 版（通常）
   - `{short-sha}-jvm-debug` - JVM 版（デバッグ）
   - `{short-sha}-native` - ネイティブ版（通常）
   - `{short-sha}-native-debug` - ネイティブ版（デバッグ）

2. **main ブランチへの push**:
   - `latest-jvm` - JVM 版の最新
   - `latest-native` - ネイティブ版の最新

3. **タグ push (v* パターン)**:
   - `{version}-jvm` - JVM 版のバージョン
   - `{version}-native` - ネイティブ版のバージョン

### トリガー条件

ワークフローは以下の条件で実行されます：

1. **手動実行**: `workflow_dispatch` により任意のタイミングで実行可能
2. **自動実行**: すべての push と pull request で実行

## クイックスタート

### 1. 前提条件

- Kubernetes クラスタへのアクセス
- `kubectl` コマンドのインストール
- GitHub Personal Access Token (read:packages 権限)

### 2. デプロイ手順

```bash
# 1. Namespace を作成
kubectl create namespace quarkus-crud

# 2. ImagePullSecret を作成
kubectl create secret docker-registry github-registry-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --namespace=quarkus-crud

# 3. マニフェストを適用
kubectl apply -k manifests/

# 4. デプロイ状態を確認
kubectl get all -n quarkus-crud
```

### 3. アプリケーションへのアクセス

デプロイが完了したら、以下の URL でアクセスできます：

```
https://quarkus-crud.ouchiserver.aokiapp.com
```

**注意**: OAuth2 Proxy 認証が有効になっているため、アクセス時に認証が必要です。

## カスタマイズ

### ホスト名の変更

`manifests/ingress.yaml` の `spec.rules[].host` を変更してください：

```yaml
spec:
  rules:
    - host: your-custom-hostname.ouchiserver.aokiapp.com
```

### Namespace の変更

`manifests/kustomization.yaml` の `namespace` フィールドを変更してください：

```yaml
namespace: your-custom-namespace
```

### イメージタグの変更

特定のバージョンを使用する場合は、`manifests/kustomization.yaml` の `images` セクションを変更してください：

```yaml
images:
  - name: ghcr.io/yuki-js/quarkus-crud
    newTag: abc1234  # コミット SHA または バージョンタグ
```

### データベースパスワードの変更

**重要**: 本番環境では必ず変更してください！

`manifests/common-secret.yaml` を編集：

```yaml
stringData:
  username: postgres
  password: your-secure-password-here
```

## CloudNativePG の管理

### PostgreSQL クラスターの状態確認

```bash
# Cluster リソースの確認
kubectl get cluster -n quarkus-crud

# Pod の状態確認
kubectl get pods -n quarkus-crud -l cnpg.io/cluster=postgres

# クラスターの詳細情報
kubectl describe cluster postgres -n quarkus-crud
```

### バックアップとリストア

CloudNativePG は組み込みのバックアップ機能を提供しています。詳細は [CloudNativePG ドキュメント](https://cloudnative-pg.io/documentation/current/backup/) を参照してください。

## トラブルシューティング

詳細なトラブルシューティング手順については、`manifests/README.md` を参照してください。

## 参考資料

- [Kubernetes 公式ドキュメント](https://kubernetes.io/docs/home/)
- [Kustomize 公式ドキュメント](https://kustomize.io/)
- [Quarkus 公式ドキュメント](https://quarkus.io/)
