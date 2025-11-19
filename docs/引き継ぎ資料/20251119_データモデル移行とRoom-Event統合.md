# データモデル移行とRoom→Event統合 引き継ぎ資料

**作成日**: 2025年11月19日  
**担当**: Copilot Agent  
**ステータス**: 部分実装完了 - 追加実装が必要

## 概要

本PRでは、Issue #18で定義された新しいデータモデルへの移行を実施しました。主な変更点は：

1. Userテーブルの認証情報を分離（authn_providersテーブル新設）
2. ユーザープロフィール管理の追加（user_profilesテーブル）
3. 友人関係の管理（friendshipsテーブル）
4. Room概念からEvent概念への移行

## 実装済み事項

### ✅ データベーススキーマ

**V1__Initial_schema.sql** で以下のテーブルを作成済み：

1. **users** - アカウントライフサイクル管理
   - account_lifecycle (created/provisioned/active/paused/deleted)
   - current_profile_revision (FK to user_profiles)
   - meta (JSONB)

2. **authn_providers** - 認証プロバイダ情報
   - user_id (FK to users)
   - auth_method (anonymous/oidc)
   - auth_identifier
   - external_subject

3. **user_profiles** - プロフィールリビジョン管理
   - user_id (FK to users)
   - profile_data (JSONB)
   - revision_meta (JSONB)
   - イミュータブルな積み上げ型

4. **friendships** - 片方向の友人関係
   - sender_id (FK to users)
   - recipient_id (FK to users)

5. **events** - クイズイベント（旧Room）
   - initiator_id (FK to users)
   - status (created/active/ended/expired/deleted)
   - meta (JSONB)
   - expires_at

6. **event_invitation_codes** - イベント参加コード
   - event_id (FK to events)
   - invitation_code (varchar(64))

7. **event_attendees** - イベント参加者
   - event_id (FK to events)
   - attendee_user_id (FK to users)
   - meta (JSONB)

### ✅ エンティティクラス

以下のエンティティを新規作成：
- `User.java` (更新)
- `AuthnProvider.java`
- `AuthMethod.java` (enum)
- `AccountLifecycle.java` (enum)
- `UserProfile.java`
- `Friendship.java`
- `Event.java`
- `EventStatus.java` (enum)
- `EventInvitationCode.java`
- `EventAttendee.java`

### ✅ MyBatisマッパー

以下のマッパーを新規作成：
- `UserMapper.java` (更新)
- `AuthnProviderMapper.java`
- `UserProfileMapper.java`
- `FriendshipMapper.java`
- `EventMapper.java`
- `EventInvitationCodeMapper.java`
- `EventAttendeeMapper.java`

### ✅ サービス層の更新

- `UserService.java` - User + AuthnProvider の同時作成に対応
- `JwtService.java` - AuthnProviderテーブルから認証情報を取得
- `AuthenticationService.java` - AuthMethod enumを使用

### ✅ 既存コードの保持

以下のファイルは **削除せず復元済み**：
- `Room.java` (entity)
- `RoomMapper.java`
- `RoomService.java`
- `RoomEventBroadcaster.java`
- `RoomsApiImpl.java`
- `LiveApiImpl.java`

## 🔴 未実装事項（要対応）

### 1. RoomServiceのEvent統合 【最優先】

**現状**: `RoomService.java` は旧Roomエンティティを直接使用
**必要な作業**: EventエンティティとEventMapperを使用するように再実装

```java
// 現在の実装 (Room直接使用)
public Room createRoom(String name, String description, Long userId) {
  Room room = new Room();
  room.setName(name);
  room.setDescription(description);
  room.setUserId(userId);
  // ...
  roomMapper.insert(room);
  return room;
}

// 必要な実装 (Event使用、Roomインターフェース維持)
public Room createRoom(String name, String description, Long userId) {
  // 1. Eventを作成
  Event event = new Event();
  event.setInitiatorId(userId);
  event.setStatus(EventStatus.CREATED);
  event.setExpiresAt(calculateExpiration());
  
  // 2. meta JSONBに name/description を格納
  String meta = createMetaJson(name, description);
  event.setMeta(meta);
  
  eventMapper.insert(event);
  
  // 3. EventInvitationCodeを生成
  EventInvitationCode code = generateInvitationCode(event.getId());
  eventInvitationCodeMapper.insert(code);
  
  // 4. Roomオブジェクトにマッピングして返す
  return mapEventToRoom(event, code);
}
```

**影響範囲**:
- `RoomService.createRoom()`
- `RoomService.findById()`
- `RoomService.findAll()`
- `RoomService.findByUserId()`
- `RoomService.updateRoom()`
- `RoomService.deleteRoom()`

**実装のポイント**:
- Room.name / Room.description → Event.meta (JSONB) に格納
- Room.id → Event.id にマッピング
- Room.userId → Event.initiatorId にマッピング
- イベント参加コードの生成と管理
- Eventのexpiresアタイムアウト管理

### 2. RoomMapperの廃止とマッピング層の実装

**現状**: `RoomMapper` はRoomテーブルに直接アクセス
**必要な作業**: 
- RoomMapperの使用を停止
- EventMapper、EventInvitationCodeMapperを使用
- Event ↔ Room の変換ロジックを実装

