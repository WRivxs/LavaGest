package com.lavagest.ui;

import com.lavagest.config.AppContainer;
import com.lavagest.controller.CatalogController;
import com.lavagest.controller.PayoutController;
import com.lavagest.controller.WashController;
import com.lavagest.controller.WorkerController;
import com.lavagest.dto.CreatePayoutRequest;
import com.lavagest.dto.CreateWashRequest;
import com.lavagest.model.AppUser;
import com.lavagest.model.VehicleTypeCatalog;
import com.lavagest.model.Wash;
import com.lavagest.model.WashType;
import com.lavagest.model.Worker;
import com.lavagest.model.WorkerPayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vista principal para el rol ENCARGADO.
 * Permite registrar lavados, filtrar historial por fechas/búsqueda,
 * y liquidar la jornada a los trabajadores con su respectivo historial.
 */
public class EncargadoFrame extends JFrame {

    private final AppUser currentUser;
    private final AppContainer container;
    private final WashController washController;
    private final WorkerController workerController;
    private final CatalogController catalogController;
    private final PayoutController payoutController;

    // --- Pestaña 1: Lavados ---
    private DefaultTableModel washTableModel;
    private JTable washTable;
    private JLabel lblTotalDia;
    private JLabel lblCountDia;
    private JComboBox<String> cmbDateFilter;
    private JTextField txtSearchWashes;
    private List<Wash> currentLoadedWashes = new ArrayList<Wash>();
    private Map<Long, String> cachedWorkerNames = new HashMap<Long, String>();

    // --- Pestaña 2: Liquidación de Jornada ---
    private DefaultTableModel workersPendingModel;
    private JTable workersPendingTable;
    private DefaultTableModel payoutsHistoryModel;
    private JTable payoutsHistoryTable;
    private JComboBox<String> cmbPayoutDateFilter;
    private JLabel lblTotalPayouts;
    private List<Worker> activeWorkersList = new ArrayList<Worker>();

