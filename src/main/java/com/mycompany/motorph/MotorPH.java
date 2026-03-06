/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.motorph;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.LocalTime;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

/**
 * ==============================================================
 * Contains most of the primary variables for use in the system.
 * Also contains all methods needed to run the program.
 * Handles CSV data reading, employee details presentation,
 * estimation of working hours, and payroll calculation
 * from June to December 2024.
 * 
 * @author Cail Maven Lusares
 * @author Dominic Gideon Remetio Abad
 * @author Dominic Lagitao
 * @author Luis Carlo Dayag
 * @author Marlu Gonzales
 * @version 1.0
 * ==============================================================
 */
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

    /**
     * ====================================================================
     * This starts the MotorPH Payroll System.
     * This method starts the whole system by loading the CSV files,
     * computing daily hours, and checking login credentials.
     * @param args command-line arguments (not used)
     * @throws FileNotFoundException if the employee or DTR CSV files are in the same directory as the code. 
     * 
     * ====================================================================
     */
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
    
    /**
     * ==============================================================
     * Displays the employee portal menu.
     * For displaying employee details/exiting the system.
     * NO access to payroll processing.
     * 
     * @param sc Scanner object used to read user input.
     * 
     * ==============================================================
     */
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
                System.exit(0);
            } else {
                System.out.println("Please enter 1 or 2 only.");
            }
        }
    }

    /**
     * ==============================================================
     * Displays the payroll portal menu and handles user interaction.
     * For generating a payslip/exiting the system.
     * 
     * @param sc Scanner object used to read user input.
     * 
     * ==============================================================
     */
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
                System.exit(0);
            } else {
                System.out.println("Please enter 1 or 2 only.");
            }
        }
    }

    /**
     * ==============================================================
     * Displays the payslip processing menu.
     * For deciding whether to process for one employee or for all employees.
     * 
     * @param sc Scanner object used to read user input.
     * 
     * ==============================================================
     */
    static void runPayslipMenu(Scanner sc) {
        while (true) {
            System.out.println("\n---- Generate Payslip ----");
            System.out.println("[1] Single Employee");
            System.out.println("[2] All Employees");
            System.out.println("[3] Exit");
            String pick = sc.nextLine().trim();

            if ("1".equals(pick)) {
                processSingleEmployee(sc);
            } else if ("2".equals(pick)) {
                processAllEmployees(sc);
            } else if ("3".equals(pick)) {
                System.out.println("Exiting payroll system.");
                sc.close();
                System.exit(0);
            } else {
                System.out.println("Please enter 1, 2, or 3 only.");
            }
        }
    }

    /**
     * ==============================================================
     * Process payroll for one employee.
     * 
     * This method uses a for loop to continue running the logic from June to December.
     * Then, the user is prompted to type a valid Employee ID.
     * The system then searches for the employee related to that ID.
     * Afterwards, it calculates hours worked, gross salary, government deductions, and net salary for each employee.
     * Finally, it prints a payroll summary for each employee.
     * 
     * @param sc Scanner object used to read user input.
     * 
     * ==============================================================
     */
    static void processSingleEmployee(Scanner sc) {
        //Asks user to input a valid employee ID
        System.out.print("Employee No.: ");
        String num = sc.nextLine().trim();

        //Searches for the inputted employee ID in the employee data 
        int idx = -1;
        for (int i = 0; i < empNum.length; i++) {
            if (num.equals(empNum[i])) { idx = i; break; }
        }

        //If employee ID is invalid, message is displayed and user will be prompted to try again.
        if (idx == -1) { System.out.println("Employee number does not exist."); return; }
        
        System.out.println("\n----Employee Details----");
        System.out.println("Employee #   : " + empNum[idx]);
        System.out.println("Employee Name: " + fname[idx] + " " + lname[idx]);
        System.out.println("Birthday     : " + bday[idx]);
            
        for (int mo = 6; mo <= 12; mo++) {
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

            System.out.println("\n========================================");
            System.out.println("  Cutoff: " + mn + " 1 to " + mn + " 15");
            System.out.println("========================================");
            System.out.println("Total Hours Worked : " + hrs1);
            System.out.println("Gross Salary       : Php" +  gross1);
            System.out.println("Net Salary         : Php" + net1);

            System.out.println("\n========================================");
            System.out.println("  Cutoff: " + mn + " 16 to " + mn + " " + lastDay);
            System.out.println("========================================");
            System.out.println("Total Hours Worked : " + hrs2);
            System.out.println("Gross Salary       : Php" + gross2);
            System.out.println("\n--- Statutory Deductions ---");
            System.out.println("  SSS             : Php" + sss);
            System.out.println("  PhilHealth      : Php" + philhealth);
            System.out.println("  Pag-IBIG        : Php" + pagibig);
            System.out.println("  Tax             : Php" + whTax);
            System.out.println("  Subtotal        : Php" + totalDed);
            System.out.println("Net Salary         : Php" + net2);
            System.out.println("========================================");
        }
    }
    
    /**
     * ==============================================================================================================
     * Process payroll for all employees (bulk).
     * 
     * This method uses a for loop to continue running the logic from June to December.
     * Then, the for loop iterates through all employees in the system.
     * Afterwards, it calculates hours worked, gross salary, government deductions, and net salary for each employee.
     * Finally, it prints a payroll summary for each employee.
     * 
     * @param sc Scanner object used to read user input
     * 
     * ==============================================================================================================
     */
    static void processAllEmployees(Scanner sc) {
        // Loops from June to December
        for (int mo = 6; mo <= 12; mo++) {
            String mn   = monthLabel(mo);
            int lastDay = YearMonth.of(2024, mo).lengthOfMonth();

            // Loop through all employees.
            for (int i = 0; i < empNum.length; i++) {
                if (empNum[i] == null) continue;

                String num  = empNum[i];
                double rate = Double.parseDouble(hrRate[i]);
                double base = Double.parseDouble(basePay[i]);

                // Compute hours and gross pay for first and second cutoff.
                double hrs1   = getHours(num, "first",  mo);
                double hrs2   = getHours(num, "second", mo);
                double gross1 = hrs1 * rate;
                double gross2 = hrs2 * rate;

                // Compute government deductions.
                double combined   = gross1 + gross2;
                double sss        = sssTable(combined);
                double philhealth = philhealthShare(combined);
                double pagibig    = pagibigShare(base);
                double taxable    = combined - sss - philhealth - pagibig;
                double whTax      = withholdingTax(taxable);
                double totalDed   = sss + philhealth + pagibig + whTax;

                // Compute net salaries.
                double net1 = gross1;
                double net2 = gross2 - totalDed;

                // Print employee payroll summary.
                System.out.println("\n========================================");
                System.out.println("Employee #   : " + num);
                System.out.println("Name         : " + fname[i] + " " + lname[i]);
                System.out.println("Birthday     : " + bday[i]);
                System.out.println("\n  Cutoff: " + mn + " 1 to " + mn + " 15");
                System.out.println("  Total Hours Worked : " + hrs1);
                System.out.println("  Gross Salary       : Php" + gross1);
                System.out.println("  Net Salary         : Php" + net1);
                System.out.println("\n  Cutoff: " + mn + " 16 to " + mn + " " + lastDay);
                System.out.println("  Total Hours Worked : " + hrs2);
                System.out.println("  Gross Salary       : Php" + gross2);
                System.out.println("  SSS                : Php" + sss);
                System.out.println("  PhilHealth         : Php" + philhealth);
                System.out.println("  Pag-IBIG           : Php" + pagibig);
                System.out.println("  Tax                : Php" + whTax);
                System.out.println("  Total Deductions   : Php" + totalDed);
                System.out.println("  Net Salary         : Php" + net2);
                System.out.println("========================================");
            }
        }
    }

    /**
     * ==============================================================================================================
     * Loads the employee master list CSV.
     * 
     * This method first opens the CSV file.
     * Then, the while loop uses a splitCSV method to slice off the commas.
     * Once completed, it stores the data from the approriate columns to the correct variables.
     * The following variables are stored: empNum (employee ID), lname (last name), fname
     * (first name), bday (birth date), basePay (base pay). and hrRate (hourly rate).
     * 
     * @param path The file pathname to the CSV file
     * @throws FileNotFoundException if the CSV file is not found at the specified path
     * 
     * ==============================================================================================================
     */
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

    /**
     * ==============================================================================================================
     * Loads the daily time record (DTR).
     * 
     * This method first opens the CSV file.
     * Then, the while loop uses a splitCSV method to slice off the commas.
     * Once completed, it stores the data from the appropriate columns to the correct variables.
     * The following variables are stored: dtrEmpNum (employee ID), dtrDate (date), timeIn, and timeOut.
     * 
     * @param path The file pathname to the CSV file
     * @throws FileNotFoundException if the CSV file is not found at the specified path
     * 
     * ==============================================================================================================
     */
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

    /**
     * ==============================================================================================================
     * Performs a preliminary computation of hours an 
     * employee worked based on the DTR CSV file and 
     * stores it in advance so that this method does
     * not have to be recalled for every payslip generation.
     * 
     * Note: An employee is late once he/she logs in from
     * 8:11 AM onwards. No overtime is counted for now.
     * 1 hour lunch break is also deducted in the logic.
     * 
     * ====================================================================
     */
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

    /**
     * ==============================================================================================================
     * Sums up hours for an employee, given the month and cutoff period
     * This method is called in both processSingleEmployee
     * and processAllEmployees to calculate gross pay.
     * 
     * @param num  The employee ID number as a String must match dtrEmpNum array).
     * @param half The cutoff period; must be either "first" (days 1-15) or "second" (days 16-end).
     * @param mo   The month of the year as an integer.
     * @return     The total hours worked for the period.
     * 
     * ====================================================================
     */
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
        return total;
    }

   /**
     * ====================================================================
     * Calculates an employee's SSS contribution based on the gross salary.
     * 
     * The contribution is determined using fixed salary brackets. 
     * Each bracket has a set contribution amount. 
     * If the employee's salary is within a bracket, 
     * the method returns the contribution assigned to that bracket.
     * 
     * @param gross The employee's gross month salary.
     * @return The corresponding SSS contribution.
     * 
     * ====================================================================
     */
    static double sssTable(double gross) {
        if (gross < 3250)   return 135.00;
        if (gross <= 3750)  return 157.50;
        if (gross <= 4250)  return 180.00;
        if (gross <= 4750)  return 202.50;
        if (gross <= 5250)  return 225.00;
        if (gross <= 5750)  return 247.50;
        if (gross <= 6250)  return 270.00;
        if (gross <= 6750)  return 292.50;
        if (gross <= 7250)  return 315.00;
        if (gross <= 7750)  return 337.50;
        if (gross <= 8250)  return 360.00;
        if (gross <= 8750)  return 382.50;
        if (gross <= 9250)  return 405.00;
        if (gross <= 9750)  return 427.50;
        if (gross <= 10250) return 450.00;
        if (gross <= 10750) return 472.50;
        if (gross <= 11250) return 495.00;
        if (gross <= 11750) return 517.50;
        if (gross <= 12250) return 540.00;
        if (gross <= 12750) return 562.50;
        if (gross <= 13250) return 585.00;
        if (gross <= 13750) return 607.50;
        if (gross <= 14250) return 630.00;
        if (gross <= 14750) return 652.50;
        if (gross <= 15250) return 675.00;
        if (gross <= 15750) return 697.50;
        if (gross <= 16250) return 720.00;
        if (gross <= 16750) return 742.50;
        if (gross <= 17250) return 765.00;
        if (gross <= 17750) return 787.50;
        if (gross <= 18250) return 810.00;
        if (gross <= 18750) return 832.50;
        if (gross <= 19250) return 855.00;
        if (gross <= 19750) return 877.50;
        if (gross <= 20250) return 900.00;
        if (gross <= 20750) return 922.50;
        if (gross <= 21250) return 945.00;
        if (gross <= 21750) return 967.50;
        if (gross <= 22250) return 990.00;
        if (gross <= 22750) return 1012.50;
        if (gross <= 23250) return 1035.00;
        if (gross <= 23750) return 1057.50;
        if (gross <= 24250) return 1080.00;
        if (gross <= 24750) return 1102.50;
        return 1125.00;
    }

    /**
     * =================================================================================================================================
     * Calculate an employee's share of PhilHealth premium.
     * 
     * This is 3% of the gross income, split between the
     * employer and employee (50/50).
     * 
     * Example: Employee's gross is 5000.
     * 1. They fall into the 1st bracket (300 premium).
     * 2. The premium is then multiplied by 0.50 to get the employee's share.
     * 
     * If the gross is greater than 60000, the premium is capped at 1800.
     * If it is between 10000 and 60000, premium is 0.03 of the gross.
     * 
     * @param taxable The employee's taxable income.
     * @return The corresponding withholding tax.
     * 
     * =================================================================================================================================
     */
    static double philhealthShare(double gross) {
        double premium;
        if (gross <= 10000)      premium = 300.00;
        else if (gross >= 60000) premium = 1800.00;
        else                     premium = gross * 0.03;
        return premium * 0.50;
    }

    /**
     * =================================================================================================================================
     * Calculate an employee's share of Pag-Ibig contributions.
     * 
     * 
     * Example: Employee's base is 1000
     * THe base is multiplied only by 1%.
     * 
     * @param base The employee's base salary.
     * @return The Pag-Ibig contribution, which is maximum 100.
     * 
     * =================================================================================================================================
     */
    static double pagibigShare(double base) {
        double c = (base <= 1500) ? base * 0.01 : base * 0.02;
        return Math.min(c, 100.00);
    }

    /**
     * =================================================================================================================================
     * Calculate an employee's withholding tax.
     * 
     * The tax is based on an employee's taxable income.
     * Each salary bracket has a fixed base tax, plus a percentage of the portion of income that goes above the start of that bracket.
     * 
     * Example: Employee's taxable income is is 37567.08
     * 1. They fall into the 3rd bracket.
     * 2. The income above the start of the bracket (33,333) is calculated; this is the excess.
     * 3. The excess is multiplied by the tax rate of 0.25.
     * 4. Finally, it adds the fixed base tax of 2500 is added to get the total tax.
     * 
     * @param taxable The employee's taxable income.
     * @return The corresponding withholding tax.
     * 
     * =================================================================================================================================
     */
    static double withholdingTax(double taxable) {
        if (taxable <= 20832)  return 0;
        if (taxable <= 33332)  return (taxable - 20833) * 0.20;
        if (taxable <= 66666)  return 2500 + (taxable - 33333) * 0.25;
        if (taxable <= 166666) return 10833 + (taxable - 66667) * 0.30;
        if (taxable <= 666666) return 40833.33 + (taxable - 166667) * 0.32;
        return 200833.33 + (taxable - 666666) * 0.35;
    }

    /**
     * =================================================================================================================================
     * Method used to display months in generated payroll.
     * 
     * 
     * Example: Employee's gross is 5000.
     * 1. They fall into the 1st bracket (300 premium).
     * 2. The premium is then multiplied by 0.50 to get the employee's share.
     * 
     * If the base salary is 1500 or less, only 0.01 of the base is taken.
     * If greater than 1500, 0.02 of the base is taken.
     * 
     * @param m The month number (6-12).
     * 
     * =================================================================================================================================
     */
    static String monthLabel(int m) {
        switch (m) {
            case 6: return "June";      case 7: return "July";
            case 8: return "August";    case 9: return "September";
            case 10: return "October";  case 11: return "November";
            case 12: return "December"; default: return "???";
        }
    }

    /**
     * =====================================================================
     * Splits a CSV line into an array of strings.
     * 
     * Handles quoted fields, commas inside quotes are ignored.
     * Leading and trailing spaces are trimmed from each field.
     * 
     * @param line The CSV line to split.
     * @return An array of strings representing each column in the CSV line.
     * 
     * =====================================================================
     */
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
