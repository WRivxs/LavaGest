package com.lavagest.config;

import com.lavagest.controller.DashboardController;
import com.lavagest.controller.PayoutController;
import com.lavagest.controller.ReportController;
import com.lavagest.controller.WashController;
import com.lavagest.controller.WorkerController;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Worker;
import com.lavagest.repository.InMemoryServiceRateRepository;
import com.lavagest.repository.InMemoryWashRepository;
import com.lavagest.repository.InMemoryWorkerPayoutRepository;
import com.lavagest.repository.InMemoryWorkerRepository;
import com.lavagest.repository.JdbcServiceRateRepository;
import com.lavagest.repository.JdbcWashRepository;
import com.lavagest.repository.JdbcWorkerPayoutRepository;
import com.lavagest.repository.JdbcWorkerRepository;
import com.lavagest.repository.ServiceRateRepository;
import com.lavagest.repository.WashRepository;
import com.lavagest.repository.WorkerPayoutRepository;
import com.lavagest.repository.WorkerRepository;
import com.lavagest.service.DashboardService;
import com.lavagest.service.LavagestService;
import com.lavagest.service.PayoutService;
import com.lavagest.service.ReportService;
import com.lavagest.service.WorkerService;

import java.math.BigDecimal;

/**
 * Contenedor de Inyección de Dependencias de LavaGest.
 * Inicializa automáticamente los repositorios JDBC conectados a Supabase si el archivo
 * .env tiene configurada la contraseña; en caso contrario, utiliza los repositorios en memoria
 * para garantizar que la aplicación funcione siempre.
 */
public class AppContainer {
    private final boolean connectedToDatabase;
    private final WorkerRepository workerRepository;
    private final WashRepository washRepository;
    private final WorkerPayoutRepository payoutRepository;
    private final ServiceRateRepository serviceRateRepository;

    private final WorkerService workerService;
    private final LavagestService lavagestService;
    private final PayoutService payoutService;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    private final WorkerController workerController;
    private final WashController washController;
    private final PayoutController payoutController;
    private final DashboardController dashboardController;
    private final ReportController reportController;

    private static AppContainer instance;

    public AppContainer() {
        DatabaseConfig db = DatabaseConfig.getInstance();
        this.connectedToDatabase = db.isConfigured();

        if (this.connectedToDatabase) {
            this.workerRepository = new JdbcWorkerRepository(db);
            this.washRepository = new JdbcWashRepository(db);
            this.payoutRepository = new JdbcWorkerPayoutRepository(db);
            this.serviceRateRepository = new JdbcServiceRateRepository(db);
            db.warmupPool(2);
        } else {
            InMemoryWorkerRepository inMemWorker = new InMemoryWorkerRepository();
            InMemoryWashRepository inMemWash = new InMemoryWashRepository();
            InMemoryWorkerPayoutRepository inMemPayout = new InMemoryWorkerPayoutRepository();
            this.serviceRateRepository = new InMemoryServiceRateRepository();

            this.workerRepository = inMemWorker;
            this.washRepository = inMemWash;
            this.payoutRepository = inMemPayout;

            // Sembrar datos de prueba iniciales en memoria
            seedDemoData(inMemWorker, inMemWash);
        }

        // Servicios
        this.workerService = new WorkerService(this.workerRepository);
        this.lavagestService = new LavagestService(this.washRepository);
        this.payoutService = new PayoutService(this.payoutRepository, this.washRepository);
        this.dashboardService = new DashboardService(this.washRepository, this.workerRepository, this.payoutRepository);
        this.reportService = new ReportService(this.washRepository, this.payoutRepository);

        // Controladores
        this.workerController = new WorkerController(this.workerService, this.lavagestService);
        this.washController = new WashController(this.lavagestService, this.workerService);
        this.payoutController = new PayoutController(this.payoutService, this.workerService);
        this.dashboardController = new DashboardController(this.dashboardService);
        this.reportController = new ReportController(this.reportService);
    }

    public static synchronized AppContainer getInstance() {
        if (instance == null) {
            instance = new AppContainer();
        }
        return instance;
    }

    private void seedDemoData(WorkerRepository wr, WashRepository washR) {
        Worker c = wr.save(new Worker(null, "Carlos", "3001234567", "Bahía 1", "Turno Completo", true));
        Worker a = wr.save(new Worker(null, "Andrés", "3019876543", "Bahía 2", "Turno Completo", true));
        Worker m = wr.save(new Worker(null, "Miguel", "3024567890", "Bahía 3", "Medio Turno", true));

        LavagestService ls = new LavagestService(washR);
        ls.registerWash("ABC-492", "Mazda 3 Blanco", VehicleType.CARRO, c, new BigDecimal("30000"), new BigDecimal("40.00"), null);
        ls.registerWash("MNO-12E", "Yamaha MT-07", VehicleType.MOTO, a, new BigDecimal("20000"), new BigDecimal("40.00"), null);
        ls.registerWash("XYZ-789", "Toyota Fortuner", VehicleType.CARRO, m, new BigDecimal("30000"), new BigDecimal("40.00"), null);
        ls.registerWash("BKS-310", "Chevrolet Tracker", VehicleType.CARRO, c, new BigDecimal("30000"), new BigDecimal("40.00"), null);
        ls.registerWash("QWE-44D", "KTM Duke 390", VehicleType.MOTO, a, new BigDecimal("20000"), new BigDecimal("40.00"), null);
    }

    // Getters
    public boolean isConnectedToDatabase() { return connectedToDatabase; }
    public WorkerController getWorkerController() { return workerController; }
    public WashController getWashController() { return washController; }
    public PayoutController getPayoutController() { return payoutController; }
    public DashboardController getDashboardController() { return dashboardController; }
    public ReportController getReportController() { return reportController; }
    public ServiceRateRepository getServiceRateRepository() { return serviceRateRepository; }
}
