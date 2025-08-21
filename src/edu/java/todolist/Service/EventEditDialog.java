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

import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

public class EventEditDialog extends JDialog {
    private EventVO event;
    private boolean isNew;

    private JTextField titleField;
    private JTextField descriptionField;
    private JTextField startDateField;
    private JTextField endDateField;
    private JComboBox<String> repeatTypeCombo;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public EventEditDialog(EventVO event, int userId) {
        if (event == null) {
            this.event = new EventVO();
            this.isNew = true;
            this.event.setUserId(userId);
        } else {
            this.event = event;
            this.isNew = false;
        }
        initUI();
    }
    
    private void initUI() {
        setTitle(isNew ? "일정 추가" : "일정 수정");
        setSize(400, 350);
        setLocationRelativeTo(null);
        setModal(true);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        Font font = new Font("맑은 고딕", Font.PLAIN, 14);

        inputPanel.add(new JLabel("제목:"));
        titleField = new JTextField(this.event.getTitle() != null ? this.event.getTitle() : "");
        titleField.setFont(font);
        inputPanel.add(titleField);

        inputPanel.add(new JLabel("설명:"));
        descriptionField = new JTextField(this.event.getDescription() != null ? this.event.getDescription() : "");
        descriptionField.setFont(font);
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("시작 (yyyy-MM-dd HH:mm):"));
        String startDateStr = this.event.getStartDate() != null ? this.event.getStartDate().format(DATE_TIME_FORMATTER) : "";
        startDateField = new JTextField(startDateStr);
        startDateField.setFont(font);
        inputPanel.add(startDateField);

        inputPanel.add(new JLabel("종료 (yyyy-MM-dd HH:mm):"));
        String endDateStr = this.event.getEndDate() != null ? this.event.getEndDate().format(DATE_TIME_FORMATTER) : "";
        endDateField = new JTextField(endDateStr);
        endDateField.setFont(font);
        inputPanel.add(endDateField);

        inputPanel.add(new JLabel("반복 유형:"));
        repeatTypeCombo = new JComboBox<>(new String[]{"없음", "매일", "매주", "매월"});
        repeatTypeCombo.setSelectedItem(this.event.getRepeatType() != null ? this.event.getRepeatType() : "없음");
        inputPanel.add(repeatTypeCombo);

        add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("저장");
        JButton cancelButton = new JButton("취소");

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            if (saveEvent()) {
                dispose();
            }
        });

        cancelButton.addActionListener(e -> dispose());
    }

    private boolean saveEvent() {
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String startDateStr = startDateField.getText().trim();
        String endDateStr = endDateField.getText().trim();
        String repeatType = (String) repeatTypeCombo.getSelectedItem();

        // 제목 체크
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "제목을 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 시작/종료 날짜 필수 체크
        if (startDateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "시작 날짜를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (endDateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "종료 날짜를 입력하세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        LocalDateTime startDate;
        LocalDateTime endDate;
        try {
            startDate = LocalDateTime.parse(startDateStr, DATE_TIME_FORMATTER);
            endDate = LocalDateTime.parse(endDateStr, DATE_TIME_FORMATTER);

            // 종료 날짜가 시작 날짜보다 이전이면 오류
            if (endDate.isBefore(startDate)) {
                JOptionPane.showMessageDialog(this, "종료 날짜는 시작 날짜보다 이전일 수 없습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "날짜는 yyyy-MM-dd HH:mm 형식이어야 합니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        event.setTitle(title);
        event.setDescription(description);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setRepeatType(repeatType);

        int result;
        if (isNew) {
            result = EventDAOImple.getInstance().insertEvent(event);
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "추가 완료!", "성공", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "추가 실패.", "오류", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            result = EventDAOImple.getInstance().updateEvent(event);
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
