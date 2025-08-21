package edu.java.todolist.Info;

public interface TodoTableInfo {

	public static final String TABLE_NAME = "todo";
	public static final String COL_TODO_ID = "todo_id";
	public static final String COL_TITLE = "title";
	public static final String COL_DESCRIPTION = "description";
	public static final String COL_PRIORITY = "priority";
	public static final String COL_STATUS = "status";
	public static final String COL_CATEGORY = "category";
	public static final String COL_USER_ID = "user_id";

	// todo 추가
	public static final String SQL_INSERT = "INSERT INTO todo (title, description, due_date, priority, status, category, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

	// 전체 todo 조회
	public static final String SQL_SELECT = "SELECT * FROM todo WHERE user_id = ? order by due_date ASC";
	
	// 특정 todo 조회
	public static final String SQL_SELECT_BY_TODO_ID = "SELECT * FROM todo WHERE todo_id = ?";
	
	// 특정 날짜 todo 조회
	public static final String SQL_SELECT_BY_DUEDATE = "SELECT * FROM todo WHERE user_id = ? AND due_date >= ? AND due_date < ? ORDER BY FIELD(priority, '상', '중', '하'), due_date ASC";

	// 날짜 범위로 일정 조회
	public static final String SQL_SELECT_BY_DATE_RANGE = "SELECT * FROM todo WHERE user_id = ? AND due_date >= ? AND due_date <= ?";

	// todo 수정
	public static final String SQL_UPDATE = "UPDATE todo SET title = ?, description = ?, due_date = ?, priority = ?, status = ?, category = ? WHERE todo_id = ?";

	// todo 삭제
	public static final String SQL_DELETE = "DELETE FROM todo WHERE todo_id = ?";

}
