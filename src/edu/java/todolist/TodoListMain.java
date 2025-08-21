package edu.java.todolist;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAO.TodoDAO;
import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.Service.CalendarPanel;
import edu.java.todolist.Service.ChangePasswordDialog;
import edu.java.todolist.Service.EventPanel;
import edu.java.todolist.Service.Login;
import edu.java.todolist.Service.StatsPanel;
import edu.java.todolist.Service.TodoPanel;
import edu.java.todolist.VO.EventVO;
import edu.java.todolist.VO.TodoVO;
import edu.java.todolist.VO.TodoVO.Status;
import edu.java.todolist.VO.UserVO;

public class TodoListMain extends JFrame {
    private static final long serialVersionUID = 1L;

    // ✅ getInstance() 유지
    private final EventDAO EventDao = EventDAOImple.getInstance();
    private final UserDAO  UserDao  = UserDAOImple.getInstance();
    private final TodoDAO  TodoDao  = TodoDAOImple.getInstance();

    private final UserVO loggedInUser;

    private DefaultTableModel scheduleTableModel;
    private DefaultTableModel todoTableModel;

    private final DateTimeFormatter timeFormatterHM     = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter timeFormatterYMDHM  = DateTimeFormatter.ofPattern("yy-MM-dd"); // 의도 유지(날짜만)
    private final DateTimeFormatter headerFormatter     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TodoListMain(UserVO user) {
        this.loggedInUser = user;
        setTitle("ToDo List - " + loggedInUser.getUsername() + "님 환영합니다");
        setSize(1500, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initialize();
    }

    private void initialize() {
        JTabbedPane tabbedPane = new JTabbedPane();
        LocalDate today = LocalDate.now();
        int userId = loggedInUser.getUserId();

        // 좌(오늘 일정)
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel leftLabel = new JLabel("오늘 일정", SwingConstants.CENTER);
        leftLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        leftPanel.add(leftLabel, BorderLayout.NORTH);

        scheduleTableModel = new DefaultTableModel(new String[] {"시작 시간", "종료 시간", "스케줄"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
     // 오늘 하루 경계(포함)
        var dayStart = today.atStartOfDay();
        var dayEnd   = today.plusDays(1).atStartOfDay().minusSeconds(1);

        // 반복 전개 포함(DAO에서 on-the-fly 전개)
        for (EventVO s : EventDao.selectEventsByDateRange(userId, today, today)) {
            var st = s.getStartDate();
            var et = (s.getEndDate() != null) ? s.getEndDate() : s.getStartDate();

            // 화면 표시는 "당일 경계"로 잘라서 보여주기(전날~내일跨 일정 가독성)
            var dispStart = st.isBefore(dayStart) ? dayStart : st;
            var dispEnd   = et.isAfter(dayEnd)    ? dayEnd   : et;

            scheduleTableModel.addRow(new Object[] {
                dispStart.format(timeFormatterHM),
                dispEnd.format(timeFormatterHM),
                s.getTitle()
            });
        }
        JTable scheduleTable = new JTable(scheduleTableModel);
        scheduleTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        scheduleTable.setRowHeight(30);
        leftPanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        // 우(To-Do 리스트)
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel rightLabel = new JLabel("To-Do 리스트", SwingConstants.CENTER);
        rightLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        rightPanel.add(rightLabel, BorderLayout.NORTH);

        todoTableModel = new DefaultTableModel(new String[] {"내용", "기한", "중요도", "상태", "카테고리"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (TodoVO s : TodoDao.selectAllTodosByUserId(userId)) {
            if (s.getStatus() != Status.완료) {
                String dueStr      = (s.getDueDate()  != null) ? s.getDueDate().format(timeFormatterYMDHM) : "기한 없음";
                String priorityStr = (s.getPriority() != null) ? s.getPriority().toString() : "없음";
                String statusStr   = (s.getStatus()   != null) ? s.getStatus().toString()   : "없음";
                String categoryStr = (s.getCategory()  != null) ? s.getCategory()            : "없음";
                Object[] row = { s.getDescription(), dueStr, priorityStr, statusStr, categoryStr };
                todoTableModel.addRow(row);
            }
        }
        JTable todoTable = new JTable(todoTableModel);
        todoTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        todoTable.setRowHeight(30);
        rightPanel.add(new JScrollPane(todoTable), BorderLayout.CENTER);

        // 좌/우 분할
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        // 메인(상단 현재시각 + 좌우 분할)
        JPanel mainPanel = new JPanel(new BorderLayout());
        JLabel mainLabel = new JLabel(LocalDateTime.now().format(headerFormatter), SwingConstants.CENTER);
        mainLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        mainPanel.add(mainLabel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 시계(1초 갱신)
        Timer clock = new Timer(1000, e -> mainLabel.setText(LocalDateTime.now().format(headerFormatter)));
        clock.start();

        // 개별 탭(프레임에 add하지 말고 탭에만 추가)
        CalendarPanel calendarPanel = new CalendarPanel(userId);
        EventPanel    eventPanel    = new EventPanel(userId);
        TodoPanel     todoPanel     = new TodoPanel(userId);
        StatsPanel    statsPanel    = new StatsPanel(userId);

        tabbedPane.addTab("홈", mainPanel);
        tabbedPane.addTab("캘린더", calendarPanel);
        tabbedPane.addTab("일정", eventPanel);
        tabbedPane.addTab("ToDo", todoPanel);
        tabbedPane.addTab("통계", statsPanel);

        // 설정(사용자) 탭
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel userLabel = new JLabel("사용자 탭");
        userLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton logoutButton = new JButton("로그아웃");
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(200, 40));
        logoutButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new Login().setVisible(true));
        });

        JButton changePasswordButton = new JButton("비밀번호 변경");
        changePasswordButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        changePasswordButton.setMaximumSize(new Dimension(200, 40));
        changePasswordButton.setMargin(new Insets(10, 10, 10, 10));
        changePasswordButton.addActionListener(e -> {
            ChangePasswordDialog dialog = new ChangePasswordDialog(this, loggedInUser);
            dialog.setVisible(true);
        });

        JButton deleteAccountButton = new JButton("회원 탈퇴");
        deleteAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteAccountButton.setMaximumSize(new Dimension(200, 40));
        deleteAccountButton.setMargin(new Insets(10, 10, 10, 10));
        deleteAccountButton.setForeground(Color.RED);
        deleteAccountButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "정말 회원 탈퇴를 진행하시겠습니까?\n모든 데이터가 삭제됩니다.",
                "회원 탈퇴 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                int result = UserDao.deleteUser(loggedInUser.getUserId());
                if (result == 1) {
                    JOptionPane.showMessageDialog(this, "회원 탈퇴가 완료되었습니다.");
                    dispose();
                    SwingUtilities.invokeLater(() -> new Login().setVisible(true));
                } else {
                    JOptionPane.showMessageDialog(this, "회원 탈퇴 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        userPanel.add(userLabel);
        userPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        userPanel.add(logoutButton);
        userPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        userPanel.add(changePasswordButton);
        userPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        userPanel.add(deleteAccountButton);

        tabbedPane.addTab("설정", userPanel);

        // 홈 탭으로 돌아올 때 데이터 새로고침
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx >= 0 && "홈".equals(tabbedPane.getTitleAt(idx))) {
                refreshMainTabData();
            }
        });

        getContentPane().add(tabbedPane);
    }

    private void refreshMainTabData() {
        LocalDate today = LocalDate.now();

        // 일정 새로고침
        scheduleTableModel.setRowCount(0);
        var dayStart = today.atStartOfDay();
        var dayEnd   = today.plusDays(1).atStartOfDay().minusSeconds(1);

        for (EventVO s : EventDao.selectEventsByDateRange(loggedInUser.getUserId(), today, today)) {
            var st = s.getStartDate();
            var et = (s.getEndDate() != null) ? s.getEndDate() : s.getStartDate();
            var dispStart = st.isBefore(dayStart) ? dayStart : st;
            var dispEnd   = et.isAfter(dayEnd)    ? dayEnd   : et;

            scheduleTableModel.addRow(new Object[]{
                dispStart.format(timeFormatterHM),
                dispEnd.format(timeFormatterHM),
                s.getTitle()
            });
        }


        // To-Do 새로고침(컬럼 5개/순서 일치)
        todoTableModel.setRowCount(0);
        for (TodoVO s : TodoDao.selectAllTodosByUserId(loggedInUser.getUserId())) {
            if (s.getStatus() != Status.완료) {
                String dueStr      = (s.getDueDate()  != null) ? s.getDueDate().format(timeFormatterYMDHM) : "기한 없음";
                String priorityStr = (s.getPriority() != null) ? s.getPriority().toString() : "없음";
                String statusStr   = (s.getStatus()   != null) ? s.getStatus().toString()   : "없음";
                String categoryStr = (s.getCategory()  != null) ? s.getCategory()            : "없음";
                Object[] row = { s.getDescription(), dueStr, priorityStr, statusStr, categoryStr };
                todoTableModel.addRow(row);
            }
        }
    }

    public static void main(String[] args) {
        // EDT에서 시작 권장
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}
