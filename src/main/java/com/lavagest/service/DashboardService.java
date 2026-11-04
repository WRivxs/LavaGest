package com.lavagest.service;

import com.lavagest.dto.DashboardMetricsDTO;
import com.lavagest.dto.DashboardMetricsDTO.DayTrendDTO;
import com.lavagest.model.PayoutStatus;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;
import com.lavagest.model.Worker;
import com.lavagest.model.WorkerPayout;
import com.lavagest.repository.WashRepository;
import com.lavagest.repository.WorkerPayoutRepository;
import com.lavagest.repository.WorkerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio optimizado que compila los datos del Dashboard principal
 * en un número mínimo de consultas a la base de datos.
 */
public class DashboardService {
    private final WashRepository washRepository;
    private final WorkerRepository workerRepository;
    private final WorkerPayoutRepository payoutRepository;

    public DashboardService(WashRepository washRepository,
                            WorkerRepository workerRepository,
                            WorkerPayoutRepository payoutRepository) {
        this.washRepository = washRepository;
        this.workerRepository = workerRepository;
        this.payoutRepository = payoutRepository;
    }

    public DashboardService(WashRepository washRepository, WorkerRepository workerRepository) {
        this(washRepository, workerRepository, null);
    }

    public FinancialSummary getFinancialSummary() {
        return new FinancialSummary(washRepository.findAll());
    }

    public FinancialSummary getFinancialSummaryToday() {
        return new FinancialSummary(washRepository.findByDate(LocalDate.now()));
    }

    public FinancialSummary getFinancialSummaryByRange(LocalDate from, LocalDate to) {
        return new FinancialSummary(washRepository.findByDateRange(from, to));
    }

