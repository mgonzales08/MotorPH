/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.motorph;

/**
 *
 * @author Marlu
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalTime;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public class MotorPH {

    // employee master list (34 employees in the company)
    static String[] empNum   = new String[34];
    static String[] lname    = new String[34];
    static String[] fname    = new String[34];
    static String[] bday     = new String[34];
    static String[] basePay  = new String[34];
    static String[] hrRate   = new String[34];
    static String[] cols     = new String[19]; // reused buffer for CSV parsing

    // DTR records — 5168 rows total in the loginandout file
    static String[]    dtrEmpNum  = new String[5168];
    static String[]    dtrDate    = new String[5168];
    static String[]    timeIn     = new String[5168];
    static String[]    timeOut    = new String[5168];
    static LocalTime[] parsedIn   = new LocalTime[5168];
    static LocalTime[] parsedOut  = new LocalTime[5168];
    static double[]    dailyHours = new double[5168];

    public static void main(String[] args) throws FileNotFoundException {

        loadEmployees("resources/motorphemployeedata.csv");
        loadDTR("resources/loginandout.csv");
        computeDailyHours();

        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        String user = sc.nextLine();
        System.out.print("Password: ");
        String pass = sc.nextLine();

        // two roles: regular employee (view own info) and payroll staff (process payslips)
        if ("employee".equals(user) && "12345".equals(pass)) {
            runEmployeePortal(sc);
        } else if ("payroll_staff".equals(user) && "12345".equals(pass)) {
            runPayrollPortal(sc);
        } else {
            System.out.println("Incorrect username and/or password. Please try again.");
            sc.close();
        }
    }

    static void runEmployeePortal(Scanner sc) {
        while (true) {
            System.out.println("\n---- Employee Portal ----");
            System.out.println("[1] Check Employee Profile");
            System.out.println("[2] Exit");
            String pick = sc.nextLine().trim();

            if ("1".equals(pick)) {
                System.out.print("Enter your Employee No.: ");
                String num = sc.nextLine().trim();
                boolean hit = false;
                for (int i = 0; i < empNum.length; i++) {
                    if (num.equals(empNum[i])) {
                        System.out.println("\nEmployee No. : " + empNum[i]);
                        System.out.println("Name         : " + fname[i] + " " + lname[i]);
                        System.out.println("Birthday     : " + bday[i]);
                        hit = true;
                        break;
                    }
                }
                if (!hit) System.out.println("Employee number does not exist. Please check the number and try again.");

            } else if ("2".equals(pick)) {
                System.out.println("Exiting payroll system.");
                sc.close();
                break;
            } else {
                System.out.println("Please enter 1 or 2 only.");
            }
        }
    }

    static void runPayrollPortal(Scanner sc) {
        while (true) {
            System.out.println("\n---- Payroll Portal ----");
            System.out.println("[1] Generate Payslip");
            System.out.println("[2] Exit");
            String pick = sc.nextLine().trim();

            if ("1".equals(pick)) {
                runPayslipMenu(sc);
            } else if ("2".equals(pick)) {
                System.out.println("Exiting payroll system.");
                sc.close();
                break;
            } else {
                System.out.println("Please enter 1 or 2 only.");
            }
        }
    }

    static void runPayslipMenu(Scanner sc) {
        while (true) {
            System.out.println("\n---- Generate Payslip ----");
            System.out.println("[1] Single Employee");
            System.out.println("[2] All Employees");
            System.out.println("[3] Back");
            String pick = sc.nextLine().trim();

            if ("1".equals(pick)) {
                processSingleEmployee(sc);
            } else if ("2".equals(pick)) {
                processAllEmployees(sc);
            } else if ("3".equals(pick)) {
                break;
            } else {
                System.out.println("Please enter 1, 2, or 3 only.");
            }
        }
    }

    static void processSingleEmployee(Scanner sc) {
        System.out.print("Employee No.: ");
        String num = sc.nextLine().trim();

        int idx = -1;
        for (int i = 0; i < empNum.length; i++) {
            if (num.equals(empNum[i])) { idx = i; break; }
        }
        if (idx == -1) { System.out.println("Employee number does not exist."); return; }

        int mo = chooseMonth(sc);
        if (mo == -1) return;

        double rate = Double.parseDouble(hrRate[idx]);
        double base = Double.parseDouble(basePay[idx]);

        double hrs1   = getHours(num, "first",  mo);
        double hrs2   = getHours(num, "second", mo);
        double gross1 = hrs1 * rate;
        double gross2 = hrs2 * rate;

        // gov't contributions computed on combined gross for the month
        double combined   = gross1 + gross2;
        double sss        = sssTable(combined);
        double philhealth = philhealthShare(combined);
        double pagibig    = pagibigShare(base); // pagibig uses contracted salary per HDMF rules
        double taxable    = combined - sss - philhealth - pagibig;
        double whTax      = withholdingTax(taxable);
        double totalDed   = sss + philhealth + pagibig + whTax;

        // net salary — late arrivals naturally earn less since hours are based on actual time in
        double net1 = gross1;
        double net2 = gross2 - totalDed;

        String mn   = monthLabel(mo);
        int lastDay = YearMonth.of(2024, mo).lengthOfMonth();
        double cap2 = (lastDay == 31) ? 88.0 : 80.0;

        System.out.println("\n----Employee Details----");
        System.out.println("Employee #   : " + empNum[idx]);
        System.out.println("Employee Name: " + fname[idx] + " " + lname[idx]);
        System.out.println("Birthday     : " + bday[idx]);

        System.out.println("\n========================================");
        System.out.println("  Cutoff: " + mn + " 1 to " + mn + " 15");
        System.out.println("  (8 hrs/day x 5 days/wk x 2 wks = 80 hrs max)");
        System.out.println("========================================");
        System.out.println("Total Hours Worked : " + String.format("%.2f", hrs1) + " / 80.00 hrs");
        System.out.println("Gross Salary       : Php" + String.format("%.2f", gross1));
        System.out.println("Net Salary         : Php" + String.format("%.2f", net1));

        System.out.println("\n========================================");
        System.out.println("  Cutoff: " + mn + " 16 to " + mn + " " + lastDay);
        System.out.println("  (8 hrs/day x 5 days/wk x 2 wks = " + String.format("%.0f", cap2) + " hrs max)");
        System.out.println("========================================");
        System.out.println("Total Hours Worked : " + String.format("%.2f", hrs2) + " / " + String.format("%.2f", cap2) + " hrs");
        System.out.println("Gross Salary       : Php" + String.format("%.2f", gross2));
        System.out.println("\n--- Statutory Deductions ---");
        System.out.println("  SSS             : Php" + String.format("%.2f", sss));
        System.out.println("  PhilHealth      : Php" + String.format("%.2f", philhealth));
        System.out.println("  Pag-IBIG        : Php" + String.format("%.2f", pagibig));
        System.out.println("  Tax             : Php" + String.format("%.2f", whTax));
        System.out.println("  Subtotal        : Php" + String.format("%.2f", totalDed));
        System.out.println("Net Salary         : Php" + String.format("%.2f", net2));
        System.out.println("========================================");
    }

    static void processAllEmployees(Scanner sc) {
        int mo = chooseMonth(sc);
        if (mo == -1) return;

        String mn   = monthLabel(mo);
        int lastDay = YearMonth.of(2024, mo).lengthOfMonth();
        double cap2 = (lastDay == 31) ? 88.0 : 80.0;

        for (int i = 0; i < empNum.length; i++) {
            if (empNum[i] == null) continue;

            String num  = empNum[i];
            double rate = Double.parseDouble(hrRate[i]);
            double base = Double.parseDouble(basePay[i]);

            double hrs1   = getHours(num, "first",  mo);
            double hrs2   = getHours(num, "second", mo);
            double gross1 = hrs1 * rate;
            double gross2 = hrs2 * rate;

            double combined   = gross1 + gross2;
            double sss        = sssTable(combined);
            double philhealth = philhealthShare(combined);
            double pagibig    = pagibigShare(base);
            double taxable    = combined - sss - philhealth - pagibig;
            double whTax      = withholdingTax(taxable);
            double totalDed   = sss + philhealth + pagibig + whTax;

            double net1 = gross1;
            double net2 = gross2 - totalDed;

            System.out.println("\n========================================");
            System.out.println("Employee #   : " + num);
            System.out.println("Name         : " + fname[i] + " " + lname[i]);
            System.out.println("Birthday     : " + bday[i]);
            System.out.println("\n  Cutoff: " + mn + " 1 to " + mn + " 15");
            System.out.println("  Total Hours Worked : " + String.format("%.2f", hrs1) + " / 80.00 hrs");
            System.out.println("  Gross Salary       : Php" + String.format("%.2f", gross1));
            System.out.println("  Net Salary         : Php" + String.format("%.2f", net1));
            System.out.println("\n  Cutoff: " + mn + " 16 to " + mn + " " + lastDay);
            System.out.println("  Total Hours Worked : " + String.format("%.2f", hrs2) + " / " + String.format("%.2f", cap2) + " hrs");
            System.out.println("  Gross Salary       : Php" + String.format("%.2f", gross2));
            System.out.println("  SSS                : Php" + String.format("%.2f", sss));
            System.out.println("  PhilHealth         : Php" + String.format("%.2f", philhealth));
            System.out.println("  Pag-IBIG           : Php" + String.format("%.2f", pagibig));
            System.out.println("  Tax                : Php" + String.format("%.2f", whTax));
            System.out.println("  Total Deductions   : Php" + String.format("%.2f", totalDed));
            System.out.println("  Net Salary         : Php" + String.format("%.2f", net2));
            System.out.println("========================================");
        }
    }

    // loads the employee masterlist CSV
    static void loadEmployees(String path) {
        try {
            Scanner f = new Scanner(new FileReader(path));
            if (f.hasNextLine()) f.nextLine(); // skip header row
            int i = 0;
            while (f.hasNextLine()) {
                String[] c = splitCSV(f.nextLine());
                empNum[i]  = c[0];
                lname[i]   = c[1];
                fname[i]   = c[2];
                bday[i]    = c[3];
                basePay[i] = c[13].replace(",", "");
                hrRate[i]  = c[18].replace(",", "");
                i++;
            }
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not open employee file: " + e.getMessage());
            System.exit(1);
        }
    }

    // loads the daily time record (DTR) file
    static void loadDTR(String path) {
        try {
            Scanner f = new Scanner(new FileReader(path));
            if (f.hasNextLine()) f.nextLine();
            int i = 0;
            while (f.hasNextLine()) {
                String[] c   = splitCSV(f.nextLine());
                dtrEmpNum[i] = c[0];
                dtrDate[i]   = c[3];
                timeIn[i]    = c[4];
                timeOut[i]   = c[5];
                i++;
            }
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not open DTR file: " + e.getMessage());
            System.exit(1);
        }
    }

    // pre-computes hours worked per DTR row so we don't redo this every payslip
    static void computeDailyHours() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);
        LocalTime grace = LocalTime.of(8, 10); // 8:10 is the cutoff — arriving by 8:10 is still "on time"
        LocalTime end   = LocalTime.of(17, 0);  // no overtime counted past 5PM

        for (int i = 0; i < 5168; i++) {
            if (timeIn[i] == null || timeOut[i] == null) continue;

            parsedIn[i]  = LocalTime.parse(timeIn[i].trim(),  fmt);
            parsedOut[i] = LocalTime.parse(timeOut[i].trim(), fmt);

            if (!parsedIn[i].isAfter(grace)) {
                // on time — full 8 hours
                dailyHours[i] = 8.0;
            } else {
                // late — cap logout at 5PM, deduct 1 hr lunch break
                LocalTime logout = parsedOut[i].isAfter(end) ? end : parsedOut[i];
                long mins = Duration.between(parsedIn[i], logout).toMinutes();
                mins = (mins > 60) ? mins - 60 : 0;
                dailyHours[i] = Math.min(mins / 60.0, 8.0);
            }
        }
    }

    // sums up hours for a given employee, month, and cutoff period
    static double getHours(String num, String half, int mo) {
        double total = 0;
        for (int j = 0; j < 5168; j++) {
            if (!num.equals(dtrEmpNum[j])) continue;
            if (dtrDate[j] == null) continue;
            try {
                String[] d = dtrDate[j].trim().split("/");
                int m   = Integer.parseInt(d[0]);
                int day = Integer.parseInt(d[1]);
                if (m != mo) continue;
                if ("first".equals(half)  && day >= 1  && day <= 15) total += dailyHours[j];
                if ("second".equals(half) && day >= 16 && day <= 31) total += dailyHours[j];
            } catch (Exception ignored) {}
        }
        // cap to max working hours for the period
        int lastDay = YearMonth.of(2024, mo).lengthOfMonth();
        int maxDays = "first".equals(half) ? 10 : (lastDay == 31 ? 11 : 10);
        return Math.min(total, maxDays * 8.0);
    }

    // SSS contribution table (based on 2023 schedule)
    static double sssTable(double gross) {
        if (gross < 3250)  return 135.00;
        if (gross < 3750)  return 157.50;
        if (gross < 4250)  return 180.00;
        if (gross < 4750)  return 202.50;
        if (gross < 5250)  return 225.00;
        if (gross < 5750)  return 247.50;
        if (gross < 6250)  return 270.00;
        if (gross < 6750)  return 292.50;
        if (gross < 7250)  return 315.00;
        if (gross < 7750)  return 337.50;
        if (gross < 8250)  return 360.00;
        if (gross < 8750)  return 382.50;
        if (gross < 9250)  return 405.00;
        if (gross < 9750)  return 427.50;
        if (gross < 10250) return 450.00;
        if (gross < 10750) return 472.50;
        if (gross < 11250) return 495.00;
        if (gross < 11750) return 517.50;
        if (gross < 12250) return 540.00;
        if (gross < 12750) return 562.50;
        if (gross < 13250) return 585.00;
        if (gross < 13750) return 607.50;
        if (gross < 14250) return 630.00;
        if (gross < 14750) return 652.50;
        if (gross < 15250) return 675.00;
        if (gross < 15750) return 697.50;
        if (gross < 16250) return 720.00;
        if (gross < 16750) return 742.50;
        if (gross < 17250) return 765.00;
        if (gross < 17750) return 787.50;
        if (gross < 18250) return 810.00;
        if (gross < 18750) return 832.50;
        if (gross < 19250) return 855.00;
        if (gross < 19750) return 877.50;
        if (gross < 20250) return 900.00;
        if (gross < 20750) return 922.50;
        if (gross < 21250) return 945.00;
        if (gross < 21750) return 967.50;
        if (gross < 22250) return 990.00;
        if (gross < 22750) return 1012.50;
        if (gross < 23250) return 1035.00;
        if (gross < 23750) return 1057.50;
        if (gross < 24250) return 1080.00;
        if (gross < 24750) return 1102.50;
        return 1125.00;
    }

    // employee share of PhilHealth premium — 3% of gross, split 50/50
    static double philhealthShare(double gross) {
        double premium;
        if (gross <= 10000)      premium = 300.00;
        else if (gross >= 60000) premium = 1800.00;
        else                     premium = gross * 0.03;
        return premium * 0.50;
    }

    // Pag-IBIG: 1% if below 1500, 2% otherwise — max 100 pesos
    static double pagibigShare(double base) {
        double c = (base <= 1500) ? base * 0.01 : base * 0.02;
        return Math.min(c, 100.00);
    }

    // BIR withholding tax brackets (monthly)
    static double withholdingTax(double taxable) {
        if (taxable <= 20832)  return 0;
        if (taxable <= 33332)  return (taxable - 20832) * 0.20;
        if (taxable <= 66666)  return 2500 + (taxable - 33332) * 0.25;
        if (taxable <= 166666) return 10833 + (taxable - 66666) * 0.30;
        if (taxable <= 666666) return 40833.33 + (taxable - 166666) * 0.32;
        return 200833.33 + (taxable - 666666) * 0.35;
    }

    // quick month selector — returns actual month number (6-12) or -1 to cancel
    static int chooseMonth(Scanner sc) {
        while (true) {
            System.out.println("\nSelect month:");
            System.out.println("[1] June  \n[2] July  \n[3] August  \n[4] September");
            System.out.println("[5] October  \n[6] November  \n[7] December  \n[8] Cancel");
            String in = sc.nextLine().trim();
            switch (in) {
                case "1": return 6;
                case "2": return 7;
                case "3": return 8;
                case "4": return 9;
                case "5": return 10;
                case "6": return 11;
                case "7": return 12;
                case "8": return -1;
                default: System.out.println("Please enter a number from 1 to 8.");
            }
        }
    }

    static String monthLabel(int m) {
        switch (m) {
            case 6: return "June";      case 7: return "July";
            case 8: return "August";    case 9: return "September";
            case 10: return "October";  case 11: return "November";
            case 12: return "December"; default: return "???";
        }
    }

    // handles CSV fields that may contain commas inside quotes (e.g. "1,500.00")
    static String[] splitCSV(String line) {
        boolean quoted = false;
        StringBuilder buf = new StringBuilder();
        int col = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                if (col < cols.length) cols[col++] = buf.toString().trim();
                buf = new StringBuilder();
            } else {
                buf.append(c);
            }
        }
        if (col < cols.length) cols[col] = buf.toString().trim();
        return cols;
    }
}
