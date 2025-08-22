package edu.java.todolist.Service;

import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;
import edu.java.todolist.VO.TodoVO.Status;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * To-Do 탭 (페이징 + 다중 선택 삭제/완료 처리)
 * - 상단: 제목 + 정렬 콤보
 * - 중앙: To-Do 테이블
 * - 하단: 추가/수정/완료/삭제 + 페이지네이션(이전/현재/다음, 페이지 크기)
 */
public class TodoPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final int userId;

    // 테이블/모델
    private JTable todoTable;
    private DefaultTableModel tableModel;
    private final String[] COLUMN_NAMES = {"기한", "할일", "상세", "카테고리", "중요도", "상태"};

    // 정렬 콤보
    private JComboBox<String> sortCombo;

    // CRUD 버튼
    private JButton addButton;
    private JButton editButton;
    private JButton completeButton;
    private JButton deleteButton;

    // 페이징 상태
    private int page = 1;
    private int pageSize = 20;
    private int total = 0;
    private JLabel pageLabel;
    private JButton prevBtn;
    private JButton nextBtn;
    private JComboBox<Integer> pageSizeCombo;

    // 데이터
    private List<TodoVO> todos = new ArrayList<>();

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yy-MM-dd");

    public TodoPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());

        // 상단 타이틀
        JLabel titleLabel = new JLabel("To-Do", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // 상단 정렬 콤보
        String[] sortOptions = {
            "기한순 정렬",
            "중요도순 정렬",
            "상태순 정렬",
            "제목순 정렬"
        };
        sortCombo = new JComboBox<>(sortOptions);
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        sortPanel.add(new JLabel("정렬 / 필터:"));
        sortPanel.add(sortCombo);
        add(sortPanel, BorderLayout.BEFORE_FIRST_LINE);

        // 테이블
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        todoTable = new JTable(tableModel);
        todoTable.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        todoTable.setRowHeight(28);

        // ✅ 여러 항목 선택 허용 (Ctrl/Shift)
        todoTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        add(new JScrollPane(todoTable), BorderLayout.CENTER);

        // 하단 버튼 + 페이지네이션
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        addButton = new JButton("추가");
        editButton = new JButton("수정");
        completeButton = new JButton("완료");
        deleteButton = new JButton("삭제");

        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEditSelected());
        completeButton.addActionListener(e -> onCompleteSelectedMulti()); // ✅ 다중 완료
        deleteButton.addActionListener(e -> onDeleteSelectedMulti());     // ✅ 다중 삭제

        bottomPanel.add(addButton);
        bottomPanel.add(editButton);
        bottomPanel.add(completeButton);
        bottomPanel.add(deleteButton);

        // 페이지네이션 컨트롤
        prevBtn = new JButton("이전");
        nextBtn = new JButton("다음");
        pageLabel = new JLabel("1 / 1");
        pageSizeCombo = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        pageSizeCombo.setSelectedItem(pageSize);

        prevBtn.addActionListener(e -> {
            if (page > 1) {
                page--;
                loadTodosPaged();
            }
        });
        nextBtn.addActionListener(e -> {
            int totalPages = Math.max(1, (int)Math.ceil(total / (double)pageSize));
            if (page < totalPages) {
                page++;
                loadTodosPaged();
            }
        });
        pageSizeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                pageSize = (Integer) pageSizeCombo.getSelectedItem();
                page = 1;
                loadTodosPaged();
            }
        });

        bottomPanel.add(prevBtn);
        bottomPanel.add(pageLabel);
        bottomPanel.add(nextBtn);
        bottomPanel.add(new JLabel(" / 페이지 크기:"));
        bottomPanel.add(pageSizeCombo);

        add(bottomPanel, BorderLayout.SOUTH);

        // 초기 로드
        loadTodosPaged();

        // 콤보 변경 시 첫 페이지로 재조회
        sortCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                page = 1;
                loadTodosPaged();
            }
        });

        // 더블클릭 상세 보기
        todoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = todoTable.rowAtPoint(e.getPoint());
                if (row == -1 || todos.isEmpty() || row >= todos.size()) return;

                if (e.getClickCount() == 2) {
                    TodoVO clicked = todos.get(row);
                    TodoDetailDialog dialog = new TodoDetailDialog(TodoPanel.this, clicked.getTodoId(), clicked.getUserId());
                    dialog.setVisible(true);
                    // 상세에서 수정/삭제했을 수 있으니 재조회
                    loadTodosPaged();
                }
            }
        });
    }

    /** 현재 페이지/정렬 상태로 재조회 */
    private void loadTodosPaged() {
        // 정렬 매핑
        String selected = (String) sortCombo.getSelectedItem();
        String orderBy = "due"; // due/priority/status/title
        boolean asc = true;

        if ("기한순 정렬".equals(selected))        { orderBy = "due";      asc = true; }
        else if ("중요도순 정렬".equals(selected)) { orderBy = "priority"; asc = true; }
        else if ("상태순 정렬".equals(selected))   { orderBy = "status";   asc = true; }
        else if ("제목순 정렬".equals(selected))   { orderBy = "title";    asc = true; }
        else { orderBy = "due"; asc = true; }

        boolean onlyNotDone = false; // 필요하면 상단 토글을 붙여 true로 전달

        // 총 개수 및 페이지 보정
        total = TodoDAOImple.getInstance().countTodosByUserId(userId, onlyNotDone);
        int totalPages = Math.max(1, (int)Math.ceil(total / (double)pageSize));
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * pageSize;

        todos = TodoDAOImple.getInstance()
                .selectTodosByUserIdPaged(userId, offset, pageSize, orderBy, asc, onlyNotDone);

        updateTable(todos);

        pageLabel.setText(page + " / " + totalPages);
        prevBtn.setEnabled(page > 1);
        nextBtn.setEnabled(page < totalPages);
    }

    private void updateTable(List<TodoVO> list) {
        tableModel.setRowCount(0);
        for (TodoVO t : list) {
            String dueStr = (t.getDueDate() == null) ? "기한 없음" : t.getDueDate().format(DATE_FMT);
            Object[] row = {
                safe(dueStr),
                t.getTitle(),
                t.getDescription() == null ? "" : t.getDescription(),
                t.getCategory() == null ? "" : t.getCategory(),		
                t.getPriority() == null ? "" : t.getPriority().toString(),
                t.getStatus()   == null ? "" : t.getStatus().toString()                
            };
            tableModel.addRow(row);
        }
    }

    // === CRUD 동작 ===
    private void onAdd() {
        try {
            TodoEditDialog dialog = new TodoEditDialog(new TodoVO(), userId);
            dialog.setVisible(true);
            loadTodosPaged();
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "추가 창을 여는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEditSelected() {
        int row = todoTable.getSelectedRow();
        if (row < 0 || row >= todos.size()) {
            JOptionPane.showMessageDialog(this, "수정할 항목을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        TodoVO sel = todos.get(row);
        try {
            TodoEditDialog dialog = new TodoEditDialog(sel, userId);
            dialog.setVisible(true);
            loadTodosPaged();
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "수정 창을 여는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** ✅ 다중 완료 처리 */
    private void onCompleteSelectedMulti() {
        int[] viewRows = todoTable.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "완료 처리할 항목을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<TodoVO> targets = new ArrayList<>();
        for (int viewRow : viewRows) {
            int modelRow = todoTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < todos.size()) {
                targets.add(todos.get(modelRow));
            }
        }
        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "선택 항목을 확인할 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String msg = (targets.size() == 1)
                ? "해당 항목을 완료로 변경하시겠습니까?"
                : "선택한 " + targets.size() + "개 항목을 완료로 변경하시겠습니까?";
        int confirm = JOptionPane.showConfirmDialog(this, msg, "완료 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int ok = 0, already = 0, fail = 0;
        for (TodoVO t : targets) {
            try {
                if (t.getStatus() == Status.완료) { already++; continue; }
                t.setStatus(Status.완료);
                int r = TodoDAOImple.getInstance().updateTodo(t);
                if (r == 1) ok++; else fail++;
            } catch (Exception ex) {
                fail++;
            }
        }

        JOptionPane.showMessageDialog(
            this,
            "완료 처리 결과: 성공 " + ok + "개"
                + (already > 0 ? ", 이미 완료 " + already + "개" : "")
                + (fail > 0 ? ", 실패 " + fail + "개" : ""),
            (fail == 0 ? "완료" : "완료 결과"),
            (fail == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE)
        );

        loadTodosPaged();
    }

    /** ✅ 다중 삭제 */
    private void onDeleteSelectedMulti() {
        int[] viewRows = todoTable.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택하세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (int viewRow : viewRows) {
            int modelRow = todoTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < todos.size()) {
                ids.add(todos.get(modelRow).getTodoId());
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
                int r = TodoDAOImple.getInstance().deleteTodo(id);
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
        loadTodosPaged();
    }

    // === 하위 호환/외부 갱신용 래퍼 ===
    /** 기존 코드 호환용: 기존 Dialog 등에서 parent.loadTodos()를 호출해도 동작하도록 */
    public void loadTodos() {
        loadTodosPaged();
    }

    // 유틸
    private static String safe(String s) { return (s == null) ? "" : s; }
}
