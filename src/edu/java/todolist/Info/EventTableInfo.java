package edu.java.todolist.Info;

public interface EventTableInfo {
	public static final String TABLE_NAME = "event";
	public static final String COL_EVENT_ID = "event_id";
	public static final String COL_TITLE = "title";
	public static final String COL_DESCRIPTION = "description";
	public static final String COL_START_DATE = "start_date";
	public static final String COL_END_DATE = "end_date";
	public static final String COL_REPEAT_TYPE = "repeat_type";
	public static final String COL_USER_ID = "user_id";
	
	// 일정 추가
	public static final String SQL_INSERT = "INSERT INTO event (title, description, start_date, end_date, repeat_type, user_id) VALUES (?, ?, ?, ?, ?, ?)";
	
	// 전체 일정 조회
	public static final String SQL_SELECT = "SELECT * FROM event WHERE user_id = ? ORDER BY start_date";
	
	// 특정 일정 조회
	public static final String SQL_SELECT_BY_EVENT_ID = "SELECT * FROM event WHERE event_id = ?";
	
	// 특정 날짜 일정 조회
	public static final String SQL_SELECT_BY_DATE = "SELECT * FROM event WHERE user_id = ? AND DATE(start_date) = ? ORDER BY start_date";
	
	// 반복 일정 조회
	public static final String SQL_SELECT_REPEATING_MASTERS = "SELECT * FROM event WHERE user_id = ? AND repeat_type IS NOT NULL AND TRIM(repeat_type) <> ''";
	
	// 날짜 범위로 일정 조회
	public static final String SQL_SELECT_BY_DATE_RANGE = "SELECT * FROM event WHERE user_id = ? AND DATE(start_date) BETWEEN ? AND ? ORDER BY start_date";
	
	public static final String SQL_COUNT_BY_USER = "SELECT COUNT(*) FROM event WHERE user_id = ?";
	// 일정 수정
	public static final String SQL_UPDATE = "UPDATE event SET title = ?, description = ?, start_date = ?, end_date = ?, repeat_type = ? WHERE event_id = ?";
	
	// 일정 삭제
	public static final String SQL_DELETE = "DELETE FROM event WHERE event_id = ?";

}
