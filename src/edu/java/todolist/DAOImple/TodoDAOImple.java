package edu.java.todolist.DAOImple;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import edu.java.todolist.DAO.TodoDAO;
import edu.java.todolist.DBConnManager;
import edu.java.todolist.Info.MySQLConnInfo;
import edu.java.todolist.Info.TodoTableInfo;
import edu.java.todolist.VO.TodoVO;

public class TodoDAOImple implements TodoDAO, TodoTableInfo, MySQLConnInfo{
	private static TodoDAOImple instance = null;

    private TodoDAOImple() {}

    public static TodoDAOImple getInstance() {
        if (instance == null) {
            instance = new TodoDAOImple();
        }
        return instance;
    }
    
    @Override
    public int insertTodo(TodoVO todo) {
        int result = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_INSERT);
            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            if (todo.getDueDate() != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(todo.getDueDate()));
            } else {
                pstmt.setNull(3, Types.TIMESTAMP);
            }
            pstmt.setString(4, todo.getPriority().name());
            pstmt.setString(5, todo.getStatus().name());
            pstmt.setString(6, todo.getCategory());
            pstmt.setInt(7, todo.getUserId());

            result = pstmt.executeUpdate();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt);
        }
        return result;
    }

    @Override
    public List<TodoVO> selectAllTodosByUserId(int userId) {
        List<TodoVO> list = new ArrayList<>();
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
                list.add(extractTodoFromResultSet(rs));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return list;
    }
    
	@Override
	public TodoVO selectAllTodoByTodoId(int todoId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TodoVO todo = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_SELECT_BY_TODO_ID);
            pstmt.setInt(1, todoId);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                todo = extractTodoFromResultSet(rs);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return todo;
	}

    @Override
    public List<TodoVO> selectTodosByDueDate(int userId, LocalDate dueDate) {
        List<TodoVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_SELECT_BY_DUEDATE);
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(dueDate.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(dueDate.plusDays(1).atStartOfDay()));

            rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(extractTodoFromResultSet(rs));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return list;
    }
    
    @Override
    public List<TodoVO> selectTodosByDueDateRange(int userId, LocalDate startDate, LocalDate endDate) {
        List<TodoVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_SELECT_BY_DATE_RANGE);
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));
            pstmt.setTimestamp(3, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay().minusSeconds(1)));

            rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(extractTodoFromResultSet(rs));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt, rs);
        }

        return list;
    }
    
    @Override
    public int countTodosByUserId(int userId, boolean onlyNotDone) {
        String sql = onlyNotDone ? SQL_COUNT_TODO_NOTDONE : SQL_SELECT;
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    @Override
    public List<TodoVO> selectTodosByUserIdPaged(int userId, int offset, int limit,
                                                 String orderBy, boolean asc, boolean onlyNotDone) {
        String column = switch (orderBy) {
            case "due"      -> "due_date";
            case "priority" -> "priority";
            case "status"   -> "status";
            case "title"    -> "title";
            default         -> "due_date";
        };
        String dir = asc ? "ASC" : "DESC";
        String base = "FROM todo WHERE user_id = ?" + (onlyNotDone ? " AND status <> '완료'" : "");
        String sql = "SELECT * " + base + " ORDER BY " + column + " " + dir + " LIMIT ? OFFSET ?";

        List<TodoVO> list = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(extractTodoFromResultSet(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
    
    @Override
    public int updateTodo(TodoVO todo) {
        int result = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_UPDATE);
            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            if (todo.getDueDate() != null) {
                pstmt.setTimestamp(3, Timestamp.valueOf(todo.getDueDate()));
            } else {
                pstmt.setNull(3, Types.TIMESTAMP);
            }
            pstmt.setString(4, todo.getPriority().name());
            pstmt.setString(5, todo.getStatus().name());
            pstmt.setString(6, todo.getCategory());
            pstmt.setInt(7, todo.getTodoId());

            result = pstmt.executeUpdate();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt);
        }
        return result;
    }

    @Override
    public int deleteTodo(int todoId) {
        int result = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            pstmt = conn.prepareStatement(SQL_DELETE);
            pstmt.setInt(1, todoId);

            result = pstmt.executeUpdate();

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            DBConnManager.close(conn, pstmt);
        }
        return result;
    }

    private TodoVO extractTodoFromResultSet(ResultSet rs) throws SQLException {
        TodoVO todo = new TodoVO();
        todo.setTodoId(rs.getInt("todo_id"));
        todo.setTitle(rs.getString("title"));
        todo.setDescription(rs.getString("description"));
        Timestamp dueTimestamp = rs.getTimestamp("due_date");
        if (dueTimestamp != null) {
            todo.setDueDate(dueTimestamp.toLocalDateTime());
        }
        String priorityStr = rs.getString("priority");
        if (priorityStr != null) {
            try {
                todo.setPriority(TodoVO.Priority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                todo.setPriority(TodoVO.Priority.중); // 기본값
            }
        } else {
            todo.setPriority(TodoVO.Priority.중);
        }
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                todo.setStatus(TodoVO.Status.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                todo.setStatus(TodoVO.Status.진행중); // 기본값
            }
        } else {
            todo.setStatus(TodoVO.Status.진행중);
        }
        todo.setStatus(TodoVO.Status.valueOf(rs.getString("status")));
        todo.setCategory(rs.getString("category"));
        todo.setUserId(rs.getInt("user_id"));
        return todo;
    }
}
