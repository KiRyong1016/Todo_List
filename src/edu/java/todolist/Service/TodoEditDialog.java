package edu.java.todolist.Service;

import com.toedter.calendar.JDateChooser;
import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;
import edu.java.todolist.VO.TodoVO.Priority;
import edu.java.todolist.VO.TodoVO.Status;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * To-Do 추가/수정 다이얼로그
 * - 기한: JDateChooser(달력) + JSpinner(시간 HH:mm)
 * - "기한 없음" 체크 시 dueDate = null
 * - Priority/Status는 VO의 enum을 직접 사용 (타입 안정)
 * - 생성/수정 겸용: model(todo)가 null이면 추가 모드
 */
public class TodoEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private TodoVO model; // null이면 추가 모드

    // UI
    private JTextField titleField;
    private JTextArea  descArea;
    private JTextField categoryField;

    private JDateChooser dueDateChooser;
    private JSpinner     dueTimeSpinner;
    private JCheckBox    noDueCheck;

    private JComboBox<Priority> priorityCombo;
    private JComboBox<Status>   statusCombo;

    // ===== 생성자 =====
    /** 편의 생성자: 추가 모드 (owner는 null 가능) */
    public TodoEditDialog(Frame owner, int userId) {
        this(owner, userId, null);
    }

    /** 편의 생성자: 기존 코드 호환 (new TodoEditDialog(todo, userId)) */
    public TodoEditDialog(TodoVO todo, int userId) {
        this((Frame) null, userId, todo);
    }

    /** 메인 생성자: 추가/수정 겸용 */
    public TodoEditDialog(Frame owner, int userId, TodoVO todo) {
        super(owner, (todo == null || todo.getTodoId() == 0) ? "할 일 추가" : "할 일 수정", true);
        this.userId = userId;
        this.model  = todo;
        initUI();
        if (model != null && model.getTodoId() != 0) {
            loadModel(model);
        } else {
            setDefaults();
        }
        pack();
        setLocationRelativeTo(owner);
    }

    // ===== UI 구성 =====
    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel header = new JLabel(getTitle());
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        add(header, BorderLayout.NORTH);

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

        // 카테고리
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("카테고리:"), gbc);
        categoryField = new JTextField(20);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(categoryField, gbc);

        // 기한 (달력 + 시간 + "기한 없음")
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("기한:"), gbc);

        JPanel duePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dueDateChooser = new JDateChooser();
        dueDateChooser.setDateFormatString("yyyy-MM-dd");
        dueDateChooser.setPreferredSize(new java.awt.Dimension(130, 24));

        dueTimeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(dueTimeSpinner, "HH:mm");
        dueTimeSpinner.setEditor(timeEditor);

        noDueCheck = new JCheckBox("기한 없음");
        noDueCheck.addActionListener(e -> {
            boolean enabled = !noDueCheck.isSelected();
            dueDateChooser.setEnabled(enabled);
            dueTimeSpinner.setEnabled(enabled);
        });

        duePanel.add(dueDateChooser);
        duePanel.add(dueTimeSpinner);
        duePanel.add(noDueCheck);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(duePanel, gbc);

        // 중요도
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("중요도:"), gbc);
        priorityCombo = new JComboBox<>(Priority.values());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(priorityCombo, gbc);

        // 상태
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("상태:"), gbc);
        statusCombo = new JComboBox<>(Status.values());
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        form.add(statusCombo, gbc);

        // 내용
        gbc.gridx = 0; gbc.gridy++; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("내용:"), gbc);
        descArea = new JTextArea(5, 28);
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

        getRootPane().setDefaultButton(saveBtn);

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void setDefaults() {
        // 신규 기본값
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        // 오늘 18:00 같은 선호가 있으면 여기서 조정 가능
        setDueDateTime(now.plusHours(1)); // 기본: 1시간 뒤
        noDueCheck.setSelected(false);

        // 기본 중요도/상태 (있으면)
        if (priorityCombo.getItemCount() > 0) {
            priorityCombo.setSelectedIndex(0);
        }
        if (statusCombo.getItemCount() > 0) {
            statusCombo.setSelectedItem(Status.진행중); // 프로젝트 enum에 맞게 조정
        }
    }

    private void loadModel(TodoVO vo) {
        titleField.setText(safe(vo.getTitle()));
        descArea.setText(safe(vo.getDescription()));
        categoryField.setText(safe(vo.getCategory()));

        if (vo.getDueDate() == null) {
            noDueCheck.setSelected(true);
            dueDateChooser.setEnabled(false);
            dueTimeSpinner.setEnabled(false);
            // 일단 화면상 값은 현재로
            setDueDateTime(LocalDateTime.now().withSecond(0).withNano(0));
        } else {
            noDueCheck.setSelected(false);
            dueDateChooser.setEnabled(true);
            dueTimeSpinner.setEnabled(true);
            setDueDateTime(vo.getDueDate());
        }

        if (vo.getPriority() != null) priorityCombo.setSelectedItem(vo.getPriority());
        if (vo.getStatus()   != null) statusCombo.setSelectedItem(vo.getStatus());
    }

    // ===== 동작 =====
    private void onSave() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "제목을 입력하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime due = null;
            if (!noDueCheck.isSelected()) {
                Date d = dueDateChooser.getDate();
                if (d == null) {
                    JOptionPane.showMessageDialog(this, "기한 날짜를 선택하세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Date t = (Date) dueTimeSpinner.getValue();
                due = combine(d, t);
            }

            boolean isNew = (model == null || model.getTodoId() == 0);
            if (isNew) model = (model == null ? new TodoVO() : model);

            // 모델 반영
            model.setUserId(userId);
            model.setTitle(title);
            model.setDescription(emptyToNull(descArea.getText().trim()));
            model.setCategory(emptyToNull(categoryField.getText().trim()));
            model.setDueDate(due);
            model.setPriority((Priority) priorityCombo.getSelectedItem());
            model.setStatus((Status) statusCombo.getSelectedItem());

            int result = isNew
                    ? TodoDAOImple.getInstance().insertTodo(model)
                    : TodoDAOImple.getInstance().updateTodo(model);

            if (result == 1) {
                JOptionPane.showMessageDialog(this, "저장되었습니다.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "저장에 실패했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "저장 처리 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== 헬퍼 =====
    private void setDueDateTime(LocalDateTime ldt) {
        ZoneId zone = ZoneId.systemDefault();

        // 날짜: 00:00 기준으로 Date 생성
        Date dateVal = Date.from(ldt.toLocalDate().atStartOfDay(zone).toInstant());
        dueDateChooser.setDate(dateVal);

        // 시간: 오늘 날짜에 시간만 맞춰서 Date 생성
        LocalTime t = ldt.toLocalTime();
        LocalDate today = LocalDate.now();
        Date timeVal = Date.from(LocalDateTime.of(today, t).atZone(zone).toInstant());
        dueTimeSpinner.setValue(timeVal);
    }

    private static LocalDateTime combine(Date datePart, Date timePart) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate d = Instant.ofEpochMilli(datePart.getTime()).atZone(zone).toLocalDate();
        LocalTime t = (timePart == null)
                ? LocalTime.of(0, 0)
                : Instant.ofEpochMilli(timePart.getTime()).atZone(zone).toLocalTime().withSecond(0).withNano(0);
        return LocalDateTime.of(d, t);
    }

    private static String safe(String s) { return (s == null) ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    // 선택적으로, 다이얼로그를 여는 정적 팩토리도 제공 가능
    public static TodoEditDialog openForAdd(Frame owner, int userId) {
        return new TodoEditDialog(owner, userId, null);
    }
    public static TodoEditDialog openForEdit(Frame owner, int userId, TodoVO vo) {
        Objects.requireNonNull(vo, "todo must not be null");
        return new TodoEditDialog(owner, userId, vo);
    }
}
