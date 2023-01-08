# Decima Workshop

An open-source GUI application for viewing and editing (for educational purposes) packfile archives from games powered by Decima engine.
It's a successor of [Project Decima](https://github.com/REDxEYE/ProjectDecima).

![](https://user-images.githubusercontent.com/35821147/194948290-bca7da81-2ca0-4c6d-a7f2-91df27e88b99.png)

# Supported games
|Name|Platform|
|---|---|
|Death Stranding|PC|
|Death Stranding: Director's Cut|PC|
|Horizon Zero Dawn|PC|

# Using the application
### Prerequisites
- Java 17 (**JRE** for [running](#running), **JDK** for [building](#building); we use and recommend [OpenJDK](https://adoptium.net/))
- Git client (for [building](#building))
- Windows

### Running
1. Download a release from the [releases](https://github.com/ShadelessFox/decima/releases) page
1. Unzip the downloaded archive
1. Run `bin/decima.bat`

### Building
Open the command line (`Win+R` &rArr; `cmd`) and execute the following commands in the specified order:
1. `git clone https://github.com/ShadelessFox/decima`
1. `cd decima`
1. `gradlew build`

Ready-to-use binaries can be found under the `build/distributions` directory. See [Running](#running) at step 2

# License
This project is licensed under the GPL-3.0 license.

This project is not sponsored nor related to Guerrilla Games, Kojima Productions, and others.

Source code and all software made with Decima engine belong to their developers.
