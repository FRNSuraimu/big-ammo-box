# BIG AMMO BOX 1.0.0 Changelog

## 正式版化
- `0.1.9-reloadstability1` を 1.0.0 の基準実装として昇格。
- `mod_version` を `1.0.0` へ更新。
- コードライセンスを GNU GPL v3.0 only (`GPL-3.0-only`) に変更。
- BIG AMMO BOX独自のビジュアルアート・テクスチャを CC BY 4.0 として明確化。
- 公開上の作者・制作者名を `FRNSuraimu` に統一。
- OpenAIの関与は共同作者表記ではなく `Development Assistance / 開発支援` として明記。
- TaCZは必須外部依存として扱い、JARやリポジトリへ同梱しない方針を明記。
- 1.0.0公開前のアセット監査で特定した未使用のTaCZ由来ringテクスチャ群をactive sourceから削除。
- `README.md` / `README_JA.md` / ライセンス・第三者通知・1.0.0向け実装/Build記録を整理。

## Config GUI日本語化
- ForgeConfigSpecへConfigカテゴリ/設定値のtranslation keyを追加。
- `ja_jp.json`へ日本語表示名を追加。
- `en_us.json`にも英語表示名を追加し、英語環境を維持。
- Configコメントは日本語+英語の併記へ更新。
- 既存のConfig path/key/default/rangeは変更しないため、既存TOMLとの互換性を維持。

## Runtimeロジック
1.0.0では戦闘・給弾・HUD・GUI・Enchant・Networkのゲームプレイロジックを変更しない。
`BigAmmoBoxConfig.java`の差分は表示用translation/comment metadataのみ。
ライセンス・クレジット・ドキュメント・未使用アセット整理もゲームプレイロジックへ影響しない。

0.1.9系までに実装された主な機能を維持する:
- BIG AMMO BOXの巨大容量 / exact BigInteger処理。
- Ammo Box Case / nested ammo bridge / 直接補充 / Bulk Ammo Storage。
- TaCZ HUD巨大弾数表示、Creative / Infinity等の無限表示。
- Ammo Retention / Quick Charge / Power / Piercing / Mending / Auto Reload / Ammo Magnet / Infinity / Stellar Overdrive。
- ZERO / HALF / MAXコマンド。
- Auto Reload logical gun identity追跡とQuick Charge snapshot安定化。

## Runtime状態
`0.1.9-reloadstability1`基準ロジックの主要機能は実Minecraftで確認済み。
ただし、ライセンス・クレジット最終反映を含む正式1.0.0候補JARのRuntime PASSは、そのJARを実Minecraftで確認した後に確定する。
