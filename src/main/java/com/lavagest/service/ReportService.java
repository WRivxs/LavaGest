package com.lavagest.service;

import com.lavagest.dto.FinancialReportDTO;
import com.lavagest.model.VehicleType;
import com.lavagest.model.Wash;
import com.lavagest.model.WorkerPayout;
import com.lavagest.repository.WashRepository;
import com.lavagest.repository.WorkerPayoutRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de reportes financieros.
 * Genera informes consolidados de ingresos, egresos y utilidad
 * para cualquier rango de fechas, agrupados por dia o por mes.
 */
public class ReportService {
    private static final DateTimeFormatter LABEL_DATE = DateTimeFormatter.ofPattern("dd 'de' MMMM yyyy",
            new java.util.Locale("es", "CO"));
    private static final DateTimeFormatter KEY_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter KEY_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter LABEL_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy",
            new java.util.Locale("es", "CO"));

    private final WashRepository washRepository;
    private final WorkerPayoutRepository payoutRepository;

    public ReportService(WashRepository washRepository, WorkerPayoutRepository payoutRepository) {
        this.washRepository = washRepository;
        this.payoutRepository = payoutRepository;
    }

    /**
     * Reporte diario de ingresos: cada fila representa un día dentro del rango.
     * Incluye desglose por tipo de vehículo y egreso real de caja.
     */
    public List<FinancialReportDTO> getDailyReport(LocalDate from, LocalDate to) {
        List<Wash> washes = washRepository.findByDateRange(from, to);
        List<WorkerPayout> payouts = payoutRepository != null
                ? payoutRepository.findByDateRange(from, to) : new ArrayList<WorkerPayout>();

        // Agrupar lavados por dia
        Map<String, List<Wash>> byDay = groupWashesByDay(washes);

        // Agrupar pagos desembolsados por dia
        Map<String, BigDecimal> payoutsByDay = groupPayoutsByDay(payouts);

        List<FinancialReportDTO> report = new ArrayList<FinancialReportDTO>();
        for (Map.Entry<String, List<Wash>> entry : byDay.entrySet()) {
            String key = entry.getKey();
            List<Wash> dayWashes = entry.getValue();
            LocalDate day = LocalDate.parse(key, KEY_DAY);
            FinancialReportDTO row = buildReportRow(dayWashes,
                    payoutsByDay.getOrDefault(key, BigDecimal.ZERO));
            row.setPeriod(key);
            row.setPeriodLabel(day.format(LABEL_DATE));
            report.add(row);
        }
        return report;
    }

    /**
     * Reporte mensual de ingresos: cada fila representa un mes dentro del rango.
     */
    public List<FinancialReportDTO> getMonthlyReport(LocalDate from, LocalDate to) {
        List<Wash> washes = washRepository.findByDateRange(from, to);
        List<WorkerPayout> payouts = payoutRepository != null
                ? payoutRepository.findByDateRange(from, to) : new ArrayList<WorkerPayout>();

        Map<String, List<Wash>> byMonth = groupWashesByMonth(washes);
        Map<String, BigDecimal> payoutsByMonth = groupPayoutsByMonth(payouts);

        List<FinancialReportDTO> report = new ArrayList<FinancialReportDTO>();
        for (Map.Entry<String, List<Wash>> entry : byMonth.entrySet()) {
            String key = entry.getKey();
            List<Wash> monthWashes = entry.getValue();
            FinancialReportDTO row = buildReportRow(monthWashes,
                    payoutsByMonth.getOrDefault(key, BigDecimal.ZERO));
            // Dias operativos del mes
            row.setOperatingDays(countUniqueDays(monthWashes));
            row.setAvgDailyIncome(
                row.getOperatingDays() > 0
                    ? row.getGrossIncome().divide(BigDecimal.valueOf(row.getOperatingDays()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            row.setPeriod(key);
            // Capitalizar primera letra del mes
            String label = LocalDate.parse(key + "-01").format(LABEL_MONTH);
            row.setPeriodLabel(label.substring(0, 1).toUpperCase() + label.substring(1));
            report.add(row);
        }
        return report;
    }

    /**
     * Resumen financiero compacto para un rango (usado en el filtro Hoy/Semana/Mes).
     */
    public FinancialReportDTO getSummaryReport(LocalDate from, LocalDate to) {
        List<Wash> washes = washRepository.findByDateRange(from, to);
        List<WorkerPayout> payouts = payoutRepository != null
                ? payoutRepository.findByDateRange(from, to) : new ArrayList<WorkerPayout>();
        BigDecimal disbursed = BigDecimal.ZERO;
        for (WorkerPayout p : payouts) {
            if (p.getStatus() == WorkerPayout.PayoutPaymentStatus.PAGADO) {
                disbursed = disbursed.add(p.getAmount());
            }
        }
        FinancialReportDTO row = buildReportRow(washes, disbursed);
        row.setPeriod(from.format(KEY_DAY) + "/" + to.format(KEY_DAY));
        row.setPeriodLabel(from.format(LABEL_DATE) + " - " + to.format(LABEL_DATE));
        row.setOperatingDays(countUniqueDays(washes));
        if (row.getOperatingDays() > 0) {
            row.setAvgDailyIncome(
                row.getGrossIncome().divide(BigDecimal.valueOf(row.getOperatingDays()), 2, RoundingMode.HALF_UP));
        }
        return row;
    }

    // ---- Helpers privados ----

    private FinancialReportDTO buildReportRow(List<Wash> washes, BigDecimal disbursed) {
        FinancialReportDTO row = new FinancialReportDTO();
        int total = washes.size();
        int cars = 0;
        int motos = 0;
        BigDecimal incomeCars = BigDecimal.ZERO;
        BigDecimal incomeMotos = BigDecimal.ZERO;
        BigDecimal grossIncome = BigDecimal.ZERO;
        BigDecimal commissions = BigDecimal.ZERO;
        BigDecimal netProfit = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeByVeh = new LinkedHashMap<>();
        Map<String, Integer> countByVeh = new LinkedHashMap<>();

        for (Wash w : washes) {
            grossIncome = grossIncome.add(w.getPrice());
            commissions = commissions.add(w.getWorkerPayment());
            netProfit = netProfit.add(w.getProfit());

            String vName = w.getVehicleModel() != null && !w.getVehicleModel().trim().isEmpty() 
                    ? w.getVehicleModel().trim() 
                    : (w.getVehicleType() != null ? w.getVehicleType().getLabel() : "Otro");
            incomeByVeh.put(vName, incomeByVeh.getOrDefault(vName, BigDecimal.ZERO).add(w.getPrice()));
            countByVeh.put(vName, countByVeh.getOrDefault(vName, 0) + 1);

            if (VehicleType.CARRO == w.getVehicleType()) {
                cars++;
                incomeCars = incomeCars.add(w.getPrice());
            } else {
                motos++;
                incomeMotos = incomeMotos.add(w.getPrice());
            }
        }

        row.setTotalWashes(total);
        row.setCarsCount(cars);
        row.setMotosCount(motos);
        row.setIncomeCars(incomeCars);
        row.setIncomeMotos(incomeMotos);
        row.setIncomeByVehicle(incomeByVeh);
        row.setCountByVehicle(countByVeh);
        row.setGrossIncome(grossIncome);
        row.setTotalCommissions(commissions);
        row.setNetProfit(netProfit);
        row.setCashPayoutsDisbursed(disbursed);
        row.setAvgTicketPrice(
            total > 0 ? grossIncome.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        row.setProfitMarginPct(
            grossIncome.signum() > 0
                ? netProfit.multiply(new BigDecimal("100")).divide(grossIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return row;
    }

    private Map<String, List<Wash>> groupWashesByDay(List<Wash> washes) {
        Map<String, List<Wash>> map = new LinkedHashMap<String, List<Wash>>();
        for (Wash w : washes) {
            String key = w.getDate().format(KEY_DAY);
            if (!map.containsKey(key)) map.put(key, new ArrayList<Wash>());
            map.get(key).add(w);
        }
        return map;
    }

    private Map<String, List<Wash>> groupWashesByMonth(List<Wash> washes) {
        Map<String, List<Wash>> map = new LinkedHashMap<String, List<Wash>>();
        for (Wash w : washes) {
            String key = w.getDate().format(KEY_MONTH);
            if (!map.containsKey(key)) map.put(key, new ArrayList<Wash>());
            map.get(key).add(w);
        }
        return map;
    }

    private Map<String, BigDecimal> groupPayoutsByDay(List<WorkerPayout> payouts) {
        Map<String, BigDecimal> map = new LinkedHashMap<String, BigDecimal>();
        for (WorkerPayout p : payouts) {
            if (p.getStatus() != WorkerPayout.PayoutPaymentStatus.PAGADO) continue;
            String key = p.getPaymentDate().format(KEY_DAY);
            map.put(key, map.getOrDefault(key, BigDecimal.ZERO).add(p.getAmount()));
        }
        return map;
    }

    private Map<String, BigDecimal> groupPayoutsByMonth(List<WorkerPayout> payouts) {
        Map<String, BigDecimal> map = new LinkedHashMap<String, BigDecimal>();
        for (WorkerPayout p : payouts) {
            if (p.getStatus() != WorkerPayout.PayoutPaymentStatus.PAGADO) continue;
            String key = p.getPaymentDate().format(KEY_MONTH);
            map.put(key, map.getOrDefault(key, BigDecimal.ZERO).add(p.getAmount()));
        }
        return map;
    }

    private int countUniqueDays(List<Wash> washes) {
        List<LocalDate> seen = new ArrayList<LocalDate>();
        for (Wash w : washes) {
            if (!seen.contains(w.getDate())) seen.add(w.getDate());
        }
        return seen.size();
    }
}
