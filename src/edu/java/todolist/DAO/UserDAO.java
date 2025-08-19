package edu.java.todolist.DAO;

import edu.java.todolist.VO.UserVO;

public interface UserDAO {
    public abstract int registerUser(UserVO user);
    
    public abstract UserVO login(String email, String password);
    
    public abstract int updateUser(UserVO user);
    
    public abstract int deactivateUser(int userId);
    
    public abstract int deleteUser(int userId);
    
    public abstract void updateLastLogin(int userId);
    
    public abstract boolean isEmailExists(String email);
}
