# HelloWorld Desktop App

A Java Swing desktop application for Windows with built-in auto-update support.

---

## Deployment (Releasing a New Version)

Releases are fully automated via GitHub Actions. Every time you push a version tag, the workflow builds a Windows EXE installer and publishes it as a GitHub Release.

### Steps to release

**1. Update the default version fallback in the workflow** *(optional but recommended)*

In [`.github/workflows/release-exe.yml`](.github/workflows/release-exe.yml), update the fallback version to match the tag you are about to push:

```yaml
version="1.0.1"   # change this to your new version
```

**2. Commit your changes**

```bash
git add .
git commit -m "Release v1.0.1"
```

**3. Push a version tag**

```bash
git tag v1.0.1
git push origin main
git push origin v1.0.1
```

That's it. GitHub Actions takes over from here.

### What the workflow does automatically

| Step | What happens |
|---|---|
| Resolve version | Reads the tag name (e.g. `v1.0.1`) and strips the `v` prefix |
| Inject version | Rewrites `CURRENT_VERSION` in `HelloWorld.java` before compiling so the built app knows its own version |
| Compile | Runs `javac` and packages a JAR |
| Build EXE | Runs `jpackage` to produce a Windows installer (`.exe`) |
| Upload artifact | Saves the EXE to the Actions run for 30 days |
| Create Release | Publishes a GitHub Release with the EXE attached |

> **Note:** Running the workflow manually via the "Run workflow" button skips the release step — the EXE is only attached to a GitHub Release when triggered by a tag push.

---

## Auto-Update (How the App Updates Itself)

When the app starts, it silently checks GitHub for a newer release in a background thread. The main window appears immediately — the check never blocks startup.

### Update flow

```
App starts
    │
    ├─► Main window shown immediately
    │
    └─► Background thread checks:
        https://api.github.com/repos/suryawanshi-dipak/Desktop_app/releases/latest
            │
            ├─ No newer version found ──► nothing happens
            │
            └─ Newer version found
                    │
                    └─► Dialog: "v1.0.1 is available. Download now?"
                            │
                            ├─ No  ──► nothing happens
                            │
                            └─ Yes
                                    │
                                    ├─► Progress bar dialog (streams EXE to %TEMP%)
                                    ├─► Launches the installer (triggers UAC if needed)
                                    └─► Current app exits
```

### Version comparison

The app compares its compiled-in `CURRENT_VERSION` against the `tag_name` from the GitHub API response. Version `1.0.1` is considered newer than `1.0.0` using numeric segment comparison (`major.minor.patch`).

### Failure handling

Any error during the update check (no internet, GitHub API down, no EXE asset attached to the release) is silently ignored. The app continues running normally.

---

## Local Development

**Requirements:** JDK 21+

```bash
# Compile
mkdir -p build/classes
javac -d build/classes HelloWorld.java

# Run
java -cp build/classes HelloWorld
```
