package com.lavagest;

import com.lavagest.controller.DashboardController;
import com.lavagest.controller.PayoutController;
import com.lavagest.controller.ReportController;
import com.lavagest.controller.WashController;
import com.lavagest.controller.WorkerController;
import com.lavagest.dto.CreatePayoutRequest;
import com.lavagest.dto.CreateWashRequest;
import com.lavagest.dto.DashboardMetricsDTO;
import com.lavagest.dto.FinancialReportDTO;
import com.lavagest.dto.WashTableRowDTO;
import com.lavagest.dto.WorkerKpiDTO;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Worker;
import com.lavagest.repository.InMemoryWashRepository;
import com.lavagest.repository.InMemoryWorkerPayoutRepository;
import com.lavagest.repository.InMemoryWorkerRepository;
import com.lavagest.service.DashboardService;
import com.lavagest.service.LavagestService;
import com.lavagest.service.PayoutService;
import com.lavagest.service.ReportService;
import com.lavagest.service.WorkerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Prueba integral de la arquitectura de LavaGest PRO (Fase 2).
 * Valida la interoperabilidad entre Repositorios, Servicios, DTOs y Controladores.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      INICIANDO VALIDACION DE ARQUITECTURA        ");
        System.out.println("               LAVAGEST PRO                       ");
        System.out.println("==================================================\n");

        // 1. Instanciación de Repositorios
        InMemoryWorkerRepository workerRepo = new InMemoryWorkerRepository();
        InMemoryWashRepository washRepo = new InMemoryWashRepository();
        InMemoryWorkerPayoutRepository payoutRepo = new InMemoryWorkerPayoutRepository();

        // 2. Instanciación de Servicios
        WorkerService workerService = new WorkerService(workerRepo);
        LavagestService lavagestService = new LavagestService(washRepo);
        PayoutService payoutService = new PayoutService(payoutRepo, washRepo);
        DashboardService dashboardService = new DashboardService(washRepo, workerRepo, payoutRepo);
        ReportService reportService = new ReportService(washRepo, payoutRepo);

        // 3. Instanciación de Controladores (Capa que usará la UI)
        WorkerController workerController = new WorkerController(workerService, lavagestService);
        WashController washController = new WashController(lavagestService, workerService);
        PayoutController payoutController = new PayoutController(payoutService, workerService);
        DashboardController dashboardController = new DashboardController(dashboardService);
        ReportController reportController = new ReportController(reportService);

        // 4. Registrar Trabajadores usando WorkerController
        System.out.println("1. Registrando operadores...");
        workerController.createWorker("Carlos", "3001234567", "Bahía 1", "Turno Completo");
        workerController.createWorker("Andrés", "3019876543", "Bahía 2", "Turno Completo");
        workerController.createWorker("Miguel", "3024567890", "Bahía 3", "Medio Turno");
        List<Worker> activeWorkers = workerController.getActiveWorkers();
        System.out.println("   -> Operadores activos: " + activeWorkers.size());

        Worker carlos = activeWorkers.get(0);
        Worker andres = activeWorkers.get(1);
        Worker miguel = activeWorkers.get(2);

        // 5. Registrar Lavados usando WashController + CreateWashRequest DTO
        System.out.println("\n2. Registrando servicios de lavado (Modo Dispatcher)...");

        // Ticket 1: Carro Carlos
        washController.registerWash(new CreateWashRequest(
                "ABC-492", "Mazda 3 Blanco", VehicleType.CARRO, carlos.getId(), new BigDecimal("30000")));

        // Ticket 2: Moto Andrés
        washController.registerWash(new CreateWashRequest(
                "MNO-12E", "Yamaha MT-07", VehicleType.MOTO, andres.getId(), new BigDecimal("20000")));

        // Ticket 3: Carro Miguel
        washController.registerWash(new CreateWashRequest(
                "XYZ-789", "Toyota Fortuner", VehicleType.CARRO, miguel.getId(), new BigDecimal("30000")));

        // Ticket 4: Carro Carlos
        washController.registerWash(new CreateWashRequest(
                "BKS-310", "Chevrolet Tracker", VehicleType.CARRO, carlos.getId(), new BigDecimal("30000")));

        // Ticket 5: Moto Andrés
        washController.registerWash(new CreateWashRequest(
                "QWE-44D", "KTM Duke 390", VehicleType.MOTO, andres.getId(), new BigDecimal("20000")));

        // 6. Consultar tabla de lavados mediante WashController
        List<WashTableRowDTO> tableRows = washController.getWashesTableToday();
        System.out.println("   -> Total lavados registrados hoy: " + tableRows.size());
        for (WashTableRowDTO row : tableRows) {
            System.out.printf("      [%s] %s | %s | %s | Cobrado: $%s | Pago: $%s | Ganancia: +$%s (%s%%)\n",
                    row.getWashTime(), row.getPlate(), row.getVehicleModel(), row.getWorkerName(),
                    row.getPrice(), row.getWorkerPayment(), row.getProfit(), row.getProfitMarginPct());
        }

        // 7. Consultar KPIs del Dashboard mediante DashboardController
        System.out.println("\n3. Obteniendo metricas del Dashboard (Periodo: HOY)...");
        DashboardMetricsDTO metrics = dashboardController.getMetrics(DashboardController.Period.HOY);
        System.out.println("   -> Total Lavados Hoy:  " + metrics.getTotalWashes());
        System.out.println("   -> Proporcion:         " + metrics.getCarsCount() + " Carros (" + metrics.getCarsPercentage() + "%) | "
                + metrics.getMotosCount() + " Motos (" + metrics.getMotosPercentage() + "%)");
        System.out.println("   -> Ingresos Totales:   $" + metrics.getTotalIncome());
        System.out.println("   -> Comisiones Lavador: $" + metrics.getTotalWorkerPayments());
        System.out.println("   -> Ganancia Negocio:   $" + metrics.getTotalNetProfit() + " (" + metrics.getProfitMarginPct() + "% margen libre)");
        System.out.println("   -> Ticket Promedio:    $" + metrics.getAvgTicketPrice());
        System.out.println("   -> Operador Lider:     " + metrics.getTopWorkerName() + " (" + metrics.getTopWorkerWashes() + " lavados)");

        // 8. Consultar Rendimiento y Nómina de Trabajadores mediante WorkerController
        System.out.println("\n4. Consultando rendimiento y nomina pendiente...");
        List<WorkerKpiDTO> workerKpis = workerController.getWorkersKpiToday();
        for (WorkerKpiDTO kpi : workerKpis) {
            System.out.printf("   -> %s (%s): %d lavados | Genero: $%s | Comision: $%s | Pendiente por pagar: $%s\n",
                    kpi.getWorkerName(), kpi.getBay(), kpi.getTotalWashes(),
                    kpi.getTotalGenerated(), kpi.getTotalCommission(), kpi.getPendingToPay());
        }

        // 9. Registrar Liquidación a un trabajador mediante PayoutController
        System.out.println("\n5. Liquidando turno a Carlos...");
        BigDecimal pendienteCarlos = payoutController.getPendingAmount(carlos.getId());
        System.out.println("   -> Saldo pendiente Carlos: $" + pendienteCarlos);

        payoutController.registerPayout(new CreatePayoutRequest(
                carlos.getId(), pendienteCarlos, "Efectivo", "Liquidacion turno tarde 2 lavados"));
        System.out.println("   -> Pago liquidado exitosamente. Nuevo saldo pendiente: $"
                + payoutController.getPendingAmount(carlos.getId()));

        // 10. Consultar Reportes de Ingresos mediante ReportController
        System.out.println("\n6. Consultando Reporte Financiero de Ingresos...");
        List<FinancialReportDTO> dailyReport = reportController.getLast30DaysDailyReport();
        for (FinancialReportDTO rep : dailyReport) {
            System.out.printf("   -> Fecha: %s | Lavados: %d | Ingreso Bruto: $%s | Comisiones: $%s | Desembolsos Caja: $%s | Ganancia Neta: $%s\n",
                    rep.getPeriodLabel(), rep.getTotalWashes(), rep.getGrossIncome(),
                    rep.getTotalCommissions(), rep.getCashPayoutsDisbursed(), rep.getNetProfit());
        }

        System.out.println("\n==================================================");
        System.out.println("   VALIDACION COMPLETADA CON EXITO - FASE 2 OK    ");
        System.out.println("==================================================");
    }
}
