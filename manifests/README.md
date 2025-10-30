# Kubernetes Manifests

このディレクトリには、Quarkus CRUD アプリケーションを Kubernetes にデプロイするためのマニフェストファイルが含まれています。

## 構成

### ファイル一覧

- `backend.yaml` - バックエンドアプリケーションの Deployment と Service
- `postgres-cluster.yaml` - CloudNativePG による PostgreSQL クラスター定義
- `common-secret.yaml` - データベース認証情報を含む Secret
- `ingress.yaml` - 外部アクセス用の Ingress 設定
- `kustomization.yaml` - Kustomize 設定ファイル

### CloudNativePG について

このプロジェクトでは、PostgreSQL データベースの管理に [CloudNativePG](https://cloudnative-pg.io/) オペレーターを使用しています。CloudNativePG は以下の機能を提供します：

- 自動フェイルオーバーと高可用性
- 自動バックアップとリカバリー
- ローリングアップデート
- 接続プーリング (PgBouncer)

**前提条件**: CloudNativePG オペレーターがクラスターにインストールされている必要があります。

インストール方法:
```bash
kubectl apply -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/release-1.23/releases/cnpg-1.23.0.yaml
```

## デプロイ前の準備

### 1. 設定の確認と変更

以下の設定を環境に合わせて変更してください：

#### Namespace (すべてのファイル)
デフォルト: `quarkus-crud`

変更する場合は、`kustomization.yaml` の `namespace` フィールドを編集してください。

#### Ingress (ingress.yaml)
- **ホスト名**: デフォルトは `quarkus-crud.ouchiserver.aokiapp.com`
- **認証**: OAuth2 Proxy による認証が有効になっています
- **TLS**: Let's Encrypt による自動証明書発行が設定されています

#### データベース認証情報 (common-secret.yaml)
**重要**: 本番環境では必ずパスワードを変更してください！

```yaml
stringData:
  username: postgres
  password: postgres  # ← これを変更してください
```

#### Docker イメージ (kustomization.yaml)
デフォルト: `ghcr.io/yuki-js/quarkus-crud:latest-jvm`

特定のバージョンを使用する場合は、`kustomization.yaml` の `images` セクションで `newTag` を変更してください。

**注意**: GitHub Actions ワークフローは以下のイメージタグを生成します：
- `latest-jvm` - main ブランチの最新 JVM ビルド
- `latest-native` - main ブランチの最新ネイティブビルド
- `{short-sha}-jvm` - 特定コミットの JVM ビルド
- `{short-sha}-native` - 特定コミットのネイティブビルド

### 2. ImagePullSecret の作成

GitHub Container Registry からイメージをプルするため、Secret を作成する必要があります：

```bash
kubectl create secret docker-registry github-registry-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --namespace=quarkus-crud
```

**注意**: GitHub Personal Access Token には `read:packages` 権限が必要です。

## デプロイ方法

### Kustomize を使用したデプロイ

```bash
# 名前空間を作成
kubectl create namespace quarkus-crud

# ImagePullSecret を作成（上記参照）
kubectl create secret docker-registry github-registry-secret \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --namespace=quarkus-crud

# マニフェストを適用
kubectl apply -k manifests/
```

### デプロイ内容の確認

```bash
# 適用される内容をプレビュー
kubectl kustomize manifests/

# または kubectl diff を使用（既にデプロイされている場合）
kubectl diff -k manifests/
```

## デプロイ後の確認

```bash
# Pod の状態を確認
kubectl get pods -n quarkus-crud

# Service の状態を確認
kubectl get svc -n quarkus-crud

# Ingress の状態を確認
kubectl get ingress -n quarkus-crud

# ログを確認
kubectl logs -n quarkus-crud -l component=backend --tail=100 -f
```

## トラブルシューティング

### Pod が ImagePullBackOff になる場合

1. ImagePullSecret が正しく作成されているか確認
```bash
kubectl get secret github-registry-secret -n quarkus-crud
```

2. Secret の内容が正しいか確認
```bash
kubectl get secret github-registry-secret -n quarkus-crud -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d
```

### データベース接続エラーの場合

1. PostgreSQL Pod が起動しているか確認
```bash
kubectl get pods -n quarkus-crud -l component=postgres
```

2. Secret が正しく設定されているか確認
```bash
kubectl get secret database-secret -n quarkus-crud -o yaml
```

3. Service の接続を確認
```bash
kubectl get svc postgres -n quarkus-crud
```

## 削除方法

```bash
# すべてのリソースを削除
kubectl delete -k manifests/

# 名前空間も削除する場合
kubectl delete namespace quarkus-crud
```

## 本番環境での推奨設定

1. **Secret の管理**: 
   - `common-secret.yaml` をリポジトリから削除し、外部のシークレット管理ツール（Sealed Secrets、External Secrets Operator など）を使用

2. **リソース制限**:
   - 本番負荷に応じて `resources.requests` と `resources.limits` を調整

3. **レプリカ数**:
   - 高可用性が必要な場合は `replicas` を増やす
   - ただし、PostgreSQL は StatefulSet への移行を検討

4. **ストレージ**:
   - PostgreSQL の PVC サイズを実際のデータ量に応じて調整
   - StorageClass の指定を検討

5. **バックアップ**:
   - PostgreSQL の定期的なバックアップを設定
