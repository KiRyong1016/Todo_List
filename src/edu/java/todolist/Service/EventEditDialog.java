package edu.java.todolist.Service;

import com.toedter.calendar.JDateChooser;
import edu.java.todolist.DAO.EventDAO;
import edu.java.todolist.DAOImple.EventDAOImple;
import edu.java.todolist.VO.EventVO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 일정 추가/수정 다이얼로그
 * - 시작/종료: JDateChooser(달력) + JSpinner(시간 HH:mm)
 * - 반복: "없음/매일/매주/매월/매년" → 저장 시 "DAILY/WEEKLY/MONTHLY/YEARLY"로 정규화
 * - 생성/수정 겸용: model(EventVO)이 null이면 생성, 있으면 수정
 */
public class EventEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final EventDAO eventDAO = EventDAOImple.getInstance();

    private final int userId;
    private EventVO model; // null이면 생성 모드

    // UI
    private JTextField titleField;
    private JTextArea  descArea;
    private JDateChooser startDateChooser, endDateChooser;
    private JSpinner     startTimeSpinner, endTimeSpinner;
    private JComboBox<String> repeatCombo;

    // --- 생성자 오버로드 ---
    public EventEditDialog(Frame owner, int userId) {
        this(owner, userId, null);
    }

    public EventEditDialog(Frame owner, int userId, EventVO event) {
        super(owner, (event == null) ? "일정 추가" : "일정 수정", true);
        this.userId = userId;
        this.model  = event;
        initUI();
        if (model != null) {
            loadModel(model);
        } else {
            setDefaults();
        }
        pack();
        setLocationRelativeTo(owner);
    }

    // 편의 오버로드(예: new EventEditDialog(sel, userId))
    public EventEditDialog(EventVO event, int userId) {
        this((Frame) null, userId, event);
    }

    // ================= UI =================
    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 헤더
        JLabel header = new JLabel(getTitle());
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        add(header, BorderLayout.NORTH);

        // 폼
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(4, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(6, 4, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // 제목
        form.add(new JLabel("제목:"), gbc);
        titleField = new JTextField(28);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(titleField, gbc);

        // 시작
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("시작:"), gbc);
        JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        startDateChooser.setPreferredSize(new java.awt.Dimension(130, 24));
        startTimeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        startTimeSpinner.setEditor(new JSpinner.DateEditor(startTimeSpinner, "HH:mm"));
        startPanel.add(startDateChooser);
        startPanel.add(startTimeSpinner);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(startPanel, gbc);

        // 종료
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("종료:"), gbc);
        JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        endDateChooser.setPreferredSize(new java.awt.Dimension(130, 24));
        endTimeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        endTimeSpinner.setEditor(new JSpinner.DateEditor(endTimeSpinner, "HH:mm"));
        endPanel.add(endDateChooser);
        endPanel.add(endTimeSpinner);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(endPanel, gbc);

        // 반복
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("반복:"), gbc);
        repeatCombo = new JComboBox<>(new String[]{"없음", "매일", "매주", "매월", "매년"});
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(repeatCombo, gbc);

        // 설명
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("설명:"), gbc);
        descArea = new JTextArea(4, 28);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        form.add(descScroll, gbc);

        add(form, BorderLayout.CENTER);

        // 버튼
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("저장");
        JButton cancelBtn = new JButton("취소");
        btns.add(saveBtn);
        btns.add(cancelBtn);
        add(btns, BorderLayout.SOUTH);

        // 기본 버튼
        getRootPane().setDefaultButton(saveBtn);

        // 이벤트
        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void setDefaults() {
        LocalDateTime now = LocalDateTime.now();
        setDateTime(startDateChooser, startTimeSpinner, now);
        setDateTime(endDateChooser,   endTimeSpinner,   now.plusHours(1));
        repeatCombo.setSelectedItem("없음");
    }

    private void loadModel(EventVO ev) {
        titleField.setText(s(ev.getTitle()));
        descArea.setText(s(ev.getDescription()));

        if (ev.getStartDate() != null) {
            setDateTime(startDateChooser, startTimeSpinner, ev.getStartDate());
        }
        if (ev.getEndDate() != null) {
            setDateTime(endDateChooser, endTimeSpinner, ev.getEndDate());
        }
        repeatCombo.setSelectedItem(ev.getRepeatType());
    }

    // ================= 동작 =================
    private void onSave() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "제목을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Date sd = startDateChooser.getDate();
            Date ed = endDateChooser.getDate();
            if (sd == null || ed == null) {
                JOptionPane.showMessageDialog(this, "시작/종료 날짜를 선택하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime start = combine(sd, (Date) startTimeSpinner.getValue());
            LocalDateTime end   = combine(ed, (Date) endTimeSpinner.getValue());
            if (end.isBefore(start)) {
                JOptionPane.showMessageDialog(this, "종료일시는 시작일시 이후여야 합니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String repeatCode = ((String) repeatCombo.getSelectedItem());

            EventVO vo = (model != null) ? model : new EventVO();
            vo.setUserId(userId);
            vo.setTitle(title);
            vo.setDescription(emptyToNull(descArea.getText().trim()));
            vo.setStartDate(start);
            vo.setEndDate(end);
            vo.setRepeatType(repeatCode);

            int result;
            if (model == null) {
                result = eventDAO.insertEvent(vo);
            } else {
                result = eventDAO.updateEvent(vo);
            }

            if (result == 1) {
                JOptionPane.showMessageDialog(this, "저장되었습니다.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "저장에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "저장 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= 헬퍼 =================
    private static void setDateTime(JDateChooser chooser, JSpinner spinner, LocalDateTime ldt) {
        ZoneId zone = ZoneId.systemDefault();
        Date dateVal = Date.from(ldt.toLocalDate().atStartOfDay(zone).toInstant());
        chooser.setDate(dateVal);

        LocalTime t = ldt.toLocalTime();
        LocalDate today = LocalDate.now();
        Date timeVal = Date.from(LocalDateTime.of(today, t).atZone(zone).toInstant());
        spinner.setValue(timeVal);
    }

    private static LocalDateTime combine(Date datePart, Date timePart) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate d = Instant.ofEpochMilli(datePart.getTime()).atZone(zone).toLocalDate();
        LocalTime t = (timePart == null)
                ? LocalTime.of(0, 0)
                : Instant.ofEpochMilli(timePart.getTime()).atZone(zone).toLocalTime().withSecond(0).withNano(0);
        return LocalDateTime.of(d, t);
    }

    private static String s(String x) { return (x == null) ? "" : x; }
    private static String emptyToNull(String x) { return (x == null || x.isEmpty()) ? null : x; }


}
