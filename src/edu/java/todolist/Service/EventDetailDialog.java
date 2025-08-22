package edu.java.todolist.Service;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

public class EventDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final EventPanel parent;
    private EventVO event;
    private final int eventId;
    private final int userId;
    private final EventDAO eventDAO = EventDAOImple.getInstance();

    // 포맷터
    private final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public EventDetailDialog(EventPanel parent, int eventId, int userId) {
        this.parent = parent;
        this.eventId = eventId;
        this.userId = userId;

        // NOTE: 메소드명이 조금 특이하지만, 현재 프로젝트에 존재하는 메서드명을 그대로 사용
        this.event = eventDAO.selectAllTodoByEventId(eventId);

        setTitle("일정 상세 보기");
        setSize(460, 360);
        setLocationRelativeTo(null);
        setModal(true);
        setResizable(true);

        initUI();
    }

    private void initUI() {
        // ===== 중앙: 스크롤 가능한 상세 뷰 =====
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));

        JTextPane detailPane = new JTextPane();
        detailPane.setContentType("text/html");
        detailPane.setEditable(false);
        detailPane.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        detailPane.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        detailPane.setText(buildDetailHtml(event));

        JScrollPane scroll = new JScrollPane(
                detailPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        contentPanel.add(scroll, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        // ===== 하단: 버튼 =====
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton editButton = new JButton("수정");
        JButton deleteButton = new JButton("삭제");
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 수정
        editButton.addActionListener(e -> {
            // 부모 프레임을 오너로 전달 (센터링/포커스 안정)
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(EventDetailDialog.this);
            EventEditDialog editDialog = new EventEditDialog(owner, userId, event);
            editDialog.setVisible(true);

            // 저장 후 새로고침
            EventVO updated = eventDAO.selectAllTodoByEventId(event.getEventId());
            if (updated != null) {
                event = updated;
                dispose();
                // 상세 다시 열기
                new EventDetailDialog(parent, event.getEventId(), event.getUserId()).setVisible(true);
                // 목록 갱신(프로젝트에 따라 loadEventsPaged가 있으면 그걸로)
                try {
                    parent.getClass().getMethod("loadEventsPaged").invoke(parent);
                } catch (NoSuchMethodException ex) {
                    parent.loadEvents();
                } catch (Exception ex) {
                    parent.loadEvents();
                }
            } else {
                JOptionPane.showMessageDialog(this, "업데이트 후 데이터를 불러올 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 삭제
        deleteButton.addActionListener((ActionEvent ev) -> {
            int confirm = JOptionPane.showConfirmDialog(
                    EventDetailDialog.this,
                    "정말 삭제하시겠습니까?",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                int result = eventDAO.deleteEvent(event.getEventId());
                if (result == 1) {
                    JOptionPane.showMessageDialog(EventDetailDialog.this, "삭제 완료!");
                    dispose();
                    try {
                        parent.getClass().getMethod("loadEventsPaged").invoke(parent);
                    } catch (NoSuchMethodException ex) {
                        parent.loadEvents();
                    } catch (Exception ex) {
                        parent.loadEvents();
                    }
                } else {
                    JOptionPane.showMessageDialog(EventDetailDialog.this, "삭제 실패!", "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /** HTML 상세 문자열 구성 (긴 내용 줄바꿈/래핑 포함) */
    private String buildDetailHtml(EventVO ev) {
        String title = safe(ev.getTitle());
        String desc  = safe(ev.getDescription());
        String rpt   = safe(ev.getRepeatType());

        LocalDateTime s = ev.getStartDate();
        LocalDateTime e = (ev.getEndDate() != null) ? ev.getEndDate() : ev.getStartDate();

        String sStr = (s != null) ? s.format(YMDHM) : "";
        String eStr = (e != null) ? e.format(YMDHM) : "";

        // 긴 단어도 잘리도록 CSS 추가 (overflow-wrap/word-break)
        return """
            <html>
            <body style="font-family: '맑은 고딕', Malgun Gothic, sans-serif; font-size: 13pt; line-height: 1.5;
                         overflow-wrap: break-word; word-wrap: break-word; word-break: break-word;">
              <div style="margin-bottom:8px;"><b>시작:</b> %s</div>
              <div style="margin-bottom:8px;"><b>종료:</b> %s</div>
              <div style="margin-bottom:8px;"><b>제목:</b> %s</div>
              <div style="margin-bottom:8px;"><b>상세:</b><br>%s</div>
              <div style="margin-bottom:8px;"><b>반복:</b> %s</div>
            </body>
            </html>
        """.formatted(escape(sStr), escape(eStr), escape(title), nl2br(escape(desc)), escape(rpt));
    }

    // 유틸
    private static String safe(String s) { return (s == null) ? "" : s; }
    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String nl2br(String s) { return s.replace("\r\n","<br>").replace("\n","<br>"); }
}
