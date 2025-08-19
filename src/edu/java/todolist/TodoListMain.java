package edu.java.todolist;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAO.UserDAO;
import edu.java.todolist.DAO.TodoDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.DAOImple.UserDAOImple;
import edu.java.todolist.VO.EventVO;
import edu.java.todolist.VO.UserVO;
import edu.java.todolist.VO.TodoVO;
import edu.java.todolist.VO.TodoVO.Status;
import edu.java.todolist.Service.CalendarPanel;
import edu.java.todolist.Service.ChangePasswordDialog;
import edu.java.todolist.Service.Login;
import edu.java.todolist.Service.TodoPanel;
import edu.java.todolist.Service.EventPanel;
import edu.java.todolist.Service.StatsPanel;

public class TodoListMain extends JFrame {
	private static final long serialVersionUID = 1L;
	EventDAO EventDao = EventDAOImple.getInstance();
	UserDAO UserDao = UserDAOImple.getInstance();
	TodoDAO TodoDao = TodoDAOImple.getInstance();
	private UserVO loggedInUser;
	private DefaultTableModel scheduleTableModel;
	private DefaultTableModel todoTableModel;
	private DateTimeFormatter timeFormatterHM = DateTimeFormatter.ofPattern("HH:mm");
	private DateTimeFormatter timeFormatterYMDHM = DateTimeFormatter.ofPattern("yy-MM-dd");
	private final DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	
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

