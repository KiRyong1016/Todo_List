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
	public List<EventVO> selectEventsByDateRange(int userId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
	    List<EventVO> result = new java.util.ArrayList<>();

	    java.sql.Connection conn = null;
	    java.sql.PreparedStatement psOverlap = null;
	    java.sql.PreparedStatement psRepeat  = null;
	    java.sql.ResultSet rs = null;

	    // 하루 경계 (포함 범위): [00:00:00 .. 23:59:59]
	    java.time.LocalDateTime rangeStart = startDate.atStartOfDay();
	    java.time.LocalDateTime rangeEnd   = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        conn = java.sql.DriverManager.getConnection(URL, USER, PASSWORD);

	        // 2-1) 겹침(Overlap) 조회: 비반복만 결과에 담는다
	        psOverlap = conn.prepareStatement(SQL_SELECT_BY_DATE_RANGE);
	        psOverlap.setInt(1, userId);
	        psOverlap.setTimestamp(2, java.sql.Timestamp.valueOf(rangeEnd));
	        psOverlap.setTimestamp(3, java.sql.Timestamp.valueOf(rangeStart));
	        rs = psOverlap.executeQuery();

	        while (rs.next()) {
	            EventVO ev = extractEventFromResultSet(rs);
	            if (!isRepeating(ev)) { // 반복 마스터는 여기서 제외 (전개에서 추가됨)
	                result.add(ev);
	            }
	        }
	        // 다음 쿼리를 위해 ResultSet 정리
	        if (rs != null) { try { rs.close(); } catch (Exception ignore) {} rs = null; }

	        // 2-2) 반복 마스터를 가져와 기간 내 occurrence로 전개
	        psRepeat = conn.prepareStatement(SQL_SELECT_REPEATING_MASTERS);
	        psRepeat.setInt(1, userId);
	        psRepeat.setTimestamp(2, java.sql.Timestamp.valueOf(rangeEnd));
	        rs = psRepeat.executeQuery();

	        while (rs.next()) {
	            EventVO master = extractEventFromResultSet(rs);
	            // 반복 타입 정규화/검증
	            String type = normalizedRepeatType(master.getRepeatType());
	            if (type == null) continue;

	            result.addAll(expandOccurrences(master, type, rangeStart, rangeEnd));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        // psOverlap는 이미 rs 닫은 상태
	        DBConnManager.close(null, psOverlap, null);
	        DBConnManager.close(conn, psRepeat, rs);
	    }

	    // 시작시각 기준 정렬
	    result.sort(java.util.Comparator.comparing(EventVO::getStartDate,
	            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

	    return result;
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
	
	/** 반복 여부 판단 */
	private boolean isRepeating(EventVO e) {
	    return normalizedRepeatType(e.getRepeatType()) != null;
	}

	/** 반복 타입 정규화(DAILY/WEEKLY/MONTHLY/YEARLY). 알 수 없으면 null */
	private String normalizedRepeatType(String rt) {
	    if (rt == null) return null;
	    String t = rt.trim().toUpperCase();
	    switch (t) {
	        case "매일":
	        case "매주":
	        case "매월":
	        case "매년":
	            return t;
	        default:
	            return null;
	    }
	}

	/** 마스터 이벤트를 주어진 기간으로 on-the-fly 전개 */
	private java.util.List<EventVO> expandOccurrences(EventVO base, String type,
	        java.time.LocalDateTime rangeStart, java.time.LocalDateTime rangeEnd) {

	    java.util.List<EventVO> out = new java.util.ArrayList<>();
	    java.time.LocalDateTime s0 = base.getStartDate();
	    if (s0 == null) return out;

	    java.time.LocalDateTime e0 = (base.getEndDate() != null) ? base.getEndDate() : base.getStartDate();
	    if (e0.isBefore(s0)) { // 잘못된 데이터 방지
	        java.time.LocalDateTime tmp = s0; s0 = e0; e0 = tmp;
	    }
	    java.time.Duration dur = java.time.Duration.between(s0, e0);

	    // rangeStart 이상으로 처음 occurrence 빠르게 당기기
	    java.time.LocalDateTime cur = alignOnOrAfter(s0, rangeStart, type);

	    int guard = 0, MAX = 2000; // 무한루프 방지 가드
	    while (cur != null && !cur.isAfter(rangeEnd) && guard++ < MAX) {
	        java.time.LocalDateTime curEnd = cur.plus(dur);
	        // [cur..curEnd] 와 [rangeStart..rangeEnd]가 겹치면 출력
	        if (!cur.isAfter(rangeEnd) && !curEnd.isBefore(rangeStart)) {
	            out.add(copyWithShift(base, cur, curEnd));
	        }
	        cur = next(cur, type);
	    }
	    return out;
	}

	/** rangeStart 이상이 되도록 최초 occurrence를 정렬 */
	private java.time.LocalDateTime alignOnOrAfter(java.time.LocalDateTime start,
	                                               java.time.LocalDateTime rangeStart,
	                                               String type) {
	    if (!start.isBefore(rangeStart)) return start;

	    switch (type) {
	        case "DAILY": {
	            long days = java.time.Duration.between(start, rangeStart).toDays();
	            java.time.LocalDateTime cand = start.plusDays(Math.max(0, days));
	            while (cand.isBefore(rangeStart)) cand = cand.plusDays(1);
	            return cand;
	        }
	        case "WEEKLY": {
	            long days = java.time.Duration.between(
	                    start.toLocalDate().atStartOfDay(),
	                    rangeStart.toLocalDate().atStartOfDay()).toDays();
	            long weeks = Math.max(0, days / 7);
	            java.time.LocalDateTime cand = start.plusWeeks(weeks);
	            while (cand.isBefore(rangeStart)) cand = cand.plusWeeks(1);
	            return cand;
	        }
	        case "MONTHLY": {
	            java.time.Period p = java.time.Period.between(start.toLocalDate(), rangeStart.toLocalDate());
	            int months = p.getYears() * 12 + p.getMonths();
	            java.time.LocalDateTime cand = start.plusMonths(Math.max(0, months));
	            while (cand.isBefore(rangeStart)) cand = cand.plusMonths(1);
	            return cand;
	        }
	        case "YEARLY": {
	            int years = rangeStart.getYear() - start.getYear();
	            java.time.LocalDateTime cand = start.plusYears(Math.max(0, years));
	            while (cand.isBefore(rangeStart)) cand = cand.plusYears(1);
	            return cand;
	        }
	        default:
	            return start;
	    }
	}

	/** 다음 occurrence 시간 */
	private java.time.LocalDateTime next(java.time.LocalDateTime t, String type) {
	    switch (type) {
	        case "매일":   return t.plusDays(1);
	        case "매주":  return t.plusWeeks(1);
	        case "매월": return t.plusMonths(1);
	        case "매년":  return t.plusYears(1);
	        default:        return null;
	    }
	}

	/** occurrence로 사용할 VO 사본(INSERT 하지 않음) */
	private EventVO copyWithShift(EventVO base,
	                              java.time.LocalDateTime start,
	                              java.time.LocalDateTime end) {
	    EventVO v = new EventVO();
	    v.setEventId(base.getEventId());        // 상세/수정 연결을 위해 원본 id 유지
	    v.setUserId(base.getUserId());
	    v.setTitle(base.getTitle());
	    v.setDescription(base.getDescription());
	    v.setRepeatType(base.getRepeatType());  // 표시 목적
	    v.setStartDate(start);
	    v.setEndDate(end);
	    return v;
	}

}