# GitHub Actions Optimization Handover

## 概要 / Summary

GitHub Actionsのワークフロー実行時間を削減するための最適化を実施しました。
**最も重要な最適化**: 同一ソースコードに対する重複したGradle実行を排除しました。

Most important optimization: Eliminated redundant Gradle executions on the same source code target.

## 🚨 Critical Finding: Redundant Gradle Executions

### 問題の本質 / Core Problem

以前のワークフローでは、**同じソースコードに対して複数回Gradleタスクを実行**していました:

**CI Workflow (Before):**
```yaml
- run: ./gradlew generateOpenApiModels  # Compilation #1
- run: ./gradlew spotlessCheck          # Compilation #2 (implicit)
- run: ./gradlew checkstyleMain...      # Compilation #3 (implicit)
- run: ./gradlew build                  # Compilation #4 + Tests
```

**問題点:**
- `build`タスクは既に`generateOpenApiModels`を含む
- `spotlessCheck`と`checkstyleMain`は暗黙的にコンパイルを実行
- **結果**: 同じコードを4回コンパイル・ビルド

**Publish-Jib Workflow (Before):**
```yaml
- run: ./gradlew build --no-daemon           # Build #1
- run: ./gradlew jib --no-daemon (4 times!)  # Build #2-5
```

**問題点:**
- `--no-daemon`フラグにより、各Gradle実行が独立したプロセス
- Gradle daemonなしでは、ビルドキャッシュが効かない
- **結果**: 実質的に5回ビルド

## 実施した最適化 / Optimizations Implemented

### 1. CI Workflow: Single Gradle Invocation

**Before (4 separate Gradle executions):**
```yaml
- run: ./gradlew generateOpenApiModels
- run: ./gradlew spotlessCheck
- run: ./gradlew checkstyleMain checkstyleTest
- run: ./gradlew build
```

**After (1 Gradle execution with multiple tasks):**
```yaml
- run: ./gradlew build spotlessCheck checkstyleMain checkstyleTest --no-daemon
```

**効果 / Impact:**
- Gradle起動オーバーヘッド: 4回 → 1回
- コンパイル回数: 4回 → 1回
- 依存性解決: 4回 → 1回
- **予想削減時間**: ~40-60秒

### 2. Publish-Jib: Gradle Daemon Enabled

**Before (daemon disabled, no incremental builds):**
```yaml
- run: ./gradlew build --no-daemon
- run: ./gradlew jib --no-daemon  # Rebuilds everything
- run: ./gradlew jib --no-daemon  # Rebuilds everything
- run: ./gradlew jib --no-daemon  # Rebuilds everything
- run: ./gradlew jib --no-daemon  # Rebuilds everything
```

**After (daemon enabled, reuses build artifacts):**
```yaml
- run: ./gradlew build           # Build once
- run: ./gradlew jib            # Reuses artifacts
- run: ./gradlew jib            # Reuses artifacts
- run: ./gradlew jib            # Reuses artifacts
- run: ./gradlew jib            # Reuses artifacts
```

**効果 / Impact:**
- Gradle daemon起動: 5回 → 1回
- フルビルド: 5回 → 1回
- Jib実行: ビルド済みartifactsを再利用
- **予想削減時間**: ~3-4分 (JVM), ~15-20分 (Native)

### 3. Parallel Job Execution

**Before:**
```yaml
jobs:
  openapi-validation: ...
  build:
    needs: openapi-validation  # Sequential
```

**After:**
```yaml
jobs:
  openapi-validation: ...
  build: ...  # Parallel
```

**効果 / Impact:** ~30-40秒削減

### 4. Conservative Caching Strategy

Only cache stable dependencies:
- ✅ Java SDK (`cache: gradle` in actions/setup-java)
- ✅ Gradle wrapper (gradle/actions/setup-gradle)
- ❌ **Not cached**: Gradle build cache, GraalVM artifacts

## Gradle Task Dependencies (参考)

Gradleのタスク依存関係を理解することが重要:

```
build
  └─ test
      └─ compileTestJava
          └─ compileJava
              └─ generateOpenApiModels (OpenAPI plugin)
```

つまり、`./gradlew build`を実行すると:
1. `generateOpenApiModels` (自動実行)
2. `compileJava` (自動実行)
3. `compileTestJava` (自動実行)
4. `test` (自動実行)

**結論**: `build`の前に個別にこれらを実行する必要はない

## --no-daemon フラグの使い分け / When to Use --no-daemon

### 使うべき場合 / Use --no-daemon when:
- ❌ **Short-lived builds**: Daemon起動オーバーヘッドの方が大きい
- ❌ **Single Gradle execution**: Daemonのメリットがない

### 使わないべき場合 / Do NOT use --no-daemon when:
- ✅ **Multiple Gradle executions**: Daemon間でキャッシュ共有
- ✅ **Incremental builds**: Up-to-date checkが効く
- ✅ **Long-running builds**: 起動オーバーヘッドが償却される

**CI Workflow**: `--no-daemon`使用 (1回だけの実行)
**Publish Workflow**: `--no-daemon`不使用 (複数回実行でdaemonのメリット大)

## 期待される効果 / Expected Impact

### CI Workflow
- **Before**: ~2-3分
- **After**: ~1.5-2分
- **削減**: ~30-40秒 (20-25%)

### Publish-Jib JVM Workflow
- **Before**: ~5-6分
- **After**: ~2-3分
- **削減**: ~3分 (50%)

### Publish-Jib Native Workflow
- **Before**: ~20-25分
- **After**: ~5-8分
- **削減**: ~15分 (60-70%)

## 重要な教訓 / Key Lessons Learned

### 1. Gradleのタスク依存関係を理解する
- 暗黙的な依存関係を見逃さない
- `build`は既に多くのタスクを含む

### 2. Gradle Daemonを正しく使う
- 複数実行時はdaemonを有効化
- 単発実行時は無効化

### 3. 同じソースコードに対する重複実行を排除
- 各Gradle実行がコストが高い
- タスクを統合して1回の実行にまとめる

### 4. キャッシュは保守的に
- 壊れやすいキャッシュは避ける
- 安定したものだけキャッシュ

## ファイル変更履歴 / File Changes

### Modified Files:
- `.github/workflows/ci.yml` - Single Gradle invocation, parallel execution
- `.github/workflows/publish-jib.yml` - Gradle daemon enabled, minimal invocations

### NOT Modified (Important):
- `gradle.properties` - No changes (affects local development)
- `settings.gradle` - No changes (affects local development)

## 検証方法 / Verification

```bash
# Local verification of Gradle tasks
./gradlew build spotlessCheck checkstyleMain checkstyleTest --dry-run

# Should show task dependency graph and prove no redundancy
```

## 今後の改善案 / Future Improvements

1. **Gradle Configuration Cache** - 一度安定したら有効化を検討
2. **Test Parallelization** - Gradleの`--parallel`フラグ (要検証)
3. **Selective Testing** - 変更されたモジュールのみテスト

## 連絡先 / Contact

この最適化に関する質問は、このPRのコメントでお願いします。
