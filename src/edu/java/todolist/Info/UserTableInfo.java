package edu.java.todolist.Info;

public interface UserTableInfo extends MySQLConnInfo{
	public static final String TABLE_NAME = "user";
	public static final String COL_USER_ID = "user_id";
	public static final String COL_EMAIL = "email";
	public static final String COL_PASSWORD = "password";
	public static final String COL_USERNAME = "username";
	public static final String COL_CREATED_AT = "created_at";
	public static final String COL_UPDATED_AT = "updated_at";
	public static final String COL_IS_ACTIVE = "is_active";
	public static final String COL_AUTH_PROVIDER = "auth_provider";
	
	// 회원 정보 등록 - 회원 가입
	public static final String SQL_INSERT =
			"INSERT INTO user (email, password, username, created_at, updated_at, is_active, auth_provider) " +
                    "VALUES (?, ?, ?, NOW(), NOW(), TRUE, ?)";
	
	// 회원 정보 조회 - 로그인
	public static final String SQL_SELECT = 
			"SELECT * FROM user WHERE email = ? AND password = ? AND is_active = TRUE";
	
	// 회원 정보 조회 - 이메일
		public static final String SQL_SELECT_EMAIL = 
				"SELECT COUNT(*) FROM user WHERE email = ?";
	
	// 회원 정보 수정 - 비밀번호 변경
	public static final String SQL_UPDATE_PASSWORD =
			"UPDATE user SET username = ?, password = ?, updated_at = NOW() WHERE user_id = ? AND is_active = TRUE";
	
	// 회원 정보 수정 - 비활성화
	public static final String SQL_UPDATE_ACTIVE =
			"UPDATE user SET is_active = FALSE, updated_at = NOW() WHERE user_id = ?";
	
	// 회원 정보 수정 - 최근 로그인
	public static final String SQL_UPDATE_LAST_LOGIN =
			"UPDATE user SET last_login = NOW() WHERE user_id = ?";
	
	// 회원 정보 삭제 - 탈퇴
		public static final String SQL_DELETE =
				"delete from " + TABLE_NAME + " where user_id = ?";
	
	// 사이즈
	public static final String SQL_COUNT =
			"SELECT COUNT(contact_id) FROM " + TABLE_NAME;
}
