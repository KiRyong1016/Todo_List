package edu.java.todolist.Service;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

public class EventPanel extends JPanel {
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private int userId;
    private JComboBox<String> filterCombo;
    private List<EventVO> events = new ArrayList<>();
    private final String[] COLUMN_NAMES = {"시작일시", "종료일시", "내용", "반복", "설명"};
    private final EventDAO eventDAO = EventDAOImple.getInstance();
    
    public EventPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("일정", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // 필터 콤보박스
        String[] filterOptions = {
            "전체 조회",
            "시작일시별 정렬",
            "제목순 정렬",
            "반복주기별 정렬"
        };
        filterCombo = new JComboBox<>(filterOptions);
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        filterPanel.add(new JLabel("정렬 / 필터:"));
        filterPanel.add(filterCombo);
        add(filterPanel, BorderLayout.BEFORE_FIRST_LINE);

        // 테이블
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventTable = new JTable(tableModel);
        eventTable.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        eventTable.setRowHeight(30);
        add(new JScrollPane(eventTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("추가");
        addButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
	            EventEditDialog dialog = new EventEditDialog(null, userId);
	            dialog.setVisible(true);
	            loadEvents();
			}            
        });
        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 초기 로딩
        loadEvents();

        // 콤보박스 이벤트
        filterCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selected = (String) filterCombo.getSelectedItem();
                switch (selected) {
                    case "전체 조회": 
                    	loadEvents(); 
                    	break;
                    case "시작일시별 정렬": 
                    	loadEventsSortedByStart(); 
                    	break;
                    case "제목순 정렬": 
                    	loadEventsSortedByTitle(); 
                    	break;
                    case "반복주기별 정렬": 
                    	loadEventsSortedByRepeat(); 
                    	break;
                }
            }
        });

        // 더블클릭 시 상세보기
        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = eventTable.rowAtPoint(e.getPoint());
                if (row == -1 || events.isEmpty() || row >= events.size()) return;

                if (e.getClickCount() == 2) {
                    EventVO clickedEvent = events.get(row);
                    EventDetailDialog dialog = new EventDetailDialog(EventPanel.this, clickedEvent.getEventId(), clickedEvent.getUserId());
                    dialog.setVisible(true);
                }
            }
        });
    }

    void loadEvents() {
        events = eventDAO.selectEventsByUserId(userId);
        updateTable(events);
        for (EventVO event : events) {
            EventReminder.scheduleReminder(event);
        }

    }

    private void loadEventsSortedByStart() {
        events = eventDAO.selectEventsByUserId(userId);
        events.sort(Comparator.comparing(EventVO::getStartDate));
        updateTable(events);
        for (EventVO event : events) {
            EventReminder.scheduleReminder(event);
        }

    }

    private void loadEventsSortedByTitle() {
        events = eventDAO.selectEventsByUserId(userId);
        events.sort(Comparator.comparing(EventVO::getTitle, String.CASE_INSENSITIVE_ORDER));
        updateTable(events);
        for (EventVO event : events) {
            EventReminder.scheduleReminder(event);
        }

    }

    private void loadEventsSortedByRepeat() {
        events = eventDAO.selectEventsByUserId(userId);
        events.sort(Comparator.comparing(EventVO::getRepeatType, Comparator.nullsLast(String::compareTo)));
        updateTable(events);
        for (EventVO event : events) {
            EventReminder.scheduleReminder(event);
        }

    }

    private void updateTable(List<EventVO> events) {
        tableModel.setRowCount(0);
        for (EventVO e : events) {
            Object[] row = {
                e.getStartDate(),
                e.getEndDate(),
                e.getTitle(),
                e.getRepeatType(),
                e.getDescription()
            };
            tableModel.addRow(row);
        }
    }
}
