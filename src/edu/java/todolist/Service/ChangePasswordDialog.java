package edu.java.todolist.Service;

import javax.swing.*;
import java.awt.*;

import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.VO.UserVO;

public class ChangePasswordDialog extends JDialog {
    private UserVO user;
    private boolean updated = false;

    public ChangePasswordDialog(JFrame parent, UserVO user) {
        super(parent, "비밀번호 변경", true);
        this.user = user;
        setPreferredSize(new Dimension(400, 300));
        setLocationRelativeTo(parent);
        initialize();
        pack();

    }

    private void initialize() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 10));
        JPasswordField currentPasswordField = new JPasswordField();
        JPasswordField newPasswordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("현재 비밀번호"));
        panel.add(currentPasswordField);
        panel.add(new JLabel("새 비밀번호"));
        panel.add(newPasswordField);
        panel.add(new JLabel("새 비밀번호 확인"));
        panel.add(confirmPasswordField);

        JButton okButton = new JButton("변경");
        JButton cancelButton = new JButton("취소");

        okButton.addActionListener(e -> {
            String current = new String(currentPasswordField.getPassword());
            String newPass = new String(newPasswordField.getPassword());
            String confirm = new String(confirmPasswordField.getPassword());

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(this, "모든 필드를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "새 비밀번호가 일치하지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.equals(current)) {
                JOptionPane.showMessageDialog(this, "기존 비밀번호는 사용할 수 없습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!current.equals(user.getPassword())) {
                JOptionPane.showMessageDialog(this, "현재 비밀번호가 틀렸습니다.", "인증 실패", JOptionPane.ERROR_MESSAGE);
                return;
            }
            user.setPassword(newPass);
            int result = UserDAOImple.getInstance().updateUser(user);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "비밀번호가 변경되었습니다.");
                updated = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "비밀번호 변경에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel();
        buttons.add(okButton);
        buttons.add(cancelButton);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    public boolean isUpdated() {
        return updated;
    }
}
