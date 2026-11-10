package com.lavagest.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.lavagest.config.AppContainer;
import com.lavagest.model.AppUser;
import com.lavagest.model.Role;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Pantalla de inicio de sesión de LavaGest PRO.
 * Redirige al MainFrame (Admin) o EncargadoFrame según el rol.
 */
public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblError;

    public LoginFrame() {
        setTitle("LavaGest PRO - Iniciar sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(420, 500);
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        setContentPane(root);

        // ---- Header ----
        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(new Color(30, 144, 255));
        header.setPreferredSize(new Dimension(420, 160));

        JLabel iconLabel = new JLabel("\uD83D\uDE97"); // 🚗
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("LavaGest PRO");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Sistema de Gestión");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(220, 235, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);
        header.add(iconLabel, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 2, 0);
        header.add(titleLabel, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
        header.add(subLabel, gbc);

        root.add(header, BorderLayout.NORTH);

        // ---- Formulario ----
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(new Color(245, 247, 250));
        form.setBorder(new EmptyBorder(32, 48, 32, 48));

        // Usuario
        JLabel lblUser = new JLabel("Usuario");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));

        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(18));

        // Contraseña
        JLabel lblPass = new JLabel("Contraseña");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblPass);
        form.add(Box.createVerticalStrut(6));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(10));

        // Error label
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(new Color(220, 50, 50));
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);
        form.add(Box.createVerticalStrut(20));

        // Botón login
        btnLogin = new JButton("Ingresar");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(30, 144, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        form.add(btnLogin);

        root.add(form, BorderLayout.CENTER);

        // ---- Footer ----
        JLabel footer = new JLabel("LavaGest PRO v2.0", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(new Color(160, 160, 160));
        footer.setBorder(new EmptyBorder(0, 0, 12, 0));
        root.add(footer, BorderLayout.SOUTH);

        // ---- Listeners ----
        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });

        KeyAdapter enterKey = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        // Foco inicial
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { txtUsername.requestFocusInWindow(); }
        });
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Ingresa usuario y contraseña.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verificando...");
        lblError.setText(" ");

        SwingWorker<AppUser, Void> worker = new SwingWorker<AppUser, Void>() {
            protected AppUser doInBackground() {
                try {
                    return AppContainer.getInstance().getAuthController().login(username, password);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Ingresar");
                try {
                    AppUser user = get();
                    if (user == null) {
                        lblError.setText("Usuario o contraseña incorrectos.");
                        txtPassword.setText("");
                        txtPassword.requestFocusInWindow();
                    } else {
                        openMainApp(user);
                    }
                } catch (Exception e) {
                    lblError.setText("Error de conexión. Intenta de nuevo.");
                }
            }
        };
        worker.execute();
    }

    private void openMainApp(AppUser user) {
        dispose(); // cierra el login
        if (user.getRole() == Role.ADMIN) {
            MainFrame frame = new MainFrame(user);
            frame.setVisible(true);
        } else {
            EncargadoFrame frame = new EncargadoFrame(user);
            frame.setVisible(true);
        }
    }
}
