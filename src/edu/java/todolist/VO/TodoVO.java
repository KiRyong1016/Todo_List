package edu.java.todolist.VO;

import java.time.LocalDateTime;

public class TodoVO {
    private int todoId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Priority priority;
    private Status status;
    private String category;
    private int userId;

    public enum Priority {
        상, 중, 하
    }

    public enum Status {
        진행중, 완료, 보류
    }

    public TodoVO() {}

    public TodoVO(int todoId, String title, String description,
                  LocalDateTime dueDate, Priority priority,
                  Status status, String category, int userId) {
        this.todoId = todoId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = (priority != null) ? priority : Priority.중;
        this.status = (status != null) ? status : Status.진행중;
        this.category = category;
        this.userId = userId;
    }

    public int getTodoId() {
        return todoId;
    }

    public void setTodoId(int todoId) {
        this.todoId = todoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "TodoVO{" +
                "todoId=" + todoId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dueDate=" + dueDate +
                ", priority=" + priority +
                ", status=" + status +
                ", category='" + category + '\'' +
                ", userId=" + userId +
                '}';
    }
}