		// 메인 탭/왼쪽 테이블 - 현재 날짜 기준 일정 표시
		JPanel leftPanel = new JPanel(new BorderLayout());
		JLabel leftLabel = new JLabel("오늘 일정", SwingConstants.CENTER);
		leftLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
		leftPanel.add(leftLabel, BorderLayout.NORTH);
		scheduleTableModel = new DefaultTableModel(new String[] { "시작 시간", "종료 시간", "스케쥴" }, 0) {
		    @Override
		    public boolean isCellEditable(int row, int column) {
		        return false;
		    }
		};
		for (EventVO s : EventDao.selectEventsByDate(userId, today)) {
			scheduleTableModel.addRow(new Object[] { s.getStartDate().format(timeFormatterHM),
					s.getEndDate().format(timeFormatterHM), s.getTitle() });
		}
		JTable scheduleTable = new JTable(scheduleTableModel);
		scheduleTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
		scheduleTable.setRowHeight(30);
		leftPanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);
		// 메인 탭/오른쪽 테이블 - 현재 날짜 기준 todo 표시
		JPanel rightPanel = new JPanel(new BorderLayout());
		JLabel rightLabel = new JLabel("To-Do 리스트", SwingConstants.CENTER);
		rightLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
		rightPanel.add(rightLabel, BorderLayout.NORTH);
		todoTableModel = new DefaultTableModel(new String[] { "내용", "기한", "중요도", "상태", "카테고리" }, 0) {
		    @Override
		    public boolean isCellEditable(int row, int column) {
		        return false;
		    }
		};
		for (TodoVO s : TodoDao.selectAllTodosByUserId(userId)) {
			if(s.getStatus() != Status.완료) {
				String dueStr = (s.getDueDate() != null) ? s.getDueDate().format(timeFormatterYMDHM) : "기한 없음";
				Object[] row = { s.getDescription(), dueStr, s.getPriority().toString(), s.getStatus().toString(), s.getCategory().toString() };
				todoTableModel.addRow(row);
			}
		}
		JTable todoTable = new JTable(todoTableModel);
		todoTable.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
		todoTable.setRowHeight(30);
		// 스크롤 패널에 추가
		JScrollPane rightScrollPane = new JScrollPane(todoTable);
		rightPanel.add(rightScrollPane, BorderLayout.CENTER);
		// 좌우 분할 패널
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		splitPane.setDividerLocation(400); // 초기 분할 위치 조정 (필요에 따라 변경)
		splitPane.setResizeWeight(0.5); // 크기 조절 시 좌우 비율 유지
		// mainPanel에 splitPane 추가
		JPanel mainPanel = new JPanel(new BorderLayout());
		JLabel mainLabel = new JLabel(LocalDateTime.now().format(headerFormatter), SwingConstants.CENTER);
		mainLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
		mainPanel.add(mainLabel, BorderLayout.NORTH);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		
		Timer clock = new Timer(1000, e -> {
		    mainLabel.setText(LocalDateTime.now().format(headerFormatter));
		});
		clock.start();
		
		// 캘린더 탭 - 캘린더 나타내기
		CalendarPanel calendarPanel = new CalendarPanel(userId);

		
		// 일정 탭 - 마감기한 최근 순으로 표시		
		EventPanel eventPanel = new EventPanel(userId);
		add(eventPanel);
		
		
		// ToDo 탭 - 마감기한 최근 순으로 표시
		TodoPanel todoPanel = new TodoPanel(userId);
		add(todoPanel);
		
		
		// 통계 탭
		StatsPanel statsPanel = new StatsPanel(userId);
		add(statsPanel);
		
		
		// 사용자 탭 - 로그아웃 & 비밀번호 변경 & 회원 탈퇴 버튼
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
			// 로그아웃 처리: 메인 윈도우 닫고 로그인 화면 다시 띄우기
			dispose();
			SwingUtilities.invokeLater(() -> {
				new Login().setVisible(true);
			});
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
		deleteAccountButton.setForeground(Color.RED); // 탈퇴 버튼이라 빨간색으로 강조 가능
		deleteAccountButton.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(this, "정말 회원 탈퇴를 진행하시겠습니까?\n모든 데이터가 삭제됩니다.", "회원 탈퇴 확인",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				int result = UserDao.deleteUser(loggedInUser.getUserId()); // DAO에서 탈퇴 처리 메서드 호출
				if (result == 1) {
					JOptionPane.showMessageDialog(this, "회원 탈퇴가 완료되었습니다.");
					dispose();
					SwingUtilities.invokeLater(() -> {
						new Login().setVisible(true);
					});
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
		
		// 탭 추가
		tabbedPane.addTab("홈", mainPanel);
		tabbedPane.addTab("캘린더", calendarPanel);
		tabbedPane.addTab("일정", eventPanel);
		tabbedPane.addTab("ToDo", todoPanel);
		tabbedPane.addTab("통계", statsPanel);
		tabbedPane.addTab("설정", userPanel);
		// 탭 선택 리스너 - 화면 업데이트
		tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            String selectedTitle = tabbedPane.getTitleAt(selectedIndex);
            if ("홈".equals(selectedTitle)) {
                refreshMainTabData();
            }
        });
		getContentPane().add(tabbedPane);
	}
	
	private void refreshMainTabData() {
	    LocalDate today = LocalDate.now();
	    scheduleTableModel.setRowCount(0); // 기존 데이터 삭제
	    for (EventVO s : EventDao.selectEventsByDate(loggedInUser.getUserId(), today)) {
	        scheduleTableModel.addRow(new Object[]{
	            s.getStartDate().format(timeFormatterHM),
	            s.getEndDate().format(timeFormatterHM),
	            s.getTitle()
	        });
	    }
	    todoTableModel.setRowCount(0);
	    for (TodoVO s : TodoDao.selectAllTodosByUserId(loggedInUser.getUserId())) {
	    	if(s.getStatus() != Status.완료) {
				String dueStr = (s.getDueDate() != null) ? s.getDueDate().format(timeFormatterYMDHM) : "기한 없음";
				Object[] row = { s.getCategory(), dueStr, s.getPriority().toString(), s.getStatus().toString() };
				todoTableModel.addRow(row);
			}
	    }
	}
	
	public static void main(String[] args) {
		new Login();
	}
}
