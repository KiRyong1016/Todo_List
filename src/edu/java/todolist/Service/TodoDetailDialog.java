package edu.java.todolist.Service;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.java.todolist.DAO.TodoDAO;
import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;

public class TodoDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final TodoPanel parent;
    private final TodoDAO todoDAO = TodoDAOImple.getInstance();
    private final int todoId;
    private final int userId;

    private TodoVO todo;

    // 중앙 표시부를 JTextPane(HTML)로 변경
    private final JTextPane detailPane = new JTextPane();
    private final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TodoDetailDialog(TodoPanel parent, int todoId, int userId) {
        super(SwingUtilities.getWindowAncestor(parent) instanceof java.awt.Frame
                ? (java.awt.Frame) SwingUtilities.getWindowAncestor(parent) : null,
                "할 일 상세 내용", true);

        this.parent = parent;
        this.todoId = todoId;
        this.userId = userId;

        setSize(420, 340);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        // ===== 내용 영역 (스크롤 가능) =====
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        detailPane.setContentType("text/html");
        detailPane.setEditable(false);
        // HTML에도 폰트 적용
        detailPane.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        detailPane.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        detailPane.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(
                detailPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(scroll, BorderLayout.CENTER);

        // ===== 버튼 영역 =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // 데이터 로드
        reload();

        // 이벤트
        editButton.addActionListener(e -> onEdit());
        deleteButton.addActionListener(e -> onDelete());
    }

    /** DB에서 다시 읽고 상세 HTML 갱신 */
    private void reload() {
        try {
            todo = todoDAO.selectAllTodoByTodoId(todoId);
            if (todo == null) {
                JOptionPane.showMessageDialog(this, "작업을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            if (todo.getUserId() != userId) {
                JOptionPane.showMessageDialog(this, "권한이 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            detailPane.setText(buildDetailHtml(todo));
            detailPane.setCaretPosition(0); // 스크롤 맨 위로
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터를 불러오는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /** HTML 상세 문자열 구성 (긴 내용 줄바꿈/래핑 포함) */
    private String buildDetailHtml(TodoVO t) {
        String due = (t.getDueDate() != null) ? formatDateTime(t.getDueDate()) : "없음";
        String title = safe(t.getTitle());
        String desc  = safe(t.getDescription());
        String cat   = (t.getCategory() != null) ? t.getCategory() : "없음";
        String prio  = (t.getPriority() != null) ? t.getPriority().toString() : "없음";
        String stat  = (t.getStatus()   != null) ? t.getStatus().toString()   : "없음";

        // 긴 단어/문장도 줄바꿈되도록 CSS 적용
        return """
            <html>
            <body style="font-family: '맑은 고딕', Malgun Gothic, sans-serif; font-size: 13pt; line-height: 1.5;
                         overflow-wrap: break-word; word-wrap: break-word; word-break: break-word; white-space: normal;">
              <div style="margin-bottom:8px;"><b>기한:</b> %s</div>
              <div style="margin-bottom:8px;"><b>할일:</b> %s</div>
              <div style="margin-bottom:8px;"><b>상세:</b><br>%s</div>
              <div style="margin-bottom:8px;"><b>카테고리:</b> %s</div>
              <div style="margin-bottom:8px;"><b>중요도:</b> %s</div>
              <div style="margin-bottom:8px;"><b>상태:</b> %s</div>
            </body>
            </html>
        """.formatted(
                escape(due),
                escape(title),
                nl2br(escape(desc)),
                escape(cat),
                escape(prio),
                escape(stat)
        );
    }

    private String formatDateTime(LocalDateTime ldt) {
        return ldt.format(YMDHM);
    }

    /** 수정 버튼 핸들러 */
    private void onEdit() {
        try {
            // 기존 시그니처 유지
            TodoEditDialog editDialog = new TodoEditDialog(todo, userId);
            editDialog.setVisible(true);

            // 저장 후 재조회 → 상세만 갱신
            TodoVO updated = todoDAO.selectAllTodoByTodoId(todoId);
            if (updated != null) {
                this.todo = updated;
                detailPane.setText(buildDetailHtml(todo));
                detailPane.setCaretPosition(0);
                if (parent != null) parent.loadTodos();
            } else {
                JOptionPane.showMessageDialog(this, "업데이트 후 데이터를 불러올 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "수정 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 삭제 버튼 핸들러 */
    private void onDelete() {
        int confirm = JOptionPane.showConfirmDialog(
                this, "정말 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            int result = todoDAO.deleteTodo(todo.getTodoId());
            if (result == 1) {
                JOptionPane.showMessageDialog(this, "삭제 완료!");
                if (parent != null) parent.loadTodos();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "삭제에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "삭제 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 유틸
    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "없음" : s;
    }
    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String nl2br(String s) {
        return s.replace("\r\n","<br>").replace("\n","<br>");
    }
}
