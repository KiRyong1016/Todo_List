package edu.java.todolist.Service;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDayChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarPanel extends JPanel {

    private JCalendar calendar;
    private int userId;
    private EventDAO eventDAO;
    private Set<LocalDate> eventDates;

    public CalendarPanel(int userId) {
        this.userId = userId;
        this.eventDAO = EventDAOImple.getInstance();
        this.eventDates = new HashSet<>();

        setLayout(new BorderLayout());
        calendar = new JCalendar();
        add(calendar, BorderLayout.CENTER);

        loadEventsAndHighlight();

        calendar.addPropertyChangeListener("calendar", evt -> {
            loadEventsAndHighlight();
        });
    }

    private void loadEventsAndHighlight() {
        LocalDate firstDayOfMonth = calendar.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .withDayOfMonth(1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        // 이번 달 + 앞뒤 날짜까지 커버하려면 범위를 넉넉하게 잡아야 함
        LocalDate queryStart = firstDayOfMonth.minusDays(7);
        LocalDate queryEnd = lastDayOfMonth.plusDays(7);

        List<EventVO> events = eventDAO.selectEventsByDateRange(userId, queryStart, queryEnd);

     // 멀티데이 일정 범위 전체를 하이라이트 대상으로 수집
        Set<LocalDate> dates = new HashSet<>();
        for (EventVO e : events) {
            if (e.getStartDate() == null) continue; // 안전 가드
            LocalDate s = e.getStartDate().toLocalDate();
            LocalDate t = (e.getEndDate() != null) ? e.getEndDate().toLocalDate() : s; // end가 null이면 단일일정

            // 혹시 end < start인 잘못된 데이터 방어
            if (t.isBefore(s)) {
                LocalDate tmp = s; s = t; t = tmp;
            }

            for (LocalDate d = s; !d.isAfter(t); d = d.plusDays(1)) {
                dates.add(d);
            }
        }
        eventDates = dates;


        highlightEventDates();
        System.out.println("loadEventsAndHighlight 호출: 이벤트 날짜 수 = " + eventDates.size());
    }

    private void highlightEventDates() {
        JDayChooser dayChooser = calendar.getDayChooser();

        Component[] components = dayChooser.getDayPanel().getComponents();

        Calendar cal = Calendar.getInstance();
        cal.setTime(calendar.getDate());
        int displayedMonth = cal.get(Calendar.MONTH) + 1; // 1~12
        int displayedYear = cal.get(Calendar.YEAR);

        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton dayButton = (JButton) comp;
                String text = dayButton.getText();

                if (!text.matches("\\d+")) {
                    continue; // 숫자 아닌 버튼은 무시
                }

                int day = Integer.parseInt(text);

                // JCalendar는 이전/다음 달도 표시하므로 실제 month 계산 필요
                int buttonMonth = displayedMonth;
                int buttonYear = displayedYear;

                if (day >= 22 && day <= 31 && text.equals(dayButton.getText())
                        && dayButton.getForeground().equals(Color.LIGHT_GRAY)) {
                    // 앞달 날짜
                    Calendar prev = Calendar.getInstance();
                    prev.set(displayedYear, displayedMonth - 2, day); // month-2: 0-based
                    buttonMonth = prev.get(Calendar.MONTH) + 1;
                    buttonYear = prev.get(Calendar.YEAR);
                } else if (day <= 14 && dayButton.getForeground().equals(Color.LIGHT_GRAY)) {
                    // 다음달 날짜
                    Calendar next = Calendar.getInstance();
                    next.set(displayedYear, displayedMonth, day);
                    buttonMonth = next.get(Calendar.MONTH) + 1;
                    buttonYear = next.get(Calendar.YEAR);
                }

                LocalDate date = LocalDate.of(buttonYear, buttonMonth, day);

                // 색상 설정
                if (eventDates.contains(date)) {
                    dayButton.setOpaque(true);
                    dayButton.setContentAreaFilled(true);
                    dayButton.setBackground(new Color(135, 206, 250)); // 하늘색
                    dayButton.setForeground(Color.WHITE);
                    dayButton.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                } else {
                    dayButton.setOpaque(true);
                    dayButton.setContentAreaFilled(true);
                    dayButton.setBackground(UIManager.getColor("Button.background"));
                    dayButton.setForeground(UIManager.getColor("Button.foreground"));
                    dayButton.setBorder(UIManager.getBorder("Button.border"));
                }

                // 중복 리스너 제거 후 새로 추가
                for (ActionListener al : dayButton.getActionListeners()) {
                    dayButton.removeActionListener(al);
                }
                dayButton.addActionListener(e -> showEventsForDate(date));

                dayButton.repaint();
                dayButton.revalidate();
            }
        }
    }

    private void showEventsForDate(LocalDate date) {
        // DB에서 해당 날짜 이벤트 가져오기
        List<EventVO> events = eventDAO.selectEventsByDateRange(userId, date, date);

        // 새 JFrame 또는 JDialog 생성
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "일정 - " + date.toString(), true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        if (events.isEmpty()) {
            textArea.setText("해당 날짜에 일정이 없습니다.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (EventVO event : events) {
                sb.append("제목: ").append(event.getTitle()).append("\n");
                sb.append("시작: ").append(event.getStartDate()).append("\n");
                sb.append("종료: ").append(event.getEndDate()).append("\n");
                sb.append("내용: ").append(event.getDescription()).append("\n");
                sb.append("------------------------\n");
            }
            textArea.setText(sb.toString());
        }

        dialog.add(new JScrollPane(textArea));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("추가");
        JButton closeButton = new JButton("닫기");
        buttonPanel.add(addButton);
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {

            EventEditDialog edit = new EventEditDialog(null, userId);
            edit.setVisible(true);

            loadEventsAndHighlight();
        });

        closeButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }
}
