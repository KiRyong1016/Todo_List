package edu.java.todolist.DAOImple;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
	    List<EventVO> result = new ArrayList<>();
	    Connection conn = null;
	    PreparedStatement ps1 = null; // 겹침(반복/비반복 모두) 조회
	    PreparedStatement ps2 = null; // 반복 마스터 조회
	    ResultSet rs1 = null, rs2 = null;

	    final LocalDateTime rangeStart = startDate.atStartOfDay();
	    final LocalDateTime rangeEnd   = endDate.plusDays(1).atStartOfDay().minusSeconds(1);

	    // ✅ 1) 반복 타입 조건 없이 "겹침"으로 모두 조회
	    final String SQL_OVERLAP_ALL =
	        "SELECT * FROM event " +
	        "WHERE user_id = ? " +
	        "  AND start_date <= ? " +                         // rangeEnd
	        "  AND COALESCE(end_date, start_date) >= ? " +     // rangeStart
	        "ORDER BY start_date";

	    // ✅ 2) 반복 마스터만(여기서도 '없음'은 제외될 수 있으나, 자바에서 다시 거름)
	    final String SQL_REPEAT_MASTERS =
	        "SELECT * FROM event " +
	        "WHERE user_id = ? " +
	        "  AND repeat_type IS NOT NULL " +
	        "  AND TRIM(repeat_type) <> ''";

	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        conn = DriverManager.getConnection(URL, USER, PASSWORD);

	        // 1) 겹침 전체 조회 → 자바에서 "비반복"만 골라 담기
	        ps1 = conn.prepareStatement(SQL_OVERLAP_ALL);
	        ps1.setInt(1, userId);
	        ps1.setTimestamp(2, java.sql.Timestamp.valueOf(rangeEnd));
	        ps1.setTimestamp(3, java.sql.Timestamp.valueOf(rangeStart));
	        rs1 = ps1.executeQuery();
	        while (rs1.next()) {
	            EventVO ev = extractEventFromResultSet(rs1);
	            if (normalizedRepeatType(ev.getRepeatType()) == null) {
	                // '없음'/NULL/'' 모두 비반복으로 취급
	                result.add(ev);
	            }
	        }

	        // 2) 반복 마스터 전개 → 기간 내 회차만 추가
	        ps2 = conn.prepareStatement(SQL_REPEAT_MASTERS);
	        ps2.setInt(1, userId);
	        rs2 = ps2.executeQuery();
	        while (rs2.next()) {
	            EventVO master = extractEventFromResultSet(rs2);
	            String rpt = normalizedRepeatType(master.getRepeatType());
	            if (rpt == null) continue; // '없음' 등은 스킵 (이미 1)에서 포함됨)

	            LocalDateTime mStart = master.getStartDate();
	            LocalDateTime mEnd   = (master.getEndDate() != null) ? master.getEndDate() : master.getStartDate();
	            java.time.Duration dur = java.time.Duration.between(mStart, mEnd);

	            LocalDateTime occStart = alignOnOrAfter(mStart, rpt, rangeStart);

	            int safety = 0, MAX = 5000;
	            while (!occStart.isAfter(rangeEnd) && safety++ < MAX) {
	                LocalDateTime occEnd = occStart.plus(dur);
	                boolean overlap = !occEnd.isBefore(rangeStart) && !occStart.isAfter(rangeEnd);
	                if (overlap) {
	                    EventVO occ = cloneEvent(master);
	                    occ.setStartDate(occStart);
	                    occ.setEndDate(occEnd);
	                    result.add(occ);
	                }
	                occStart = nextOccurrence(occStart, rpt);
	            }
	        }
	    } catch (SQLException | ClassNotFoundException e) {
	        e.printStackTrace();
	    } finally {
	        DBConnManager.close(null, null, rs1);
	        DBConnManager.close(null, ps1, null);
	        DBConnManager.close(conn, ps2, rs2);
	    }

	    result.sort(Comparator.comparing(EventVO::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())));
	    return result;
	}

	
	@Override
	public List<EventVO> selectEventsByUserIdPaged(int userId, int offset, int limit, String orderBy, boolean asc) {
	    // 안전한 컬럼 화이트리스트
	    String column = switch (orderBy) {
	        case "start"  -> "start_date";
	        case "title"  -> "title";
	        case "repeat" -> "repeat_type";
	        default       -> "start_date";
	    };
	    String dir = asc ? "ASC" : "DESC";
	    String sql = "SELECT * FROM event WHERE user_id = ? ORDER BY " + column + " " + dir + " LIMIT ? OFFSET ?";

	    List<EventVO> list = new ArrayList<>();
	    try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
	         PreparedStatement ps = c.prepareStatement(sql)) {
	        ps.setInt(1, userId);
	        ps.setInt(2, limit);
	        ps.setInt(3, offset);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) list.add(extractEventFromResultSet(rs));
	        }
	    } catch (Exception e) { e.printStackTrace(); }
	    return list;
	}

	@Override
	public int countEventsByUserId(int userId) {
	    try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
	         PreparedStatement ps = c.prepareStatement(SQL_COUNT_BY_USER)) {
	        ps.setInt(1, userId);
	        try (ResultSet rs = ps.executeQuery()) {
	            return rs.next() ? rs.getInt(1) : 0;
	        }
	    } catch (Exception e) { e.printStackTrace(); return 0; }
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
	
	private static EventVO cloneEvent(EventVO src) {
	    EventVO v = new EventVO();
	    v.setEventId(src.getEventId());
	    v.setUserId(src.getUserId());
	    v.setTitle(src.getTitle());
	    v.setDescription(src.getDescription());
	    v.setRepeatType(src.getRepeatType());
	    v.setStartDate(src.getStartDate());
	    v.setEndDate(src.getEndDate());
	    return v;
	}

	private String normalizedRepeatType(String rt) {
	    if (rt == null) return null;
	    String s = rt.trim();
	    if (s.isEmpty() || "없음".equals(s)) return null;
	    switch (s) {
	        case "매일": return "DAILY";
	        case "매주": return "WEEKLY";
	        case "매월":
	        case "매달": return "MONTHLY";
	        case "매년": return "YEARLY";
	        default:
	            String u = s.toUpperCase(Locale.ROOT);
	            return switch (u) {
	                case "DAILY", "WEEKLY", "MONTHLY", "YEARLY" -> u;
	                default -> null;
	            };
	    }
	}

	private LocalDateTime alignOnOrAfter(LocalDateTime base, String type, LocalDateTime pivot) {
	    if (!base.isBefore(pivot)) return base;
	    switch (type) {
	        case "DAILY"  -> { long d = ChronoUnit.DAYS.between(base.toLocalDate(),   pivot.toLocalDate());   LocalDateTime x = base.plusDays(d);  return x.isBefore(pivot) ? x.plusDays(1)  : x; }
	        case "WEEKLY" -> { long w = ChronoUnit.WEEKS.between(base.toLocalDate(),  pivot.toLocalDate());   LocalDateTime x = base.plusWeeks(w); return x.isBefore(pivot) ? x.plusWeeks(1) : x; }
	        case "MONTHLY"-> {
	            long m = ChronoUnit.MONTHS.between(base.toLocalDate().withDayOfMonth(1), pivot.toLocalDate().withDayOfMonth(1));
	            LocalDateTime x = base.plusMonths(m);
	            int dom = Math.min(base.getDayOfMonth(), x.toLocalDate().lengthOfMonth());
	            x = LocalDateTime.of(LocalDate.of(x.getYear(), x.getMonth(), dom), base.toLocalTime());
	            return x.isBefore(pivot) ? nextOccurrence(x, "MONTHLY") : x;
	        }
	        case "YEARLY" -> { long y = ChronoUnit.YEARS.between(base.toLocalDate().withDayOfYear(1), pivot.toLocalDate().withDayOfYear(1));
	                           LocalDateTime x = base.plusYears(y); return x.isBefore(pivot) ? x.plusYears(1) : x; }
	        default -> { return base; }
	    }
	}

	private LocalDateTime nextOccurrence(LocalDateTime cur, String type) {
	    return switch (type) {
	        case "DAILY"   -> cur.plusDays(1);
	        case "WEEKLY"  -> cur.plusWeeks(1);
	        case "MONTHLY" -> {
	            LocalDateTime x = cur.plusMonths(1);
	            int dom = Math.min(cur.getDayOfMonth(), x.toLocalDate().lengthOfMonth());
	            yield LocalDateTime.of(LocalDate.of(x.getYear(), x.getMonth(), dom), cur.toLocalTime());
	        }
	        case "YEARLY"  -> cur.plusYears(1);
	        default        -> cur;
	    };
	}

}