```java
// RoomService内で変換メソッドを実装
private Room mapEventToRoom(Event event, EventInvitationCode code) {
  Room room = new Room();
  room.setId(event.getId());
  room.setUserId(event.getInitiatorId());
  
  // metaからname/descriptionを抽出
  JsonNode meta = parseJson(event.getMeta());
  room.setName(meta.get("name").asText());
  room.setDescription(meta.get("description").asText());
  
  room.setCreatedAt(event.getCreatedAt());
  room.setUpdatedAt(event.getUpdatedAt());
  
  return room;
}

private String createMetaJson(String name, String description) {
  ObjectMapper mapper = new ObjectMapper();
  ObjectNode meta = mapper.createObjectNode();
  meta.put("name", name);
  meta.put("description", description);
  return meta.toString();
}
```

### 3. RoomEventBroadcasterの統合

**現状**: Room専用のイベントブロードキャスター
**必要な作業**: Event用に機能を拡張

- イベント作成/更新/削除時のブロードキャスト
- EventAttendeeの参加/退出イベント
- イベントステータス変更の通知

### 4. LiveApiImplの更新

**現状**: RoomEventResponseを使用
**必要な作業**: 
- イベント関連のSSEストリームを実装
- Event情報をRoomResponse形式に変換
- クライアント側の互換性維持

### 5. テストの再実装 【重要】

以下のテストファイルがRoom依存のため失敗中：

**修正が必要なテスト**:
- `RoomCrudIntegrationTest.java` - Room CRUD操作のテスト
- `RoomServiceTest.java` (存在する場合)
- `RoomsApiImplTest.java` (存在する場合)

**対応方針**:
1. Eventエンティティを使用するように書き換え
2. Room APIの互換性をテスト
3. Event特有の機能（参加コード、参加者管理）のテストを追加

### 6. 新機能の実装

Issue #18で定義されているが未実装の機能：

#### 6.1 プロフィール管理
- プロフィールリビジョンの作成
- 最新プロフィールの取得
- プロフィール履歴の参照
- User.current_profile_revisionの更新

#### 6.2 友人関係管理
- プロフィールカードの送信/受信
- 友人リストの取得
- 友人関係の削除

#### 6.3 イベント参加コード管理
- 参加コードの生成（ユニーク性担保）
- 排他制御の実装
- 期限切れイベントのコード再利用

#### 6.4 イベント参加者管理
- イベントへの参加/退出
- 参加者リストの取得
- 参加者固有のメタデータ管理

## データ移行の注意事項

### マイグレーション戦略

**現状**: V1スキーマで完全リセット
- 既存のusersテーブルとroomsテーブルは削除される
- 後方互換性なし（Issue #18で許可済み）

**本番環境への適用時**:
1. 既存データのバックアップ必須
2. ダウンタイムが発生
3. 既存ユーザーは再登録が必要

### JSONBフィールドの設計

以下のフィールドでJSONBを使用：
- `users.meta` - 停止理由などの管理情報
- `user_profiles.profile_data` - プロフィール本体
- `user_profiles.revision_meta` - リビジョン管理情報
- `events.meta` - イベント名、説明など
- `event_attendees.meta` - 参加者固有情報

**推奨JSONスキーマ**:

```json
// events.meta
{
  "name": "クイズイベント1",
  "description": "楽しいクイズ大会",
  "quiz_data": {
    "questions": [...],
    "settings": {...}
  }
}

// user_profiles.profile_data
{
  "display_name": "ユーザー太郎",
  "avatar_url": "https://...",
  "bio": "自己紹介文",
  "custom_fields": {...}
}
```

## API互換性

### 維持されるエンドポイント

現在のOpenAPI仕様は変更なし：
- `POST /api/rooms` - createRoom
- `GET /api/rooms` - getAllRooms
- `GET /api/rooms/{id}` - getRoomById
- `PUT /api/rooms/{id}` - updateRoom
- `DELETE /api/rooms/{id}` - deleteRoom
- `GET /api/rooms/my` - getMyRooms
- `GET /api/live/rooms` - streamRoomEvents (SSE)

### 追加が必要なエンドポイント

新データモデルに対応した新規API：
- イベント参加コード管理
- イベント参加者管理
- プロフィール管理
- 友人関係管理

## 実装優先順位

### Phase 1: Room-Event統合完了 【今すぐ】
1. RoomServiceをEvent使用に書き換え
2. Room ↔ Event マッピング実装
3. 統合テスト実施

### Phase 2: イベント固有機能 【次】
1. 参加コード生成・検証
2. 参加者管理機能
3. イベントライフサイクル管理

### Phase 3: 新機能実装 【その後】
1. プロフィール管理API
2. 友人関係API
3. フロントエンド統合

## 技術的な課題と解決策

### 課題1: Room.nameとRoom.descriptionの保存先

**問題**: RoomにはnameとdescriptionフィールドがあるがEventにはない
**解決策**: Event.meta (JSONB) に格納

### 課題2: イベント参加コードのユニーク性

**問題**: アクティブなイベント間でのみユニークである必要がある
**解決策**: 
- アプリケーション層で排他制御
- トランザクション内でコード生成と検証
- SELECT FOR UPDATE を使用

### 課題3: 後方互換性の破棄

**問題**: 既存のRoomデータとAPIが使えなくなる
**対策**: 
- 移行期間中はRoom APIを維持
- 段階的にEvent APIに移行
- ドキュメント化と周知

## 参照ドキュメント

- Issue #18: データモデル定義
- `docs/data-model.md`: スキーマ詳細
- `MIGRATION.md`: 移行サマリー
- `src/main/resources/db/migration/V1__Initial_schema.sql`: DDL

## 問い合わせ先

実装に関する質問は Issue #18 または PR コメントで @copilot にメンション

---

**最終更新**: 2025年11月19日
**次回レビュー予定**: Phase 1完了後
