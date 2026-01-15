# 引き継ぎ資料: イベント参加履歴 API の実装

## 日付
2026-01-15

## 概要
ユーザーが参加したイベントの一覧を取得する新しい API エンドポイント `/api/users/{userId}/attended-events` を実装しました。

## 実装内容

### 1. OpenAPI 仕様の追加
- **ファイル**: `openapi/paths/events.yaml`
- **エンドポイント**: `GET /api/users/{userId}/attended-events`
- **説明**: 指定されたユーザーが参加者として登録されているイベントの一覧を返す
- **レスポンス**: `Event[]` (イベントの配列)

### 2. データ層の確認
- **既存のメソッド**: `EventAttendeeMapper.findByAttendeeUserId(Long attendeeUserId)` は既に実装済み
- このメソッドは `event_attendees` テーブルからユーザーが参加しているイベントの関連データを取得

### 3. サービス層の実装
- **ファイル**: `src/main/java/app/aoki/quarkuscrud/service/EventService.java`
- **新規メソッド**: `findAttendedEventsByUserId(Long userId)`
  - `EventAttendeeMapper.findByAttendeeUserId()` を使用して参加者レコードを取得
  - 各参加者レコードからイベント詳細を取得
  - Stream API を使用して効率的に処理

```java
public List<Event> findAttendedEventsByUserId(Long userId) {
  List<EventAttendee> attendees = eventAttendeeMapper.findByAttendeeUserId(userId);
  return attendees.stream()
      .map(attendee -> eventMapper.findById(attendee.getEventId()))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
}
```

### 4. ユースケース層の実装
- **ファイル**: `src/main/java/app/aoki/quarkuscrud/usecase/EventUseCase.java`
- **新規メソッド**: `listAttendedEventsByUser(Long userId, Long requestingUserId)`
  - ユーザーの存在確認
  - セキュリティ考慮: 招待コードはイベント作成者にのみ表示
  - DTO へのマッピング

### 5. API リソース層の実装
- **ファイル**: `src/main/java/app/aoki/quarkuscrud/resource/EventsApiImpl.java`
- **新規メソッド**: `listAttendedEventsByUser(@PathParam("userId") Long userId)`
  - 認証必須 (`@Authenticated`)
  - エラーハンドリング (404: ユーザーが見つからない場合)

### 6. 統合テストの追加
- **ファイル**: `src/test/java/app/aoki/quarkuscrud/EventCrudIntegrationTest.java`
- **新規テスト**:
  - `testListAttendedEventsByUser`: ユーザーが参加したイベントが正しく返されることを確認
  - `testListAttendedEventsForUserWithNoEvents`: イベントに参加していないユーザーの場合、空配列が返されることを確認

## セキュリティ考慮事項

### CWE-284 対策
招待コードの漏洩防止のため、以下のセキュリティロジックを実装:
- 招待コードはイベント作成者 (initiator) にのみ返される
- 参加者は自分が参加したイベント情報は取得できるが、招待コードは見えない
- これにより、参加者が招待コードを知らない第三者に共有することを防止

## テスト結果

### 単体テスト
- `./gradlew test --tests EventCrudIntegrationTest`: ✅ 成功

### コード品質チェック
- `./gradlew spotlessCheck`: ✅ 成功
- `./gradlew checkstyleMain checkstyleTest`: ✅ 成功

### OpenAPI バリデーション
- Spectral: 既存の警告のみ (今回の変更による新規エラーなし)
- OpenAPI Generator CLI: ✅ 成功

### フルビルド
- `./gradlew build`: ✅ 成功 (全 122 テスト成功)

## API 使用例

### リクエスト
```bash
GET /api/users/123/attended-events
Authorization: Bearer {JWT_TOKEN}
```

### レスポンス例
```json
[
  {
    "id": 456,
    "initiatorId": 789,
    "status": "CREATED",
    "expiresAt": "2026-01-20T10:00:00Z",
    "createdAt": "2026-01-15T08:00:00Z",
    "updatedAt": "2026-01-15T08:00:00Z",
    "meta": {}
  }
]
```

**注意**: `invitationCode` フィールドは、リクエストしたユーザーがイベント作成者の場合のみ含まれます。

## 既存 API との違い

| エンドポイント | 用途 |
|--------------|------|
| `GET /api/users/{userId}/events` | ユーザーが**作成**したイベント一覧 |
| `GET /api/users/{userId}/attended-events` | ユーザーが**参加**したイベント一覧 (新規) |

## フロントエンド実装への影響

### 従来の問題点 (localStorage 使用)
- ❌ ブラウザ/デバイス間で同期されない
- ❌ ブラウザデータクリア時に消失
- ❌ イベント詳細取得に N 回の API コールが必要
- ❌ 参加日時などのメタデータが取得できない

### 新 API の利点
- ✅ デバイス間でデータ同期
- ✅ 永続的で信頼性の高いデータ
- ✅ 単一の API コールで全データ取得
- ✅ 参加日時やロールなどのメタデータが含まれる可能性

## 次のステップ (フロントエンド側)

1. localStorage 読み取りを API コールに置き換え
2. localStorage 書き込みはオフライン対応のキャッシュとして維持
3. アプリ起動時に localStorage とバックエンドを同期
4. 参加日時の表示機能を追加

## CI/CD への影響

- ✅ すべての CI チェックが通過
- ✅ 破壊的変更なし
- ✅ 既存のテストに影響なし

## 備考

- データベーススキーマの変更は不要 (既存の `event_attendees` テーブルを使用)
- パフォーマンス: N+1 問題の可能性 (多数のイベント参加時)
  - 将来的に JOIN クエリへの最適化を検討可能
- ページネーション未実装 (多数のイベント参加時の対応として今後追加を検討)

## 関連ファイル

### 変更ファイル
- `openapi/openapi.yaml`
- `openapi/paths/events.yaml`
- `src/main/java/app/aoki/quarkuscrud/resource/EventsApiImpl.java`
- `src/main/java/app/aoki/quarkuscrud/service/EventService.java`
- `src/main/java/app/aoki/quarkuscrud/usecase/EventUseCase.java`
- `src/test/java/app/aoki/quarkuscrud/EventCrudIntegrationTest.java`

### 生成ファイル (自動生成)
- `build/generated-src/openapi/src/gen/java/app/aoki/quarkuscrud/generated/api/EventsApi.java`

## 作業時間
約 1.5 時間

## 問い合わせ先
GitHub Copilot Agent (copilot/add-attended-events-api ブランチ)
