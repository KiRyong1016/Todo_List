package edu.java.todolist.DAOImple;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DBConnManager;
import edu.java.todolist.Info.EventTableInfo;
import edu.java.todolist.Info.MySQLConnInfo;
import edu.java.todolist.VO.EventVO;

public class EventDAOImple implements EventDAO, EventTableInfo, MySQLConnInfo {
	private static EventDAOImple instance = null;

	private EventDAOImple() {
	}

	public static EventDAOImple getInstance() {
		if (instance == null) {
			instance = new EventDAOImple();
		}
		return instance;
	}

	@Override
	public int insertEvent(EventVO event) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			if (event.getEndDate() == null || event.getEndDate() == null) {
	            throw new IllegalArgumentException("시작과 종료 날짜는 필수 입력입니다.");
	        }
			pstmt = conn.prepareStatement(SQL_INSERT);
			pstmt.setString(1, event.getTitle());
			pstmt.setString(2, event.getDescription());
			pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(event.getStartDate()));
			pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(event.getEndDate()));
			pstmt.setString(5, event.getRepeatType());
			pstmt.setInt(6, event.getUserId());

			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	@Override
	public List<EventVO> selectEventsByUserId(int userId) {
		List<EventVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_SELECT);
            pstmt.setInt(1, userId);

            rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(extractEventFromResultSet(rs));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return list;
	}
	
	@Override
	public EventVO selectAllTodoByEventId(int eventId) {
		Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        EventVO event = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_SELECT_BY_EVENT_ID);
            pstmt.setInt(1, eventId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                event = extractEventFromResultSet(rs);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return event;
	}
	
	@Override
	public List<EventVO> selectEventsByDate(int userId, LocalDate date) {
		List<EventVO> list = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			pstmt = conn.prepareStatement(SQL_SELECT_BY_DATE);
			pstmt.setInt(1, userId);
			pstmt.setDate(2, java.sql.Date.valueOf(date));
			rs = pstmt.executeQuery();

			while (rs.next()) {
				list.add(extractEventFromResultSet(rs));
			}
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt, rs);
		}
		return list;
	}

	@Override
	public List<EventVO> selectEventsByDateRange(int userId, LocalDate startDate, LocalDate endDate) {
	    List<EventVO> list = new ArrayList<>();
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;

	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");

	        java.time.LocalDateTime rangeStart = startDate.atStartOfDay();
	        java.time.LocalDateTime rangeEnd   = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

	        conn = DriverManager.getConnection(URL, USER, PASSWORD);
	        pstmt = conn.prepareStatement(SQL_SELECT_BY_DATE_RANGE);

	        pstmt.setInt(1, userId);
	        pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(rangeEnd));   // start_date <= ?
	        pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(rangeStart)); // COALESCE(end_date, start_date) >= ?

	        rs = pstmt.executeQuery();
	        while (rs.next()) {
	            list.add(extractEventFromResultSet(rs));
	        }
	    } catch (SQLException | ClassNotFoundException e) {
	        e.printStackTrace();
	    } finally {
	        DBConnManager.close(conn, pstmt, rs);
	    }
	    return list;
	}


	@Override
	public int updateEvent(EventVO event) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			if (event.getEndDate() == null || event.getEndDate() == null) {
	            throw new IllegalArgumentException("시작과 종료 날짜는 필수 입력입니다.");
	        }
			pstmt = conn.prepareStatement(SQL_UPDATE);
			pstmt.setString(1, event.getTitle());
			pstmt.setString(2, event.getDescription());
			pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(event.getStartDate()));
			pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(event.getEndDate()));
			pstmt.setString(5, event.getRepeatType());
			pstmt.setInt(6, event.getEventId());

			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	@Override
	public int deleteEvent(int eventId) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);

			pstmt = conn.prepareStatement(SQL_DELETE);
			pstmt.setInt(1, eventId);
			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	private EventVO extractEventFromResultSet(ResultSet rs) {
	    EventVO event = new EventVO();
	    try {
	        event.setEventId(rs.getInt("event_id"));
	        event.setTitle(rs.getString("title"));
	        event.setDescription(rs.getString("description"));

	        event.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
			event.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());

	        event.setRepeatType(rs.getString("repeat_type"));
	        event.setUserId(rs.getInt("user_id"));
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return event;
	}

}
