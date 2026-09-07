# BIG AMMO BOX

Source repository for BIG AMMO BOX, a Minecraft Forge 1.20.1 addon for TaCZ.

Japanese README: `README_JA.md`

## Project information

- Version: `1.0.0`
- Minecraft: `1.20.1`
- Forge: `47.4.20`
- Java: `17`
- Author / Developer: `FRNSuraimu`
- Development Assistance: `OpenAI`
- Required mod: TaCZ
- Optional integration: Re:Avaritia

This project is developed by FRNSuraimu with AI-assisted development support from OpenAI. FRNSuraimu reviews and takes responsibility for the released project. This does not imply sponsorship, endorsement, or an official partnership with OpenAI.

## Main features

- Large-capacity TaCZ-compatible ammo boxes, including exact `BigInteger` storage for extreme capacities.
- Portable Ammo Box Cases with nested ammo supply support.
- Direct ammo insertion and Bulk Ammo Storage.
- TaCZ HUD integration for large ammo counts, including compact/scientific display and infinity display where applicable.
- Ammo Case enchantment effects including Ammo Retention, Quick Charge, Power, Piercing, Mending, Auto Reload, Ammo Magnet, Infinity support, and Stellar Overdrive.
- Administrative `ZERO` / `HALF` / `MAX` fill commands for BIG AMMO BOX items.
- Optional Re:Avaritia endgame recipes without a mandatory Java compile dependency.

## Build

The repository stores the Gradle project directly. GitHub Actions builds the checked-in source tree with Java 17 / Gradle 8.8 and obtains the TaCZ 1.1.8 hotfix dependency separately for compilation. TaCZ is not bundled with this project.

A successful CI run uploads `build/libs/*.jar` as the `big-ammo-box-build` workflow artifact.

## License

Copyright (C) 2026 FRNSuraimu.

- Project code is licensed under the GNU General Public License version 3 only (`GPL-3.0-only`). See `LICENSE`.
- BIG AMMO BOX original visual artwork and textures are licensed under Creative Commons Attribution 4.0 International (`CC BY 4.0`). See `ASSET_LICENSE.md`.
- Third-party projects, dependencies, and assets remain subject to their own licenses. See `THIRD_PARTY_NOTICES.md`.
- TaCZ is a required external dependency and is not bundled with BIG AMMO BOX.
- Identified TaCZ-derived visual assets were removed before the 1.0.0 release and are not included in the active 1.0.0 source/JAR.
