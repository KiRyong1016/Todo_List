package edu.java.todolist.Service;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

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
    private final JLabel detailLabel = new JLabel();

    public TodoDetailDialog(TodoPanel parent, int todoId, int userId) {
        // 부모 윈도우 기준 모달
        super(SwingUtilities.getWindowAncestor(parent) instanceof java.awt.Frame
                ? (java.awt.Frame) SwingUtilities.getWindowAncestor(parent) : null,
              "할 일 상세 내용", true);

        this.parent = parent;
        this.todoId = todoId;
        this.userId = userId;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ===== 내용 영역 =====
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        detailLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        detailLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        contentPanel.add(detailLabel, BorderLayout.CENTER);

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

    /** DB에서 다시 읽고 라벨 갱신 */
    private void reload() {
        try {
            todo = todoDAO.selectAllTodoByTodoId(todoId);
            if (todo == null) {
                JOptionPane.showMessageDialog(this, "작업을 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            // 권한 가드
            if (todo.getUserId() != userId) {
                JOptionPane.showMessageDialog(this, "권한이 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            updateDetailLabel();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터를 불러오는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /** 현재 todo로 상세 HTML 갱신 */
    private void updateDetailLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:맑은 고딕; font-size:12pt;'>");
        sb.append("<b>제목:</b> ").append(safe(todo.getTitle())).append("<br><br>");
        sb.append("<b>내용:</b> ").append(safe(todo.getDescription())).append("<br><br>");
        sb.append("<b>마감기한:</b> ")
          .append(todo.getDueDate() != null ? todo.getDueDate().toString() : "없음")
          .append("<br><br>");
        sb.append("<b>중요도:</b> ").append(todo.getPriority() != null ? todo.getPriority() : "없음").append("<br><br>");
        sb.append("<b>상태:</b> ").append(todo.getStatus() != null ? todo.getStatus() : "없음").append("<br><br>");
        sb.append("<b>카테고리:</b> ").append(todo.getCategory() != null ? todo.getCategory() : "없음").append("<br><br>");
        sb.append("</body></html>");
        detailLabel.setText(sb.toString());
    }

    /** 수정 버튼 핸들러 */
    private void onEdit() {
        try {
            // 네 프로젝트 시그니처 유지
            TodoEditDialog editDialog = new TodoEditDialog(todo, userId);
            editDialog.setVisible(true);

            // 저장 후 재조회→라벨만 갱신(다이얼로그 재오픈 제거)
            TodoVO updated = todoDAO.selectAllTodoByTodoId(todoId);
            if (updated != null) {
                this.todo = updated;
                updateDetailLabel();
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

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "없음" : s;
    }
}
