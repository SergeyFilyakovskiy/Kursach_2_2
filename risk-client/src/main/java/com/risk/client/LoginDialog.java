package com.risk.client;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class LoginDialog {

    /* ------ вход ------ */
    public static boolean showLogin(){
        Dialog<ButtonType> dlg=makeDlg("Вход");
        TextField u=new TextField();
        PasswordField p=new PasswordField();
        GridPane gp=commonGrid(u,p,null);
        dlg.getDialogPane().setContent(gp);

        if(dlg.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){
            String role=ApiClient.login(u.getText(),p.getText());
            if(role!=null) return true;
            alert("Неверные данные", Alert.AlertType.ERROR);
        }
        return false;
    }

    /* ------ регистрация ------ */
    public static void showRegister(){
        Dialog<ButtonType> dlg=makeDlg("Регистрация");
        TextField u=new TextField();
        PasswordField p=new PasswordField();
        ComboBox<String> role=new ComboBox<>();
        role.getItems().addAll("ANALYST","ADMIN","GUEST");
        role.setValue("ANALYST");

        GridPane gp=commonGrid(u,p,role);
        dlg.getDialogPane().setContent(gp);

        if(dlg.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){
            boolean ok=ApiClient.register(u.getText(),p.getText(),role.getValue());
            alert(ok? "Успешно!" : "Логин занят", ok? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        }
    }

    /* ---- helpers ---- */
    private static Dialog<ButtonType> makeDlg(String title){
        Dialog<ButtonType> d=new Dialog<>();
        d.setTitle(title);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        return d;
    }
    private static GridPane commonGrid(TextField u,PasswordField p,ComboBox<String> role){
        GridPane g=new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        g.addRow(0,new Label("Логин:"),u);
        g.addRow(1,new Label("Пароль:"),p);
        if(role!=null) g.addRow(2,new Label("Роль:"),role);
        return g;
    }
    private static void alert(String msg,Alert.AlertType t){
        new Alert(t,msg,ButtonType.OK).showAndWait();
    }
}
