package edu.java.todolist;

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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
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

public class TodoListMain extends JFrame {
    private static final long serialVersionUID = 1L;

    // DAO (getInstance 유지)
    private final EventDAO EventDao = EventDAOImple.getInstance();
    private final UserDAO  UserDao  = UserDAOImple.getInstance();
    private final TodoDAO  TodoDao  = TodoDAOImple.getInstance();

    // 로그인 사용자
    private final UserVO loggedInUser;

    // 포맷터 (요청대로 유지)
    private final DateTimeFormatter timeFormatterHM    = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter timeFormatterYMDHM = DateTimeFormatter.ofPattern("yy-MM-dd");
    private final DateTimeFormatter headerFormatter    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 탭/모델 참조(필드 유지!)
    private JTabbedPane tabbedPane;
    private JPanel      mainPanel;
    private DefaultTableModel scheduleTableModel;
    private DefaultTableModel todoTableModel;

    public TodoListMain(UserVO user) {
        this.loggedInUser = user;

        setTitle("ToDo List - " + loggedInUser.getUsername() + "님 환영합니다");
        setSize(1500, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initialize();
    }

    private void initialize() {
        tabbedPane = new JTabbedPane();

        // ===== 홈 패널(왼쪽: 오늘 일정 / 오른쪽: 미완료 To-Do) =====
        // 왼쪽(오늘 일정)
        JPanel leftPanel = new JPanel(new BorderLayout());
        JLabel leftLabel = new JLabel("오늘 일정", SwingConstants.CENTER);
        leftLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        leftPanel.add(leftLabel, BorderLayout.NORTH);

        scheduleTableModel = new DefaultTableModel(new String[] {"시작 시간", "종료 시간", "스케쥴"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable scheduleTable = new JTable(scheduleTableModel);
        scheduleTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        scheduleTable.setRowHeight(30);
        leftPanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        // 오른쪽(To-Do)
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel rightLabel = new JLabel("To-Do 리스트", SwingConstants.CENTER);
        rightLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        rightPanel.add(rightLabel, BorderLayout.NORTH);

        todoTableModel = new DefaultTableModel(new String[] {"기한", "할일", "카테고리", "중요도", "상태"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable todoTable = new JTable(todoTableModel);
        todoTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        todoTable.setRowHeight(30);
        rightPanel.add(new JScrollPane(todoTable), BorderLayout.CENTER);

        // 좌우 분할
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        // 메인 상단 시계 라벨
        mainPanel = new JPanel(new BorderLayout());
        JLabel mainLabel = new JLabel(LocalDateTime.now().format(headerFormatter), SwingConstants.CENTER);
        mainLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        mainPanel.add(mainLabel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 시계 업데이트
        Timer clock = new Timer(1000, e -> mainLabel.setText(LocalDateTime.now().format(headerFormatter)));
        clock.start();

        // ===== 다른 탭들 =====
        int userId = loggedInUser.getUserId();
        CalendarPanel calendarPanel = new CalendarPanel(userId);
        EventPanel    eventPanel    = new EventPanel(userId);
        TodoPanel     todoPanel     = new TodoPanel(userId);
        StatsPanel    statsPanel    = new StatsPanel(userId);

        // 설정(사용자) 탭
        JPanel userPanel = buildUserPanel();

        // 탭 추가
        tabbedPane.addTab("홈", mainPanel);
        tabbedPane.addTab("캘린더", calendarPanel);
        tabbedPane.addTab("일정", eventPanel);
        tabbedPane.addTab("ToDo", todoPanel);
        tabbedPane.addTab("통계", statsPanel);
        tabbedPane.addTab("설정", userPanel);

        // ✅ 문자열 비교 대신 '컴포넌트 참조'로 홈 탭 갱신
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == mainPanel) {
                refreshMainTabData();
            }
        });

        // 초기 로딩
        refreshMainTabData();

        getContentPane().add(tabbedPane);
    }

    private JPanel buildUserPanel() {
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new javax.swing.BoxLayout(userPanel, javax.swing.BoxLayout.Y_AXIS));
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

        return userPanel;
    }

    /** 홈 탭 데이터 갱신: 오늘 일정(겹침/반복) + 미완료 To-Do */
    private void refreshMainTabData() {
        LocalDate today = LocalDate.now();

        // --- 오늘 일정: 겹침 + 반복 전개 반영 ---
        scheduleTableModel.setRowCount(0);
        for (EventVO s : EventDao.selectEventsByDateRange(loggedInUser.getUserId(), today, today)) {
            String startHm = (s.getStartDate() != null) ? s.getStartDate().format(timeFormatterHM) : "";
            String endHm   = (s.getEndDate()   != null) ? s.getEndDate().format(timeFormatterHM)   : "";
            scheduleTableModel.addRow(new Object[]{ startHm, endHm, s.getTitle() });
        }
        scheduleTableModel.fireTableDataChanged();

        // --- 미완료 To-Do ---
        todoTableModel.setRowCount(0);
        for (TodoVO t : TodoDao.selectAllTodosByUserId(loggedInUser.getUserId())) {
            if (t.getStatus() != Status.완료) {
                String dueStr   = (t.getDueDate() != null) ? t.getDueDate().format(timeFormatterYMDHM) : "기한 없음";
                String priority = (t.getPriority() != null) ? t.getPriority().toString() : "";
                String status   = (t.getStatus()   != null) ? t.getStatus().toString()   : "";
                String category = (t.getCategory() != null) ? t.getCategory().toString() : "없음";
                // 컬럼 순서: "내용","기한","중요도","상태","카테고리"
                todoTableModel.addRow(new Object[]{ dueStr, t.getDescription(), category, priority, status });
            }
        }
        todoTableModel.fireTableDataChanged();
    }

    public static void main(String[] args) {
        new Login();
    }
}
