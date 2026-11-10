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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private DefaultTableModel payoutsTableModel;
    private DefaultTableModel dailyReportsModel;
    private DefaultTableModel vehicleCatalogModel;
    private DefaultTableModel washTypeCatalogModel;
    private DefaultTableModel usersTableModel;
    private JComboBox<VehicleTypeCatalog> cmbCatalogVehicles;
    private List<VehicleTypeCatalog> adminVehiclesList = new ArrayList<VehicleTypeCatalog>();
    private List<WashType> adminWashTypesList = new ArrayList<WashType>();

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

        // 1. Cliente y Placa
        JPanel clientPlateRow = new JPanel(new GridLayout(1, 2, 12, 0));
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
        txtClient.putClientProperty("JTextField.placeholderText", "Nombre del cliente");
        clientCol.add(clientLabel);
        clientCol.add(Box.createVerticalStrut(2));
        clientCol.add(txtClient);

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
            BigDecimal wp = p.multiply(new BigDecimal("40.00")).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal profit = p.subtract(wp);
            previewPriceLabel.setText(CURRENCY_FMT.format(p));
            previewPaymentLabel.setText(CURRENCY_FMT.format(wp));
            previewProfitLabel.setText(CURRENCY_FMT.format(profit));
        };

        cmbVehicles.addActionListener(e -> updateWashTypes.run());
        cmbWashes.addActionListener(e -> {
            WashType selW = (WashType) cmbWashes.getSelectedItem();
            if (selW != null) {
                currentPrice[0] = selW.getPrice();
                BigDecimal p = currentPrice[0];
                BigDecimal wp = p.multiply(new BigDecimal("40.00")).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
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
            request.setVehicleModel(selV.getName());
            request.setVehicleTypeCatalogId(selV.getId());
            request.setVehicleTypeName(selV.getName());
            request.setWashTypeId(selW.getId());
            request.setWashTypeName(selW.getName());
            request.setWorkerId(modalSelectedWorkerId[0]);
            request.setPrice(selW.getPrice());
            request.setCommissionPct(new BigDecimal("40.00"));
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

        JButton btnNewWorker = createPrimaryButton("＋ Registrar Operador", 34);
        btnNewWorker.addActionListener(e -> showRegisterWorkerDialog());

        header.add(title, BorderLayout.WEST);
        header.add(btnNewWorker, BorderLayout.EAST);

        String[] cols = {"ID", "NOMBRE", "TELÉFONO", "BAHÍA", "TURNO", "LAVADOS", "GENERADO", "COMISIÓN", "ESTADO"};
        workersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(workersTableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Historial General de Lavados");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        header.add(title, BorderLayout.WEST);

        String[] cols = {"FECHA", "HORA", "PLACA", "TIPO", "MODELO", "OPERADOR", "PRECIO", "PAGO TRAB.", "GANANCIA", "ESTADO"};
        allWashesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(allWashesModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPayoutsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Liquidación de Comisiones y Desembolsos");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        JButton btnPayout = createPrimaryButton("💵 Liquidar Turno a Operador", 34);
        btnPayout.addActionListener(e -> showPayoutDialog());

        header.add(title, BorderLayout.WEST);
        header.add(btnPayout, BorderLayout.EAST);

        String[] cols = {"ID PAGO", "FECHA", "HORA", "OPERADOR", "MONTO LIQUIDADO", "MÉTODO PAGO", "ESTADO", "NOTAS"};
        payoutsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(payoutsTableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReportsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Reportes Financieros de Ingresos");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);

        header.add(title, BorderLayout.WEST);

        String[] cols = {"PERÍODO", "LAVADOS", "CARROS", "MOTOS", "INGRESO BRUTO", "COMISIONES", "CAJA DESEMBOLSOS", "GANANCIA NETA"};
        dailyReportsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = createStyledTable(dailyReportsModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildConfigView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JLabel title = new JLabel("Configuración de Tarifas y Comisiones");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        JLabel desc = new JLabel("Ajusta los precios base y porcentaje de comisión en cualquier momento:");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(desc);
        panel.add(Box.createVerticalStrut(16));

        ServiceRate rateCarro = serviceRateRepository.findByVehicleType(VehicleType.CARRO);
        ServiceRate rateMoto = serviceRateRepository.findByVehicleType(VehicleType.MOTO);

        JPanel formCard = new JPanel(new GridLayout(2, 4, 12, 12));
        formCard.setBackground(SURFACE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        formCard.setMaximumSize(new Dimension(720, 110));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField txtPriceCarro = new JTextField(rateCarro != null ? rateCarro.getDefaultPrice().toString() : "30000");
        JTextField txtCommCarro = new JTextField(rateCarro != null ? rateCarro.getCommissionPct().toString() : "40");
        JTextField txtPriceMoto = new JTextField(rateMoto != null ? rateMoto.getDefaultPrice().toString() : "20000");
        JTextField txtCommMoto = new JTextField(rateMoto != null ? rateMoto.getCommissionPct().toString() : "40");

        formCard.add(new JLabel("🚗 Precio Carro ($):"));
        formCard.add(txtPriceCarro);
        formCard.add(new JLabel("Comisión Carro (%):"));
        formCard.add(txtCommCarro);

        formCard.add(new JLabel("🏍 Precio Moto ($):"));
        formCard.add(txtPriceMoto);
        formCard.add(new JLabel("Comisión Moto (%):"));
        formCard.add(txtCommMoto);

        panel.add(formCard);
        panel.add(Box.createVerticalStrut(14));

        JButton btnSaveRates = createPrimaryButton("Guardar Tarifas", 36);
        btnSaveRates.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSaveRates.addActionListener(e -> {
            try {
                if (rateCarro != null) {
                    rateCarro.setDefaultPrice(new BigDecimal(txtPriceCarro.getText().trim()));
                    rateCarro.setCommissionPct(new BigDecimal(txtCommCarro.getText().trim()));
                    serviceRateRepository.update(rateCarro);
                }
                if (rateMoto != null) {
                    rateMoto.setDefaultPrice(new BigDecimal(txtPriceMoto.getText().trim()));
                    rateMoto.setCommissionPct(new BigDecimal(txtCommMoto.getText().trim()));
                    serviceRateRepository.update(rateMoto);
                }
                updateVehicleSelectionUI();
                updateFormPreview();
                JOptionPane.showMessageDialog(this, "Tarifas actualizadas correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar tarifas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(btnSaveRates);

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

        String[] vCols = {"ID", "NOMBRE", "ESTADO"};
        vehicleCatalogModel = new DefaultTableModel(vCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable vTable = createStyledTable(vehicleCatalogModel);
        vTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        vTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        vTable.getColumnModel().getColumn(2).setPreferredWidth(70);
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
                    String status = (String) vehicleCatalogModel.getValueAt(row, 2);
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
            String status = (String) vehicleCatalogModel.getValueAt(row, 2);
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

        JButton btnRefresh = new JButton("↻ Actualizar");
        btnRefresh.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnRefresh.addActionListener(e -> loadUsersDataAsync());

        header.add(title, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Formulario de creación arriba
        JPanel formCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formCard.setBackground(SURFACE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JTextField txtNewUsername = new JTextField(12);
        txtNewUsername.putClientProperty("JTextField.placeholderText", "Nombre de usuario");
        JPasswordField txtNewPassword = new JPasswordField(10);
        txtNewPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        JComboBox<Role> cmbRole = new JComboBox<Role>(new Role[]{Role.ENCARGADO, Role.ADMIN});
        JButton btnCreateUser = createPrimaryButton("＋ Crear Cuenta", 32);

        formCard.add(new JLabel("Usuario:"));
        formCard.add(txtNewUsername);
        formCard.add(new JLabel("Contraseña:"));
        formCard.add(txtNewPassword);
        formCard.add(new JLabel("Rol:"));
        formCard.add(cmbRole);
        formCard.add(btnCreateUser);

        btnCreateUser.addActionListener(e -> {
            String user = txtNewUsername.getText().trim();
            String pass = new String(txtNewPassword.getPassword()).trim();
            Role role = (Role) cmbRole.getSelectedItem();
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingresa nombre de usuario y contraseña.", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                authController.createUser(user, pass, role, null);
                txtNewUsername.setText("");
                txtNewPassword.setText("");
                loadUsersDataAsync();
                JOptionPane.showMessageDialog(this, "Cuenta de usuario creada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al crear cuenta: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Tabla de usuarios
        String[] cols = {"ID", "USUARIO", "ROL", "ESTADO"};
        usersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(usersTableModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(formCard, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);

        JButton btnDeactivateUser = new JButton("Desactivar Cuenta Seleccionada");
        btnDeactivateUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnDeactivateUser.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long uId = (Long) usersTableModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "¿Deseas desactivar el acceso para este usuario?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                authController.deleteUser(uId);
                loadUsersDataAsync();
            }
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        bottomBar.setOpaque(false);
        bottomBar.add(btnDeactivateUser);
        centerPanel.add(bottomBar, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
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
                        vehicleCatalogModel.addRow(new Object[]{v.getId(), v.getName(), v.isActive() ? "Activo" : "Inactivo"});
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
                    if (usersTableModel != null) {
                        usersTableModel.setRowCount(0);
                        for (AppUser u : users) {
                            usersTableModel.addRow(new Object[]{
                                    u.getId(),
                                    u.getUsername(),
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
                                "—",
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
        new SwingWorker<List<WashTableRowDTO>, Void>() {
            @Override
            protected List<WashTableRowDTO> doInBackground() {
                return washController.getAllWashesTable();
            }
            @Override
            protected void done() {
                try {
                    List<WashTableRowDTO> list = get();
                    if (allWashesModel == null) return;
                    allWashesModel.setRowCount(0);
                    for (WashTableRowDTO r : list) {
                        allWashesModel.addRow(new Object[]{
                                r.getWashDate(),
                                r.getWashTime(),
                                r.getPlate(),
                                r.getVehicleTypeLabel(),
                                r.getVehicleModel(),
                                r.getWorkerName(),
                                CURRENCY_FMT.format(r.getPrice()),
                                CURRENCY_FMT.format(r.getWorkerPayment()),
                                "+" + CURRENCY_FMT.format(r.getProfit()),
                                r.getPayoutStatus()
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadPayoutsDataAsync() {
        new SwingWorker<List<WorkerPayout>, Void>() {
            @Override
            protected List<WorkerPayout> doInBackground() {
                return payoutController.getAllPayouts();
            }
            @Override
            protected void done() {
                try {
                    List<WorkerPayout> list = get();
                    if (payoutsTableModel == null) return;
                    payoutsTableModel.setRowCount(0);
                    for (WorkerPayout p : list) {
                        Worker w = container.getWorkerController().getActiveWorkers().stream()
                                .filter(wk -> wk.getId().equals(p.getWorkerId()))
                                .findFirst().orElse(null);
                        String workerName = (w != null) ? w.getName() : "ID #" + p.getWorkerId();
                        payoutsTableModel.addRow(new Object[]{
                                "#" + p.getId(),
                                p.getPaymentDate(),
                                p.getPaymentTime(),
                                workerName,
                                CURRENCY_FMT.format(p.getAmount()),
                                p.getPaymentMethod().getLabel(),
                                p.getStatus().getLabel(),
                                p.getNotes() != null ? p.getNotes() : "—"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadReportsDataAsync() {
        new SwingWorker<List<FinancialReportDTO>, Void>() {
            @Override
            protected List<FinancialReportDTO> doInBackground() {
                return reportController.getLast30DaysDailyReport();
            }
            @Override
            protected void done() {
                try {
                    List<FinancialReportDTO> list = get();
                    if (dailyReportsModel == null) return;
                    dailyReportsModel.setRowCount(0);
                    for (FinancialReportDTO r : list) {
                        dailyReportsModel.addRow(new Object[]{
                                r.getPeriodLabel(),
                                r.getTotalWashes(),
                                r.getCarsCount(),
                                r.getMotosCount(),
                                CURRENCY_FMT.format(r.getGrossIncome()),
                                CURRENCY_FMT.format(r.getTotalCommissions()),
                                CURRENCY_FMT.format(r.getCashPayoutsDisbursed()),
                                CURRENCY_FMT.format(r.getNetProfit())
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
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
