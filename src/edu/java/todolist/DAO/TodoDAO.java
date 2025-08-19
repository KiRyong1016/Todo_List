package edu.java.todolist.DAO;

import java.time.LocalDate;
import java.util.List;
import edu.java.todolist.VO.TodoVO;

public interface TodoDAO {

	public abstract int insertTodo(TodoVO todo);

    public abstract List<TodoVO> selectAllTodosByUserId(int userId);
    
    public abstract TodoVO selectAllTodoByTodoId(int todoId);
    
    public abstract List<TodoVO> selectTodosByDueDate(int userId, LocalDate dueDate); // 날짜별 검색
    
    public abstract List<TodoVO> selectTodosByDueDateRange(int userId, LocalDate startDate, LocalDate endDate);

    public abstract int updateTodo(TodoVO todo);

    public abstract int deleteTodo(int todoId);
}