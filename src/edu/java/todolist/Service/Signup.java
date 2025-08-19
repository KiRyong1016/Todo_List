package edu.java.todolist.Service;

import javax.swing.*;
import java.awt.*;

import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.VO.UserVO;

public class Signup extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField usernameField;
    private JButton signupButton;
    private JButton backToLoginButton;

    public Signup() {
        initialize();
    }

    private void initialize() {
        setTitle("회원가입");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 320);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // 이메일 입력
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 5, 10);
        add(new JLabel("이메일:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1;
        add(emailField, gbc);

        // 사용자 이름 입력
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("이름:"), gbc);
        usernameField = new JTextField(20);
        gbc.gridx = 1;
        add(usernameField, gbc);

        // 비밀번호 입력
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("비밀번호:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(passwordField, gbc);

        // 비밀번호 확인 입력
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("비밀번호 확인:"), gbc);
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(confirmPasswordField, gbc);
              
        // 회원가입 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        backToLoginButton = new JButton("로그인으로");
        signupButton = new JButton("회원가입");
        buttonPanel.add(backToLoginButton);
        buttonPanel.add(signupButton);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 10, 0);
        add(buttonPanel, gbc);

        signupButton.addActionListener(e -> signUp());
        backToLoginButton.addActionListener(e -> {
            new Login().setVisible(true); // 로그인 화면 열기
            dispose(); // 회원가입 창 닫기
        });
        
        setVisible(true);
    }

    private void signUp() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if(email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 필드를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if(!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserDAO userDao = UserDAOImple.getInstance();
        if(userDao.isEmailExists(email)) {
            JOptionPane.showMessageDialog(this, "이미 존재하는 이메일입니다.", "가입 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserVO user = new UserVO();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(password);
        user.setAuthProvider("local");

        int result = userDao.registerUser(user);
        if(result == 1) {
            JOptionPane.showMessageDialog(this, "회원가입 성공! 로그인 해주세요.");
            new Login().setVisible(true);
            dispose();  // 회원가입 창 닫기
        } else {
            JOptionPane.showMessageDialog(this, "회원가입 실패. 다시 시도하세요.", "가입 오류", JOptionPane.ERROR_MESSAGE);
        }
    }
}
