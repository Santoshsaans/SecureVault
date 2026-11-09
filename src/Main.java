package src;

import src.ui.ConsoleUI;
import src.ui.SwingUI;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("console")) {
            new ConsoleUI().start();
        } else {
            SwingUI.main(args);
        }
    }
}