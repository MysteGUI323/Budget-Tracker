import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UITheme.applyDarkLookAndFeel();

        // Load saved data (theme loads here too, before splash is built)
        SaveManager.load();

        SwingUtilities.invokeLater(() -> {
            SplashScreen.show(() -> {
                BudgetApp app = new BudgetApp();
                app.setVisible(true);
            });
        });
    }
}