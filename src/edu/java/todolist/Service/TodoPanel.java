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

import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;

public class TodoPanel extends JPanel {
    private JTable todoTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private int userId;
    private JComboBox<String> filterCombo;
    private List<TodoVO> todos = new ArrayList<TodoVO>();
    private final String[] COLUMN_NAMES = {"기한", "내용", "중요도", "상태", "카테고리"};

    public TodoPanel(int userId) {
        this.userId = userId;        
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("ToDo", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // 필터 콤보박스 (하나만)
        String[] filterOptions = {
            "전체 조회",
            "중요도별 정렬",
            "상태별 정렬",
            "카테고리별 정렬"
        };
        filterCombo = new JComboBox<>(filterOptions);
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        filterPanel.add(new JLabel("정렬 / 필터:"));
        filterPanel.add(filterCombo);
        add(filterPanel, BorderLayout.BEFORE_FIRST_LINE);

        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 불가
            }
        };
      
        todoTable = new JTable(tableModel);
        todoTable.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        todoTable.setRowHeight(30);
        add(new JScrollPane(todoTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        addButton = new JButton("추가");
        addButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
		        TodoEditDialog dialog = new TodoEditDialog(null, userId);  // null이면 새로 추가
		        dialog.setVisible(true);

		        // 다이얼로그 닫힌 후 갱신
		        loadTodos();
		    }
		});
        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 초기 전체 로딩
        loadTodos();

        // 콤보박스 선택에 따른 필터/정렬 처리
        filterCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selected = (String) filterCombo.getSelectedItem();
                switch (selected) {
                    case "전체 조회":
                        loadTodos();
                        break;
                    case "중요도별 정렬":
                        loadTodosSortedByPriority();
                        break;
                    case "상태별 정렬":
                        loadTodosSortedByStatus();
                        break;
                    case "카테고리별 정렬":
                        loadTodosSortedByCategory();
                        break;
                }
            }
        });
        
        todoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = todoTable.rowAtPoint(e.getPoint());
                if (row == -1 || todos == null || todos.isEmpty() || row >= todos.size()) return;
                
                if (e.getClickCount() == 2) {
	                TodoVO clickedTodo = todos.get(row);
	                TodoDetailDialog dialog = new TodoDetailDialog(TodoPanel.this, clickedTodo.getTodoId(), clickedTodo.getUserId());
	                dialog.setVisible(true);
                }
            }
        });


    }

    void loadTodos() {
        todos = TodoDAOImple.getInstance().selectAllTodosByUserId(userId);
        updateTable(todos);
        System.out.println(todos.size());
    }

    private void loadTodosSortedByPriority() {
        todos = TodoDAOImple.getInstance().selectAllTodosByUserId(userId);
        todos.sort(Comparator.comparing(TodoVO::getPriority));
        updateTable(todos);
    }

    private void loadTodosSortedByStatus() {
        todos = TodoDAOImple.getInstance().selectAllTodosByUserId(userId);
        todos.sort(Comparator.comparing(TodoVO::getStatus));
        updateTable(todos);
    }

    private void loadTodosSortedByCategory() {
        todos = TodoDAOImple.getInstance().selectAllTodosByUserId(userId);
        todos.sort(Comparator.comparing(TodoVO::getCategory, Comparator.nullsLast(String::compareTo)));
        updateTable(todos);
    }

    private void updateTable(List<TodoVO> todos) {
        tableModel.setRowCount(0);
        for (TodoVO todo : todos) {
            Object[] rowData = {
                todo.getDueDate(),
                todo.getDescription(),
                todo.getPriority(),
                todo.getStatus(),
                todo.getCategory()
            };
            tableModel.addRow(rowData);
        }
    }
}
