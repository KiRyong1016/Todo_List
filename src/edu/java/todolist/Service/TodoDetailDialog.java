package edu.java.todolist.Service;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;

public class TodoDetailDialog extends JDialog {
	private TodoPanel parent;
	private TodoVO todo;
	private int todoId;
	private int userId;
	
    public TodoDetailDialog(TodoPanel parent, int todoId, int userId) {
    	this.parent = parent;
    	this.todoId = todoId;
    	this.userId = userId;
    	this.todo = TodoDAOImple.getInstance().selectAllTodoByTodoId(todoId);  	
    	
        setTitle("할 일 상세 내용");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setModal(true);
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));        
        Font font = new Font("맑은 고딕", Font.PLAIN, 16);
        
        StringBuilder detail = new StringBuilder();        
        detail.append("<html><body style='font-family:맑은 고딕; font-size:12pt;'>");
        detail.append("<b>제목:</b> ").append(todo.getTitle()).append("<br><br>");
        detail.append("<b>내용:</b> ").append(todo.getDescription()).append("<br><br>");
        detail.append("<b>마감기한:</b> ").append(todo.getDueDate() != null ? todo.getDueDate().toString() : "없음").append("<br><br>");
        detail.append("<b>중요도:</b> ").append(todo.getPriority()).append("<br><br>");
        detail.append("<b>상태:</b> ").append(todo.getStatus()).append("<br><br>");
        detail.append("<b>카테고리:</b> ").append(todo.getCategory() != null ? todo.getCategory() : "없음").append("<br><br>");
        detail.append("</body></html>");
        
        JLabel detailLabel = new JLabel(detail.toString());
        detailLabel.setFont(font);
        
        contentPanel.add(detailLabel, BorderLayout.CENTER);
        
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
                TodoEditDialog editDialog = new TodoEditDialog(todo, userId);
                editDialog.setVisible(true);

                // 수정 다이얼로그가 닫힌 후에 (모달이라면 아래 코드가 실행됨)
                // 수정이 완료됐으면 상세 다이얼로그도 갱신 또는 닫기
                TodoVO updatedTodo = TodoDAOImple.getInstance().selectAllTodoByTodoId(todo.getTodoId());
                if (updatedTodo != null) {
                    todo = updatedTodo;
                    dispose();
                    new TodoDetailDialog(parent, todo.getTodoId(), todo.getUserId()).setVisible(true);
                    ((TodoPanel)parent).loadTodos();
                } else {
                    JOptionPane.showMessageDialog(null, "업데이트 후 데이터를 불러올 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    TodoDetailDialog.this,
                    "정말 삭제하시겠습니까?",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    int result = TodoDAOImple.getInstance().deleteTodo(todo.getTodoId());
                    if (result == 1) {
                        JOptionPane.showMessageDialog(TodoDetailDialog.this, "삭제 완료!");
                        dispose(); // 다이얼로그 닫기
                        ((TodoPanel)parent).loadTodos();
                    } else {
                        JOptionPane.showMessageDialog(TodoDetailDialog.this, "삭제 실패!", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}

