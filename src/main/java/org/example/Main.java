package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.Employee;
import org.example.model.PensionPlan;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        List<Employee> employees = loadSampleData();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Feature 1: all employees JSON sorted by yearlySalary desc, lastName asc
        List<Employee> allSorted = employees.stream()
                .sorted(Comparator.comparingDouble(Employee::getYearlySalary).reversed()
                        .thenComparing(Employee::getLastName))
                .collect(Collectors.toList());

        System.out.println("=== All Employees (JSON) ===");
        System.out.println(mapper.writeValueAsString(allSorted));

        // Feature 2: Quarterly Upcoming Enrollees JSON
        LocalDate now = LocalDate.now();
        LocalDate[] nextQuarterRange = computeNextQuarterRange(now);
        LocalDate nextQuarterStart = nextQuarterRange[0];
        LocalDate nextQuarterEnd = nextQuarterRange[1];

        List<Employee> upcoming = findQuarterlyUpcomingEnrollees(employees, now);

        System.out.println("=== Quarterly Upcoming Enrollees (from " + nextQuarterStart + " to " + nextQuarterEnd + ") ===");
        System.out.println(mapper.writeValueAsString(upcoming));
    }

    private static LocalDate[] computeNextQuarterRange(LocalDate reference) {
        int month = reference.getMonthValue();
        int year = reference.getYear();

        int nextQuarterIndex = ((month - 1) / 3 + 1) % 4; // 0..3
        int startMonth = nextQuarterIndex * 3 + 1;
        int startYear = startMonth <= month ? year + 1 : year;
        LocalDate start = LocalDate.of(startYear, startMonth, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);
        return new LocalDate[]{start, end};
    }

    private static List<Employee> findQuarterlyUpcomingEnrollees(List<Employee> employees, LocalDate reference) {
        LocalDate[] range = computeNextQuarterRange(reference);
        LocalDate start = range[0];
        LocalDate end = range[1];

        return employees.stream()
                .filter(e -> {
                    // skip if already has a pension plan
                    if (e.getPensionPlan() != null) return false;

                    LocalDate threeYr = e.getEmploymentDate().plusYears(3);
                    // already completed >= 3 years as of reference
                    if (!threeYr.isAfter(reference)) return true;

                    // otherwise include if employmentDate itself falls within next quarter
                    LocalDate emp = e.getEmploymentDate();
                    return !emp.isBefore(start) && !emp.isAfter(end);
                })
                .sorted(Comparator.comparing(Employee::getEmploymentDate).reversed())
                .collect(Collectors.toList());
    }

    private static List<Employee> loadSampleData() {
        List<Employee> list = new ArrayList<>();

        // 1 EX1089 Daniel Agar 105,945.50 2023-01-17 null $100.00
        PensionPlan p1 = new PensionPlan("EX1089", null, 100.00);
        list.add(new Employee(1L, "Daniel", "Agar", LocalDate.parse("2023-01-17"), 105945.50, p1));

        // 2 Benard Shaw 197,750.00 2022-09-03 2025-09-03 null
        // No plan reference given in the dataset -> treat as not having active PensionPlan
        list.add(new Employee(2L, "Benard", "Shaw", LocalDate.parse("2022-09-03"), 197750.00, null));

        // 3 SM2307 Carly Agar 842,000.75 2014-05-16 2017-05-17 $1,555.50
        PensionPlan p3 = new PensionPlan("SM2307", LocalDate.parse("2017-05-17"), 1555.50);
        list.add(new Employee(3L, "Carly", "Agar", LocalDate.parse("2014-05-16"), 842000.75, p3));

        // 4 Wesley Schneider 74,500.00 2023-07-21
        list.add(new Employee(4L, "Wesley", "Schneider", LocalDate.parse("2023-07-21"), 74500.00, null));

        // 5 Anna Wiltord 85,750.00 2023-03-15
        list.add(new Employee(5L, "Anna", "Wiltord", LocalDate.parse("2023-03-15"), 85750.00, null));

        // 6 Yosef Tesfalem 100,000.00 2024-10-31
        list.add(new Employee(6L, "Yosef", "Tesfalem", LocalDate.parse("2024-10-31"), 100000.00, null));

        return list;
    }
}