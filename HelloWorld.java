import java.awt.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.regex.*;
import javax.swing.*;

public class HelloWorld {

    private static final String CURRENT_VERSION = "0.0.0"; // replaced at build time by CI
    private static final String GITHUB_API =
        "https://api.github.com/repos/suryawanshi-dipak/Desktop_app/releases/latest";

    // Persistent folder — survives across launches so the next startup can auto-apply
    private static final Path UPDATE_DIR = Path.of(
        System.getProperty("user.home"), "AppData", "Local", "HelloWorld", "updates");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = buildFrame();
            frame.setVisible(true);
            startUpdateCheck(frame);
        });
    }

    // ── Main UI ──────────────────────────────────────────────────────────────

    private static JFrame buildFrame() {
        JFrame frame = new JFrame("Hello World App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(30, 30, 46));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel label = new JLabel("Hello, World! 1");
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        label.setForeground(new Color(205, 214, 244));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Your first Java desktop app 3");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(108, 112, 134));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn = new JButton("Click Me!");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(new Color(137, 180, 250));
        btn.setForeground(new Color(30, 30, 46));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(120, 36));

        btn.addActionListener(e -> JOptionPane.showMessageDialog(frame,
            "Hello from Dipak!",
            "Greeting",
            JOptionPane.INFORMATION_MESSAGE));

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(sub);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(btn);

        frame.setContentPane(panel);
        return frame;
    }

    // ── Auto-update ──────────────────────────────────────────────────────────

    private static void startUpdateCheck(JFrame owner) {
        Thread t = new Thread(() -> {
            // 1. Apply a previously-downloaded update silently (no dialog)
            if (applyPendingUpdate(owner)) return;
            // 2. Check GitHub for a brand-new update
            try { checkForUpdate(owner); } catch (Exception ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * If a newer JAR was downloaded in a previous session, hot-swap it silently.
     * Returns true if a swap was initiated (caller should stop further checks).
     */
    private static boolean applyPendingUpdate(JFrame owner) {
        try {
            Path jar = UPDATE_DIR.resolve("HelloWorld-update.jar");
            Path verFile = UPDATE_DIR.resolve("version.txt");
            if (!Files.exists(jar) || !Files.exists(verFile)) return false;

            String pendingVersion = Files.readString(verFile).trim();
            if (!isNewer(pendingVersion, CURRENT_VERSION)) return false;

            hotSwap(owner, pendingVersion, jar); // silent — no confirmation dialog
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void checkForUpdate(JFrame owner) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_API))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "HelloWorld-App/" + CURRENT_VERSION)
            .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return;

        String body = resp.body();
        String tagName = extractJsonValue(body, "tag_name");
        if (tagName == null) return;

        String latest = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        if (!isNewer(latest, CURRENT_VERSION)) return;

        String downloadUrl = findJarDownloadUrl(body);
        if (downloadUrl == null) return;

        SwingUtilities.invokeLater(() -> promptUpdate(owner, latest, downloadUrl));
    }

    private static void promptUpdate(JFrame owner, String version, String downloadUrl) {
        int choice = JOptionPane.showConfirmDialog(owner,
            "<html>A new version <b>v" + version + "</b> is available!<br>"
                + "You are running: v" + CURRENT_VERSION + "<br><br>"
                + "Update now? The app will refresh automatically.</html>",
            "Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            downloadAndHotSwap(owner, version, downloadUrl);
        }
    }

    private static void downloadAndHotSwap(JFrame owner, String version, String downloadUrl) {
        owner.setTitle("Hello World App  —  Updating to v" + version + "...");

        new Thread(() -> {
            try {
                Files.createDirectories(UPDATE_DIR);
                Path jar = UPDATE_DIR.resolve("HelloWorld-update.jar");

                // Download JAR
                URL url = URI.create(downloadUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "HelloWorld-App/" + CURRENT_VERSION);
                conn.connect();

                try (var in = conn.getInputStream();
                     var out = Files.newOutputStream(jar)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }

                // Persist version so the next startup auto-applies without a dialog
                Files.writeString(UPDATE_DIR.resolve("version.txt"), version);

                hotSwap(owner, version, jar);

            } catch (IOException | ReflectiveOperationException ex) {
                SwingUtilities.invokeLater(() -> {
                    owner.setTitle("Hello World App");
                    JOptionPane.showMessageDialog(owner,
                        "Update failed:\n" + ex.getMessage(),
                        "Update Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "hw-update-thread").start();
    }

    private static void hotSwap(JFrame owner, String version, Path jar)
            throws IOException, ReflectiveOperationException {
        URLClassLoader loader = new URLClassLoader(
            new URL[]{jar.toUri().toURL()},
            ClassLoader.getPlatformClassLoader()
        ) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("HelloWorld".equals(name)) {
                    try { return findClass(name); }
                    catch (ClassNotFoundException ignored) {}
                }
                return super.loadClass(name);
            }
        };
        // Loader must stay open while the new class runs (lambdas load lazily).
        // Close it only when the JVM exits.
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> { try { loader.close(); } catch (IOException ignored) {} })
        );

        Class<?> newClass = loader.loadClass("HelloWorld");
        Method mainMethod = newClass.getMethod("main", String[].class);

        SwingUtilities.invokeLater(() -> {
            try {
                if (owner != null) owner.dispose();
                mainMethod.invoke(null, (Object) new String[]{});
            } catch (ReflectiveOperationException e) {
                JOptionPane.showMessageDialog(null,
                    "Hot-reload failed:\n" + e.getMessage(),
                    "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String extractJsonValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String findJarDownloadUrl(String json) {
        Matcher m = Pattern.compile(
            "\"browser_download_url\"\\s*:\\s*\"([^\"]+/HelloWorld[^\"]+\\.jar)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isNewer(String latest, String current) {
        int[] l = parseVersion(latest);
        int[] c = parseVersion(current);
        for (int i = 0; i < Math.max(l.length, c.length); i++) {
            int lv = i < l.length ? l[i] : 0;
            int cv = i < c.length ? c[i] : 0;
            if (lv > cv) return true;
            if (lv < cv) return false;
        }
        return false;
    }

    private static int[] parseVersion(String v) {
        String[] parts = v.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { nums[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", "")); }
            catch (NumberFormatException ignored) { nums[i] = 0; }
        }
        return nums;
    }
}
