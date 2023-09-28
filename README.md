[![Java](https://img.shields.io/badge/Java-≥17-orange?logo=openjdk&logoColor=white)](https://adoptium.net/temurin/releases/?package=jre&arch=x64&version=17)
[![Download](https://img.shields.io/github/v/release/ShadelessFox/decima?label=Download&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0Ij4KCTxwYXRoIGZpbGw9IndoaXRlIiBkPSJNMTEuMiAwYS44LjggMCAwIDAtLjguOHYxMS40TDcuMjYgOS40NGEuODAzLjgwMyAwIDAgMC0xLjEzLjA3NGwtMS4wNSAxLjJhLjguOCAwIDAgMCAuMDczIDEuMTNsNi4zMyA1LjU0YS43OTUuNzk1IDAgMCAwIDEuMDUgMGw2LjMyLTUuNTRhLjguOCAwIDAgMCAuMDc0LTEuMTNsLTEuMDUtMS4yYS44MDQuODA0IDAgMCAwLTEuMTMtLjA3NGwtMy4xNCAyLjc2Vi44YS44LjggMCAwIDAtLjgtLjh6bS04IDIwLjhhLjguOCAwIDAgMC0uOC44djEuNmEuOC44IDAgMCAwIC44LjhoMTcuNmEuOC44IDAgMCAwIC44LS44di0xLjZhLjguOCAwIDAgMC0uOC0uOHoiPjwvcGF0aD4KPC9zdmc+Cg==)](https://github.com/ShadelessFox/decima/releases/latest)
[![Discord](https://img.shields.io/discord/1012475585605414983?label=Chat&logo=discord&logoColor=white)](https://discord.gg/Gt4gkMwadB)

# Decima Workshop

Decima Workshop is an open-source modding tool for games powered by Decima engine. Its goal is to allow viewing, creating, and editing "packfile" archives and contained "core" files inside.

![](https://github.com/ShadelessFox/decima/assets/35821147/22a3fb92-8009-499e-bc30-583607b6ba4b)

# Supported games

| Game                            | Platform |
|---------------------------------|----------|
| Death Stranding                 | PC       |
| Death Stranding: Director's Cut | PC       |
| Horizon Zero Dawn               | PC       |

# Using the application
### Prerequisites
- Java 17 (**JRE** for [running](#running), **JDK** for [building](#building); we use and recommend [Adoptium](https://adoptium.net/))
- Git client (for [building](#building))
- Windows 10 (64-bit)

### Running
1. Download a release from the [releases](https://github.com/ShadelessFox/decima/releases) page
2. Unzip the downloaded archive
3. Launch using `bin/decima.bat`
4. For further steps, [check out the wiki](https://github.com/ShadelessFox/decima/wiki/Getting-started)

### Building
**Make sure you are using the minimum required JRE version or the build will fail**

Open your favorite terminal app and execute the following commands in the specified order:
1. `git clone https://github.com/ShadelessFox/decima`
2. `cd decima`
3. `./gradlew build`

Ready-to-use distribution can be found under the `build/distributions` directory. See [Running](#running) at step 2

# License
This project is licensed under the GPL-3.0 license.

This project is not sponsored nor related to Guerrilla Games, Kojima Productions, Sony Interactive Entertainment, and others.

Source code and all software made with Decima engine belong to their developers.
