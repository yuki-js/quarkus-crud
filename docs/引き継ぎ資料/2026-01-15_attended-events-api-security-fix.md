# 引き継ぎ資料: 参加イベント取得APIのセキュリティ修正

## 日付
2026-01-15

## 概要
`/api/users/{userId}/attended-events` エンドポイントにおけるプライバシー脆弱性を修正しました。従来、任意の認証済みユーザーが他のユーザーの参加イベント一覧を閲覧できる設計でしたが、これを `/api/me/attended-events` に変更し、ユーザーは自分自身の参加イベントのみを取得できるようにしました。

## 問題点

### セキュリティ脆弱性
- **CWE-639**: Authorization Bypass Through User-Controlled Key
- **問題**: `/api/users/{userId}/attended-events` は、URLパスの `userId` パラメータを変更することで、任意のユーザーの参加イベントを閲覧できる設計でした
- **影響**: プライバシー侵害 - ユーザーAがユーザーBの参加イベント一覧を勝手に閲覧できる

### 設計上の問題
- URLパスに他ユーザーのIDを指定できるパターンは、アクセス制御の実装漏れを招きやすい
- 認可チェックを実装したとしても、URLパターン自体が誤解を招く設計

## 実施した変更

### 1. APIエンドポイントの変更

#### 変更前
```
GET /api/users/{userId}/attended-events
```
- パスパラメータ `userId` を受け取る
- 認証必須だが、任意のユーザーIDを指定可能

#### 変更後
```
GET /api/me/attended-events
```
- パスパラメータなし
- 認証トークンから自動的に現在のユーザーを特定
- **設計原則**: ユーザーは自分のリソースのみアクセス可能

### 2. OpenAPI仕様の更新

**ファイル**: `openapi/openapi.yaml`
```yaml
# 変更前
/api/users/{userId}/attended-events:
  $ref: './paths/events.yaml#/paths/~1api~1users~1{userId}~1attended-events'

# 変更後
/api/me/attended-events:
  $ref: './paths/events.yaml#/paths/~1api~1me~1attended-events'
```

**ファイル**: `openapi/paths/events.yaml`
```yaml
/api/me/attended-events:
  get:
    tags:
      - Events
    summary: List events attended by the current user
    description: Retrieve events where the authenticated user is an attendee.
    operationId: listMyAttendedEvents
    responses:
      '200':
        description: List of events attended by the current user.
        # ... (詳細は省略)
      '401':
        description: Authentication required.
      '500':
        description: Unexpected error.
```

**重要な変更点**:
- `userId` パラメータを削除
- `403 Forbidden` レスポンスを削除（不要になったため）
- `404 Not Found` レスポンスを削除（ユーザー不在のチェックが不要になったため）
- operationId を `listAttendedEventsByUser` から `listMyAttendedEvents` に変更

### 3. 実装コードの更新

**ファイル**: `src/main/java/app/aoki/quarkuscrud/resource/EventsApiImpl.java`

```java
// 変更前
@Override
@Authenticated
@GET
@Path("/users/{userId}/attended-events")
@Produces(MediaType.APPLICATION_JSON)
public Response listAttendedEventsByUser(@PathParam("userId") Long userId) {
  User user = authenticatedUser.get();
  
  // Authorization check: Users can only view their own attended events
  if (!user.getId().equals(userId)) {
    return Response.status(Response.Status.FORBIDDEN)
        .entity(new ErrorResponse("Access denied. You can only view your own attended events."))
        .build();
  }
  
  try {
    List<Event> events = eventUseCase.listAttendedEventsByUser(userId, user.getId());
    return Response.ok(events).build();
  } catch (IllegalArgumentException e) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(new ErrorResponse(e.getMessage()))
        .build();
  }
}

// 変更後
@Override
@Authenticated
@GET
@Path("/me/attended-events")
@Produces(MediaType.APPLICATION_JSON)
public Response listMyAttendedEvents() {
  User user = authenticatedUser.get();

  try {
    List<Event> events = eventUseCase.listAttendedEventsByUser(user.getId(), user.getId());
    return Response.ok(events).build();
  } catch (IllegalArgumentException e) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(new ErrorResponse(e.getMessage()))
        .build();
  }
}
```

**重要な変更点**:
- メソッド名を `listAttendedEventsByUser` から `listMyAttendedEvents` に変更
- `@PathParam("userId")` パラメータを削除
- 認可チェックのコードを削除（URLパターンで保証されるため不要）
- 403 Forbiddenレスポンスを削除

### 4. テストの更新

**ファイル**: `src/test/java/app/aoki/quarkuscrud/EventCrudIntegrationTest.java`

#### テスト: `testListAttendedEventsByUser`
```java
// 変更: エンドポイントを /api/me/attended-events に変更
given()
    .header("Authorization", "Bearer " + newUserToken)
    .when()
    .get("/api/me/attended-events")  // 変更: userId パラメータを削除
    .then()
    .statusCode(200)
    .body("size()", greaterThanOrEqualTo(1))
    .body("[0].id", equalTo(eventId.intValue()));
```

