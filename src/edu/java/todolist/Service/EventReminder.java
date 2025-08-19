package edu.java.todolist.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JOptionPane;

import edu.java.todolist.VO.EventVO;

public class EventReminder {

    // 이벤트 하나의 알림 등록
    public static void scheduleReminder(EventVO event) {
        if (event.getStartDate() == null) return;

        int[] minutesBefore = {30, 10, 5}; // 알림 시간

        LocalDateTime now = LocalDateTime.now();

        for (int minutes : minutesBefore) {
            LocalDateTime reminderTime = event.getStartDate().minusMinutes(minutes);
            long delay = Duration.between(now, reminderTime).toMillis();
            if (delay <= 0) continue; // 이미 지난 시간은 패스

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            "일정 \"" + event.getTitle() + "\" 시작 " + minutes + "분 전입니다!",
                            "알림",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }, delay);
        }
    }
}
