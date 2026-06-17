# HappyCamper

> **Camp staff:** head to the [downloads page](https://jaymoran103.github.io/happycamper-heart/docs/) to get the latest installer. No technical setup needed. Message Jay on WhatsApp if you have questions!

HappyCamper is a Java Swing desktop app for camp roster validation. It cross-references camper and activity rosters to surface scheduling problems: activity conflicts, swim level mismatches, unfulfilled preferences, and more.

This repository serves as a **rapid-prototype distribution point** throughout the early 2026 summer season — features are shipped directly to staff for real-world feedback, with git discipline and architecture taking a back seat to iteration speed. This approach was intentional: the goal is learn fast with active users, not to build clean, optimized software. (yet)

The results of that work inform **[happycamper-v3](https://github.com/jaymoran103/happycamper-v3)**, where development continues with proper branching, PRs, and documentation.

## Download & Install

HappyCamper runs entirely on your computer — no account, no internet connection, and no Java install required (the installer bundles everything). The installed app is named with its version — e.g. **HappyCamper-2.2** — so you can always tell which one you have.

| Your computer | Download |
|---|---|
| Mac (Apple Silicon, 2021+) | [HappyCamper-macos-arm64.dmg](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-macos-arm64.dmg) |
| Mac (Intel, pre-2021) | [HappyCamper-macos-intel.dmg](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-macos-intel.dmg) |
| Windows | [HappyCamper-windows.msi](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-windows.msi) |
| Linux (Debian/Ubuntu) | [HappyCamper-linux.deb](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-linux.deb) |

### Mac
1. Open the downloaded `.dmg` and drag **HappyCamper** into the **Applications** folder.
2. The first time you open it, macOS warns that it can't verify the app. This is expected for apps distributed outside the App Store — close the warning, then go to **System Settings → Privacy & Security** and click **Open Anyway**. (On macOS 14 and earlier, right-clicking the app and choosing **Open** also works.)
3. Details and screenshots in Apple's guide: [Safely open apps on your Mac](https://support.apple.com/en-us/102445).

### Windows
1. Run the downloaded `.msi` installer and click through — no administrator password needed.
2. If Windows shows a blue "Windows protected your PC" screen, click **More info → Run anyway**. (This appears because the app isn't registered with Microsoft — this is expected.)
3. Launch HappyCamper from the Start menu.

### Demo Data

To test the app with sample data, download [HappyCamper-demo-data.zip](https://github.com/jaymoran103/happycamper-heart/releases/latest/download/HappyCamper-demo-data.zip) — anonymized rosters sorted in guided scenarios, with a `START-HERE.txt` explaining what each one demonstrates. On windows, ensure you've extracted/unzipped before trying to use the files (right-click and choose **Extract All**)
