package edu.java.todolist.Service;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.VO.UserVO;

public class Signup extends JFrame {
    private static final long serialVersionUID = 1L;

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

        // 이메일
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 5, 10);
        add(new JLabel("이메일:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1;
        add(emailField, gbc);

        // 이름
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("이름:"), gbc);
        usernameField = new JTextField(20);
        gbc.gridx = 1;
        add(usernameField, gbc);

        // 비밀번호
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("비밀번호:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(passwordField, gbc);

        // 비밀번호 확인
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("비밀번호 확인:"), gbc);
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        add(confirmPasswordField, gbc);

        // 버튼 패널 (UI 레이아웃 유지)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        backToLoginButton = new JButton("로그인으로");
        signupButton = new JButton("회원가입");
        buttonPanel.add(backToLoginButton);
        buttonPanel.add(signupButton);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 10, 0);
        add(buttonPanel, gbc);

        // 이벤트
        signupButton.addActionListener(e -> signUp());
        backToLoginButton.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });

        setVisible(true);
    }

    private void signUp() {
        final String email = emailField.getText().trim();
        final String username = usernameField.getText().trim();
        final char[] pwChars = passwordField.getPassword();
        final char[] confirmChars = confirmPasswordField.getPassword();

        // 문자열로 변환(DAO가 문자열을 받는 구조라면 필요)
        final String password = new String(pwChars);
        final String confirmPassword = new String(confirmChars);

        try {
            // 기본 검증
            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "모든 필드를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 이메일 형식 검증(간단한 정규식)
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                JOptionPane.showMessageDialog(this, "이메일 형식이 올바르지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 비밀번호 동일성/강도 체크(정책은 필요 시 조정)
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
                JOptionPane.showMessageDialog(this, "비밀번호는 8자 이상, 영문/숫자를 포함해야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // DAO(getInstance 유지)
            UserDAO userDao = UserDAOImple.getInstance();

            if (userDao.isEmailExists(email)) {
                JOptionPane.showMessageDialog(this, "이미 존재하는 이메일입니다.", "가입 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserVO user = new UserVO();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(password);
            user.setAuthProvider("local");

            int result = userDao.registerUser(user);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "회원가입 성공! 로그인 해주세요.");
                new Login().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "회원가입 실패. 다시 시도하세요.", "가입 오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "회원가입 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        } finally {
            // 보안상 패스워드 배열 초기화
            java.util.Arrays.fill(pwChars, '\0');
            java.util.Arrays.fill(confirmChars, '\0');
        }
    }
}