    private static final NumberFormat COP = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    public EncargadoFrame(AppUser user) {
        this.currentUser = user;
        this.container = AppContainer.getInstance();
        this.washController = container.getWashController();
        this.workerController = container.getWorkerController();
        this.catalogController = container.getCatalogController();
        this.payoutController = container.getPayoutController();

        setTitle("LavaGest PRO - Encargado: " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(850, 560));

        buildUI();
        refreshAllData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        setContentPane(root);

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 119, 242));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("🚗 LavaGest PRO — Encargado de Turno");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        JLabel lblUser = new JLabel("👤 " + currentUser.getUsername() + " (ENCARGADO)");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(225, 238, 255));

        JButton btnLogout = new JButton("Cerrar sesión");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setOpaque(true);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            dispose();
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });

        headerRight.add(lblUser);
        headerRight.add(btnLogout);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Pestañas Centrales ----
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(new Color(245, 247, 250));

        tabbedPane.addTab("🚿 Control de Lavados", buildWashesPanel());
        tabbedPane.addTab("💵 Pago de Jornada (Liquidación)", buildPayoutsPanel());

        root.add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // PESTAÑA 1: CONTROL DE LAVADOS
    // =========================================================================
    private JPanel buildWashesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(245, 247, 250));

        // Subpanel superior: KPIs, botón + Nuevo Lavado y barra de filtros
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(Color.WHITE);
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 235)));

        // Fila 1: KPIs y acciones
        JPanel kpiRow = new JPanel(new BorderLayout());
        kpiRow.setOpaque(false);
        kpiRow.setBorder(new EmptyBorder(10, 18, 8, 18));

        JPanel kpiLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        kpiLeft.setOpaque(false);

        lblCountDia = new JLabel("0 lavados");
        lblCountDia.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCountDia.setForeground(new Color(24, 119, 242));

        lblTotalDia = new JLabel("Total Cobrado: $0");
        lblTotalDia.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotalDia.setForeground(new Color(34, 139, 34));

        kpiLeft.add(lblCountDia);
        kpiLeft.add(new JSeparator(SwingConstants.VERTICAL));
        kpiLeft.add(lblTotalDia);

        JPanel kpiRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        kpiRight.setOpaque(false);

        JButton btnNuevoLavado = new JButton("＋ Nuevo Lavado");
        btnNuevoLavado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNuevoLavado.setBackground(new Color(24, 119, 242));
        btnNuevoLavado.setForeground(Color.WHITE);
        btnNuevoLavado.setBorderPainted(false);
        btnNuevoLavado.setFocusPainted(false);
        btnNuevoLavado.setOpaque(true);
        btnNuevoLavado.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNuevoLavado.addActionListener(e -> showNuevoLavadoModal());

        JButton btnActualizar = new JButton("↻ Actualizar");
        btnActualizar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnActualizar.addActionListener(e -> loadWashesDataAsync());

        kpiRight.add(btnNuevoLavado);
        kpiRight.add(btnActualizar);

        kpiRow.add(kpiLeft, BorderLayout.WEST);
        kpiRow.add(kpiRight, BorderLayout.EAST);
        topContainer.add(kpiRow);

        // Fila 2: Filtro por Fechas y Buscador
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        filterRow.setOpaque(false);
        filterRow.setBorder(new EmptyBorder(0, 18, 10, 18));

        JLabel lblFiltro = new JLabel("Filtrar:");
        lblFiltro.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFiltro.setForeground(new Color(100, 110, 125));

        cmbDateFilter = new JComboBox<String>(new String[]{
                "📅 Hoy",
                "📅 Ayer",
                "📅 Esta Semana",
                "📅 Este Mes",
                "📅 Todos los Registros"
        });
        cmbDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbDateFilter.setPreferredSize(new Dimension(175, 30));
        cmbDateFilter.addActionListener(e -> loadWashesDataAsync());

        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBuscar.setForeground(new Color(100, 110, 125));

        txtSearchWashes = new JTextField(18);
        txtSearchWashes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearchWashes.setPreferredSize(new Dimension(210, 30));
        txtSearchWashes.putClientProperty("JTextField.placeholderText", "Placa, cliente o teléfono...");
        txtSearchWashes.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterWashesTableLocal(); }
            public void removeUpdate(DocumentEvent e) { filterWashesTableLocal(); }
            public void changedUpdate(DocumentEvent e) { filterWashesTableLocal(); }
        });

        JButton btnClearSearch = new JButton("✖");
        btnClearSearch.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnClearSearch.setToolTipText("Limpiar búsqueda");
        btnClearSearch.addActionListener(e -> txtSearchWashes.setText(""));

        filterRow.add(lblFiltro);
        filterRow.add(cmbDateFilter);
        filterRow.add(Box.createHorizontalStrut(10));
        filterRow.add(lblBuscar);
        filterRow.add(txtSearchWashes);
        filterRow.add(btnClearSearch);

        topContainer.add(filterRow);
        panel.add(topContainer, BorderLayout.NORTH);

        // Tabla de lavados
        String[] cols = {"#", "Hora", "Fecha", "Cliente", "Teléfono", "Placa", "Tipo Vehículo", "Tipo Lavado", "Trabajador", "Precio", "Estado Pago"};
        washTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        washTable = new JTable(washTableModel);
        washTable.setRowHeight(32);
        washTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        washTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        washTable.getTableHeader().setBackground(new Color(246, 248, 252));
        washTable.getTableHeader().setForeground(new Color(80, 90, 105));
        washTable.setGridColor(new Color(230, 234, 240));
        washTable.setSelectionBackground(new Color(225, 238, 255));
        washTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Anchos preferidos
        int[] widths = {35, 55, 75, 115, 95, 75, 95, 130, 115, 90, 85};
        for (int i = 0; i < widths.length && i < washTable.getColumnCount(); i++) {
            washTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer payoutStatusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    String val = value != null ? value.toString() : "";
                    if ("Liquidado".equalsIgnoreCase(val) || "Pagado".equalsIgnoreCase(val)) {
                        l.setForeground(new Color(22, 101, 52));
                    } else {
                        l.setForeground(new Color(180, 83, 9));
                    }
                }
                return l;
            }
        };

        washTable.getColumnModel().getColumn(0).setCellRenderer(center);
        washTable.getColumnModel().getColumn(1).setCellRenderer(center);
        washTable.getColumnModel().getColumn(2).setCellRenderer(center);
        washTable.getColumnModel().getColumn(4).setCellRenderer(center);
        washTable.getColumnModel().getColumn(5).setCellRenderer(center);
        washTable.getColumnModel().getColumn(6).setCellRenderer(center);
        washTable.getColumnModel().getColumn(9).setCellRenderer(right);
        washTable.getColumnModel().getColumn(10).setCellRenderer(payoutStatusRenderer);

        JScrollPane scroll = new JScrollPane(washTable);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // PESTAÑA 2: PAGO DE JORNADA (LIQUIDACIONES)
    // =========================================================================
    private JPanel buildPayoutsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(new EmptyBorder(12, 16, 14, 16));

        // Subpanel Superior: Liquidar Trabajador
        JPanel topCard = new JPanel(new BorderLayout(0, 8));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(12, 16, 12, 16)));

        JPanel topTitleRow = new JPanel(new BorderLayout());
        topTitleRow.setOpaque(false);

        JLabel lblTopTitle = new JLabel("💵 Trabajadores en Turno — Saldo de Comisiones por Liquidar");
        lblTopTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTopTitle.setForeground(new Color(24, 119, 242));

        JButton btnPaySelected = new JButton("💵 Cancelar Jornada al Trabajador");
        btnPaySelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPaySelected.setBackground(new Color(34, 139, 34));
        btnPaySelected.setForeground(Color.WHITE);
        btnPaySelected.setBorderPainted(false);
        btnPaySelected.setFocusPainted(false);
        btnPaySelected.setOpaque(true);
        btnPaySelected.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPaySelected.addActionListener(e -> handlePaySelectedWorker());

        topTitleRow.add(lblTopTitle, BorderLayout.WEST);
        topTitleRow.add(btnPaySelected, BorderLayout.EAST);
        topCard.add(topTitleRow, BorderLayout.NORTH);

        String[] workerCols = {"ID", "Trabajador", "Bahía", "Turno", "Servicios Pendientes", "Comisión Acumulada ($)"};
        workersPendingModel = new DefaultTableModel(workerCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        workersPendingTable = new JTable(workersPendingModel);
        workersPendingTable.setRowHeight(30);
        workersPendingTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        workersPendingTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        workersPendingTable.getTableHeader().setBackground(new Color(246, 248, 252));
        workersPendingTable.setSelectionBackground(new Color(225, 238, 255));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        workersPendingTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        workersPendingTable.getColumnModel().getColumn(0).setCellRenderer(center);
        workersPendingTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        workersPendingTable.getColumnModel().getColumn(2).setCellRenderer(center);
        workersPendingTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        workersPendingTable.getColumnModel().getColumn(3).setCellRenderer(center);
        workersPendingTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        workersPendingTable.getColumnModel().getColumn(4).setCellRenderer(center);
        workersPendingTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        workersPendingTable.getColumnModel().getColumn(5).setCellRenderer(right);

        // Doble clic para pagar
        workersPendingTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handlePaySelectedWorker();
                }
            }
        });

        JScrollPane scrollWorkers = new JScrollPane(workersPendingTable);
        scrollWorkers.setPreferredSize(new Dimension(0, 160));
        scrollWorkers.setBorder(BorderFactory.createLineBorder(new Color(230, 234, 240), 1));
        topCard.add(scrollWorkers, BorderLayout.CENTER);

        // Subpanel Inferior: Historial de Liquidaciones
        JPanel bottomCard = new JPanel(new BorderLayout(0, 8));
        bottomCard.setBackground(Color.WHITE);
        bottomCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(12, 16, 12, 16)));

        JPanel bottomTitleRow = new JPanel(new BorderLayout());
        bottomTitleRow.setOpaque(false);

        JLabel lblHistTitle = new JLabel("📋 Historial de Liquidaciones Entregadas en Turno");
        lblHistTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHistTitle.setForeground(new Color(70, 80, 95));

        JPanel histActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        histActions.setOpaque(false);

        lblTotalPayouts = new JLabel("Total Entregado: $0");
        lblTotalPayouts.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalPayouts.setForeground(new Color(185, 28, 28));

        cmbPayoutDateFilter = new JComboBox<String>(new String[]{
                "📅 Hoy",
                "📅 Esta Semana",
                "📅 Este Mes",
                "📅 Todas"
        });
        cmbPayoutDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbPayoutDateFilter.addActionListener(e -> loadPayoutsDataAsync());

        JButton btnRefreshPayouts = new JButton("↻ Actualizar");
        btnRefreshPayouts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshPayouts.addActionListener(e -> loadPayoutsDataAsync());

        histActions.add(lblTotalPayouts);
        histActions.add(Box.createHorizontalStrut(8));
        histActions.add(cmbPayoutDateFilter);
        histActions.add(btnRefreshPayouts);

        bottomTitleRow.add(lblHistTitle, BorderLayout.WEST);
        bottomTitleRow.add(histActions, BorderLayout.EAST);
        bottomCard.add(bottomTitleRow, BorderLayout.NORTH);

        String[] payoutCols = {"#", "Fecha", "Hora", "Trabajador", "Monto Entregado", "Método", "Notas / Detalle"};
        payoutsHistoryModel = new DefaultTableModel(payoutCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        payoutsHistoryTable = new JTable(payoutsHistoryModel);
        payoutsHistoryTable.setRowHeight(28);
        payoutsHistoryTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        payoutsHistoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        payoutsHistoryTable.getTableHeader().setBackground(new Color(246, 248, 252));
        payoutsHistoryTable.setSelectionBackground(new Color(225, 238, 255));

        payoutsHistoryTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        payoutsHistoryTable.getColumnModel().getColumn(0).setCellRenderer(center);
        payoutsHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        payoutsHistoryTable.getColumnModel().getColumn(1).setCellRenderer(center);
        payoutsHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        payoutsHistoryTable.getColumnModel().getColumn(2).setCellRenderer(center);
        payoutsHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        payoutsHistoryTable.getColumnModel().getColumn(4).setCellRenderer(right);
        payoutsHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        payoutsHistoryTable.getColumnModel().getColumn(5).setCellRenderer(center);

        JScrollPane scrollPayouts = new JScrollPane(payoutsHistoryTable);
        scrollPayouts.setBorder(BorderFactory.createLineBorder(new Color(230, 234, 240), 1));
        bottomCard.add(scrollPayouts, BorderLayout.CENTER);

        // Split en panel central
        JPanel centerSplit = new JPanel(new GridLayout(2, 1, 0, 12));
        centerSplit.setOpaque(false);
        centerSplit.add(topCard);
        centerSplit.add(bottomCard);

        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // CARGA DE DATOS ASÍNCRONA
    // =========================================================================
    private void refreshAllData() {
        loadWashesDataAsync();
        loadWorkersPendingDataAsync();
        loadPayoutsDataAsync();
    }

    private void loadWashesDataAsync() {
        String filter = (String) cmbDateFilter.getSelectedItem();
        if (filter == null) filter = "📅 Hoy";

        final String finalFilter = filter;
        new SwingWorker<List<Wash>, Void>() {
            @Override
            protected List<Wash> doInBackground() {
                LocalDate today = LocalDate.now();
                if (finalFilter.contains("Hoy")) {
                    return washController.getWashesByDate(today);
                } else if (finalFilter.contains("Ayer")) {
                    return washController.getWashesByDate(today.minusDays(1));
                } else if (finalFilter.contains("Esta Semana")) {
                    LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return washController.getWashesByDateRange(startOfWeek, today);
                } else if (finalFilter.contains("Este Mes")) {
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    return washController.getWashesByDateRange(startOfMonth, today);
                } else {
                    return washController.getAllWashes();
                }
            }

            @Override
            protected void done() {
                try {
                    currentLoadedWashes = get();
                    // Refrescar mapa de trabajadores
                    List<Worker> workers = workerController.getActiveWorkers();
                    cachedWorkerNames.clear();
                    for (Worker w : workers) {
                        cachedWorkerNames.put(w.getId(), w.getName());
                    }
                    filterWashesTableLocal();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterWashesTableLocal() {
        String query = txtSearchWashes != null ? txtSearchWashes.getText().trim().toLowerCase() : "";
        washTableModel.setRowCount(0);

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (Wash w : currentLoadedWashes) {
            String hora = w.getTime() != null ? w.getTime().toString().substring(0, 5) : "--";
            String fecha = w.getDate() != null ? w.getDate().toString() : "--";
            String cliente = w.getClientName() != null ? w.getClientName() : "";
            String telefono = w.getClientPhone() != null ? w.getClientPhone() : "";
            String placa = w.getPlate() != null ? w.getPlate() : "";
            String tipoVeh = w.getVehicleType() != null ? w.getVehicleType().getLabel() : "--";
            String tipoLav = w.getWashTypeName() != null ? w.getWashTypeName() : "--";
            String trabajador = cachedWorkerNames.containsKey(w.getWorkerId())
                    ? cachedWorkerNames.get(w.getWorkerId()) : ("ID:" + w.getWorkerId());
            String estadoPago = w.getPayoutStatus() != null ? w.getPayoutStatus().getLabel() : "Pendiente";

            // Búsqueda en texto
            if (!query.isEmpty()) {
                boolean matches = placa.toLowerCase().contains(query)
                        || cliente.toLowerCase().contains(query)
                        || telefono.toLowerCase().contains(query)
                        || trabajador.toLowerCase().contains(query);
                if (!matches) continue;
            }

            count++;
            if (w.getPrice() != null) total = total.add(w.getPrice());

            washTableModel.addRow(new Object[]{
                    count,
                    hora,
                    fecha,
                    cliente.isEmpty() ? "--" : cliente,
                    telefono.isEmpty() ? "--" : telefono,
                    placa,
                    tipoVeh,
                    tipoLav,
                    trabajador,
                    COP.format(w.getPrice()),
                    estadoPago
            });
        }

        lblCountDia.setText(count + " lavado" + (count == 1 ? "" : "s"));
        lblTotalDia.setText("Total: " + COP.format(total));
    }

    private void loadWorkersPendingDataAsync() {
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<Object[]>();
                activeWorkersList = workerController.getActiveWorkers();
                for (Worker w : activeWorkersList) {
                    BigDecimal pending = payoutController.getPendingAmount(w.getId());
                    int count = payoutController.getPendingWashesCount(w.getId());
                    rows.add(new Object[]{
                            w.getId(),
                            w.getName(),
                            w.getBay() != null ? w.getBay() : "-",
                            w.getShift() != null ? w.getShift() : "-",
                            count,
                            COP.format(pending),
                            pending // index 6 hidden value
                    });
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    workersPendingModel.setRowCount(0);
                    for (Object[] r : rows) {
                        workersPendingModel.addRow(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5]});
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadPayoutsDataAsync() {
        String filter = (String) cmbPayoutDateFilter.getSelectedItem();
        if (filter == null) filter = "📅 Hoy";

        final String finalFilter = filter;
        new SwingWorker<List<WorkerPayout>, Void>() {
            @Override
            protected List<WorkerPayout> doInBackground() {
                LocalDate today = LocalDate.now();
                if (finalFilter.contains("Hoy")) {
                    return payoutController.getPayoutsToday();
                } else if (finalFilter.contains("Esta Semana")) {
                    LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return payoutController.getPayoutsByDateRange(startOfWeek, today);
                } else if (finalFilter.contains("Este Mes")) {
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    return payoutController.getPayoutsByDateRange(startOfMonth, today);
                } else {
                    return payoutController.getAllPayouts();
                }
            }

            @Override
            protected void done() {
                try {
                    List<WorkerPayout> payouts = get();
                    payoutsHistoryModel.setRowCount(0);
                    BigDecimal total = BigDecimal.ZERO;
                    int i = 1;
                    for (WorkerPayout p : payouts) {
                        String fecha = p.getPaymentDate() != null ? p.getPaymentDate().toString() : "--";
                        String hora = p.getPaymentTime() != null ? p.getPaymentTime().toString().substring(0, 5) : "--";
                        String trabajador = cachedWorkerNames.containsKey(p.getWorkerId())
                                ? cachedWorkerNames.get(p.getWorkerId()) : ("ID:" + p.getWorkerId());
                        String metodo = p.getPaymentMethod() != null ? p.getPaymentMethod().getLabel() : "Efectivo";
                        String notas = p.getNotes() != null ? p.getNotes() : "";

                        total = total.add(p.getAmount());
                        payoutsHistoryModel.addRow(new Object[]{
                                i++,
                                fecha,
                                hora,
                                trabajador,
                                COP.format(p.getAmount()),
                                metodo,
                                notas
                        });
                    }
                    lblTotalPayouts.setText("Total Entregado: " + COP.format(total));
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================================
    // MODAL DE LIQUIDACIÓN DE JORNADA
    // =========================================================================
    private void handlePaySelectedWorker() {
        int row = workersPendingTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona un trabajador de la tabla para cancelar su jornada.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long workerId = (Long) workersPendingModel.getValueAt(row, 0);
        Worker worker = null;
        for (Worker w : activeWorkersList) {
            if (w.getId().equals(workerId)) {
                worker = w;
                break;
            }
        }
        if (worker == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el trabajador.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal pendingAmount = payoutController.getPendingAmount(workerId);
        int pendingCount = payoutController.getPendingWashesCount(workerId);

        if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this,
                    "El trabajador " + worker.getName() + " no tiene comisiones pendientes por cobrar.",
                    "Al día", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        showLiquidacionModal(worker, pendingAmount, pendingCount);
    }

    private void showLiquidacionModal(Worker worker, BigDecimal pendingAmount, int pendingCount) {
        JDialog dialog = new JDialog(this, "Cancelar Jornada — " + worker.getName(), true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Resumen
        JPanel infoCard = new JPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBackground(new Color(246, 248, 254));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 228, 245), 1),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel lblWName = new JLabel("Operador: " + worker.getName() + " (" + worker.getBay() + ")");
        lblWName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblWName.setForeground(new Color(24, 119, 242));

        JLabel lblWCount = new JLabel("Servicios pendientes de pago: " + pendingCount + " lavados");
        lblWCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblWCount.setForeground(new Color(80, 90, 105));

        JLabel lblMonto = new JLabel("Monto a Entregar: " + COP.format(pendingAmount));
        lblMonto.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblMonto.setForeground(new Color(34, 139, 34));

        infoCard.add(lblWName);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(lblWCount);
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(lblMonto);

        // Campos: Método de pago y Notas
        JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
        form.setOpaque(false);

        JComboBox<String> cmbMetodo = new JComboBox<String>(new String[]{
                "Efectivo",
                "Nequi",
                "Daviplata",
                "Transferencia"
        });
        cmbMetodo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JTextField txtNotas = new JTextField();
        txtNotas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtNotas.putClientProperty("JTextField.placeholderText", "Ej: Cierre de jornada");

        form.add(createFieldCell("Método de Pago", cmbMetodo));
        form.add(createFieldCell("Notas / Observación", txtNotas));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.add(infoCard, BorderLayout.NORTH);
        centerPanel.add(form, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnConfirmar = new JButton("✔ Confirmar Pago y Liquidar");
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConfirmar.setBackground(new Color(34, 139, 34));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setBorderPainted(false);
        btnConfirmar.setFocusPainted(false);
        btnConfirmar.setOpaque(true);
        btnConfirmar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnConfirmar.addActionListener(e -> {
            btnConfirmar.setEnabled(false);
            btnConfirmar.setText("Registrando...");

            String method = (String) cmbMetodo.getSelectedItem();
            String notes = txtNotas.getText().trim();
            if (notes.isEmpty()) notes = "Liquidación jornada " + LocalDate.now();

            final String finalNotes = notes;
            SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        CreatePayoutRequest req = new CreatePayoutRequest(
                                worker.getId(),
                                pendingAmount,
                                method,
                                finalNotes
                        );
                        payoutController.registerPayout(req);
                        return true;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    btnConfirmar.setEnabled(true);
                    try {
                        if (get()) {
                            dialog.dispose();
                            refreshAllData();
                            JOptionPane.showMessageDialog(EncargadoFrame.this,
                                    "Jornada cancelada exitosamente a " + worker.getName() + " por " + COP.format(pendingAmount),
                                    "Pago Registrado", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Error al registrar la liquidación.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ignored) {}
                }
            };
            sw.execute();
        });

        btnPanel.add(btnCancelar);
        btnPanel.add(btnConfirmar);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // =========================================================================
    // MODAL NUEVO LAVADO (4 FILAS X 2 COLUMNAS)
    // =========================================================================
    private void showNuevoLavadoModal() {
        JDialog dialog = new JDialog(this, "Nuevo Lavado — Despacho", true);
        dialog.setSize(540, 440);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(16, 22, 16, 22));
        panel.setBackground(Color.WHITE);

        // Cuadrícula 4 filas x 2 columnas
        JPanel grid = new JPanel(new GridLayout(4, 2, 14, 10));
        grid.setBackground(Color.WHITE);

        // Fila 1: Cliente y Teléfono
        JTextField txtCliente = new JTextField();
        txtCliente.putClientProperty("JTextField.placeholderText", "Nombre del cliente");
        JTextField txtTelefono = new JTextField();
        txtTelefono.putClientProperty("JTextField.placeholderText", "Ej: 3001234567 (opcional)");

        grid.add(createFieldCell("Cliente *", txtCliente));
        grid.add(createFieldCell("Teléfono (Opcional)", txtTelefono));

        // Fila 2: Placa y Tipo de Vehículo
        JTextField txtPlaca = new JTextField();
        txtPlaca.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPlaca.putClientProperty("JTextField.placeholderText", "Ej: KLR-882");
        txtPlaca.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                txtPlaca.setText(txtPlaca.getText().toUpperCase());
            }
        });

        List<VehicleTypeCatalog> vehicleTypes = catalogController.getAllVehicleTypes();
        JComboBox<VehicleTypeCatalog> cmbVehicle = new JComboBox<VehicleTypeCatalog>();
        for (VehicleTypeCatalog v : vehicleTypes) cmbVehicle.addItem(v);

        grid.add(createFieldCell("Placa del Vehículo *", txtPlaca));
        grid.add(createFieldCell("Tipo de Vehículo *", cmbVehicle));

        // Fila 3: Tipo de Lavado y Precio a Cobrar
        JComboBox<WashType> cmbWashType = new JComboBox<WashType>();
        JTextField txtPrecio = new JTextField("0");
        txtPrecio.setEditable(false);
        txtPrecio.setBackground(new Color(245, 247, 252));
        txtPrecio.setFont(new Font("Segoe UI", Font.BOLD, 13));

        grid.add(createFieldCell("Tipo de Lavado *", cmbWashType));
        grid.add(createFieldCell("Precio a Cobrar ($)", txtPrecio));

        // Fila 4: Trabajador Asignado y Detalle Informativo
        List<Worker> workers = workerController.getActiveWorkers();
        JComboBox<Worker> cmbWorker = new JComboBox<Worker>();
        for (Worker w : workers) cmbWorker.addItem(w);

        JLabel lblInfoComision = new JLabel("✔ Comisión operador: 40% automático");
        lblInfoComision.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblInfoComision.setForeground(new Color(110, 120, 135));
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        infoPanel.setOpaque(false);
        infoPanel.add(lblInfoComision);

        grid.add(createFieldCell("Trabajador Asignado *", cmbWorker));
        grid.add(createFieldCell("Información de Pago", infoPanel));

        panel.add(grid, BorderLayout.CENTER);

        // Botones abajo
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(Color.WHITE);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> dialog.dispose());

        JButton btnGuardar = new JButton("Registrar Lavado");
        btnGuardar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnGuardar.setBackground(new Color(24, 119, 242));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setOpaque(true);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnPanel.add(btnCancelar);
        btnPanel.add(btnGuardar);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);

        // ---- Lógica reactiva tipo vehículo → tipo de lavado ----
        Runnable updateWashTypes = () -> {
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
        };

        cmbVehicle.addActionListener(e -> updateWashTypes.run());
        cmbWashType.addActionListener(e -> {
            WashType sel = (WashType) cmbWashType.getSelectedItem();
            if (sel != null) txtPrecio.setText(COP.format(sel.getPrice()));
        });

        // Inicializar
        updateWashTypes.run();

        // ---- Guardar Lavado ----
        btnGuardar.addActionListener(e -> {
            String cliente = txtCliente.getText().trim();
            String telefono = txtTelefono.getText().trim();
            String placa = txtPlaca.getText().trim().toUpperCase();
            WashType washType = (WashType) cmbWashType.getSelectedItem();
            Worker worker = (Worker) cmbWorker.getSelectedItem();
            VehicleTypeCatalog vehicle = (VehicleTypeCatalog) cmbVehicle.getSelectedItem();

            if (cliente.isEmpty() && placa.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Ingresa al menos el nombre del cliente o la placa.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (washType == null) {
                JOptionPane.showMessageDialog(dialog, "Selecciona el tipo de lavado.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (worker == null) {
                JOptionPane.showMessageDialog(dialog, "Selecciona un trabajador asignado.", "Campo requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            final String finalCliente = cliente.isEmpty() ? placa : cliente;
            final String finalPlaca = placa.isEmpty() ? (finalCliente.length() <= 8 ? finalCliente.toUpperCase() : "S/P") : placa;
            final String finalTelefono = telefono.isEmpty() ? null : telefono;

            SwingWorker<Boolean, Void> saveWorker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        CreateWashRequest req = new CreateWashRequest();
                        req.setPlate(finalPlaca);
                        req.setClientName(finalCliente);
                        req.setClientPhone(finalTelefono);
                        req.setVehicleTypeCatalogId(vehicle.getId());
                        req.setVehicleTypeName(vehicle.getName());
                        req.setWashTypeId(washType.getId());
                        req.setWashTypeName(washType.getName());
                        req.setWorkerId(worker.getId());
                        req.setPrice(washType.getPrice());
                        req.setCommissionPct(vehicle != null && vehicle.getCommissionPct() != null
                                ? vehicle.getCommissionPct() : new BigDecimal("40.00"));
                        req.setCreatedByUserId(currentUser.getId());
                        washController.registerWash(req);
                        return true;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Registrar Lavado");
                    try {
                        if (get()) {
                            dialog.dispose();
                            refreshAllData();
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
        });

        dialog.setVisible(true);
    }

    private JPanel createFieldCell(String labelText, JComponent comp) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(60, 70, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 32));

        cell.add(lbl);
        cell.add(Box.createVerticalStrut(3));
        cell.add(comp);
        return cell;
    }
}
