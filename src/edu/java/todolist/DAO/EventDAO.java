package edu.java.todolist.DAO;

import java.time.LocalDate;
import java.util.List;
import edu.java.todolist.VO.EventVO;

public interface EventDAO {
	public abstract int insertEvent(EventVO event);
	
	public abstract List<EventVO> selectEventsByUserId(int userId);
	
	public abstract EventVO selectAllTodoByEventId(int eventId);

	public abstract List<EventVO> selectEventsByDate(int userId, LocalDate date);
	
	public abstract List<EventVO> selectEventsByDateRange(int userId, LocalDate startDate, LocalDate endDate);
	
	public abstract int updateEvent(EventVO event);

	public abstract int deleteEvent(int eventId);
}
