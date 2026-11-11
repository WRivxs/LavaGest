package com.lavagest.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.lavagest.config.AppContainer;
import com.lavagest.controller.AuthController;
import com.lavagest.controller.CatalogController;
import com.lavagest.controller.DashboardController;
import com.lavagest.controller.PayoutController;
import com.lavagest.controller.ReportController;
import com.lavagest.controller.WashController;
import com.lavagest.controller.WorkerController;
import com.lavagest.dto.CreatePayoutRequest;
import com.lavagest.dto.CreateWashRequest;
import com.lavagest.dto.DashboardMetricsDTO;
import com.lavagest.dto.DashboardMetricsDTO.DayTrendDTO;
import com.lavagest.dto.FinancialReportDTO;
import com.lavagest.dto.WashTableRowDTO;
import com.lavagest.dto.WorkerKpiDTO;
import com.lavagest.model.AppUser;
import com.lavagest.model.Role;
import com.lavagest.model.ServiceRate;
import com.lavagest.model.VehicleType;
import com.lavagest.model.VehicleTypeCatalog;
import com.lavagest.model.WashType;
import com.lavagest.model.Worker;
import com.lavagest.model.WorkerPayout;
import com.lavagest.repository.ServiceRateRepository;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.lavagest.service.DashboardService;

import com.lavagest.model.AppUser;

/**
 * Ventana Principal de LavaGest PRO:
 * - Dashboard limpio y espacioso (sin el formulario en el panel central).
 * - Modal "Nuevo Lavado" interactivo con lista de selección con checks de operarios.
 * - Rendimiento optimizado y cero scroll horizontal.
 */
public class MainFrame extends JFrame {

    public static final Color BG = new Color(250, 248, 255);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(218, 226, 253);
    public static final Color PRIMARY = new Color(0, 97, 148);
    public static final Color SECONDARY = new Color(0, 104, 122);
    public static final Color GREEN = new Color(0, 105, 71);
    public static final Color TEXT = new Color(19, 27, 46);
    public static final Color MUTED = new Color(63, 72, 80);
    public static final Color MUTED_LIGHT = new Color(112, 120, 129);