#### テスト: `testListAttendedEventsForUserWithNoEvents`
```java
// 変更: エンドポイントを /api/me/attended-events に変更
given()
    .header("Authorization", "Bearer " + newUserToken)
    .when()
    .get("/api/me/attended-events")  // 変更
    .then()
    .statusCode(200)
    .body("size()", equalTo(0));
```

#### テスト: `testCannotViewOtherUsersAttendedEvents` → `testMyAttendedEventsEndpointWorksCorrectly`
- テスト名を変更：「他ユーザーの閲覧不可」から「自分のイベント取得が正常動作」に変更
- 403エラーのテストを削除（設計上不可能になったため）
- 認証ユーザーが自分の参加イベントを取得できることを確認

## テスト結果

### 単体テスト
```bash
./gradlew test --tests EventCrudIntegrationTest
```
**結果**: ✅ 全テスト成功（12テスト）

### コード品質チェック
```bash
./gradlew build spotlessCheck checkstyleMain checkstyleTest
```
**結果**: ✅ 成功（既存の警告のみ、新規エラーなし）

### OpenAPIバリデーション
```bash
spectral lint build/openapi-compiled/openapi.yaml --ruleset .spectral.yaml
```
**結果**: ✅ 既存の警告のみ（新規エラーなし）

```bash
java -jar openapi-generator-cli.jar validate -i build/openapi-compiled/openapi.yaml
```
**結果**: ✅ 成功（1つの推奨事項のみ）

## セキュリティ改善

### 修正前の問題
1. **URLベースのアクセス制御**: `/api/users/123/attended-events` という設計は、「ユーザー123の参加イベントにアクセスできる」ことを暗示
2. **実装依存のセキュリティ**: 認可チェックコードを追加することで対処していたが、コードレビューや実装ミスのリスクが存在
3. **ユーザビリティの問題**: フロントエンド開発者が「他ユーザーのイベントも取得できる」と誤解する可能性

### 修正後の改善
1. **設計レベルでのセキュリティ**: `/api/me/attended-events` は、設計上「現在のユーザーのみ」を保証
2. **実装の簡素化**: 認可チェックが不要になり、コードが簡潔に
3. **明確なAPI設計**: エンドポイント名から「自分の情報のみアクセス可能」であることが明確

## API使用例

### リクエスト
```bash
GET /api/me/attended-events
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

## フロントエンド実装への影響

### 変更が必要な箇所
- エンドポイントURLを変更: `/api/users/${userId}/attended-events` → `/api/me/attended-events`
- パスパラメータ `userId` を削除

### 変更例（JavaScript）
```javascript
// 変更前
fetch(`/api/users/${userId}/attended-events`, {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})

// 変更後
fetch('/api/me/attended-events', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
```

## マイグレーション戦略

### 後方互換性
- ⚠️ **破壊的変更**: 古いエンドポイント `/api/users/{userId}/attended-events` は削除されました
- フロントエンドは新しいエンドポイントに更新が必要

### 推奨手順
1. バックエンドデプロイ前に、フロントエンドを新エンドポイントに対応
2. 両方のエンドポイントを一時的に共存させる場合は、古いエンドポイントを非推奨化
3. 十分な移行期間後に古いエンドポイントを削除

## 関連ファイル

### 変更ファイル
- `openapi/openapi.yaml`
- `openapi/paths/events.yaml`
- `src/main/java/app/aoki/quarkuscrud/resource/EventsApiImpl.java`
- `src/test/java/app/aoki/quarkuscrud/EventCrudIntegrationTest.java`

### 生成ファイル（自動生成）
- `build/generated-src/openapi/src/gen/java/app/aoki/quarkuscrud/generated/api/EventsApi.java`
- `build/openapi-compiled/openapi.yaml`
- `build/generated/openapi-clients/javascript-fetch/**/*`

## 今後の課題

1. **他のエンドポイントのレビュー**: 同様のパターン `/api/users/{userId}/*` があれば、同様の修正を検討
2. **認可ポリシーの標準化**: `/api/me/*` パターンを他のエンドポイントにも適用
3. **ドキュメント更新**: APIドキュメントやSwagger UIでの説明を充実

## 参考情報

### CWE-639: Authorization Bypass Through User-Controlled Key
https://cwe.mitre.org/data/definitions/639.html

**説明**: ユーザーが制御可能なキー（この場合はURLパスの `userId`）を使用して、適切な認可チェックなしにリソースへアクセスできる脆弱性。

**対策**: リソースアクセスをセッション情報（認証トークン）から取得したユーザーIDに基づいて行う。

## 作業時間
約1.5時間

## 問い合わせ先
GitHub Copilot Agent (copilot/fix-attended-events-api-visibility ブランチ)