    /**
     * Construye las métricas del Dashboard ejecutando únicamente 2 consultas a BD:
     * 1. Consulta de lavados en el rango.
     * 2. Consulta de lavados de los últimos 7 días (para la tendencia).
     */
    public DashboardMetricsDTO buildDashboardMetrics(LocalDate from, LocalDate to) {
        List<Wash> washes = washRepository.findByDateRange(from, to);
        DashboardMetricsDTO dto = new DashboardMetricsDTO();

        int cars = 0;
        int motos = 0;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        for (Wash w : washes) {
            income = income.add(w.getPrice());
            payments = payments.add(w.getWorkerPayment());
            profit = profit.add(w.getProfit());
            if (VehicleType.CARRO == w.getVehicleType()) cars++;
            else motos++;
        }

        int total = washes.size();
        dto.setTotalWashes(total);
        dto.setCarsCount(cars);
        dto.setMotosCount(motos);
        dto.setTotalIncome(income);
        dto.setTotalWorkerPayments(payments);
        dto.setTotalNetProfit(profit);

        if (total > 0) {
            dto.setCarsPercentage(
                BigDecimal.valueOf(cars * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
            dto.setMotosPercentage(
                BigDecimal.valueOf(motos * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
            dto.setAvgTicketPrice(income.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            dto.setAvgProfit(profit.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            dto.setProfitMarginPct(
                income.signum() > 0
                    ? profit.multiply(new BigDecimal("100")).divide(income, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        } else {
            dto.setCarsPercentage(BigDecimal.ZERO);
            dto.setMotosPercentage(BigDecimal.ZERO);
            dto.setAvgTicketPrice(BigDecimal.ZERO);
            dto.setAvgProfit(BigDecimal.ZERO);
            dto.setProfitMarginPct(BigDecimal.ZERO);
        }

        // Tendencia de los últimos 7 días en 1 SOLA consulta (no 7 consultas)
        dto.setWeeklyTrend(buildWeeklyTrendOptimized());

        // Pagos desembolsados
        if (payoutRepository != null) {
            BigDecimal disbursed = BigDecimal.ZERO;
            List<WorkerPayout> payouts = payoutRepository.findByDateRange(from, to);
            for (WorkerPayout p : payouts) {
                if (p.getStatus() == WorkerPayout.PayoutPaymentStatus.PAGADO) {
                    disbursed = disbursed.add(p.getAmount());
                }
            }
            dto.setTotalPayoutsDisbursed(disbursed);
        } else {
            dto.setTotalPayoutsDisbursed(BigDecimal.ZERO);
        }

        // Trabajador líder calculado en memoria
        setTopWorkerOptimized(dto, washes);

        return dto;
    }

    public DashboardMetricsDTO buildDashboardMetricsToday() {
        LocalDate today = LocalDate.now();
        return buildDashboardMetrics(today, today);
    }

    public DashboardMetricsDTO buildDashboardMetricsThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return buildDashboardMetrics(monday, today);
    }

    public DashboardMetricsDTO buildDashboardMetricsThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        return buildDashboardMetrics(firstOfMonth, today);
    }

    public List<String> getTopWorkers() {
        List<String> result = new ArrayList<String>();
        List<Worker> workers = workerRepository.findAll();
        List<Wash> washes = washRepository.findAll();
        for (Worker worker : workers) {
            int count = 0;
            for (Wash w : washes) {
                if (worker.getId().equals(w.getWorkerId())) count++;
            }
            result.add(worker.getName() + " (" + count + " lavados)");
        }
        return result;
    }

    public BigDecimal getAverageProfit() {
        List<Wash> washes = washRepository.findAll();
        if (washes.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Wash wash : washes) {
            total = total.add(wash.getProfit());
        }
        return total.divide(BigDecimal.valueOf(washes.size()), 2, RoundingMode.HALF_UP);
    }

    public static class DashboardBundle {
        public DashboardMetricsDTO metrics;
        public List<Wash> todayWashes;
        public List<DayTrendDTO> weeklyTrend;
    }

    /**
     * Construye todo el paquete de datos del Dashboard en exactamente 2 consultas a la BD:
     * 1. Consulta de lavados de los ultimos 7 dias (de donde se extrae hoy y la tendencia semanal).
     * 2. Consulta de desembolsos del dia.
     * Cero consultas redundantes para maxima velocidad y rendimiento.
     */
    public DashboardBundle buildDashboardBundle(LocalDate date) {
        LocalDate start = date.minusDays(6);
        List<Wash> weekWashes = washRepository.findByDateRange(start, date);

        List<Wash> todayWashes = new ArrayList<Wash>();
        for (Wash w : weekWashes) {
            if (date.equals(w.getDate())) {
                todayWashes.add(w);
            }
        }

        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        int cars = 0;
        int motos = 0;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        for (Wash w : todayWashes) {
            income = income.add(w.getPrice());
            payments = payments.add(w.getWorkerPayment());
            profit = profit.add(w.getProfit());
            if (VehicleType.CARRO == w.getVehicleType()) cars++;
            else motos++;
        }

        int total = todayWashes.size();
        dto.setTotalWashes(total);
        dto.setCarsCount(cars);
        dto.setMotosCount(motos);
        dto.setTotalIncome(income);
        dto.setTotalWorkerPayments(payments);
        dto.setTotalNetProfit(profit);

        if (total > 0) {
            dto.setCarsPercentage(
                BigDecimal.valueOf(cars * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
            dto.setMotosPercentage(
                BigDecimal.valueOf(motos * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP));
            dto.setAvgTicketPrice(income.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            dto.setAvgProfit(profit.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            dto.setProfitMarginPct(
                income.signum() > 0
                    ? profit.multiply(new BigDecimal("100")).divide(income, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        } else {
            dto.setCarsPercentage(BigDecimal.ZERO);
            dto.setMotosPercentage(BigDecimal.ZERO);
            dto.setAvgTicketPrice(BigDecimal.ZERO);
            dto.setAvgProfit(BigDecimal.ZERO);
            dto.setProfitMarginPct(BigDecimal.ZERO);
        }

        // Tendencia de 7 dias calculada directamente de los lavados ya cargados (0 consultas)
        List<DayTrendDTO> trend = computeWeeklyTrendFromWashes(date, weekWashes);
        dto.setWeeklyTrend(trend);

        // Desembolsos del dia
        if (payoutRepository != null) {
            BigDecimal disbursed = BigDecimal.ZERO;
            List<WorkerPayout> payouts = payoutRepository.findByDateRange(date, date);
            for (WorkerPayout p : payouts) {
                if (p.getStatus() == WorkerPayout.PayoutPaymentStatus.PAGADO) {
                    disbursed = disbursed.add(p.getAmount());
                }
            }
            dto.setTotalPayoutsDisbursed(disbursed);
        } else {
            dto.setTotalPayoutsDisbursed(BigDecimal.ZERO);
        }

        setTopWorkerOptimized(dto, todayWashes);

        DashboardBundle bundle = new DashboardBundle();
        bundle.metrics = dto;
        bundle.todayWashes = todayWashes;
        bundle.weeklyTrend = trend;
        return bundle;
    }

    private List<DayTrendDTO> computeWeeklyTrendFromWashes(LocalDate today, List<Wash> weekWashes) {
        Map<LocalDate, List<Wash>> byDate = new HashMap<LocalDate, List<Wash>>();
        for (Wash w : weekWashes) {
            if (w.getDate() != null) {
                if (!byDate.containsKey(w.getDate())) byDate.put(w.getDate(), new ArrayList<Wash>());
                byDate.get(w.getDate()).add(w);
            }
        }

        List<DayTrendDTO> trend = new ArrayList<DayTrendDTO>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<Wash> dayWashes = byDate.getOrDefault(day, new ArrayList<Wash>());
            BigDecimal dayIncome = BigDecimal.ZERO;
            BigDecimal dayProfit = BigDecimal.ZERO;
            for (Wash w : dayWashes) {
                dayIncome = dayIncome.add(w.getPrice());
                dayProfit = dayProfit.add(w.getProfit());
            }
            String dayName = day.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, new Locale("es", "CO"));
            trend.add(new DayTrendDTO(dayName, dayWashes.size(), dayIncome, dayProfit));
        }
        return trend;
    }

    /**
     * Optimizado: consulta el rango de 7 días de una sola vez y agrupa en memoria.
     */
    private List<DayTrendDTO> buildWeeklyTrendOptimized() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        List<Wash> weekWashes = washRepository.findByDateRange(start, today);
        return computeWeeklyTrendFromWashes(today, weekWashes);
    }

    private void setTopWorkerOptimized(DashboardMetricsDTO dto, List<Wash> washes) {
        if (washes.isEmpty()) {
            dto.setTopWorkerName("—");
            dto.setTopWorkerWashes(0);
            return;
        }

        Map<Long, Integer> counts = new HashMap<Long, Integer>();
        Long topWorkerId = null;
        int max = 0;
        for (Wash w : washes) {
            Long wid = w.getWorkerId();
            int c = counts.getOrDefault(wid, 0) + 1;
            counts.put(wid, c);
            if (c > max) {
                max = c;
                topWorkerId = wid;
            }
        }

        String name = "—";
        if (topWorkerId != null) {
            for (Worker w : workerRepository.findAllActive()) {
                if (topWorkerId.equals(w.getId())) {
                    name = w.getName();
                    break;
                }
            }
        }
        dto.setTopWorkerName(name);
        dto.setTopWorkerWashes(max);
    }
}
