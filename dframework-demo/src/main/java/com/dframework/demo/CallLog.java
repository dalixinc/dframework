package com.dframework.demo;

import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.table.AbstractTableModel;

/** A made-up Tracker 2 style call log, generated from a fixed seed so it is the same every run. */
final class CallLog extends AbstractTableModel {

    @Serial
    private static final long serialVersionUID = 1L;

    /** An enum rather than text, so sorting gives High, Medium, Low instead of alphabetical order. */
    enum Priority {
        HIGH("High"), MEDIUM("Medium"), LOW("Low");

        private final String label;

        Priority(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record Call(int number, LocalDate logged, String customer, String summary,
            Priority priority, String status, String engineer) {
    }

    private static final String[] COLUMNS = {"Call", "Logged", "Customer", "Summary", "Priority", "Status", "Engineer"};
    private static final Class<?>[] TYPES = {Integer.class, LocalDate.class, String.class, String.class,
            Priority.class, String.class, String.class};

    private static final String[] CUSTOMERS = {"Acme Ltd", "Bolt & Co", "Cobalt plc", "Dunmore Estates",
            "Ellis Freight", "Fenwick Dental", "Greystone Legal", "Harbour Cafe", "Ionic Labs", "Juniper Schools"};
    private static final String[] PROBLEMS = {"Printer fault", "Email not syncing", "Password reset",
            "New starter laptop", "VPN dropping", "Monitor flickering", "Backup failed", "Slow network",
            "Software licence", "Phone system down", "Printer jam", "Shared drive missing", "Virus warning",
            "Wi-Fi in meeting room", "Spreadsheet macro error"};
    private static final String[] WHERE = {"reception", "floor 2", "accounts", "warehouse", "the MD's office",
            "meeting room B", "home worker", "branch office"};
    private static final Priority[] PRIORITIES = {Priority.HIGH, Priority.MEDIUM, Priority.MEDIUM, Priority.LOW, Priority.LOW};
    private static final String[] STATUSES = {"Open", "Open", "On hold", "Closed", "Closed", "Closed"};
    private static final String[] ENGINEERS = {"Dale", "Priya", "Tom", "Megan", "Sam"};

    private final transient List<Call> calls = new ArrayList<>();

    CallLog(int count) {
        Random random = new Random(2005);   // Tracker 2's vintage
        LocalDate day = LocalDate.of(2026, 10, 10);
        for (int i = 0; i < count; i++) {
            calls.add(new Call(
                    1000 + count - i,
                    day.minusDays(i / 4),
                    pick(random, CUSTOMERS),
                    pick(random, PROBLEMS) + ", " + pick(random, WHERE),
                    PRIORITIES[random.nextInt(PRIORITIES.length)],
                    i < 20 ? "Open" : pick(random, STATUSES),
                    pick(random, ENGINEERS)));
        }
    }

    private static String pick(Random random, String[] options) {
        return options[random.nextInt(options.length)];
    }

    @Override
    public int getRowCount() {
        return calls.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Call call = calls.get(row);
        return switch (column) {
            case 0 -> call.number();
            case 1 -> call.logged();
            case 2 -> call.customer();
            case 3 -> call.summary();
            case 4 -> call.priority();
            case 5 -> call.status();
            default -> call.engineer();
        };
    }
}
