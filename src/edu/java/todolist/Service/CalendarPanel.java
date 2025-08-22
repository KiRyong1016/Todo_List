package edu.java.todolist.Service;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDayChooser;
import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 달력 패널
 * - 현재 월 기준(+/- 7일 버퍼) 범위로 이벤트 조회
 * - 멀티데이: 시작~종료 모든 날짜 하이라이트
 * - 날짜 클릭: 그 날짜와 겹치는 모든 이벤트 상세 표시(반복 전개 포함)
 * UI 레이아웃은 기존과 동일
 */
public class CalendarPanel extends JPanel {

    private final JCalendar calendar;
    private final int userId;
    private final EventDAO eventDAO;
    private Set<LocalDate> eventDates;

    public CalendarPanel(int userId) {
        this.userId = userId;
        this.eventDAO = EventDAOImple.getInstance();
        this.eventDates = new HashSet<>();

        setLayout(new BorderLayout());
        calendar = new JCalendar();
        add(calendar, BorderLayout.CENTER);

        loadEventsAndHighlight();

        // 월 전환/날짜 변경 시 갱신
        calendar.addPropertyChangeListener("calendar", evt -> loadEventsAndHighlight());
    }

    /** 현재 표시 월의 앞뒤 7일을 포함해 이벤트를 조회하고 하이라이트 세팅 */
    private void loadEventsAndHighlight() {
        LocalDate firstDayOfMonth = calendar.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .withDayOfMonth(1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        LocalDate queryStart = firstDayOfMonth.minusDays(7);
        LocalDate queryEnd   = lastDayOfMonth.plusDays(7);

        // 반복 전개 포함: 해당 범위 전체 이벤트
        List<EventVO> events = eventDAO.selectEventsByDateRange(userId, queryStart, queryEnd);

        // 멀티데이 포함: 시작~종료 모든 날짜를 eventDates에 적재
        Set<LocalDate> dates = new HashSet<>();
        for (EventVO e : events) {
            LocalDate s = e.getStartDate().toLocalDate();
            LocalDate tEnd = (e.getEndDate() != null ? e.getEndDate() : e.getStartDate()).toLocalDate();
            // 범위를 너무 벗어난 것 클리핑
            if (tEnd.isBefore(queryStart) || s.isAfter(queryEnd)) continue;
            LocalDate from = (s.isBefore(queryStart) ? queryStart : s);
            LocalDate to   = (tEnd.isAfter(queryEnd) ? queryEnd : tEnd);
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                dates.add(d);
            }
        }
        this.eventDates = dates;

        highlightEventDates();
    }

    /** day panel의 각 날짜 버튼에 하이라이트 및 클릭 리스너 설정 */
    private void highlightEventDates() {
        JDayChooser dayChooser = calendar.getDayChooser();
        Component[] components = dayChooser.getDayPanel().getComponents();

        Calendar cal = Calendar.getInstance();
        cal.setTime(calendar.getDate());
        int displayedMonth = cal.get(Calendar.MONTH) + 1; // 1~12
        int displayedYear  = cal.get(Calendar.YEAR);

        for (Component comp : components) {
            if (!(comp instanceof JButton)) continue;

            JButton dayButton = (JButton) comp;
            String text = dayButton.getText();
            if (!text.matches("\\d+")) continue; // 숫자만 날짜 버튼

            int day = Integer.parseInt(text);

            // 기본은 현재 월
            int buttonMonth = displayedMonth;
            int buttonYear  = displayedYear;

            // 회색(다른 달) 판단: JCalendar 기본 렌더링 기준
            // 앞달 날짜(보통 22~31 범위) 또는 다음달 날짜(보통 1~14 범위)가 연회색으로 나타남
            if (Color.LIGHT_GRAY.equals(dayButton.getForeground())) {
                if (day >= 22) {
                    // 앞달
                    Calendar prev = Calendar.getInstance();
                    prev.set(displayedYear, displayedMonth - 2, day); // month-2: 0-based
                    buttonMonth = prev.get(Calendar.MONTH) + 1;
                    buttonYear  = prev.get(Calendar.YEAR);
                } else {
                    // 다음달
                    Calendar next = Calendar.getInstance();
                    next.set(displayedYear, displayedMonth, day);
                    buttonMonth = next.get(Calendar.MONTH) + 1;
                    buttonYear  = next.get(Calendar.YEAR);
                }
            }

            LocalDate date = LocalDate.of(buttonYear, buttonMonth, day);

            // 하이라이트
            if (eventDates.contains(date)) {
                dayButton.setOpaque(true);
                dayButton.setContentAreaFilled(true);
                dayButton.setBackground(new Color(135, 206, 250)); // 하늘색
                dayButton.setForeground(Color.WHITE);
                dayButton.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                dayButton.setOpaque(true);
                dayButton.setContentAreaFilled(true);
                dayButton.setBackground(javax.swing.UIManager.getColor("Button.background"));
                dayButton.setForeground(javax.swing.UIManager.getColor("Button.foreground"));
                dayButton.setBorder(javax.swing.UIManager.getBorder("Button.border"));
            }

            // 기존 리스너 제거 후 다시 설정 (중복 방지)
            for (var al : dayButton.getActionListeners()) {
                dayButton.removeActionListener(al);
            }
            dayButton.addActionListener(e -> showEventsForDate(date));

            dayButton.repaint();
            dayButton.revalidate();
        }
    }

    /** 해당 날짜와 겹치는 모든 이벤트를 상세로 표시 (반복 전개 포함) */
    private void showEventsForDate(LocalDate date) {
        List<EventVO> events = eventDAO.selectEventsByDateRange(userId, date, date);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "일정 - " + date, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        DateTimeFormatter TIME  = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        if (events.isEmpty()) {
            textArea.setText("해당 날짜에 일정이 없습니다.");
        } else {
            events.sort(Comparator.comparing(EventVO::getStartDate));
            StringBuilder sb = new StringBuilder();
            for (EventVO event : events) {
                LocalDateTime s = event.getStartDate();
                LocalDateTime e = (event.getEndDate() != null) ? event.getEndDate() : event.getStartDate();

                boolean bothOnTarget = s.toLocalDate().equals(date) && e.toLocalDate().equals(date);

                if (bothOnTarget) {
                    sb.append("시간: ").append(s.format(TIME)).append(" ~ ").append(e.format(TIME)).append("\n");
                } else {
                    sb.append("시작: ").append(s.format(YMDHM)).append("\n");
                    sb.append("종료: ").append(e.format(YMDHM)).append("\n");
                }
                sb.append("제목: ").append(event.getTitle()).append("\n");
                sb.append("내용: ");
                if (event.getDescription() == null) sb.append("\n");
                else sb.append(event.getDescription()).append("\n");
                sb.append("------------------------\n");
            }
            textArea.setText(sb.toString());
        }

        dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("추가");
        JButton closeButton = new JButton("닫기");
        buttonPanel.add(addButton);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            EventEditDialog edit = new EventEditDialog(owner, userId);
            edit.setVisible(true);
            loadEventsAndHighlight(); // 추가 후 하이라이트 갱신
        });

        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }
}
