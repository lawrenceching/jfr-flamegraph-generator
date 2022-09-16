# jfr-flamegraph-generator

Generate Flame Graph from `.jfr` file.

### Get Started

Executable jar and executable binary for Linux, Windows and macOS are provided.

Download the latest package from [GitHub Release](https://github.com/lawrenceching/jfr-flamegraph-generator/releases)

```bash
# Run as executable jar
java -jar jfr-flamegraph-generator.jar --from /path/to/your.jfr --to flamegraph.html

# Run the executable binary
./jfr-flamegraph-generator # Linux/macOS
./jfr-flamegraph-generator.exe # Windows
```

### Build

##### Windows 10/11

Open `x64 Native Tools Command Prompt` and run:
```
SET PATH=C:\path\to\graalvm\;C:\path\to\maven;%PATH%;
mvn install -Pnative
```