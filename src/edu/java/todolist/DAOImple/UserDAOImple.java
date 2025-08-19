package edu.java.todolist.DAOImple;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import edu.java.todolist.DBConnManager;
import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.Info.MySQLConnInfo;
import edu.java.todolist.Info.UserTableInfo;
import edu.java.todolist.VO.UserVO;

public class UserDAOImple implements UserDAO, UserTableInfo, MySQLConnInfo {
	private static UserDAOImple instance = null;

	private UserDAOImple() {
	}

	public static UserDAOImple getInstance() {
		if (instance == null) {
			instance = new UserDAOImple();
		}
		return instance;
	}

	// 회원 가입
	@Override
	public int registerUser(UserVO user) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			pstmt = conn.prepareStatement(SQL_INSERT);
			pstmt.setString(1, user.getEmail());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getUsername());
			pstmt.setString(4, user.getAuthProvider());
			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	// 로그인
	@Override
	public UserVO login(String email, String password) {
		UserVO vo = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			String sql = "SELECT * FROM user WHERE email = ? AND password = ? AND is_active = TRUE";
			pstmt = conn.prepareStatement(sql);

			// 5. SQL 문장 작성 (파라미터 바인딩)
			pstmt.setString(1, email);
			pstmt.setString(2, password);

			// 6. SQL 문장 실행
			rs = pstmt.executeQuery();

			if (rs.next()) { // 결과 레코드가 있을 때
				vo = extractUserFromResultSet(rs);
				updateLastLogin(vo.getUserId());
				System.out.println(vo);
			}

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt, rs);
		}

		return vo;
	}

	// 비밀번호 수정
	@Override
	public int updateUser(UserVO user) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			pstmt = conn.prepareStatement(SQL_UPDATE_PASSWORD);
			pstmt.setString(1, user.getUsername());
			pstmt.setString(2, user.getPassword());
			pstmt.setInt(3, user.getUserId()); // userId는 UserVO 내부에서 가져옴
			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	// 회원 비활성화
	@Override
	public int deactivateUser(int userId) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			pstmt = conn.prepareStatement(SQL_UPDATE_ACTIVE);
			pstmt.setInt(1, userId);
			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	// 회원 삭제
	@Override
	public int deleteUser(int userId) {
		int result = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			pstmt = conn.prepareStatement(SQL_DELETE);
			pstmt.setInt(1, userId);
			result = pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
		return result;
	}

	// 로그인 시 last_login 업데이트
	@Override
	public void updateLastLogin(int userId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");
			
			pstmt = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN);
			pstmt.setInt(1, userId);
			pstmt.executeUpdate();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt);
		}
	}
	
	// 회원가입 시 이메일 중복 여부 확인
	@Override
	public boolean isEmailExists(String email) {
		boolean exists = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			System.out.println("드라이버 로드 성공");
			conn = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("DB 연결 성공");

			pstmt = conn.prepareStatement(SQL_SELECT_EMAIL);
			pstmt.setString(1, email);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				exists = rs.getInt(1) > 0;
			}
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			DBConnManager.close(conn, pstmt, rs);
		}
		return exists;
	}
	
	// ResultSet → UserVO 변환
		private UserVO extractUserFromResultSet(ResultSet rs) {
		    UserVO user = new UserVO();
		    try {
		        user.setUserId(rs.getInt("user_id"));
		        user.setEmail(rs.getString("email"));
		        user.setPassword(rs.getString("password"));
		        user.setUsername(rs.getString("username"));
		        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
		        Timestamp lastLogin = rs.getTimestamp("last_login");
		        if (lastLogin != null) {
		            user.setLastLogin(lastLogin.toLocalDateTime());
		        }
		        user.setActive(rs.getBoolean("is_active"));
		        user.setAuthProvider(rs.getString("auth_provider"));
		    } catch (SQLException e) {
		        e.printStackTrace();
		    }
		    return user;
		}

}
