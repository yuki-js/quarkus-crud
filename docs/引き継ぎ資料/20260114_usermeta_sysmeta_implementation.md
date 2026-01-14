# usermeta/sysmeta 実装引き継ぎ資料

作成日: 2026-01-14

## 概要

既存の `meta` カラムを `usermeta` と `sysmeta` に分離し、各リソースに `/meta` APIエンドポイントを追加する実装。

## 目的

1. **データベーススキーマの変更**
   - 既存の `meta` カラム → `usermeta` に改名
   - 新しく `sysmeta` カラムを追加（JSONB型）
   - `revision_meta` → `sysmeta` に改名

2. **API実装**
   - 各リソースに `*/meta` エンドポイントを追加
   - GET/PUT で usermeta の読み書きが可能
   - 適切な権限チェック（所有者または関係者のみ）

## 対象テーブル

### meta → usermeta/sysmeta に分割するテーブル
- `users` (meta)
- `events` (meta)
- `event_attendees` (meta)
- `friendships` (meta)

### revision_meta → sysmeta に改名するテーブル
- `user_profiles` (revision_meta)
- `event_user_data` (revision_meta)

### 新たに usermeta/sysmeta を追加するテーブル
- `rooms` (現在metaなし)
- `authn_providers` (現在metaなし)
- `event_invitation_codes` (現在metaなし)

## 実装ステップ

### 1. データベースマイグレーション (V4)

```sql
-- Step 1: Rename meta to usermeta
ALTER TABLE users RENAME COLUMN meta TO usermeta;
ALTER TABLE events RENAME COLUMN meta TO usermeta;
ALTER TABLE event_attendees RENAME COLUMN meta TO usermeta;
ALTER TABLE friendships RENAME COLUMN meta TO usermeta;

-- Step 2: Add sysmeta column
ALTER TABLE users ADD COLUMN sysmeta JSONB;
ALTER TABLE events ADD COLUMN sysmeta JSONB;
ALTER TABLE event_attendees ADD COLUMN sysmeta JSONB;
ALTER TABLE friendships ADD COLUMN sysmeta JSONB;

-- Step 3: Rename revision_meta to sysmeta
ALTER TABLE user_profiles RENAME COLUMN revision_meta TO sysmeta;
ALTER TABLE event_user_data RENAME COLUMN revision_meta TO sysmeta;

-- Step 4: Add usermeta and sysmeta to new tables
ALTER TABLE rooms ADD COLUMN usermeta JSONB;
ALTER TABLE rooms ADD COLUMN sysmeta JSONB;
ALTER TABLE authn_providers ADD COLUMN usermeta JSONB;
ALTER TABLE authn_providers ADD COLUMN sysmeta JSONB;
ALTER TABLE event_invitation_codes ADD COLUMN usermeta JSONB;
ALTER TABLE event_invitation_codes ADD COLUMN sysmeta JSONB;
ALTER TABLE user_profiles ADD COLUMN usermeta JSONB;
ALTER TABLE event_user_data ADD COLUMN usermeta JSONB;

-- Update comments
COMMENT ON COLUMN users.usermeta IS 'User-editable metadata for this user';
COMMENT ON COLUMN users.sysmeta IS 'System/admin-only metadata';
-- ... (and so on for other tables)
```

### 2. エンティティクラスの更新

全エンティティに以下を追加：
```java
private String usermeta;
private String sysmeta;

// + getters/setters
```

### 3. Mapperの更新

すべてのSELECT/INSERT/UPDATEクエリでusermeta/sysmetaを扱うように更新。

### 4. OpenAPI定義の更新

- 既存スキーマに usermeta/sysmeta プロパティを追加
- MetaData スキーマを定義
- 各リソースに `/meta` エンドポイントを追加

```yaml
/api/users/{userId}/meta:
  get:
    summary: Get user metadata
  put:
    summary: Update user metadata
```

### 5. API実装

各リソースに MetaController または Meta用エンドポイントを実装：
- GET `/meta`: usermeta を返す
- PUT `/meta`: usermeta を更新
- 権限チェック: 所有者または関係者のみ

## 権限ルール

- **users**: 本人のみRW
- **events**: attendeeのみRW
- **event_attendees**: attendee全体RW
- **friendships**: senderのみRW(mutualなfriendshipが形成されるならばもう片方のrelationが張られるはずなので無問題)
- **user_profiles**: 本人のみRW
- **event_user_data**: R: attendee, W: 本人のみ
- **rooms**: attendee全体RW
- **authn_providers**: user本人のみRW
- **event_invitation_codes**: event initiatorのみRW

## テスト戦略

1. マイグレーションテスト: 既存データが正しく移行されることを確認
2. API統合テスト: 各 `/meta` エンドポイントの動作確認
3. 権限テスト: 不正アクセスが拒否されることを確認
4. データ整合性テスト: JSON形式の検証

## 注意事項

- **後方互換性**: 既存のコードで `meta` フィールドを参照している箇所をすべて `usermeta` に変更する
- **NULL許容**: usermeta/sysmeta は両方ともNULL許容
- **sysmeta へのアクセス**: 通常のAPIからは読み書き不可、管理者またはシステムのみ

## CI/CDチェックリスト

- [ ] ./gradlew build が成功
- [ ] ./gradlew spotlessCheck checkstyleMain checkstyleTest が成功
- [ ] OpenAPI validation (spectral + openapi-generator-cli) が成功
- [ ] 全テストが成功

## 進捗状況

- [x] 計画策定
- [x] データベースマイグレーション作成
- [x] エンティティ更新
- [x] Mapper更新
- [x] Service層更新
- [ ] OpenAPI定義更新 (未着手)
- [ ] API実装 (未着手)
- [ ] テスト作成・更新 (一部完了、2テスト失敗中)
- [ ] CI検証 (未完了)

## 既知の問題

### EventServiceTest の2テスト失敗

**症状:**
- `testInvitationCodeCanBeReusedAfterExpiration()` が失敗
- `testInvitationCodeCanBeReusedAfterDeletion()` が失敗
- 両方とも `insertIfInvitationCodeAvailable()` が期待値1ではなく0を返す

**原因調査:**
1. マイグレーションは正常に適用されている（V4まで）
2. 197/199テストは成功しており、基本機能は正常
3. `EventInvitationCode`に`usermeta`/`sysmeta`を追加したが、これらのフィールドが`insertIfInvitationCodeAvailable`のSQL WHERE句に影響している可能性
4. テストでは`EventInvitationCode`のusermeta/sysmetaを明示的にNULLに設定済み
5. `EventService.createEventInTransaction`でもusermeta/sysmetaを明示的にNULLに設定済み

**推測される問題:**
- `insertIfInvitationCodeAvailable`のSELECT句で`#{usermeta}::jsonb, #{sysmeta}::jsonb`を追加したことにより、何らかの副作用が発生している可能性
- または、WHERE句の`LOWER(e.status) NOT IN ('expired', 'deleted')`が正しく評価されていない可能性

**次のアクション:**
1. PostgreSQLで直接SQLを実行して、`insertIfInvitationCodeAvailable`の動作を確認
2. テストにデバッグログを追加して、データベースの状態を確認
3. 必要に応じて、`insertIfInvitationCodeAvailable`のSQLを修正
4. または、これらのテストが実際に必要か、テストロジックが正しいかを検証

**回避策:**
現時点では、この2テストの失敗は主要機能（usermeta/sysmetaの分離）に影響を与えないため、一旦保留とする。
