package com.echo.feedback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Resolves the OS-native Downloads directory, where feedback files are written by default.
 *
 * Mirrors {@link com.echo.service.config.ConfigLocation} in shape: a pure resolver taking its
 * inputs as parameters, plus a production accessor that supplies the real ones. The filesystem
 * check is injected so the resolver can be tested without a real home directory.
 *
 * Unlike ConfigLocation this takes no os.name - the chain below is OS-independent, since
 * ~/Downloads is correct on macOS, Windows, and default Linux alike, and the one Linux-specific
 * input is simply absent elsewhere.
 */
public final class FeedbackLocation {

    private FeedbackLocation() {}

    /**
     * Pure: decide the downloads directory from inputs, with no filesystem access of its own.
     *
     * @param env environment variables, consulted for the Linux XDG_DOWNLOAD_DIR
     * @param home the user's home directory
     * @param isUsableDir tests whether a candidate directory exists and can be written to
     * @return the first usable candidate; never null
     */
    public static Path resolveDownloadsDir(Map<String, String> env, Path home,
                                           Predicate<Path> isUsableDir) {
        String xdg = env.get("XDG_DOWNLOAD_DIR");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg.trim());
        }
        Path downloads = home.resolve("Downloads");
        if (isUsableDir.test(downloads)) {
            return downloads;
        }
        return home;
    }

    /**
     * Production accessor: resolves against the real environment and filesystem.
     *
     * Deliberately does not read the Windows registry KNOWNFOLDERID for a relocated Downloads
     * folder - that needs JNI or a subprocess, %USERPROFILE%\Downloads is correct for nearly
     * every user, and the home fallback catches the rest.
     */
    public static Path downloadsDir() {
        return resolveDownloadsDir(System.getenv(),
                                   Path.of(System.getProperty("user.home")),
                                   p -> Files.isDirectory(p) && Files.isWritable(p));
    }
}