    private static final DecimalFormat CURRENCY_FMT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FMT = new DecimalFormat("$#,##0", symbols);
    }

    private final AppContainer container;
    private final DashboardController dashboardController;
    private final WashController washController;
    private final WorkerController workerController;
    private final PayoutController payoutController;
    private final ReportController reportController;
    private final ServiceRateRepository serviceRateRepository;
    private final CatalogController catalogController;
    private final AuthController authController;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final List<JButton> navButtons = new ArrayList<JButton>();
    private final Set<String> loadedViews = new HashSet<String>();

    private DashboardController.Period currentPeriod = DashboardController.Period.HOY;

    // Componentes del Dashboard
    private JLabel clockLabel;
    private JLabel dbStatusBadge;
    private JLabel kpiFormulaIncome;
    private JLabel kpiFormulaPayout;
    private JLabel kpiFormulaProfit;

    private JLabel kpiWashesCount;
    private JLabel kpiWashesSubtitle;
    private JLabel kpiIncomeTotal;
    private JLabel kpiIncomeSubtitle;
    private JLabel kpiPayoutsTotal;
    private JLabel kpiPayoutsSubtitle;
    private JLabel kpiProfitTotal;
    private JLabel kpiProfitSubtitle;

    // Componentes de Rendimiento
    private JPanel operationalRatioBar;
    private int ratioCarsCount = 0;
    private int ratioTotalWashes = 0;
    private JLabel ratioCarrosLabel;
    private JLabel ratioMotosLabel;
    private JLabel ratioVehiclesTotalLabel;
    private JPanel weeklyBarsContainer;
    private JPanel workerCardsContainer;

    // Tablas
    private DefaultTableModel recentWashesModel;
    private JTable recentWashesTable;
    private DefaultTableModel allWashesModel;
    private DefaultTableModel workersTableModel;
    private JTable workersTable;
    private DefaultTableModel payoutsTableModel;
    private DefaultTableModel dailyReportsModel;
    private DefaultTableModel vehicleCatalogModel;
    private DefaultTableModel washTypeCatalogModel;
    private DefaultTableModel usersTableModel;
    private JComboBox<VehicleTypeCatalog> cmbCatalogVehicles;
    private JComboBox<VehicleTypeCatalog> cmbCommissionVehicle;
    private List<VehicleTypeCatalog> adminVehiclesList = new ArrayList<VehicleTypeCatalog>();
    private List<WashType> adminWashTypesList = new ArrayList<WashType>();

    // Filtros Historial Admin
    private JComboBox<String> cmbHistoryDateFilter;
    private JTextField txtSearchHistory;
    private JLabel lblHistoryCount;
    private JLabel lblHistoryTotal;
    private List<WashTableRowDTO> currentLoadedHistory = new ArrayList<WashTableRowDTO>();

    // Filtros Liquidación Admin
    private JComboBox<String> cmbPayoutDateFilterAdmin;
    private JTextField txtSearchPayoutsAdmin;
    private JLabel lblTotalPayoutsAdmin;
    private JLabel lblCountPayoutsAdmin;
    private List<WorkerPayout> currentLoadedPayoutsAdmin = new ArrayList<WorkerPayout>();

    // Módulo Reportes Financieros
    private JComboBox<String> cmbReportPeriodFilter;
    private JLabel repKpiIncomeTotal;
    private JLabel repKpiIncomeSub;
    private JLabel repKpiCommissionsTotal;
    private JLabel repKpiCommissionsSub;
    private JLabel repKpiProfitTotal;
    private JLabel repKpiProfitSub;
    private JLabel repKpiMarginTotal;
    private JLabel repKpiMarginSub;
    private JLabel repKpiTicketAvg;
    private JLabel repKpiTicketSub;
    private JPanel repChartTrendPanel;
    private JPanel repChartVehiclePanel;
    private JLabel repTableSummaryLabel;

    // Usuarios Admin
    private List<AppUser> allUsersList = new ArrayList<AppUser>();
    private JTable usersTable;

    // Modal Nuevo Lavado
    private VehicleType selectedVehicleType = VehicleType.CARRO;
    private JButton btnSelectCarro;
    private JButton btnSelectMoto;
    private JLabel previewPriceLabel;
    private JLabel previewPaymentLabel;
    private JLabel previewProfitLabel;

    private AppUser currentUser;

    public MainFrame() {
        this(null);
    }

    public MainFrame(AppUser user) {
        this.currentUser = user;
        this.container = AppContainer.getInstance();
        this.dashboardController = container.getDashboardController();
        this.washController = container.getWashController();
        this.workerController = container.getWorkerController();
        this.payoutController = container.getPayoutController();
        this.reportController = container.getReportController();
        this.serviceRateRepository = container.getServiceRateRepository();
        this.catalogController = container.getCatalogController();
        this.authController = container.getAuthController();

        initLookAndFeel();

        setTitle("LavaGest PRO - Control Operativo y Finanzas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 740);
        setMinimumSize(new Dimension(1020, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setBackground(BG);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        contentPanel.add(buildDashboardView(), "dashboard");
        contentPanel.add(buildWorkersView(), "workers");
        contentPanel.add(buildHistoryView(), "history");
        contentPanel.add(buildPayoutsView(), "payouts");
        contentPanel.add(buildReportsView(), "reports");
        contentPanel.add(buildCatalogManagementView(), "catalogs");
        contentPanel.add(buildUserManagementView(), "users");
        contentPanel.add(buildConfigView(), "config");

        showView("dashboard");

        Timer timer = new Timer(1000, e -> updateClock());
        timer.start();
    }

    private void initLookAndFeel() {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 6);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // SIDEBAR COMPACTO (210px de ancho)
    // =========================================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(246, 248, 252));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
        sidebar.setPreferredSize(new Dimension(210, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        // Logo
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        brandPanel.setOpaque(false);
        brandPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel("◫");
        logoIcon.setFont(new Font("SansSerif", Font.BOLD, 22));
        logoIcon.setForeground(PRIMARY);

        JLabel brandTitle = new JLabel("LavaGest");
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        brandTitle.setForeground(TEXT);

        JLabel proBadge = new JLabel("PRO");
        proBadge.setOpaque(true);
        proBadge.setBackground(new Color(218, 235, 254));
        proBadge.setForeground(PRIMARY);
        proBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        proBadge.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        brandPanel.add(logoIcon);
        brandPanel.add(brandTitle);
        brandPanel.add(proBadge);

        JLabel subtitle = new JLabel("Control Operativo & Caja");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(MUTED_LIGHT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(2, 28, 12, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Botón CTA Principal "+ Nuevo Lavado" -> Abre el Modal
        JButton btnQuickNew = createPrimaryButton("＋ Nuevo Lavado", 36);
        btnQuickNew.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnQuickNew.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnQuickNew.addActionListener(e -> showNewWashModal());

        JPanel navMenu = new JPanel();
        navMenu.setOpaque(false);
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));
        navMenu.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        navMenu.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] navLabels = {
                "Dashboard Operativo",
                "Trabajadores",
                "Historial de Lavados",
                "Liquidación / Nómina",
                "Reportes Financieros",
                "Tipos de Lavado",
                "Encargados y Accesos",
                "Configuración"
        };
        String[] navKeys = {"dashboard", "workers", "history", "payouts", "reports", "catalogs", "users", "config"};
        String[] navIcons = {"▣", "▦", "☰", "◷", "▤", "🏷", "👥", "⚙"};

        for (int i = 0; i < navLabels.length; i++) {
            final String key = navKeys[i];
            JButton btn = createNavButton(navIcons[i], navLabels[i], i == 0);
            btn.addActionListener(e -> showView(key));
            navButtons.add(btn);
            navMenu.add(btn);
            navMenu.add(Box.createVerticalStrut(3));
        }

        content.add(brandPanel);
        content.add(subtitle);
        content.add(btnQuickNew);
        content.add(navMenu);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel turnoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        turnoRow.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setForeground(GREEN);
        dot.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel turnoLabel = new JLabel("TURNO ACTIVO");
        turnoLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        turnoLabel.setForeground(TEXT);
        turnoRow.add(dot);
        turnoRow.add(turnoLabel);

        dbStatusBadge = new JLabel(container.isConnectedToDatabase() ? "☁ Supabase" : "⚡ En Memoria");
        dbStatusBadge.setFont(new Font("SansSerif", Font.PLAIN, 10));
        dbStatusBadge.setForeground(container.isConnectedToDatabase() ? GREEN : PRIMARY);

        footer.add(turnoRow, BorderLayout.WEST);
        footer.add(dbStatusBadge, BorderLayout.EAST);

        sidebar.add(content, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    // =========================================================================
    // TOPBAR COMPACTA
    // =========================================================================
    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(SURFACE);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        topBar.setPreferredSize(new Dimension(0, 54));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);
        JTextField txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(240, 32));
        txtSearch.putClientProperty("JTextField.placeholderText", "🔍 Buscar por placa...");
        txtSearch.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterBySearch(txtSearch.getText().trim());
            }
        });
        left.add(txtSearch);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        right.setOpaque(false);

        right.add(createPeriodChip("Hoy", DashboardController.Period.HOY, true));
        right.add(createPeriodChip("Ayer", DashboardController.Period.AYER, false));
        right.add(createPeriodChip("Esta Semana", DashboardController.Period.ESTA_SEMANA, false));
        right.add(createPeriodChip("Este Mes", DashboardController.Period.ESTE_MES, false));

        // Botón superior de registrar lavado
        JButton btnTopNew = createPrimaryButton("＋ Registrar Lavado", 32);
        btnTopNew.addActionListener(e -> showNewWashModal());
        right.add(btnTopNew);

        clockLabel = new JLabel("🕒 --:--:--");
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        clockLabel.setForeground(MUTED);
        clockLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        right.add(clockLabel);

        JLabel userBadge = new JLabel("👤 " + (currentUser != null ? currentUser.getUsername() : "admin") + " (ADMIN)");
        userBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        userBadge.setForeground(PRIMARY);
        userBadge.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        right.add(userBadge);

        JButton btnLogout = new JButton("Salir");
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnLogout.setBackground(new Color(254, 237, 237));
        btnLogout.setForeground(new Color(192, 40, 40));
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(248, 180, 180), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        right.add(btnLogout);

        topBar.add(left, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    private JButton createPeriodChip(String label, DashboardController.Period period, boolean selected) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyChipStyle(btn, selected);
        btn.addActionListener(e -> {
            this.currentPeriod = period;
            loadDashboardDataAsync();
        });
        return btn;
    }

    private void applyChipStyle(JButton btn, boolean selected) {
        btn.setBackground(selected ? PRIMARY : new Color(244, 246, 252));
        btn.setForeground(selected ? Color.WHITE : MUTED);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? PRIMARY : BORDER, 1),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)));
    }

    // =========================================================================
    // VISTA DASHBOARD (LIMPIO, ESPACIOSO Y SIN SCROLL HORIZONTAL)
    // =========================================================================
    private JScrollPane buildDashboardView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));

        panel.add(buildOperationalBanner());
        panel.add(Box.createVerticalStrut(12));

        panel.add(buildKpisRow());
        panel.add(Box.createVerticalStrut(14));

        // Fila Central: 2 Tarjetas elegantes (Rendimiento Operativo y Operadores en Turno)
        JPanel middleRow = new JPanel(new GridLayout(1, 2, 14, 0));
        middleRow.setOpaque(false);
        middleRow.add(buildOperationalRatioAndTrendCard());
        middleRow.add(buildWorkersShiftCard());

        panel.add(middleRow);
        panel.add(Box.createVerticalStrut(16));

        panel.add(buildRecentWashesSection());

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JPanel buildOperationalBanner() {
        JPanel banner = new JPanel(new BorderLayout(14, 0));
        banner.setBackground(SURFACE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Bienvenido, Administrador");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT);
        JLabel tag = new JLabel("Jornada Regular");
        tag.setOpaque(true);
        tag.setBackground(new Color(236, 244, 255));
        tag.setForeground(PRIMARY);
        tag.setFont(new Font("SansSerif", Font.BOLD, 10));
        tag.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        titleRow.add(title);
        titleRow.add(tag);

        JLabel desc = new JLabel("Supervisión operativa y balance de caja en tiempo real.");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        desc.setForeground(MUTED);

        left.add(titleRow);
        left.add(desc);

        JPanel formulaCard = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        formulaCard.setOpaque(false);

        kpiFormulaIncome = new JLabel("$0");
        kpiFormulaIncome.setFont(new Font("SansSerif", Font.BOLD, 13));
        kpiFormulaIncome.setForeground(PRIMARY);

        kpiFormulaPayout = new JLabel("$0");
        kpiFormulaPayout.setFont(new Font("SansSerif", Font.BOLD, 13));
        kpiFormulaPayout.setForeground(new Color(186, 26, 26));

        kpiFormulaProfit = new JLabel("$0");
        kpiFormulaProfit.setFont(new Font("SansSerif", Font.BOLD, 14));
        kpiFormulaProfit.setForeground(GREEN);

        formulaCard.add(new JLabel("Ingresos:"));
        formulaCard.add(kpiFormulaIncome);
        formulaCard.add(new JLabel("— Egresos (Comisiones):"));
        formulaCard.add(kpiFormulaPayout);
        formulaCard.add(new JLabel("= Ganancia Libre:"));
        formulaCard.add(kpiFormulaProfit);

        banner.add(left, BorderLayout.WEST);
        banner.add(formulaCard, BorderLayout.EAST);
        return banner;
    }

    private JPanel buildKpisRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);

        kpiWashesCount = new JLabel("0");
        kpiWashesSubtitle = new JLabel("0 Carros | 0 Motos");
        row.add(createKpiCard("LAVADOS REGISTRADOS", kpiWashesCount, kpiWashesSubtitle, PRIMARY));

        kpiIncomeTotal = new JLabel("$0");
        kpiIncomeSubtitle = new JLabel("Ticket prom: $0");
        row.add(createKpiCard("INGRESOS TOTALES", kpiIncomeTotal, kpiIncomeSubtitle, PRIMARY));

        kpiPayoutsTotal = new JLabel("$0");
        kpiPayoutsSubtitle = new JLabel("0 Operadores activos");
        row.add(createKpiCard("PAGOS TRABAJADORES", kpiPayoutsTotal, kpiPayoutsSubtitle, SECONDARY));

        kpiProfitTotal = new JLabel("$0");
        kpiProfitSubtitle = new JLabel("0% Margen libre");
        row.add(createKpiCard("GANANCIA NETA NEGOCIO", kpiProfitTotal, kpiProfitSubtitle, GREEN));

        return row;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, JLabel subLabel, Color accent) {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        titleLbl.setForeground(MUTED_LIGHT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        valueLabel.setForeground(TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));

        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subLabel.setForeground(accent);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLbl);
        card.add(valueLabel);
        card.add(subLabel);
        return card;
    }

    // --- Tarjeta 1 Centro: Rendimiento Operativo y Tendencia ---
    private JPanel buildOperationalRatioAndTrendCard() {
        JPanel card = new JPanel();
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("📊 Proporción de Vehículos");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(TEXT);
        ratioVehiclesTotalLabel = new JLabel("0 Vehículos");
        ratioVehiclesTotalLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        ratioVehiclesTotalLabel.setForeground(MUTED);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(ratioVehiclesTotalLabel, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(8));

        operationalRatioBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = 6;

                int carWidth = (ratioTotalWashes > 0) ? (w * ratioCarsCount) / ratioTotalWashes : w / 2;

                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, carWidth, h, arc, arc);

                g2.setColor(SECONDARY);
                g2.fillRoundRect(carWidth, 0, w - carWidth, h, arc, arc);

                g2.dispose();
            }
        };
        operationalRatioBar.setPreferredSize(new Dimension(0, 12));
        operationalRatioBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        card.add(operationalRatioBar);
        card.add(Box.createVerticalStrut(4));

        JPanel ratioLabelsRow = new JPanel(new BorderLayout());
        ratioLabelsRow.setOpaque(false);
        ratioCarrosLabel = new JLabel("🚗 Carros: 0 (0%)");
        ratioCarrosLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ratioCarrosLabel.setForeground(PRIMARY);

        ratioMotosLabel = new JLabel("🏍 Motos: 0 (0%)");
        ratioMotosLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ratioMotosLabel.setForeground(SECONDARY);

        ratioLabelsRow.add(ratioCarrosLabel, BorderLayout.WEST);
        ratioLabelsRow.add(ratioMotosLabel, BorderLayout.EAST);
        card.add(ratioLabelsRow);
        card.add(Box.createVerticalStrut(14));

        JLabel weeklyTitle = new JLabel("📈 Tendencia Semanal de Ingresos (7 Días)");
        weeklyTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        weeklyTitle.setForeground(TEXT);
        weeklyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(weeklyTitle);
        card.add(Box.createVerticalStrut(6));

        weeklyBarsContainer = new JPanel(new GridLayout(1, 7, 8, 0));
        weeklyBarsContainer.setOpaque(false);
        weeklyBarsContainer.setPreferredSize(new Dimension(0, 68));
        weeklyBarsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        card.add(weeklyBarsContainer);

        return card;
    }

    // --- Tarjeta 2 Centro: Operadores en Turno y Rendimiento ---
    private JPanel buildWorkersShiftCard() {
        JPanel card = new JPanel();
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("👥 Operadores en Turno (Hoy)");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(TEXT);

        JButton btnAddWashQuick = createPrimaryButton("＋ Registrar Lavado", 28);
        btnAddWashQuick.addActionListener(e -> showNewWashModal());

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(btnAddWashQuick, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(10));

        workerCardsContainer = new JPanel();
        workerCardsContainer.setOpaque(false);
        workerCardsContainer.setLayout(new BoxLayout(workerCardsContainer, BoxLayout.Y_AXIS));

        JScrollPane workerCardsScroll = new JScrollPane(workerCardsContainer);
        workerCardsScroll.setBorder(null);
        workerCardsScroll.setOpaque(false);
        workerCardsScroll.getViewport().setOpaque(false);
        workerCardsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(workerCardsScroll);

        return card;
    }

    private JPanel buildRecentWashesSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("🕒 Últimos Lavados Registrados Hoy");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT);

        JButton btnRefresh = new JButton("⟳ Actualizar");
        btnRefresh.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> {
            loadedViews.clear();
            loadDashboardDataAsync();
        });

        header.add(title, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);

        String[] cols = {"HORA", "TIPO", "PLACA", "DETALLE", "OPERADOR", "COBRADO", "PAGO TRAB.", "GANANCIA"};
        recentWashesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        recentWashesTable = createStyledTable(recentWashesModel);
        JScrollPane scroll = new JScrollPane(recentWashesTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(0, 220));

        section.add(header, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    // =========================================================================
    // MODAL DE REGISTRO DE LAVADO (CON CATÁLOGOS DINÁMICOS Y LISTA DE CHECKS)
    // =========================================================================
    private void showNewWashModal() {
        JDialog dialog = new JDialog(this, "Registrar Nuevo Lavado [Modo Despachador]", true);
        dialog.setSize(500, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // 1. Cliente, Teléfono y Placa
        JPanel clientPlateRow = new JPanel(new GridLayout(1, 3, 10, 0));
        clientPlateRow.setOpaque(false);
        clientPlateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel clientCol = new JPanel();
        clientCol.setOpaque(false);
        clientCol.setLayout(new BoxLayout(clientCol, BoxLayout.Y_AXIS));
        JLabel clientLabel = new JLabel("1. CLIENTE *");
        clientLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        clientLabel.setForeground(MUTED_LIGHT);
        JTextField txtClient = new JTextField();
        txtClient.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtClient.putClientProperty("JTextField.placeholderText", "Nombre cliente");
        clientCol.add(clientLabel);
        clientCol.add(Box.createVerticalStrut(2));
        clientCol.add(txtClient);

        JPanel phoneCol = new JPanel();
        phoneCol.setOpaque(false);
        phoneCol.setLayout(new BoxLayout(phoneCol, BoxLayout.Y_AXIS));
        JLabel phoneLabel = new JLabel("TELÉFONO");
        phoneLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        phoneLabel.setForeground(MUTED_LIGHT);
        JTextField txtPhone = new JTextField();
        txtPhone.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtPhone.putClientProperty("JTextField.placeholderText", "Opcional");
        phoneCol.add(phoneLabel);
        phoneCol.add(Box.createVerticalStrut(2));
        phoneCol.add(txtPhone);

        JPanel plateCol = new JPanel();
        plateCol.setOpaque(false);
        plateCol.setLayout(new BoxLayout(plateCol, BoxLayout.Y_AXIS));
        JLabel plateLabel = new JLabel("PLACA *");
        plateLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        plateLabel.setForeground(MUTED_LIGHT);
        JTextField modalPlate = new JTextField();
        modalPlate.setFont(new Font("SansSerif", Font.BOLD, 13));
        modalPlate.putClientProperty("JTextField.placeholderText", "Ej: KLR-882");
        modalPlate.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                modalPlate.setText(modalPlate.getText().toUpperCase());
            }
        });
        plateCol.add(plateLabel);
        plateCol.add(Box.createVerticalStrut(2));
        plateCol.add(modalPlate);

        clientPlateRow.add(clientCol);
        clientPlateRow.add(phoneCol);
        clientPlateRow.add(plateCol);
        form.add(clientPlateRow);
        form.add(Box.createVerticalStrut(12));

        // 2. Selección de Vehículo y Tipo de Lavado filtrado dinámicamente
        JLabel step2 = new JLabel("2. VEHÍCULO Y TIPO DE LAVADO ASIGNADO *");
        step2.setFont(new Font("SansSerif", Font.BOLD, 10));
        step2.setForeground(MUTED_LIGHT);
        form.add(step2);
        form.add(Box.createVerticalStrut(4));

        JPanel catRow = new JPanel(new GridLayout(1, 2, 12, 0));
        catRow.setOpaque(false);
        catRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel vCol = new JPanel();
        vCol.setOpaque(false);
        vCol.setLayout(new BoxLayout(vCol, BoxLayout.Y_AXIS));
        JLabel vLbl = new JLabel("Tipo de Vehículo");
        vLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JComboBox<VehicleTypeCatalog> cmbVehicles = new JComboBox<VehicleTypeCatalog>();
        List<VehicleTypeCatalog> vList = catalogController.getAllVehicleTypes();
        for (VehicleTypeCatalog v : vList) cmbVehicles.addItem(v);
        vCol.add(vLbl);
        vCol.add(Box.createVerticalStrut(2));
        vCol.add(cmbVehicles);

        JPanel wCol = new JPanel();
        wCol.setOpaque(false);
        wCol.setLayout(new BoxLayout(wCol, BoxLayout.Y_AXIS));
        JLabel wLbl = new JLabel("Tipo de Lavado");
        wLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        JComboBox<WashType> cmbWashes = new JComboBox<WashType>();
        wCol.add(wLbl);
        wCol.add(Box.createVerticalStrut(2));
        wCol.add(cmbWashes);

        catRow.add(vCol);
        catRow.add(wCol);
        form.add(catRow);
        form.add(Box.createVerticalStrut(12));

        // 3. ASIGNAR OPERADOR MEDIANTE LISTA CON CHECKS
        JLabel step3 = new JLabel("3. ASIGNAR OPERADOR (SELECCIONA DE LA LISTA) *");
        step3.setFont(new Font("SansSerif", Font.BOLD, 10));
        step3.setForeground(MUTED_LIGHT);
        form.add(step3);
        form.add(Box.createVerticalStrut(4));

        JPanel workerCheckListPanel = new JPanel();
        workerCheckListPanel.setLayout(new BoxLayout(workerCheckListPanel, BoxLayout.Y_AXIS));
        workerCheckListPanel.setOpaque(false);

        List<Worker> activeWorkers = workerController.getActiveWorkers();
        final List<JCheckBox> checkList = new ArrayList<JCheckBox>();
        final Long[] modalSelectedWorkerId = new Long[1];

        if (activeWorkers.isEmpty()) {
            workerCheckListPanel.add(new JLabel("No hay operadores registrados activos."));
        } else {
            for (Worker w : activeWorkers) {
                JCheckBox chk = new JCheckBox("☑  " + w.getName() + " — " + w.getBay() + " (" + w.getShift() + ")");
                chk.setFont(new Font("SansSerif", Font.PLAIN, 12));
                chk.setOpaque(true);
                chk.setBackground(new Color(248, 250, 255));
                chk.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(228, 234, 248), 1),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
                chk.setFocusPainted(false);
                chk.setCursor(new Cursor(Cursor.HAND_CURSOR));

                final Long wId = w.getId();
                chk.addActionListener(e -> {
                    for (JCheckBox other : checkList) {
                        if (other != chk) other.setSelected(false);
                    }
                    if (chk.isSelected()) {
                        modalSelectedWorkerId[0] = wId;
                    } else {
                        modalSelectedWorkerId[0] = null;
                    }
                });

                checkList.add(chk);
                workerCheckListPanel.add(chk);
                workerCheckListPanel.add(Box.createVerticalStrut(3));
            }
            if (!checkList.isEmpty()) {
                checkList.get(0).setSelected(true);
                modalSelectedWorkerId[0] = activeWorkers.get(0).getId();
            }
        }

        JScrollPane workerScroll = new JScrollPane(workerCheckListPanel);
        workerScroll.setPreferredSize(new Dimension(0, 95));
        workerScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        workerScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        workerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        form.add(workerScroll);
        form.add(Box.createVerticalStrut(10));

        // 4. Preview Financiero
        JPanel previewBox = buildTicketPreviewBox();
        form.add(previewBox);
        form.add(Box.createVerticalStrut(10));

        // Filtro reactivo tipo de vehiculo -> tipos de lavado
        final BigDecimal[] currentPrice = new BigDecimal[]{BigDecimal.ZERO};
        Runnable updateWashTypes = () -> {
            VehicleTypeCatalog selV = (VehicleTypeCatalog) cmbVehicles.getSelectedItem();
            cmbWashes.removeAllItems();
            if (selV != null) {
                List<WashType> types = catalogController.getWashTypesByVehicleType(selV.getId());
                for (WashType wt : types) cmbWashes.addItem(wt);
            }
            WashType selW = (WashType) cmbWashes.getSelectedItem();
            if (selW != null) {
                currentPrice[0] = selW.getPrice();
            } else {
                currentPrice[0] = BigDecimal.ZERO;
            }
            BigDecimal p = currentPrice[0];
            BigDecimal commPct = (selV != null && selV.getCommissionPct() != null) ? selV.getCommissionPct() : new BigDecimal("40.00");
            BigDecimal wp = p.multiply(commPct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal profit = p.subtract(wp);
            previewPriceLabel.setText(CURRENCY_FMT.format(p));
            previewPaymentLabel.setText(CURRENCY_FMT.format(wp));
            previewProfitLabel.setText(CURRENCY_FMT.format(profit));
        };

        cmbVehicles.addActionListener(e -> updateWashTypes.run());
        cmbWashes.addActionListener(e -> {
            WashType selW = (WashType) cmbWashes.getSelectedItem();
            VehicleTypeCatalog selV = (VehicleTypeCatalog) cmbVehicles.getSelectedItem();
            if (selW != null) {
                currentPrice[0] = selW.getPrice();
                BigDecimal p = currentPrice[0];
                BigDecimal commPct = (selV != null && selV.getCommissionPct() != null) ? selV.getCommissionPct() : new BigDecimal("40.00");
                BigDecimal wp = p.multiply(commPct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                BigDecimal profit = p.subtract(wp);
                previewPriceLabel.setText(CURRENCY_FMT.format(p));
                previewPaymentLabel.setText(CURRENCY_FMT.format(wp));
                previewProfitLabel.setText(CURRENCY_FMT.format(profit));
            }
        });
        updateWashTypes.run();

        // 5. Botones Cancelar / Guardar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setOpaque(false);
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("✔ Guardar y Registrar Lavado", 36);
        btnSave.addActionListener(e -> {
            String plate = modalPlate.getText().trim();
            String client = txtClient.getText().trim();
            VehicleTypeCatalog selV = (VehicleTypeCatalog) cmbVehicles.getSelectedItem();
            WashType selW = (WashType) cmbWashes.getSelectedItem();

            if (plate.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Por favor ingresa la placa del vehículo.", "Validación", JOptionPane.WARNING_MESSAGE);
                modalPlate.requestFocus();
                return;
            }
            if (selV == null || selW == null) {
                JOptionPane.showMessageDialog(dialog, "Selecciona un tipo de vehículo y tipo de lavado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (modalSelectedWorkerId[0] == null) {
                JOptionPane.showMessageDialog(dialog, "Por favor selecciona el operador asignado en la lista.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("⏳ Guardando...");

            CreateWashRequest request = new CreateWashRequest();
            request.setPlate(plate);
            request.setClientName(client.isEmpty() ? plate : client);
            request.setClientPhone(txtPhone.getText().trim().isEmpty() ? null : txtPhone.getText().trim());
            request.setVehicleModel(selV.getName());
            request.setVehicleTypeCatalogId(selV.getId());
            request.setVehicleTypeName(selV.getName());
            request.setWashTypeId(selW.getId());
            request.setWashTypeName(selW.getName());
            request.setWorkerId(modalSelectedWorkerId[0]);
            request.setPrice(selW.getPrice());
            request.setCommissionPct(selV != null && selV.getCommissionPct() != null
                    ? selV.getCommissionPct() : new BigDecimal("40.00"));
            if (currentUser != null) {
                request.setCreatedByUserId(currentUser.getId());
            }

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    washController.registerWash(request);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        loadedViews.clear();
                        loadDashboardDataAsync();
                        JOptionPane.showMessageDialog(MainFrame.this, "¡Lavado registrado y guardado exitosamente!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        btnSave.setEnabled(true);
                        btnSave.setText("✔ Guardar y Registrar Lavado");
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        JOptionPane.showMessageDialog(dialog, "Error al registrar lavado: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JButton createVehicleToggle(String title, String subtitle, String price, boolean selected) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout());
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyVehicleToggleStyle(btn, title, subtitle, price, selected);
        return btn;
    }

    private void applyVehicleToggleStyle(JButton btn, String title, String subtitle, String price, boolean selected) {
        btn.removeAll();
        btn.setBackground(selected ? new Color(240, 247, 255) : Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? PRIMARY : BORDER, selected ? 2 : 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setForeground(TEXT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));
        subLbl.setForeground(MUTED);

        left.add(titleLbl);
        left.add(subLbl);

        JLabel priceLbl = new JLabel(price);
        priceLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        priceLbl.setForeground(PRIMARY);

        btn.add(left, BorderLayout.WEST);
        btn.add(priceLbl, BorderLayout.EAST);
        btn.revalidate();
        btn.repaint();
    }

    private void updateVehicleSelectionUI() {
        if (btnSelectCarro == null || btnSelectMoto == null) return;
        ServiceRate rCarro = serviceRateRepository.findByVehicleType(VehicleType.CARRO);
        ServiceRate rMoto = serviceRateRepository.findByVehicleType(VehicleType.MOTO);
        BigDecimal pCarro = rCarro != null ? rCarro.getDefaultPrice() : new BigDecimal("30000");
        BigDecimal pMoto = rMoto != null ? rMoto.getDefaultPrice() : new BigDecimal("20000");

        applyVehicleToggleStyle(btnSelectCarro, "🚗 Carro", "General", CURRENCY_FMT.format(pCarro), selectedVehicleType == VehicleType.CARRO);
        applyVehicleToggleStyle(btnSelectMoto, "🏍 Moto", "Detallado", CURRENCY_FMT.format(pMoto), selectedVehicleType == VehicleType.MOTO);
    }

    private JPanel buildTicketPreviewBox() {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(new Color(248, 250, 255));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 224, 250), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel header = new JLabel("PREVIEW FINANCIERO DEL TICKET");
        header.setFont(new Font("SansSerif", Font.BOLD, 9));
        header.setForeground(MUTED_LIGHT);
        box.add(header, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 6, 0));
        metrics.setOpaque(false);
        metrics.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        previewPriceLabel = new JLabel("$0");
        previewPriceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        previewPriceLabel.setForeground(TEXT);

        previewPaymentLabel = new JLabel("$0");
        previewPaymentLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        previewPaymentLabel.setForeground(new Color(186, 26, 26));

        previewProfitLabel = new JLabel("$0");
        previewProfitLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        previewProfitLabel.setForeground(GREEN);

        metrics.add(createMiniStat("Cobro Total", previewPriceLabel));
        metrics.add(createMiniStat("Pago Operador (40%)", previewPaymentLabel));
        metrics.add(createMiniStat("Ganancia Negocio (60%)", previewProfitLabel));

        box.add(metrics, BorderLayout.CENTER);
        return box;
    }

    private JPanel createMiniStat(String title, JLabel value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.PLAIN, 9));
        t.setForeground(MUTED);

        p.add(t);
        p.add(value);
        return p;
    }

    private void updateFormPreview() {
        if (previewPriceLabel == null) return;
        ServiceRate rate = serviceRateRepository.findByVehicleType(selectedVehicleType);
        BigDecimal price = (rate != null) ? rate.getDefaultPrice() : new BigDecimal(selectedVehicleType == VehicleType.CARRO ? "30000" : "20000");
        BigDecimal commissionPct = (rate != null) ? rate.getCommissionPct() : new BigDecimal("40.00");

        BigDecimal[] breakdown = washController.previewTicket(price, commissionPct);
        previewPriceLabel.setText(CURRENCY_FMT.format(breakdown[0]));
        previewPaymentLabel.setText(CURRENCY_FMT.format(breakdown[1]));
        previewProfitLabel.setText(CURRENCY_FMT.format(breakdown[2]));
    }

    // =========================================================================
    // OTRAS VISTAS (TRABAJADORES, HISTORIAL, NÓMINA, REPORTES, CONFIG)
    // =========================================================================

    private JPanel buildWorkersView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Gestión de Operadores y Trabajadores");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton btnNewWorker = createPrimaryButton("＋ Registrar Operador", 34);
        btnNewWorker.addActionListener(e -> showRegisterWorkerDialog());

        JButton btnRefreshWorkers = new JButton("↻ Actualizar");
        btnRefreshWorkers.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefreshWorkers.addActionListener(e -> loadWorkersDataAsync());

        headerRight.add(btnNewWorker);
        headerRight.add(btnRefreshWorkers);

        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        String[] cols = {"ID", "NOMBRE", "TELÉFONO", "BAHÍA", "TURNO", "LAVADOS", "GENERADO", "COMISIÓN", "ESTADO"};
        workersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        workersTable = createStyledTable(workersTableModel);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer workerStatusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    String val = value != null ? value.toString() : "";
                    if ("Activo".equalsIgnoreCase(val)) {
                        l.setForeground(new Color(22, 101, 52));
                    } else {
                        l.setForeground(new Color(220, 38, 38));
                    }
                }
                return l;
            }
        };

        workersTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        workersTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        workersTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        workersTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        workersTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        workersTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        workersTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        workersTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        workersTable.getColumnModel().getColumn(8).setPreferredWidth(80);

        workersTable.getColumnModel().getColumn(0).setCellRenderer(center);
        workersTable.getColumnModel().getColumn(2).setCellRenderer(center);
        workersTable.getColumnModel().getColumn(3).setCellRenderer(center);
        workersTable.getColumnModel().getColumn(4).setCellRenderer(center);
        workersTable.getColumnModel().getColumn(5).setCellRenderer(center);
        workersTable.getColumnModel().getColumn(6).setCellRenderer(right);
        workersTable.getColumnModel().getColumn(7).setCellRenderer(right);
        workersTable.getColumnModel().getColumn(8).setCellRenderer(workerStatusRenderer);

        // Doble clic para editar
        workersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && workersTable.getSelectedRow() >= 0) {
                    openEditSelectedWorker();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(workersTable);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(scroll, BorderLayout.CENTER);

        // Barra inferior de acciones
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        JLabel lblHint = new JLabel("💡 Haz doble clic en un operador o usa los botones de la derecha para gestionar sus datos.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(MUTED);
        bottomBar.add(lblHint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnEditWorker = new JButton("✏️ Editar Operador");
        btnEditWorker.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnEditWorker.addActionListener(e -> openEditSelectedWorker());

        JButton btnToggleStatus = new JButton("🔄 Activar / Desactivar");
        btnToggleStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnToggleStatus.addActionListener(e -> toggleSelectedWorkerStatus());

        actions.add(btnEditWorker);
        actions.add(btnToggleStatus);
        bottomBar.add(actions, BorderLayout.EAST);

        centerPanel.add(bottomBar, BorderLayout.SOUTH);

        panel.add(header, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private void openEditSelectedWorker() {
        if (workersTable == null || workersTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un operador de la tabla para editar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = workersTable.getSelectedRow();
        Long wId = (Long) workersTableModel.getValueAt(row, 0);
        Worker target = workerController.getWorkerById(wId);
        if (target != null) {
            showEditWorkerDialog(target);
        }
    }

    private void showEditWorkerDialog(Worker worker) {
        if (worker == null) return;
        JDialog dialog = new JDialog(this, "Editar Operador: " + worker.getName(), true);
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Editar Información del Operador");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY);

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 12));
        form.setOpaque(false);

        JTextField txtName = new JTextField(worker.getName() != null ? worker.getName() : "");
        JTextField txtPhone = new JTextField(worker.getPhone() != null ? worker.getPhone() : "");
        txtPhone.putClientProperty("JTextField.placeholderText", "Ej: 3001234567");

        JComboBox<String> cmbBay = new JComboBox<String>(new String[]{"Bahía 1", "Bahía 2", "Bahía 3", "Bahía 4"});
        if (worker.getBay() != null) cmbBay.setSelectedItem(worker.getBay());

        JComboBox<String> cmbShift = new JComboBox<String>(new String[]{"Turno Completo", "Medio Turno"});
        if (worker.getShift() != null) cmbShift.setSelectedItem(worker.getShift());

        form.add(new JLabel("Nombre Completo *:"));
        form.add(txtName);
        form.add(new JLabel("Teléfono:"));
        form.add(txtPhone);
        form.add(new JLabel("Bahía Asignada:"));
        form.add(cmbBay);
        form.add(new JLabel("Turno de Trabajo:"));
        form.add(cmbShift);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("💾 Guardar Cambios", 34);
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            String bay = (String) cmbBay.getSelectedItem();
            String shift = (String) cmbShift.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El nombre del operador no puede estar vacío.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                workerController.updateWorker(worker.getId(), name, phone.isEmpty() ? null : phone, bay, shift);
                dialog.dispose();
                loadWorkersDataAsync();
                loadDashboardDataAsync();
                JOptionPane.showMessageDialog(this, "Operador actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(btnCancel);
        buttons.add(btnSave);

        content.add(title, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void toggleSelectedWorkerStatus() {
        if (workersTable == null || workersTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un operador de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = workersTable.getSelectedRow();
        Long wId = (Long) workersTableModel.getValueAt(row, 0);
        String name = (String) workersTableModel.getValueAt(row, 1);
        String estado = (String) workersTableModel.getValueAt(row, 8);
        boolean isActive = "Activo".equalsIgnoreCase(estado);

        if (isActive) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Deseas DESACTIVAR al operador '" + name + "'?\n(No aparecerá para nuevos lavados ni en los turnos de liquidación).",
                    "Confirmar Desactivación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                workerController.deactivateWorker(wId);
                loadWorkersDataAsync();
                loadDashboardDataAsync();
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Deseas ACTIVAR al operador '" + name + "' para nuevos turnos?",
                    "Confirmar Activación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                workerController.reactivateWorker(wId);
                loadWorkersDataAsync();
                loadDashboardDataAsync();
            }
        }
    }

    private JPanel buildHistoryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Historial General de Lavados");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        lblHistoryCount = new JLabel("0 lavados");
        lblHistoryCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHistoryCount.setForeground(new Color(24, 119, 242));

        lblHistoryTotal = new JLabel("Total: $ 0");
        lblHistoryTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblHistoryTotal.setForeground(new Color(22, 101, 52));

        JButton btnRefreshHistory = new JButton("↻ Actualizar");
        btnRefreshHistory.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefreshHistory.addActionListener(e -> loadHistoryDataAsync());

        headerRight.add(lblHistoryCount);
        headerRight.add(new JLabel(" | "));
        headerRight.add(lblHistoryTotal);
        headerRight.add(Box.createHorizontalStrut(10));
        headerRight.add(btnRefreshHistory);

        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // Barra de Filtros
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setBackground(SURFACE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel lblFiltro = new JLabel("📅 Período:");
        lblFiltro.setFont(new Font("Segoe UI", Font.BOLD, 12));

        cmbHistoryDateFilter = new JComboBox<String>(new String[]{
                "📅 Hoy",
                "📅 Ayer",
                "📅 Esta Semana",
                "📅 Este Mes",
                "📅 Todos los Registros"
        });
        cmbHistoryDateFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbHistoryDateFilter.setPreferredSize(new Dimension(175, 30));
        cmbHistoryDateFilter.addActionListener(e -> loadHistoryDataAsync());

        JLabel lblBuscar = new JLabel("🔍 Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        txtSearchHistory = new JTextField();
        txtSearchHistory.setPreferredSize(new Dimension(240, 30));
        txtSearchHistory.putClientProperty("JTextField.placeholderText", "Placa, cliente, teléfono u operador...");
        txtSearchHistory.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterHistoryTableLocal(); }
            public void removeUpdate(DocumentEvent e) { filterHistoryTableLocal(); }
            public void changedUpdate(DocumentEvent e) { filterHistoryTableLocal(); }
        });

        JButton btnClearSearch = new JButton("✕");
        btnClearSearch.setToolTipText("Limpiar búsqueda");
        btnClearSearch.addActionListener(e -> txtSearchHistory.setText(""));

        filterBar.add(lblFiltro);
        filterBar.add(cmbHistoryDateFilter);
        filterBar.add(Box.createHorizontalStrut(12));
        filterBar.add(lblBuscar);
        filterBar.add(txtSearchHistory);
        filterBar.add(btnClearSearch);

        JPanel topContainer = new JPanel();
        topContainer.setOpaque(false);
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(header);
        topContainer.add(filterBar);
        topContainer.add(Box.createVerticalStrut(10));

        String[] cols = {"#", "FECHA", "HORA", "CLIENTE", "TELÉFONO", "PLACA", "TIPO VEHÍCULO", "TIPO LAVADO", "OPERADOR", "PRECIO", "ESTADO"};
        allWashesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(allWashesModel);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
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

        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);
        table.getColumnModel().getColumn(6).setCellRenderer(center);
        table.getColumnModel().getColumn(9).setCellRenderer(right);
        table.getColumnModel().getColumn(10).setCellRenderer(statusRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPayoutsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("Liquidación de Comisiones y Desembolsos");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        lblCountPayoutsAdmin = new JLabel("0 pagos");
        lblCountPayoutsAdmin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCountPayoutsAdmin.setForeground(new Color(24, 119, 242));

        lblTotalPayoutsAdmin = new JLabel("Total: $ 0");
        lblTotalPayoutsAdmin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotalPayoutsAdmin.setForeground(new Color(180, 83, 9));

        JButton btnPayout = createPrimaryButton("💵 Liquidar Turno a Operador", 34);
        btnPayout.addActionListener(e -> showPayoutDialog());

        JButton btnRefreshPayouts = new JButton("↻ Actualizar");
        btnRefreshPayouts.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefreshPayouts.addActionListener(e -> loadPayoutsDataAsync());

        headerRight.add(lblCountPayoutsAdmin);
        headerRight.add(new JLabel(" | "));
        headerRight.add(lblTotalPayoutsAdmin);
        headerRight.add(Box.createHorizontalStrut(10));
        headerRight.add(btnPayout);
        headerRight.add(btnRefreshPayouts);

        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // Barra de Filtros
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setBackground(SURFACE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel lblFiltro = new JLabel("📅 Período:");
        lblFiltro.setFont(new Font("Segoe UI", Font.BOLD, 12));

        cmbPayoutDateFilterAdmin = new JComboBox<String>(new String[]{
                "📅 Hoy",
                "📅 Ayer",
                "📅 Esta Semana",
                "📅 Este Mes",
                "📅 Todos los Registros"
        });
        cmbPayoutDateFilterAdmin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbPayoutDateFilterAdmin.setPreferredSize(new Dimension(175, 30));
        cmbPayoutDateFilterAdmin.addActionListener(e -> loadPayoutsDataAsync());

        JLabel lblBuscar = new JLabel("🔍 Buscar:");
        lblBuscar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        txtSearchPayoutsAdmin = new JTextField();
        txtSearchPayoutsAdmin.setPreferredSize(new Dimension(220, 30));
        txtSearchPayoutsAdmin.putClientProperty("JTextField.placeholderText", "Operador, método o notas...");
        txtSearchPayoutsAdmin.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPayoutsTableLocal(); }
            public void removeUpdate(DocumentEvent e) { filterPayoutsTableLocal(); }
            public void changedUpdate(DocumentEvent e) { filterPayoutsTableLocal(); }
        });

        JButton btnClearSearch = new JButton("✕");
        btnClearSearch.setToolTipText("Limpiar búsqueda");
        btnClearSearch.addActionListener(e -> txtSearchPayoutsAdmin.setText(""));

        filterBar.add(lblFiltro);
        filterBar.add(cmbPayoutDateFilterAdmin);
        filterBar.add(Box.createHorizontalStrut(12));
        filterBar.add(lblBuscar);
        filterBar.add(txtSearchPayoutsAdmin);
        filterBar.add(btnClearSearch);

        JPanel topContainer = new JPanel();
        topContainer.setOpaque(false);
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(header);
        topContainer.add(filterBar);
        topContainer.add(Box.createVerticalStrut(10));

        String[] cols = {"#", "ID PAGO", "FECHA", "HORA", "OPERADOR", "MONTO LIQUIDADO", "MÉTODO PAGO", "ESTADO", "NOTAS"};
        payoutsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(payoutsTableModel);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(right);
        table.getColumnModel().getColumn(6).setCellRenderer(center);
        table.getColumnModel().getColumn(7).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReportsView() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        // 1. HEADER CON SELECTOR DE PERÍODO
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel leftHeader = new JPanel();
        leftHeader.setOpaque(false);
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("📊 Reportes Financieros y Balance General");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("Auditoría de ingresos brutos, comisiones de nómina, desembolsos y ganancia neta");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        leftHeader.add(title);
        leftHeader.add(Box.createVerticalStrut(2));
        leftHeader.add(subtitle);

        JPanel filterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterControls.setOpaque(false);

        JLabel lblFilter = new JLabel("Período:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFilter.setForeground(TEXT);

        String[] periods = {"📅 Hoy", "📅 Ayer", "📅 Esta Semana", "📅 Este Mes", "📅 Últimos 30 Días", "📅 Todo el Historial"};
        cmbReportPeriodFilter = new JComboBox<>(periods);
        cmbReportPeriodFilter.setSelectedIndex(3); // Por defecto: Este Mes
        cmbReportPeriodFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbReportPeriodFilter.setPreferredSize(new Dimension(160, 32));
        cmbReportPeriodFilter.addActionListener(e -> loadReportsDataAsync());

        JButton btnRefresh = new JButton("↻ Actualizar");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setPreferredSize(new Dimension(110, 32));
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadReportsDataAsync());

        filterControls.add(lblFilter);
        filterControls.add(cmbReportPeriodFilter);
        filterControls.add(btnRefresh);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(filterControls, BorderLayout.EAST);

        // 2. FILA DE 5 KPIS EJECUTIVOS
        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 85));

        repKpiIncomeTotal = new JLabel("$0");
        repKpiIncomeSub = new JLabel("0 lavados");
        kpiRow.add(createKpiCard("FACTURACIÓN BRUTA", repKpiIncomeTotal, repKpiIncomeSub, PRIMARY));

        repKpiCommissionsTotal = new JLabel("$0");
        repKpiCommissionsSub = new JLabel("Nómina devengada");
        kpiRow.add(createKpiCard("COMISIÓN OPERADORES", repKpiCommissionsTotal, repKpiCommissionsSub, SECONDARY));

        repKpiProfitTotal = new JLabel("$0");
        repKpiProfitSub = new JLabel("Ganancia neta");
        kpiRow.add(createKpiCard("UTILIDAD DEL NEGOCIO", repKpiProfitTotal, repKpiProfitSub, GREEN));

        repKpiMarginTotal = new JLabel("0.0%");
        repKpiMarginSub = new JLabel("Margen libre dueño");
        kpiRow.add(createKpiCard("% RENTABILIDAD", repKpiMarginTotal, repKpiMarginSub, new Color(161, 85, 0)));

        repKpiTicketAvg = new JLabel("$0");
        repKpiTicketSub = new JLabel("Promedio por lavado");
        kpiRow.add(createKpiCard("TICKET PROMEDIO", repKpiTicketAvg, repKpiTicketSub, PRIMARY));

        // 3. SECCIÓN CENTRAL: 2 GRÁFICAS NATIVAS DE BARRAS
        JPanel chartsSplit = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsSplit.setOpaque(false);
        chartsSplit.setPreferredSize(new Dimension(0, 190));

        // Gráfica A: Ingresos vs Utilidad
        JPanel trendCard = new JPanel(new BorderLayout());
        trendCard.setBackground(SURFACE);
        trendCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JPanel trendHeader = new JPanel(new BorderLayout());
        trendHeader.setOpaque(false);
        JLabel lblTrendTitle = new JLabel("📈 Tendencia: Facturación vs Utilidad Neta");
        lblTrendTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTrendTitle.setForeground(TEXT);

        JLabel lblLegend = new JLabel("■ Facturación  ░ Ganancia ");
        lblLegend.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLegend.setForeground(MUTED);
        trendHeader.add(lblTrendTitle, BorderLayout.WEST);
        trendHeader.add(lblLegend, BorderLayout.EAST);

        repChartTrendPanel = new JPanel(new BorderLayout());
        repChartTrendPanel.setOpaque(false);
        trendCard.add(trendHeader, BorderLayout.NORTH);
        trendCard.add(repChartTrendPanel, BorderLayout.CENTER);

        // Gráfica B: Desglose por Tipo de Vehículo
        JPanel vehCard = new JPanel(new BorderLayout());
        vehCard.setBackground(SURFACE);
        vehCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel lblVehTitle = new JLabel("🚗 Distribución y Rendimiento por Tipo de Vehículo");
        lblVehTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVehTitle.setForeground(TEXT);

        repChartVehiclePanel = new JPanel();
        repChartVehiclePanel.setOpaque(false);
        repChartVehiclePanel.setLayout(new BoxLayout(repChartVehiclePanel, BoxLayout.Y_AXIS));

        vehCard.add(lblVehTitle, BorderLayout.NORTH);
        vehCard.add(repChartVehiclePanel, BorderLayout.CENTER);

        chartsSplit.add(trendCard);
        chartsSplit.add(vehCard);

        // Contenedor Norte que junta Header + KPIs + Gráficos
        JPanel topSection = new JPanel();
        topSection.setOpaque(false);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(header);
        topSection.add(Box.createVerticalStrut(14));
        topSection.add(kpiRow);
        topSection.add(Box.createVerticalStrut(14));
        topSection.add(chartsSplit);
        topSection.add(Box.createVerticalStrut(14));

        // 4. TABLA DETALLADA HISTÓRICA CON PIE DE TOTALES
        JPanel tableContainer = new JPanel(new BorderLayout(0, 6));
        tableContainer.setOpaque(false);

        JPanel tableTitleBar = new JPanel(new BorderLayout());
        tableTitleBar.setOpaque(false);

        JLabel lblTableTitle = new JLabel("📋 Detalle Cronológico del Período");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTableTitle.setForeground(TEXT);

        repTableSummaryLabel = new JLabel("Cargando datos...");
        repTableSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        repTableSummaryLabel.setForeground(PRIMARY);

        tableTitleBar.add(lblTableTitle, BorderLayout.WEST);
        tableTitleBar.add(repTableSummaryLabel, BorderLayout.EAST);

        String[] cols = {"PERÍODO", "LAVADOS", "FACTURACIÓN", "COMISIÓN LAVADOR", "DESEMBOLSOS CAJA", "UTILIDAD NETA", "RENTABILIDAD"};
        dailyReportsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(dailyReportsModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        tableContainer.add(tableTitleBar, BorderLayout.NORTH);
        tableContainer.add(scroll, BorderLayout.CENTER);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildConfigView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JLabel title = new JLabel("Configuración General del Sistema");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        JLabel desc = new JLabel("Información y enlaces a la gestión operativa del negocio:");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(16));

        // Tarjeta informativa de Catálogos y Tarifas
        JPanel cardCatalogs = new JPanel(new BorderLayout(14, 10));
        cardCatalogs.setBackground(SURFACE);
        cardCatalogs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        cardCatalogs.setMaximumSize(new Dimension(800, 120));
        cardCatalogs.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel catText = new JPanel();
        catText.setOpaque(false);
        catText.setLayout(new BoxLayout(catText, BoxLayout.Y_AXIS));

        JLabel lblCatTitle = new JLabel("🚗 Tarifas y Precios de Servicios");
        lblCatTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCatTitle.setForeground(PRIMARY);

        JLabel lblCatDesc = new JLabel("<html>Los precios y tarifas de los servicios ahora se gestionan de forma dinámica en la sección<br><b>Tipos de Lavado & Vehículos</b>, donde cada servicio tiene su precio asignado por tipo de vehículo.</html>");
        lblCatDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCatDesc.setForeground(TEXT);

        catText.add(lblCatTitle);
        catText.add(Box.createVerticalStrut(6));
        catText.add(lblCatDesc);

        JButton btnGoCatalogs = createPrimaryButton("🚿 Gestionar Catálogos", 34);
        btnGoCatalogs.addActionListener(e -> showView("catalogs"));

        cardCatalogs.add(catText, BorderLayout.CENTER);
        cardCatalogs.add(btnGoCatalogs, BorderLayout.EAST);
        panel.add(cardCatalogs);
        panel.add(Box.createVerticalStrut(16));

        // Tarjeta de Información del Negocio y Base de Datos
        JPanel cardSystem = new JPanel(new GridLayout(3, 2, 12, 10));
        cardSystem.setBackground(SURFACE);
        cardSystem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        cardSystem.setMaximumSize(new Dimension(800, 130));
        cardSystem.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardSystem.add(new JLabel("🏷 Software:"));
        JLabel lblApp = new JLabel("LavaGest PRO v0.1.0");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cardSystem.add(lblApp);

        cardSystem.add(new JLabel("☁️ Base de Datos:"));
        JLabel lblDb = new JLabel("PostgreSQL en Supabase (Conectado)");
        lblDb.setForeground(new Color(22, 101, 52));
        lblDb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cardSystem.add(lblDb);

        cardSystem.add(new JLabel("👤 Sesión Actual:"));
        String userStr = (currentUser != null ? currentUser.getUsername() + " (" + currentUser.getRole() + ")" : "admin (ADMIN)");
        JLabel lblUser = new JLabel(userStr);
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cardSystem.add(lblUser);

        panel.add(cardSystem);

        return panel;
    }

    // =========================================================================
    // GESTIÓN DE CATÁLOGOS (VEHÍCULOS Y TIPOS DE LAVADO)
    // =========================================================================
    private JPanel buildCatalogManagementView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        topHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel title = new JLabel("Catálogos Operativos: Tipos de Vehículo y Lavados");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JButton btnRefresh = new JButton("↻ Actualizar");
        btnRefresh.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> loadCatalogsDataAsync());

        topHeader.add(title, BorderLayout.WEST);
        topHeader.add(btnRefresh, BorderLayout.EAST);
        panel.add(topHeader, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 16, 0));
        split.setOpaque(false);

        // --- Panel Izquierdo: Tipos de Vehículo ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(SURFACE);
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel vTitle = new JLabel("1. TIPOS DE VEHÍCULO");
        vTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        vTitle.setForeground(PRIMARY);

        JPanel vForm = new JPanel(new BorderLayout(6, 0));
        vForm.setOpaque(false);
        vForm.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
        JTextField txtNewVehicle = new JTextField();
        txtNewVehicle.putClientProperty("JTextField.placeholderText", "Ej: Campero, Camioneta");
        JButton btnAddVehicle = createPrimaryButton("＋ Agregar", 30);
        JLabel lblNewV = new JLabel("Nuevo: ");
        lblNewV.setFont(new Font("SansSerif", Font.PLAIN, 11));
        vForm.add(lblNewV, BorderLayout.WEST);
        vForm.add(txtNewVehicle, BorderLayout.CENTER);
        vForm.add(btnAddVehicle, BorderLayout.EAST);

        JPanel vTop = new JPanel();
        vTop.setOpaque(false);
        vTop.setLayout(new BoxLayout(vTop, BoxLayout.Y_AXIS));
        vTop.add(vTitle);
        vTop.add(vForm);
        leftPanel.add(vTop, BorderLayout.NORTH);

        String[] vCols = {"ID", "NOMBRE", "COMISIÓN (%)", "ESTADO"};
        vehicleCatalogModel = new DefaultTableModel(vCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable vTable = createStyledTable(vehicleCatalogModel);
        vTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        vTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        vTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        vTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        leftPanel.add(new JScrollPane(vTable), BorderLayout.CENTER);

        JPanel vBottom = new JPanel(new BorderLayout(8, 0));
        vBottom.setOpaque(false);
        vBottom.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel lblHintV = new JLabel("💡 Doble clic para editar");
        lblHintV.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblHintV.setForeground(MUTED);

        JPanel vActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        vActions.setOpaque(false);

        JButton btnEditVehicle = new JButton("✏ Editar");
        btnEditVehicle.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnEditVehicle.setBackground(new Color(240, 245, 255));
        btnEditVehicle.setForeground(PRIMARY);
        btnEditVehicle.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnToggleVehicle = new JButton("⊘ Desactivar");
        btnToggleVehicle.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnToggleVehicle.setBackground(new Color(254, 242, 242));
        btnToggleVehicle.setForeground(new Color(185, 28, 28));
        btnToggleVehicle.setCursor(new Cursor(Cursor.HAND_CURSOR));

        vActions.add(btnEditVehicle);
        vActions.add(btnToggleVehicle);

        vBottom.add(lblHintV, BorderLayout.WEST);
        vBottom.add(vActions, BorderLayout.EAST);
        leftPanel.add(vBottom, BorderLayout.SOUTH);

        vTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = vTable.getSelectedRow();
                if (row >= 0 && row < vehicleCatalogModel.getRowCount()) {
                    String status = (String) vehicleCatalogModel.getValueAt(row, 3);
                    if ("Activo".equalsIgnoreCase(status)) {
                        btnToggleVehicle.setText("⊘ Desactivar");
                        btnToggleVehicle.setBackground(new Color(254, 242, 242));
                        btnToggleVehicle.setForeground(new Color(185, 28, 28));
                    } else {
                        btnToggleVehicle.setText("↻ Activar");
                        btnToggleVehicle.setBackground(new Color(236, 253, 245));
                        btnToggleVehicle.setForeground(new Color(22, 101, 52));
                    }
                }
            }
        });

        btnEditVehicle.addActionListener(e -> {
            int row = vTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo de vehículo de la tabla para editar.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openEditVehicleDialog(row);
        });

        vTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = vTable.getSelectedRow();
                    if (row >= 0) {
                        openEditVehicleDialog(row);
                    }
                }
            }
        });

        btnToggleVehicle.addActionListener(e -> {
            int row = vTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo de vehículo de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long vId = (Long) vehicleCatalogModel.getValueAt(row, 0);
            String vName = (String) vehicleCatalogModel.getValueAt(row, 1);
            String status = (String) vehicleCatalogModel.getValueAt(row, 3);
            boolean isActive = "Activo".equalsIgnoreCase(status);

            if (isActive) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas desactivar el tipo de vehículo '" + vName + "'?\nNo aparecerá en nuevos registros pero mantendrá todo su historial intacto.",
                        "Confirmar Desactivación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    catalogController.deleteVehicleType(vId);
                    loadCatalogsDataAsync();
                }
            } else {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas reactivar el tipo de vehículo '" + vName + "'?",
                        "Confirmar Activación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    catalogController.activateVehicleType(vId);
                    loadCatalogsDataAsync();
                }
            }
        });

        btnAddVehicle.addActionListener(e -> {
            String name = txtNewVehicle.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingresa un nombre para el tipo de vehículo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                catalogController.createVehicleType(name);
                txtNewVehicle.setText("");
                loadCatalogsDataAsync();
                JOptionPane.showMessageDialog(this, "Tipo de vehículo creado con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al crear: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- Panel Derecho: Tipos de Lavado ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(SURFACE);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel wTitle = new JLabel("2. TIPOS DE LAVADO POR VEHÍCULO");
        wTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        wTitle.setForeground(SECONDARY);

        JPanel wForm = new JPanel(new GridLayout(2, 2, 10, 8));
        wForm.setOpaque(false);
        wForm.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        cmbCatalogVehicles = new JComboBox<VehicleTypeCatalog>();
        JTextField txtWashName = new JTextField();
        txtWashName.putClientProperty("JTextField.placeholderText", "Ej: Lavado con motor");
        JTextField txtWashPrice = new JTextField();
        txtWashPrice.putClientProperty("JTextField.placeholderText", "Ej: 45000");
        JButton btnAddWash = createPrimaryButton("＋ Crear Lavado", 30);

        JPanel cellVeh = new JPanel(new BorderLayout(4, 0));
        cellVeh.setOpaque(false);
        JLabel lblVeh = new JLabel("Vehículo:");
        lblVeh.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cellVeh.add(lblVeh, BorderLayout.WEST);
        cellVeh.add(cmbCatalogVehicles, BorderLayout.CENTER);

        JPanel cellPrice = new JPanel(new BorderLayout(4, 0));
        cellPrice.setOpaque(false);
        JLabel lblPrice = new JLabel("Precio $:");
        lblPrice.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cellPrice.add(lblPrice, BorderLayout.WEST);
        cellPrice.add(txtWashPrice, BorderLayout.CENTER);

        JPanel cellName = new JPanel(new BorderLayout(4, 0));
        cellName.setOpaque(false);
        JLabel lblName = new JLabel("Nombre:");
        lblName.setFont(new Font("SansSerif", Font.PLAIN, 11));
        cellName.add(lblName, BorderLayout.WEST);
        cellName.add(txtWashName, BorderLayout.CENTER);

        wForm.add(cellVeh);
        wForm.add(cellPrice);
        wForm.add(cellName);
        wForm.add(btnAddWash);

        JPanel wTop = new JPanel();
        wTop.setOpaque(false);
        wTop.setLayout(new BoxLayout(wTop, BoxLayout.Y_AXIS));
        wTop.add(wTitle);
        wTop.add(wForm);
        rightPanel.add(wTop, BorderLayout.NORTH);

        String[] wCols = {"ID", "VEHÍCULO", "TIPO DE LAVADO", "PRECIO", "ESTADO"};
        washTypeCatalogModel = new DefaultTableModel(wCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable wTable = createStyledTable(washTypeCatalogModel);
        wTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        wTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        wTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        wTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        wTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        rightPanel.add(new JScrollPane(wTable), BorderLayout.CENTER);

        JPanel wBottom = new JPanel(new BorderLayout(8, 0));
        wBottom.setOpaque(false);
        wBottom.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel lblHintW = new JLabel("💡 Doble clic para editar");
        lblHintW.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblHintW.setForeground(MUTED);

        JPanel wActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        wActions.setOpaque(false);

        JButton btnEditWash = new JButton("✏ Editar");
        btnEditWash.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnEditWash.setBackground(new Color(240, 245, 255));
        btnEditWash.setForeground(PRIMARY);
        btnEditWash.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnToggleWash = new JButton("⊘ Desactivar");
        btnToggleWash.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnToggleWash.setBackground(new Color(254, 242, 242));
        btnToggleWash.setForeground(new Color(185, 28, 28));
        btnToggleWash.setCursor(new Cursor(Cursor.HAND_CURSOR));

        wActions.add(btnEditWash);
        wActions.add(btnToggleWash);

        wBottom.add(lblHintW, BorderLayout.WEST);
        wBottom.add(wActions, BorderLayout.EAST);
        rightPanel.add(wBottom, BorderLayout.SOUTH);

        wTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = wTable.getSelectedRow();
                if (row >= 0 && row < washTypeCatalogModel.getRowCount()) {
                    String status = (String) washTypeCatalogModel.getValueAt(row, 4);
                    if ("Activo".equalsIgnoreCase(status)) {
                        btnToggleWash.setText("⊘ Desactivar");
                        btnToggleWash.setBackground(new Color(254, 242, 242));
                        btnToggleWash.setForeground(new Color(185, 28, 28));
                    } else {
                        btnToggleWash.setText("↻ Activar");
                        btnToggleWash.setBackground(new Color(236, 253, 245));
                        btnToggleWash.setForeground(new Color(22, 101, 52));
                    }
                }
            }
        });

        btnEditWash.addActionListener(e -> {
            int row = wTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo de lavado de la tabla para editar.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openEditWashTypeDialog(row);
        });

        wTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = wTable.getSelectedRow();
                    if (row >= 0) {
                        openEditWashTypeDialog(row);
                    }
                }
            }
        });

        btnToggleWash.addActionListener(e -> {
            int row = wTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo de lavado de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long wId = (Long) washTypeCatalogModel.getValueAt(row, 0);
            String wName = (String) washTypeCatalogModel.getValueAt(row, 2);
            String status = (String) washTypeCatalogModel.getValueAt(row, 4);
            boolean isActive = "Activo".equalsIgnoreCase(status);

            if (isActive) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas desactivar el tipo de lavado '" + wName + "'?\nNo aparecerá para nuevos registros pero mantendrá todo su historial intacto.",
                        "Confirmar Desactivación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    catalogController.deleteWashType(wId);
                    loadCatalogsDataAsync();
                }
            } else {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "¿Deseas reactivar el tipo de lavado '" + wName + "'?",
                        "Confirmar Activación",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    catalogController.activateWashType(wId);
                    loadCatalogsDataAsync();
                }
            }
        });

        btnAddWash.addActionListener(e -> {
            VehicleTypeCatalog vSel = (VehicleTypeCatalog) cmbCatalogVehicles.getSelectedItem();
            String name = txtWashName.getText().trim();
            String pStr = txtWashPrice.getText().trim();
            if (vSel == null) {
                JOptionPane.showMessageDialog(this, "Primero crea y selecciona un tipo de vehículo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (name.isEmpty() || pStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingresa el nombre y precio del tipo de lavado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                BigDecimal price = new BigDecimal(pStr);
                catalogController.createWashType(name, price, vSel.getId());
                txtWashName.setText("");
                txtWashPrice.setText("");
                loadCatalogsDataAsync();
                JOptionPane.showMessageDialog(this, "Tipo de lavado creado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Precio inválido o error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        split.add(leftPanel);
        split.add(rightPanel);
        panel.add(split, BorderLayout.CENTER);

        // --- Panel Inferior: Comisión por Tipo de Vehículo ---
        JPanel commPanel = new JPanel(new BorderLayout());
        commPanel.setBackground(SURFACE);
        commPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        JLabel commTitle = new JLabel("💰 3. ESQUEMA DE COMISIONES POR TIPO DE VEHÍCULO");
        commTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        commTitle.setForeground(new Color(161, 85, 0));
        commTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel commForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        commForm.setOpaque(false);

        JLabel lblCommVeh = new JLabel("Vehículo:");
        lblCommVeh.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JComboBox<VehicleTypeCatalog> cmbCommVehicle = new JComboBox<>();
        cmbCommVehicle.setPreferredSize(new Dimension(170, 32));

        JLabel lblCommPct = new JLabel("Comisión Trabajador (%):");
        lblCommPct.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JTextField txtCommPct = new JTextField(5);
        txtCommPct.putClientProperty("JTextField.placeholderText", "Ej: 40.00");
        txtCommPct.setPreferredSize(new Dimension(75, 32));

        JLabel lblNegocioPct = new JLabel("Margen Negocio: 60.00%");
        lblNegocioPct.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNegocioPct.setForeground(new Color(22, 101, 52));

        JLabel lblSimulacion = new JLabel("  • Ejemplo ($30.000): Trabajador $12.000 | Negocio $18.000");
        lblSimulacion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSimulacion.setForeground(MUTED);

        JButton btnSaveComm = createPrimaryButton("💾 Guardar Comisión", 32);

        // Helper para recalcular el margen de negocio y ejemplo en tiempo real
        Runnable recalcularVista = () -> {
            String val = txtCommPct.getText().trim();
            try {
                BigDecimal workerPct = val.isEmpty() ? BigDecimal.ZERO : new BigDecimal(val);
                if (workerPct.compareTo(BigDecimal.ZERO) >= 0 && workerPct.compareTo(new BigDecimal("100")) <= 0) {
                    BigDecimal ownerPct = new BigDecimal("100.00").subtract(workerPct);
                    lblNegocioPct.setText("Margen Negocio: " + ownerPct.toPlainString() + "%");
                    
                    BigDecimal refPrice = new BigDecimal("30000");
                    BigDecimal workerVal = refPrice.multiply(workerPct).divide(new BigDecimal("100"));
                    BigDecimal ownerVal = refPrice.subtract(workerVal);
                    lblSimulacion.setText("  • Ejemplo ($30.000): Trabajador " + CURRENCY_FMT.format(workerVal) + 
                                          " (" + workerPct.toPlainString() + "%) | Negocio " + 
                                          CURRENCY_FMT.format(ownerVal) + " (" + ownerPct.toPlainString() + "%)");
                }
            } catch (Exception ignored) {}
        };

        txtCommPct.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalcularVista.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalcularVista.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalcularVista.run(); }
        });

        // Al seleccionar un vehículo en el combo, pre-cargar su % actual
        cmbCommVehicle.addActionListener(e2 -> {
            VehicleTypeCatalog sel = (VehicleTypeCatalog) cmbCommVehicle.getSelectedItem();
            if (sel != null && sel.getCommissionPct() != null) {
                txtCommPct.setText(sel.getCommissionPct().toPlainString());
                recalcularVista.run();
            }
        });

        btnSaveComm.addActionListener(e2 -> {
            VehicleTypeCatalog sel = (VehicleTypeCatalog) cmbCommVehicle.getSelectedItem();
            if (sel == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un tipo de vehículo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String pctStr = txtCommPct.getText().trim();
            if (pctStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingresa el porcentaje de comisión.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                BigDecimal pct = new BigDecimal(pctStr);
                if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                    JOptionPane.showMessageDialog(this, "El porcentaje debe estar entre 0 y 100.", "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                boolean ok = catalogController.updateVehicleCommission(sel.getId(), pct);
                if (ok) {
                    BigDecimal ownerPct = new BigDecimal("100.00").subtract(pct);
                    JOptionPane.showMessageDialog(this,
                            "✅ Comisión actualizada para " + sel.getName() + ":\n" +
                            "• Trabajador: " + pct.toPlainString() + "%\n" +
                            "• Dueño / Negocio: " + ownerPct.toPlainString() + "%",
                            "Comisión Guardada", JOptionPane.INFORMATION_MESSAGE);
                    loadCatalogsDataAsync();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar la comisión.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Porcentaje inválido. Usa formato numérico (Ej: 40.00)", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        commForm.add(lblCommVeh);
        commForm.add(cmbCommVehicle);
        commForm.add(lblCommPct);
        commForm.add(txtCommPct);
        commForm.add(btnSaveComm);
        commForm.add(lblNegocioPct);
        commForm.add(lblSimulacion);

        JPanel commContent = new JPanel(new BorderLayout());
        commContent.setOpaque(false);
        commContent.add(commTitle, BorderLayout.NORTH);
        commContent.add(commForm, BorderLayout.CENTER);

        commPanel.add(commContent, BorderLayout.CENTER);
        panel.add(commPanel, BorderLayout.SOUTH);

        // Guardamos referencia al combo de comisiones para llenarlo en loadCatalogsDataAsync
        this.cmbCommissionVehicle = cmbCommVehicle;

        return panel;
    }

    private void openEditVehicleDialog(int row) {
        if (row < 0 || row >= vehicleCatalogModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Selecciona un tipo de vehículo de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) vehicleCatalogModel.getValueAt(row, 0);
        String currentName = (String) vehicleCatalogModel.getValueAt(row, 1);

        JDialog dialog = new JDialog(this, "Editar Tipo de Vehículo", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(380, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.setBackground(SURFACE);

        JLabel lblName = new JLabel("Nombre del Tipo de Vehículo:");
        lblName.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblName.setForeground(TEXT);

        JTextField txtName = new JTextField(currentName);
        txtName.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        content.add(lblName);
        content.add(Box.createVerticalStrut(8));
        content.add(txtName);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(new Color(248, 249, 250));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("Guardar Cambios", 32);
        btnSave.addActionListener(e -> {
            String newName = txtName.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El nombre no puede estar vacío.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                catalogController.updateVehicleType(id, newName);
                dialog.dispose();
                loadCatalogsDataAsync();
                JOptionPane.showMessageDialog(this, "Tipo de vehículo actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openEditWashTypeDialog(int row) {
        if (row < 0 || row >= washTypeCatalogModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Selecciona un tipo de lavado de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) washTypeCatalogModel.getValueAt(row, 0);
        WashType currentWt = null;
        for (WashType wt : adminWashTypesList) {
            if (wt.getId().equals(id)) {
                currentWt = wt;
                break;
            }
        }
        if (currentWt == null) {
            JOptionPane.showMessageDialog(this, "No se encontró el tipo de lavado.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final WashType targetWt = currentWt;

        JDialog dialog = new JDialog(this, "Editar Tipo de Lavado", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 310);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.setBackground(SURFACE);

        JLabel lblVeh = new JLabel("Tipo de Vehículo:");
        lblVeh.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblVeh.setForeground(TEXT);

        JComboBox<VehicleTypeCatalog> cmbVeh = new JComboBox<VehicleTypeCatalog>();
        VehicleTypeCatalog matchVeh = null;
        for (VehicleTypeCatalog v : adminVehiclesList) {
            if (v.isActive() || v.getId().equals(targetWt.getVehicleTypeCatalogId())) {
                cmbVeh.addItem(v);
                if (v.getId().equals(targetWt.getVehicleTypeCatalogId())) {
                    matchVeh = v;
                }
            }
        }
        if (matchVeh != null) {
            cmbVeh.setSelectedItem(matchVeh);
        }
        cmbVeh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblName = new JLabel("Nombre del Tipo de Lavado:");
        lblName.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblName.setForeground(TEXT);

        JTextField txtName = new JTextField(targetWt.getName());
        txtName.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblPrice = new JLabel("Precio ($):");
        lblPrice.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPrice.setForeground(TEXT);

        JTextField txtPrice = new JTextField(targetWt.getPrice() != null ? targetWt.getPrice().toPlainString() : "");
        txtPrice.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        content.add(lblVeh);
        content.add(Box.createVerticalStrut(4));
        content.add(cmbVeh);
        content.add(Box.createVerticalStrut(10));
        content.add(lblName);
        content.add(Box.createVerticalStrut(4));
        content.add(txtName);
        content.add(Box.createVerticalStrut(10));
        content.add(lblPrice);
        content.add(Box.createVerticalStrut(4));
        content.add(txtPrice);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(new Color(248, 249, 250));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("Guardar Cambios", 32);
        btnSave.addActionListener(e -> {
            VehicleTypeCatalog selVeh = (VehicleTypeCatalog) cmbVeh.getSelectedItem();
            String newName = txtName.getText().trim();
            String priceStr = txtPrice.getText().trim().replace("$", "").replace(".", "").replace(",", ".");
            if (selVeh == null) {
                JOptionPane.showMessageDialog(dialog, "Selecciona un tipo de vehículo.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Ingresa un nombre para el tipo de lavado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Ingresa el precio del lavado.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                BigDecimal newPrice = new BigDecimal(priceStr);
                catalogController.updateWashType(targetWt.getId(), newName, newPrice, selVeh.getId());
                dialog.dispose();
                loadCatalogsDataAsync();
                JOptionPane.showMessageDialog(this, "Tipo de lavado actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "El precio ingresado no es válido.", "Validación", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // =========================================================================
    // GESTIÓN DE USUARIOS Y ENCARGADOS
    // =========================================================================
    private JPanel buildUserManagementView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Control de Acceso: Usuarios y Encargados");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton btnNewUser = createPrimaryButton("＋ Nuevo Usuario", 34);
        btnNewUser.addActionListener(e -> showCreateUserDialog());

        JButton btnRefresh = new JButton("↻ Actualizar");
        btnRefresh.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> loadUsersDataAsync());

        headerRight.add(btnNewUser);
        headerRight.add(btnRefresh);

        header.add(title, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Tabla de usuarios
        String[] cols = {"ID", "USUARIO", "NOMBRE COMPLETO", "TELÉFONO", "ROL", "ESTADO"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = createStyledTable(usersTableModel);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer userStatusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if (!isSelected) {
                    String val = value != null ? value.toString() : "";
                    if ("Activo".equalsIgnoreCase(val)) {
                        l.setForeground(new Color(22, 101, 52));
                    } else {
                        l.setForeground(new Color(220, 38, 38));
                    }
                }
                return l;
            }
        };

        usersTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        usersTable.getColumnModel().getColumn(0).setCellRenderer(center);
        usersTable.getColumnModel().getColumn(3).setCellRenderer(center);
        usersTable.getColumnModel().getColumn(4).setCellRenderer(center);
        usersTable.getColumnModel().getColumn(5).setCellRenderer(userStatusRenderer);

        // Doble clic para editar
        usersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && usersTable.getSelectedRow() >= 0) {
                    openEditSelectedUser();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(scroll, BorderLayout.CENTER);

        // Barra inferior de acciones
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        JLabel lblHint = new JLabel("💡 Haz doble clic en un usuario o usa los botones de la derecha para gestionar su cuenta.");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(MUTED);
        bottomBar.add(lblHint, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnEditUser = new JButton("✏️ Editar Usuario");
        btnEditUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnEditUser.addActionListener(e -> openEditSelectedUser());

        JButton btnToggleStatus = new JButton("🔄 Activar / Desactivar");
        btnToggleStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnToggleStatus.addActionListener(e -> toggleSelectedUserStatus());

        actions.add(btnEditUser);
        actions.add(btnToggleStatus);
        bottomBar.add(actions, BorderLayout.EAST);

        centerPanel.add(bottomBar, BorderLayout.SOUTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private void openEditSelectedUser() {
        if (usersTable == null || usersTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla para editar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = usersTable.getSelectedRow();
        Long uId = (Long) usersTableModel.getValueAt(row, 0);
        AppUser target = null;
        for (AppUser u : allUsersList) {
            if (u.getId().equals(uId)) {
                target = u;
                break;
            }
        }
        if (target != null) {
            showEditUserDialog(target);
        }
    }

    private void toggleSelectedUserStatus() {
        if (usersTable == null || usersTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = usersTable.getSelectedRow();
        Long uId = (Long) usersTableModel.getValueAt(row, 0);
        AppUser target = null;
        for (AppUser u : allUsersList) {
            if (u.getId().equals(uId)) {
                target = u;
                break;
            }
        }
        if (target == null) return;

        if (currentUser != null && currentUser.getId().equals(target.getId()) && target.isActive()) {
            JOptionPane.showMessageDialog(this, "No puedes desactivar tu propia cuenta activa de administrador.", "Operación no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (target.isActive()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Deseas DESACTIVAR el acceso para el usuario '" + target.getUsername() + "'?",
                    "Confirmar Desactivación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                authController.deleteUser(target.getId());
                loadUsersDataAsync();
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Deseas ACTIVAR nuevamente el acceso para el usuario '" + target.getUsername() + "'?",
                    "Confirmar Activación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                authController.activateUser(target.getId());
                loadUsersDataAsync();
            }
        }
    }

    private void showEditUserDialog(AppUser user) {
        if (user == null) return;
        JDialog dialog = new JDialog(this, "Editar Usuario: " + user.getUsername(), true);
        dialog.setSize(460, 390);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Editar Datos de Acceso y Perfil");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setOpaque(false);

        JTextField txtFullName = new JTextField(user.getFullName() != null ? user.getFullName() : "");
        txtFullName.putClientProperty("JTextField.placeholderText", "Nombre completo");

        JTextField txtPhone = new JTextField(user.getPhone() != null ? user.getPhone() : "");
        txtPhone.putClientProperty("JTextField.placeholderText", "Ej: 3001234567");

        JTextField txtUsername = new JTextField(user.getUsername() != null ? user.getUsername() : "");

        JComboBox<Role> cmbRole = new JComboBox<Role>(new Role[]{Role.ENCARGADO, Role.ADMIN});
        if (user.getRole() != null) cmbRole.setSelectedItem(user.getRole());

        JPasswordField txtPass = new JPasswordField();
        txtPass.putClientProperty("JTextField.placeholderText", "Nueva contraseña (opcional)");

        form.add(new JLabel("Nombre Completo:"));
        form.add(txtFullName);
        form.add(new JLabel("Teléfono:"));
        form.add(txtPhone);
        form.add(new JLabel("Nombre de Usuario *:"));
        form.add(txtUsername);
        form.add(new JLabel("Rol de Acceso *:"));
        form.add(cmbRole);
        form.add(new JLabel("Nueva Contraseña:"));
        form.add(txtPass);

        JLabel lblPassHint = new JLabel("💡 Deja la contraseña vacía si deseas conservar la actual.");
        lblPassHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblPassHint.setForeground(MUTED);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(form);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(lblPassHint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("💾 Guardar Cambios", 34);
        btnSave.addActionListener(e -> {
            String uName = txtUsername.getText().trim();
            String fName = txtFullName.getText().trim();
            String phone = txtPhone.getText().trim();
            Role role = (Role) cmbRole.getSelectedItem();
            String newPass = new String(txtPass.getPassword()).trim();

            if (uName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El nombre de usuario no puede estar vacío.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                boolean ok = authController.updateUser(user.getId(), uName, fName, phone, role, newPass.isEmpty() ? null : newPass);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog, "Usuario actualizado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadUsersDataAsync();
                } else {
                    JOptionPane.showMessageDialog(dialog, "No se pudo actualizar el usuario.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(btnCancel);
        buttons.add(btnSave);

        content.add(title, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void showCreateUserDialog() {
        JDialog dialog = new JDialog(this, "Registrar Nuevo Usuario", true);
        dialog.setSize(480, 370);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Crear Cuenta de Usuario");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(PRIMARY);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setOpaque(false);

        JTextField txtFullName = new JTextField();
        txtFullName.putClientProperty("JTextField.placeholderText", "Ej: Juan Carlos Pérez");

        JTextField txtPhone = new JTextField();
        txtPhone.putClientProperty("JTextField.placeholderText", "Ej: 3001234567");

        JTextField txtUsername = new JTextField();
        txtUsername.putClientProperty("JTextField.placeholderText", "Ej: juanp");

        JComboBox<Role> cmbRole = new JComboBox<Role>(new Role[]{Role.ENCARGADO, Role.ADMIN});

        JPasswordField txtPassword = new JPasswordField();
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña segura");

        form.add(new JLabel("Nombre Completo:"));
        form.add(txtFullName);
        form.add(new JLabel("Teléfono:"));
        form.add(txtPhone);
        form.add(new JLabel("Nombre de Usuario *:"));
        form.add(txtUsername);
        form.add(new JLabel("Rol de Acceso *:"));
        form.add(cmbRole);
        form.add(new JLabel("Contraseña *:"));
        form.add(txtPassword);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = createPrimaryButton("＋ Crear Cuenta", 34);
        btnSave.addActionListener(e -> {
            String fullName = txtFullName.getText().trim();
            String phone = txtPhone.getText().trim();
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword()).trim();
            Role role = (Role) cmbRole.getSelectedItem();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Ingresa nombre de usuario y contraseña.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                authController.createUser(user, pass, role, null, fullName.isEmpty() ? null : fullName, phone.isEmpty() ? null : phone);
                dialog.dispose();
                loadUsersDataAsync();
                JOptionPane.showMessageDialog(this, "Cuenta de usuario creada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al crear cuenta: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttons.add(btnCancel);
        buttons.add(btnSave);

        content.add(title, BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void loadCatalogsDataAsync() {
        new SwingWorker<Void, Void>() {
            private List<VehicleTypeCatalog> vehicles;
            private List<WashType> washTypes;

            @Override
            protected Void doInBackground() {
                vehicles = catalogController.getAllVehicleTypesAdmin();
                washTypes = catalogController.getAllWashTypesAdmin();
                return null;
            }

            @Override
            protected void done() {
                adminVehiclesList = vehicles != null ? vehicles : new ArrayList<VehicleTypeCatalog>();
                adminWashTypesList = washTypes != null ? washTypes : new ArrayList<WashType>();

                if (vehicleCatalogModel != null) {
                    vehicleCatalogModel.setRowCount(0);
                    for (VehicleTypeCatalog v : adminVehiclesList) {
                        String commStr = v.getCommissionPct() != null ? v.getCommissionPct().toPlainString() + "%" : "40.00%";
                        vehicleCatalogModel.addRow(new Object[]{v.getId(), v.getName(), commStr, v.isActive() ? "Activo" : "Inactivo"});
                    }
                }
                if (cmbCatalogVehicles != null) {
                    cmbCatalogVehicles.removeAllItems();
                    for (VehicleTypeCatalog v : adminVehiclesList) {
                        if (v.isActive()) {
                            cmbCatalogVehicles.addItem(v);
                        }
                    }
                }
                if (cmbCommissionVehicle != null) {
                    VehicleTypeCatalog prevSel = (VehicleTypeCatalog) cmbCommissionVehicle.getSelectedItem();
                    cmbCommissionVehicle.removeAllItems();
                    for (VehicleTypeCatalog v : adminVehiclesList) {
                        if (v.isActive()) {
                            cmbCommissionVehicle.addItem(v);
                        }
                    }
                    // Restaurar selección anterior si aún existe
                    if (prevSel != null) {
                        for (int i = 0; i < cmbCommissionVehicle.getItemCount(); i++) {
                            if (cmbCommissionVehicle.getItemAt(i).getId().equals(prevSel.getId())) {
                                cmbCommissionVehicle.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
                if (washTypeCatalogModel != null) {
                    washTypeCatalogModel.setRowCount(0);
                    for (WashType wt : adminWashTypesList) {
                        washTypeCatalogModel.addRow(new Object[]{
                                wt.getId(),
                                wt.getVehicleTypeName() != null ? wt.getVehicleTypeName() : "-",
                                wt.getName(),
                                CURRENCY_FMT.format(wt.getPrice()),
                                wt.isActive() ? "Activo" : "Inactivo"
                        });
                    }
                }
            }
        }.execute();
    }

    private void loadUsersDataAsync() {
        new SwingWorker<List<AppUser>, Void>() {
            @Override
            protected List<AppUser> doInBackground() {
                return authController.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<AppUser> users = get();
                    allUsersList = users != null ? users : new ArrayList<AppUser>();
                    if (usersTableModel != null) {
                        usersTableModel.setRowCount(0);
                        for (AppUser u : allUsersList) {
                            usersTableModel.addRow(new Object[]{
                                    u.getId(),
                                    u.getUsername(),
                                    u.getFullName() != null && !u.getFullName().trim().isEmpty() ? u.getFullName() : "--",
                                    u.getPhone() != null && !u.getPhone().trim().isEmpty() ? u.getPhone() : "--",
                                    u.getRole() != null ? u.getRole().name() : "-",
                                    u.isActive() ? "Activo" : "Inactivo"
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================================
    // CARGA ASÍNCRONA INTELIGENTE
    // =========================================================================

    private void showView(String key) {
        cardLayout.show(contentPanel, key);

        for (JButton b : navButtons) {
            b.setBackground(new Color(246, 248, 252));
            b.setForeground(MUTED);
            b.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        }

        int index = 0;
        switch (key) {
            case "dashboard": index = 0; break;
            case "workers": index = 1; break;
            case "history": index = 2; break;
            case "payouts": index = 3; break;
            case "reports": index = 4; break;
            case "catalogs": index = 5; break;
            case "users": index = 6; break;
            case "config": index = 7; break;
        }
        if (index < navButtons.size()) {
            JButton active = navButtons.get(index);
            active.setBackground(new Color(230, 240, 255));
            active.setForeground(PRIMARY);
            active.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, PRIMARY),
                    BorderFactory.createEmptyBorder(8, 7, 8, 10)));
        }

        if (!loadedViews.contains(key)) {
            loadedViews.add(key);
            switch (key) {
                case "dashboard": loadDashboardDataAsync(); break;
                case "workers": loadWorkersDataAsync(); break;
                case "history": loadHistoryDataAsync(); break;
                case "payouts": loadPayoutsDataAsync(); break;
                case "reports": loadReportsDataAsync(); break;
                case "catalogs": loadCatalogsDataAsync(); break;
                case "users": loadUsersDataAsync(); break;
            }
        }
    }

    private void loadDashboardDataAsync() {
        new SwingWorker<DashboardService.DashboardBundle, Void>() {
            private List<WashTableRowDTO> recentWashes;
            private List<WorkerKpiDTO> workerKpis;

            @Override
            protected DashboardService.DashboardBundle doInBackground() {
                LocalDate today = LocalDate.now();
                DashboardService.DashboardBundle bundle = dashboardController.getDashboardBundle(today);
                recentWashes = washController.toTableRows(bundle.todayWashes);
                workerKpis = workerController.buildKpisFromWashes(bundle.todayWashes, today, today);
                return bundle;
            }

            @Override
            protected void done() {
                try {
                    DashboardService.DashboardBundle bundle = get();
                    applyDashboardMetrics(bundle.metrics);
                    applyRecentWashes(recentWashes);
                    applyWorkerCards(workerKpis);

                    if (dbStatusBadge != null) {
                        dbStatusBadge.setText(container.isConnectedToDatabase() ? "☁ Supabase" : "⚡ En Memoria");
                        dbStatusBadge.setForeground(container.isConnectedToDatabase() ? GREEN : PRIMARY);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void applyDashboardMetrics(DashboardMetricsDTO m) {
        kpiFormulaIncome.setText(CURRENCY_FMT.format(m.getTotalIncome()));
        kpiFormulaPayout.setText(CURRENCY_FMT.format(m.getTotalWorkerPayments()));
        kpiFormulaProfit.setText(CURRENCY_FMT.format(m.getTotalNetProfit()));

        kpiWashesCount.setText(String.valueOf(m.getTotalWashes()));
        kpiWashesSubtitle.setText(m.getCarsCount() + " Carros | " + m.getMotosCount() + " Motos");

        kpiIncomeTotal.setText(CURRENCY_FMT.format(m.getTotalIncome()));
        kpiIncomeSubtitle.setText("Ticket prom: " + CURRENCY_FMT.format(m.getAvgTicketPrice()));

        kpiPayoutsTotal.setText(CURRENCY_FMT.format(m.getTotalWorkerPayments()));
        kpiPayoutsSubtitle.setText(workerController.getActiveWorkers().size() + " Operadores activos");

        kpiProfitTotal.setText(CURRENCY_FMT.format(m.getTotalNetProfit()));
        kpiProfitSubtitle.setText(m.getProfitMarginPct() + "% Margen libre");

        ratioVehiclesTotalLabel.setText(m.getTotalWashes() + " Vehículos");
        ratioCarrosLabel.setText("🚗 Carros: " + m.getCarsCount() + " (" + m.getCarsPercentage() + "%)");
        ratioMotosLabel.setText("🏍 Motos: " + m.getMotosCount() + " (" + m.getMotosPercentage() + "%)");
        ratioCarsCount = m.getCarsCount();
        ratioTotalWashes = m.getTotalWashes();
        if (operationalRatioBar != null) operationalRatioBar.repaint();

        updateWeeklyBars(m.getWeeklyTrend());
    }

    private void applyRecentWashes(List<WashTableRowDTO> rows) {
        if (recentWashesModel == null) return;
        recentWashesModel.setRowCount(0);
        for (WashTableRowDTO r : rows) {
            recentWashesModel.addRow(new Object[]{
                    r.getWashTime(),
                    r.getVehicleTypeLabel(),
                    r.getPlate(),
                    r.getVehicleModel().isEmpty() ? "—" : r.getVehicleModel(),
                    r.getWorkerName(),
                    CURRENCY_FMT.format(r.getPrice()),
                    CURRENCY_FMT.format(r.getWorkerPayment()),
                    "+" + CURRENCY_FMT.format(r.getProfit())
            });
        }
    }

    private void applyWorkerCards(List<WorkerKpiDTO> kpis) {
        if (workerCardsContainer == null) return;
        workerCardsContainer.removeAll();

        for (WorkerKpiDTO k : kpis) {
            if (!k.isActive()) continue;

            JPanel card = new JPanel(new BorderLayout(8, 0));
            card.setOpaque(true);
            card.setBackground(new Color(248, 250, 255));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(228, 234, 248), 1),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            JLabel name = new JLabel(k.getWorkerName() + " (" + k.getBay() + ")");
            name.setFont(new Font("SansSerif", Font.BOLD, 11));
            name.setForeground(TEXT);

            JLabel details = new JLabel(k.getTotalWashes() + " lavados | Generó: " + CURRENCY_FMT.format(k.getTotalGenerated())
                    + " | Su pago: " + CURRENCY_FMT.format(k.getTotalCommission()));
            details.setFont(new Font("SansSerif", Font.PLAIN, 10));
            details.setForeground(MUTED);

            card.add(name, BorderLayout.WEST);
            card.add(details, BorderLayout.EAST);

            workerCardsContainer.add(card);
            workerCardsContainer.add(Box.createVerticalStrut(3));
        }

        workerCardsContainer.revalidate();
        workerCardsContainer.repaint();
    }

    private void updateWeeklyBars(List<DayTrendDTO> trend) {
        if (weeklyBarsContainer == null) return;
        weeklyBarsContainer.removeAll();

        if (trend == null || trend.isEmpty()) {
            weeklyBarsContainer.revalidate();
            weeklyBarsContainer.repaint();
            return;
        }

        BigDecimal maxIncome = BigDecimal.ZERO;
        for (DayTrendDTO d : trend) {
            if (d.getTotalIncome().compareTo(maxIncome) > 0) {
                maxIncome = d.getTotalIncome();
            }
        }
        if (maxIncome.signum() == 0) maxIncome = new BigDecimal("1");

        for (DayTrendDTO d : trend) {
            JPanel barCol = new JPanel(new BorderLayout());
            barCol.setOpaque(false);

            final BigDecimal dayIncome = d.getTotalIncome();
            final BigDecimal max = maxIncome;

            JPanel bar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int h = getHeight();
                    int w = getWidth();
                    int barH = (int) ((dayIncome.doubleValue() / max.doubleValue()) * (h - 12));
                    if (barH < 3 && dayIncome.signum() > 0) barH = 3;

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(dayIncome.signum() > 0 ? PRIMARY : new Color(220, 226, 240));
                    g2.fillRoundRect(w / 4, h - barH - 2, w / 2, barH, 3, 3);
                    g2.dispose();
                }
            };
            bar.setOpaque(false);

            JLabel label = new JLabel(d.getDayName(), SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.PLAIN, 9));
            label.setForeground(MUTED);

            barCol.add(bar, BorderLayout.CENTER);
            barCol.add(label, BorderLayout.SOUTH);
            weeklyBarsContainer.add(barCol);
        }

        weeklyBarsContainer.revalidate();
        weeklyBarsContainer.repaint();
    }

    private void loadWorkersDataAsync() {
        new SwingWorker<List<WorkerKpiDTO>, Void>() {
            @Override
            protected List<WorkerKpiDTO> doInBackground() {
                return workerController.getAllWorkersKpiHistoric();
            }
            @Override
            protected void done() {
                try {
                    List<WorkerKpiDTO> list = get();
                    if (workersTableModel == null) return;
                    workersTableModel.setRowCount(0);
                    for (WorkerKpiDTO k : list) {
                        workersTableModel.addRow(new Object[]{
                                k.getWorkerId(),
                                k.getWorkerName(),
                                (k.getPhone() != null && !k.getPhone().trim().isEmpty()) ? k.getPhone() : "—",
                                k.getBay(),
                                k.getShift(),
                                k.getTotalWashes(),
                                CURRENCY_FMT.format(k.getTotalGenerated()),
                                CURRENCY_FMT.format(k.getTotalCommission()),
                                k.isActive() ? "Activo" : "Inactivo"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadHistoryDataAsync() {
        String filter = cmbHistoryDateFilter != null ? (String) cmbHistoryDateFilter.getSelectedItem() : "📅 Todos los Registros";
        if (filter == null) filter = "📅 Todos los Registros";
        final String finalFilter = filter;

        new SwingWorker<List<WashTableRowDTO>, Void>() {
            @Override
            protected List<WashTableRowDTO> doInBackground() {
                LocalDate today = LocalDate.now();
                if (finalFilter.contains("Hoy")) {
                    return washController.getWashesTableToday();
                } else if (finalFilter.contains("Ayer")) {
                    LocalDate yesterday = today.minusDays(1);
                    return washController.getWashesTableByRange(yesterday, yesterday);
                } else if (finalFilter.contains("Esta Semana")) {
                    LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    return washController.getWashesTableByRange(startOfWeek, today);
                } else if (finalFilter.contains("Este Mes")) {
                    LocalDate startOfMonth = today.withDayOfMonth(1);
                    return washController.getWashesTableByRange(startOfMonth, today);
                } else {
                    return washController.getAllWashesTable();
                }
            }
            @Override
            protected void done() {
                try {
                    currentLoadedHistory = get();
                    filterHistoryTableLocal();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterHistoryTableLocal() {
        if (allWashesModel == null) return;
        allWashesModel.setRowCount(0);

        String query = txtSearchHistory != null ? txtSearchHistory.getText().trim().toLowerCase() : "";
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (WashTableRowDTO r : currentLoadedHistory) {
            String placa = r.getPlate() != null ? r.getPlate() : "";
            String cliente = r.getClientName() != null ? r.getClientName() : "";
            String telefono = r.getClientPhone() != null ? r.getClientPhone() : "";
            String operador = r.getWorkerName() != null ? r.getWorkerName() : "";
            String tipoLav = r.getWashTypeName() != null ? r.getWashTypeName() : "";

            if (!query.isEmpty()) {
                boolean matches = placa.toLowerCase().contains(query)
                        || cliente.toLowerCase().contains(query)
                        || telefono.toLowerCase().contains(query)
                        || operador.toLowerCase().contains(query)
                        || tipoLav.toLowerCase().contains(query);
                if (!matches) continue;
            }

            count++;
            if (r.getPrice() != null) total = total.add(r.getPrice());

            allWashesModel.addRow(new Object[]{
                    count,
                    r.getWashDate(),
                    r.getWashTime(),
                    cliente.isEmpty() ? "--" : cliente,
                    telefono.isEmpty() ? "--" : telefono,
                    placa,
                    r.getVehicleTypeLabel(),
                    tipoLav.isEmpty() ? "--" : tipoLav,
                    operador,
                    CURRENCY_FMT.format(r.getPrice()),
                    r.getPayoutStatus()
            });
        }

        if (lblHistoryCount != null) {
            lblHistoryCount.setText(count + " lavado" + (count == 1 ? "" : "s"));
        }
        if (lblHistoryTotal != null) {
            lblHistoryTotal.setText("Total: " + CURRENCY_FMT.format(total));
        }
    }

    private void loadPayoutsDataAsync() {
        String filter = cmbPayoutDateFilterAdmin != null ? (String) cmbPayoutDateFilterAdmin.getSelectedItem() : "📅 Todos los Registros";
        if (filter == null) filter = "📅 Todos los Registros";
        final String finalFilter = filter;

        new SwingWorker<List<WorkerPayout>, Void>() {
            @Override
            protected List<WorkerPayout> doInBackground() {
                LocalDate today = LocalDate.now();
                if (finalFilter.contains("Hoy")) {
                    return payoutController.getPayoutsToday();
                } else if (finalFilter.contains("Ayer")) {
                    LocalDate yesterday = today.minusDays(1);
                    return payoutController.getPayoutsByDateRange(yesterday, yesterday);
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
                    currentLoadedPayoutsAdmin = get();
                    filterPayoutsTableLocal();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void filterPayoutsTableLocal() {
        if (payoutsTableModel == null) return;
        payoutsTableModel.setRowCount(0);

        String query = txtSearchPayoutsAdmin != null ? txtSearchPayoutsAdmin.getText().trim().toLowerCase() : "";
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;

        List<Worker> activeWorkers = container.getWorkerController().getActiveWorkers();
        Map<Long, String> workerNames = new HashMap<Long, String>();
        for (Worker wk : activeWorkers) {
            workerNames.put(wk.getId(), wk.getName());
        }

        for (WorkerPayout p : currentLoadedPayoutsAdmin) {
            String workerName = workerNames.containsKey(p.getWorkerId())
                    ? workerNames.get(p.getWorkerId())
                    : "ID #" + p.getWorkerId();
            String metodo = p.getPaymentMethod() != null ? p.getPaymentMethod().getLabel() : "Efectivo";
            String notas = p.getNotes() != null ? p.getNotes() : "";

            if (!query.isEmpty()) {
                boolean matches = workerName.toLowerCase().contains(query)
                        || metodo.toLowerCase().contains(query)
                        || notas.toLowerCase().contains(query);
                if (!matches) continue;
            }

            count++;
            if (p.getAmount() != null) total = total.add(p.getAmount());

            payoutsTableModel.addRow(new Object[]{
                    count,
                    "#" + p.getId(),
                    p.getPaymentDate(),
                    p.getPaymentTime(),
                    workerName,
                    CURRENCY_FMT.format(p.getAmount()),
                    metodo,
                    p.getStatus() != null ? p.getStatus().getLabel() : "Completado",
                    notas.isEmpty() ? "—" : notas
            });
        }

        if (lblCountPayoutsAdmin != null) {
            lblCountPayoutsAdmin.setText(count + " pago" + (count == 1 ? "" : "s"));
        }
        if (lblTotalPayoutsAdmin != null) {
            lblTotalPayoutsAdmin.setText("Total: " + CURRENCY_FMT.format(total));
        }
    }

    private void loadReportsDataAsync() {
        String filter = cmbReportPeriodFilter != null ? (String) cmbReportPeriodFilter.getSelectedItem() : "📅 Este Mes";
        LocalDate today = LocalDate.now();
        final LocalDate fromDate;
        final LocalDate toDate;

        if (filter != null && filter.contains("Hoy")) {
            fromDate = today;
            toDate = today;
        } else if (filter != null && filter.contains("Ayer")) {
            fromDate = today.minusDays(1);
            toDate = today.minusDays(1);
        } else if (filter != null && filter.contains("Esta Semana")) {
            fromDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            toDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } else if (filter != null && filter.contains("Últimos 30 Días")) {
            fromDate = today.minusDays(29);
            toDate = today;
        } else if (filter != null && filter.contains("Todo el Historial")) {
            fromDate = today.minusYears(5);
            toDate = today;
        } else {
            // "Este Mes" por defecto
            fromDate = today.withDayOfMonth(1);
            toDate = today.withDayOfMonth(today.lengthOfMonth());
        }

        new SwingWorker<List<FinancialReportDTO>, Void>() {
            @Override
            protected List<FinancialReportDTO> doInBackground() {
                return reportController.getDailyReport(fromDate, toDate);
            }

            @Override
            protected void done() {
                try {
                    List<FinancialReportDTO> list = get();
                    if (list == null) list = new ArrayList<>();
                    updateFinancialReportsView(list);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void updateFinancialReportsView(List<FinancialReportDTO> list) {
        if (dailyReportsModel == null) return;
        dailyReportsModel.setRowCount(0);

        int totalWashes = 0;
        BigDecimal grossIncome = BigDecimal.ZERO;
        BigDecimal commissions = BigDecimal.ZERO;
        BigDecimal disbursed = BigDecimal.ZERO;
        BigDecimal netProfit = BigDecimal.ZERO;
        Map<String, BigDecimal> vehicleIncomeTotals = new LinkedHashMap<>();
        Map<String, Integer> vehicleCountTotals = new LinkedHashMap<>();

        // Llenar tabla cronológica
        for (FinancialReportDTO r : list) {
            totalWashes += r.getTotalWashes();
            grossIncome = grossIncome.add(r.getGrossIncome());
            commissions = commissions.add(r.getTotalCommissions());
            disbursed = disbursed.add(r.getCashPayoutsDisbursed());
            netProfit = netProfit.add(r.getNetProfit());

            // Acumular por vehículo
            if (r.getIncomeByVehicle() != null) {
                for (Map.Entry<String, BigDecimal> e : r.getIncomeByVehicle().entrySet()) {
                    vehicleIncomeTotals.put(e.getKey(), vehicleIncomeTotals.getOrDefault(e.getKey(), BigDecimal.ZERO).add(e.getValue()));
                }
            }
            if (r.getCountByVehicle() != null) {
                for (Map.Entry<String, Integer> e : r.getCountByVehicle().entrySet()) {
                    vehicleCountTotals.put(e.getKey(), vehicleCountTotals.getOrDefault(e.getKey(), 0) + e.getValue());
                }
            }

            BigDecimal rowMargin = r.getGrossIncome().signum() > 0
                    ? r.getNetProfit().multiply(new BigDecimal("100")).divide(r.getGrossIncome(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            dailyReportsModel.addRow(new Object[]{
                    r.getPeriodLabel(),
                    r.getTotalWashes(),
                    CURRENCY_FMT.format(r.getGrossIncome()),
                    CURRENCY_FMT.format(r.getTotalCommissions()),
                    CURRENCY_FMT.format(r.getCashPayoutsDisbursed()),
                    CURRENCY_FMT.format(r.getNetProfit()),
                    rowMargin.toPlainString() + "%"
            });
        }

        // Fila destacada de consolidado total
        BigDecimal globalMargin = grossIncome.signum() > 0
                ? netProfit.multiply(new BigDecimal("100")).divide(grossIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgTicket = totalWashes > 0
                ? grossIncome.divide(BigDecimal.valueOf(totalWashes), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (!list.isEmpty()) {
            dailyReportsModel.addRow(new Object[]{
                    "★ TOTALES CONSOLIDADOS",
                    totalWashes,
                    CURRENCY_FMT.format(grossIncome),
                    CURRENCY_FMT.format(commissions),
                    CURRENCY_FMT.format(disbursed),
                    CURRENCY_FMT.format(netProfit),
                    globalMargin.toPlainString() + "%"
            });
        }

        // Actualizar KPIs superiores
        if (repKpiIncomeTotal != null) repKpiIncomeTotal.setText(CURRENCY_FMT.format(grossIncome));
        if (repKpiIncomeSub != null) repKpiIncomeSub.setText(totalWashes + " servicio" + (totalWashes == 1 ? "" : "s") + " facturados");

        if (repKpiCommissionsTotal != null) repKpiCommissionsTotal.setText(CURRENCY_FMT.format(commissions));
        if (repKpiCommissionsSub != null) repKpiCommissionsSub.setText("Desembolsado: " + CURRENCY_FMT.format(disbursed));

        if (repKpiProfitTotal != null) repKpiProfitTotal.setText(CURRENCY_FMT.format(netProfit));
        if (repKpiProfitSub != null) repKpiProfitSub.setText("Ganancia libre para el dueño");

        if (repKpiMarginTotal != null) repKpiMarginTotal.setText(globalMargin.toPlainString() + "%");
        if (repKpiMarginSub != null) repKpiMarginSub.setText(grossIncome.signum() > 0 ? "Rentabilidad neta del lavadero" : "Sin ventas en período");

        if (repKpiTicketAvg != null) repKpiTicketAvg.setText(CURRENCY_FMT.format(avgTicket));
        if (repKpiTicketSub != null) repKpiTicketSub.setText("Ingreso promedio por vehículo");

        if (repTableSummaryLabel != null) {
            repTableSummaryLabel.setText("Total Facturado: " + CURRENCY_FMT.format(grossIncome) + " | Utilidad: " + CURRENCY_FMT.format(netProfit));
        }

        // Renderizar Gráfica A: Tendencia Temporal (Barras duales)
        renderTrendChart(list);

        // Renderizar Gráfica B: Rendimiento por Tipo de Vehículo
        renderVehicleChart(vehicleIncomeTotals, vehicleCountTotals, grossIncome);
    }

    private void renderTrendChart(List<FinancialReportDTO> list) {
        if (repChartTrendPanel == null) return;
        repChartTrendPanel.removeAll();

        if (list == null || list.isEmpty()) {
            JLabel empty = new JLabel("No hay datos registrados en el período seleccionado.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            empty.setForeground(MUTED);
            repChartTrendPanel.add(empty, BorderLayout.CENTER);
            repChartTrendPanel.revalidate();
            repChartTrendPanel.repaint();
            return;
        }

        // Encontrar maximo para escalar barras
        BigDecimal maxVal = BigDecimal.ONE;
        for (FinancialReportDTO r : list) {
            if (r.getGrossIncome().compareTo(maxVal) > 0) maxVal = r.getGrossIncome();
            if (r.getNetProfit().compareTo(maxVal) > 0) maxVal = r.getNetProfit();
        }
        final BigDecimal scaleMax = maxVal;

        JPanel barsBox = new JPanel(new GridLayout(1, Math.min(list.size(), 14), 8, 0));
        barsBox.setOpaque(false);
        barsBox.setBorder(BorderFactory.createEmptyBorder(10, 8, 4, 8));

        // Mostrar hasta los últimos 14 registros para que no se aprieten
        int startIdx = Math.max(0, list.size() - 14);
        for (int i = startIdx; i < list.size(); i++) {
            FinancialReportDTO r = list.get(i);
            final BigDecimal income = r.getGrossIncome();
            final BigDecimal profit = r.getNetProfit();

            JPanel col = new JPanel(new BorderLayout(0, 4));
            col.setOpaque(false);

            JPanel barCanvas = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int h = getHeight() - 10;
                    int w = getWidth();
                    int barWidth = Math.max(6, (w / 2) - 4);

                    int hIncome = scaleMax.signum() > 0 ? (int) ((income.doubleValue() / scaleMax.doubleValue()) * h) : 0;
                    int hProfit = scaleMax.signum() > 0 ? (int) ((profit.doubleValue() / scaleMax.doubleValue()) * h) : 0;
                    if (hIncome < 3 && income.signum() > 0) hIncome = 3;
                    if (hProfit < 3 && profit.signum() > 0) hProfit = 3;

                    // Barra Facturación (Azul)
                    g2.setColor(PRIMARY);
                    g2.fillRoundRect(w / 2 - barWidth - 2, h - hIncome, barWidth, hIncome, 4, 4);

                    // Barra Ganancia Neta (Verde)
                    g2.setColor(GREEN);
                    g2.fillRoundRect(w / 2 + 2, h - hProfit, barWidth, hProfit, 4, 4);

                    g2.dispose();
                }
            };
            barCanvas.setOpaque(false);

            String shortLabel = r.getPeriod();
            if (shortLabel != null && shortLabel.length() >= 5) {
                shortLabel = shortLabel.substring(5); // Ej: 11-10
            }
            JLabel lblDate = new JLabel(shortLabel != null ? shortLabel : "-", SwingConstants.CENTER);
            lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblDate.setForeground(MUTED);

            col.add(barCanvas, BorderLayout.CENTER);
            col.add(lblDate, BorderLayout.SOUTH);
            barsBox.add(col);
        }

        repChartTrendPanel.add(barsBox, BorderLayout.CENTER);
        repChartTrendPanel.revalidate();
        repChartTrendPanel.repaint();
    }

    private void renderVehicleChart(Map<String, BigDecimal> vehicleIncomeTotals, Map<String, Integer> vehicleCountTotals, BigDecimal grossIncome) {
        if (repChartVehiclePanel == null) return;
        repChartVehiclePanel.removeAll();

        if (vehicleIncomeTotals == null || vehicleIncomeTotals.isEmpty() || grossIncome.signum() == 0) {
            JLabel empty = new JLabel("Sin servicios registrados en el período.", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            empty.setForeground(MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            repChartVehiclePanel.add(Box.createVerticalGlue());
            repChartVehiclePanel.add(empty);
            repChartVehiclePanel.add(Box.createVerticalGlue());
            repChartVehiclePanel.revalidate();
            repChartVehiclePanel.repaint();
            return;
        }

        repChartVehiclePanel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

        Color[] colors = {PRIMARY, new Color(16, 185, 129), new Color(245, 158, 11), new Color(139, 92, 246), new Color(236, 72, 153)};
        int cIdx = 0;

        for (Map.Entry<String, BigDecimal> entry : vehicleIncomeTotals.entrySet()) {
            String vName = entry.getKey();
            BigDecimal vIncome = entry.getValue();
            int count = vehicleCountTotals.getOrDefault(vName, 0);
            double pct = grossIncome.signum() > 0 ? (vIncome.doubleValue() / grossIncome.doubleValue()) * 100.0 : 0.0;
            final Color barColor = colors[cIdx % colors.length];
            cIdx++;

            JPanel row = new JPanel(new BorderLayout(8, 2));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(800, 36));

            JPanel labels = new JPanel(new BorderLayout());
            labels.setOpaque(false);

            JLabel lblName = new JLabel("🚗 " + vName + " (" + count + " lavados)");
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblName.setForeground(TEXT);

            JLabel lblVal = new JLabel(CURRENCY_FMT.format(vIncome) + " (" + String.format(java.util.Locale.US, "%.1f", pct) + "%)");
            lblVal.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblVal.setForeground(barColor);

            labels.add(lblName, BorderLayout.WEST);
            labels.add(lblVal, BorderLayout.EAST);

            // Barra horizontal de progreso
            JPanel pBar = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(new Color(240, 243, 250));
                    g2.fillRoundRect(0, 1, w, h - 2, 4, 4);

                    int fillW = (int) ((pct / 100.0) * w);
                    if (fillW < 4 && pct > 0) fillW = 4;
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 1, fillW, h - 2, 4, 4);
                    g2.dispose();
                }
            };
            pBar.setOpaque(false);
            pBar.setPreferredSize(new Dimension(0, 8));

            row.add(labels, BorderLayout.NORTH);
            row.add(pBar, BorderLayout.CENTER);
            repChartVehiclePanel.add(row);
            repChartVehiclePanel.add(Box.createVerticalStrut(6));
        }

        repChartVehiclePanel.revalidate();
        repChartVehiclePanel.repaint();
    }

    private void filterBySearch(String query) {
        if (query.isEmpty()) {
            loadDashboardDataAsync();
            return;
        }
        new SwingWorker<List<WashTableRowDTO>, Void>() {
            @Override
            protected List<WashTableRowDTO> doInBackground() {
                return washController.searchByPlate(query);
            }
            @Override
            protected void done() {
                try {
                    applyRecentWashes(get());
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void showRegisterWorkerDialog() {
        JDialog dialog = new JDialog(this, "Registrar Nuevo Operador", true);
        dialog.setSize(380, 290);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 12));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JTextField txtName = new JTextField();
        JTextField txtPhone = new JTextField();
        JComboBox<String> cmbBay = new JComboBox<String>(new String[]{"Bahía 1", "Bahía 2", "Bahía 3", "Bahía 4"});
        JComboBox<String> cmbShift = new JComboBox<String>(new String[]{"Turno Completo", "Medio Turno"});

        form.add(new JLabel("Nombre:"));
        form.add(txtName);
        form.add(new JLabel("Teléfono:"));
        form.add(txtPhone);
        form.add(new JLabel("Bahía:"));
        form.add(cmbBay);
        form.add(new JLabel("Turno:"));
        form.add(cmbShift);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = createPrimaryButton("Guardar", 32);
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                workerController.createWorker(name, txtPhone.getText().trim(), (String) cmbBay.getSelectedItem(), (String) cmbShift.getSelectedItem());
                dialog.dispose();
                loadDashboardDataAsync();
                loadWorkersDataAsync();
                JOptionPane.showMessageDialog(this, "Operador registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnSave);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showPayoutDialog() {
        List<Worker> workers = workerController.getActiveWorkers();
        if (workers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay operadores activos.", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Liquidar Turno a Operador (Nómina)", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 12));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JComboBox<WorkerItem> cmbWorker = new JComboBox<WorkerItem>();
        for (Worker w : workers) {
            cmbWorker.addItem(new WorkerItem(w.getId(), w.getName() + " (" + w.getBay() + ")"));
        }

        JLabel lblPending = new JLabel("$0");
        lblPending.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPending.setForeground(new Color(186, 26, 26));

        JTextField txtAmount = new JTextField();
        JComboBox<String> cmbMethod = new JComboBox<String>(new String[]{"Efectivo", "Nequi", "Daviplata", "Transferencia"});
        JTextField txtNotes = new JTextField("Liquidación jornada");

        Runnable updatePending = () -> {
            WorkerItem item = (WorkerItem) cmbWorker.getSelectedItem();
            if (item != null) {
                BigDecimal pending = payoutController.getPendingAmount(item.id);
                lblPending.setText(CURRENCY_FMT.format(pending));
                txtAmount.setText(pending.toString());
            }
        };

        cmbWorker.addActionListener(e -> updatePending.run());
        updatePending.run();

        form.add(new JLabel("Operador:"));
        form.add(cmbWorker);
        form.add(new JLabel("Saldo Pendiente:"));
        form.add(lblPending);
        form.add(new JLabel("Monto a Liquidar:"));
        form.add(txtAmount);
        form.add(new JLabel("Método de Pago:"));
        form.add(cmbMethod);
        form.add(new JLabel("Notas:"));
        form.add(txtNotes);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnPay = createPrimaryButton("Confirmar Pago", 32);
        btnPay.addActionListener(e -> {
            WorkerItem item = (WorkerItem) cmbWorker.getSelectedItem();
            if (item == null) return;
            try {
                BigDecimal amount = new BigDecimal(txtAmount.getText().trim());
                payoutController.registerPayout(new CreatePayoutRequest(
                        item.id, amount, (String) cmbMethod.getSelectedItem(), txtNotes.getText().trim()));
                dialog.dispose();
                loadDashboardDataAsync();
                loadPayoutsDataAsync();
                JOptionPane.showMessageDialog(this, "Liquidación registrada y lavados marcados como pagados.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnPay);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static class WorkerItem {
        final Long id;
        final String name;
        WorkerItem(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private void updateClock() {
        if (clockLabel != null) {
            clockLabel.setText("🕒 " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private JButton createPrimaryButton(String text, int height) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, height));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private JButton createNavButton(String icon, String label, boolean selected) {
        JButton btn = new JButton(icon + "  " + label);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setPreferredSize(new Dimension(190, 34));
        btn.setBackground(selected ? new Color(230, 240, 255) : new Color(246, 248, 252));
        btn.setForeground(selected ? PRIMARY : MUTED);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return btn;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setSelectionBackground(new Color(236, 244, 255));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBackground(new Color(246, 248, 254));
        header.setForeground(MUTED);
        header.setPreferredSize(new Dimension(0, 30));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(new Font("SansSerif", Font.BOLD, 11));
                if (!isSelected) {
                    String val = value != null ? value.toString() : "";
                    if ("Activo".equalsIgnoreCase(val)) {
                        c.setForeground(new Color(22, 101, 52));
                    } else if ("Inactivo".equalsIgnoreCase(val)) {
                        c.setForeground(new Color(185, 28, 28));
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            String colName = table.getColumnName(i);
            if (colName.contains("PRECIO") || colName.contains("PAGO") || colName.contains("COBRADO")
                    || colName.contains("GENERADO") || colName.contains("MONTO") || colName.contains("INGRESO") || colName.contains("GANANCIA")) {
                table.getColumnModel().getColumn(i).setCellRenderer(right);
            } else if (colName.equals("ESTADO")) {
                table.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
            } else if (colName.equals("HORA") || colName.equals("FECHA") || colName.equals("PLACA") || colName.equals("ID") || colName.equals("TIPO")) {
                table.getColumnModel().getColumn(i).setCellRenderer(center);
            }
        }

        return table;
    }
}
