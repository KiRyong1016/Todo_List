package edu.java.todolist.Service;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

public class EventDetailDialog extends JDialog {
	private EventPanel parent;
    private EventVO event;
	private final int eventId;
    private final int userId;
    private final EventDAO eventDAO = EventDAOImple.getInstance();
    
    private JLabel titleLabel;
    private JLabel descriptionLabel;
    private JLabel startDateLabel;
    private JLabel endDateLabel;
    private JLabel repeatTypeLabel;

    public EventDetailDialog(EventPanel parent, int eventId, int userId) {
        this.parent = parent;
        this.eventId = eventId;
        this.userId = userId;
        this.event = eventDAO.selectAllTodoByEventId(eventId);

        setTitle("일정 상세 보기");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setModal(true);
        initUI();
    }
    
    private void initUI() {
        JPanel contentPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        titleLabel = new JLabel("제목: " + (event.getTitle() != null ? event.getTitle() : ""));
        descriptionLabel = new JLabel("설명: " + (event.getDescription() != null ? event.getDescription() : ""));
        startDateLabel = new JLabel("시작: " + (event.getStartDate() != null ? event.getStartDate() : ""));
        endDateLabel = new JLabel("종료: " + (event.getEndDate() != null ? event.getEndDate() : ""));
        repeatTypeLabel = new JLabel("반복: " + (event.getRepeatType() != null ? event.getRepeatType() : ""));

        Font font = new Font("맑은 고딕", Font.PLAIN, 16);
        titleLabel.setFont(font);
        descriptionLabel.setFont(font);
        startDateLabel.setFont(font);
        endDateLabel.setFont(font);
        repeatTypeLabel.setFont(font);

        contentPanel.add(titleLabel);
        contentPanel.add(descriptionLabel);
        contentPanel.add(startDateLabel);
        contentPanel.add(endDateLabel);
        contentPanel.add(repeatTypeLabel);

        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 수정 다이얼로그를 새 창으로 열기
                EventEditDialog editDialog = new EventEditDialog(event, userId);
                editDialog.setVisible(true);

                // 수정 다이얼로그가 닫힌 후에 (모달이라면 아래 코드가 실행됨)
                // 수정이 완료됐으면 상세 다이얼로그도 갱신 또는 닫기
                EventVO updatedTodo = eventDAO.selectAllTodoByEventId(event.getEventId());
                if (updatedTodo != null) {
                    event = updatedTodo;
                    dispose();
                    new EventDetailDialog(parent, event.getEventId(), event.getUserId()).setVisible(true);
                    ((EventPanel)parent).loadEvents();
                } else {
                    JOptionPane.showMessageDialog(null, "업데이트 후 데이터를 불러올 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    EventDetailDialog.this,
                    "정말 삭제하시겠습니까?",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    int result = eventDAO.deleteEvent(event.getEventId());
                    if (result == 1) {
                        JOptionPane.showMessageDialog(EventDetailDialog.this, "삭제 완료!");
                        dispose(); // 다이얼로그 닫기
                        ((EventPanel)parent).loadEvents();
                    } else {
                        JOptionPane.showMessageDialog(EventDetailDialog.this, "삭제 실패!", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}