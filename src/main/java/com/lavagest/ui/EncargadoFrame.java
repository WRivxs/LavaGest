package com.lavagest.ui;

import com.lavagest.config.AppContainer;
import com.lavagest.controller.CatalogController;
import com.lavagest.controller.WashController;
import com.lavagest.controller.WorkerController;
import com.lavagest.model.AppUser;
import com.lavagest.model.VehicleTypeCatalog;
import com.lavagest.model.WashType;
import com.lavagest.model.Worker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/**
 * Vista principal para el rol ENCARGADO.
 * Permite ver sus lavados del día y registrar nuevos lavados.
 */
public class EncargadoFrame extends JFrame {

    private final AppUser currentUser;
    private final AppContainer container;
    private final WashController washController;
    private final WorkerController workerController;
    private final CatalogController catalogController;

    private DefaultTableModel tableModel;
    private JTable washTable;
    private JLabel lblTotalDia;
    private JLabel lblCountDia;

    private static final NumberFormat COP = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    public EncargadoFrame(AppUser user) {
        this.currentUser = user;
        this.container = AppContainer.getInstance();
        this.washController = container.getWashController();
        this.workerController = container.getWorkerController();
        this.catalogController = container.getCatalogController();

        setTitle("LavaGest PRO - Encargado: " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(750, 500));

        buildUI();
        loadTodayWashes();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        setContentPane(root);

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 144, 255));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("🚗 LavaGest PRO — Encargado");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JLabel lblUser = new JLabel("👤 " + currentUser.getUsername());
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblUser.setForeground(new Color(220, 235, 255));

        JButton btnLogout = new JButton("Cerrar sesión");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnLogout.setBackground(new Color(220, 50, 50));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setOpaque(true);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            }
        });

        headerRight.add(lblUser);
        headerRight.add(btnLogout);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Panel central ----
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(new Color(245, 247, 250));
        root.add(center, BorderLayout.CENTER);

        // KPI bar
        JPanel kpiBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
        kpiBar.setBackground(Color.WHITE);
        kpiBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 235)));

        lblCountDia = new JLabel("0 lavados hoy");
        lblCountDia.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCountDia.setForeground(new Color(30, 144, 255));

        lblTotalDia = new JLabel("Total: $0");
        lblTotalDia.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotalDia.setForeground(new Color(34, 139, 34));

        JButton btnNuevoLavado = new JButton("+ Nuevo Lavado");
        btnNuevoLavado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNuevoLavado.setBackground(new Color(30, 144, 255));
        btnNuevoLavado.setForeground(Color.WHITE);
        btnNuevoLavado.setBorderPainted(false);
        btnNuevoLavado.setFocusPainted(false);
        btnNuevoLavado.setOpaque(true);
        btnNuevoLavado.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevoLavado.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showNuevoLavadoModal();
            }
        });

        JButton btnActualizar = new JButton("↻ Actualizar");
        btnActualizar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnActualizar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { loadTodayWashes(); }
        });

        kpiBar.add(lblCountDia);
        kpiBar.add(new JSeparator(SwingConstants.VERTICAL));
        kpiBar.add(lblTotalDia);
        kpiBar.add(Box.createHorizontalStrut(20));
        kpiBar.add(btnNuevoLavado);
        kpiBar.add(btnActualizar);

        center.add(kpiBar, BorderLayout.NORTH);

        // Tabla de lavados
        String[] cols = {"#", "Hora", "Cliente", "Tipo Vehículo", "Tipo Lavado", "Trabajador", "Precio"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        washTable = new JTable(tableModel);
        washTable.setRowHeight(32);
        washTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        washTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        washTable.setGridColor(new Color(230, 234, 240));
        washTable.setSelectionBackground(new Color(210, 230, 255));

        // Anchos de columna
        int[] widths = {40, 60, 130, 110, 140, 130, 90};
        for (int i = 0; i < widths.length; i++) {
            washTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(washTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        center.add(scroll, BorderLayout.CENTER);
    }

    private void loadTodayWashes() {
        SwingWorker<Object[][], Void> worker = new SwingWorker<Object[][], Void>() {
            protected Object[][] doInBackground() {
                try {
                    List<com.lavagest.model.Wash> washes = washController.getWashesByDate(LocalDate.now());
                    List<Worker> workers = workerController.getActiveWorkers();

                    // Crear mapa de worker id -> nombre
                    java.util.Map<Long, String> workerNames = new java.util.HashMap<Long, String>();
                    for (Worker w : workers) {
                        workerNames.put(w.getId(), w.getName());
                    }

                    Object[][] rows = new Object[washes.size()][7];
                    BigDecimal total = BigDecimal.ZERO;
                    for (int i = 0; i < washes.size(); i++) {
                        com.lavagest.model.Wash w = washes.get(i);
                        String hora = w.getTime() != null ? w.getTime().toString().substring(0, 5) : "--";
                        String cliente = w.getClientName() != null ? w.getClientName() : w.getPlate();
                        String tipoVehiculo = w.getVehicleType() != null ? w.getVehicleType().getLabel() : "--";
                        String tipoLavado = w.getWashTypeName() != null ? w.getWashTypeName() : "--";
                        String trabajador = workerNames.containsKey(w.getWorkerId())
                                ? workerNames.get(w.getWorkerId()) : "ID:" + w.getWorkerId();

                        rows[i] = new Object[]{
                            (i + 1),
                            hora,
                            cliente,
                            tipoVehiculo,
                            tipoLavado,
                            trabajador,
                            COP.format(w.getPrice())
                        };
                        total = total.add(w.getPrice());
                    }

                    // Guardamos total y count en un wrapper
                    final BigDecimal finalTotal = total;
                    final int count = washes.size();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            lblCountDia.setText(count + " lavado" + (count != 1 ? "s" : "") + " hoy");
                            lblTotalDia.setText("Total: " + COP.format(finalTotal));
                        }
                    });
                    return rows;
                } catch (Exception e) {
                    return new Object[0][0];
                }
            }

            protected void done() {
                try {
                    Object[][] rows = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void showNuevoLavadoModal() {
        JDialog dialog = new JDialog(this, "Nuevo Lavado", true);
        dialog.setSize(480, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));
        panel.setBackground(Color.WHITE);

        // Cliente
        addLabel(panel, "Cliente / Placa");
        JTextField txtCliente = new JTextField();
        txtCliente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.add(txtCliente);
        panel.add(Box.createVerticalStrut(14));

        // Tipo de Vehículo
        addLabel(panel, "Tipo de Vehículo");
        List<VehicleTypeCatalog> vehicleTypes = catalogController.getAllVehicleTypes();
        JComboBox<VehicleTypeCatalog> cmbVehicle = new JComboBox<VehicleTypeCatalog>();
        for (VehicleTypeCatalog v : vehicleTypes) cmbVehicle.addItem(v);
        cmbVehicle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.add(cmbVehicle);
        panel.add(Box.createVerticalStrut(14));

        // Tipo de Lavado (filtrado por vehículo)
        addLabel(panel, "Tipo de Lavado");
        JComboBox<WashType> cmbWashType = new JComboBox<WashType>();
        cmbWashType.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.add(cmbWashType);
        panel.add(Box.createVerticalStrut(14));

        // Precio (auto-llenado)
        addLabel(panel, "Precio");
        JTextField txtPrecio = new JTextField("0");
        txtPrecio.setEditable(false);
        txtPrecio.setBackground(new Color(240, 245, 255));
        txtPrecio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.add(txtPrecio);
        panel.add(Box.createVerticalStrut(14));

        // Trabajador
        addLabel(panel, "Trabajador");
        List<Worker> workers = workerController.getActiveWorkers();
        JComboBox<Worker> cmbWorker = new JComboBox<Worker>();
        for (Worker w : workers) cmbWorker.addItem(w);
        cmbWorker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.add(cmbWorker);
        panel.add(Box.createVerticalStrut(20));

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnCancelar = new JButton("Cancelar");
        JButton btnGuardar = new JButton("Registrar Lavado");
        btnGuardar.setBackground(new Color(30, 144, 255));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setOpaque(true);
        btnGuardar.setFocusPainted(false);

        btnPanel.add(btnCancelar);
        btnPanel.add(btnGuardar);
        panel.add(btnPanel);

        dialog.setContentPane(panel);

        // ---- Lógica de filtro vehículo → tipos de lavado ----
        Runnable updateWashTypes = new Runnable() {
            public void run() {
                VehicleTypeCatalog selected = (VehicleTypeCatalog) cmbVehicle.getSelectedItem();
                cmbWashType.removeAllItems();
                txtPrecio.setText("0");
                if (selected == null) return;
                List<WashType> types = catalogController.getWashTypesByVehicleType(selected.getId());
                for (WashType wt : types) cmbWashType.addItem(wt);
                if (cmbWashType.getItemCount() > 0) {
                    WashType first = (WashType) cmbWashType.getItemAt(0);
                    txtPrecio.setText(COP.format(first.getPrice()));
                }
            }
        };

        cmbVehicle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateWashTypes.run(); }
        });

        cmbWashType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                WashType sel = (WashType) cmbWashType.getSelectedItem();
                if (sel != null) txtPrecio.setText(COP.format(sel.getPrice()));
            }
        });

        // Cargar tipos inicial
        updateWashTypes.run();

        // ---- Acción guardar ----
        btnGuardar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cliente = txtCliente.getText().trim();
                WashType washType = (WashType) cmbWashType.getSelectedItem();
                Worker worker = (Worker) cmbWorker.getSelectedItem();
                VehicleTypeCatalog vehicle = (VehicleTypeCatalog) cmbVehicle.getSelectedItem();

                if (cliente.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Ingresa el nombre del cliente o placa.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (washType == null) {
                    JOptionPane.showMessageDialog(dialog, "Selecciona el tipo de lavado.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (worker == null) {
                    JOptionPane.showMessageDialog(dialog, "Selecciona un trabajador.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                btnGuardar.setEnabled(false);
                btnGuardar.setText("Guardando...");

                final WashType finalWashType = washType;
                final Worker finalWorker = worker;
                final VehicleTypeCatalog finalVehicle = vehicle;
                final String finalCliente = cliente;

                SwingWorker<Boolean, Void> saveWorker = new SwingWorker<Boolean, Void>() {
                    protected Boolean doInBackground() {
                        try {
                            com.lavagest.dto.CreateWashRequest req = new com.lavagest.dto.CreateWashRequest();
                            req.setPlate(finalCliente.length() <= 8 ? finalCliente.toUpperCase() : "S/P");
                            req.setClientName(finalCliente);
                            req.setVehicleTypeCatalogId(finalVehicle.getId());
                            req.setVehicleTypeName(finalVehicle.getName());
                            req.setWashTypeId(finalWashType.getId());
                            req.setWashTypeName(finalWashType.getName());
                            req.setWorkerId(finalWorker.getId());
                            req.setPrice(finalWashType.getPrice());
                            req.setCommissionPct(new BigDecimal("40.00"));
                            req.setCreatedByUserId(currentUser.getId());
                            washController.registerWash(req);
                            return true;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return false;
                        }
                    }

                    protected void done() {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("Registrar Lavado");
                        try {
                            if (get()) {
                                dialog.dispose();
                                loadTodayWashes();
                                JOptionPane.showMessageDialog(EncargadoFrame.this,
                                    "Lavado registrado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(dialog,
                                    "Error al guardar el lavado.", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception ignored) {}
                    }
                };
                saveWorker.execute();
            }
        });

        btnCancelar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dialog.dispose(); }
        });

        dialog.setVisible(true);
    }

    private void addLabel(JPanel panel, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(4));
    }
}
