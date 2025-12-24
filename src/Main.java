import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalTime;

/*
 Simple Store Operations Management System - Main
 Note: CSV parsing here is minimal (split by comma) and assumes no embedded commas in fields.
*/

public class Main {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        Scanner scan = new Scanner(System.in);

        System.out.println("=== Employee Login ===");
        System.out.print("Enter User ID: ");
        String userID = scan.nextLine();

        System.out.print("Enter Password: ");
        String password = scan.nextLine();

        // --- Login state variables ---
        boolean isFound = false;       // set true when credentials match a CSV record
        String userName = "";         // Employee name read from employee.csv
        String userRole = "";         // Role read from employee.csv (e.g., Manager)
        String outletName = "Unknown"; // resolved outlet name after lookup

        try (Scanner fileScanner = new Scanner(new File("employee.csv"))) {
            // Iterate over employee.csv lines to locate matching credentials
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                // Expecting: EmployeeID,EmployeeName,Role,Password
                // Expecting line format: EmployeeID,EmployeeName,Role,Password
                if (parts.length >= 4) {
                    String fileUser = parts[0].trim();
                    String fileName = parts[1].trim();
                    String fileRole = parts[2].trim();
                    String filePass = parts[3].trim();

                    // Skip header row if present
                    if (fileUser.equalsIgnoreCase("EmployeeID")) continue;

                    if (fileUser.equals(userID) && filePass.equals(password)) {
                        isFound = true;
                        userName = fileName;
                        userRole = fileRole;
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Employee file not found.");
        }

        if (isFound) {
            System.out.println("\nLogin Successful!");
            System.out.println("\nWelcome, " + userName + " (" + userID + ")");

            // If the authenticated user is a Manager, allow registering new employees
            if (userRole.equalsIgnoreCase("Manager")) {
                String answer = "";
                do {
                    System.out.print("\nDo you want to register a new employee? (yes/no): ");
                    answer = scan.nextLine();
                        if (answer.equalsIgnoreCase("yes")) {
                        // Append a new employee line to employee.csv (simple CSV append)
                        BufferedWriter writer = new BufferedWriter(new FileWriter("employee.csv", true));
                        System.out.print("\nEnter New Employee ID: ");
                        String newEmpID = scan.nextLine();
                        System.out.print("Enter New Employee Name: ");
                        String newEmpName = scan.nextLine();
                        System.out.print("Enter New Employee Role: ");
                        String newEmpRole = scan.nextLine();
                        System.out.print("Enter New Employee Password: ");
                        String newEmpPass = scan.nextLine();
                        // append new employee to CSV
                        writer.write(newEmpID + "," + newEmpName + "," + newEmpRole + "," + newEmpPass);
                        writer.newLine();
                        writer.close();
                        System.out.println("\nEmployee successfully registered! ");
                    } else if (answer.equalsIgnoreCase("no")) {
                        break;
                    } else {
                        System.out.println("Invalid input. Please enter 'yes' or 'no'.");
                    }
                } while (!answer.equalsIgnoreCase("no"));
            }
        } else {
            System.out.println("\nLogin Failed: Invalid User ID or Password.");
            return; //program ends
        }

        // --- Attendance Clock - In ---
        // Determine outlet from employee ID prefix or ask the user for an outlet code.
        System.out.println("\n=== Attendance Clock - In ===");
        System.out.println("Employee ID: " + userID);
        System.out.println("Employee Name: " + userName);

        String outletCode_Prefix = "";
        if (userID != null && userID.length() >= 3) {
            outletCode_Prefix = userID.substring(0, 3);
        } else {
            System.out.print("Enter Outlet Code: ");
            outletCode_Prefix = scan.nextLine().trim();
        }

        try (Scanner outletScanner = new Scanner(new File("outlet.csv"))) {
            boolean outletFound = false;
            while (outletScanner.hasNextLine()) {
                String line = outletScanner.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                // Expecting: OutletCode,OutletName
                if (parts.length >= 2) {
                    String fileOutletCode = parts[0].trim();
                    String fileOutletName = parts[1].trim();

                    if (fileOutletCode.equalsIgnoreCase(outletCode_Prefix)) {
                        outletName = fileOutletName;
                        outletFound = true;
                        break;
                    }
                }
            }
            if (!outletFound) {
                System.out.println("Outlet not found for code prefix: " + outletCode_Prefix);
            } else {
                System.out.println("Outlet Name: " + outletCode_Prefix + " " + "("+ outletName + ")");
            }
        } catch (FileNotFoundException e) {
            System.out.println("Outlet file not found.");

        }

        //Attendance Clock-In
        LocalDate date = LocalDate.now();
        // Truncate nanoseconds so output shows only HH:mm:ss
        LocalTime timeIn = LocalTime.now().withNano(0);
        System.out.println("\nClock-In Successful!");
        System.out.println("Date: " + date);
        System.out.println("Time: " + timeIn);
        
        //Stock Management Module
        System.out.println("\n=== Stock Management Module ===");
        System.out.println("1. Morning/Night Stock Count");
        System.out.println("2. Stock Movement (In/Out)");
        System.out.println("3. Skip");
        System.out.print("Select activity: ");
        
        String stockChoiceStr = scan.nextLine();
        int stockChoice = 0;
        try {
            stockChoice = Integer.parseInt(stockChoiceStr);
        } catch (NumberFormatException e) {
            stockChoice = 3; // Default skip
        }

        if (stockChoice == 1) {
            // --- 1. STOCK COUNT ---
            System.out.println("\n--- Stock Count Mode (" + (LocalTime.now().getHour() < 12 ? "Morning" : "Night") + ") ---");
            
            // Logic: Cari column index dalam model.csv yang sama dengan Outlet Code user (contoh: "C60")
            int targetColumnIndex = -1; 
            
            java.util.ArrayList<String> modelList = new java.util.ArrayList<>();
            java.util.ArrayList<Integer> recordedQtyList = new java.util.ArrayList<>();

            try (Scanner modelFile = new Scanner(new File("model.csv"))) {
                if (modelFile.hasNextLine()) {
                    
                    String headerLine = modelFile.nextLine().trim();
                    String[] headers = headerLine.split(",");
                    
                    // Loop header untuk cari column mana milik outlet user
                    for (int i = 0; i < headers.length; i++) {
                        
                        if (headers[i].trim().equalsIgnoreCase(outletCode_Prefix)) {
                            targetColumnIndex = i;
                            break;
                        }
                    }
                }

                if (targetColumnIndex == -1) {
                    System.out.println("Error: Outlet code " + outletCode_Prefix + " not found in model.csv headers.");
                } else {
                
                    while (modelFile.hasNextLine()) {
                        String line = modelFile.nextLine().trim();
                        if (line.isEmpty()) continue;
                        
                        String[] parts = line.split(",");
                        
                        
                        if (parts.length > targetColumnIndex) {
                            String mName = parts[0].trim(); 
                            
                            
                            int qty = 0;
                            try {
                                qty = Integer.parseInt(parts[targetColumnIndex].trim());
                            } catch (Exception e) { qty = 0; }

                            modelList.add(mName);
                            recordedQtyList.add(qty);
                        }
                    }
                    
                    //Stock Count Process
                    int correctTally = 0;
                    int mismatchCount = 0;
                    
                    System.out.println("Outlet: " + outletCode_Prefix + " (" + outletName + ")");
                    System.out.println("Date: " + LocalDate.now());
                    System.out.println("--------------------------------");

                    for (int i = 0; i < modelList.size(); i++) {
                        String mName = modelList.get(i);
                        int sysQty = recordedQtyList.get(i);

                        System.out.println("\nModel: " + mName);
                        System.out.print("Enter Physical Count: ");
                        int userCount = 0;
                        try {
                            userCount = Integer.parseInt(scan.nextLine());
                        } catch (Exception e) { userCount = 0; }

                        System.out.println("Store Record: " + sysQty);

                        if (userCount == sysQty) {
                            System.out.println("Status: Stock tally correct."); 
                            correctTally++;
                        } else {
                            int diff = Math.abs(userCount - sysQty);
                            System.out.println("Status: ! Mismatch detected (" + diff + " unit difference)");
                            mismatchCount++;
                        }
                    }

                    // Summary
                    System.out.println("\n--------------------------------");
                    System.out.println("Total Models Checked: " + modelList.size());
                    System.out.println("Tally Correct: " + correctTally);
                    System.out.println("Mismatches: " + mismatchCount);
                    
                    if (mismatchCount > 0) {
                        System.out.println("Warning: Please verify stock.");
                    } else {
                        System.out.println("Stock count completed successfully.");
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("Error: model.csv file missing.");
            }

        } else if (stockChoice == 2) {
            // --- 2. STOCK MOVEMENT (Generate Receipt) ---
            System.out.println("\n--- Stock Movement ---");
            System.out.println("1. Stock In (Received)");
            System.out.println("2. Stock Out (Transfer)");
            System.out.print("Select type: ");
            String typeInput = scan.nextLine();
            
            String transType = typeInput.equals("1") ? "Stock In" : "Stock Out";
            
            // Setup info untuk receipt
            String currentOutletInfo = outletCode_Prefix + " (" + outletName + ")";
            String fromOutlet = "";
            String toOutlet = "";

            if (transType.equals("Stock In")) {
                System.out.print("From (Outlet Code/HQ): ");
                fromOutlet = scan.nextLine();
                toOutlet = currentOutletInfo; 
            } else {
                fromOutlet = currentOutletInfo;
                System.out.print("To (Outlet Code): ");
                toOutlet = scan.nextLine();
            }

            
            StringBuilder modelsBuffer = new StringBuilder();
            int totalQty = 0;
            String moreModels = "y";

            while (moreModels.equalsIgnoreCase("y")) {
                System.out.print("Enter Model Name: ");
                String mName = scan.nextLine();
                System.out.print("Enter Quantity: ");
                int qty = 0;
                try {
                    qty = Integer.parseInt(scan.nextLine());
                } catch (Exception e) { qty = 0; }

                
                modelsBuffer.append(" - ").append(mName).append(" (Quantity: ").append(qty).append(")\n");
                totalQty += qty;

                System.out.print("Add another model? (y/n): ");
                moreModels = scan.nextLine();
            }

            // Generate Receipt File 
            LocalDate today = LocalDate.now();
            String receiptFileName = "receipts_" + today + ".txt";
            
            String receiptContent = 
                "\n=== " + transType + " ===\n" +
                "Date: " + today + "\n" +
                "Time: " + LocalTime.now().withNano(0) + "\n" +
                "From: " + fromOutlet + "\n" +
                "To: " + toOutlet + "\n" +
                "Models:\n" + modelsBuffer.toString() +
                "Total Quantity: " + totalQty + "\n" +
                "Employee: " + userName + "\n" +
                "-----------------------------------\n";

            System.out.println(receiptContent);
            
            // Save ke text file (Append mode: true)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(receiptFileName, true))) {
                writer.write(receiptContent);
                
                System.out.println("Receipt generated: " + receiptFileName);
                System.out.println(transType + " recorded.");
            } catch (Exception e) {
                System.out.println("Error saving receipt file.");
            }
        }

        //Attendance Clock-Out
        System.out.println("\n=== Attendance Clock - Out ===");
        System.out.println("Employee ID: " + userID);
        System.out.println("Employee Name: " + userName);
        System.out.println("Outlet Name: " + outletCode_Prefix + " " + "("+ outletName + ")"); 
        System.out.println("\nClock-Out Successful!");
        LocalTime timeOut = LocalTime.now().withNano(0);
        System.out.println("Date: " + date);
        System.out.println("Time: " + timeOut);
        System.out.println("Total Hours Worked: " + java.time.Duration.between(timeIn, timeOut).toHours() + " hours");

        scan.close();
    }
}
