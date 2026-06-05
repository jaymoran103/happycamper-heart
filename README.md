# HappyCamper

HappyCamper is a roster validation app created by Jay Moran in 2025. This repository is intended as a simple access point for summer leaders, best accessed [here](https://jaymoran103.github.io/happycamper-heart/docs/)

This README is intentionally left bare. For an equivalent codebase with more thorough top-level documentation, see the [main public source code](https://github.com/jaymoran103/happycamper-public)

For the most up to date implementation (assertion-based validation checks, modern web UI, and advanced preference reporting), contact Jay directly at [jaymorandev@gmail.com](mailto:jaymorandev@gmail.com)


## Download & Install

HappyCamper runs entirely on your computer — no account, no internet connection, and no Java install required (the installer bundles everything). The installed app is named with its version — e.g. **HappyCamper-2.2** — so you can always tell which one you have.

| Your computer | Download |
|---|---|
| Mac (Apple Silicon, 2021+) | [HappyCamper-macos-arm64.dmg](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-macos-arm64.dmg) |
| Mac (Intel, pre-2021) | [HappyCamper-macos-intel.dmg](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-macos-intel.dmg) |
| Windows | [HappyCamper-windows.msi](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-windows.msi) |
| Linux (Debian/Ubuntu) | [HappyCamper-linux.deb](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-linux.deb) |

Not sure which Mac you have? Click the Apple menu → About This Mac: "Apple M1/M2/M3/M4" means Apple Silicon; "Intel" means Intel.

### Mac
1. Open the downloaded `.dmg` and drag **HappyCamper** into the **Applications** folder.
2. The first time you open it, macOS warns that it can't verify the app. This is expected for apps distributed outside the App Store — close the warning, then go to **System Settings → Privacy & Security** and click **Open Anyway**. (On macOS 14 and earlier, right-clicking the app and choosing **Open** also works.)
3. Details and screenshots in Apple's guide: [Safely open apps on your Mac](https://support.apple.com/en-us/102445).

### Windows
1. Run the downloaded `.msi` installer and click through — no administrator password needed.
2. If Windows shows a blue "Windows protected your PC" screen, click **More info → Run anyway**. (This appears because the app isn't registered with Microsoft — this is expected.)
3. Launch HappyCamper from the Start menu.

### Try it with sample data

Don't have a roster export handy? Download [HappyCamper-demo-data.zip](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-demo-data.zip) — fictional rosters sorted into guided scenarios, with a `START-HERE.txt` explaining what each one demonstrates.

**Unzip it before use** — on Windows, right-click the downloaded file and choose **Extract All**. (Double-clicking a zip only previews it; HappyCamper can't read files still inside one.)