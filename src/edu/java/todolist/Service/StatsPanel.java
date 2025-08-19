package edu.java.todolist.Service;

import edu.java.todolist.DAOImple.TodoDAOImple;
import edu.java.todolist.VO.TodoVO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.List;

/**
 * 통계 패널
 * - 상단: 전체 완료율(도넛/파이 차트) + KPI 라벨
 * - 하단: 기간/단위(일별·주별) 선택 → 기한(dueDate) 기준 예정건/완료건(막대) + 완료율%(라인) 복합 차트
 */
public class StatsPanel extends JPanel {
    private final int userId;

    // KPI 라벨
    private final JLabel lblOverall = new JLabel("-", SwingConstants.CENTER);

    // 차트 패널
    private ChartPanel donutPanel;
    private ChartPanel comboChartPanel;

    // 필터 UI
    private final JTextField txtFrom = new JTextField(10);
    private final JTextField txtTo = new JTextField(10);
    private final JComboBox<String> periodCombo = new JComboBox<>(new String[]{"일별", "주별"});
    private final JButton btnApply = new JButton("적용");

    public StatsPanel(int userId) {
        this.userId = userId;
        setLayout(new BorderLayout());

        // ===== 상단: KPI + 도넛 =====
        JPanel north = new JPanel(new BorderLayout());
        north.add(buildKpiPanel(), BorderLayout.NORTH);

        List<TodoVO> initTodos = loadTodos();
        donutPanel = new ChartPanel(buildDonutChart(initTodos));
        donutPanel.setPreferredSize(new Dimension(400, 220));
        north.add(donutPanel, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // ===== 중간: 필터 + 복합 차트 =====
        JPanel mid = new JPanel(new BorderLayout());
        mid.add(buildFilterBar(), BorderLayout.NORTH);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        comboChartPanel = new ChartPanel(buildComboChart(initTodos, today.minusDays(6), today, "일별"));
        comboChartPanel.setPreferredSize(new Dimension(900, 380));
        mid.add(comboChartPanel, BorderLayout.CENTER);

        add(mid, BorderLayout.CENTER);

        // 초기 KPI 텍스트
        updateOverallText(initTodos);

        // 이벤트
        btnApply.addActionListener(e -> {
            List<TodoVO> todos = loadTodos();
            LocalDate from = parseDateOrDefault(txtFrom.getText(), LocalDate.now().minusDays(6));
            LocalDate to   = parseDateOrDefault(txtTo.getText(), LocalDate.now());
            String unit = (String) periodCombo.getSelectedItem();

            donutPanel.setChart(buildDonutChart(todos));
            comboChartPanel.setChart(buildComboChart(todos, from, to, unit));
        });
    }

    // ===================== UI 빌더 =====================
    private JPanel buildKpiPanel() {
        JPanel p = new JPanel(new GridLayout(1, 1, 12, 0));
        JPanel card = new JPanel(new BorderLayout());
        JLabel title = new JLabel("전체 완료율", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        lblOverall.setFont(lblOverall.getFont().deriveFont(Font.BOLD, 20f));
        card.add(title, BorderLayout.NORTH);
        card.add(lblOverall, BorderLayout.CENTER);
        card.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        p.add(card);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel filter = new JPanel();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        txtFrom.setText(today.minusDays(6).toString());
        txtTo.setText(today.toString());
        filter.add(new JLabel("기간:"));
        filter.add(txtFrom);
        filter.add(new JLabel("~"));
        filter.add(txtTo);
        filter.add(new JLabel("단위:"));
        filter.add(periodCombo);
        filter.add(btnApply);
        return filter;
    }

    // ===================== 데이터 로드/헬퍼 =====================
    private List<TodoVO> loadTodos() {
        List<TodoVO> list = TodoDAOImple.getInstance().selectAllTodosByUserId(userId);
        return (list != null) ? list : Collections.emptyList();
    }

    private LocalDate parseDateOrDefault(String text, LocalDate fallback) {
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void updateOverallText(List<TodoVO> todos) {
        long done = todos.stream().filter(t -> t.getStatus() == TodoVO.Status.완료).count();
        long total = todos.size();
        double rate = total == 0 ? 0.0 : (double) done / total * 100.0;
        lblOverall.setText(String.format("%.0f%% (%d/%d)", rate, done, total));
    }

    // ===================== 차트: 도넛(파이) =====================
    private JFreeChart buildDonutChart(List<TodoVO> todos) {
        long done = todos.stream().filter(t -> t.getStatus() == TodoVO.Status.완료).count();
        long total = todos.size();
        long notDone = Math.max(0, total - done);

        DefaultPieDataset ds = new DefaultPieDataset();
        ds.setValue("완료", done);
        ds.setValue("미완료", notDone);

        JFreeChart chart = ChartFactory.createPieChart("전체 완료율", ds, true, true, false);
        applyKoreanFont(chart);
        updateOverallText(todos);
        return chart;
    }

    // ===================== 차트: 복합(막대+라인) =====================
    private JFreeChart buildComboChart(List<TodoVO> todos, LocalDate from, LocalDate to, String unit) {
        DefaultCategoryDataset barDs = new DefaultCategoryDataset();   // 예정건/완료건
        DefaultCategoryDataset lineDs = new DefaultCategoryDataset();  // 완료율(%)

        if ("주별".equals(unit)) {
            Map<LocalDate, GroupAgg> agg = aggregateByWeek(todos, from, to); // key = 주 시작(월요일)
            DateTimeFormatter weekLabel = DateTimeFormatter.ofPattern("YYYY-'W'ww");
            for (Map.Entry<LocalDate, GroupAgg> e : agg.entrySet()) {
                String key = e.getKey().format(weekLabel);
                GroupAgg g = e.getValue();
                barDs.addValue(g.total, "예정건", key);
                barDs.addValue(g.done,  "완료건", key);
                double ratePct = g.total == 0 ? 0.0 : (g.done * 100.0 / g.total);
                lineDs.addValue(ratePct, "완료율(%)", key);
            }
            JFreeChart chart = ChartFactory.createBarChart(
                    "기한 기준 주별 완료 현황", "주", "건수", barDs);
            applyComboPlotStyling(chart, lineDs);
            return chart;
        } else {
            Map<LocalDate, GroupAgg> agg = aggregateByDay(todos, from, to); // key = 날짜
            DateTimeFormatter dayLabel = DateTimeFormatter.ISO_LOCAL_DATE;
            for (Map.Entry<LocalDate, GroupAgg> e : agg.entrySet()) {
                String key = e.getKey().format(dayLabel);
                GroupAgg g = e.getValue();
                barDs.addValue(g.total, "예정건", key);
                barDs.addValue(g.done,  "완료건", key);
                double ratePct = g.total == 0 ? 0.0 : (g.done * 100.0 / g.total);
                lineDs.addValue(ratePct, "완료율(%)", key);
            }
            JFreeChart chart = ChartFactory.createBarChart(
                    "기한 기준 일별 완료 현황", "날짜", "건수", barDs);
            applyComboPlotStyling(chart, lineDs);
            return chart;
        }
    }

    private void applyComboPlotStyling(JFreeChart chart, DefaultCategoryDataset lineDs) {
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        NumberAxis countAxis = (NumberAxis) plot.getRangeAxis();
        countAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        // 오른쪽(%) 축 추가
        NumberAxis percentAxis = new NumberAxis("완료율(%)");
        percentAxis.setAutoRangeIncludesZero(true);
        plot.setRangeAxis(1, percentAxis);

        // 라인 렌더러/데이터셋
        LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer(true, true);
        plot.setDataset(1, lineDs);
        plot.setRenderer(1, lineRenderer);
        plot.mapDatasetToRangeAxis(1, 1); // lineDs -> percentAxis

        // 막대 간격
        BarRenderer barRenderer = (BarRenderer) plot.getRenderer(0);
        barRenderer.setItemMargin(0.15);

        applyKoreanFont(chart);
    }

    // ===================== 집계 로직 =====================
    /** 일별: from~to 날짜를 모두 키로 채운 뒤 dueDate가 해당일인 항목 집계 */
    private Map<LocalDate, GroupAgg> aggregateByDay(List<TodoVO> todos, LocalDate from, LocalDate to) {
        Map<LocalDate, GroupAgg> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d, new GroupAgg());
        }
        for (TodoVO t : todos) {
            if (t.getDueDate() == null) continue;
            LocalDate d = t.getDueDate().toLocalDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;
            GroupAgg g = map.get(d);
            if (g == null) continue; // 안전
            g.total++;
            if (t.getStatus() == TodoVO.Status.완료) g.done++;
        }
        return map;
    }

    /** 주별(ISO, 월요일 시작): from~to의 각 주 시작일(월요일)을 키로 선채움 후 집계 */
    private Map<LocalDate, GroupAgg> aggregateByWeek(List<TodoVO> todos, LocalDate from, LocalDate to) {
        WeekFields wf = WeekFields.ISO; // 한국과 동일(월요일 시작)
        LocalDate startWeek = from.with(wf.dayOfWeek(), 1); // from이 속한 주의 월요일
        LocalDate endWeekBase = to.with(wf.dayOfWeek(), 1);
        Map<LocalDate, GroupAgg> map = new LinkedHashMap<>();
        for (LocalDate d = startWeek; !d.isAfter(endWeekBase); d = d.plusWeeks(1)) {
            map.put(d, new GroupAgg());
        }
        for (TodoVO t : todos) {
            if (t.getDueDate() == null) continue;
            LocalDate due = t.getDueDate().toLocalDate();
            if (due.isBefore(from) || due.isAfter(to)) continue;
            LocalDate weekStart = due.with(wf.dayOfWeek(), 1);
            GroupAgg g = map.get(weekStart);
            if (g == null) continue;
            g.total++;
            if (t.getStatus() == TodoVO.Status.완료) g.done++;
        }
        return map;
    }

    private static class GroupAgg {
        int total = 0;
        int done  = 0;
    }

    // ===================== 한글 폰트 적용 =====================
    // 시스템에 존재할 법한 한글 폰트 후보
    private static final String[] KOREAN_FONT_CANDIDATES = new String[] {
            "맑은 고딕", "Malgun Gothic",              // Windows
            "Apple SD Gothic Neo", "AppleGothic",      // macOS
            "Noto Sans CJK KR", "NanumGothic", "나눔고딕", "굴림" // Linux/기타
    };

    private Font pickKoreanFont(int style, int size) {
        String[] installed = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        Set<String> set = new HashSet<>(Arrays.asList(installed));
        for (String name : KOREAN_FONT_CANDIDATES) {
            if (set.contains(name)) {
                return new Font(name, style, size);
            }
        }
        return new Font("SansSerif", style, size); // 최후수단(네모 위험)
    }

    /** 차트에 한글 폰트 일괄 적용 */
    private void applyKoreanFont(JFreeChart chart) {
        Font titleF   = pickKoreanFont(Font.BOLD, 16);
        Font legendF  = pickKoreanFont(Font.PLAIN, 12);
        Font labelF   = pickKoreanFont(Font.BOLD, 12);
        Font tickF    = pickKoreanFont(Font.PLAIN, 11);

        if (chart.getTitle() != null) chart.getTitle().setFont(titleF);
        for (var s : chart.getSubtitles()) {
            if (s instanceof org.jfree.chart.title.LegendTitle lt) {
                lt.setItemFont(legendF);
            } else if (s instanceof org.jfree.chart.title.TextTitle tt) {
                tt.setFont(legendF);
            }
        }

        var plot = chart.getPlot();
        if (plot instanceof CategoryPlot cp) {
            if (cp.getDomainAxis() != null) {
                cp.getDomainAxis().setLabelFont(labelF);
                cp.getDomainAxis().setTickLabelFont(tickF);
            }
            if (cp.getRangeAxis() != null) {
                cp.getRangeAxis().setLabelFont(labelF);
                cp.getRangeAxis().setTickLabelFont(tickF);
            }
        } else if (plot instanceof org.jfree.chart.plot.XYPlot xp) {
            if (xp.getDomainAxis() != null) {
                xp.getDomainAxis().setLabelFont(labelF);
                xp.getDomainAxis().setTickLabelFont(tickF);
            }
            if (xp.getRangeAxis() != null) {
                xp.getRangeAxis().setLabelFont(labelF);
                xp.getRangeAxis().setTickLabelFont(tickF);
            }
        } else if (plot instanceof org.jfree.chart.plot.PiePlot pp) {
            pp.setLabelFont(tickF);
        }

        chart.getRenderingHints().put(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
    }
}
