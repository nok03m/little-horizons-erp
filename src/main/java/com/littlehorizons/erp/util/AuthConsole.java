package com.littlehorizons.erp.util;

import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.io.Console;

/** Input and output used by authentication, with a masked dialog when no terminal exists. */
public final class AuthConsole {

    private final Console console;

    public AuthConsole(Console console) {
        this.console = console;
    }

    public String readLine(String prompt) {
        if (console != null) {
            return console.readLine(prompt);
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Authentication requires a terminal or a graphical desktop.");
        }
        String value = JOptionPane.showInputDialog(null, prompt, "Little Horizons ERP", JOptionPane.QUESTION_MESSAGE);
        return value == null ? "" : value;
    }

    public char[] readPassword(String prompt) {
        if (console != null) {
            return console.readPassword(prompt);
        }
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Password entry requires a terminal or a graphical desktop.");
        }
        JPasswordField passwordField = new JPasswordField(20);
        int result = JOptionPane.showConfirmDialog(null, passwordField, prompt,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return passwordField.getPassword();
    }

    public void printf(String format, Object... arguments) {
        if (console != null) {
            console.printf(format, arguments);
        } else {
            System.out.printf(format, arguments);
        }
    }
}
