JavaScript Fetch Client ガイド
=============================

OpenAPI から生成した `@yuki-js/quarkus-crud-js-fetch-client` の導入方法と使い方を、初心者でも迷わないように整理しました。設定 → インストール → 呼び出し → エラー対処の順で追えば、そのまま自分のプロジェクトに組み込めます。

## 前提

- Node.js と npm がインストールされている（`node -v` と `npm -v` が動けばOK）
- ターミナルは開ける
- すでにあるフロントエンドプロジェクトか、新しく作る空の npm プロジェクトを用意できる

## 1. プロジェクトを用意する

既存のフロントエンドアプリに組み込みたいときは、そのプロジェクトのフォルダで terminal を開きます。新規で試すときは次のように打てば最小構成ができます。

```bash
mkdir aoki-client-demo
cd aoki-client-demo
npm init -y
```

これで `package.json` ができました。`npm run dev` などを叩くときはこのフォルダで作業します。

## 2. npm から OpenAPI クライアントをインストールする

下のコマンドをコピペしてください。URL だけ変えれば他のバージョンでも同じです。

```bash
npm install https://quarkus-crud.ouchiserver.aokiapp.com/clients/yuki-js-quarkus-crud-js-fetch-client-0.0.1.tgz
```

ポイント:

- `https://quarkus-crud.ouchiserver.aokiapp.com/...` の URL を使うこと。常に公開環境のアーカイブを使えば、チーム全員が同じクライアントを参照できます。
- インストール中に止める必要が出たら **Ctrl+C** でキャンセルできます。

## 3. コードから使う

TypeScript／JavaScript どちらでもOKです。API クラスは `@yuki-js/quarkus-crud-js-fetch-client` から import できます。

```ts
// src/lib/aokiClient.ts
import { Configuration, AuthenticationApi, EventsApi } from '@yuki-js/quarkus-crud-js-fetch-client';

let jwtToken: string | null = null;

export function setJwtToken(token: string | null) {
  jwtToken = token;
}

const config = new Configuration({
  basePath: 'https://quarkus-crud.ouchiserver.aokiapp.com', // バックエンドの本番URL
  accessToken: () => jwtToken ?? undefined, // 設定済みトークンをそのまま流用
});

export const authApi = new AuthenticationApi(config);
export const eventsApi = new EventsApi(config);

export async function createGuestUser() {
  const response = await authApi.createGuestUserRaw();
  const authorization = response.raw.headers.get('authorization');
  let token: string | null = null;

  if (authorization) {
    token = authorization.startsWith('Bearer ')
      ? authorization.slice('Bearer '.length)
      : authorization;
  }

  setJwtToken(token ?? null);

  const user = await response.value();
  return { user, token };
}

export async function fetchCurrentUser() {
  return authApi.getCurrentUser();
}
```

たとえば React で「ゲストとして参加」ボタンから呼びたい場合は次のように書けます。

```tsx
import { useState } from 'react';
import { createGuestUser } from './lib/aokiClient';

export function GuestLoginButton() {
  const [status, setStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle');
  const [user, setUser] = useState(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setStatus('loading');
    try {
      const result = await createGuestUser();
      setUser(result.user);
      setStatus('done');
    } catch (error) {
      console.error(error);
      setStatus('error');
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <button type="submit" disabled={status === 'loading'}>
        {status === 'loading' ? '作成中...' : 'ゲストとして参加'}
      </button>
      {status === 'done' && user && <p>{user.displayName} として参加しました</p>}
    </form>
  );
}
```

### ゲストログイン処理の流れ

1. **ゲストを作成し、トークン＋ユーザー情報を取得する**

    ```ts
    export async function createGuestUser() {
      const response = await authApi.createGuestUserRaw();
      const authorization = response.raw.headers.get('authorization');
      let token: string | null = null;

      if (authorization) {
        token = authorization.startsWith('Bearer ')
          ? authorization.slice('Bearer '.length)
          : authorization;
      }

      setJwtToken(token ?? null);
      if (token) {
        localStorage.setItem('jwt', token);
      }

      const user = await response.value();
      return { user, token };
    }
    ```

2. **取得したユーザー情報を UI に反映する**  
   上の返り値 `user` をそのまま React state に突っ込んで表示すれば OK（例: `setUser(result.user)`）。

3. **後続 API では同じ `eventsApi` などを呼ぶだけ**  
   `Configuration` が `accessToken: () => jwtToken ?? undefined` になっているので、`setJwtToken()` 済みであれば自動的に `Authorization: Bearer ...` が付与されます。

4. **ページ再読込後にトークンを復元する**

    ```ts
    export async function bootstrapAuth() {
      const savedToken = localStorage.getItem('jwt');
      if (!savedToken) {
        return null;
      }

      setJwtToken(savedToken);
      try {
        return await fetchCurrentUser();
      } catch (error) {
        console.warn('保存していたトークンが無効でした', error);
        setJwtToken(null);
        localStorage.removeItem('jwt');
        return null;
      }
    }
    ```

   アプリ起動時（例: `App.tsx`）でこの関数を一度呼ぶだけで、前回のログイン状態を復元できます。

