package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

/**
 * Shared console input helpers with validation and retry.
 */
public final class ConsoleHelper {

    private final Scanner scanner;

    public ConsoleHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public void pause() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("Value cannot be empty. Please try again.");
        }
    }

    public String readOptional(String prompt) {
        String value = readLine(prompt);
        return value.isBlank() ? null : value;
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt);
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    System.out.printf("Please enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    public int readPositiveInt(String prompt) {
        while (true) {
            String raw = readLine(prompt);
            try {
                int value = Integer.parseInt(raw);
                if (value <= 0) {
                    System.out.println("Please enter a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    public BigDecimal readGradeValue(String prompt) {
        while (true) {
            String raw = readRequired(prompt + " (0.0 to 5.0): ");
            try {
                BigDecimal value = new BigDecimal(raw);
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("5.0")) > 0) {
                    System.out.println("Grade must be between 0.0 and 5.0.");
                    continue;
                }
                return value.setScale(1, java.math.RoundingMode.HALF_UP);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid decimal. Example: 4.5");
            }
        }
    }

    public BigDecimal readMoney(String prompt) {
        while (true) {
            String raw = readRequired(prompt + " (example 350000.00): ");
            try {
                BigDecimal value = new BigDecimal(raw);
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Amount must be greater than zero.");
                    continue;
                }
                return value.setScale(2, java.math.RoundingMode.HALF_UP);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid amount. Example: 350000.00");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String raw = readRequired(prompt + " (yyyy-MM-dd): ");
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException exception) {
                System.out.println("Invalid date format. Example: 2015-03-25");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
        while (true) {
            String raw = readRequired(prompt + " (HH:mm): ");
            try {
                return LocalTime.parse(raw, format);
            } catch (DateTimeParseException exception) {
                System.out.println("Invalid time. Example: 08:30");
            }
        }
    }

    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String raw = readLine(prompt + " (yyyy-MM-dd, blank to skip): ");
            if (raw.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException exception) {
                System.out.println("Invalid date format. Example: 2015-03-25");
            }
        }
    }

    public String readChoice(String prompt, List<String> options) {
        while (true) {
            System.out.println(prompt);
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, options.get(i));
            }
            int selected = readIntInRange("Choose: ", 1, options.size());
            return options.get(selected - 1);
        }
    }

    public String readMatching(String prompt, Predicate<String> validator, String errorMessage) {
        while (true) {
            String value = readRequired(prompt);
            if (validator.test(value)) {
                return value;
            }
            System.out.println(errorMessage);
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String value = readLine(prompt + " (y/n): ").toLowerCase();
            if (value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    public void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    public void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }
}
