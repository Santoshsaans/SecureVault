package src.ui;

import src.crypto.PasswordGenerator;
import src.storage.VaultEntry;
import src.storage.VaultFile;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private static final String VAULT_PATH = "vault.dat";
    private final Scanner scanner = new Scanner(System.in);
    private final Console console = System.console();

    public void start() {
        System.out.println("=== SecureVault (Console) ===");
        VaultFile vault = new VaultFile(VAULT_PATH);

        if (!vault.exists()) {
            System.out.println("No vault found. Creating new vault.");
            createVault(vault);
        } else {
            System.out.println("Vault found. Please unlock.");
        }

        List<VaultEntry> entries = null;
        while (entries == null) {
            char[] master = readPassword("Master password: ");
            try {
                entries = vault.unlock(master);
            } catch (Exception e) {
                System.out.println("Incorrect master password or corrupted vault. Try again.");
            }
        }

        System.out.println("Vault unlocked. " + entries.size() + " entries loaded.");
        menu(vault, entries);
    }

    private void createVault(VaultFile vault) {
        while (true) {
            char[] p1 = readPassword("Set master password: ");
            char[] p2 = readPassword("Confirm master password: ");
            if (p1.length < 8) {
                System.out.println("Password must be at least 8 characters. Try again.");
                continue;
            }
            if (new String(p1).equals(new String(p2))) {
                try {
                    vault.createNew(p1, new ArrayList<>());
                    System.out.println("Vault created successfully.");
                    return;
                } catch (Exception e) {
                    System.out.println("Error creating vault: " + e.getMessage());
                }
            } else {
                System.out.println("Passwords do not match. Try again.");
            }
        }
    }

    private void menu(VaultFile vault, List<VaultEntry> entries) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("1. List entries");
            System.out.println("2. Add entry");
            System.out.println("3. Show password for site");
            System.out.println("4. Delete entry");
            System.out.println("5. Generate password");
            System.out.println("6. Lock & exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": listEntries(entries); break;
                case "2": addEntry(vault, entries); break;
                case "3": showPassword(entries); break;
                case "4": deleteEntry(vault, entries); break;
                case "5": generatePassword(); break;
                case "6": running = false; break;
                default: System.out.println("Invalid choice.");
            }
        }
        System.out.println("Vault locked. Goodbye.");
    }

    private void listEntries(List<VaultEntry> entries) {
        if (entries.isEmpty()) { System.out.println("No entries."); return; }
        for (int i = 0; i < entries.size(); i++) {
            VaultEntry e = entries.get(i);
            System.out.println((i + 1) + ". " + e.getSite() + " (" + e.getUsername() + ")");
        }
    }

    private void addEntry(VaultFile vault, List<VaultEntry> entries) {
        System.out.print("Site: ");
        String site = scanner.nextLine().trim();
        System.out.print("Username: ");
        String user = scanner.nextLine().trim();
        System.out.print("Password (blank = generate): ");
        String pass = scanner.nextLine();
        if (pass.isBlank()) {
            pass = PasswordGenerator.generate(20);
            System.out.println("Generated: " + pass);
        }
        System.out.print("Notes: ");
        String notes = scanner.nextLine().trim();

        entries.add(new VaultEntry(site, user, pass, notes));
        try {
            vault.save(entries);
            System.out.println("Saved.");
        } catch (Exception e) {
            System.out.println("Save error: " + e.getMessage());
        }
    }

    private void showPassword(List<VaultEntry> entries) {
        System.out.print("Site name: ");
        String site = scanner.nextLine().trim();
        for (VaultEntry e : entries) {
            if (e.getSite().equalsIgnoreCase(site)) {
                System.out.print("Show password? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    System.out.println("Password: " + e.getPassword());
                }
                return;
            }
        }
        System.out.println("Not found.");
    }

    private void deleteEntry(VaultFile vault, List<VaultEntry> entries) {
        System.out.print("Site to delete: ");
        String site = scanner.nextLine().trim();
        boolean removed = entries.removeIf(e -> e.getSite().equalsIgnoreCase(site));
        if (removed) {
            try { vault.save(entries); System.out.println("Deleted and saved."); }
            catch (Exception e) { System.out.println("Save error: " + e.getMessage()); }
        } else {
            System.out.println("Not found.");
        }
    }

    private void generatePassword() {
        System.out.print("Length (default 20): ");
        String lenStr = scanner.nextLine().trim();
        int len = lenStr.isBlank() ? 20 : Integer.parseInt(lenStr);
        System.out.println("Generated: " + PasswordGenerator.generate(len));
    }

    private char[] readPassword(String prompt) {
        if (console != null) {
            char[] pw = console.readPassword(prompt);
            return pw != null ? pw : new char[0];
        }
        System.out.print(prompt);
        return scanner.nextLine().toCharArray();
    }
}