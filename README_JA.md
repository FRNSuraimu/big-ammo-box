# BIG AMMO BOX 1.0.0

Minecraft 1.20.1 / Forge 47.4.20 / TaCZ 1.1.8-hotfix 向けの大容量弾薬箱・Ammo Box Caseアドオンです。

## プロジェクト情報

- バージョン: `1.0.0`
- Minecraft: `1.20.1`
- Forge: `47.4.20`
- Java: `17`
- 作者・開発: `FRNSuraimu`
- 開発支援: `OpenAI`
- 必須MOD: TaCZ
- 任意連携: Re:Avaritia

本MODはFRNSuraimuが制作し、OpenAIによるAI開発支援を利用しています。公開内容はFRNSuraimuが確認し、最終的な責任を負います。OpenAIによる公式な提携・推薦・スポンサーを示すものではありません。

## 主な機能

- TaCZ互換の大容量弾薬箱。極端な容量でも`BigInteger`による正確な弾数管理。
- Ammo Box Caseによる入れ子の弾薬供給。
- Ammo Box Caseへの直接弾薬投入とBulk Ammo Storage。
- TaCZ HUDの巨大弾数表示。通常表示、短縮/科学表記、Infinity/Creative等の無限表示に対応。
- Ammo Retention / Quick Charge / Power / Piercing / Mending / Auto Reload / Ammo Magnet / Infinity / Stellar Overdrive。
- BIG AMMO BOX用の`ZERO` / `HALF` / `MAX`管理コマンド。
- Java側の必須依存を増やさないRe:Avaritia終盤レシピ連携。

## 1.0.0の方針

`0.1.9-reloadstability1`で実機確認したゲームプレイロジックを1.0.0の基準として維持しています。正式版化に伴う変更は、リリースメタデータ、Config GUI日本語/英語表示、ライセンス、ドキュメント、公開用アセット整理が中心です。

GitHub Actionsの成功はBuild/Static確認として扱い、Runtime PASSは生成されたJARを実Minecraft環境で確認した後にのみ確定します。

## ライセンス

Copyright (C) 2026 FRNSuraimu.

- プロジェクトコード: GNU General Public License version 3 only (`GPL-3.0-only`)。`LICENSE`を参照。
- BIG AMMO BOX独自のビジュアルアート・テクスチャ: Creative Commons Attribution 4.0 International (`CC BY 4.0`)。`ASSET_LICENSE.md`を参照。
- 外部MOD・依存関係・第三者アセットには、それぞれのライセンスが適用されます。`THIRD_PARTY_NOTICES.md`を参照。
- TaCZは必須の外部依存であり、BIG AMMO BOXには同梱しません。
- 1.0.0公開前に特定したTaCZ由来のビジュアルアセットはactive source/JARから削除済みで、正式1.0.0には含めません。
