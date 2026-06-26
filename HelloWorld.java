import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.regex.*;

public class HelloWorld {

    private static final String CURRENT_VERSION = "0.0.0"; // replaced at build time by CI
    private static final String GITHUB_API =
        "https://api.github.com/repos/suryawanshi-dipak/Desktop_app/releases/latest";

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

        JLabel label = new JLabel("Hello, World!");
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        label.setForeground(new Color(205, 214, 244));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Your first Java desktop app");
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
            "Hello from Java Swing!",
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
            try {
                checkForUpdate(owner);
            } catch (Exception ignored) {
                // Never crash the app due to a failed update check
            }
        });
        t.setDaemon(true);
        t.start();
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

        String downloadUrl = findExeDownloadUrl(body);
        if (downloadUrl == null) return;

        SwingUtilities.invokeLater(() -> promptUpdate(owner, latest, downloadUrl));
    }

    private static void promptUpdate(JFrame owner, String version, String downloadUrl) {
        int choice = JOptionPane.showConfirmDialog(owner,
            "<html>A new version <b>v" + version + "</b> is available!<br>"
                + "You are running: v" + CURRENT_VERSION + "<br><br>"
                + "Download and install now?</html>",
            "Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            downloadAndInstall(owner, version, downloadUrl);
        }
    }

    private static void downloadAndInstall(JFrame owner, String version, String downloadUrl) {
        JDialog dlg = new JDialog(owner, "Downloading Update", true);
        dlg.setSize(360, 110);
        dlg.setLocationRelativeTo(owner);
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel statusLabel = new JLabel("Connecting...");
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        dlg.setContentPane(panel);

        Thread downloadThread = new Thread(() -> {
            try {
                Path tempDir = Files.createTempDirectory("hw-update-");
                Path installer = tempDir.resolve("HelloWorld-" + version + "-setup.exe");

                SwingUtilities.invokeLater(() -> statusLabel.setText("Downloading v" + version + "..."));

                URL url = URI.create(downloadUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "HelloWorld-App/" + CURRENT_VERSION);
                conn.connect();
                int total = conn.getContentLength();

                try (var in = conn.getInputStream();
                     var out = Files.newOutputStream(installer)) {
                    byte[] buf = new byte[8192];
                    int read;
                    long downloaded = 0;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        downloaded += read;
                        if (total > 0) {
                            int pct = (int) (downloaded * 100L / total);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(pct));
                        }
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Launching installer...");
                    progressBar.setValue(100);
                });

                // Launch installer (cmd /c triggers UAC elevation if required)
                new ProcessBuilder("cmd", "/c", installer.toAbsolutePath().toString()).start();

                SwingUtilities.invokeLater(() -> {
                    dlg.dispose();
                    System.exit(0);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    dlg.dispose();
                    JOptionPane.showMessageDialog(owner,
                        "Update failed:\n" + ex.getMessage(),
                        "Update Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
        downloadThread.setDaemon(true);
        downloadThread.start();

        dlg.setVisible(true); // modal — blocks EDT; background thread drives it via invokeLater
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String extractJsonValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String findExeDownloadUrl(String json) {
        Matcher m = Pattern.compile(
            "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.exe)\"").matcher(json);
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
