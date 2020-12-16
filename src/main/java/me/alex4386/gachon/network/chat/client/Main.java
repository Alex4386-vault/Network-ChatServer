package me.alex4386.gachon.network.chat.client;

public class Main {
    public static FormWindow formWindow;
    public static LoginForm loginForm;

    public static void main(String[] args) {
        loginForm = new LoginForm();
        formWindow = new FormWindow(loginForm.getPanel(), "Login");

        formWindow.open();
    }
}
