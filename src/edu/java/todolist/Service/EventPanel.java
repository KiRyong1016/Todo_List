package edu.java.todolist.Service;

import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 일정 탭 (페이징 + 다중 삭제 + 수정)
 * - 상단: 제목 + 정렬/필터 콤보
 * - 중앙: 일정 테이블
 * - 하단: 추가 / 수정 / 삭제(다중) + 페이지네이션(이전/현재/다음, 페이지 크기)
 */
public class EventPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final int userId;

    private JTable eventTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JComboBox<String> filterCombo;

    // 페이징 상태
    private int page = 1;
    private int pageSize = 20;
    private int total = 0;
    private JLabel pageLabel;
    private JButton prevBtn;
    private JButton nextBtn;
    private JComboBox<Integer> pageSizeCombo;

    private List<EventVO> events = new ArrayList<>();
    private static final String[] COLUMN_NAMES = {"시작일시", "종료일시", "내용", "반복", "설명"};

    public EventPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());

        // 상단 타이틀
        JLabel titleLabel = new JLabel("일정", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // 상단 정렬/필터 바
        String[] filterOptions = {
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
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        eventTable = new JTable(tableModel);
        eventTable.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        eventTable.setRowHeight(30);

        // ✅ 여러 항목 선택 허용
        eventTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        add(new JScrollPane(eventTable), BorderLayout.CENTER);

        // 하단: 추가 / 수정 / 삭제 + 페이지네이션
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        addButton = new JButton("추가");
        addButton.addActionListener(e -> {
            EventEditDialog dialog = new EventEditDialog(null, userId);
            dialog.setVisible(true);
            loadEventsPaged(); // 저장 후 현재 페이지 갱신
        });
        buttonPanel.add(addButton);

        editButton = new JButton("수정");
        editButton.addActionListener(e -> onEditSelected());
        buttonPanel.add(editButton);

        deleteButton = new JButton("삭제");
        deleteButton.addActionListener(e -> onDeleteSelectedMulti());
        buttonPanel.add(deleteButton);

        prevBtn = new JButton("이전");
        nextBtn = new JButton("다음");
        pageLabel = new JLabel("1 / 1");
        pageSizeCombo = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        pageSizeCombo.setSelectedItem(pageSize);

        prevBtn.addActionListener(e -> {
            if (page > 1) {
                page--;
                loadEventsPaged();
            }
        });
        nextBtn.addActionListener(e -> {
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            if (page < totalPages) {
                page++;
                loadEventsPaged();
            }
        });
        pageSizeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                pageSize = (Integer) pageSizeCombo.getSelectedItem();
                page = 1; // 페이지 크기 바꾸면 첫 페이지로
                loadEventsPaged();
            }
        });

        buttonPanel.add(prevBtn);
        buttonPanel.add(pageLabel);
        buttonPanel.add(nextBtn);
        buttonPanel.add(new JLabel(" / 페이지 크기:"));
        buttonPanel.add(pageSizeCombo);

        add(buttonPanel, BorderLayout.SOUTH);

        // 초기 로딩
        loadEventsPaged();

        // 정렬 콤보 변경 시 재조회
        filterCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                page = 1;
                loadEventsPaged();
            }
        });

        // 더블클릭 상세
        eventTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = eventTable.rowAtPoint(e.getPoint());
                if (row == -1 || events.isEmpty() || row >= events.size()) return;

                if (e.getClickCount() == 2) {
                    EventVO clickedEvent = events.get(row);
                    EventDetailDialog dialog = new EventDetailDialog(EventPanel.this, clickedEvent.getEventId(), clickedEvent.getUserId());
                    dialog.setVisible(true);
                    // 상세에서 삭제/수정했을 수 있으니 재조회
                    loadEventsPaged();
                }
            }
        });
    }

    /** 현재 페이징/정렬 상태에 맞춰 DB 재조회 */
    private void loadEventsPaged() {
        // 정렬 매핑
        String selected = (String) filterCombo.getSelectedItem();
        String orderBy = "start"; boolean asc = true;
        if ("시작일시별 정렬".equals(selected))      { orderBy = "start";  asc = true; }
        else if ("제목순 정렬".equals(selected))     { orderBy = "title";  asc = true; }
        else if ("반복주기별 정렬".equals(selected)) { orderBy = "repeat"; asc = true; }
        else { orderBy = "start"; asc = true; } // "전체 조회"도 시작일시 정렬로

        // 총 개수 + 현재 페이지 보정
        total = EventDAOImple.getInstance().countEventsByUserId(userId);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * pageSize;
        events = EventDAOImple.getInstance()
                .selectEventsByUserIdPaged(userId, offset, pageSize, orderBy, asc);

        updateTable(events);

        // (선택) 현재 페이지의 이벤트들만 알림 스케줄
        for (EventVO ev : events) {
            EventReminder.scheduleReminder(ev);
        }

        // 페이지 컨트롤 상태 업데이트
        pageLabel.setText(page + " / " + totalPages);
        prevBtn.setEnabled(page > 1);
        nextBtn.setEnabled(page < totalPages);
    }

    private void updateTable(List<EventVO> list) {
        tableModel.setRowCount(0);
        for (EventVO e : list) {
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

    // === 수정 ===
    private void onEditSelected() {
        int[] selected = eventTable.getSelectedRows();
        if (selected == null || selected.length == 0) {
            JOptionPane.showMessageDialog(this, "수정할 항목을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (selected.length > 1) {
            JOptionPane.showMessageDialog(this, "수정은 한 번에 하나만 가능합니다.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int viewRow = selected[0];
        int modelRow = eventTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= events.size()) {
            JOptionPane.showMessageDialog(this, "선택 항목을 확인할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        EventVO sel = events.get(modelRow);
        try {
            EventEditDialog dialog = new EventEditDialog(sel, userId);
            dialog.setVisible(true);
            loadEventsPaged();
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "수정 창을 여는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // === 다중 삭제 ===
    private void onDeleteSelectedMulti() {
        int[] viewRows = eventTable.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (int viewRow : viewRows) {
            int modelRow = eventTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < events.size()) {
                ids.add(events.get(modelRow).getEventId());
            }
        }
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "선택 항목을 확인할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String msg = (ids.size() == 1)
                ? "정말 삭제하시겠습니까?"
                : "선택한 " + ids.size() + "개 항목을 삭제하시겠습니까?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int ok = 0, fail = 0;
        for (Integer id : ids) {
            try {
                int r = EventDAOImple.getInstance().deleteEvent(id);
                if (r == 1) ok++; else fail++;
            } catch (Exception ex) {
                fail++;
            }
        }

        if (fail == 0) {
            JOptionPane.showMessageDialog(this, ok + "개 항목 삭제 완료!");
        } else {
            JOptionPane.showMessageDialog(
                this,
                ok + "개 성공, " + fail + "개 실패했습니다.",
                "삭제 결과",
                JOptionPane.WARNING_MESSAGE
            );
        }
        loadEventsPaged();
    }

    // === 하위 호환/외부 갱신용 래퍼 ===
    public void loadEvents() {
        loadEventsPaged();
    }
    public void loadEventsSortedByStart() {
        page = 1;
        if (filterCombo != null) filterCombo.setSelectedItem("시작일시별 정렬");
        loadEventsPaged();
    }
    public void loadEventsSortedByTitle() {
        page = 1;
        if (filterCombo != null) filterCombo.setSelectedItem("제목순 정렬");
        loadEventsPaged();
    }
    public void loadEventsSortedByRepeat() {
        page = 1;
        if (filterCombo != null) filterCombo.setSelectedItem("반복주기별 정렬");
        loadEventsPaged();
    }
}
