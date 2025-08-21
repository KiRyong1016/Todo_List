package edu.java.todolist.Service;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.java.todolist.DAO.TodoDAO;
import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;

public class TodoEditDialog extends JDialog {
    private TodoVO todo;
    private boolean isNew;
    private int todoId;
    private final TodoDAO todoDAO = TodoDAOImple.getInstance();
    
    private JTextField titleField;
    private JTextField descriptionField;
    private JTextField dueDateField;  // yyyy-MM-dd HH:mm 형식으로 수정
    private JComboBox<TodoVO.Priority> priorityCombo;
    private JComboBox<TodoVO.Status> statusCombo;
    private JTextField categoryField;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TodoEditDialog(TodoVO todo, int userId) {   	
        if (todo == null) {
            this.todo = new TodoVO();  
            this.isNew = true;
            this.todo.setUserId(userId);  // 여기 userId를 파라미터로 받아서 설정
        } else {
            this.todo = todo;
            this.isNew = false;
        }    

        setTitle(isNew ? "할 일 추가" : "할 일 수정");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setModal(true);
        initUI();
    }
    
    private void initUI() {
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        Font font = new Font("맑은 고딕", Font.PLAIN, 14);

        inputPanel.add(new JLabel("제목:"));
        titleField = new JTextField(this.todo.getTitle() != null ? this.todo.getTitle() : "");
        titleField.setFont(font);
        inputPanel.add(titleField);

        inputPanel.add(new JLabel("내용:"));
        descriptionField = new JTextField(this.todo.getDescription() != null ? this.todo.getDescription() : "");
        descriptionField.setFont(font);
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("마감기한 (yyyy-MM-dd HH:mm):"));
        String dueDateStr = this.todo.getDueDate() != null ? this.todo.getDueDate().format(DATE_TIME_FORMATTER) : "";
        dueDateField = new JTextField(dueDateStr);
        dueDateField.setFont(font);
        inputPanel.add(dueDateField);

        inputPanel.add(new JLabel("중요도:"));
        priorityCombo = new JComboBox<>(TodoVO.Priority.values());
        priorityCombo.setSelectedItem(this.todo.getPriority() != null ? this.todo.getPriority() : TodoVO.Priority.중);  // 기본값 설정 가능
        inputPanel.add(priorityCombo);

        inputPanel.add(new JLabel("상태:"));
        statusCombo = new JComboBox<>(TodoVO.Status.values());
        statusCombo.setSelectedItem(this.todo.getStatus() != null ? this.todo.getStatus() : TodoVO.Status.진행중);  // 기본값 설정 가능
        inputPanel.add(statusCombo);

        inputPanel.add(new JLabel("카테고리:"));
        categoryField = new JTextField(this.todo.getCategory() != null ? this.todo.getCategory() : "");
        categoryField.setFont(font);
        inputPanel.add(categoryField);

        add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("저장");
        JButton cancelButton = new JButton("취소");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            if (saveTodo()) {
                dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            dispose();
        });
    }

    private boolean saveTodo() {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String dueDateStr = dueDateField.getText().trim();
        TodoVO.Priority priority = (TodoVO.Priority) priorityCombo.getSelectedItem();
        TodoVO.Status status = (TodoVO.Status) statusCombo.getSelectedItem();
        String category = categoryField.getText().trim();

        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "내용을 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        LocalDateTime dueDate = null;
        if (!dueDateStr.isEmpty()) {
            try {
                dueDate = LocalDateTime.parse(dueDateStr, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "마감기한은 yyyy-MM-dd HH:mm 형식이어야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        
        if (status == TodoVO.Status.완료 && dueDate != null && dueDate.isAfter(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(
                this,
                "마감기한이 현재 시각 이후인데 상태를 '완료'로 저장할 수 없습니다.\n" +
                "마감기한을 조정하거나 상태를 '진행중/보류'로 변경해주세요.",
                "입력 오류",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        todo.setTitle(title);
        todo.setDescription(description);
        todo.setDueDate(dueDate);
        todo.setPriority(priority);
        todo.setStatus(status);
        todo.setCategory(category.isEmpty() ? null : category);

        int result;
        if (isNew) {
            result = TodoDAOImple.getInstance().insertTodo(todo);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "추가 완료!", "성공", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "추가 실패.", "오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            result = TodoDAOImple.getInstance().updateTodo(todo);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "수정 완료!", "성공", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "수정 실패.", "오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }
}