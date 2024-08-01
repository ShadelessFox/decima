[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://adoptium.net/temurin/releases/?package=jre&arch=x64&version=17)
[![Download](https://img.shields.io/github/v/release/ShadelessFox/decima?label=Download&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0Ij4KCTxwYXRoIGZpbGw9IndoaXRlIiBkPSJNMTEuMiAwYS44LjggMCAwIDAtLjguOHYxMS40TDcuMjYgOS40NGEuODAzLjgwMyAwIDAgMC0xLjEzLjA3NGwtMS4wNSAxLjJhLjguOCAwIDAgMCAuMDczIDEuMTNsNi4zMyA1LjU0YS43OTUuNzk1IDAgMCAwIDEuMDUgMGw2LjMyLTUuNTRhLjguOCAwIDAgMCAuMDc0LTEuMTNsLTEuMDUtMS4yYS44MDQuODA0IDAgMCAwLTEuMTMtLjA3NGwtMy4xNCAyLjc2Vi44YS44LjggMCAwIDAtLjgtLjh6bS04IDIwLjhhLjguOCAwIDAgMC0uOC44djEuNmEuOC44IDAgMCAwIC44LjhoMTcuNmEuOC44IDAgMCAwIC44LS44di0xLjZhLjguOCAwIDAgMC0uOC0uOHoiPjwvcGF0aD4KPC9zdmc+Cg==)](https://github.com/ShadelessFox/decima/releases/latest)
[![Discord](https://img.shields.io/discord/1012475585605414983?label=Chat&logo=discord&logoColor=white)](https://discord.gg/Gt4gkMwadB)
[![Support](https://img.shields.io/badge/Support-Ko--fi-blue?logo=kofi&logoColor=white)](https://ko-fi.com/shadelessfox)

# Decima Workshop

Decima Workshop is an open-source modding tool for [games](#supported-games) powered by [Decima engine](https://en.wikipedia.org/wiki/Decima_(game_engine)).

- Browse and edit core objects with complete type information
- Preview models, textures, shaders, etc
- Export models, textures, audio, etc
- Repack archives with your changes

![](https://github.com/user-attachments/assets/78c1f23d-9028-4ee4-be26-cc5960787ab3)

### Running

1. Make sure you have **Java 17** installed. We recommend using [Adoptium](https://adoptium.net/temurin/releases/?arch=x64&version=17&package=jre)
2. Download the latest version of the application from the [releases page](https://github.com/ShadelessFox/decima/releases/latest)
3. Unzip the downloaded archive and launch using `decima.bat` on Windows or `decima` on Linux
4. For further steps, [check out the wiki](https://github.com/ShadelessFox/decima/wiki/Getting-started)

#### Nightly builds

If you want to try the latest features and improvements, you can download the latest build from the [actions page](https://github.com/ShadelessFox/decima/actions).
Click on the latest workflow run and download the artifact from the `Artifacts` section.

### Build

Open your favorite terminal app and execute the following commands in the specified order:
1. Make sure you have **Java 17** installed. We recommend using [Adoptium](https://adoptium.net/temurin/releases/?arch=x64&version=17&package=jdk)
2. Make sure you have **Git** installed. Alternatively, you can download the source code as a ZIP archive and extract it
3. Open the terminal and execute the following commands:
   1. `git clone https://github.com/ShadelessFox/decima`
   2. `cd decima`
   3. `gradlew build`

Ready-to-use distributions can be found under the `build/distributions` directory. To run the application, see [Running](#running).

## Supported games

| Game                            | Platform |
|---------------------------------|----------|
| Death Stranding                 | PC       |
| Death Stranding: Director's Cut | PC       |
| Horizon Zero Dawn               | PC       |

## License
This project is licensed under the GPL-3.0 license.

This project is not sponsored by or related to Guerrilla Games, Kojima Productions, Sony Interactive Entertainment, or others.

Source code and all software made with Decima engine belong to their developers.
