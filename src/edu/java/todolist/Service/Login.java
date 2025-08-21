package edu.java.todolist.Service;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.TodoListMain;
import edu.java.todolist.VO.UserVO;

public class Login extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;

    public Login() {
    	setTitle("로그인");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 280);
        setLocationRelativeTo(null);
        getContentPane().setLayout(null);
        getContentPane().setBackground(new Color(245, 245, 245));
        initialize();
    }

    private void initialize() {     
        Font titleFont = new Font("맑은 고딕", Font.BOLD, 28);
        Font labelFont = new Font("맑은 고딕", Font.PLAIN, 16);
        Font btnFont = new Font("맑은 고딕", Font.BOLD, 16);

        JLabel titleLabel = new JLabel("로그인");
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setBounds(150, 20, 100, 40);
        getContentPane().add(titleLabel);

        JLabel emailLabel = new JLabel("이메일:");
        emailLabel.setFont(labelFont);
        emailLabel.setBounds(30, 80, 70, 30);
        getContentPane().add(emailLabel);

        emailField = new JTextField("이메일 입력");
        emailField.setFont(labelFont);
        emailField.setForeground(Color.GRAY);
        emailField.setBounds(110, 80, 230, 30);
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        emailField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (emailField.getText().equals("이메일 입력")) {
                    emailField.setText("");
                    emailField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (emailField.getText().isEmpty()) {
                    emailField.setForeground(Color.GRAY);
                    emailField.setText("이메일 입력");
                }
            }
        });
        emailField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logIn();
            }
        });
        getContentPane().add(emailField);

        JLabel passwordLabel = new JLabel("비밀번호:");
        passwordLabel.setFont(labelFont);
        passwordLabel.setBounds(30, 120, 70, 30);
        getContentPane().add(passwordLabel);

        passwordField = new JPasswordField("비밀번호 입력");
        passwordField.setFont(labelFont);
        passwordField.setForeground(Color.GRAY);
        passwordField.setBounds(110, 120, 230, 30);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                String pwd = new String(passwordField.getPassword());
                if (pwd.equals("비밀번호 입력")) {
                    passwordField.setText("");
                    passwordField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                String pwd = new String(passwordField.getPassword());
                if (pwd.isEmpty()) {
                    passwordField.setForeground(Color.GRAY);
                    passwordField.setText("비밀번호 입력");
                }
            }
        });
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logIn();
            }
        });
        getContentPane().add(passwordField);

        loginButton = new JButton("로그인");
        loginButton.setFont(btnFont);
        loginButton.setBackground(new Color(0, 120, 215));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        loginButton.setBounds(230, 170, 110, 40);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logIn();
            }
        });
        getContentPane().add(loginButton);

        signupButton = new JButton("회원가입");
        signupButton.setFont(btnFont);
        signupButton.setBackground(new Color(100, 100, 100));
        signupButton.setForeground(Color.WHITE);
        signupButton.setFocusPainted(false);
        signupButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        signupButton.setBounds(90, 170, 110, 40);
        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Signup();
                dispose();
            }
        });
        getContentPane().add(signupButton);

        setVisible(true);
    }

    private void logIn() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        UserDAO userDao = UserDAOImple.getInstance();
        UserVO user = userDao.login(email, password);

        if (user != null) {
            //JOptionPane.showMessageDialog(this, user.getUsername() + "님, 로그인 성공!");
            new TodoListMain(user).setVisible(true); // 메인 화면 이동
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "이메일 또는 비밀번호가 잘못되었습니다.", "로그인 실패", JOptionPane.ERROR_MESSAGE);
        }
    }
}
