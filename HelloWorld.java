import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class HelloWorld {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
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

            btn.addActionListener(e -> {
                JOptionPane.showMessageDialog(frame,
                    "Hello from Java Swing! 🎉",
                    "Greeting",
                    JOptionPane.INFORMATION_MESSAGE);
            });

            panel.add(label);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(sub);
            panel.add(Box.createRigidArea(new Dimension(0, 30)));
            panel.add(btn);

            frame.setContentPane(panel);
            frame.setVisible(true);
        });
    }
}