## 4. どの API クラスを呼べばいい？

このクライアントには OpenAPI の各タグごとにクラスが用意されています。代表的な使い方をまとめました。必要な引数は型定義 (`.d.ts`) に全部書いてあるので、エディタの入力補完に頼ってOKです。

| クラス | 主なメソッド | 例 |
|-------|-------------|----|
| `AuthenticationApi` | `createGuestUser`, `getCurrentUser` | ゲスト作成、現在のユーザー確認 |
| `EventsApi` | `createEvent`, `getEventById`, `joinEventByCode`, `listEventsByUser` | イベント作成・参加 |
| `FriendshipsApi` | `createFriendship`, `listReceivedFriendships` | フレンド関連 |
| `ProfilesApi` | `getLatestProfile`, `updateProfile` | プロフィール取得・更新 |
| `UsersApi` | `listUsers`, `getUserById` | ユーザー検索 |

### 呼び出しパターン

```ts
import { Configuration, EventsApi } from '@yuki-js/quarkus-crud-js-fetch-client';

let currentToken: string | null = null;

export function updateToken(token: string | null) {
  currentToken = token;
}

const config = new Configuration({
  basePath: 'https://quarkus-crud.ouchiserver.aokiapp.com',
  accessToken: () => currentToken ?? undefined,
});

const eventsApi = new EventsApi(config);

export async function createEvent(input: { title: string; startAt: string }) {
  return await eventsApi.createEvent({
    eventCreateRequest: {
      title: input.title,
      startAt: input.startAt,
    },
  });
}
```

- すべてのメソッドは Promise を返します。`await` でそのまま結果を受け取ってください。
- リクエストボディが必要なときはメソッド引数に `{ eventCreateRequest: {...} }` のように **OpenAPI で指定されている名前** を付ける必要があります（TypeScript の型が教えてくれます）。

### エラー処理

```ts
try {
  await eventsApi.createEvent({ eventCreateRequest: {...} });
} catch (error) {
  if (error instanceof Response) {
    const body = await error.json();
    console.error('サーバーエラー', error.status, body);
  } else {
    console.error('通信に失敗しました', error);
  }
}
```

`fetch` ベースなので、HTTP ステータス 400 以降は `Response` オブジェクトとして throw されます。アプリ側で `error instanceof Response` を調べると、サーバーが返した JSON を読めます。

## 5. トークンの扱い

- `Configuration` は一度作ったら使い回し、`accessToken` に渡す関数の側で最新の JWT を返すようにします（例: `updateToken(newToken)` でモジュール変数を更新）。
- `localStorage` を使うなら、ログイン後に `setJwtToken(localStorage.getItem('jwt'))` のように同期しておけば OK です。
- トークンがない状態（匿名ユーザー）で呼ぶときは `null` を渡すだけで、ヘッダーが付与されません。

## 6. React Query / TanStack Query と組み合わせる例

```ts
import { useQuery } from '@tanstack/react-query';
import { eventsApi } from './lib/aokiClient';

export function useMyEvents(userId: number) {
  return useQuery({
    queryKey: ['events', userId],
    queryFn: () => eventsApi.listEventsByUser({ userId }),
  });
}
```

`queryFn` にはクライアントのメソッド（Promise を返す関数）をそのまま渡せばOK。レスポンス型も付いてくるので、`data?.[0].title` のようなアクセスでも型補完が効きます。

## 7. うまくいかないときは？

- **インストールでエラーが出た**  
  もう一度同じコマンドを実行します。途中で止めた場合は `Ctrl+C` でキャンセルしてからやり直してください。

- **型エラーが多すぎてよくわからない**  
  import 文が正しいか、`Configuration` に正しい `basePath` を入れているか確認しましょう。`@yuki-js/...` のスペルミスも多いのでコピペ推奨です。

- **トークンをどう入れればいいのか分からない**  
  `Configuration` に `accessToken` 関数を渡すと、毎回その関数が呼ばれてヘッダーに `Authorization: Bearer ...` が付きます。ログイン直後に取得した JWT 等を返しましょう。

## 8. バージョンアップ時

新しいバージョンが出たら、`npm install https://quarkus-crud.ouchiserver.aokiapp.com/clients/yuki-js-quarkus-crud-js-fetch-client-X.Y.Z.tgz` とバージョン番号を変えて再インストールするだけです。`package.json` には `file:https://...` として記録されるので、チーム全員が同じバージョンを使えます。

---

困ったら「ターミナルでどのコマンドを打ったか」「どんなエラーが出たか」をスクリーンショット付きで共有してください。状態が分かればチームでフォローしやすくなります。
