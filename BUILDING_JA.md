# BIG AMMO BOX 1.0.0 ビルド手順

## 必要環境

- JDK 17
- Gradle 8.8
- Minecraft 1.20.1 / Forge 47.4.20
- TaCZ 1.20.1 / 1.1.8-hotfix 系 JAR

## TaCZ JARの配置

このソースリポジトリには第三者MODのTaCZ JARを同梱していません。

ビルド前にTaCZ 1.20.1版JARを次の名前で配置してください。

`libs/tacz-1.20.1.jar`

## ビルド

プロジェクト直下でGradle 8.8を使い、次を実行してください。

```text
gradle clean build
```

出力先:

`build/libs/big_ammo_box-1.0.0.jar`

`build.gradle` は Java 17 の `sourceCompatibility/targetCompatibility` と `--release 17` を指定しています。

GitHub ActionsではTaCZ 1.1.8-hotfix依存をCI時に外部から取得してビルドします。TaCZ JARはリポジトリやBIG AMMO BOXのJARへ同梱しません。
